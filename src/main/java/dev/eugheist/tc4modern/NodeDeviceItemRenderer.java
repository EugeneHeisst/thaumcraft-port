package dev.eugheist.tc4modern;
/** Item host uses the same unchanged original OBJ as the placed device. */
public final class NodeDeviceItemRenderer extends net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer {
 private final NodeDeviceVisuals model=new NodeDeviceVisuals();
 public NodeDeviceItemRenderer(){super(net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher(),net.minecraft.client.Minecraft.getInstance().getEntityModels());}
 @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){
  model.render(stack.is(TC4Modern.NODE_CONVERTER_ITEM.get()),0,stack.is(TC4Modern.NODE_STABILIZER_ADV_ITEM.get())?2:0,0,poses,buffers,light,overlay,true);
 }
}
