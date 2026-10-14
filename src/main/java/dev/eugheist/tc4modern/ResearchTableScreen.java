package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.aspects.*;
import thaumcraft.common.lib.utils.HexUtils;
import thaumcraft.common.lib.research.ResearchNoteData;

/** Modern adapter for original research-table drawing, sounds and drag/drop gestures. */
public class ResearchTableScreen extends net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<ResearchTableMenu> {
    private static final ResourceLocation BACKGROUND=texture("textures/gui/guiresearchtable2.png");
    private static final ResourceLocation PARTICLES=texture("textures/misc/particles.png");
    private Aspect select1,select2,dragged;
    private int page;
    private long nextCombine,lastRune;
    private String runeNote="";
    private final Random random=new Random();
    private record Rune(HexUtils.Hex hex,int glyph,long start,long end) {}
    private final Map<String,Rune> runes=new HashMap<>();
    private final long openedAt=System.nanoTime();
    public ResearchTableScreen(ResearchTableMenu menu,Inventory inventory,Component title){super(menu,inventory,title);imageWidth=255;imageHeight=255;}
    private static ResourceLocation texture(String path){return ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,path);}
    private AspectList known(){var data=new net.minecraft.nbt.CompoundTag();data.put(ResearchKnowledge.KEY,ClientKnowledge.knowledge.copy());return ResearchKnowledge.load(data).getAspectsDiscovered("current");}
    private Set<String> research(){var data=new net.minecraft.nbt.CompoundTag();data.put(ResearchKnowledge.KEY,ClientKnowledge.knowledge.copy());return ResearchProgress.completed(data);}
    private int available(Aspect aspect){return known().getAmount(aspect)+menu.bonus.getAmount(aspect);}
    private Aspect[] visible(){return Arrays.stream(known().getAspectsSorted()).skip(page*5L).limit(25).toArray(Aspect[]::new);}
    private float ticks(){return (System.nanoTime()-openedAt)/50_000_000f;}
    private static void blend(){com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();}
    private void image(GuiGraphics gui,ResourceLocation image,int x,int y,int u,int v,int w,int h,float alpha){blend();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,alpha);gui.blit(image,x,y,u,v,w,h,256,256);com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);}
    private void icon(GuiGraphics gui,Aspect aspect,double x,double y,int count,float alpha,boolean dark){
        if(aspect==null)return;int color=dark?0x191919:aspect.getColor();blend();com.mojang.blaze3d.systems.RenderSystem.setShaderColor((color>>16&255)/255f,(color>>8&255)/255f,(color&255)/255f,alpha);
        gui.pose().pushPose();gui.pose().translate(x,y,0);gui.blit(aspect.getImage(),0,0,0,0,16,16,16,16);gui.pose().popPose();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
        if(count>0){String amount=Integer.toString(count);gui.pose().pushPose();gui.pose().scale(.5f,.5f,1);int tx=(int)(x*2+32-font.width(amount)),ty=(int)(y*2+32-font.lineHeight);for(int[] p:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})gui.drawString(font,amount,tx+p[0],ty+p[1],0,false);gui.drawString(font,amount,tx,ty,0xFFFFFF,false);gui.pose().popPose();}
    }
    static float lastOrbShade=1;
    private void orb(GuiGraphics gui,double x,double y,Integer color){
        float t=ticks();int rgb=color==null?((int)((.85+.15*Math.sin((t+x)/10))*255)<<16|(int)((.85+.15*Math.sin((t+x+y)/11))*255)<<8|(int)((.85+.15*Math.sin((t+y)/12))*255)):color;
        /* GuiResearchTable.drawOrb: colour_blind darkens every channel by 1.8 */float shade=WarpClientConfig.colorBlind()?1/1.8f:1;lastOrbShade=shade;
        blend();com.mojang.blaze3d.systems.RenderSystem.setShaderColor((rgb>>16&255)/255f*shade,(rgb>>8&255)/255f*shade,(rgb&255)/255f*shade,1);
        gui.pose().pushPose();gui.pose().translate(x+8,y+8,0);gui.pose().mulPose(new org.joml.Quaternionf().rotationZ((float)Math.PI/2));gui.blit(PARTICLES,-8,-8,(128+((int)t%8)*32)%256,128,16,16,256,256);gui.pose().popPose();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
    }
    @Override protected void renderBg(GuiGraphics gui,float partial,int mx,int my){
        image(gui,BACKGROUND,leftPos,topPos,0,0,255,167,1);image(gui,BACKGROUND,leftPos+40,topPos+167,0,166,184,88,1);
        AspectList known=known();Aspect[] aspects=visible();
        for(int i=0;i<aspects.length;i++){int x=leftPos+10+i/5*16,y=topPos+40+i%5*16;icon(gui,aspects[i],x,y,known.getAmount(aspects[i]),available(aspects[i])>0?1:.33f,false);
            if(menu.bonus.getAmount(aspects[i])>0)image(gui,PARTICLES,x-4,y-4,((int)ticks()%16)*16,80,16,16,1);
        }
        if(select1!=null&&available(select1)<=0)select1=null;if(select2!=null&&available(select2)<=0)select2=null;
        icon(gui,select1,leftPos+13,topPos+139,0,1,false);icon(gui,select2,leftPos+71,topPos+139,0,1,false);
        if(select1!=null&&select2!=null){image(gui,BACKGROUND,leftPos+35,topPos+139,184,184,32,16,1);if(System.nanoTime()>nextCombine)orb(gui,leftPos+43,topPos+139,null);else image(gui,BACKGROUND,leftPos+35,topPos+139,184,168,32,16,1);}
        if(page>0)image(gui,BACKGROUND,leftPos+27,topPos+121,184,208,24,8,1);
        if((page+1)*5+20<known.size())image(gui,BACKGROUND,leftPos+51,topPos+121,208,208,24,8,1);
        drawNote(gui,mx,my);
    }
    private void drawNote(GuiGraphics gui,int mx,int my){
        var note=NoteData.read(menu.table.getItem(1));if(note==null){runes.clear();runeNote="";return;}
        image(gui,texture("textures/misc/parchment3.png"),leftPos+94,topPos+8,0,0,150,150,1);
        if(!note.key.equals(runeNote)){runes.clear();runeNote=note.key;}
        long now=System.nanoTime()/1_000_000;
        if(now>=lastRune){lastRune=now+250;var hex=new HexUtils.Pixel(random.nextInt(120)-60,random.nextInt(120)-60).toHex(9);if(!note.hexes.containsKey(hex.toString())&&!runes.containsKey(hex.toString()))runes.put(hex.toString(),new Rune(hex,random.nextInt(16),now,now+15250+random.nextInt(10000)));}
        runes.values().removeIf(r->r.end<now);
        for(var rune:runes.values()){var p=rune.hex.toPixel(9);float progress=(float)(now-rune.start)/(rune.end-rune.start),alpha=(progress<.25f?progress*2:progress>.5f?1-progress:.5f)*.66f;blend();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(0,0,0,alpha);gui.pose().pushPose();gui.pose().translate(leftPos+169+p.x,topPos+83+p.y,0);gui.pose().mulPose(new org.joml.Quaternionf().rotationZ(-(float)Math.PI/2));gui.blit(texture("textures/misc/script.png"),-5,-5,10,10,rune.glyph*16,0,16,16,256,16);gui.pose().popPose();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);}
        var graph=ResearchTableGraph.connected(note,known());
        for(var edge:graph.edges()){var a=edge.first().toPixel(9);var b=edge.second().toPixel(9);ResearchTableEffects.connection(gui,(float)(leftPos+169+a.x),(float)(topPos+83+a.y),(float)(leftPos+169+b.x),(float)(topPos+83+b.y),ticks());}
        var hover=cell(mx,my,note);
        for(var hex:note.hexes.values()){var entry=note.hexEntries.get(hex.toString());var pixel=hex.toPixel(9);double x=leftPos+161+pixel.x,y=topPos+75+pixel.y;
            if(!note.complete){if(entry.type==1)orb(gui,x,y,null);else{
                if(hover!=null&&hex.equals(hover)){blend();com.mojang.blaze3d.systems.RenderSystem.blendFunc(770,1);gui.pose().pushPose();gui.pose().translate(x,y,0);gui.blit(texture("textures/gui/hex2.png"),0,0,0,0,16,16,16,16);gui.pose().popPose();}
                blend();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,.25f);gui.pose().pushPose();gui.pose().translate(x,y,0);gui.blit(texture("textures/gui/hex1.png"),0,0,0,0,16,16,16,16);gui.pose().popPose();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);
            }}
            if(entry.aspect==null)continue;
            if(!known().aspects.containsKey(entry.aspect)){blend();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(0,0,0,.5f);gui.pose().pushPose();gui.pose().translate(x,y,0);gui.blit(texture("textures/aspects/_unknown.png"),0,0,0,0,16,16,16,16);gui.pose().popPose();com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1,1,1,1);}
            else icon(gui,entry.aspect,x,y,0,entry.type==1||graph.highlight().contains(hex.toString())?1:.528f,entry.type!=1&&!graph.highlight().contains(hex.toString()));
        }
        if(note.complete&&research().contains("RESEARCHDUPE"))image(gui,BACKGROUND,leftPos+37,topPos+5,232,200,24,24,1);
    }
    private HexUtils.Hex cell(double mx,double my,ResearchNoteData note){if(mx<leftPos+94||mx>=leftPos+244||my<topPos+8||my>=topPos+158)return null;var hex=new HexUtils.Pixel(mx-leftPos-169,my-topPos-83).toHex(9);return note.hexes.containsKey(hex.toString())?hex:null;}
    private Aspect aspectAt(double mx,double my,boolean ignoreZero){var visible=visible();for(int i=0;i<visible.length;i++){int x=leftPos+10+i/5*16,y=topPos+40+i%5*16;if(mx>=x&&mx<x+16&&my>=y&&my<y+16&&(ignoreZero||available(visible[i])>0))return visible[i];}return null;}
    @Override protected void renderLabels(GuiGraphics gui,int x,int y){}
    @Override public void render(GuiGraphics gui,int mx,int my,float partial){
        super.render(gui,mx,my,partial);
        gui.pose().pushPose();gui.pose().translate(0,0,300);ResearchNotificationHud.draw(gui);gui.pose().popPose();
        if(dragged!=null){gui.pose().pushPose();gui.pose().translate(0,0,400);orb(gui,mx-8,my-8,dragged.getColor());gui.pose().popPose();return;}
        renderTooltip(gui,mx,my);drawCopyCost(gui,mx,my);var aspect=aspectAt(mx,my,true);
        if(aspect!=null){gui.renderComponentTooltip(font,List.of(Component.literal(aspect.getName()),Component.literal(aspect.getLocalizedDescription())),mx,my-8);if(research().contains("RESEARCHER1")&&aspect.getComponents()!=null){icon(gui,aspect.getComponents()[0],mx+8,my+8,0,1,false);icon(gui,aspect.getComponents()[1],mx+26,my+8,0,1,false);}}
        drawNoInk(gui);
    }
    /** GuiResearchTable.drawScreen: with a note on the table and no usable ink, the warning always sits over the note (drawCustomTooltip, aqua/grey). */
    private void drawNoInk(GuiGraphics gui){
        if(NoteData.read(menu.table.getItem(1))!=null&&!inkAvailable()){List<Component> lines=List.of(Component.translatable("tile.researchtable.noink.0").withStyle(net.minecraft.ChatFormatting.AQUA),Component.translatable("tile.researchtable.noink.1").withStyle(net.minecraft.ChatFormatting.GRAY));
            int sx=Math.max(font.width(lines.get(0)),font.width(lines.get(1)))/2;gui.pose().pushPose();gui.pose().translate(0,0,350);gui.renderComponentTooltip(font,lines,leftPos+157-sx,topPos+84);gui.pose().popPose();}
    }
    private void drawCopyCost(GuiGraphics gui,int mx,int my){
        var note=NoteData.read(menu.table.getItem(1));
        if(note==null||!note.complete||!research().contains("RESEARCHDUPE")||mx<leftPos+37||mx>=leftPos+61||my<topPos+5||my>=topPos+29)return;
        var entry=thaumcraft.api.research.ResearchCategories.getResearch(note.key);if(entry==null)return;
        gui.pose().pushPose();gui.pose().translate(0,0,400);
        image(gui,BACKGROUND,leftPos+100,topPos+21,184,224,48,16,1);
        gui.drawString(font,Component.translatable("tc.research.copy"),leftPos+100,topPos+12,0xFFFFFF,false);
        int index=0;for(var aspect:entry.tags.getAspectsSorted())icon(gui,aspect,leftPos+148+16*index++,topPos+21,entry.tags.getAmount(aspect)+note.copies,1,false);
        gui.pose().popPose();
    }
    private boolean inkAvailable(){var ink=menu.table.getItem(0);return ScribingToolsItem.hasInk(ink);}
    protected void sound(String key,float volume,boolean vary){var event=net.minecraft.sounds.SoundEvent.createVariableRangeEvent(texture(key));minecraft.getSoundManager().play(SimpleSoundInstance.forUI(event,vary?1+random.nextFloat()*.1f:1,volume));}
    protected void place(HexUtils.Hex hex,Aspect aspect){PacketDistributor.sendToServer(new PlaceAspectPayload(menu.containerId,hex.q,hex.r,aspect==null?"":aspect.getTag()));}
    protected void combine(Aspect first,Aspect second){PacketDistributor.sendToServer(new CombinePayload(menu.containerId,first.getTag(),second.getTag()));}
    protected void duplicate(){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,5);}
    void auditDraw(GuiGraphics gui,int mx,int my){renderBg(gui,0,mx,my);for(var slot:menu.slots)if(slot.hasItem())gui.renderItem(slot.getItem(),leftPos+slot.x,topPos+slot.y);if(dragged!=null)orb(gui,mx-8,my-8,dragged.getColor());drawCopyCost(gui,mx,my);drawNoInk(gui);}
    int[] auditAspect(Aspect a){var list=visible();for(int i=0;i<list.length;i++)if(list[i]==a)return new int[]{leftPos+18+i/5*16,topPos+48+i%5*16};throw new IllegalArgumentException(a.getTag());}
    int[] auditCell(HexUtils.Hex hex){var p=hex.toPixel(9);return new int[]{leftPos+169+(int)Math.round(p.x),topPos+83+(int)Math.round(p.y)};}
    int[] auditCombineSlot(boolean second){return new int[]{leftPos+(second?79:20),topPos+146};}
    int[] auditCopyButton(){return new int[]{leftPos+49,topPos+17};}
    int[] auditCombineButton(){return new int[]{leftPos+51,topPos+147};}
    @Override public boolean mouseClicked(double mx,double my,int button){
        int x=(int)mx-leftPos,y=(int)my-topPos;var note=NoteData.read(menu.table.getItem(1));
        if(button==0){
            if(y>=121&&y<129){if(x>=27&&x<51&&page>0){page--;sound("key",.3f,false);return true;}if(x>=51&&x<75&&(page+1)*5+20<known().size()){page++;sound("key",.3f,false);return true;}}
            if(x>=35&&x<67&&y>=139&&y<155&&select1!=null&&select2!=null&&System.nanoTime()>nextCombine){nextCombine=System.nanoTime()+200_000_000;sound("cameraclack",.4f,false);sound("hhon",.3f,false);combine(select1,select2);return true;}
            if(x>=11&&x<29&&y>=137&&y<155&&select1!=null){select1=null;sound("hhoff",.2f,true);return true;}if(x>=71&&x<87&&y>=137&&y<155&&select2!=null){select2=null;sound("hhoff",.2f,true);return true;}
            if(note!=null&&note.complete&&research().contains("RESEARCHDUPE")&&x>=37&&x<61&&y>=5&&y<29){duplicate();sound("cameraclack",.4f,false);return true;}
            var aspect=aspectAt(mx,my,hasShiftDown()&&research().contains("RESEARCHER2"));
            if(aspect!=null){if(hasShiftDown()&&research().contains("RESEARCHER2")&&!aspect.isPrimal()&&available(aspect.getComponents()[0])>0&&available(aspect.getComponents()[1])>0){sound("hhon",.3f,false);combine(aspect.getComponents()[0],aspect.getComponents()[1]);return true;}
                if(available(aspect)>0){dragged=aspect;setDragging(true);sound("hhoff",.2f,true);return true;}}
        }
        if(note!=null&&!note.complete){var hex=cell(mx,my,note);if(hex!=null&&note.hexEntries.get(hex.toString()).type==2&&inkAvailable()){sound("hhon",.3f,false);sound("erase",.2f,true);place(hex,null);return true;}}
        return super.mouseClicked(mx,my,button);
    }
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){return dragged!=null||super.mouseDragged(mx,my,button,dx,dy);}
    @Override public boolean mouseReleased(double mx,double my,int button){
        if(button==0&&dragged!=null){Aspect aspect=dragged;dragged=null;setDragging(false);var note=NoteData.read(menu.table.getItem(1));var hex=note==null?null:cell(mx,my,note);
            if(note!=null&&!note.complete&&hex!=null&&note.hexEntries.get(hex.toString()).type==0&&inkAvailable()){sound("hhon",.3f,false);sound("write",.2f,false);place(hex,aspect);return true;}
            double x=mx-leftPos,y=my-topPos;if(x>=4&&x<36&&y>=130&&y<162){select1=aspect;sound("hhoff",.2f,true);}else if(x>=63&&x<95&&y>=130&&y<162){select2=aspect;sound("hhoff",.2f,true);}else if(aspectAt(mx,my,false)==aspect){if(select1==null)select1=aspect;else if(select2==null)select2=aspect;}
            return true;
        }
        return super.mouseReleased(mx,my,button);
    }
}
