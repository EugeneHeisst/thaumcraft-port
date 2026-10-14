package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
/** Original TileNodeStabilizerRenderer/TileNodeConverterRenderer transforms over unchanged OBJ groups. */
public final class NodeDeviceVisuals {
 private final ObjParts mesh=new ObjParts(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"models/node_stabilizer.obj"));
 private static ResourceLocation texture(String name){return ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/models/"+name+".png");}
 private static int brightness(float amount,float ticks){return 50+(int)(170f*(amount*((float)Math.sin(ticks/3f)*.1f+.9f)));}
 public void render(boolean converter,int count,int status,float ticks,PoseStack poses,MultiBufferSource buffers,int light,int overlay){
  render(converter,count,status,ticks,poses,buffers,light,overlay,false);
 }
 public void render(boolean converter,int count,int status,float ticks,PoseStack poses,MultiBufferSource buffers,int light,int overlay,boolean inventory){
  String name=converter?"node_converter":"node_stabilizer";
  var base=texture(name);var over=texture(name+"_over");
  float v=converter?Math.min(50,count)/137f:count/100f;
  int tint=converter?(status==2?0xFFFF004C:status==1?0xFFFF9919:0xFF7FFF7F):(status==2?0xFFFF3333:0xFFFFFFFF);
  poses.pushPose();poses.translate(.5,converter?1:0,.5);poses.mulPose(Axis.XP.rotationDegrees(converter?90:-90));
  mesh.render("lock",base,poses,buffers,light,overlay,0xFFFFFFFF);
  if(converter)mesh.render("lock",over,poses,buffers,inventory?light:brightness(v*2.5f,ticks),overlay,tint);
  for(int a=0;a<4;a++){
   poses.pushPose();poses.mulPose(Axis.ZP.rotationDegrees(90*a));poses.mulPose(Axis.YP.rotationDegrees(45));poses.translate(0,0,v);
   mesh.render("piston",base,poses,buffers,light,overlay,0xFFFFFFFF);
   mesh.render("piston",over,poses,buffers,inventory?light:brightness(converter?v*2.5f:count/37f,ticks+a*5),overlay,tint);
   poses.popPose();
  }
  poses.popPose();
 }
 public static int stabilizerCount(int count,boolean nodeAbove,boolean powered){
  if(nodeAbove&&!powered){if(count<37)++count;}else if(count>0)--count;
  return count;
 }
 public static int converterCount(int count,int status,boolean powered){
  if(status==0||!powered){if(count>0)--count;}else if(count<1000)++count;
  return Math.min(count,1000);
 }
}
