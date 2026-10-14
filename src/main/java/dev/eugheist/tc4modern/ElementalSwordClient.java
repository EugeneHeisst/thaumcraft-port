package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.*;
import net.minecraft.client.particle.*;
import net.minecraft.world.entity.player.Player;
/** Original FXSmokeSpiral, including two turns, hemisphere path and ground clipping. */
public final class ElementalSwordClient {
 static int spawned,rendered;
 static void wind(Player p){var mc=Minecraft.getInstance();if(mc.level==null)return;int miny=(int)(p.getBoundingBox().minY-2);if(p.onGround())miny=net.minecraft.util.Mth.floor(p.getBoundingBox().minY);for(int i=0;i<5;i++){mc.particleEngine.add(new Spiral(mc.level,p.getX(),p.getBoundingBox().minY+p.getBbHeight()/2,p.getZ(),mc.level.random.nextInt(360),miny));spawned++;}if(p.onGround()){float angle=p.getRandom().nextFloat()*(float)Math.PI*2;p.level().addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,p.getX(),p.getBoundingBox().minY+.1,p.getZ(),-Math.sin(angle)/5,0,Math.cos(angle)/5);}}
 static final class Spiral extends Particle {
  final int start,miny;final float size;
  Spiral(net.minecraft.client.multiplayer.ClientLevel l,double x,double y,double z,int start,int miny){super(l,x,y,z);this.start=start;this.miny=miny;lifetime=20+random.nextInt(10);size=(random.nextFloat()*.5f+.5f)*2;setSize(.01f,.01f);rCol=gCol=bCol=221/255f;}
  @Override public void tick(){alpha=(lifetime-age)/(float)lifetime;if(age++>=lifetime)remove();}
  @Override public ParticleRenderType getRenderType(){return ElementalToolsClient.Bubble.TYPE;}
  @Override public void render(VertexConsumer out,Camera camera,float partial){rendered++;float progress=(age+partial)/lifetime;double a=Math.toRadians(start+720*progress),b=Math.toRadians(90-180*progress);var eye=camera.getPosition();float px=(float)(x-Math.sin(a)*Math.cos(b)*1.5-eye.x),py=(float)(Math.max(y-Math.sin(b)*1.5,miny+.1f)-eye.y),pz=(float)(z+Math.cos(a)*Math.cos(b)*1.5-eye.z);int frame=(int)(1+age/(float)lifetime*4);float u=frame%16/16f,v=frame/16/16f;float[] xs={1,1,-1,-1},ys={-1,1,1,-1};for(int i=0;i<4;i++){var q=new org.joml.Vector3f(xs[i]*size*.15f,ys[i]*size*.15f,0).rotate(camera.rotation());out.addVertex(px+q.x,py+q.y,pz+q.z).setUv(i<2?u+.0624375f:u,i==0||i==3?v+.0624375f:v).setColor(rCol,gCol,bCol,alpha*.66f).setLight(getLightColor(partial));}}
 }
}
