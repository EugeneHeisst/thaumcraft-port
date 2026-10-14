package dev.eugheist.tc4modern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
final class ResearchTableEffects {
    static void connection(GuiGraphics gui,float x,float y,float x2,float y2,float ticks){
        float dx=x2-x,dy=y2-y,length=(float)Math.hypot(dx,dy);if(length<.001f)return;float nx=-dy/length*1.5f,ny=dx/length*1.5f;
        int color=(int)((.6f+(float)Math.sin(ticks+x)*.3f)*255)<<24|0x0099CC;
        var v=gui.bufferSource().getBuffer(RenderType.gui());var m=gui.pose().last().pose();v.addVertex(m,x+nx,y+ny,0).setColor(color);v.addVertex(m,x2+nx,y2+ny,0).setColor(color);v.addVertex(m,x2-nx,y2-ny,0).setColor(color);v.addVertex(m,x-nx,y-ny,0).setColor(color);gui.flush();
    }
}
