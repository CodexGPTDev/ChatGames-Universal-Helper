package dev.clares.chatgames.chat;
import net.minecraft.client.MinecraftClient;

public final class ChatAnswerSender implements AnswerSender {
    private final MinecraftClient client;
    public ChatAnswerSender(MinecraftClient client) { this.client=client; }
    public boolean send(String raw) {
        if (client.player==null || client.getNetworkHandler()==null || raw==null) return false;
        String s=raw.replaceAll("[\\p{Cntrl}\\r\\n]", "").strip();
        if (s.isEmpty() || s.startsWith("/") || s.length()>256) return false;
        client.getNetworkHandler().sendChatMessage(s);
        return true;
    }
    public boolean clickCommand(String raw) {
        if (client.player==null || client.getNetworkHandler()==null || raw==null || !raw.matches("/[a-zA-Z0-9_:-]+(?: [a-zA-Z0-9_ .:-]+)?")) return false;
        client.getNetworkHandler().sendChatCommand(raw.substring(1));
        return true;
    }
}
