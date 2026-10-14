package dev.eugheist.tc4modern;

import net.minecraft.client.gui.GuiGraphics;

final class GuiLines {
    private GuiLines() {}
    static void line(GuiGraphics gui,int x,int y,int targetX,int targetY,int color) {
        int steps=Math.max(Math.abs(targetX-x),Math.abs(targetY-y));
        if(steps==0) return;
        for(int step=0;step<=steps;step++) {
            int px=x+(targetX-x)*step/steps,py=y+(targetY-y)*step/steps;
            gui.fill(px,py,px+1,py+1,color);
        }
    }
}
