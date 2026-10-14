package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.particle.*;
/** FXSmokeSpiral's two-turn spherical path and original smoke frames. */
final class SmokeSpiralParticle extends Particle {
 final float radius,start,size;final int minY;
 SmokeSpiralParticle(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,float radius,int start,int minY,int color){super(level,x,y,z);this.radius=radius;this.start=start;this.minY=minY;lifetime=20+random.nextInt(10);size=.15f*(random.nextFloat()*.5f+.5f)*2;rCol=(color>>16&255)/255f;gCol=(color>>8&255)/255f;bCol=(color&255)/255f;hasPhysics=false;}
 @Override public ParticleRenderType getRenderType(){return VentParticle.TYPE;}
 @Override public void tick(){alpha=(lifetime-age)/(float)lifetime;if(age++>=lifetime)remove();}
 @Override public void render(VertexConsumer out,net.minecraft.client.Camera camera,float partial){float progress=(age+partial)/lifetime,a=(float)Math.toRadians(start+720*progress),b=(float)Math.toRadians(90-180*progress);double px=x-Math.sin(a)*Math.cos(b)*radius,py=Math.max(y-Math.sin(b)*radius,minY+.1),pz=z+Math.cos(a)*Math.cos(b)*radius;var eye=camera.getPosition();float cx=(float)(px-eye.x),cy=(float)(py-eye.y),cz=(float)(pz-eye.z);int frame=(int)(1+age/(float)lifetime*4);float u=frame/16f;float[][] corners={{-1,-1,u+.0624375f,.0624375f},{1,-1,u,.0624375f},{1,1,u,0},{-1,1,u+.0624375f,0}};for(var c:corners){var p=new org.joml.Vector3f(c[0]*size,c[1]*size,0).rotate(camera.rotation());out.addVertex(cx+p.x,cy+p.y,cz+p.z).setUv(c[2],c[3]).setColor(rCol,gCol,bCol,.66f*alpha).setLight(getLightColor(partial));}}
}
