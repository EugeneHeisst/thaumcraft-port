package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class ThaumatoriumClient {
    static final ObjParts MODEL=new ObjParts(AlchemyContent.id("models/thaumatorium.obj"));
    @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(Thaumatorium.ENTITY.get(),Renderer::new);}
    @SubscribeEvent static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(Thaumatorium.MENU.get(),Screen::new);}
    public static final class Renderer implements BlockEntityRenderer<ThaumatoriumEntity> {
        Renderer(BlockEntityRendererProvider.Context c){}
        @Override public void render(ThaumatoriumEntity t,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){
            if(t.getBlockState().getValue(Thaumatorium.TOP))return;
            p.pushPose();p.translate(.5,0,.5);p.mulPose(Axis.XN.rotationDegrees(90));
            switch(t.facing()){case EAST->p.mulPose(Axis.ZP.rotationDegrees(180));case SOUTH->p.mulPose(Axis.ZP.rotationDegrees(90));case NORTH->p.mulPose(Axis.ZP.rotationDegrees(270));default->{}}
            MODEL.renderAll(RenderType.entityCutout(AlchemyContent.id("textures/models/thaumatorium.png")),p,b,light,overlay,0xFFFFFFFF);p.popPose();
            var keys=t.state.selected;
            if(!keys.isEmpty()){
                int tick=Minecraft.getInstance().player==null?0:Minecraft.getInstance().player.tickCount;
                var r=CrucibleRecipes.byKey().get(keys.get(tick/40%keys.size()));
                if(r!=null){var d=t.facing();p.pushPose();p.translate(.5+d.getStepX()/1.99,1.325,.5+d.getStepZ()/1.99);
                    switch(d){case EAST->p.mulPose(Axis.YP.rotationDegrees(90));case WEST->p.mulPose(Axis.YP.rotationDegrees(270));case NORTH->p.mulPose(Axis.YP.rotationDegrees(180));default->{}}
                    p.scale(.375f,.375f,.375f);Minecraft.getInstance().getItemRenderer().renderStatic(r.getRecipeOutput(),ItemDisplayContext.FIXED,light,overlay,p,b,t.getLevel(),0);p.popPose();
                }
            }
        }
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(ThaumatoriumEntity t){return new net.minecraft.world.phys.AABB(t.getBlockPos()).inflate(1).expandTowards(0,1,0);}
    }
    public static final class Screen extends AbstractContainerScreen<ThaumatoriumMenu> {
        int index,start,lastSize=-1;
        static final net.minecraft.resources.ResourceLocation TEX=AlchemyContent.id("textures/gui/gui_thaumatorium.png");
        public Screen(ThaumatoriumMenu m,Inventory inv,Component title){super(m,inv,title);}
        @Override protected void renderLabels(GuiGraphics g,int x,int y){}
        @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){
            g.blit(TEX,leftPos,topPos,0,0,imageWidth,imageHeight);
            var all=menu.recipes();if(all.isEmpty())return;if(lastSize!=all.size()){lastSize=all.size();for(int i=0;i<all.size();i++)if(menu.tile.state.selected.contains(all.get(i))){index=i;break;}start=0;}index=Math.min(index,all.size()-1);var key=all.get(index);var r=CrucibleRecipes.byKey().get(key);
            boolean selected=menu.tile.state.selected.contains(key);
            if(selected)g.blit(TEX,leftPos+104,topPos+8,176,96,48,48);
            if(!menu.tile.state.selected.isEmpty())g.blit(TEX,leftPos+88,topPos+16,176,56,24,24);
            g.renderItem(r.getRecipeOutput(),leftPos+112,topPos+16);g.renderItemDecorations(font,r.getRecipeOutput(),leftPos+112,topPos+16);
            if(all.size()>1){g.blit(TEX,leftPos+128,topPos+16,index>0?192:176,16,16,8);g.blit(TEX,leftPos+128,topPos+24,index+1<all.size()?192:176,24,16,8);}
            var aspects=r.aspects.getAspectsSorted();if(aspects.length<=6)start=0;
            for(int i=start;i<Math.min(aspects.length,start+6);i++){
                var a=aspects[i];int x=leftPos+40+(i-start)*16;
                RecipeBookBridge.drawTag(g,a,r.aspects.getAmount(a),x,topPos+40);
                if(selected){g.blit(TEX,x+1,topPos+57,176,8,14,6);int n=Math.min(12,menu.tile.state.essentia.getAmount(a)*12/r.aspects.getAmount(a));g.fill(x+2,topPos+58,x+2+n,topPos+62,0xFF000000|a.getColor());}
            }
            if(aspects.length>6){g.blit(TEX,leftPos+32,topPos+40,start>0?192:176,32,8,16);g.blit(TEX,leftPos+136,topPos+40,start<aspects.length-1?200:184,32,8,16);}
            if(menu.tile.state.maxRecipes>1){g.pose().pushPose();g.pose().translate(leftPos+136,topPos+33,0);g.pose().scale(.5f,.5f,1);g.drawCenteredString(font,menu.tile.state.selected.size()+"/"+menu.tile.state.maxRecipes,0,0,0xFFFFFF);g.pose().popPose();}
        }
        @Override public void render(GuiGraphics g,int mx,int my,float partial){super.render(g,mx,my,partial);renderTooltip(g,mx,my);var all=menu.recipes();if(!all.isEmpty()&&mx>=leftPos+112&&mx<leftPos+128&&my>=topPos+16&&my<topPos+32)g.renderTooltip(font,CrucibleRecipes.byKey().get(all.get(Math.min(index,all.size()-1))).getRecipeOutput(),mx,my);}
        @Override public boolean mouseClicked(double mx,double my,int button){
            var all=menu.recipes();int x=(int)mx-leftPos,y=(int)my-topPos;
            if(!all.isEmpty()){
                index=Math.min(index,all.size()-1);
                if(x>=112&&x<128&&y>=16&&y<32){minecraft.gameMode.handleInventoryButtonClick(menu.containerId,index);return true;}
                if(x>=128&&x<144&&y>=16&&y<32){index=Math.clamp(index+(y<24?-1:1),0,all.size()-1);start=0;return true;}
                if(y>=40&&y<56){if(x>=32&&x<40){start=Math.max(0,start-1);return true;}if(x>=136&&x<144){start=Math.min(CrucibleRecipes.byKey().get(all.get(index)).aspects.size()-1,start+1);return true;}}
            }
            return super.mouseClicked(mx,my,button);
        }
    }
}
