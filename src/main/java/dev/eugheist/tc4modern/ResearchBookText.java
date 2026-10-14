package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.lib.ResearchBookLayout;

/** GUI adapter for TC4's original Unicode font, formatting and embedded illustrations. */
final class ResearchBookText {
    static final ResourceLocation BOOK=texture("textures/gui/gui_researchbook.png");
    private static ResearchBookLayout layout;
    static void invalidate(){layout=null;}
    static ResourceLocation texture(String path) {return ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,path);}
    static ResearchBookLayout layout() {
        if(layout==null) try(var input=Minecraft.getInstance().getResourceManager().open(texture("font/glyph_sizes.bin"))) {
            layout=new ResearchBookLayout(input.readAllBytes());
        } catch(java.io.IOException e) {throw new IllegalStateException("Import the original Minecraft font with the TC4 resource pack",e);}
        return layout;
    }
    static void page(GuiGraphics gui,String text,int x,int y,int availableHeight) {
        var parts=layout().layout(text,139);int extent=0;for(var part:parts){int h=9;if(part.kind()==ResearchBookLayout.Kind.IMAGE){var p=part.value().substring(5,part.value().length()-6).split(":");h=(int)(Integer.parseInt(p[5])*Float.parseFloat(p[6]));}extent=Math.max(extent,part.y()+h);}
        float pageScale=Math.min(1,availableHeight/(float)Math.max(1,extent));gui.pose().pushPose();gui.pose().translate(x,y,0);gui.pose().scale(pageScale,pageScale,1);x=0;y=0;
        for(var part:parts) switch(part.kind()) {
            case TEXT -> string(gui,part.value(),x,y+part.y(),0);
            case LINE -> gui.blit(BOOK,x+139/2-48,y+part.y()+2,24,184,96,4,256,256);
            case IMAGE -> {
                String[] p=part.value().substring(5,part.value().length()-6).split(":");
                var image=ResourceLocation.fromNamespaceAndPath(p[0].equals("thaumcraft")?TC4Modern.MODID:p[0],p[1]);
                int w=Integer.parseInt(p[4]),h=Integer.parseInt(p[5]);float scale=Float.parseFloat(p[6]);
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
                gui.pose().pushPose();gui.pose().translate(x-3+139/2-w/2*scale,y+part.y(),0);gui.pose().scale(scale,scale,1);
                gui.blit(image,0,0,Integer.parseInt(p[2]),Integer.parseInt(p[3]),w,h,256,256);gui.pose().popPose();
            }
        }
        gui.pose().popPose();
    }
    static void string(GuiGraphics gui,String text,float x,float y,int baseColor) {
        int color=baseColor;boolean bold=false,italic=false,underline=false,strike=false;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='§' && i+1<text.length()) {
                char code=Character.toLowerCase(text.charAt(++i));int index="0123456789abcdef".indexOf(code);
                if(index>=0){int bright=(index>>3&1)*85;int r=(index>>2&1)*170+bright,g=(index>>1&1)*170+bright,b=(index&1)*170+bright;if(index==6)r+=85;color=r<<16|g<<8|b;bold=italic=underline=strike=false;}
                else switch(code){case 'l'->bold=true;case 'o'->italic=true;case 'm'->strike=true;case 'n'->underline=true;case 'r'->{color=baseColor;bold=italic=underline=strike=false;}default->{}}
                continue;
            }
            int glyph=layout().glyph(c);float advance=c==' '?4:0;
            if(c!=' '&&glyph!=0){int start=glyph>>>4,end=(glyph&15)+1;float width=(end-start-.02f)/2;
                drawGlyph(gui,c,x,y,start,width,italic,color);
                if(bold)drawGlyph(gui,c,x+.5f,y,start,width,italic,color);
                advance=(end-start)/2f+1;
            }
            if(bold)advance++;
            if(underline)gui.fill((int)x,(int)(y+8),(int)(x+advance),(int)(y+9),0xFF000000|color);
            if(strike)gui.fill((int)x,(int)(y+4),(int)(x+advance),(int)(y+5),0xFF000000|color);
            x+=advance;
        }
    }
    private static void drawGlyph(GuiGraphics gui,char c,float x,float y,int start,float width,boolean italic,int color) {
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        gui.pose().pushPose();gui.pose().translate(x+(italic?1:0),y,0);
        if(italic)gui.pose().last().pose().m10(gui.pose().last().pose().m10()-.25f);
        gui.pose().scale(width,7.99f,1);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,1);
        gui.blit(texture(String.format(java.util.Locale.ROOT,"textures/font/unicode_page_%02x.png",(int)c/256)),0,0,1,1,c%16*16+start,(c&255)/16*16,Math.round(width*2),16,256,256);
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);gui.pose().popPose();
    }
}
