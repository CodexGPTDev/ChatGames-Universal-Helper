package dev.clares.chatgames.solver;

import com.google.gson.reflect.TypeToken;
import dev.clares.chatgames.config.ConfigManager;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Normalizer;
import java.util.*;

/** User dictionaries take priority; packaged frequency lists cover older installations too. */
public final class WordSolver {
    private final Map<String,List<String>> anagrams = new HashMap<>();
    private final Map<String,List<String>> phraseAnagrams = new HashMap<>();
    private final Map<Integer,List<String>> byLength = new HashMap<>();
    private final Map<String,String> cache = new HashMap<>();
    private final Map<String,Map<String,List<String>>> knownFillouts = new HashMap<>();
    private final Map<String,List<String>> serverWords = new HashMap<>();
    private final Path cachePath;
    public WordSolver(Path dir) {
        cachePath = dir.resolve("word-cache.json");
        try(var resource=WordSolver.class.getResourceAsStream("/fillout-known.json")) {
            if(resource!=null) {
                Map<String,Map<String,List<String>>> data=ConfigManager.GSON.fromJson(new InputStreamReader(resource,StandardCharsets.UTF_8),new TypeToken<Map<String,Map<String,List<String>>>>(){}.getType());
                if(data!=null)knownFillouts.putAll(data);
            }
        }catch(Exception ignored) { }
        try(var resource=WordSolver.class.getResourceAsStream("/server-words.json")) {
            if(resource!=null) {
                Map<String,List<String>> data=ConfigManager.GSON.fromJson(new InputStreamReader(resource,StandardCharsets.UTF_8),new TypeToken<Map<String,List<String>>>(){}.getType());
                if(data!=null)serverWords.putAll(data);
            }
        }catch(Exception ignored) { }
        Set<String> loaded = new HashSet<>();
        for (String language : List.of("es","en")) {
            try {
                for (String word : Files.readAllLines(dir.resolve("words-"+language+".txt"),StandardCharsets.UTF_8)) add(word,loaded);
            } catch (Exception ignored) { }
        }
        for (String language : List.of("es","en")) {
            try (var stream = WordSolver.class.getResourceAsStream("/words-"+language+"-extended.txt")) {
                if (stream == null) continue;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))) {
                    String word; while ((word=reader.readLine())!=null) add(word,loaded);
                }
            } catch (Exception ignored) { }
        }
        try {
            Map<String,String> saved = ConfigManager.GSON.fromJson(Files.readString(cachePath),new TypeToken<Map<String,String>>(){}.getType());
            if (saved != null) cache.putAll(saved);
        } catch (Exception ignored) { }
    }
    private void add(String raw,Set<String> loaded) {
        String word=raw.strip().replaceAll("\\s+"," ");
        if (word.length()<2 || word.length()>80 || !word.matches("[\\p{L}]+(?: [\\p{L}]+)*") || !loaded.add(normalizeWords(word))) return;
        if (word.contains(" ")) {
            phraseAnagrams.computeIfAbsent(phraseSignature(word),key->new ArrayList<>()).add(word);
            return;
        }
        anagrams.computeIfAbsent(signature(word),key->new ArrayList<>()).add(word);
        byLength.computeIfAbsent(normalize(word).length(),key->new ArrayList<>()).add(word);
    }
    public static String normalize(String s) {
        return Normalizer.normalize(s,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
    }
    private static String signature(String s) { char[] chars = normalize(s).toCharArray(); Arrays.sort(chars); return new String(chars); }
    private static String normalizeWords(String input) {
        return Arrays.stream(input.strip().split("\\s+")).map(WordSolver::normalize).collect(java.util.stream.Collectors.joining(" "));
    }
    private static String phraseSignature(String input) {
        return Arrays.stream(input.strip().split("\\s+")).map(WordSolver::signature).collect(java.util.stream.Collectors.joining(" "));
    }
    private static String key(String input) { return "u:"+normalizeWords(input); }
    private static String caseLikePrompt(String answer,String input) {
        if (answer.isEmpty() || !input.codePoints().anyMatch(Character::isUpperCase)) return answer;
        if (!answer.equals(answer.toLowerCase(Locale.ROOT))) return answer;
        return answer.substring(0,1).toUpperCase(Locale.ROOT)+answer.substring(1);
    }
    public synchronized Optional<String> unscramble(String input) {
        if (input==null || input.isBlank() || input.length()>80) return Optional.empty();
        input=input.strip().replaceAll("\\s+"," ");
        String exact=key(input);
        String saved=cache.get(exact);
        if (saved==null) saved=cache.get("u:"+signature(input)); // Migration from 1.0.0.
        if (saved!=null && !normalizeWords(input).equals(normalizeWords(saved)) && matchesUnscramble(input,saved)) return Optional.of(caseLikePrompt(saved,input));
        if (input.contains(" ")) {
            List<String> phrases=phraseAnagrams.getOrDefault(phraseSignature(input),List.of());
            if (!phrases.isEmpty()) return Optional.of(phrases.getFirst());
            String[] parts=input.split(" ");
            if (parts.length>8) return Optional.empty();
            List<String> solved=new ArrayList<>();
            for (String part:parts) {
                Optional<String> answer=unscramble(part);
                if (answer.isEmpty()) return Optional.empty();
                solved.add(answer.get());
            }
            String answer=String.join(" ",solved);
            cache.put(exact,answer);save();return Optional.of(answer);
        }
        List<String> candidates=anagrams.getOrDefault(signature(input),List.of());
        if (candidates.isEmpty()) return Optional.empty();
        String normalizedInput=normalize(input);
        String answer=caseLikePrompt(candidates.stream().filter(w->!normalize(w).equals(normalizedInput)).findFirst().orElse(candidates.getFirst()),input);
        cache.put(exact,answer); save();
        return Optional.of(answer);
    }
    /** Learn the server's canonical spelling only when the letters match exactly. */
    public synchronized boolean rememberUnscramble(String input,String answer) {
        if (!matchesUnscramble(input,answer) || normalizeWords(input).equals(normalizeWords(answer))) return false;
        cache.put(key(input),answer.strip()); save(); return true;
    }
    public static boolean matchesFillout(String input,String answer) {
        if(input==null || answer==null)return false;
        String pattern=normalizeMask(input), candidate=normalizeMask(answer);
        if (pattern.length()!=candidate.length() || pattern.indexOf('_')<0 && pattern.indexOf('?')<0) return false;
        for (int i=0;i<pattern.length();i++)
            if (pattern.charAt(i)!='_' && pattern.charAt(i)!='?' && pattern.charAt(i)!=candidate.charAt(i)) return false;
        return true;
    }
    public static boolean matchesUnscramble(String input,String answer) {
        return input!=null && answer!=null && !answer.isBlank() && phraseSignature(input).equals(phraseSignature(answer));
    }
    private static String normalizeMask(String input) {
        return Normalizer.normalize(input.strip().replaceAll("\\s+"," "),Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);
    }
    public synchronized boolean rememberFillout(String input,String answer) {
        return rememberFillout(input,answer,"");
    }
    public synchronized boolean rememberFillout(String input,String answer,String context) {
        if (!matchesFillout(input,answer)) return false;
        cache.put(filloutKey(input,context),answer.strip()); save(); return true;
    }
    private static String filloutKey(String input,String context) {return "f:"+(context.isEmpty()?"":context+":")+normalizeMask(input);}
    private static String filloutCase(String answer,String mask) {
        if (answer.isEmpty() || mask.isEmpty()) return answer;
        // Keep every visible letter's case; prefer a capital initial when it is hidden.
        mask=mask.strip().replaceAll("\\s+"," ");
        char[] letters=answer.strip().toCharArray();
        if (mask.charAt(0)=='_' || mask.charAt(0)=='?') letters[0]=Character.toUpperCase(letters[0]);
        for (int i=0;i<Math.min(mask.length(),letters.length);i++) {
            if (Character.isUpperCase(mask.charAt(i))) letters[i]=Character.toUpperCase(letters[i]);
            else if (Character.isLowerCase(mask.charAt(i))) letters[i]=Character.toLowerCase(letters[i]);
        }
        return new String(letters);
    }
    public synchronized Optional<String> fillout(String input) {
        return fillout(input,"");
    }
    public synchronized Optional<String> fillout(String input,String context) {
        if(input==null || input.isBlank() || input.length()>80)return Optional.empty();
        String pattern = normalizeMask(input);
        if(!pattern.matches("[a-z_?]+(?: [a-z_?]+)*") || !pattern.contains("_") && !pattern.contains("?"))return Optional.empty();
        String cacheKey=filloutKey(input,context);
        if(!context.isEmpty()) {
            // A live result learned for this exact format takes precedence over bundled data.
            String learned=cache.get(cacheKey);
            if(learned!=null && matchesFillout(input,learned))return Optional.of(filloutCase(learned,input));
            List<String> confirmed=knownFillouts.getOrDefault(context,Map.of()).getOrDefault(pattern,List.of());
            if(!confirmed.isEmpty()) {
                // A mask can be genuinely ambiguous. Prefer the most recent confirmed candidate.
                String answer=confirmed.getLast();
                if(matchesFillout(input,answer))return Optional.of(filloutCase(answer,input));
            }
        }
        String generic=cache.get("f:"+pattern);
        if(generic!=null && matchesFillout(input,generic))return Optional.of(filloutCase(generic,input));
        List<String> candidates=new ArrayList<>(serverWords.getOrDefault(context,List.of()));
        if(context.isEmpty())for(var known:serverWords.values())candidates.addAll(known);
        for(String word:candidates)if(matchesFillout(input,word))return Optional.of(filloutCase(word,input));
        for (String word : byLength.getOrDefault(pattern.length(),List.of())) {
            if (matchesFillout(input,word)) {String answer=filloutCase(word,input);cache.put(cacheKey,answer);save();return Optional.of(answer);}
        }
        return Optional.empty();
    }
    public synchronized int cacheSize() { return cache.size(); }
    private void save() { try { Files.writeString(cachePath,ConfigManager.GSON.toJson(cache),StandardCharsets.UTF_8); } catch (Exception ignored) { } }
}
