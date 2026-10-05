package dev.clares.chatgames.game;
import java.util.ArrayList;
import java.util.List;
public final class ChallengeBuffer {
    private final List<String> lines = new ArrayList<>();
    private long start = System.currentTimeMillis(), last = start;
    public void add(String line) { if (lines.size() < 25) lines.add(line); last = System.currentTimeMillis(); }
    public String text() { return String.join("\n", lines); }
    public String last() { return lines.isEmpty() ? "" : lines.getLast(); }
    public boolean ready(long now) { return !lines.isEmpty() && (now - last > 350 || now - start > 3000); }
    public int size() { return lines.size(); }
}
