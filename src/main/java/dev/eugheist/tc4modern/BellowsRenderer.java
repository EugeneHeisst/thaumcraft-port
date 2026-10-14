package dev.eugheist.tc4modern;
public final class BellowsRenderer implements net.minecraft.client.renderer.blockentity.BlockEntityRenderer<BellowsEntity> {
    private final BellowsModelRenderer model=new BellowsModelRenderer();
    public BellowsRenderer(net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context context){}
    @Override public void render(BellowsEntity tile,float partial,com.mojang.blaze3d.vertex.PoseStack poses,net.minecraft.client.renderer.MultiBufferSource buffers,int light,int overlay){model.render(tile.bellows.inflation,tile.bellows.orientation,poses,buffers,light,overlay);}
}
