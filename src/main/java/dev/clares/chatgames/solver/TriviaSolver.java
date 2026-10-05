package dev.clares.chatgames.solver;
import com.google.gson.reflect.TypeToken;
import dev.clares.chatgames.config.ConfigManager;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.InputStreamReader;
import java.util.*;

public final class TriviaSolver {
    private final Map<String,String> answers = new HashMap<>(), cache = new HashMap<>();
    private final Path path;
    public TriviaSolver(Path dir) {
        path = dir.resolve("trivia-cache.json");
        // Bundled answers also reach installations that already have trivia.json.
        try (var resource=TriviaSolver.class.getResourceAsStream("/trivia-known-es.json")) {
            if (resource!=null) {
                Map<String,String> known=ConfigManager.GSON.fromJson(new InputStreamReader(resource,StandardCharsets.UTF_8),new TypeToken<Map<String,String>>(){}.getType());
                if (known!=null) known.forEach((q,a)->{if(validAnswer(q,a)) answers.put(normalize(q),a.strip());});
            }
        } catch (Exception ignored) { }
        for (String file : List.of("trivia.json","trivia-cache.json")) {
            try {
                Map<String,String> data = ConfigManager.GSON.fromJson(Files.readString(dir.resolve(file)),new TypeToken<Map<String,String>>(){}.getType());
                if (data != null) data.forEach((q,a) -> {
                    if (validAnswer(q,a)) (file.equals("trivia.json") ? answers : cache).put(normalize(q),a.strip());
                });
            } catch (Exception ignored) { }
        }
    }
    public static String normalize(String q) { return WordSolver.normalize(q).replaceAll("\\s+", ""); }
    public static boolean validAnswer(String question,String answer) {
        if (question==null || answer==null || answer.isBlank() || answer.length()>80 || answer.contains("\n") || answer.contains("?") || answer.contains("¿")) return false;
        String cleaned=normalize(answer);
        return !cleaned.isEmpty() && !cleaned.equals(normalize(question));
    }
    public synchronized Optional<String> answer(String q) { return Optional.ofNullable(cache.getOrDefault(normalize(q), answers.get(normalize(q)))); }
    public synchronized void put(String q,String a) { if (!validAnswer(q,a)) return; cache.put(normalize(q),a.strip()); try { Files.writeString(path,ConfigManager.GSON.toJson(cache)); } catch(Exception ignored) { } }
    public synchronized int cacheSize() { return cache.size(); }
}
