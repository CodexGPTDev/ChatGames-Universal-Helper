package dev.clares.chatgames.game;

import dev.clares.chatgames.config.ConfigManager;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class GameLifecycleTest {
    @TempDir Path temp;

    @Test void winnerFromRichChatCancelsPendingAnswerAndCannotStartAHoverReply() throws Exception {
        ConfigManager config=new ConfigManager(temp);
        config.get().delay.mode="fixed";config.get().delay.minMs=60000;
        try (GameManager manager=new GameManager(config,null,message -> {})) {
            manager.onLog("You have 20 seconds to unscramble: `attocarreT`");
            runOnQueue(manager,()->{
                var flush=GameManager.class.getDeclaredMethod("flushNow");flush.setAccessible(true);flush.invoke(manager);
                return null;
            });
            GameSession session=manager.current();
            assertNotNull(session);
            assertEquals("Terracotta",session.answer);
            assertEquals(GameState.WAITING,session.status);
            manager.onRich(Text.literal("Clares was the first to type `attocarreT` and received 10 points!")
                .styled(style -> style.withHoverEvent(new HoverEvent.ShowText(Text.literal("attocarreT")))));
            runOnQueue(manager,()->null);
            assertNull(manager.current());
            assertEquals(GameState.CANCELLED,session.status);
            manager.onLog("Clares was the first to type `attocarreT` and received 10 points!");
            manager.onLog("[CHAT GAMES] You have started a unscramble chat event in your chat!");
            runOnQueue(manager,()->{
                var flush=GameManager.class.getDeclaredMethod("flushNow");flush.setAccessible(true);flush.invoke(manager);
                return null;
            });
            assertNull(manager.current());
            assertSame(session,manager.last());
        }
    }

    private static <T> T runOnQueue(GameManager manager,Callable<T> action) throws Exception {
        var field=GameManager.class.getDeclaredField("queue");field.setAccessible(true);
        return ((ScheduledExecutorService)field.get(manager)).submit(action).get(15,TimeUnit.SECONDS);
    }
}
