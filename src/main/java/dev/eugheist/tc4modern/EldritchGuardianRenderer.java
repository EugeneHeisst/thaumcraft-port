package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class EldritchGuardianRenderer extends MobRenderer<EldritchGuardian,EldritchGuardianModel<EldritchGuardian>> {
 static final ResourceLocation TEXTURE=AlchemyContent.id("textures/models/eldritch_guardian.png");static int rendered;
 @SubscribeEvent static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(WarpEntities.ELDRITCH_GUARDIAN.get(),EldritchGuardianRenderer::new);}
 public EldritchGuardianRenderer(EntityRendererProvider.Context context){super(context,new EldritchGuardianModel<>(),.5f);}
 static float opacity(double distance,boolean hard){return distance<256?.6f:(float)(1-Math.min((hard?576:1024)-256,distance-256)/((hard?576:1024)-256))*.6f;}
 @Override public void render(EldritchGuardian g,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){var eye=Minecraft.getInstance().getCameraEntity();float alpha=g.level().dimension()==OuterDimension.KEY?1:eye==null?.6f:opacity(g.distanceToSqr(eye),g.level().getDifficulty()==Difficulty.HARD);rendered++;super.render(g,yaw,partial,pose,type->type==Body.TYPE?new MindSpiderRenderer.AlphaConsumer(buffers.getBuffer(type),alpha):buffers.getBuffer(type),light);}
 @Override protected RenderType getRenderType(EldritchGuardian g,boolean visible,boolean translucent,boolean glowing){return Body.TYPE;}
 @Override public ResourceLocation getTextureLocation(EldritchGuardian g){return TEXTURE;}
 abstract static class Body extends RenderType {
  Body(){super("guardian",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
  static final RenderType TYPE=create("tc4_guardian",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,4096,false,true,CompositeState.builder().setShaderState(new ShaderStateShard(()->MindSpiderRenderer.bodyShader)).setTextureState(new TextureStateShard(TEXTURE,false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
 }
}
