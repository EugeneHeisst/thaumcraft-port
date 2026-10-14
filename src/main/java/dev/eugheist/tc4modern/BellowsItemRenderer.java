package dev.eugheist.tc4modern;
public final class BellowsItemRenderer extends net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer {
    private final BellowsModelRenderer model=new BellowsModelRenderer();
    public BellowsItemRenderer(){super(net.minecraft.client.Minecraft.getInstance().getBlockEntityRenderDispatcher(),net.minecraft.client.Minecraft.getInstance().getEntityModels());}
    @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext context,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){
        var player=net.minecraft.client.Minecraft.getInstance().player;
        float scale=player==null?.7f:net.minecraft.util.Mth.sin(player.tickCount/8f)*.3f+.7f;
        // BlockWoodenDeviceRenderer metadata 0: turned 90 degrees about the centre; the pumping is original too.
        poses.pushPose();poses.translate(.5,.5,.5);poses.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90));poses.translate(-.5,-.5,-.5);
        model.render(scale,2,poses,buffers,light,overlay);
        poses.popPose();
    }
}
