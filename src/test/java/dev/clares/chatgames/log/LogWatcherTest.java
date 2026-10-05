package dev.clares.chatgames.log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
class LogWatcherTest {
    @TempDir Path temp;
    @Test void followsOnlyNewUtf8LinesAndRecoversFromReplacement() throws Exception {
        Path path=temp.resolve("latest.log");
        Files.writeString(path,"[19:00:00] [Render thread/INFO]: [System] [CHAT] OLD\n");
        BlockingQueue<String> messages=new LinkedBlockingQueue<>();
        try(LogWatcher watcher=new LogWatcher(path,messages::add)) {
            watcher.start();
            long until=System.currentTimeMillis()+3000;
            while(!watcher.watching() && System.currentTimeMillis()<until) Thread.sleep(25);
            assertTrue(watcher.watching());
            Files.writeString(path,"[19:00:01] [Render thread/INFO]: [System] [CHAT] ¿Cuál es la capital de Alemania?\n",StandardCharsets.UTF_8,StandardOpenOption.APPEND);
            assertEquals("¿Cuál es la capital de Alemania?",messages.poll(3,TimeUnit.SECONDS));
            assertFalse(messages.contains("OLD"));
            Path replacement=temp.resolve("replacement.log");
            Files.writeString(replacement,"[19:00:02] [Render thread/INFO]: [System] [CHAT] HISTORY\n");
            Files.move(replacement,path,StandardCopyOption.REPLACE_EXISTING);
            Thread.sleep(400);
            Files.writeString(path,"[19:00:03] [Render thread/INFO]: [System] [CHAT] NEW\n",StandardOpenOption.APPEND);
            assertEquals("NEW",messages.poll(3,TimeUnit.SECONDS));
        }
    }
}
