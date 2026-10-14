package dev.eugheist.tc4modern;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class CrabClient {
 static final ModelLayerLocation ZOMBIE=new ModelLayerLocation(AlchemyContent.id("inhabited_zombie"),"main");
 static final ModelLayerLocation CRAB=new ModelLayerLocation(AlchemyContent.id("eldritch_crab"),"main");public static int crabs,zombies;
 @SubscribeEvent static void layers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions e){e.registerLayerDefinition(CRAB,EldritchCrabModel::layer);e.registerLayerDefinition(ZOMBIE,()->{var mesh=HumanoidModel.createMesh(net.minecraft.client.model.geom.builders.CubeDeformation.NONE,0);var root=mesh.getRoot();root.addOrReplaceChild("left_arm",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(40,16).mirror().addBox(-1,-2,-2,4,12,4),PartPose.offset(5,2,0));root.addOrReplaceChild("left_leg",net.minecraft.client.model.geom.builders.CubeListBuilder.create().texOffs(0,16).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(1.9f,12,0));return net.minecraft.client.model.geom.builders.LayerDefinition.create(mesh,64,64);});}
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(CrabContent.CRAB.get(),CrabRenderer::new);e.registerEntityRenderer(CrabContent.ZOMBIE.get(),InhabitedRenderer::new);}
 static final class CrabRenderer extends MobRenderer<EldritchCrab,EldritchCrabModel>{
  CrabRenderer(EntityRendererProvider.Context c){super(c,new EldritchCrabModel(c.bakeLayer(CRAB)),1);addLayer(new RenderLayer<>(this){@Override public void render(com.mojang.blaze3d.vertex.PoseStack p,MultiBufferSource buffers,int light,EldritchCrab e,float walk,float speed,float partial,float age,float yaw,float pitch){if(!e.isInvisible())getParentModel().renderToBuffer(p,buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id("textures/models/craboverlay.png"))),200,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);}});}
  @Override public ResourceLocation getTextureLocation(EldritchCrab e){return AlchemyContent.id("textures/models/crab.png");}
  @Override public void render(EldritchCrab e,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack p,MultiBufferSource b,int light){crabs++;super.render(e,yaw,partial,p,b,light);}
 }
 static final class InhabitedRenderer extends HumanoidMobRenderer<InhabitedZombie,ZombieModel<InhabitedZombie>>{
  InhabitedRenderer(EntityRendererProvider.Context c){super(c,new ZombieModel<>(c.bakeLayer(ZOMBIE)),.5f);addLayer(new HumanoidArmorLayer<>(this,new HumanoidModel<>(c.bakeLayer(ModelLayers.ZOMBIE_INNER_ARMOR)),new HumanoidModel<>(c.bakeLayer(ModelLayers.ZOMBIE_OUTER_ARMOR)),c.getModelManager()));}
  @Override public ResourceLocation getTextureLocation(InhabitedZombie e){return AlchemyContent.id("textures/models/czombie.png");}
  @Override public void render(InhabitedZombie e,float yaw,float partial,com.mojang.blaze3d.vertex.PoseStack p,MultiBufferSource b,int light){zombies++;super.render(e,yaw,partial,p,b,light);}
 }
}
