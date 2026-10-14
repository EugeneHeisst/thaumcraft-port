package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT,bus=EventBusSubscriber.Bus.MOD)
public final class OuterPortalClient implements BlockEntityRenderer<OuterPortal.Portal> {
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerBlockEntityRenderer(OuterPortal.TYPE.get(),c->new OuterPortalClient());}
 @Override public int getViewDistance(){return 96;}
 @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(OuterPortal.Portal tile){return new net.minecraft.world.phys.AABB(tile.getBlockPos()).inflate(1);}
 @Override public void render(OuterPortal.Portal tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
  float scale=Math.max(0,(int)Math.min(5,tile.opencount+partial))/5f,sy=Math.max(0,(int)Math.min(30,tile.opencount+partial))/30f;
  float u=(System.nanoTime()/50000000L%16)/16f;
  var consumer=buffers.getBuffer(Types.PORTAL);var rotation=Minecraft.getInstance().gameRenderer.getMainCamera().rotation();
  float[][] vertices={{-1,-1,u,1},{-1,1,u+.0625f,1},{1,1,u+.0625f,0},{1,-1,u,0}};
  for(var v:vertices){var point=new org.joml.Vector3f(v[0],v[1],0).rotate(rotation);consumer.addVertex(pose.last().pose(),.5f+point.x*scale,.5f+point.y*sy,.5f+point.z*scale).setColor(-1).setUv(v[2],v[3]).setOverlay(overlay).setLight(220).setNormal(pose.last(),0,0,-1);}
 }
 private abstract static class Types extends RenderType {
  Types(){super("tc4_outer_portal",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
  // Buffered cutout banners may flush later: keep portal depth to occlude those behind it.
  // The entity shader discards transparent texels, preserving the circular edge.
  static final RenderType PORTAL=create("tc4_outer_portal",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(AlchemyContent.id("textures/misc/eldritch_portal.png"),false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(COLOR_DEPTH_WRITE).createCompositeState(false));
 }
}
