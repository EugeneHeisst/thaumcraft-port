package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Literal staged transforms from TileEtherealBloomRenderer. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class EtherealBloomClient {
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerBlockEntityRenderer(EtherealBloom.ENTITY.get(),Renderer::new);}
 public static final class Renderer implements BlockEntityRenderer<EtherealBloom.Entity> {
  final ModelPart cube;
  public Renderer(BlockEntityRendererProvider.Context context){var mesh=new MeshDefinition();mesh.getRoot().addOrReplaceChild("cube",CubeListBuilder.create().texOffs(0,0).mirror().addBox(-8,-8,-8,16,16,16),PartPose.offset(8,8,8));cube=LayerDefinition.create(mesh,64,32).bakeRoot();}
  @Override public void render(EtherealBloom.Entity tile,float partial,PoseStack pose,MultiBufferSource buffers,int light,int overlay){
   float growth=tile.growthCounter+partial,scale1=Math.min(growth,100)/100,scale2=Math.min(growth,50)/60+.1666666f,scale3=Mth.clamp(growth-33,0,33)/33,scale4=Mth.clamp(growth-66,0,33)/33*.7f;
   pose.pushPose();pose.translate(.5,scale1,.5);pose.mulPose(Minecraft.getInstance().gameRenderer.getMainCamera().rotation());pose.scale(scale1,scale1,scale1);
   int frame=tile.counter%32;quad(pose,buffers.getBuffer(NodeRenderTypes.get(true,false)),-1,1,frame/32f,(frame+1)/32f,6/32f,7/32f,0xFFAADDFF,220);pose.popPose();
   pose.pushPose();pose.translate(.5-scale4/8,scale1-scale4/6,.5-scale4/8);pose.scale(scale4/4,scale4/3,scale4/4);cube.render(pose,buffers.getBuffer(NodeRenderTypes.additive(AlchemyContent.id("textures/models/crystalcapacitor.png"))),light,overlay);pose.popPose();
   foliage(pose,buffers,.25f,0,scale3,scale1,"purifier_leaves");
   foliage(pose,buffers,.6f,45,scale4,scale1*.7f,"purifier_leaves");
   pose.pushPose();pose.translate(.5,.5,.5);pose.mulPose(Axis.XP.rotationDegrees(180));pose.translate(0,(1-scale1)/2,0);pose.scale(scale2,scale1,scale2);
   var out=buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id("textures/blocks/purifier_stalk.png")));for(int i=0;i<4;i++){quad(pose,out,-.5f,.5f,0,1,1,0,-1,200);pose.mulPose(Axis.YP.rotationDegrees(90));}pose.popPose();
  }
  static void foliage(PoseStack pose,MultiBufferSource buffers,float height,float rotation,float width,float scaleY,String texture){pose.pushPose();pose.translate(.5,height,.5);pose.mulPose(Axis.YP.rotationDegrees(rotation));pose.mulPose(Axis.XP.rotationDegrees(180));pose.scale(width,scaleY,width);var out=buffers.getBuffer(RenderType.entityTranslucent(AlchemyContent.id("textures/blocks/"+texture+".png")));for(int i=0;i<4;i++){quad(pose,out,-.5f,.5f,0,1,1,0,-1,200);pose.mulPose(Axis.YP.rotationDegrees(90));}pose.popPose();}
  static void quad(PoseStack pose,VertexConsumer out,float min,float max,float u0,float u1,float v0,float v1,int color,int light){var matrix=pose.last().pose();float[][] points={{min,min,u1,v1},{min,max,u1,v0},{max,max,u0,v0},{max,min,u0,v1}};for(var p:points)out.addVertex(matrix,p[0],p[1],0).setColor(color).setUv(p[2],p[3]).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),0,0,1);}
 }
}
