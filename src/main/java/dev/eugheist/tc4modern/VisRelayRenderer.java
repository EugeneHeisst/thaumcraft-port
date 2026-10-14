package dev.eugheist.tc4modern;
import com.mojang.math.Axis;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.blockentity.*;
/** Original TileVisRelayRenderer transforms and unchanged OBJ groups. */
public final class VisRelayRenderer implements BlockEntityRenderer<VisRelayEntity>{
 private final ObjParts mesh=new ObjParts(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"models/vis_relay.obj"));
 private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/models/vis_relay.png");
 public VisRelayRenderer(BlockEntityRendererProvider.Context context){}
 public void render(VisRelayEntity tile,float partial,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){
  poses.pushPose();poses.translate(.5,.5,.5);
  switch(tile.relay.orientation){case 0->poses.mulPose(Axis.XP.rotationDegrees(-90));case 1->poses.mulPose(Axis.XP.rotationDegrees(90));case 3->poses.mulPose(Axis.YP.rotationDegrees(180));case 4->poses.mulPose(Axis.YP.rotationDegrees(90));case 5->poses.mulPose(Axis.YP.rotationDegrees(-90));}
  poses.mulPose(Axis.XP.rotationDegrees(180));poses.mulPose(Axis.ZP.rotationDegrees(45));
  poses.pushPose();poses.scale(.75f,.75f,.75f);poses.translate(0,0,-.16);
  mesh.render("RingBase",TEXTURE,poses,buffers,light,overlay,0xFFFFFFFF);poses.popPose();
  mesh.render("RingFloat",TEXTURE,poses,buffers,light,overlay,0xFFFFFFFF);
  int color=0xFFFFFFFF;
  if(tile.relay.color>=0&&tile.relay.color<6){int rgb=thaumcraft.common.tiles.VisRelayState.COLORS[tile.relay.color];color=0xFF000000|Math.min(255,(rgb>>16&255)*255/200)<<16|Math.min(255,(rgb>>8&255)*255/200)<<8|Math.min(255,(rgb&255)*255/200);}
  var player=net.minecraft.client.Minecraft.getInstance().player;float ticks=(player==null?0:player.tickCount)+partial;
  int brightness=(tile.parent()!=null?50:0)+(int)(150*((float)Math.sin(ticks/2)*.05f+.95f));
  mesh.render("Crystal",TEXTURE,poses,buffers,brightness,overlay,color);poses.popPose();

 }
}
