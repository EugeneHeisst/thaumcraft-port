package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class WispClient {
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(WispContent.WISP.get(),Renderer::new);Wisp.fx=w->{WispMoteParticle.tick(w);int serial=w.getEntityData().get(Wisp.ZAP_SERIAL);if(serial!=w.zapSeen){w.zapSeen=serial;var target=w.level().getEntity(w.getEntityData().get(Wisp.ZAP_TARGET));if(target!=null)NodeBoltParticle.spawn((net.minecraft.client.multiplayer.ClientLevel)w.level(),w.getX(),w.getY()+.45,w.getZ(),target.getX(),target.getEyeY()-.7,target.getZ(),3,.4f,4,0,.03f);}};}
 public static int rendered;
 static final class Renderer extends EntityRenderer<Wisp>{
  static final ResourceLocation TEX=AlchemyContent.id("textures/misc/wisp.png"),PARTICLES=AlchemyContent.id("textures/misc/particles.png");
  Renderer(EntityRendererProvider.Context c){super(c);}
  @Override public ResourceLocation getTextureLocation(Wisp w){return TEX;}
  @Override public void render(Wisp w,float yaw,float partial,PoseStack p,MultiBufferSource buffers,int light){if(!w.isAlive())return;rendered++;p.pushPose();p.translate(0,.45,0);p.mulPose(entityRenderDispatcher.cameraOrientation());int frame=w.tickCount%16,color=w.aspect()==null?0:w.aspect().getColor();float r=(color>>16&255)/255f,g=(color>>8&255)/255f,b=(color&255)/255f;if(w.hurtTime>0){r=1;g*=.85f;b*=.85f;}quad(p,buffers,TEX,1,frame%4/4f,frame/4/4f,.25f,r,g,b);quad(p,buffers,PARTICLES,.4f+(float)Math.sin((w.tickCount+partial)/10)*.1f,frame/16f,5/16f,1/16f,1,1,1);p.popPose();super.render(w,yaw,partial,p,buffers,light);}
  static void quad(PoseStack p,MultiBufferSource buffers,ResourceLocation texture,float size,float u,float v,float step,float r,float g,float b){var out=buffers.getBuffer(NodeRenderTypes.additive(texture));var mat=p.last().pose();out.addVertex(mat,-size,-size,0).setUv(u+step,v+step).setColor(r,g,b,1);out.addVertex(mat,size,-size,0).setUv(u,v+step).setColor(r,g,b,1);out.addVertex(mat,size,size,0).setUv(u,v).setColor(r,g,b,1);out.addVertex(mat,-size,size,0).setUv(u+step,v).setColor(r,g,b,1);}
 }
}
