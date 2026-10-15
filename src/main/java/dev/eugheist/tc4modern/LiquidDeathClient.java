package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.*;
import net.minecraft.core.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=Dist.CLIENT)
public final class LiquidDeathClient {
 @SubscribeEvent static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions(){
  @Override public ResourceLocation getStillTexture(){return AlchemyContent.id("blocks/fluiddeath");}
  @Override public ResourceLocation getFlowingTexture(){return getStillTexture();}
 },LiquidDeath.TYPE.get());}
 @SubscribeEvent static void colors(net.neoforged.neoforge.client.event.RegisterColorHandlersEvent.Item event){event.register((stack,tint)->{var a=EssentiaItems.getAspects(stack);var all=thaumcraft.api.aspects.Aspect.aspects.values().toArray(thaumcraft.api.aspects.Aspect[]::new);return 0xFF000000|(a==null?all[(int)(System.currentTimeMillis()/500%all.length)]:a.getAspects()[0]).getColor();},LiquidDeath.CRYSTAL.get());}
 @SubscribeEvent static void renderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(BottleTaint.ENTITY.get(),net.minecraft.client.renderer.entity.ThrownItemRenderer::new);}
 @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->{
  ItemBlockRenderTypes.setRenderLayer(LiquidDeath.FLUID.get(),RenderType.translucent());
  LiquidDeath.bubbles=(level,pos)->{
   var r=level.random;int meta=level.getBlockState(pos).getValue(LiquidDeath.AMOUNT)-1;
   var bubble=new Bubble((ClientLevel)level,pos.getX()+r.nextFloat(),pos.getY()+.1+.225*meta,pos.getZ()+r.nextFloat(),.075f+r.nextFloat()*.075f);
   bubble.setColor(.3f-r.nextFloat()*.1f,0,.4f+r.nextFloat()*.1f);Minecraft.getInstance().particleEngine.add(bubble);
   if(r.nextInt(50)==0)level.playLocalSound(pos.getX()+r.nextFloat(),pos.getY()+1,pos.getZ()+r.nextFloat(),net.minecraft.sounds.SoundEvents.LAVA_POP,net.minecraft.sounds.SoundSource.BLOCKS,.1f+r.nextFloat()*.1f,.9f+r.nextFloat()*.15f,false);
  };
  BottleTaint.burst=p->{LiquidDeathAudit.impacts++;burst(p);};
 });}
 static final ParticleRenderType BUBBLE_TYPE=new ParticleRenderType(){public BufferBuilder begin(Tesselator t,net.minecraft.client.renderer.texture.TextureManager textures){com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);RenderSystem.setShader(GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,FociFx.PARTICLES);RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.depthMask(false);return t.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}};
 /** Exact FXSlimyBubble frames 144..150, including its five-tick jump and late damping. */
 static final class Bubble extends Particle {
  final float size;int frame=144;
  Bubble(ClientLevel level,double x,double y,double z,float size){super(level,x,y,z);this.size=size;alpha=.8f;lifetime=15+level.random.nextInt(5);xd=yd=zd=0;setSize(.01f,.01f);}
  @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();if(age-1<6){frame=144+age/2;if(age==5)y+=.1;}else if(age<lifetime-4){yd+=.005;frame=147+age%4/2;}else{yd/=2;frame=150-(lifetime-age)/2;}y+=yd;}
  @Override public ParticleRenderType getRenderType(){return BUBBLE_TYPE;}
  @Override public void render(VertexConsumer out,Camera camera,float partial){
   float u=(frame%16)/16f,v=(frame/16)/16f;var eye=camera.getPosition();float cx=(float)(Mth.lerp(partial,xo,x)-eye.x),cy=(float)(Mth.lerp(partial,yo,y)-eye.y),cz=(float)(Mth.lerp(partial,zo,z)-eye.z);
   float[][] corners={{-1,-1,u+.0625f,v+.0625f},{-1,1,u+.0625f,v},{1,1,u,v},{1,-1,u,v+.0625f}};
   for(var c:corners){var o=new org.joml.Vector3f(-c[0]*size,c[1]*size,0).rotate(camera.rotation());out.addVertex(cx+o.x,cy+o.y,cz+o.z).setUv(c[2],c[3]).setColor(rCol,gCol,bCol,alpha).setLight(getLightColor(partial));}
  }
 }
 static void burst(BottleTaint.Projectile entity){
  var level=(ClientLevel)entity.level();var r=level.random;int count=Minecraft.getInstance().options.particles().get()==net.minecraft.client.ParticleStatus.MINIMAL?0:Minecraft.getInstance().options.particles().get()==net.minecraft.client.ParticleStatus.DECREASED?100:200;
  for(int i=0;i<count;i++)Minecraft.getInstance().particleEngine.add(new TaintFragment(level,entity.getX(),entity.getY()+r.nextFloat()*entity.getBbHeight(),entity.getZ()));
  for(int i=0;i<8;i++)level.addParticle(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM,new ItemStack(BottleTaint.ITEM.get())),entity.getX(),entity.getY(),entity.getZ(),r.nextGaussian()*.15,r.nextDouble()*.2,r.nextGaussian()*.15);
  level.playLocalSound(entity.getX(),entity.getY(),entity.getZ(),net.minecraft.sounds.SoundEvents.SPLASH_POTION_BREAK,net.minecraft.sounds.SoundSource.NEUTRAL,1,r.nextFloat()*.1f+.9f,false);
 }
 /** Original FXBreaking uses slime-ball fragments with purple tint, shrinking and fading. */
 static final class TaintFragment extends TextureSheetParticle {
  final float initial,jitterU,jitterV;
  TaintFragment(ClientLevel level,double x,double y,double z){
   super(level,x,y,z);setSprite(Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(Items.SLIME_BALL),level,null,0).getParticleIcon());
   initial=(random.nextFloat()*.5f+.5f)*.1f;jitterU=random.nextFloat()*3;jitterV=random.nextFloat()*3;gravity=1;
   if(level.random.nextBoolean()){setColor(.6f,0,.3f);alpha=.4f;}else{setColor(.3f,0,.3f);alpha=.6f;}
   var d=new net.minecraft.world.phys.Vec3(random.nextDouble()*2-1,random.nextDouble()*2-1,random.nextDouble()*2-1).normalize().scale((random.nextDouble()+random.nextDouble()+1)*.15*.9640000000596046);
   xd=d.x;yd=d.y+.1;zd=d.z;lifetime=(int)(66/(level.random.nextFloat()*.9f+.1f));
  }
  @Override public ParticleRenderType getRenderType(){return ParticleRenderType.TERRAIN_SHEET;}
  @Override public float getQuadSize(float partial){return initial*(1-(age+partial)/lifetime);}
  @Override protected float getU0(){return sprite.getU(jitterU/4);}@Override protected float getU1(){return sprite.getU((jitterU+1)/4);}
  @Override protected float getV0(){return sprite.getV(jitterV/4);}@Override protected float getV1(){return sprite.getV((jitterV+1)/4);}
  @Override public void render(VertexConsumer out,Camera camera,float partial){float old=alpha;alpha*=1-age/(float)lifetime;super.render(out,camera,partial);alpha=old;}
 }
}
