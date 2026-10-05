package dev.clares.chatgames.gui;

import dev.clares.chatgames.config.*;
import dev.clares.chatgames.command.TimeCommand;

/** Edits are isolated from the live configuration until save succeeds. */
public final class TimeDraft {
    public enum Mode { FIXED, RANDOM, INHERIT }
    public final String key;
    private final ConfigManager settings;
    public Mode mode;
    public String minimum,maximum;
    public TimeDraft(ConfigManager settings,String key) {
        this.settings=settings;this.key=key;
        ModConfig config=settings.get();ModConfig.Delay d=config.delayFor(key);
        mode=!key.isEmpty()&&!config.gameDelays.containsKey(key)?Mode.INHERIT:
            "fixed".equalsIgnoreCase(d.mode)?Mode.FIXED:Mode.RANDOM;
        minimum=seconds(d.minMs);maximum=seconds(d.maxMs);
    }
    public void nextMode(){mode=switch(mode){case FIXED->Mode.RANDOM;case RANDOM->key.isEmpty()?Mode.FIXED:Mode.INHERIT;case INHERIT->Mode.FIXED;};}
    public void preset(int min,int max){mode=min==max?Mode.FIXED:Mode.RANDOM;minimum=seconds(min);maximum=seconds(max);}
    public ModConfig.Delay value() {
        if(mode==Mode.INHERIT){if(key.isEmpty())throw new IllegalArgumentException("El tiempo general no puede heredarse.");return null;}
        int min,max;
        try{min=TimeCommand.parseDuration(minimum);max=mode==Mode.FIXED?min:TimeCommand.parseDuration(maximum);}
        catch(IllegalArgumentException e){throw new IllegalArgumentException("Escribe un tiempo válido entre 0 y 60 segundos.");}
        if(min>max)throw new IllegalArgumentException("El mínimo no puede superar al máximo.");
        return mode==Mode.FIXED?ModConfig.Delay.fixed(min):ModConfig.Delay.random(min,max);
    }
    public String error(){try{value();return "";}catch(IllegalArgumentException e){return e.getMessage();}}
    public void save() {
        ModConfig.Delay next=value();ModConfig c=settings.get();
        synchronized(c) {
            ModConfig.Delay old=key.isEmpty()?c.delay.copy():c.gameDelays.get(key);
            c.setDelay(key,next);
            try{settings.save();}catch(RuntimeException e){c.setDelay(key,old);throw e;}
        }
    }
    public static String seconds(int ms){return java.math.BigDecimal.valueOf(ms,3).stripTrailingZeros().toPlainString();}
    public static String describe(ModConfig.Delay d){return "fixed".equalsIgnoreCase(d.mode)||d.minMs==d.maxMs?seconds(d.minMs)+" s":seconds(d.minMs)+"–"+seconds(d.maxMs)+" s";}
}
