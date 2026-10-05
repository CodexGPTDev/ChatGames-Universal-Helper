package dev.clares.chatgames.command;

import dev.clares.chatgames.config.*;
import dev.clares.chatgames.game.GameType;
import java.util.*;

/** Validates the complete interval before replacing any saved setting. */
public final class TimeCommand {
    private TimeCommand() { }
    public static List<String> execute(ConfigManager settings,String input) {
        ModConfig config=settings.get();
        if(input.isBlank()) {
            List<String> lines=new ArrayList<>();
            lines.add("Tiempo general: "+describe(config.delayFor(""))+". Los modos con tiempo propio conservan su intervalo.");
            synchronized(config.gameDelays) {
                config.gameDelays.forEach((type,d)->{if(d!=null)lines.add(type.toUpperCase(Locale.ROOT)+": "+describe(d));});
            }
            return lines;
        }
        String[] p=input.strip().split("\\s+");
        String type="";int start=0;
        try {
            GameType mode=GameType.valueOf(p[0].toUpperCase(Locale.ROOT));
            // The legacy three-token form "random 2s 3s" sets the general interval.
            if(mode!=GameType.UNKNOWN && !(mode==GameType.RANDOM && p.length==3)) {type=mode.name().toLowerCase(Locale.ROOT);start=1;}
        }catch(IllegalArgumentException ignored) { }
        String label=type.isEmpty()?"Tiempo general":type.toUpperCase(Locale.ROOT);
        if(p.length==start)return List.of(label+": "+describe(config.delayFor(type)));
        ModConfig.Delay value;
        if(p[start].equalsIgnoreCase("default")) {
            if(type.isEmpty() || p.length!=start+1)throw new IllegalArgumentException();
            config.setDelay(type,null);settings.save();
            return List.of(label+": usa el tiempo general ("+describe(config.delayFor(type))+").");
        }
        if(p[start].equalsIgnoreCase("random")) {
            if(p.length!=start+3)throw new IllegalArgumentException();
            int min=parseDuration(p[start+1]),max=parseDuration(p[start+2]);
            if(min>max)throw new IllegalArgumentException("El mínimo supera al máximo");
            value=ModConfig.Delay.random(min,max);
        }else {
            if(p.length!=start+1)throw new IllegalArgumentException();
            value=ModConfig.Delay.fixed(parseDuration(p[start]));
        }
        config.setDelay(type,value);settings.save();
        return List.of(label+": "+describe(value)+". Se aplica a las próximas respuestas.");
    }
    public static int parseDuration(String value) {
        String s=value.strip().replace(',','.').toLowerCase(Locale.ROOT);
        double n=Double.parseDouble(s.replaceAll("(ms|s)$",""));
        double millis=n*(s.endsWith("ms")?1:1000);
        if(!Double.isFinite(millis) || millis<0 || millis>60000)throw new IllegalArgumentException();
        return (int)Math.round(millis);
    }
    private static String seconds(int ms) {return java.math.BigDecimal.valueOf(ms,3).stripTrailingZeros().toPlainString()+" s";}
    private static String describe(ModConfig.Delay d) {
        return "fixed".equalsIgnoreCase(d.mode)?seconds(d.minMs)+" fijo":seconds(d.minMs)+"–"+seconds(d.maxMs)+" al azar";
    }
}
