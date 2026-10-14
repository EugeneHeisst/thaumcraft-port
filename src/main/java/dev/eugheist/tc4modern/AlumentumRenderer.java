package dev.eugheist.tc4modern;

/** Original RenderAlumentum draws no mesh; its visual comes from the projectile's particle trail. */
public final class AlumentumRenderer extends net.minecraft.client.renderer.entity.EntityRenderer<thaumcraft.common.entities.projectile.EntityAlumentum> {
    public AlumentumRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context){super(context);shadowRadius=.1f;}
    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(thaumcraft.common.entities.projectile.EntityAlumentum entity){return net.minecraft.client.resources.DefaultPlayerSkin.getDefaultTexture();}
}
