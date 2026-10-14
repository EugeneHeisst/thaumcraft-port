package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class PrimalArrowsClient {
 static int rendered;
 @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->{var bow=PrimalArrows.bow();net.minecraft.client.renderer.item.ItemProperties.register(bow,AlchemyContent.id("pull"),(s,l,entity,seed)->entity!=null&&entity.isUsingItem()&&entity.getUseItem()==s?s.getUseDuration(entity)-entity.getUseItemRemainingTicks():0);});}
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(PrimalArrows.ENTITY.get(),Renderer::new);}
 static final class Renderer extends EntityRenderer<PrimalArrows.ArrowEntity> {
  static final ResourceLocation ARROW=ResourceLocation.withDefaultNamespace("textures/entity/projectiles/arrow.png"),WISP=AlchemyContent.id("textures/misc/wisp.png");static final int[] COLORS={0xffff7e,16727041,37119,40960,0xeeccff,0x555577};
  Renderer(EntityRendererProvider.Context c){super(c);}
  public ResourceLocation getTextureLocation(PrimalArrows.ArrowEntity arrow){return ARROW;}
  @Override public void render(PrimalArrows.ArrowEntity arrow,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){rendered++;float alpha=(100-arrow.groundTicks)/100f;pose.pushPose();pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(partial,arrow.yRotO,arrow.getYRot())-90));pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(partial,arrow.xRotO,arrow.getXRot())));float shake=arrow.shakeTime-partial;if(shake>0)pose.mulPose(Axis.ZP.rotationDegrees(-Mth.sin(shake*3)*shake));pose.mulPose(Axis.XP.rotationDegrees(45));pose.scale(.05625f,.05625f,.05625f);pose.translate(-4,0,0);var body=buffers.getBuffer(NodeRenderTypes.additive(ARROW));int white=Math.max(0,Math.min(255,(int)(alpha*255)))<<24|0xffffff;
   quad(pose,body,new float[][]{{-7,-2,-2,0,.15625f},{-7,-2,2,.15625f,.15625f},{-7,2,2,.15625f,.3125f},{-7,2,-2,0,.3125f}},white,light);for(int i=0;i<4;i++){pose.mulPose(Axis.XP.rotationDegrees(90));quad(pose,body,new float[][]{{-8,-2,0,0,0},{8,-2,0,.5f,0},{8,2,0,.5f,.15625f},{-8,2,0,0,.15625f}},white,light);}pose.popPose();
   pose.pushPose();pose.mulPose(entityRenderDispatcher.cameraOrientation());int frame=arrow.tickCount%16;float u=frame%4*.25f,v=frame/4*.25f;var glow=buffers.getBuffer(arrow.type()<5?NodeRenderTypes.additive(WISP):RenderType.entityTranslucent(WISP));int color=(white&0xff000000)|COLORS[arrow.type()];quad(pose,glow,new float[][]{{-.5f,-.5f,0,u+.2499f,v+.2499f},{.5f,-.5f,0,u,v+.2499f},{.5f,.5f,0,u,v},{-.5f,.5f,0,u+.2499f,v}},color,LightTexture.FULL_BRIGHT);pose.popPose();super.render(arrow,yaw,partial,pose,buffers,light);
  }
  static void quad(PoseStack p,VertexConsumer out,float[][] vertices,int color,int light){for(var v:vertices)out.addVertex(p.last().pose(),v[0],v[1],v[2]).setColor(color).setUv(v[3],v[4]).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p.last(),0,0,1);}
 }
}
