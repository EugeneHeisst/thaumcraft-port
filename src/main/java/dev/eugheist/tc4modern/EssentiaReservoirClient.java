package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** TileEssentiaReservoirRenderer: reservoir.obj turned to the connecting face, and the jar liquid at W3..W13 rising with the fill level in the fading aspect colour. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class EssentiaReservoirClient {
    static final ObjParts CAGE=new ObjParts(AlchemyContent.id("models/reservoir.obj"));static int rendered;
    @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(EssentiaReservoir.ENTITY.get(),Renderer::new);}
    @SubscribeEvent static void items(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent e){e.registerItem(new net.neoforged.neoforge.client.extensions.common.IClientItemExtensions(){
        final BlockEntityWithoutLevelRenderer renderer=new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()){
            /* renderInventoryBlock: the W2..W14 glass cube, then the empty tile's cage facing down */
            @Override public void renderByItem(net.minecraft.world.item.ItemStack stack,net.minecraft.world.item.ItemDisplayContext ctx,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
                Minecraft.getInstance().getBlockRenderer().renderSingleBlock(EssentiaReservoir.BLOCK.get().defaultBlockState(),pose,buffers,light,overlay);cage(Direction.DOWN,pose,buffers,light,overlay);}};
        @Override public BlockEntityWithoutLevelRenderer getCustomRenderer(){return renderer;}},EssentiaReservoir.ITEM.get());}
    static void cage(Direction facing,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
        pose.pushPose();pose.translate(.5,.5,.5);
        switch(facing){case DOWN->pose.mulPose(Axis.XP.rotationDegrees(-90));case UP->pose.mulPose(Axis.XP.rotationDegrees(90));case SOUTH->pose.mulPose(Axis.YP.rotationDegrees(180));case WEST->pose.mulPose(Axis.YP.rotationDegrees(90));case EAST->pose.mulPose(Axis.YP.rotationDegrees(-90));default->{}}
        pose.translate(0,0,-.5);CAGE.render("Cylinder001",RenderType.entityCutoutNoCull(AlchemyContent.id("textures/models/reservoir.png")),pose,buffers,light,overlay,0xFFFFFFFF);pose.popPose();}
    static final class Renderer implements BlockEntityRenderer<EssentiaReservoir.ReservoirEntity>{
        Renderer(BlockEntityRendererProvider.Context c){}
        @Override public void render(EssentiaReservoir.ReservoirEntity t,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
            rendered++;cage(t.facing,pose,buffers,light,overlay);
            if(t.displayAspect==null||t.essentia.visSize()==0)return;
            float level=t.essentia.visSize()/(float)256;float w3=3/16f,w13=13/16f,w10=10/16f;
            int color=(int)(.9f*255)<<24|(clamp(t.cr)<<16)|(clamp(t.cg)<<8)|clamp(t.cb);int bright=Math.max(200,light&0xFFFF)|(light&0xFFFF0000);
            EssentiaRenderers.box(pose,buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)),EssentiaRenderers.sprite("blocks/animatedglow"),w3,w3,w3,w13,w3+w10*level,w13,color,bright);
        }
        static int clamp(float v){return Math.max(0,Math.min(255,(int)(v*255)));}
    }
}
