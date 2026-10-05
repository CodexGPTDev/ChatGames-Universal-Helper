package dev.clares.chatgames.game;

import dev.clares.chatgames.chat.AnswerSender;
import dev.clares.chatgames.config.ConfigManager;
import dev.clares.chatgames.solver.*;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ServerLog106Test {
    @TempDir Path temp;

    @Test void screenshotWinnerClosesRoundWithoutASecondSendOrLocalNotice() throws Exception {
        ConfigManager config=new ConfigManager(temp);
        config.get().delay.mode="fixed";config.get().delay.minMs=100;
        List<String> sent=new CopyOnWriteArrayList<>(), notices=new CopyOnWriteArrayList<>();
        CountDownLatch first=new CountDownLatch(1), second=new CountDownLatch(1), third=new CountDownLatch(1);
        AnswerSender sender=new AnswerSender() {
            public boolean send(String answer) {sent.add(answer);if(sent.size()==1)first.countDown();else if(sent.size()==2)second.countDown();else third.countDown();return true;}
            public boolean clickCommand(String command) {fail("Unexpected command");return false;}
        };
        try(GameManager manager=new GameManager(config,notices::add,Runnable::run,()->true,sender)) {
            String prompt="You have 20 seconds to unscramble: `Cperere`";
            manager.onLog("[CHAT GAMES] You have started a unscramble chat event in your chat!");
            manager.onLog("✓ ✯ CHAT GAMES ✯ ✓");manager.onLog(prompt);flush(manager);
            assertTrue(first.await(5,TimeUnit.SECONDS));barrier(manager);
            assertEquals(List.of("Creeper"),sent);
            assertEquals(GameState.ANSWERED,manager.current().status);
            String result="Marco035xd has correctly unscrambled `Creeper` (3.0s) and got a prize!";
            manager.onRich(Text.literal(result));manager.onLog("✓ ✯ CHAT GAMES ✯ ✓");manager.onLog(result);flush(manager);
            assertNull(manager.current());
            assertFalse(second.await(700,TimeUnit.MILLISECONDS),"Winner summary caused a second outgoing message");
            assertTrue(notices.isEmpty(),"Automatic local notices should be silent by default");
            // A new explicitly started round can legitimately have the same prompt and answer.
            manager.onLog("[CHAT GAMES] You have started a unscramble chat event in your chat!");manager.onLog(prompt);flush(manager);
            assertTrue(second.await(5,TimeUnit.SECONDS));barrier(manager);
            assertEquals(List.of("Creeper","Creeper"),sent);
            manager.onLog("Marco035xd has correctly unscrambled Creeper (3.0s) and got a prize!");flush(manager);
            assertNull(manager.current());assertFalse(third.await(700,TimeUnit.MILLISECONDS));
        }
    }

    @Test void spanishGameInstructionsAndAllObservedEndingFormsAreDetected() {
        assertEquals(GameType.UNSCRAMBLE,GameDetector.detect("Tienes 20 segundos para poner en orden la palabra: `ardeiP`",true).orElseThrow().type());
        assertEquals("euovH",GameDetector.detect("------------ CHAT GAME ------------\nOrdena correctamente esta palabra:\neuovH\nTienes 20 segundos para responder.",true).orElseThrow().question());
        assertEquals(GameType.UNREVERSE,GameDetector.detect("------------ CHAT GAME ------------\nDescubre la palabra escrita al revés:\nacnalaP\nTienes 20 segundos para responder.",true).orElseThrow().type());
        assertEquals(GameType.FILLOUT,GameDetector.detect("Tienes 20 segundos para completar la palabra: `Dia_an_e`",true).orElseThrow().type());
        assertEquals(GameType.TRIVIA,GameDetector.detect("Tienes 20 segundos para responder:\n`¿Cuántos lados tiene un dado? (número)`",true).orElseThrow().type());
        for(String result:List.of("Marco035xd has correctly unscrambled `Creeper` (3.0s) and got a prize!","cristoferrex fue el más rápido en poner en orden `Piedra` (3.3s) y recibió un premio!","Marco035xd fue el primero en responder `Tierra` (5.6s) y recibió un premio!","Marco035xd respondió correctamente: Vidrio.","Han pasado 20s! El juego de completar ha terminado!","Han pasado 20s! El juego de poner en orden ha terminado!","El evento ha terminado","Nadie respondió correctamente a tiempo.","La respuesta correcta era Mobs!")) {
            assertTrue(GameDetector.winner(result),result);assertTrue(GameDetector.detect(result,true).isEmpty(),result);
        }
        assertEquals("Creeper",GameDetector.announcedAnswer("Marco035xd has correctly unscrambled Creeper (3.0s) and got a prize!").orElseThrow());
        assertEquals("Vidrio",GameDetector.announcedAnswer("Marco035xd respondió correctamente: Vidrio.").orElseThrow());
        assertTrue(GameDetector.announcedAnswer("<Marco035xd> La respuesta es Casa").isEmpty());
    }

    @Test void bundledTriviaAndKnownMasksAreAvailableOnExistingInstallations() throws Exception {
        Path dir=temp.resolve("config/chatgames-universal-helper");Files.createDirectories(dir);
        Files.writeString(dir.resolve("config.json"),"{\"configRevision\":4,\"stats\":true,\"debug\":true,\"games\":{\"trivia\":false,\"math\":true}}");
        Files.writeString(dir.resolve("trivia.json"),"{\"Mi pregunta personal\":\"Mi respuesta\"}");
        Files.writeString(dir.resolve("word-cache.json"),"{\"f:_a_a\":\"Casa\",\"f:m_n_o\":\"Mundo\"}");
        ConfigManager config=new ConfigManager(temp);
        assertTrue(config.get().game("trivia"));assertFalse(config.get().stats);assertFalse(config.get().debug);
        TriviaSolver trivia=new TriviaSolver(config.dir());
        Map<String,String> expected=Map.of("¿En qué planeta vivimos?","Tierra","¿Cuántos lados tiene un dado? (número)","6","¿Qué bloque transparente se fabrica fundiendo arena?","Vidrio","¿Qué idioma se habla principalmente en Brasil?","Portugués","¿Qué mob verde explota cuando se acerca a un jugador?","Creeper","¿Qué objeto se necesita para encender un portal al Nether?","Mechero","¿Qué animal puede ser montado usando una silla?","Caballo");
        expected.forEach((q,a)->assertEquals(a,trivia.answer(q).orElseThrow(),q));
        assertEquals("Mi respuesta",trivia.answer("Mi pregunta personal").orElseThrow());
        assertTrue(trivia.answer("¿Pregunta desconocida que no existe?").isEmpty());
        WordSolver words=new WordSolver(config.dir());
        for(var entry:Map.of("__bs","Mobs","_a_a","Lava","_r_eper","Creeper","Dia_an_e","Diamante","__co","Pico").entrySet())assertEquals(entry.getValue(),words.fillout(entry.getKey()).orElseThrow());
        assertEquals("mundo",words.fillout("m_n_o").orElseThrow());
    }

    @Test void timeoutAnswerOnFollowingLineIsLearnedWithoutSendingIt() throws Exception {
        ConfigManager config=new ConfigManager(temp);config.get().delay.mode="fixed";config.get().delay.minMs=60000;
        try(GameManager manager=new GameManager(config,null,message->{})) {
            manager.onLog("Tienes 20 segundos para completar la palabra: `C_s_`");flush(manager);
            manager.onLog("Han pasado 20s! El juego de completar ha terminado!");
            manager.onLog("La respuesta correcta era Casa!");flush(manager);
            assertNull(manager.current());
            assertEquals("Casa",manager.solveLocal(GameType.FILLOUT,"C_s_").orElseThrow());
            manager.onLog("------------ TRIVIA ------------");manager.onLog("¿Un objeto nuevo de prueba?");flush(manager);
            manager.onLog("------------ ¡GANADOR! ------------");
            manager.onLog("Marco035xd respondió correctamente: Respuesta nueva.");flush(manager);
            assertNull(manager.current());
            assertEquals("Respuesta nueva",manager.solveLocal(GameType.TRIVIA,"¿Un objeto nuevo de prueba?").orElseThrow());
        }
    }
    private static void flush(GameManager manager) throws Exception {
        onQueue(manager,()->{var method=GameManager.class.getDeclaredMethod("flushNow");method.setAccessible(true);method.invoke(manager);return null;});
    }
    private static void barrier(GameManager manager) throws Exception {onQueue(manager,()->null);}
    private static <T> T onQueue(GameManager manager,Callable<T> action) throws Exception {
        var field=GameManager.class.getDeclaredField("queue");field.setAccessible(true);
        return ((ScheduledExecutorService)field.get(manager)).submit(action).get(15,TimeUnit.SECONDS);
    }
}
