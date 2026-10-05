package dev.clares.chatgames.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Collections;
import dev.clares.chatgames.solver.TriviaSolver;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private final Path dir;
    private final Path path;
    private ModConfig config;
    public ConfigManager(Path gameDir) {
        dir = gameDir.resolve("config/chatgames-universal-helper"); path = dir.resolve("config.json");
        try {
            Files.createDirectories(dir);
            config = Files.exists(path) ? GSON.fromJson(Files.readString(path), ModConfig.class) : new ModConfig();
            if (config == null || config.delay == null || config.games == null || config.ai == null) config = new ModConfig();
            if(config.gameDelays==null)config.gameDelays=new LinkedHashMap<>();
            config.games=Collections.synchronizedMap(new LinkedHashMap<>(config.games));
            config.gameDelays=Collections.synchronizedMap(new LinkedHashMap<>(config.gameDelays));
            boolean update=config.configRevision<7;
            if (config.configRevision<6) {
                config.games.put("trivia",true);
                config.stats=false;
                config.debug=false;
            }
            seed("words-es.txt", "Satélite\nVacábulo\nMundo\nAlemania\nCosmos\nManzana\nComputadora\n");
            seed("words-en.txt", "satellite\nworld\ncomputer\napple\nplanet\n");
            seed("trivia.json", "{\n  \"¿Cuál es la capital de Alemania?\": \"Berlín\",\n  \"¿Qué color tiene el cielo en un día despejado?\": \"Azul\"\n}\n");
            seed("trivia-cache.json", "{}\n");
            seed("word-cache.json", "{}\n");
            if (update) {importKnownWords();importKnownTrivia();config.configRevision=7;}
            if(config.configRevision<8) {
                config.gameDelays.putIfAbsent("math",ModConfig.Delay.random(2000,3000));
                config.gameDelays.putIfAbsent("variable",ModConfig.Delay.random(3000,5000));
                config.configRevision=8;
            }
            if(config.configRevision<9) {
                // This release applies the requested fast interval to every answering mode.
                config.delay=ModConfig.Delay.random(0,1000);
                config.gameDelays.clear();
                config.configRevision=9;
            }
            save();
        } catch (Exception e) { throw new IllegalStateException("Cannot load ChatGames configuration", e); }
    }
    private void seed(String name, String text) throws Exception { Path file = dir.resolve(name); if (!Files.exists(file)) {
            try (var resource = ConfigManager.class.getResourceAsStream("/"+name)) {
                if (resource != null) Files.copy(resource,file);
                else Files.writeString(file,text,StandardCharsets.UTF_8);
            }
        } }
    private void importKnownWords() throws Exception {
        Path file=dir.resolve("word-cache.json");
        Map<String,String> saved;
        try {saved=GSON.fromJson(Files.readString(file),new TypeToken<Map<String,String>>(){}.getType());}
        catch (Exception ignored) {saved=null;}
        if (saved==null) saved=new LinkedHashMap<>();
        try (var resource=ConfigManager.class.getResourceAsStream("/word-answers-es.json")) {
            if (resource==null) return;
            Map<String,String> known=GSON.fromJson(new InputStreamReader(resource,StandardCharsets.UTF_8),new TypeToken<Map<String,String>>(){}.getType());
            if (known!=null) saved.putAll(known);
        }
        Files.writeString(file,GSON.toJson(saved),StandardCharsets.UTF_8);
    }
    private void importKnownTrivia() throws Exception {
        Path file=dir.resolve("trivia-cache.json");
        Map<String,String> saved=new LinkedHashMap<>();
        try {
            Map<String,String> old=GSON.fromJson(Files.readString(file),new TypeToken<Map<String,String>>(){}.getType());
            if(old!=null)old.forEach((q,a)->{if(TriviaSolver.validAnswer(q,a))saved.put(TriviaSolver.normalize(q),a);});
        }catch(Exception ignored) { }
        try(var resource=ConfigManager.class.getResourceAsStream("/trivia-known-es.json")) {
            if(resource!=null) {
                Map<String,String> known=GSON.fromJson(new InputStreamReader(resource,StandardCharsets.UTF_8),new TypeToken<Map<String,String>>(){}.getType());
                if(known!=null)known.forEach((q,a)->saved.put(TriviaSolver.normalize(q),a));
            }
        }
        Files.writeString(file,GSON.toJson(saved),StandardCharsets.UTF_8);
    }
    public Path dir() { return dir; }
    public ModConfig get() { return config; }
    public synchronized void save() { try { Files.writeString(path, GSON.toJson(config), StandardCharsets.UTF_8); } catch (Exception e) { throw new IllegalStateException(e); } }
}
