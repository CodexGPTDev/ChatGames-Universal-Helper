package dev.clares.chatgames;

import dev.clares.chatgames.command.ClientCommands;
import dev.clares.chatgames.config.ConfigManager;
import dev.clares.chatgames.game.GameManager;
import dev.clares.chatgames.hud.ChatGamesHud;
import dev.clares.chatgames.gui.GuiController;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import dev.clares.chatgames.log.LogWatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class ChatGamesUniversalHelperClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        MinecraftClient client=MinecraftClient.getInstance();
        Path dir=FabricLoader.getInstance().getGameDir();
        ConfigManager config=new ConfigManager(dir);
        Consumer<String> feedback=msg -> client.execute(() -> {if(client.player!=null)client.player.sendMessage(Text.literal("[CGH] "+msg),false);});
        GameManager manager=new GameManager(config,client,feedback);
        Path log="auto".equalsIgnoreCase(config.get().logPath)?dir.resolve("logs/latest.log"):Path.of(config.get().logPath);
        LogWatcher watcher=new LogWatcher(log,manager::onLog); watcher.start();
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {if(!overlay)manager.onRich(message);});
        ClientPlayConnectionEvents.JOIN.register((handler,sender,c) -> manager.resetServer(handler.getConnection().getAddress().toString()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler,c) -> manager.resetServer(""));
        ClientLifecycleEvents.CLIENT_STOPPING.register(c -> {watcher.close();manager.close();});
        Consumer<Text> styledFeedback=msg -> client.execute(() -> {
            if(client.player!=null)client.player.sendMessage(Text.literal("[CGH] ").append(msg),false);
        });
        GuiController gui=new GuiController(config,manager);
        ClientTickEvents.END_CLIENT_TICK.register(gui::tick);
        new ClientCommands(config,manager,watcher,feedback,styledFeedback,gui::requestOpen).register();
        ChatGamesHud.register(manager,config);
    }
}
