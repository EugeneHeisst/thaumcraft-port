package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Original advanced furnace mesh and temperature/fill overlays. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class AdvancedFurnaceClient {
    static final ObjParts MODEL=new ObjParts(AlchemyContent.id("models/adv_alch_furnace.obj"));
    @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(AdvancedFurnace.CORE.get(),Renderer::new);}
    static void bubbles(Level l,BlockPos p,RandomSource r){
        var engine=net.minecraft.client.Minecraft.getInstance().particleEngine;
        engine.add(new SlimyBubbleParticle(p.getX()+r.nextFloat(),p.getY()+1,p.getZ()+r.nextFloat(),.06f+r.nextFloat()*.06f));
        if(r.nextInt(50)==0)l.playLocalSound(p.getX()+r.nextFloat(),p.getY()+1,p.getZ()+r.nextFloat(),net.minecraft.sounds.SoundEvents.LAVA_POP,net.minecraft.sounds.SoundSource.BLOCKS,.1f+r.nextFloat()*.1f,.9f+r.nextFloat()*.15f,false);
        int q=r.nextInt(2),w=r.nextInt(2);engine.add(new SlimyBubbleParticle(p.getX()-.6+r.nextFloat()*.2+q*2,p.getY()+2,p.getZ()-.6+r.nextFloat()*.2+w*2,.06f+r.nextFloat()*.06f));
    }
    static void part(String group,String texture,PoseStack p,MultiBufferSource b,int light,int overlay){MODEL.render(group,RenderType.entityCutoutNoCull(AlchemyContent.id("textures/models/"+texture+".png")),p,b,light,overlay,0xFFFFFFFF);}
    static void quad(PoseStack p,MultiBufferSource b,String texture,int light,float width){
        var sprite=net.minecraft.client.Minecraft.getInstance().getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS).apply(texture.contains(":")?net.minecraft.resources.ResourceLocation.parse(texture):AlchemyContent.id(texture));var out=b.getBuffer(RenderType.entityTranslucent(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS));var pose=p.last();
        float[][] v={{0,1,0,sprite.getU1(),sprite.getV1()},{1,1,0,sprite.getU0(),sprite.getV1()},{1,width,0,sprite.getU0(),sprite.getV0()},{0,width,0,sprite.getU1(),sprite.getV0()}};
        for(var a:v)out.addVertex(pose,a[0],a[1],a[2]).setColor(0xFFFFFFFF).setUv(a[3],a[4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,0,1);
    }
    static final class Renderer implements BlockEntityRenderer<AdvancedFurnace.CoreEntity>{
        Renderer(BlockEntityRendererProvider.Context c){}
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(AdvancedFurnace.CoreEntity t){return new net.minecraft.world.phys.AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(t.getBlockPos().offset(-1,0,-1)),net.minecraft.world.phys.Vec3.atLowerCornerOf(t.getBlockPos().offset(2,2,2)));}
        @Override public void render(AdvancedFurnace.CoreEntity t,float partial,PoseStack p,MultiBufferSource b,int light,int overlay){
            p.pushPose();p.translate(.5,0,.5);p.mulPose(Axis.XN.rotationDegrees(90));
            part("Base",t.heat<=100?"alch_furnace":"alch_furnace_on",p,b,light,overlay);
            for(int a=0;a<4;a++){p.pushPose();p.mulPose(Axis.ZP.rotationDegrees(90*a));part("Tank",t.vis<=0?"alch_furnace_tank":"alch_furnace_tank_on",p,b,light,overlay);p.popPose();}
            if(t.vis>0){p.pushPose();p.translate(.5,-.5,1.1);p.mulPose(Axis.YP.rotationDegrees(180));quad(p,b,"blocks/fluxgoo",190,0);p.popPose();float f=1-t.vis/(float)AdvancedFurnace.CoreEntity.MAX_VIS;
                for(int a=0;a<4;a++){
                    p.pushPose();p.mulPose(Axis.ZP.rotationDegrees(90*a));p.mulPose(Axis.XN.rotationDegrees(90));p.translate(.85,-1.8,-1.4);p.scale(.3f,.6f,1);quad(p,b,"blocks/metalbase",150,0);p.translate(0,0,-.01);quad(p,b,"blocks/fluxgoo",190,f);p.popPose();
                    p.pushPose();p.mulPose(Axis.ZN.rotationDegrees(90*a));p.mulPose(Axis.XP.rotationDegrees(90));p.translate(1.15,1.8,-1.4);p.scale(-.3f,-.6f,-1);quad(p,b,"blocks/metalbase",150,0);p.translate(0,0,.01);quad(p,b,"blocks/fluxgoo",190,f);p.popPose();}}
            if(t.heat>100){p.pushPose();p.translate(0,0,1);for(int a=0;a<4;a++){p.pushPose();p.mulPose(Axis.ZP.rotationDegrees(90*a));p.mulPose(Axis.XP.rotationDegrees(135));p.mulPose(Axis.ZP.rotationDegrees(180));p.translate(-.5,0,-1);quad(p,b,"minecraft:block/fire_0",220,1-Math.min(1,t.heat/(float)AdvancedFurnace.CoreEntity.MAX_POWER));p.translate(0,0,.05);quad(p,b,"blocks/metalbase",150,0);p.popPose();}p.popPose();}
            p.popPose();
        }
    }
}
