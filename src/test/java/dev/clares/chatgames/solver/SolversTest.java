package dev.clares.chatgames.solver;
import dev.clares.chatgames.config.ConfigManager;
import dev.clares.chatgames.game.*;
import dev.clares.chatgames.log.LogChatParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class SolversTest {
    @TempDir Path temp;
    @Test void math() {
        assertEquals("132",MathParser.solve("38 + 94"));
        assertEquals("794",MathParser.solve("792 + 2"));
        assertEquals("3572",MathParser.solve("38 × 94"));
        assertEquals("782",MathParser.solve("34 x 23"));
        assertEquals("782",MathParser.solve("34X23"));
        assertEquals("782",MathParser.solve("34 · 23"));
        assertEquals("12",MathParser.solve("144 ÷ 12"));
        assertEquals("23",MathParser.solve("3 + 4 * 5"));
        assertEquals("35",MathParser.solve("(3 + 4) * 5"));
        assertEquals("15",MathParser.solve("-10 + 25"));
        assertEquals("11",MathParser.solve("5.5 * 2"));
        assertThrows(IllegalArgumentException.class,()->MathParser.solve("hello"));
    }
    @Test void wordsAndTrivia() throws Exception {
        ConfigManager c=new ConfigManager(temp);
        WordSolver words=new WordSolver(c.dir());TriviaSolver trivia=new TriviaSolver(c.dir());
        assertEquals("Satélite",words.unscramble("talétieS").orElseThrow());
        assertTrue(words.rememberUnscramble("talétieS","Satélite"));
        assertEquals("Satélite",new WordSolver(c.dir()).unscramble("talétieS").orElseThrow());
        // A preexisting user dictionary must still get the new bundled vocabulary.
        java.nio.file.Files.writeString(c.dir().resolve("words-es.txt"),"palabra\n");
        java.nio.file.Files.writeString(c.dir().resolve("word-cache.json"),"{}\n");
        assertEquals("Satélite",new WordSolver(c.dir()).unscramble("talétieS").orElseThrow());
        assertEquals("Vestíbulo",new WordSolver(c.dir()).fillout("V____bulo").orElseThrow());
        assertEquals("Vacábulo",words.fillout("Va__bulo").orElseThrow());
        assertTrue(words.rememberFillout("V____bulo","Vestíbulo"));
        assertFalse(words.rememberFillout("V____bulo","Vocábulo"));
        assertEquals("Vestíbulo",new WordSolver(c.dir()).fillout("V____bulo").orElseThrow());
        assertEquals("Berlín",trivia.answer("cual es la capital de alemania").orElseThrow());
        assertEquals("Azul",trivia.answer("¿Qué color tiene el cielo en un día despejado?").orElseThrow());
        String question="¿Cuál es la capital de Alemania?";
        trivia.put(question,question);
        assertEquals("Berlín",new TriviaSolver(c.dir()).answer(question).orElseThrow());
        trivia.put(question,"Berlín");
        assertEquals("Berlín",new TriviaSolver(c.dir()).answer(question).orElseThrow());
        assertFalse(TriviaSolver.validAnswer(question,question));
    }
    @Test void detectionAndLog() {
        String line="[19:08:15] [Render thread/INFO]:[System] [CHAT] ¿Cuál es la capital de Alemania?";
        assertEquals("¿Cuál es la capital de Alemania?",LogChatParser.parse(line).orElseThrow());
        assertTrue(LogChatParser.parse("[19:08:15] [Render thread/INFO]:[System] [CHAT] [CGH] Status").isEmpty());
        var d=GameDetector.detect("✯ MINIJUEGO ✯\nTienes 60 segundos para responder\n38 + 94",true).orElseThrow();
        assertEquals(GameType.MATH,d.type());assertEquals("132",MathParser.solve(d.question()));
        assertEquals(GameType.REACTION,GameDetector.detect("Tienes 60 segundos para Escribir la palabra: `Cosmos`",true).orElseThrow().type());
        assertEquals("Cosmos",GameDetector.detect("Tienes 60 segundos para Escribir la palabra: `Cosmos`",true).orElseThrow().question());
        assertEquals(GameType.UNSCRAMBLE,GameDetector.detect("Tienes 60 segundos para descifrarlo `talétieS`",true).orElseThrow().type());
        assertEquals(GameType.TRIVIA,GameDetector.detect("✯ MINIJUEGO ✯\n¿Cuál es la capital de Alemania?",true).orElseThrow().type());
        assertEquals(GameType.UNSCRAMBLE,GameDetector.detect("✯ MINIJUEGO ✯\nDescifra la palabra\ntalétieS",true).orElseThrow().type());
        for(String challenge : java.util.List.of("34 × 23","34 x 23","34X23","34 * 23","34 · 23")) {
            var math=GameDetector.detect("✯ MINIJUEGO ✯\nTienes 60 segundos para responder\n"+challenge,true).orElseThrow();
            assertEquals(GameType.MATH,math.type(),challenge);
            assertEquals("782",MathParser.solve(math.question()),challenge);
        }
        assertEquals(GameType.VARIABLE,GameDetector.detect("✯ MINIJUEGO ✯\n2x = 30",true).orElseThrow().type());
        assertEquals(GameType.FILLOUT,GameDetector.detect("✯ MINIJUEGO ✯\nCompleta la palabra `V____bulo`, Tienes 60 segundos",true).orElseThrow().type());
        assertEquals("V____bulo",GameDetector.detect("✯ MINIJUEGO ✯\nCompleta la palabra `V____bulo`, Tienes 60 segundos",true).orElseThrow().question());
        var question=GameDetector.detect("✯ MINIJUEGO ✯\nTienes 60 segundos para responder\n¿Qué color tiene el cielo en un día despejado?",true).orElseThrow();
        assertEquals(GameType.TRIVIA,question.type());
        assertEquals("Azul",new TriviaSolver(new ConfigManager(temp).dir()).answer(question.question()).orElseThrow());
        assertEquals(GameType.VARIABLE,GameDetector.detect("✯ MINIJUEGO ✯\n20 + x = 25",true).orElseThrow().type());
        assertEquals("5",VariableSolver.solve(GameDetector.detect("✯ MINIJUEGO ✯\n20 + x = 25",true).orElseThrow().question()).orElseThrow());
        assertTrue(GameDetector.detect("✓ ✯ CHAT GAMES ✯ ✓\nYou have 20 seconds to solve for: `✗`",true).isEmpty());
        assertTrue(GameDetector.detect("✓ ✯ CHAT GAMES ✯ ✓\nYou have 20 seconds to write\ngame",true).isEmpty());
        assertTrue(GameDetector.detect("✯ CHAT GAMES ✯\nWhat is the color of the sky?",true).filter(detected->detected.type()==GameType.TRIVIA).isPresent());
        assertTrue(GameDetector.winner("¡Papas1000 respondió primero `794` y fue recompensado!"));
        assertTrue(GameDetector.detect("vendo 32 diamantes por 5k",false).isEmpty());
    }
    @Test void variable() {
        assertEquals("15",VariableSolver.solve("x + 5 = 20").orElseThrow());
        assertEquals("15",VariableSolver.solve("2x = 30").orElseThrow());
        assertEquals("5",VariableSolver.solve("3x + 2 = 17").orElseThrow());
        assertEquals("16",VariableSolver.solve("20 - x = 4").orElseThrow());
        assertEquals("5",VariableSolver.solve("20 + x = 25").orElseThrow());
        assertEquals("5",VariableSolver.solve("25 = 20 + x").orElseThrow());
        assertEquals("5",VariableSolver.solve("2 × x + 5 = 15").orElseThrow());
        assertEquals("5",VariableSolver.solve("3x + 2 = x + 12").orElseThrow());
        assertTrue(VariableSolver.solve("34 x 23 = 782").isEmpty());
    }
    @Test void multilineSymbolVariable() {
        String block="✓ ✯ CHAT GAMES ✯ ✓\nYou have 20 seconds to solve for: `✗`\n"
                +"✯ + ✯ + ✯ = 30\n❅ + ❅ + ❅ = 60\n✯ + ❅ + ✗ = 32";
        assertEquals("2",SymbolVariableSolver.solve(block).orElseThrow());
        var detection=GameDetector.detect(block,true).orElseThrow();
        assertEquals(GameType.VARIABLE,detection.type());
        assertEquals("2",SymbolVariableSolver.solve(detection.question()).orElseThrow());
        assertTrue(SymbolVariableSolver.solve("You have 20 seconds to solve for: `✗`\n✯ + ✗ = 32").isEmpty());
    }
    @Test void aSessionCanClaimEachScheduledResponseOnlyOnce() {
        GameSession session=new GameSession();
        long first=session.prepareSend(System.currentTimeMillis()+2000);
        assertTrue(session.claimSend(first));
        assertFalse(session.claimSend(first));
        session.finishSend(first,true);
        assertEquals(GameState.ANSWERED,session.status);
        long second=session.prepareSend(System.currentTimeMillis()+2000); // GUESS_THE_NUMBER can make another guess.
        assertFalse(session.claimSend(first));
        assertTrue(session.claimSend(second));
        session.cancelSend();
        assertFalse(session.claimSend(second));
    }
}
