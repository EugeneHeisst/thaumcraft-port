package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.EntityFallingTaint;
/** Renders the falling entity with the imported original crust texture. */
public final class TaintFallingRenderer extends EntityRenderer<EntityFallingTaint>{
    private final net.minecraft.client.renderer.block.BlockRenderDispatcher blocks;
    public TaintFallingRenderer(EntityRendererProvider.Context context){super(context);blocks=context.getBlockRenderDispatcher();shadowRadius=.5f;}
    @Override public void render(EntityFallingTaint entity,float yaw,float partial,PoseStack pose,MultiBufferSource buffer,int light){
        pose.pushPose();pose.translate(-.5,0,-.5);
        blocks.renderSingleBlock(entity.blockState(),pose,buffer,light,OverlayTexture.NO_OVERLAY);
        pose.popPose();super.render(entity,yaw,partial,pose,buffer,light);
    }
    @Override public ResourceLocation getTextureLocation(EntityFallingTaint entity){return TextureAtlas.LOCATION_BLOCKS;}
}
