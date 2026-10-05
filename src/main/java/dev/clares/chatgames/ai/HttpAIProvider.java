package dev.clares.chatgames.ai;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.clares.chatgames.config.ModConfig;
import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.Optional;

/** Sends only the challenge prompt; a key is read at request time from an environment variable. */
public final class HttpAIProvider implements AIProvider {
    private final ModConfig.Ai config;
    public HttpAIProvider(ModConfig.Ai config) { this.config=config; }
    @Override public Optional<String> ask(String instruction,String question) {
        try {
            String key=System.getenv(config.apiKeyEnv);
            if (!config.enabled || key==null || key.isBlank() || config.endpoint.isBlank() || config.model.isBlank()) return Optional.empty();
            JsonObject body=new JsonObject(); body.addProperty("model",config.model);
            String endpoint=config.endpoint;
            if (config.provider.equalsIgnoreCase("gemini")) {
                JsonObject item=new JsonObject(); JsonObject part=new JsonObject(); part.addProperty("text",instruction+"\n"+question);
                com.google.gson.JsonArray parts=new com.google.gson.JsonArray(); parts.add(part); item.add("parts",parts);
                com.google.gson.JsonArray contents=new com.google.gson.JsonArray(); contents.add(item); body=new JsonObject(); body.add("contents",contents);
                if (!endpoint.contains(":generateContent")) endpoint=endpoint.replaceAll("/$","")+"/models/"+config.model+":generateContent";
            } else {
                com.google.gson.JsonArray messages=new com.google.gson.JsonArray();
                JsonObject sys=new JsonObject(); sys.addProperty("role","system"); sys.addProperty("content",instruction); messages.add(sys);
                JsonObject user=new JsonObject(); user.addProperty("role","user"); user.addProperty("content",question); messages.add(user); body.add("messages",messages);
            }
            HttpRequest.Builder req=HttpRequest.newBuilder(URI.create(endpoint)).timeout(Duration.ofMillis(Math.max(500,config.timeoutMs))).header("Content-Type","application/json");
            req.header(config.provider.equalsIgnoreCase("gemini") ? "x-goog-api-key" : "Authorization",config.provider.equalsIgnoreCase("gemini")?key:"Bearer "+key);
            HttpResponse<String> response=HttpClient.newHttpClient().send(req.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(),HttpResponse.BodyHandlers.ofString());
            if (response.statusCode()!=200) return Optional.empty();
            JsonObject json=JsonParser.parseString(response.body()).getAsJsonObject();
            String result=config.provider.equalsIgnoreCase("gemini") ? json.getAsJsonArray("candidates").get(0).getAsJsonObject().getAsJsonObject("content").getAsJsonArray("parts").get(0).getAsJsonObject().get("text").getAsString() : json.getAsJsonArray("choices").get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
            result=result.strip().replaceAll("^[`'\"]+|[`'\"]+$","");
            return result.isBlank() || result.length()>80 || result.contains("\n") ? Optional.empty() : Optional.of(result);
        } catch (Exception e) { return Optional.empty(); }
    }
}
