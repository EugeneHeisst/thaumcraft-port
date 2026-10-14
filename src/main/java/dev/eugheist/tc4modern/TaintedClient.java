package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class TaintedClient {
 static int rendered,orbs;
 @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e){e.enqueueWork(()->TaintedMob.spawnFx=mob->{var setting=net.minecraft.client.Minecraft.getInstance().options.particles().get();SporeSplooshParticle.burst(mob,setting==net.minecraft.client.ParticleStatus.MINIMAL?0:setting==net.minecraft.client.ParticleStatus.DECREASED?10:20);});}
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){for(var entry:TaintedContent.TYPES.entrySet()){String species=entry.getKey();e.registerEntityRenderer(entry.getValue().get(),c->new Renderer(c,species));}e.registerEntityRenderer(TaintedContent.ORB.get(),OrbRenderer::new);}
 static final class Renderer extends MobRenderer<TaintedMob,EntityModel<TaintedMob>> {
  final String species;Renderer(EntityRendererProvider.Context c,String species){super(c,model(species),species.equals("chicken")?.3f:.5f);this.species=species;
   if(species.equals("sheep"))addLayer(new RenderLayer<>(this){final SheepGeometry wool=new SheepGeometry(true);@Override public void render(PoseStack p,MultiBufferSource b,int light,TaintedMob e,float swing,float amount,float partial,float age,float yaw,float pitch){if(e.sheared()||e.isInvisible())return;wool.prepareMobModel(e,swing,amount,partial);wool.setupAnim(e,swing,amount,age,yaw,pitch);wool.renderToBuffer(p,b.getBuffer(RenderType.entityCutoutNoCull(AlchemyContent.id("textures/models/sheep_fur.png"))),light,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);}});
  }
  @Override public ResourceLocation getTextureLocation(TaintedMob e){return AlchemyContent.id("textures/models/"+species+".png");}
  @Override protected void scale(TaintedMob e,PoseStack p,float partial){if(species.equals("villager"))p.scale(.9375f,.9375f,.9375f);if(species.equals("creeper")){float f=net.minecraft.util.Mth.lerp(partial,e.fuseO,e.fuse())/28;float wobble=1+(float)Math.sin(f*100)*f*.01f;f=net.minecraft.util.Mth.clamp(f,0,1);f*=f;f*=f;p.scale((1+f*.4f)*wobble,(1+f*.1f)/wobble,(1+f*.4f)*wobble);}}
  @Override protected float getBob(TaintedMob e,float partial){rendered++;return species.equals("chicken")?((float)Math.sin(net.minecraft.util.Mth.lerp(partial,e.flapO,e.flap))+1)*net.minecraft.util.Mth.lerp(partial,e.flapSpeedO,e.flapSpeed):super.getBob(e,partial);}
  static EntityModel<TaintedMob> model(String s){return switch(s){case "cow"->new CowModel<>(CowModel.createBodyLayer().bakeRoot());case "pig"->new PigModel<>(PigModel.createBodyLayer(CubeDeformation.NONE).bakeRoot());case "chicken"->new ChickenModel<>(ChickenModel.createBodyLayer().bakeRoot());case "sheep"->new SheepGeometry(false);case "creeper"->new CreeperModel<>(CreeperModel.createBodyLayer(CubeDeformation.NONE).bakeRoot());default->new VillagerModel<>(villager());};}
 }
 static ModelPart villager(){var mesh=new MeshDefinition();var root=mesh.getRoot();var head=root.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-10,-4,8,10,8),PartPose.ZERO);head.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO).addOrReplaceChild("hat_rim",CubeListBuilder.create(),PartPose.ZERO);head.addOrReplaceChild("nose",CubeListBuilder.create().texOffs(24,0).addBox(-1,-1,-6,2,4,2),PartPose.offset(0,-2,0));root.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,20).addBox(-4,0,-3,8,12,6).texOffs(0,38).addBox(-4,0,-3,8,18,6,new CubeDeformation(.5f)),PartPose.ZERO);root.addOrReplaceChild("arms",CubeListBuilder.create().texOffs(44,22).addBox(-8,-2,-2,4,8,4).addBox(4,-2,-2,4,8,4).texOffs(40,38).addBox(-4,2,-2,8,4,4),PartPose.offsetAndRotation(0,2,0,-.75f,0,0));root.addOrReplaceChild("right_leg",CubeListBuilder.create().texOffs(0,22).addBox(-2,0,-2,4,12,4),PartPose.offset(-2,12,0));root.addOrReplaceChild("left_leg",CubeListBuilder.create().texOffs(0,22).mirror().addBox(-2,0,-2,4,12,4),PartPose.offset(2,12,0));return LayerDefinition.create(mesh,64,64).bakeRoot();}

 static final class SheepGeometry extends QuadrupedModel<TaintedMob>{float headAngle;SheepGeometry(boolean wool){super(layer(wool),false,8,4,2,2,24);}
  static ModelPart layer(boolean wool){var mesh=QuadrupedModel.createBodyMesh(12,CubeDeformation.NONE);var r=mesh.getRoot();r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-3,-4,wool?-4:-6,6,6,wool?6:8,new CubeDeformation(wool?.6f:0)),PartPose.offset(0,6,-8));r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(28,8).addBox(-4,-10,-7,8,16,6,new CubeDeformation(wool?1.75f:0)),PartPose.offsetAndRotation(0,5,2,(float)Math.PI/2,0,0));if(wool){String[] names={"right_hind_leg","left_hind_leg","right_front_leg","left_front_leg"};for(int i=0;i<4;i++)r.addOrReplaceChild(names[i],CubeListBuilder.create().texOffs(0,16).addBox(-2,0,-2,4,6,4,new CubeDeformation(.5f)),PartPose.offset(i%2==0?-3:3,12,i<2?7:-5));}return LayerDefinition.create(mesh,64,32).bakeRoot();}
  @Override public void prepareMobModel(TaintedMob e,float swing,float amount,float partial){super.prepareMobModel(e,swing,amount,partial);int timer=e.sheepTimer;float bend=timer<=0?0:timer>=4&&timer<=36?1:timer<4?(timer-partial)/4:-(timer-40-partial)/4;head.y=6+bend*9;headAngle=timer>4&&timer<=36?.62831855f+.2199115f*(float)Math.sin((timer-4-partial)/32*28.7):timer>0?.62831855f:e.getXRot()*(float)Math.PI/180;}
  @Override public void setupAnim(TaintedMob e,float swing,float amount,float age,float yaw,float pitch){super.setupAnim(e,swing,amount,age,yaw,pitch);head.xRot=headAngle;}
 }
 static abstract class OrbTypes extends RenderType {
  OrbTypes(){super("aspect_orb",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{});}
  static final RenderType ADD=create("tc4_aspect_orb",DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS,256,false,true,CompositeState.builder().setShaderState(RENDERTYPE_ENTITY_TRANSLUCENT_SHADER).setTextureState(new TextureStateShard(AlchemyContent.id("textures/misc/particles.png"),false,false)).setTransparencyState(new TransparencyStateShard("aspect_orb_add",()->{com.mojang.blaze3d.systems.RenderSystem.enableBlend();com.mojang.blaze3d.systems.RenderSystem.blendFunc(770,1);},()->{com.mojang.blaze3d.systems.RenderSystem.disableBlend();com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();})).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
 }
 static final class OrbRenderer extends EntityRenderer<AspectOrb>{static final ResourceLocation TEX=AlchemyContent.id("textures/misc/particles.png");OrbRenderer(EntityRendererProvider.Context c){super(c);shadowRadius=.1f;}
  @Override public ResourceLocation getTextureLocation(AspectOrb e){return TEX;}
  @Override public void render(AspectOrb e,float yaw,float partial,PoseStack p,MultiBufferSource b,int light){orbs++;p.pushPose();p.mulPose(entityRenderDispatcher.cameraOrientation());float size=.1f+.3f*(150-e.tickCount-partial)/150;p.scale(size,size,size);int frame=(int)(System.nanoTime()/25000000%16),color=e.aspect().getColor(),r=color>>16&255,g=color>>8&255,blue=color&255;int orbLight=Math.min(240,(light&255)+120)|(light&0xFF0000);var out=b.getBuffer(e.aspect().getBlend()==1?OrbTypes.ADD:RenderType.entityTranslucent(TEX));var pose=p.last();float u=frame/16f,v=.5f;vertex(out,pose,-.5f,-.25f,u,v+.0625f,r,g,blue,orbLight);vertex(out,pose,.5f,-.25f,u+.0625f,v+.0625f,r,g,blue,orbLight);vertex(out,pose,.5f,.75f,u+.0625f,v,r,g,blue,orbLight);vertex(out,pose,-.5f,.75f,u,v,r,g,blue,orbLight);p.popPose();super.render(e,yaw,partial,p,b,light);}
  static void vertex(VertexConsumer out,PoseStack.Pose pose,float x,float y,float u,float v,int r,int g,int b,int light){out.addVertex(pose,x,y,0).setColor(r,g,b,128).setUv(u,v).setOverlay(net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose,0,1,0);}
 }
}
