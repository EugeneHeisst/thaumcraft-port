package dev.eugheist.tc4modern;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
final class VillageClient {
    static int rendered;
    @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(VillageContent.WIZARD.get(),Renderer::new);e.registerEntityRenderer(VillageContent.BANKER.get(),Renderer::new);}
    static final class Renderer extends MobRenderer<ThaumcraftVillager,VillagerModel<ThaumcraftVillager>> {
        Renderer(EntityRendererProvider.Context context){super(context,new VillagerModel<>(TaintedClient.villager()),.5f);}
        @Override public ResourceLocation getTextureLocation(ThaumcraftVillager villager){rendered++;return AlchemyContent.id("textures/models/"+(villager.wizard()?"wizard":"moneychanger")+".png");}
        @Override protected void scale(ThaumcraftVillager villager,com.mojang.blaze3d.vertex.PoseStack pose,float partial){float scale=.9375f*villager.getAgeScale();pose.scale(scale,scale,scale);}
    }
}
