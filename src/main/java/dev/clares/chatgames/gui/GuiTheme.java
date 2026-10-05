package dev.clares.chatgames.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Text;

final class GuiTheme {
    static final int TEXT=0xFFE8F1F5,MUTED=0xFF9DAFBC,GOLD=0xFFFFCF70,GREEN=0xFF8BE0AD,RED=0xFFFF9999;
    static void panel(DrawContext c,int x,int y,int w,int h) {
        c.fill(x-2,y-2,x+w+2,y+h+2,0xCC03080F);
        c.fill(x,y,x+w,y+h,0xF0172233);
        c.fill(x,y,x+w,y+2,0xFF5EC6BD);
        c.fill(x,y+34,x+w,y+35,0xFF2C3C50);
    }
    static void fit(DrawContext c,TextRenderer r,String s,int x,int y,int width,int color) {
        String clipped=r.getWidth(s)<=width?s:r.trimToWidth(s,Math.max(0,width-r.getWidth("…")))+"…";
        c.drawTextWithShadow(r,clipped,x,y,color);
    }
    static Text text(String s){return Text.literal(s);}
    private GuiTheme(){}
}
