package dev.eugheist.tc4modern;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class BrainyClient {
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(BrainyContent.NORMAL.get(),Renderer::new);e.registerEntityRenderer(BrainyContent.GIANT.get(),Renderer::new);}
 static final class Renderer extends HumanoidMobRenderer<BrainyZombie,ZombieModel<BrainyZombie>>{
 Renderer(EntityRendererProvider.Context c){super(c,new ZombieModel<>(c.bakeLayer(CrabClient.ZOMBIE)),.5f);addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(c.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),new HumanoidModel<>(c.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),c.getModelManager()));}
 @Override public ResourceLocation getTextureLocation(BrainyZombie e){return AlchemyContent.id("textures/models/bzombie.png");}
 @Override protected void scale(BrainyZombie e,com.mojang.blaze3d.vertex.PoseStack pose,float partial){if(e.giant())pose.scale(e.anger(),e.anger(),e.anger());}
 }
}
