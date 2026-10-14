package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
/** Original reverse sixteen-frame animation and damage-dependent portal distortion. */
public final class CultistPortalRenderer extends EntityRenderer<CultistPortal> {
 public CultistPortalRenderer(EntityRendererProvider.Context context){super(context);shadowRadius=.1f;shadowStrength=.5f;}
 @Override public net.minecraft.resources.ResourceLocation getTextureLocation(CultistPortal e){return AlchemyContent.id("textures/misc/cultist_portal.png");}
 @Override public void render(CultistPortal e,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
  float age=e.tickCount+partial,growth=Math.min(50,(int)age),sy=1.5f;
  if(e.hurtTime>0){float d=(float)Math.sin(Math.toRadians(e.hurtTime*72));sy-=d/4;growth+=6*d;}
  if(e.pulse>0){float d=(float)Math.sin(Math.toRadians(e.pulse*36));sy+=d/4;growth+=12*d;}
  float sx=growth/50*1.3f,m=(1-e.getHealth()/e.getMaxHealth())/3,bob=(float)Math.sin(e.tickCount/(5-12*m))*m+m,bob2=(float)Math.sin(e.tickCount/(6-15*m))*m+m;
  sy-=bob/4;sx-=bob2/3;float u=(15-System.nanoTime()/50000000L%16)/16f;
  var out=buffers.getBuffer(Types.PORTAL);var rotation=entityRenderDispatcher.cameraOrientation();
  float[][] vertices={{-1,-1,u+.0625f,0},{-1,1,u+.0625f,1},{1,1,u,1},{1,-1,u,0}};
  for(var v:vertices){var p=new org.joml.Vector3f(v[0],v[1],0).rotate(rotation);out.addVertex(pose.last().pose(),p.x*sx,1.5f+p.y*sy,p.z*sx).setColor(255,255,255,(int)((1-bob)*255)).setUv(v[2],v[3]).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(220).setNormal(pose.last(),0,0,-1);}
  super.render(e,yaw,partial,pose,buffers,light);
 }
 private abstract static class Types extends RenderType {
  Types(){super("tc4_cultist_portal",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,()->{},()->{});}
  static final RenderType PORTAL=create("tc4_cultist_portal",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(AlchemyContent.id("textures/misc/cultist_portal.png"),false,false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
 }
}
