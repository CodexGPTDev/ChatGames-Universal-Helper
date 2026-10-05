package dev.clares.chatgames.game;

import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.CommandDispatcher;
import dev.clares.chatgames.chat.AnswerSender;
import dev.clares.chatgames.command.*;
import dev.clares.chatgames.config.ConfigManager;
import dev.clares.chatgames.log.LogWatcher;
import dev.clares.chatgames.solver.*;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ServerLog107Test {
    @TempDir Path temp;
    record Round(String type,String question,String answer,String prompt,String style) { }

    @Test void confirmedCorpusIsAvailableAndMatchesTheObservedPrompts() throws Exception {
        ConfigManager config=new ConfigManager(temp);
        WordSolver words=new WordSolver(config.dir());TriviaSolver trivia=new TriviaSolver(config.dir());
        List<Round> rounds;
        try(var reader=new InputStreamReader(getClass().getResourceAsStream("/server-rounds-107.json"),StandardCharsets.UTF_8)) {
            rounds=ConfigManager.GSON.fromJson(reader,new TypeToken<List<Round>>(){}.getType());
        }
        assertEquals(85,rounds.stream().filter(r->r.type.equals("trivia")).map(Round::question).distinct().count());
        assertEquals(110,rounds.stream().filter(r->r.type.equals("fillout")).map(Round::question).distinct().count());
        for(Round r:rounds) {
            switch(r.type) {
                case "trivia" -> {
                    assertEquals(r.answer,trivia.answer(r.question).orElseThrow(),r.question);
                    String block="You have 20 seconds to answer:\n`"+r.question+"`";
                    var detected=GameDetector.detect(block,true).orElseThrow();
                    assertEquals(GameType.TRIVIA,detected.type());
                    assertEquals(r.question,detected.question());
                }
                case "fillout" -> {
                    String answer=words.fillout(r.question,r.style).orElseThrow();
                    if(r.question.equals("_a_a") && r.style.equals("es60"))assertTrue(Set.of("Caja","Cama").contains(answer));
                    else assertEquals(r.answer,answer,r.question);
                    assertTrue(WordSolver.matchesFillout(r.question,answer));
                    var detected=GameDetector.detect(r.prompt,true).orElseThrow();
                    assertEquals(GameType.FILLOUT,detected.type());assertEquals(r.question,detected.question());
                }
                case "unscramble" -> assertEquals(r.answer,words.unscramble(r.question).orElseThrow(),r.question);
            }
        }
        assertTrue(trivia.answer("¿Pregunta totalmente nueva sin respuesta conocida?").isEmpty());
    }

    @Test void wrappedAndUnpunctuatedTriviaUsesTheWholeQuestion() {
        for(var pair:Map.of(
            "You have 20 seconds to answer:\n`How many eyes of ender are needed to open the end\nportal?`","How many eyes of ender are needed to open the end portal?",
            "You have 20 seconds to answer:\n`How many wooden planks does it take to craft 16 crafting\ntables?`","How many wooden planks does it take to craft 16 crafting tables?",
            "You have 20 seconds to answer:\n`What item is dropped when a turtle dies by lightning`","What item is dropped when a turtle dies by lightning").entrySet()) {
            var detected=GameDetector.detect(pair.getKey(),true).orElseThrow();
            assertEquals(GameType.TRIVIA,detected.type());assertEquals(pair.getValue(),detected.question());
        }
    }

    @Test void multiwordFilloutAndContextAvoidTruncationAndWrongCachedAnswers() throws Exception {
        ConfigManager config=new ConfigManager(temp);
        Files.writeString(config.dir().resolve("word-cache.json"),"{\"f:l_v_r\":\"Lavar\",\"f:_a_a\":\"Casa\"}");
        WordSolver words=new WordSolver(config.dir());
        assertEquals("Lever",words.fillout("L_v_r","en20").orElseThrow());
        assertEquals("Lava",words.fillout("_a_a","es20").orElseThrow());
        assertEquals("Caja",words.fillout("_a_a","es60").orElseThrow());
        assertEquals("Caja registradora",words.fillout("_aja _e_is___dora ","es60").orElseThrow());
        assertTrue(words.rememberFillout("_a_a","Cama","es60"));
        assertEquals("Cama",new WordSolver(config.dir()).fillout("_a_a","es60").orElseThrow());
        assertEquals("Lava",new WordSolver(config.dir()).fillout("_a_a","es20").orElseThrow());
    }

    @Test void upgradePreservesOffAndModePreferencesAndRepairsOnlyKnownCacheEntries() throws Exception {
        Path dir=temp.resolve("config/chatgames-universal-helper");Files.createDirectories(dir);
        Files.writeString(dir.resolve("config.json"),"{\"configRevision\":6,\"enabled\":false,\"stats\":true,\"games\":{\"trivia\":false,\"math\":true}}");
        Files.writeString(dir.resolve("trivia-cache.json"),"{\"What biome is home to bamboo?\":\"Wrong old answer\",\"Pregunta personal\":\"Personal\"}");
        Files.writeString(dir.resolve("word-cache.json"),"{\"f:l_v_r\":\"Lavar\",\"f:m_n_o\":\"Mundo\"}");
        ConfigManager config=new ConfigManager(temp);
        assertFalse(config.get().enabled);assertFalse(config.get().game("trivia"));assertTrue(config.get().stats);
        assertEquals("Jungle",new TriviaSolver(config.dir()).answer("What biome is home to bamboo?").orElseThrow());
        assertEquals("Personal",new TriviaSolver(config.dir()).answer("Pregunta personal").orElseThrow());
        assertEquals("Lever",new WordSolver(config.dir()).fillout("L_v_r").orElseThrow());
        assertEquals("mundo",new WordSolver(config.dir()).fillout("m_n_o").orElseThrow());
        new TriviaSolver(config.dir()).put("What biome is home to bamboo?","Nueva del servidor");
        assertEquals("Nueva del servidor",new TriviaSolver(new ConfigManager(temp).dir()).answer("What biome is home to bamboo?").orElseThrow());
    }

    @Test void offCommandCancelsAlreadyQueuedClientCallbackAndDoesNotResumeItOnOn() throws Exception {
        ConfigManager config=new ConfigManager(temp);config.get().delay.mode="fixed";config.get().delay.minMs=100;
        BlockingQueue<Runnable> callbacks=new LinkedBlockingQueue<>();List<String> sent=new CopyOnWriteArrayList<>(),notices=new CopyOnWriteArrayList<>();
        try(GameManager manager=new GameManager(config,notices::add,callbacks::add,()->true,sender(sent))) {
            CommandDispatcher<Object> dispatcher=commands(config,manager,notices);
            manager.onLog("You have 20 seconds to write out the word: `Spyglass`");flush(manager);
            Runnable old=callbacks.poll(5,TimeUnit.SECONDS);assertNotNull(old);
            dispatcher.execute("cgedit off",new Object());
            assertFalse(config.get().enabled);assertNull(manager.current());
            manager.onLog("You have 20 seconds to write out the word: `Rabbit`");manager.onRich(Text.literal("You have 20 seconds to write out the word: `Rabbit`"));barrier(manager);
            assertNull(manager.current());
            dispatcher.execute("cgedit on",new Object());barrier(manager);old.run();
            assertTrue(sent.isEmpty(),"A callback from before off/on was sent");
            assertTrue(config.get().enabled);
            manager.onLog("You have 20 seconds to write out the word: `Froglight`");flush(manager);
            Runnable fresh=callbacks.poll(5,TimeUnit.SECONDS);assertNotNull(fresh);fresh.run();
            assertEquals(List.of("Froglight"),sent);
            dispatcher.execute("cgedit off",new Object());
            assertFalse(new ConfigManager(temp).get().enabled,"Global off must survive restart");
        }
    }

    @Test void offCancelsSchedulerAndOldIncomingMessagesAndPerModeOffCancelsItsSend() throws Exception {
        ConfigManager config=new ConfigManager(temp);config.get().delay.mode="fixed";config.get().delay.minMs=100;
        BlockingQueue<Runnable> callbacks=new LinkedBlockingQueue<>();List<String> sent=new CopyOnWriteArrayList<>();
        try(GameManager manager=new GameManager(config,s->{},callbacks::add,()->true,sender(sent))) {
            manager.onLog("You have 20 seconds to write out the word: `Spyglass`");flush(manager);
            manager.setEnabled(false);barrier(manager);
            assertNull(callbacks.poll(300,TimeUnit.MILLISECONDS),"Cancelled scheduler still dispatched");
            manager.setEnabled(true);barrier(manager);
            var queue=queue(manager);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);
            queue.submit(()->{entered.countDown();release.await(5,TimeUnit.SECONDS);return null;});assertTrue(entered.await(5,TimeUnit.SECONDS));
            manager.onLog("You have 20 seconds to write out the word: `Rabbit`");
            manager.setEnabled(false);manager.setEnabled(true);release.countDown();barrier(manager);flush(manager);
            assertNull(manager.current());assertNull(callbacks.poll(300,TimeUnit.MILLISECONDS));
            manager.onLog("You have 20 seconds to unscramble: `uMics icsD`");flush(manager);
            Runnable old=callbacks.poll(5,TimeUnit.SECONDS);assertNotNull(old);
            manager.setGameEnabled(GameType.UNSCRAMBLE,false);manager.setGameEnabled(GameType.UNSCRAMBLE,true);barrier(manager);old.run();
            assertTrue(sent.isEmpty(),"Per-mode off/on resumed an old answer");
            manager.onLog("You have 20 seconds to write out the word: `Rabbit`");flush(manager);
            Runnable fresh=callbacks.poll(5,TimeUnit.SECONDS);assertNotNull(fresh);fresh.run();assertEquals(List.of("Rabbit"),sent);
        }
    }

    @Test void statsOffOnlyHidesNoticesAndExplainsTheOffCommand() throws Exception {
        ConfigManager config=new ConfigManager(temp);config.get().delay.mode="fixed";config.get().delay.minMs=100;config.get().stats=true;
        BlockingQueue<Runnable> callbacks=new LinkedBlockingQueue<>();List<String> sent=new CopyOnWriteArrayList<>(),notices=new CopyOnWriteArrayList<>();
        try(GameManager manager=new GameManager(config,notices::add,callbacks::add,()->true,sender(sent))) {
            var dispatcher=commands(config,manager,notices);dispatcher.execute("cgedit stats off",new Object());
            assertFalse(config.get().stats);assertTrue(config.get().enabled);assertTrue(notices.getLast().contains("/cgedit off"));
            manager.onLog("You have 20 seconds to write out the word: `Spyglass`");flush(manager);
            var send=callbacks.poll(5,TimeUnit.SECONDS);assertNotNull(send);send.run();assertEquals(List.of("Spyglass"),sent);
            assertTrue(notices.stream().noneMatch(n->n.startsWith("Respuesta enviada")));
        }
    }

    @Test void fillAndVariableWinnerMessagesCancelTheirQueuedResponses() throws Exception {
        ConfigManager config=new ConfigManager(temp);config.get().delay.mode="fixed";config.get().delay.minMs=100;
        BlockingQueue<Runnable> callbacks=new LinkedBlockingQueue<>();List<String> sent=new CopyOnWriteArrayList<>();
        try(GameManager manager=new GameManager(config,s->{},callbacks::add,()->true,sender(sent))) {
            manager.onLog("You have 20 seconds to fill in the word: `B_t_on`");flush(manager);
            Runnable old=callbacks.poll(5,TimeUnit.SECONDS);assertNotNull(old);
            String result="Marco035xd was the fastest to fill `Button` (3.0s) and got a prize!";
            assertTrue(GameDetector.winner(result));manager.onRich(Text.literal(result));manager.onLog(result);barrier(manager);old.run();
            assertNull(manager.current());assertTrue(sent.isEmpty());
            assertTrue(GameDetector.winner("Marco035xd was the fastest to get `54` (3.68s) and got a prize!"));
        }
    }

    private static AnswerSender sender(List<String> sent) {return new AnswerSender() {
        public boolean send(String answer){sent.add(answer);return true;}
        public boolean clickCommand(String command){sent.add("/"+command);return true;}
    };}
    private CommandDispatcher<Object> commands(ConfigManager config,GameManager manager,List<String> notices) throws Exception {
        var commands=new ClientCommands(config,manager,new LogWatcher(temp.resolve("latest.log"),s->{}),notices::add,t->{});
        var run=ClientCommands.class.getDeclaredMethod("run",String.class);run.setAccessible(true);
        CommandDispatcher<Object> dispatcher=new CommandDispatcher<>();dispatcher.register(CommandTree.build(command->{
            try{return (int)run.invoke(commands,command);}catch(Exception e){throw new IllegalStateException(e);}
        }));return dispatcher;
    }
    private static ScheduledExecutorService queue(GameManager manager) throws Exception {
        var field=GameManager.class.getDeclaredField("queue");field.setAccessible(true);return (ScheduledExecutorService)field.get(manager);
    }
    private static void barrier(GameManager manager) throws Exception {queue(manager).submit(()->{}).get(15,TimeUnit.SECONDS);}
    private static void flush(GameManager manager) throws Exception {
        queue(manager).submit(()->{var method=GameManager.class.getDeclaredMethod("flushNow");method.setAccessible(true);method.invoke(manager);return null;}).get(15,TimeUnit.SECONDS);
    }
}
