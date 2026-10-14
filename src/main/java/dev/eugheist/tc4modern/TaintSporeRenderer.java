package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import thaumcraft.client.renderers.models.entities.ModelTaintSpore;
import thaumcraft.common.entities.monster.EntityTaintSpore;

/** Modern renderer bridge for original RenderTaintSpore negative scaling and pulse. */
public final class TaintSporeRenderer extends MobRenderer<EntityTaintSpore,ModelTaintSpore> {
    private static final ResourceLocation TEXTURE=ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/models/taint_spore.png");
    public TaintSporeRenderer(EntityRendererProvider.Context context){super(context,new ModelTaintSpore(ModelTaintSpore.layer().bakeRoot()),.25f);}
    @Override public ResourceLocation getTextureLocation(EntityTaintSpore entity){return TEXTURE;}
    @Override protected void scale(EntityTaintSpore entity,PoseStack pose,float partial){
        float size=entity.displaySize;if(size<entity.getSporeSize())size+=.02f*partial;
        float pulse=.025f*net.minecraft.util.Mth.sin(entity.tickCount*.075f);
        pose.scale(-.12f*size-pulse,-.12f*size+pulse,-.12f*size-pulse);
    }
    @Override protected float getFlipDegrees(EntityTaintSpore entity){return 0;}
    private BlockPos lightPos(EntityTaintSpore entity){return BlockPos.containing(entity.getX(),entity.getBoundingBox().minY+entity.getBbHeight()*.66,entity.getZ());}
    @Override protected int getBlockLightLevel(EntityTaintSpore entity,BlockPos pos){return entity instanceof thaumcraft.common.entities.monster.EntityTaintSporeSwarmer?entity.level().getBrightness(net.minecraft.world.level.LightLayer.BLOCK,lightPos(entity)):15;}
    @Override protected int getSkyLightLevel(EntityTaintSpore entity,BlockPos pos){return entity instanceof thaumcraft.common.entities.monster.EntityTaintSporeSwarmer?entity.level().getBrightness(net.minecraft.world.level.LightLayer.SKY,lightPos(entity)):15;}
}
