package dev.clares.chatgames.log;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LogChatParser {
    private static final Pattern CHAT = Pattern.compile("^\\[[\\d:.]+\\] \\[[^]]+\\]\\s*:\\s*(?:\\[[^]]+\\]\\s*)?\\[CHAT\\]\\s*(.*)$");
    private LogChatParser() { }
    public static Optional<String> parse(String line) {
        Matcher m = CHAT.matcher(line);
        return m.matches() && !m.group(1).startsWith("[CGH]") ? Optional.of(m.group(1)) : Optional.empty();
    }
}
