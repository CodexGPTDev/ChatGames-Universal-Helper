package dev.clares.chatgames.gui;

import dev.clares.chatgames.config.*;
import dev.clares.chatgames.game.GameManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import java.time.Duration;

public final class ChatGamesScreen extends Screen {
    private final ConfigManager settings;
    private final GameManager games;
    private final Screen parent;
    private GuiLayout layout;
    private int page;
    private String notice="Pasa el cursor para ver la información. Haz clic para editar.";
    public ChatGamesScreen(ConfigManager settings,GameManager games,Screen parent) {
        super(Text.literal("ChatGames · Configuración"));this.settings=settings;this.games=games;this.parent=parent;
    }
    @Override protected void init() {
        layout=GuiLayout.main(width,height);page=Math.min(page,layout.pages(GameCatalog.ENTRIES.size())-1);
        ModConfig config=settings.get();int x=layout.x()+12,y=layout.y()+38,w=layout.width()-24;
        button(config.enabled?"Mod: ACTIVADO":"Mod: EN PAUSA",x,y,w/2-4,18,()->{
            games.setEnabled(!settings.get().enabled);notice=settings.get().enabled?"Mod activado. Esperando el próximo reto.":"Mod en pausa. Respuestas pendientes canceladas.";clearAndInit();
        },"Activa o pausa todas las respuestas. Conserva el estado de cada juego.");
        button("Tiempo general: "+TimeDraft.describe(config.delayFor("")),x+w/2+4,y,w/2-4,18,
            ()->client.setScreen(new TimeEditorScreen(this,settings,null)),"Edita el intervalo que heredan los juegos sin tiempo propio.");
        int start=page*layout.pageSize(),end=Math.min(GameCatalog.ENTRIES.size(),start+layout.pageSize());
        for(int i=start;i<end;i++) {
            var entry=GameCatalog.ENTRIES.get(i);String key=entry.key();
            int slot=i-start,cx=layout.cardX(slot),cy=layout.cardY(slot),cw=layout.cardWidth();
            boolean enabled=config.game(key),own=config.gameDelays.containsKey(key);
            GameCard card=addDrawableChild(new GameCard(cx,cy,cw,entry,TimeDraft.describe(config.delayFor(key)),
                own?"Propio":"General",b->client.setScreen(new TimeEditorScreen(this,settings,entry))));
            MutableText lore=Text.literal("").append(Text.literal("✦ "+entry.title()).formatted(Formatting.GOLD,Formatting.BOLD))
                .append(Text.literal("\n\n"+entry.description()+"\n"+entry.detail()).formatted(Formatting.GRAY))
                .append(Text.literal("\n\nEstado: "+(enabled?"ACTIVADO":"PAUSADO")).formatted(enabled?Formatting.GREEN:Formatting.RED))
                .append(Text.literal("\nRespuesta: "+TimeDraft.describe(config.delayFor(key))).formatted(Formatting.AQUA))
                .append(Text.literal("\n"+(own?"Intervalo propio":"Usa el tiempo general")).formatted(Formatting.DARK_GRAY))
                .append(Text.literal(config.enabled?"":"\nEl mod está en pausa general.").formatted(Formatting.YELLOW))
                .append(Text.literal("\n\nClic: configurar y guardar el tiempo.").formatted(Formatting.YELLOW));
            card.setTooltip(Tooltip.of(lore));card.setTooltipDelay(Duration.ofMillis(180));
            button(enabled?"Activo":"Pausado",cx+cw-54,cy+34,54,18,()->{
                games.setGameEnabled(entry.type(),!settings.get().game(key));
                notice=entry.title()+": "+(settings.get().game(key)?"activado y guardado.":"pausado; envío pendiente cancelado.");clearAndInit();
            },"Cambiar el estado de "+entry.title()+". Se guarda inmediatamente.");
        }
        int fy=layout.footer();
        button("Avisos: "+(config.stats?"sí":"no"),x,fy,w/2-4,18,()->{config.stats=!config.stats;persistAndRefresh("Avisos locales actualizados.");},"Muestra u oculta «Respuesta enviada». No activa ni pausa el mod.");
        button("HUD: "+(config.hud?"sí":"no"),x+w/2+4,fy,w/2-4,18,()->{config.hud=!config.hud;persistAndRefresh("HUD actualizado.");},"Muestra u oculta el pequeño panel de estado durante un reto.");
        ButtonWidget previous=button("‹",x,fy+22,26,18,()->{page--;clearAndInit();},"Página anterior");previous.active=page>0;
        ButtonWidget next=button("›",x+30,fy+22,26,18,()->{page++;clearAndInit();},"Página siguiente");next.active=page<layout.pages(GameCatalog.ENTRIES.size())-1;
        button("Cerrar",x+w-70,fy+22,70,18,this::close,"Los cambios guardados se conservan al reiniciar.");
    }
    private void persistAndRefresh(String text){try{settings.save();notice=text;}catch(RuntimeException e){notice="No se pudo guardar. Revisa el archivo de configuración.";}clearAndInit();}
    private ButtonWidget button(String label,int x,int y,int w,int h,Runnable action,String lore) {
        Text text=Text.literal(label);
        if(label.equals("Activo")||label.equals("Mod: ACTIVADO"))text=Text.literal(label).formatted(Formatting.GREEN);
        if(label.equals("Pausado")||label.equals("Mod: EN PAUSA"))text=Text.literal(label).formatted(Formatting.RED);
        return addDrawableChild(ButtonWidget.builder(text,b->{try{action.run();}catch(RuntimeException e){notice="No se pudo guardar el ajuste. Revisa la configuración.";clearAndInit();}}).dimensions(x,y,w,h).tooltip(Tooltip.of(Text.literal(lore))).build());
    }
    public void saved(String title){notice=title+": tiempo guardado. Se aplica a las próximas respuestas.";}
    @Override public void render(DrawContext c,int mx,int my,float delta) {
        c.fill(0,0,width,height,0xBF080E19);GuiTheme.panel(c,layout.x(),layout.y(),layout.width(),layout.height());
        c.drawTextWithShadow(textRenderer,"CHATGAMES",layout.x()+12,layout.y()+10,GuiTheme.GOLD);
        GuiTheme.fit(c,textRenderer,notice,layout.x()+12,layout.y()+24,layout.width()-24,GuiTheme.MUTED);
        int enabled=(int)GameCatalog.ENTRIES.stream().filter(e->settings.get().game(e.key())).count();
        String badge=enabled+"/"+GameCatalog.ENTRIES.size()+" juegos activos";
        c.drawTextWithShadow(textRenderer,badge,layout.x()+layout.width()-12-textRenderer.getWidth(badge),layout.y()+10,GuiTheme.GREEN);
        for(int slot=0;slot<Math.min(layout.pageSize(),GameCatalog.ENTRIES.size()-page*layout.pageSize());slot++)
            c.fill(layout.cardX(slot),layout.cardY(slot),layout.cardX(slot)+layout.cardWidth(),layout.cardY(slot)+52,0xFF203044);
        c.drawTextWithShadow(textRenderer,(page+1)+" / "+layout.pages(GameCatalog.ENTRIES.size()),layout.x()+78,layout.footer()+27,GuiTheme.MUTED);
        super.render(c,mx,my,delta);
    }
    @Override public boolean shouldPause(){return false;}
    @Override public void close(){client.setScreen(parent);}
}
