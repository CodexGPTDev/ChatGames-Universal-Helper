package dev.clares.chatgames.game;

import dev.clares.chatgames.chat.AnswerSender;
import dev.clares.chatgames.command.TimeCommand;
import dev.clares.chatgames.config.ConfigManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class Timing109Test {
    @TempDir Path temp;

    @Test void zeroDelaySendsImmediatelyAfterSolvingAndWinnerPreventsDuplicates() throws Exception {
        ConfigManager config=new ConfigManager(temp);TimeCommand.execute(config,"0s");
        BlockingQueue<Runnable> callbacks=new LinkedBlockingQueue<>();List<String> sent=new CopyOnWriteArrayList<>();
        AnswerSender sender=new AnswerSender() {
            public boolean send(String answer){sent.add(answer);return true;}
            public boolean clickCommand(String command){fail("Unexpected command");return false;}
        };
        try(GameManager manager=new GameManager(config,s->{},callbacks::add,()->true,sender)) {
            // Finish loading the local word dictionary before measuring dispatch.
            assertEquals("Music Disc",manager.solveLocal(GameType.UNSCRAMBLE,"uMics icsD").orElseThrow());
            String[][] rounds={
                {"You have 20 seconds to solve: `6 × 7`","42"},
                {"You have 20 seconds to write out: `aJCOfw`","aJCOfw"},
                {"You have 20 seconds to unreverse: `ssalgypS`","Spyglass"},
                {"You have 20 seconds to unscramble: `uMics icsD`","Music Disc"},
                {"You have 20 seconds to fill in the word: `_abbi_`","Rabbit"},
                {"You have 20 seconds to solve for: `✗`\n✯ + ✯ + ✯ = 30\n❅ + ❅ + ❅ = 60\n✯ + ❅ + ✗ = 32","2"}
            };
            for(String[] round:rounds) {
                manager.onLog("[CHAT GAMES] You have started a random chat event in your chat!");
                for(String line:round[0].split("\n"))manager.onLog(line);
                flush(manager);
                assertEquals(0,config.get().delayFor(manager.current().type.name()).sampleMillis());
                Runnable callback=callbacks.poll(750,TimeUnit.MILLISECONDS);assertNotNull(callback,round[0]);
                callback.run();assertEquals(round[1],sent.getLast());int count=sent.size();
                manager.onLog("Marco035xd was the fastest to write `"+round[1]+"` (0.5s) and got a prize!");flush(manager);
                assertNull(manager.current());callback.run();assertEquals(count,sent.size());assertTrue(callbacks.isEmpty());
            }
            manager.onLog("You have 20 seconds to solve: `12 - 5`");flush(manager);
            Runnable cancelled=callbacks.poll(750,TimeUnit.MILLISECONDS);assertNotNull(cancelled);
            manager.setEnabled(false);cancelled.run();assertEquals(rounds.length,sent.size());
        }
    }

    @Test void numberHintsUseTheConfiguredZeroInterval() throws Exception {
        ConfigManager config=new ConfigManager(temp);TimeCommand.execute(config,"0s");
        BlockingQueue<Runnable> callbacks=new LinkedBlockingQueue<>();List<String> sent=new CopyOnWriteArrayList<>();
        AnswerSender sender=new AnswerSender() {
            public boolean send(String answer){sent.add(answer);return true;}
            public boolean clickCommand(String command){return false;}
        };
        try(GameManager manager=new GameManager(config,s->{},callbacks::add,()->true,sender)) {
            manager.onLog("Guess the number between 1 and 100");flush(manager);
            Runnable first=callbacks.poll(750,TimeUnit.MILLISECONDS);assertNotNull(first);first.run();
            manager.onLog("Too low! Guess higher!");flush(manager);
            Runnable next=callbacks.poll(750,TimeUnit.MILLISECONDS);assertNotNull(next,"Retry retained the previous 1s/2.5s minimum");next.run();
            assertEquals(List.of("50","75"),sent);
        }
    }

    private static void flush(GameManager manager) throws Exception {
        var field=GameManager.class.getDeclaredField("queue");field.setAccessible(true);
        ((ScheduledExecutorService)field.get(manager)).submit(()->{
            var method=GameManager.class.getDeclaredMethod("flushNow");method.setAccessible(true);method.invoke(manager);return null;
        }).get(15,TimeUnit.SECONDS);
    }
}
