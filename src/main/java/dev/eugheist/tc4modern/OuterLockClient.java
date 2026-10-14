package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Original lock's four retracting arms and five-block portal plane. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
final class OuterLockClient implements BlockEntityRenderer<OuterGate.Lock> {
 final ModelPart cube;
 OuterLockClient(){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(0,0).addBox(-8,-8,-8,16,16,16),PartPose.ZERO);cube=LayerDefinition.create(mesh,64,64).bakeRoot().getChild("cube");}
 @SubscribeEvent static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(OuterGate.TYPE.get(),c->new OuterLockClient());}
 @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(OuterGate.Lock lock){return new net.minecraft.world.phys.AABB(lock.getBlockPos()).inflate(2.25);}
 @Override public int getViewDistance(){return 96;}
 @Override public void render(OuterGate.Lock lock,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){var mc=Minecraft.getInstance();var direction=lock.facing();float time=mc.getCameraEntity()==null?partial:mc.getCameraEntity().tickCount+partial;var texture=AlchemyContent.id("textures/models/eldritch_cube.png");pose.pushPose();pose.translate(.5,.5,.5);for(int u=0;u<4;u++){pose.pushPose();pose.mulPose(new org.joml.Quaternionf().rotationAxis((float)Math.toRadians(90*u),direction.getStepX(),0,direction.getStepZ()));for(int a=1;a<5-(lock.count+u*5)/20;a++){pose.pushPose();pose.translate(0,.25+.5*a,0);float width=(float)Math.sin((time+a*10+u*20)/20)*.1f;if(a==1||a==4)width=width/2+.2f;pose.scale(.5f+width,.5f,.5f+width);cube.render(pose,buffers.getBuffer(RenderType.entitySolid(texture)),light,overlay);pose.popPose();}pose.popPose();}pose.popPose();
  if(lock.count>=0){pose.pushPose();pose.translate(.5+direction.getStepX()*.525,.285,.5+direction.getStepZ()*.525);pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(switch(direction){case EAST->90;case WEST->270;case NORTH->180;default->0;}));mc.getItemRenderer().renderStatic(new net.minecraft.world.item.ItemStack(OuterItems.TABLET.get()),net.minecraft.world.item.ItemDisplayContext.GROUND,light,overlay,pose,buffers,lock.getLevel(),0);pose.popPose();}
  var viewer=mc.getCameraEntity();var pos=lock.getBlockPos();boolean near=viewer!=null&&viewer.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<512;var out=buffers.getBuffer(near?CrimsonRenderers.Types.PORTAL:RenderType.entityCutoutNoCull(AlchemyContent.id("textures/misc/particlefield32.png")));float[][] corners={{-2,3,1,1},{-2,-2,1,0},{3,-2,0,0},{3,3,0,1}};for(var v:corners){float x=direction.getAxis()==net.minecraft.core.Direction.Axis.X?.5f:v[0],z=direction.getAxis()==net.minecraft.core.Direction.Axis.X?v[0]:.5f;out.addVertex(pose.last().pose(),x,v[1],z);if(!near)out.setColor(-1).setUv(v[2],v[3]).setOverlay(overlay).setLight(180).setNormal(pose.last(),direction.getStepX(),0,direction.getStepZ());}
 }
}
