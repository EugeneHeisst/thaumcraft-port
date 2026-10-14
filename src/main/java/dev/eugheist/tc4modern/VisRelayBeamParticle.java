package dev.eugheist.tc4modern;

import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.*;
/** ParticleEngine ownership lets the original beam lifetime outlive relay disconnection/removal. */
final class VisRelayBeamParticle extends Particle {
 private final VisRelayEntity tile;
 private final thaumcraft.client.fx.beams.BeamPowerState state=new thaumcraft.client.fx.beams.BeamPowerState();
 private int px,py,pz,orientation=-1;
 private VisRelayBeamParticle(VisRelayEntity tile){
  super((net.minecraft.client.multiplayer.ClientLevel)tile.getLevel(),tile.x()+.5,tile.y()+.5,tile.z()+.5);
  this.tile=tile;hasPhysics=false;capture();
 }
 private void capture(){var parent=tile.parent();if(parent==null)return;px=parent.x();py=parent.y();pz=parent.z();orientation=parent instanceof VisRelayEntity relay?relay.relay.orientation:-1;}
 static void update(VisRelayEntity tile){
  if(tile.parent()==null)return;
  if(tile.clientBeam instanceof VisRelayBeamParticle beam&&beam.isAlive()){
   beam.capture();beam.state.update(tile.relay.pulse>0,tile.relay.red,tile.relay.green,tile.relay.blue);
  }else{
   var beam=new VisRelayBeamParticle(tile);tile.clientBeam=beam;Minecraft.getInstance().particleEngine.add(beam);
  }
 }
 @Override public void tick(){state.tick();if(state.dead)remove();}
 @Override public ParticleRenderType getRenderType(){return ParticleRenderType.CUSTOM;}
 @Override public void render(VertexConsumer unused,net.minecraft.client.Camera camera,float partial){
  var poses=new PoseStack();var eye=camera.getPosition();poses.translate(tile.x()-eye.x,tile.y()-eye.y,tile.z()-eye.z);
  var buffers=Minecraft.getInstance().renderBuffers().bufferSource();
  VisRelayBeam.render(tile,px,py,pz,orientation,state,partial,poses,buffers);
  buffers.endBatch(NodeRenderTypes.additive(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/beam1.png")));
  buffers.endBatch(NodeRenderTypes.wisp());
 }
 @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(float partial){return new net.minecraft.world.phys.AABB(x,y,z,px+.5,py+.5,pz+.5).inflate(.6);}
}
