package dev.clares.chatgames.solver;

import dev.clares.chatgames.command.HelpPages;
import dev.clares.chatgames.config.ConfigManager;
import dev.clares.chatgames.game.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class Update104Test {
    @TempDir Path temp;

    @Test void multiwordUnscramblePreservesSpacesAndCapitalization() {
        ConfigManager config=new ConfigManager(temp);
        WordSolver solver=new WordSolver(config.dir());
        var detection=GameDetector.detect("You have 20 seconds to unscramble: `uMics icsD`",true).orElseThrow();
        assertEquals(GameType.UNSCRAMBLE,detection.type());
        assertEquals("Music Disc",solver.unscramble(detection.question()).orElseThrow());
        assertEquals("Music Disc",new WordSolver(config.dir()).unscramble("uMics icsD").orElseThrow());
        assertEquals("Terracotta",solver.unscramble("attocarreT").orElseThrow());
        assertTrue(WordSolver.matchesUnscramble("uMics icsD","Music Disc"));
        assertFalse(WordSolver.matchesUnscramble("uMics icsD","MusicDisc"));
        assertFalse(WordSolver.matchesUnscramble("uMics icsD","Disc Music"));
    }

    @Test void userPhrasesAndLearnedAnswersKeepWordBoundaries() throws Exception {
        ConfigManager config=new ConfigManager(temp);
        Files.writeString(config.dir().resolve("words-en.txt"),"Music Disc\n");
        WordSolver solver=new WordSolver(config.dir());
        assertEquals("Music Disc",solver.unscramble("uMics icsD").orElseThrow());
        assertTrue(solver.rememberUnscramble("uMics icsD","Music Disc"));
        assertFalse(solver.rememberUnscramble("uMics icsD","MusicDisc"));
        assertFalse(solver.rememberUnscramble("attocarreT","attocarreT"));
    }

    @Test void cachedScrambledPromptIsNotUsedAsTheAnswer() throws Exception {
        ConfigManager config=new ConfigManager(temp);
        Files.writeString(config.dir().resolve("word-cache.json"),"{\"u:attocarret\":\"attocarreT\",\"u:umicsicsd\":\"MusicDisc\"}");
        WordSolver solver=new WordSolver(config.dir());
        assertEquals("Terracotta",solver.unscramble("attocarreT").orElseThrow());
        assertEquals("Music Disc",solver.unscramble("uMics icsD").orElseThrow());
    }

    @Test void resultAnnouncementsAndCommandConfirmationsNeverBecomeChallenges() {
        String admin="[CHAT GAMES] You have started a unscramble chat event in your chat!";
        assertTrue(GameDetector.administrative(admin));
        assertTrue(GameDetector.detect(admin,true).isEmpty());
        assertTrue(GameDetector.detect(admin,false).isEmpty());
        for (String result: List.of(
            "Clares was the first to type `attocarreT` and received 10 points!",
            "Clares was the first to unscramble `attocarreT`!",
            "Clares answered `Terracotta` correctly!",
            "Clares successfully unscrambled `attocarreT`!",
            "[CHAT GAMES] The game is now over! The word was `attocarreT`.",
            "Time's up! No one answered `attocarreT`.",
            "Clares respondió primero `Terracotta` y fue recompensado!")) {
            assertTrue(GameDetector.winner(result),result);
            assertTrue(GameDetector.detect(result,true).isEmpty(),result);
            assertTrue(GameDetector.detect("You have 20 seconds to unscramble: `attocarreT`\n"+result,true).isEmpty(),result);
        }
        assertFalse(GameDetector.winner("Be the first to type: `Cosmos`"));
        assertEquals(GameType.REACTION,GameDetector.detect("Be the first to type: `Cosmos`",true).orElseThrow().type());
        assertFalse(GameDetector.winner("You have 20 seconds to unscramble: `uMics icsD`"));
        assertEquals(GameType.REACTION,GameDetector.detect("You have 20 seconds to type: `Terracotta`",true).orElseThrow().type());
    }

    @Test void upgradeEnablesKnownTriviaSilencesStatsAndPreservesLaterToggles() throws Exception {
        Path dir=temp.resolve("config/chatgames-universal-helper");
        Files.createDirectories(dir);
        Files.writeString(dir.resolve("config.json"),"{\"games\":{\"trivia\":true,\"math\":true},\"stats\":true}");
        ConfigManager config=new ConfigManager(temp);
        assertTrue(config.get().game("trivia"));
        assertTrue(config.get().game("math"));
        assertFalse(config.get().stats);
        config.get().games.put("trivia",false);config.get().stats=true;config.save();
        assertFalse(new ConfigManager(temp).get().game("trivia"));
        assertTrue(new ConfigManager(temp).get().stats);
    }

    @Test void helpListsEveryCommandWithDescriptionInThreeShortPages() {
        assertEquals(3,HelpPages.count());
        var entries=java.util.stream.IntStream.rangeClosed(1,3).mapToObj(HelpPages::page).flatMap(page -> page.entries().stream()).toList();
        assertEquals(17,entries.size());
        assertTrue(entries.stream().allMatch(entry -> entry.command().startsWith("/cgedit ") && !entry.description().isBlank()));
        assertThrows(IllegalArgumentException.class,()->HelpPages.page(0));
        assertThrows(IllegalArgumentException.class,()->HelpPages.page(4));
    }
}
