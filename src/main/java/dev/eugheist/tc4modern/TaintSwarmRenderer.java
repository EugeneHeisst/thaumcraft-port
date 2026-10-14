package dev.eugheist.tc4modern;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
/** Original RenderTaintSwarm has no mesh or shadow; its appearance consists of particles. */
public final class TaintSwarmRenderer extends EntityRenderer<thaumcraft.common.entities.monster.EntityTaintSwarm>{
    public TaintSwarmRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=0;}
    @Override public void render(thaumcraft.common.entities.monster.EntityTaintSwarm entity,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffer,int light){}
    @Override public ResourceLocation getTextureLocation(thaumcraft.common.entities.monster.EntityTaintSwarm entity){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
}
