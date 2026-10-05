package dev.clares.chatgames.gui;

import dev.clares.chatgames.config.ConfigManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.Text;

public final class TimeEditorScreen extends Screen {
    private final ChatGamesScreen parent;
    private final ConfigManager settings;
    private final TimeDraft draft;
    private final String gameTitle;
    private TextFieldWidget minimum,maximum;
    private ButtonWidget mode,save;
    private int x,y,w;
    private String failure="";
    public TimeEditorScreen(ChatGamesScreen parent,ConfigManager settings,GameCatalog.Entry entry) {
        super(Text.literal("Tiempo de respuesta"));this.parent=parent;this.settings=settings;
        gameTitle=entry==null?"Tiempo general":entry.title();draft=new TimeDraft(settings,entry==null?"":entry.key());
    }
    @Override protected void init() {
        w=Math.min(440,width-24);x=(width-w)/2;y=Math.max(2,(height-218)/2);int inner=w-24,field=(inner-12)/2;
        mode=button("",x+12,y+40,inner,20,()->{draft.nextMode();refresh();},"Fijo: un tiempo. Aleatorio: mínimo y máximo. General: hereda el intervalo común.");
        minimum=addDrawableChild(new TextFieldWidget(textRenderer,x+12,y+80,field,20,Text.literal("Tiempo mínimo en segundos")));
        maximum=addDrawableChild(new TextFieldWidget(textRenderer,x+24+field,y+80,field,20,Text.literal("Tiempo máximo en segundos")));
        minimum.setMaxLength(12);maximum.setMaxLength(12);minimum.setUneditableColor(0xFF7A8B9A);maximum.setUneditableColor(0xFF7A8B9A);
        minimum.setText(draft.minimum);maximum.setText(draft.maximum);
        minimum.setChangedListener(s->{draft.minimum=s;failure="";refresh();});
        maximum.setChangedListener(s->{draft.maximum=s;failure="";refresh();});
        minimum.setTooltip(Tooltip.of(Text.literal("De 0 a 60 segundos. Ejemplos: 0, 0.5, 1, 2.5. También puedes escribir 500ms.")));
        maximum.setTooltip(Tooltip.of(Text.literal("Límite superior del intervalo aleatorio. Debe ser igual o mayor que el mínimo.")));
        int presetW=(inner-16)/5;
        for(int i=0;i<5;i++) {
            int seconds=new int[]{0,1,2,3,5}[i];
            button(seconds+" s",x+12+i*(presetW+4),y+120,presetW,20,()->preset(seconds*1000,seconds*1000),"Tiempo fijo de "+seconds+(seconds==1?" segundo.":" segundos.")+" Pulsa Guardar para aplicarlo.");
        }
        button("Al azar: 0–1 s",x+12,y+145,inner/2-4,20,()->preset(0,1000),"Carga un rango rápido. Todavía no se ha guardado.");
        button("Al azar: 3–5 s",x+12+inner/2+4,y+145,inner/2-4,20,()->preset(3000,5000),"Carga un rango para ecuaciones. Todavía no se ha guardado.");
        button("Cancelar",x+12,y+188,inner/2-4,20,this::close,"Vuelve sin guardar este intervalo.");
        save=button("Guardar cambios",x+12+inner/2+4,y+188,inner/2-4,20,()->{
            try{draft.save();parent.saved(gameTitle);client.setScreen(parent);}
            catch(IllegalArgumentException e){failure=e.getMessage();refresh();}
            catch(RuntimeException e){failure="No se pudo guardar. Puedes volver a intentarlo.";refresh();}
        },"Guarda el intervalo en tu configuración. Se conserva después de reiniciar Minecraft.");
        refresh();
    }
    private void preset(int min,int max){draft.preset(min,max);minimum.setText(draft.minimum);maximum.setText(draft.maximum);refresh();}
    private void refresh() {
        if(mode==null||minimum==null||maximum==null)return;
        mode.setMessage(Text.literal(switch(draft.mode){case FIXED->"Modo: tiempo fijo";case RANDOM->"Modo: intervalo aleatorio";case INHERIT->"Modo: usar tiempo general";}));
        minimum.setEditable(draft.mode!=TimeDraft.Mode.INHERIT);minimum.active=draft.mode!=TimeDraft.Mode.INHERIT;
        maximum.setEditable(draft.mode==TimeDraft.Mode.RANDOM);maximum.active=draft.mode==TimeDraft.Mode.RANDOM;
        if(save!=null)save.active=draft.error().isEmpty();
    }
    private ButtonWidget button(String label,int x,int y,int width,int height,Runnable action,String lore) {
        return addDrawableChild(ButtonWidget.builder(Text.literal(label),b->action.run()).dimensions(x,y,width,height).tooltip(Tooltip.of(Text.literal(lore))).build());
    }
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        c.fill(0,0,width,height,0xC7080E19);GuiTheme.panel(c,x,y,w,218);
        GuiTheme.fit(c,textRenderer,gameTitle+" · tiempo de respuesta",x+12,y+10,w-24,GuiTheme.GOLD);
        GuiTheme.fit(c,textRenderer,"Elige el intervalo y guarda para aplicarlo.",x+12,y+24,w-24,GuiTheme.MUTED);
        c.drawTextWithShadow(textRenderer,draft.mode==TimeDraft.Mode.FIXED?"Segundos":"Mínimo (s)",x+12,y+68,GuiTheme.TEXT);
        c.drawTextWithShadow(textRenderer,draft.mode==TimeDraft.Mode.FIXED?"No se usa":"Máximo (s)",x+24+(w-36)/2,y+68,draft.mode==TimeDraft.Mode.RANDOM?GuiTheme.TEXT:GuiTheme.MUTED);
        c.drawTextWithShadow(textRenderer,"Opciones rápidas",x+12,y+108,GuiTheme.MUTED);
        String error=draft.error();String message=!failure.isEmpty()?failure:!error.isEmpty()?error:
            draft.mode==TimeDraft.Mode.INHERIT?"General actual: "+TimeDraft.describe(settings.get().delayFor("")):"0 s = sin espera añadida tras resolver el reto.";
        GuiTheme.fit(c,textRenderer,message,x+12,y+173,w-24,!failure.isEmpty()||!error.isEmpty()?GuiTheme.RED:GuiTheme.GREEN);
        super.render(c,mx,my,delta);
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void close(){client.setScreen(parent);}
}
