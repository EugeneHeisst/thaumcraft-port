package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.*;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Original white FXBubble for purified liquid and pink soap bubbles, original OGG sounds. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class HygieneClient {
 static int bubbles,finishes;
 @SubscribeEvent static void extensions(net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent event){event.registerFluidType(new net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions(){@Override public ResourceLocation getStillTexture(){return AlchemyContent.id("blocks/fluidpure");}@Override public ResourceLocation getFlowingTexture(){return getStillTexture();}},PureWater.TYPE.get());}
 @SubscribeEvent static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event){event.register(Hygiene.SPA_MENU.get(),SpaScreen::new);}
 @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event){event.enqueueWork(()->{
  ItemBlockRenderTypes.setRenderLayer(PureWater.FLUID.get(),RenderType.translucent());
  Hygiene.pureBubbles=(level,pos)->{var r=level.random;int decay=level.getBlockState(pos).getValue(PureWater.DECAY);bubble((ClientLevel)level,pos.getX()+r.nextFloat(),pos.getY()+.125*(8-decay),pos.getZ()+r.nextFloat(),0,1,1,1,.25f);if(r.nextInt(25)==0)level.playLocalSound(pos.getX()+r.nextFloat(),pos.getY()+1,pos.getZ()+r.nextFloat(),net.minecraft.sounds.SoundEvents.LAVA_POP,net.minecraft.sounds.SoundSource.BLOCKS,.1f+r.nextFloat()*.1f,.9f+r.nextFloat()*.15f,false);};
  Hygiene.soapEffects=(player,finished)->{var level=(ClientLevel)player.level();var r=level.random;var mc=Minecraft.getInstance();int multiplier=mc.options.particles().get()==ParticleStatus.MINIMAL?0:mc.options.particles().get()==ParticleStatus.DECREASED?1:2;
   if(finished){finishes++;level.playLocalSound(player.getX(),player.getY(),player.getZ(),FociContent.sound("craftstart"),net.minecraft.sounds.SoundSource.PLAYERS,.25f,1,false);}else if(r.nextFloat()<.2f)level.playLocalSound(player.getX(),player.getY(),player.getZ(),TC4Modern.ROOTS.get(),net.minecraft.sounds.SoundSource.PLAYERS,.1f,1.5f+r.nextFloat()*.2f,false);
   for(int i=0;i<(finished?20:5)*multiplier;i++)bubble(level,(float)player.getX()-.5f+r.nextFloat()*(finished?1.5f:1),(float)player.getBoundingBox().minY+r.nextFloat()*player.getBbHeight(),(float)player.getZ()-.5f+r.nextFloat()*(finished?1.5f:1),1,1,finished?.7f:.8f,.9f,1);
  };
 });}
 static void bubble(ClientLevel level,double x,double y,double z,int age,float red,float green,float blue,float scale){Minecraft.getInstance().particleEngine.add(new Bubble(level,x,y,z,age,red,green,blue,scale));bubbles++;}
 static final class Bubble extends Particle {
  final thaumcraft.client.fx.particles.FXBubble original;final java.util.random.RandomGenerator rng;
  Bubble(ClientLevel level,double x,double y,double z,int age,float red,float green,float blue,float scale){super(level,x,y,z);rng=new java.util.random.RandomGenerator(){public long nextLong(){return random.nextLong();}public float nextFloat(){return random.nextFloat();}public double nextDouble(){return random.nextDouble();}};original=new thaumcraft.client.fx.particles.FXBubble(x,y,z,age,rng);original.scale*=scale;setColor(red,green,blue);}
  @Override public void tick(){xo=x;yo=y;zo=z;original.tick(rng);x=original.x;y=original.y;z=original.z;if(original.dead)remove();}
  @Override public ParticleRenderType getRenderType(){return LiquidDeathClient.BUBBLE_TYPE;}
  @Override public void render(VertexConsumer out,Camera camera,float partial){float u=original.particle%16/16f,v=original.particle/16/16f,size=original.scale*.1f;var eye=camera.getPosition();float cx=(float)(Mth.lerp(partial,xo,x)-eye.x),cy=(float)(Mth.lerp(partial,yo,y)-eye.y),cz=(float)(Mth.lerp(partial,zo,z)-eye.z);float[][] points={{-1,-1,u+.0624375f,v+.0624375f},{-1,1,u+.0624375f,v},{1,1,u,v},{1,-1,u,v+.0624375f}};for(var p:points){var d=new org.joml.Vector3f(p[0]*size,p[1]*size,0).rotate(camera.rotation());out.addVertex(cx+d.x,cy+d.y,cz+d.z).setUv(p[2],p[3]).setColor(rCol,gCol,bCol,1).setLight(getLightColor(partial));}}
 }
}
