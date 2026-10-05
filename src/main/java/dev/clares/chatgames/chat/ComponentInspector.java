package dev.clares.chatgames.chat;
import net.minecraft.text.*;
import java.util.ArrayList;
import java.util.List;

/** Rich text is read from the receive callback; latest.log contains no click or hover metadata. */
public final class ComponentInspector {
    public record Rich(String label,String command,String hover) { }
    public static List<Rich> inspect(Text text) {
        List<Rich> result=new ArrayList<>();
        text.visit((style,content) -> {
            String command="", hover="";
            if (style.getClickEvent() instanceof ClickEvent.RunCommand run) command=run.command();
            if (style.getHoverEvent() instanceof HoverEvent.ShowText shown) hover=shown.value().getString();
            if (!command.isEmpty() || !hover.isEmpty()) result.add(new Rich(content,command,hover));
            return java.util.Optional.<Void>empty();
        },Style.EMPTY);
        return result;
    }
}
