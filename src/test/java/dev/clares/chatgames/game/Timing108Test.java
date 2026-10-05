package dev.clares.chatgames.game;

import com.mojang.brigadier.CommandDispatcher;
import dev.clares.chatgames.chat.AnswerSender;
import dev.clares.chatgames.command.*;
import dev.clares.chatgames.config.ConfigManager;
import dev.clares.chatgames.log.LogWatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class Timing108Test {
    @TempDir Path temp;

    @Test void mathAndSymbolEquationsReallyWaitForTheirOwnIntervalsBeforeSending() throws Exception {
        ConfigManager config=new ConfigManager(temp);config.get().setDelay("math",dev.clares.chatgames.config.ModConfig.Delay.random(2000,3000));config.get().setDelay("variable",dev.clares.chatgames.config.ModConfig.Delay.random(3000,5000));config.get().delay.mode="fixed";config.get().delay.minMs=100;
        BlockingQueue<Runnable> callbacks=new LinkedBlockingQueue<>();List<String> sent=new CopyOnWriteArrayList<>();
        AnswerSender sender=new AnswerSender() {
            public boolean send(String answer) {sent.add(answer);return true;}
            public boolean clickCommand(String command) {fail("Unexpected command");return false;}
        };
        try(GameManager manager=new GameManager(config,s->{},callbacks::add,()->true,sender)) {
            manager.onLog("You have 20 seconds to solve: `6 × 7`");flush(manager);
            assertEquals(GameType.MATH,manager.current().type);
            assertNull(callbacks.poll(1500,TimeUnit.MILLISECONDS),"MATH used the 100ms general time instead of 2–3s");
            Runnable math=callbacks.poll(2500,TimeUnit.MILLISECONDS);assertNotNull(math);math.run();assertEquals(List.of("42"),sent);
            manager.onLog("Marco035xd was the fastest to get `42` (2.7s) and got a prize!");
            manager.onLog("You have 20 seconds to solve for: `✗`");
            manager.onLog("❅ + ❅ + ❅ = 60");manager.onLog("✯ + ✯ + ✯ = 30");manager.onLog("❅ + ✯ + ✗ = 96");flush(manager);
            assertEquals(GameType.VARIABLE,manager.current().type);
            assertNull(callbacks.poll(2500,TimeUnit.MILLISECONDS),"VARIABLE used the general time instead of 3–5s");
            Runnable variable=callbacks.poll(3500,TimeUnit.MILLISECONDS);assertNotNull(variable);variable.run();assertEquals(List.of("42","66"),sent);
            manager.onLog("Marco035xd was the fastest to get `66` (4.4s) and got a prize!");flush(manager);
            assertNull(manager.current());assertTrue(callbacks.isEmpty());
        }
    }

    @Test void realClientCommandUpdatesTheNextMathIntervalOnly() throws Exception {
        ConfigManager config=new ConfigManager(temp);BlockingQueue<Runnable> callbacks=new LinkedBlockingQueue<>();List<String> sent=new CopyOnWriteArrayList<>(),feedback=new ArrayList<>();
        AnswerSender sender=new AnswerSender() {
            public boolean send(String answer){sent.add(answer);return true;}
            public boolean clickCommand(String command){return false;}
        };
        try(GameManager manager=new GameManager(config,feedback::add,callbacks::add,()->true,sender)) {
            var commands=new ClientCommands(config,manager,new LogWatcher(temp.resolve("latest.log"),s->{}),feedback::add,t->{});
            var method=ClientCommands.class.getDeclaredMethod("run",String.class);method.setAccessible(true);
            CommandDispatcher<Object> dispatcher=new CommandDispatcher<>();dispatcher.register(CommandTree.build(command->{
                try{return (int)method.invoke(commands,command);}catch(Exception e){throw new IllegalStateException(e);}
            }));
            dispatcher.execute("cgedit settime math random 100ms 200ms",new Object());
            assertTrue(feedback.getLast().contains("MATH"));
            manager.onLog("You have 20 seconds to solve: `12 - 5`");flush(manager);
            Runnable send=callbacks.poll(1,TimeUnit.SECONDS);assertNotNull(send);send.run();assertEquals(List.of("7"),sent);
            assertEquals(0,config.get().delayFor("variable").minMs);
            dispatcher.execute("cgedit settime",new Object());
            assertTrue(feedback.stream().anyMatch(s->s.contains("general") && s.contains("0 s–1 s")));
        }
    }
    private static void flush(GameManager manager) throws Exception {
        var field=GameManager.class.getDeclaredField("queue");field.setAccessible(true);
        ((ScheduledExecutorService)field.get(manager)).submit(()->{
            var method=GameManager.class.getDeclaredMethod("flushNow");method.setAccessible(true);method.invoke(manager);return null;
        }).get(15,TimeUnit.SECONDS);
    }
}
