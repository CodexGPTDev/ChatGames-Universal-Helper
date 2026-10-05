package dev.clares.chatgames.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.item.*;
import net.minecraft.text.Text;

/** A keyboard-accessible Minecraft button with a decorative game item. */
final class GameCard extends ButtonWidget {
    private final ItemStack icon;
    private final String time,source;
    GameCard(int x,int y,int width,GameCatalog.Entry entry,String time,String source,PressAction action) {
        super(x,y,width,32,net.minecraft.text.Text.literal(entry.title()),action,DEFAULT_NARRATION_SUPPLIER);
        this.time=time;this.source=source;
        icon=new ItemStack(switch(entry.type()) {
            case MATH->Items.REDSTONE;case VARIABLE->Items.COMPARATOR;case TRIVIA->Items.BOOK;
            case UNSCRAMBLE->Items.NAME_TAG;case FILLOUT->Items.WRITABLE_BOOK;case UNREVERSE->Items.SPYGLASS;
            case REACTION->Items.FEATHER;case RANDOM->Items.PAPER;case CLICKABLE->Items.STONE_BUTTON;
            default->Items.ENDER_PEARL;
        });
    }
    @Override protected void drawIcon(DrawContext c,int mouseX,int mouseY,float delta) {
        boolean selected=isHovered()||isFocused();int x=getX(),y=getY();
        c.fill(x,y,x+width,y+height,selected?0xFF30475B:0xFF203044);
        c.fill(x,y,x+2,y+height,selected?0xFF81E5D7:0xFF4C7C83);
        c.drawItem(icon,x+8,y+9);
        var r=MinecraftClient.getInstance().textRenderer;
        GuiTheme.fit(c,r,getMessage().getString(),x+30,y+8,width-34,GuiTheme.TEXT);
        GuiTheme.fit(c,r,time,x+30,y+22,width-34,GuiTheme.GOLD);
        GuiTheme.fit(c,r,source,x+8,y+38,width-66,GuiTheme.MUTED);
    }
    @Override protected void drawLabel(DrawnTextConsumer consumer){}
}
