package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TrunkClient {
    static int rendered;
    @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(TravelingTrunk.ENTITY.get(),Renderer::new);}
    @SubscribeEvent static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent e){e.register(TravelingTrunk.MENU.get(),Screen::new);}
    static final ResourceLocation TEXTURE=AlchemyContent.id("textures/models/trunk.png"),ANGRY=AlchemyContent.id("textures/models/trunkangry.png");
    /** RenderTravelingTrunk: scale (f2*f3, 0.5/f2*f3, f2*f3) with f2=1/1.4 and f3=2/1.5 (2/1.33 with the earth upgrade), shifted -0.5,0.5,-0.5. */
    static final class Renderer extends MobRenderer<TravelingTrunk,Model> {
        Renderer(EntityRendererProvider.Context c){super(c,new Model(),.5f);}
        @Override public ResourceLocation getTextureLocation(TravelingTrunk t){return t.anger()>0?ANGRY:TEXTURE;}
        @Override protected void scale(TravelingTrunk t,PoseStack pose,float partial){float f2=1/1.4f,f3=t.upgrade()==1?2/1.33f:2/1.5f;pose.scale(f2*f3,.5f/f2*f3,f2*f3);pose.translate(-.5f,.5f,-.5f);}
        @Override public void render(TravelingTrunk t,float yaw,float partial,PoseStack pose,MultiBufferSource b,int light){rendered++;getModel().partial=partial;super.render(t,yaw,partial,pose,b,light);}
        @Override protected float getFlipDegrees(TravelingTrunk t){return 0;}
    }
    /** ModelTrunk: lid 14x5x14 hinged at (1,7,15), knob 2x4x1 on the lid, base 14x10x14; 64x64 texture. */
    static final class Model extends EntityModel<TravelingTrunk> {
        final ModelPart root,lid,knob,below;TravelingTrunk trunk;float partial;
        Model(){super(RenderType::entityCutoutNoCull);var mesh=new MeshDefinition();var r=mesh.getRoot();
            r.addOrReplaceChild("lid",CubeListBuilder.create().texOffs(0,0).addBox(0,-5,-14,14,5,14),PartPose.offset(1,7,15));
            r.addOrReplaceChild("knob",CubeListBuilder.create().texOffs(0,0).addBox(-1,-2,-15,2,4,1),PartPose.offset(8,7,15));
            r.addOrReplaceChild("below",CubeListBuilder.create().texOffs(0,19).addBox(0,0,0,14,10,14),PartPose.offset(1,6,1));
            root=LayerDefinition.create(mesh,64,64).bakeRoot();lid=root.getChild("lid");knob=root.getChild("knob");below=root.getChild("below");}
        @Override public void setupAnim(TravelingTrunk t,float limb,float amount,float age,float yaw,float pitch){trunk=t;float rot=net.minecraft.util.Mth.lerp(partial,t.lidrotO,t.lidrot),f1=1-rot;f1=1-f1*f1*f1;lid.xRot=-(f1*(float)Math.PI/2);knob.xRot=lid.xRot;}
        @Override public void renderToBuffer(PoseStack p,VertexConsumer v,int light,int overlay,int color){root.render(p,v,light,overlay,color);
            if(trunk==null||trunk.upgrade()<0)return;int port=TravelingTrunk.portIndex(trunk.upgrade());if(port<0)return;
            /* the upgrade sigil sits on the knob, rotating with the lid */
            p.pushPose();p.translate(knob.x/16,knob.y/16,knob.z/16);p.mulPose(Axis.XP.rotation(knob.xRot));p.translate(-.075f,-.115f,-.94301f);p.scale(.15f,.15f,.15f);
            var out=Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.entityTranslucent(AlchemyContent.id("textures/items/golem_upgrade_"+Golemancy.UPGRADES[port]+".png")));
            for(float[] q:new float[][]{{0,0,0,1},{1,0,1,1},{1,1,1,0},{0,1,0,0}})out.addVertex(p.last().pose(),q[0],q[1],0).setColor(color).setUv(q[2],q[3]).setOverlay(overlay).setLight(light).setNormal(p.last(),0,0,-1);
            p.popPose();}
    }
    /** GuiTravelingTrunk: 176x200, health bar 39 px, earth row overlay, stay toggle at (112,0). */
    static final class Screen extends AbstractContainerScreen<TravelingTrunk.Menu> {
        static final ResourceLocation BG=AlchemyContent.id("textures/gui/guitrunkbase.png");
        Screen(TravelingTrunk.Menu m,Inventory i,Component t){super(m,i,t);imageWidth=176;imageHeight=200;}
        @Override protected void renderBg(GuiGraphics g,float partial,int mx,int my){var t=menu.trunk;if(t==null)return;if(!t.isAlive()){onClose();return;}
            g.blit(BG,leftPos,topPos,0,0,imageWidth,imageHeight,256,256);int hp=Math.round(t.getHealth()/t.getMaxHealth()*39);g.blit(BG,leftPos+134,topPos+2,176,16,hp,6,256,256);
            if(t.upgrade()==1)g.blit(BG,leftPos,topPos+80,0,206,imageWidth,27,256,256);if(t.stay())g.blit(BG,leftPos+112,topPos,176,0,10,10,256,256);}
        @Override protected void renderLabels(GuiGraphics g,int x,int y){g.pose().pushPose();g.pose().scale(.5f,.5f,1);g.drawString(font,menu.trunk==null?"":menu.trunk.ownerName()+Component.translatable("entity.tc4modern.traveling_trunk.guiname").getString(),8,4,12624112,false);g.pose().popPose();}
        @Override public boolean mouseClicked(double x,double y,int button){var t=menu.trunk;if(t!=null&&x>=leftPos+112&&x<leftPos+122&&y>=topPos&&y<=topPos+10){minecraft.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),.3f,.6f+(t.stay()?0:.2f));minecraft.player.displayClientMessage(Component.translatable(t.stay()?"entity.tc4modern.traveling_trunk.move":"entity.tc4modern.traveling_trunk.stay"),false);minecraft.gameMode.handleInventoryButtonClick(menu.containerId,1);return true;}return super.mouseClicked(x,y,button);}
        @Override public void render(GuiGraphics g,int mx,int my,float p){super.render(g,mx,my,p);renderTooltip(g,mx,my);}
        @Override public boolean isPauseScreen(){return false;}
    }
}
