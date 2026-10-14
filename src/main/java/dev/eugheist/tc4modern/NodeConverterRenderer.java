package dev.eugheist.tc4modern;
public final class NodeConverterRenderer implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<NodeConverterEntity> {
 private final NodeDeviceVisuals model=new NodeDeviceVisuals();
 public NodeConverterRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context){}
 @Override public void render(NodeConverterEntity tile,float partial,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){
  var player=net.minecraft.client.Minecraft.getInstance().player;
  model.render(true,tile.converter.count,tile.converter.status,player==null?0:player.tickCount,poses,buffers,light,overlay);
 }
}
