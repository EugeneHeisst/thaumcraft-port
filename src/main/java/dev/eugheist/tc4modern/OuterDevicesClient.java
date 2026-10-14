package dev.eugheist.tc4modern;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class OuterDevicesClient {
 static final ObjParts VENT=new ObjParts(AlchemyContent.id("models/crabvent.obj"));
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(OuterDevices.TYPE.get(),c->new BlockEntityRenderer<>(){
  @Override public void render(OuterDevices.Device tile,float partial,com.mojang.blaze3d.vertex.PoseStack pose,MultiBufferSource buffers,int light,int overlay){if(!tile.getBlockState().is(OuterDevices.VENT.get()))return;pose.pushPose();pose.translate(.5,.5,.5);switch(tile.facing()){case DOWN->pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));case UP->pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-90));case NORTH->pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180));case WEST->pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-90));case EAST->pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));default->{}}VENT.renderAll(RenderType.entityCutoutNoCull(AlchemyContent.id("textures/models/crabvent.png")),pose,buffers,light,overlay,0xFFFFFFFF);pose.popPose();}
 });}
 static void vent(OuterDevices.Device tile){var l=(net.minecraft.client.multiplayer.ClientLevel)tile.getLevel();var p=tile.getBlockPos();var d=tile.facing();var r=l.random;Minecraft.getInstance().particleEngine.add(new VentParticle(l,p.getX()+.5+.15-r.nextFloat()*.3+d.getStepX()/2.1,p.getY()+.5+.15-r.nextFloat()*.3+d.getStepY()/2.1,p.getZ()+.5+.15-r.nextFloat()*.3+d.getStepZ()/2.1,d.getStepX()/3.0+.1-r.nextFloat()*.2,d.getStepY()/3.0+.1-r.nextFloat()*.2,d.getStepZ()/3.0+.1-r.nextFloat()*.2));}
 static void zap(OuterDevices.TrapZap packet){var l=Minecraft.getInstance().level;if(l==null)return;var target=l.getEntity(packet.entity());if(target==null)return;var p=packet.from();NodeBoltParticle.spawn(l,p.getX()+.5,p.getY()+.5,p.getZ()+.5,target.getX(),target.getEyeY(),target.getZ());l.playLocalSound(p.getX()+.5,p.getY()+.5,p.getZ()+.5,FociContent.sound("zap"),net.minecraft.sounds.SoundSource.BLOCKS,.1f,1+l.random.nextFloat()*.2f,false);}
}
