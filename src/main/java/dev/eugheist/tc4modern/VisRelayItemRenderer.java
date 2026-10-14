package dev.eugheist.tc4modern;
/** Inventory/hand submission of the same original relay OBJ as the world block. */
public final class VisRelayItemRenderer extends net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer {
 private final VisRelayRenderer renderer=new VisRelayRenderer(null);
 private VisRelayEntity tile;
 public VisRelayItemRenderer(){super(net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher(),net.minecraft.client.Minecraft.getInstance().getEntityModels());}
 @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){
  if(tile==null)tile=new VisRelayEntity(TC4Modern.VIS_RELAY_ENTITY.get(),net.minecraft.core.BlockPos.ZERO,TC4Modern.VIS_RELAY.get().defaultBlockState());
  // BlockMetalDeviceRenderer metadata 14: scale 1.5 about the icon centre, lifted a quarter block.
  poses.pushPose();poses.translate(.5,.5,.5);poses.scale(1.5f,1.5f,1.5f);poses.translate(-.5,-.25,-.5);
  renderer.render(tile,0,poses,buffers,light,overlay);
  poses.popPose();
 }
}
