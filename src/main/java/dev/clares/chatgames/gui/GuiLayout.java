package dev.clares.chatgames.gui;

/** Two-column pages adapt to the Minecraft GUI scale without overlapping controls. */
public record GuiLayout(int x,int y,int width,int height,int rows) {
    public static GuiLayout main(int screenWidth,int screenHeight) {
        int rows=Math.max(1,Math.min(3,(screenHeight-110)/58));
        int w=Math.min(620,screenWidth-24),h=rows*58+102;
        return new GuiLayout((screenWidth-w)/2,Math.max(2,(screenHeight-h)/2),w,h,rows);
    }
    public int pageSize(){return rows*2;}
    public int pages(int entries){return Math.max(1,(entries+pageSize()-1)/pageSize());}
    public int cardWidth(){return (width-32)/2;}
    public int cardX(int slot){return x+12+(slot%2)*(cardWidth()+8);}
    public int cardY(int slot){return y+60+(slot/2)*58;}
    public int footer(){return y+60+rows*58;}
}
