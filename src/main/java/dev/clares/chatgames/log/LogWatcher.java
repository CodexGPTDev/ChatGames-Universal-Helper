package dev.clares.chatgames.log;

import java.io.ByteArrayOutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.function.Consumer;

public final class LogWatcher implements AutoCloseable {
    private final Path path;
    private final Consumer<String> listener;
    private volatile boolean running, watching;
    private volatile String lastChat = "";
    private Thread thread;
    public LogWatcher(Path path, Consumer<String> listener) { this.path = path; this.listener = listener; }
    public Path path() { return path; }
    public boolean watching() { return watching; }
    public String lastChat() { return lastChat; }
    public void start() {
        running = true;
        thread = new Thread(this::loop, "ChatGames-log-watcher");
        thread.setDaemon(true); thread.start();
    }
    private void loop() {
        Object key = null;
        boolean opened = false;
        long position = 0;
        ByteArrayOutputStream pending = new ByteArrayOutputStream(512);
        while (running) {
            try {
                if (!Files.isRegularFile(path)) { key = null; opened = false; position = 0; pending.reset(); watching = false; Thread.sleep(250); continue; }
                Object nextKey = Files.readAttributes(path, java.nio.file.attribute.BasicFileAttributes.class).fileKey();
                long length = Files.size(path);
                if (!opened || !java.util.Objects.equals(key,nextKey) || length < position) { key = nextKey; opened = true; position = length; pending.reset(); }
                watching = true;
                if (length > position) {
                    try (RandomAccessFile raf = new RandomAccessFile(path.toFile(), "r")) {
                        raf.seek(position);
                        byte[] bytes = new byte[8192];
                        while (raf.getFilePointer() < length) {
                            int n = raf.read(bytes, 0, (int)Math.min(bytes.length, length - raf.getFilePointer()));
                            if (n < 0) break;
                            for (int i=0; i<n; i++) {
                                if (bytes[i] == '\n') {
                                    String raw = pending.toString(StandardCharsets.UTF_8).replaceAll("\\r$", ""); pending.reset();
                                    LogChatParser.parse(raw).ifPresent(msg -> { lastChat = msg; listener.accept(msg); });
                                } else if (pending.size() < 65536) pending.write(bytes[i]); else pending.reset();
                            }
                        }
                        position = raf.getFilePointer();
                    }
                }
                Thread.sleep(100);
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
              catch (Exception e) { watching = false; try { Thread.sleep(500); } catch (InterruptedException x) { Thread.currentThread().interrupt(); break; } }
        }
    }
    @Override public void close() { running = false; if (thread != null) thread.interrupt(); }
}
