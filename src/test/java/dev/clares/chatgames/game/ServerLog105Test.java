package dev.clares.chatgames.game;

import dev.clares.chatgames.config.ConfigManager;
import dev.clares.chatgames.log.LogChatParser;
import dev.clares.chatgames.solver.WordSolver;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ServerLog105Test {
    @TempDir Path temp;

    @Test void exactWinnerMessagesAreTerminalEvenWithPrizeOnNextLine() {
        for (String announcement:List.of(
                "Marco035xd was the fastest to write out `Spyglass` (3.04s)",
                "Marco035xd was the fastest to write `aJCOfw` (3.6s) and got")) {
            String logged="[09:55:45] [Render thread/INFO]:[System] [CHAT] "+announcement;
            String parsed=LogChatParser.parse(logged).orElseThrow();
            assertTrue(GameDetector.winner(parsed));
            assertTrue(GameDetector.detect(parsed,false).isEmpty());
            assertTrue(GameDetector.detect("✓ ✯ CHAT GAMES ✯ ✓\n"+parsed+"\nand got a prize!",true).isEmpty());
        }
        assertFalse(GameDetector.winner("Be the fastest to write out: `Spyglass`"));
        assertEquals("aJCOfw",GameDetector.detect("You have 20 seconds to write out: `aJCOfw`",true).orElseThrow().question());
    }

    @Test void completedUnreverseIsNotReopenedByEitherInputSource() throws Exception {
        for (boolean richFirst:List.of(false,true)) {
            ConfigManager config=config(temp.resolve(Boolean.toString(richFirst)));
            try (GameManager manager=new GameManager(config,null,message->{})) {
                open(manager,"You have 20 seconds to unreverse: `ssalgypS`");
                GameSession session=manager.current();
                assertEquals(GameType.UNREVERSE,session.type);
                assertEquals("Spyglass",session.answer);
                markSent(manager,session);
                String winner="Marco035xd was the fastest to write out `Spyglass` (3.04s)";
                Text rich=Text.literal(winner).styled(style->style.withHoverEvent(new HoverEvent.ShowText(Text.literal("Spyglass"))));
                manager.onLog("✓ ✯ CHAT GAMES ✯ ✓");
                if (richFirst) {manager.onRich(rich);manager.onLog(winner);}
                else {manager.onLog(winner);manager.onRich(rich);}
                manager.onLog("and got a prize!");
                flush(manager);
                assertNull(manager.current());
                assertSame(session,manager.last());
                assertEquals(GameState.CANCELLED,session.status);
                // The next real event is still handled with its original capitalization.
                manager.onLog("[CHAT GAMES] You have started a random chat event in your chat!");
                open(manager,"You have 20 seconds to write out: `aJCOfw`");
                assertEquals("aJCOfw",manager.current().answer);
                manager.onLog("Marco035xd was the fastest to write `aJCOfw` (3.6s) and got");
                manager.onLog("a prize!");flush(manager);
                assertNull(manager.current());
            }
        }
    }

    @Test void hiddenInitialUsesCapitalAndVisibleCaseIsPreservedWithOldCache() throws Exception {
        ConfigManager config=config(temp);
        Files.writeString(config.dir().resolve("word-cache.json"),"{\"f:_abbi_\":\"rabbit\"}");
        WordSolver solver=new WordSolver(config.dir());
        assertEquals("Rabbit",solver.fillout("_abbi_").orElseThrow());
        assertEquals("rabbit",solver.fillout("r_bbi_").orElseThrow());
        assertEquals("Rabbit",solver.fillout("R_bbi_").orElseThrow());
    }

    @Test void capitalizationHintSchedulesOnlyOneCorrectionAndTimeoutCancelsIt() throws Exception {
        ConfigManager config=config(temp);
        try (GameManager manager=new GameManager(config,null,message->{})) {
            open(manager,"You have 20 seconds to fill in the word: `_abbi_`");
            GameSession session=manager.current();
            assertEquals(GameType.FILLOUT,session.type);
            assertEquals("Rabbit",session.answer);
            // Reproduce the rejected 1.0.4 answer, already sent to the server.
            onQueue(manager,()->{session.answer="rabbit";return null;});
            markSent(manager,session);
            long original=session.scheduleVersion;
            String hint="[CHAT GAMES] Almost... Check your capitalization!";
            manager.onRich(Text.literal(hint));manager.onLog(hint);onQueue(manager,()->null);
            assertEquals("Rabbit",session.answer);
            assertTrue(session.capitalizationRetried);
            assertEquals(GameState.WAITING,session.status);
            assertEquals(original+1,session.scheduleVersion);
            manager.onLog("20s have passed! The fillout game is now over!");
            manager.onLog("The correct answer was Rabbit!");
            manager.onLog(hint);flush(manager);
            assertNull(manager.current());
            assertEquals(GameState.CANCELLED,session.status);
            assertFalse(session.claimSend(original+1));
        }
    }

    private ConfigManager config(Path dir) {
        ConfigManager config=new ConfigManager(dir);
        config.get().delay.mode="fixed";config.get().delay.minMs=60000;
        return config;
    }
    private static void open(GameManager manager,String prompt) throws Exception {manager.onLog(prompt);flush(manager);}
    private static void flush(GameManager manager) throws Exception {
        onQueue(manager,()->{var method=GameManager.class.getDeclaredMethod("flushNow");method.setAccessible(true);method.invoke(manager);return null;});
    }
    private static void markSent(GameManager manager,GameSession session) throws Exception {
        onQueue(manager,()->{assertTrue(session.claimSend(session.scheduleVersion));session.finishSend(session.scheduleVersion,true);return null;});
    }
    private static <T> T onQueue(GameManager manager,Callable<T> action) throws Exception {
        var field=GameManager.class.getDeclaredField("queue");field.setAccessible(true);
        return ((ScheduledExecutorService)field.get(manager)).submit(action).get(15,TimeUnit.SECONDS);
    }
}
