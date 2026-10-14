package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SpiderModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.common.entities.monster.EntityTaintSpider;

/** Modern vanilla spider model bridge with original tainted textures and scaling. */
public final class TaintSpiderRenderer extends MobRenderer<EntityTaintSpider,SpiderModel<EntityTaintSpider>> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/models/taint_spider.png");
    public TaintSpiderRenderer(EntityRendererProvider.Context context){
        super(context,new SpiderModel<>(thaumcraft.client.renderers.models.entities.TaintSpiderGeometry.layer().bakeRoot()),.5f);
        addLayer(new EyesLayer<EntityTaintSpider,SpiderModel<EntityTaintSpider>>(this){
            @Override public RenderType renderType(){return RenderType.eyes(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/models/taint_spider_eyes.png"));}
        });
    }
    @Override public ResourceLocation getTextureLocation(EntityTaintSpider entity){return TEXTURE;}
    @Override protected void scale(EntityTaintSpider entity,PoseStack pose,float partial){pose.scale(.4f,.5f,.4f);}
    @Override protected float getFlipDegrees(EntityTaintSpider entity){return 180;}
}
