package dev.eugheist.tc4modern;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class OuterCrystalClient {
 static final ObjParts MODEL=new ObjParts(AlchemyContent.id("models/vcrystal.obj"));
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(OuterCrystal.TYPE.get(),c->new BlockEntityRenderer<>(){
  @Override public void render(OuterCrystal.Crystal tile,float partial,com.mojang.blaze3d.vertex.PoseStack pose,MultiBufferSource buffers,int light,int overlay){pose.pushPose();pose.translate(.5,.5,.5);switch(tile.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING)){case DOWN->pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));case UP->pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));case NORTH->pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));case WEST->pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));case EAST->pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));default->{}}pose.translate(0,0,-.5);pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90*(tile.hashCode()%4)));MODEL.render("Base",RenderType.entityCutoutNoCull(AlchemyContent.id("textures/blocks/crust.png")),pose,buffers,light,overlay,0xFFFFFFFF);var player=net.minecraft.client.Minecraft.getInstance().player;float shade=(float)Math.sin((player==null?0:player.tickCount)/6f)*.075f+.925f;MODEL.render("Crystal",RenderType.entityTranslucent(AlchemyContent.id("textures/models/vcrystal.png")),pose,buffers,(int)(210*shade),overlay,0xB3FFFFFF);pose.popPose();}
 });}
}
