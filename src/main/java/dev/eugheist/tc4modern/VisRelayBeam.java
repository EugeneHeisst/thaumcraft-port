package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** FXBeamPower crossed strips and animated endpoint flare with original dimensions and UVs. */
final class VisRelayBeam {
 private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/beam1.png");
 static void render(VisRelayEntity tile,int px,int py,int pz,int parentOrientation,thaumcraft.client.fx.beams.BeamPowerState state,float partial,PoseStack poses,MultiBufferSource buffers){
  var mc=Minecraft.getInstance();if(mc.player==null)return;
  if(mc.gameRenderer.getMainCamera().getPosition().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(tile.getBlockPos()))>(Minecraft.useFancyGraphics()?50:25))return;
  var d=Direction.from3DDataValue(tile.relay.orientation);
  double tx=.5-d.getStepX()*.05,ty=.5-d.getStepY()*.05,tz=.5-d.getStepZ()*.05;
  double sx=px-tile.x()+.5,sy=py-tile.y()+.5,sz=pz-tile.z()+.5;
  if(parentOrientation>=0){var pd=Direction.from3DDataValue(parentOrientation);sx-=pd.getStepX()*.05;sy-=pd.getStepY()*.05;sz-=pd.getStepZ()*.05;}
  double dx=sx-tx,dy=sy-ty,dz=sz-tz;float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
  float yaw=(float)Math.toDegrees(Math.atan2(dx,dz)),pitch=(float)Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)));
  var helmet=mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
  boolean reveals=helmet.getItem() instanceof thaumcraft.api.nodes.IRevealer;
  float opacity=state.opacity;
  float ticks=mc.player.tickCount+partial,slide=-ticks*.2f-(float)Math.floor(-ticks*.1f);
  poses.pushPose();poses.translate(sx,sy,sz);
  poses.mulPose(Axis.XP.rotationDegrees(90));poses.mulPose(Axis.ZN.rotationDegrees(180+yaw));poses.mulPose(Axis.XP.rotationDegrees(pitch));
  var out=buffers.getBuffer(NodeRenderTypes.additive(TEXTURE));
  for(int strip=0;strip<2;strip++){
   poses.mulPose(Axis.YP.rotationDegrees(90));float v=-1+slide+strip/3f;
   vertex(out,poses,state,-.105f,length,0,1,length+v,opacity*(reveals?1:.1f));
   vertex(out,poses,state,-.105f,0,0,1,v,opacity*(reveals?1:.1f));
   vertex(out,poses,state,.105f,0,0,0,v,opacity*(reveals?1:.1f));
   vertex(out,poses,state,.105f,length,0,0,length+v,opacity*(reveals?1:.1f));
  }
  poses.popPose();poses.pushPose();poses.translate(tx,ty,tz);poses.mulPose(mc.getEntityRenderDispatcher().cameraOrientation());
  var flare=buffers.getBuffer(NodeRenderTypes.wisp());float size=.66f*opacity,u=(state.age%16)/16f;
  float[][] corners={{-size,-size,u+.0624375f,.375f-.0000625f},{-size,size,u+.0624375f,.3125f},{size,size,u,.3125f},{size,-size,u,.375f-.0000625f}};
  for(var c:corners)vertex(flare,poses,state,c[0],c[1],0,c[2],c[3],opacity*(reveals?1:.2f));
  poses.popPose();
 }
 private static void vertex(com.mojang.blaze3d.vertex.VertexConsumer out,PoseStack poses,thaumcraft.client.fx.beams.BeamPowerState state,float x,float y,float z,float u,float v,float alpha){
  out.addVertex(poses.last(),x,y,z).setColor(state.red,state.green,state.blue,alpha).setUv(u,v);
 }
}
