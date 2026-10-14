package dev.eugheist.tc4modern;
public final class NodeStabilizerRenderer implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<NodeStabilizerEntity> {
 @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(NodeStabilizerEntity tile){return new net.minecraft.world.phys.AABB(tile.getBlockPos()).expandTowards(0,1,0);}
 private final NodeDeviceVisuals model=new NodeDeviceVisuals();
 public NodeStabilizerRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context){}
 @Override public void render(NodeStabilizerEntity tile,float partial,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){
  var player=net.minecraft.client.Minecraft.getInstance().player;
  model.render(false,tile.count,tile.lock(),player==null?0:player.tickCount,poses,buffers,light,overlay);
  if(tile.count>0){
   poses.pushPose();poses.translate(.5,1.5,.5);poses.mulPose(net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
   var b=buffers.getBuffer(NodeRenderTypes.additive(AlchemyContent.id("textures/misc/node_bubble.png")));var m=poses.last().pose();
   float alpha=tile.count/37f*((float)Math.sin((player==null?0:player.tickCount)/8f)*.1f+.5f);int color=((int)(alpha*255)<<24)|(tile.lock()==2?0xFF4444:0xFFFFFF);
   b.addVertex(m,-.9f,-.9f,0).setUv(0,1).setColor(color);b.addVertex(m,.9f,-.9f,0).setUv(1,1).setColor(color);b.addVertex(m,.9f,.9f,0).setUv(1,0).setColor(color);b.addVertex(m,-.9f,.9f,0).setUv(0,0).setColor(color);poses.popPose();
  }
 }
}
