package dev.eugheist.tc4modern;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;

/** Floating-point ribbons keep connections smooth and anchored at both research icons. */
final class ResearchBrowserLines {
    record Point(float x,float y) {}
    static Point point(float x,float y,float targetX,float targetY,float t,boolean animated,float ticks){
        float dx=targetX-x,dy=targetY-y;
        float bend=Math.min(18,(float)Math.hypot(dx,dy)*.12f);
        float envelope=(float)Math.sin(Math.PI*t);
        float wave=animated?(float)Math.sin(ticks*.075f-t*5)*1.2f*envelope*envelope:0;
        if(Math.abs(dx)>=Math.abs(dy))return new Point(x+dx*t,y+dy*t+(bend+wave)*envelope);
        return new Point(x+dx*t+(bend+wave)*envelope,y+dy*t);
    }
    static void draw(GuiGraphics gui,int x,int y,int x2,int y2,int rgb,boolean animated,float ticks){
        float length=(float)Math.hypot(x2-x,y2-y);
        if(length<.01f)return;
        int steps=Math.max(12,(int)Math.ceil(length));
        var vertices=gui.bufferSource().getBuffer(RenderType.gui());var matrix=gui.pose().last().pose();
        Point previous=point(x,y,x2,y2,0,animated,ticks);
        for(int i=1;i<=steps;i++){
            float t=(float)i/steps;Point next=point(x,y,x2,y2,t,animated,ticks);
            float dx=next.x-previous.x,dy=next.y-previous.y,segment=(float)Math.hypot(dx,dy);
            if(segment<.0001f)continue;
            float nx=-dy/segment,ny=dx/segment;
            float alpha=animated?.48f+.12f*(float)Math.sin(ticks*.075f-t*5):.55f;
            // Transparent outer ribbons soften the stroke without integer raster stair steps.
            for(int layer=0;layer<3;layer++){
                float half=layer==0?1.25f:layer==1?.85f:.5f;
                int opacity=(int)(255*alpha*(layer==0?.10f:layer==1?.20f:.65f));int color=opacity<<24|rgb;
                vertices.addVertex(matrix,previous.x+nx*half,previous.y+ny*half,0).setColor(color);
                vertices.addVertex(matrix,next.x+nx*half,next.y+ny*half,0).setColor(color);
                vertices.addVertex(matrix,next.x-nx*half,next.y-ny*half,0).setColor(color);
                vertices.addVertex(matrix,previous.x-nx*half,previous.y-ny*half,0).setColor(color);
            }
            previous=next;
        }
        gui.flush();
    }
}
