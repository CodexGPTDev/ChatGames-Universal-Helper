package dev.clares.chatgames.command;
import dev.clares.chatgames.config.*;
import dev.clares.chatgames.game.*;
import dev.clares.chatgames.log.LogWatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.Locale;
import java.util.function.Consumer;

public final class ClientCommands {
    private final ConfigManager settings; private final GameManager games; private final LogWatcher watcher; private final Consumer<String> say; private final Consumer<Text> styled; private final Runnable openGui;
    public ClientCommands(ConfigManager settings,GameManager games,LogWatcher watcher,Consumer<String> say,Consumer<Text> styled) {this(settings,games,watcher,say,styled,()->{});}
    public ClientCommands(ConfigManager settings,GameManager games,LogWatcher watcher,Consumer<String> say,Consumer<Text> styled,Runnable openGui) {this.settings=settings;this.games=games;this.watcher=watcher;this.say=say;this.styled=styled;this.openGui=openGui;}
    public void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(CommandTree.build(this::run)));
    }
    private int run(String args) {
        ModConfig c=settings.get(); String[] p=args.strip().split("\\s+",4);
        try {
            switch(p[0].toLowerCase(Locale.ROOT)) {
                case "gui","menu" -> openGui.run();
                case "help" -> {
                    int page=p.length==1?1:Integer.parseInt(p[1]);
                    if (page<1 || page>HelpPages.count()) {say.accept("Elige /cgedit help 1, 2 o 3.");break;}
                    showHelp(page);
                }
                case "on","off" -> {boolean value=p[0].equalsIgnoreCase("on");games.setEnabled(value);say.accept(value?"Respuestas automáticas: ACTIVADAS. Esperando el próximo juego.":"Respuestas automáticas: DESACTIVADAS. Envíos pendientes cancelados.");}
                case "status" -> say.accept("Respuestas automáticas: "+(c.enabled?"ACTIVADAS":"DESACTIVADAS")+" | Avisos: "+(c.stats?"on":"off")+" | Log: "+watcher.watching()+" | Sesión: "+(games.current()==null?"ninguna":games.current().status));
                case "testlog" -> say.accept("Log: "+(watcher.watching()?"DETECTED":"WAITING")+" | Path: "+watcher.path()+" | Watching: "+watcher.watching()+" | Last chat line: "+watcher.lastChat());
                case "last" -> {GameSession s=games.last(); say.accept(s==null?"Sin desafío":"Tipo: "+s.type+" | Entrada: "+s.question+" | Respuesta: "+s.answer+" | Fuente: "+s.source+" | Estado: "+s.status);}
                case "cache" -> say.accept("Entradas locales en caché: "+games.cacheSize());
                case "ai" -> say.accept("IA: "+c.ai.enabled+" / "+c.ai.provider+" / modelo "+c.ai.model+" / clave de entorno "+c.ai.apiKeyEnv);
                case "debug","stats" -> {boolean value=toggle(p,1); if(p[0].equalsIgnoreCase("debug"))c.debug=value;else c.stats=value;settings.save();say.accept(p[0].equalsIgnoreCase("stats")?"Avisos de respuesta enviada: "+(value?"on":"off")+". Para pausar las respuestas: /cgedit off":"debug: "+value);}
                case "game" -> {if(p.length<3)throw new IllegalArgumentException();GameType type=GameType.valueOf(p[1].toUpperCase(Locale.ROOT));boolean value=toggle(p,2);games.setGameEnabled(type,value);say.accept(p[1]+": "+(value?"ACTIVADO":"DESACTIVADO; envío pendiente cancelado"));}
                case "settime" -> TimeCommand.execute(settings,args.length()>7?args.substring(7).strip():"").forEach(say);
                case "test" -> {if(p.length<3)throw new IllegalArgumentException();GameType t=GameType.valueOf(p[1].toUpperCase(Locale.ROOT));String input=args.substring(args.indexOf(p[1])+p[1].length()).strip();say.accept("Test "+t+": "+games.solveLocal(t,input).orElse("sin respuesta local"));}
                case "simulate" -> {if(p.length<2)throw new IllegalArgumentException();games.simulate(args.substring(args.indexOf(' ')+1).replaceAll("^\\\"|\\\"$", ""));}
                default -> say.accept("Comando desconocido. /cgedit help");
            }
        } catch (Exception ex) {say.accept("Uso inválido. /cgedit help");}
        return 1;
    }
    private void showHelp(int number) {
        HelpPages.Page page=HelpPages.page(number);
        styled.accept(Text.literal("ChatGames · "+page.title()+" · "+number+"/"+HelpPages.count()).formatted(Formatting.GOLD,Formatting.BOLD));
        for (HelpPages.Entry entry:page.entries()) {
            styled.accept(Text.literal(entry.command()).formatted(Formatting.AQUA)
                .append(Text.literal("\n  "+entry.description()).formatted(Formatting.GRAY)));
        }
        styled.accept(Text.literal("Más ayuda: /cgedit help 1 · /cgedit help 2 · /cgedit help 3").formatted(Formatting.YELLOW));
    }
    private static boolean toggle(String[] p,int i) {if(p.length<=i||!p[i].equalsIgnoreCase("on")&&!p[i].equalsIgnoreCase("off"))throw new IllegalArgumentException();return p[i].equalsIgnoreCase("on");}
}
