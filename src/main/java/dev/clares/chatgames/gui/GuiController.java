package dev.clares.chatgames.gui;

import dev.clares.chatgames.config.ConfigManager;
import dev.clares.chatgames.game.GameManager;
import net.minecraft.client.MinecraftClient;
import java.util.concurrent.atomic.AtomicBoolean;

public final class GuiController {
    private final ConfigManager settings;
    private final GameManager games;
    private final AtomicBoolean requested=new AtomicBoolean();
    public GuiController(ConfigManager settings,GameManager games){this.settings=settings;this.games=games;}
    public void requestOpen(){requested.set(true);}
    /** Open on the next client tick, after ChatScreen finishes processing the command. */
    public void tick(MinecraftClient client){if(requested.getAndSet(false))client.setScreen(new ChatGamesScreen(settings,games,null));}
}
