package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TaintacleClient {
 static int rendered;
 @SubscribeEvent static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){for(var t:java.util.List.of(TaintacleContent.NORMAL,TaintacleContent.SMALL,TaintacleContent.GIANT))e.registerEntityRenderer(t.get(),Renderer::new);}
 static final class Renderer extends EntityRenderer<Taintacle>{
  static final net.minecraft.resources.ResourceLocation TEX=AlchemyContent.id("textures/models/taintacle.png");
  final ModelPart root=cube(0,0,8),segment=cube(0,16,8),orb=cube(0,56,4),bulb=cube(0,32,12);
  Renderer(EntityRendererProvider.Context c){super(c);shadowRadius=.25f;}
  static ModelPart cube(int u,int v,int size){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(u,v).addBox(-size/2f,-size/2f,-size/2f,size,size,size),PartPose.ZERO);return LayerDefinition.create(mesh,64,64).bakeRoot();}
  @Override public net.minecraft.resources.ResourceLocation getTextureLocation(Taintacle e){return TEX;}
  @Override public void render(Taintacle e,float yaw,float partial,PoseStack p,MultiBufferSource buffers,int light){
   rendered++;p.pushPose();p.mulPose(Axis.YP.rotationDegrees(180-yaw));p.scale(-1,-1,1);
   float h=e.getBbHeight(),age=e.tickCount+partial,rise=Math.max(0,(h*10-age)/(h*10))*h;
   // RenderLiving used a 1.5-block model origin before the original ModelTaintacle offset.
   p.translate(0,-1.5+(h==3?.6:1.2)+rise,0);float size=h/3*(e.giant()?1.33f:1);p.scale(size,size,size);p.translate(0,.75,0);
   var out=buffers.getBuffer(RenderType.entityTranslucent(TEX));root.render(p,out,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
   int length=e.small()?6:e.giant()?14:10;float fi=e.flailIntensity+(e.hurtTime>0||e.attackTicks>0?.0125f:-.0125f),fs=e.agitated()?3:.9875f;
   for(int i=0;i<length-1;i++){p.scale(.88f,.88f,.88f);p.translate(0,-.5,0);p.mulPose(Axis.ZP.rotation(.1f/fi*(float)Math.sin(age*.15-i/2f)));p.mulPose(Axis.XP.rotation(.15f*fi*(float)Math.sin(age*.1*fs-i/2f)));segment.render(p,out,light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);}
   p.scale(.88f,.88f,.88f);p.translate(0,-.5,0);orb.render(p,out,LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);bulb.render(p,out,LightTexture.FULL_BRIGHT,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
   p.popPose();super.render(e,yaw,partial,p,buffers,light);
  }
 }
}
