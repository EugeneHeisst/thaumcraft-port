package dev.eugheist.tc4modern;

/** Original vanilla ModelSlime(16)/(0) meshes and TC4 tslime texture, with original squish scaling. */
public final class ThaumicSlimeRenderer extends net.minecraft.client.renderer.entity.MobRenderer<thaumcraft.common.entities.monster.EntityThaumicSlime,net.minecraft.client.model.SlimeModel<thaumcraft.common.entities.monster.EntityThaumicSlime>> {
    public ThaumicSlimeRenderer(net.minecraft.client.renderer.entity.EntityRendererProvider.Context context){
        super(context,new net.minecraft.client.model.SlimeModel<>(context.bakeLayer(net.minecraft.client.model.geom.ModelLayers.SLIME)),.25f);
        addLayer(new net.minecraft.client.renderer.entity.layers.SlimeOuterLayer<>(this,context.getModelSet()));
    }
    @Override public net.minecraft.resources.ResourceLocation getTextureLocation(thaumcraft.common.entities.monster.EntityThaumicSlime entity){return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/models/tslime.png");}
    @Override protected void scale(thaumcraft.common.entities.monster.EntityThaumicSlime entity,com.mojang.blaze3d.vertex.PoseStack poses,float partial){
        float size=(float)Math.sqrt(entity.getSlimeSize());
        float amount=(entity.previousSquish+(entity.squish-entity.previousSquish)*partial)/(size*.25f+1),inverse=1/(amount+1);
        poses.scale(inverse*size+.1f,size/inverse+.1f,inverse*size+.1f);
    }
}
