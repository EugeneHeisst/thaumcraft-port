package dev.eugheist.tc4modern;
import net.minecraft.client.particle.*;
import com.mojang.blaze3d.vertex.VertexConsumer;
/** Original FXSpark: random eight-frame strip on particles2, flipped UVs and fixed .2 radius. */
final class ChampionSpark extends Particle {
 final int start;final boolean flip;float radius=.2f,opacity=1;
 ChampionSpark(double x,double y,double z,float r,float g,float b){super(net.minecraft.client.Minecraft.getInstance().level,x,y,z);xd=yd=zd=0;setColor(r,g,b);lifetime=5+level.random.nextInt(5);start=level.random.nextInt(3)*8;flip=level.random.nextBoolean();hasPhysics=false;setSize(.01f,.01f);}
 @Override public ParticleRenderType getRenderType(){return FociFx.ADDITIVE2;}
 @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();}
 @Override public void render(VertexConsumer out,net.minecraft.client.Camera camera,float partial){int frame=start+(int)(age/(float)lifetime*7);float u=frame%8/8f,v=frame/8/8f;var eye=camera.getPosition();float cx=(float)(x-eye.x),cy=(float)(y-eye.y),cz=(float)(z-eye.z);float[][] corners={{-1,-1,u+.125f,v+.125f},{1,-1,u,v+.125f},{1,1,u,v},{-1,1,u+.125f,v}};for(var c:corners){var o=new org.joml.Vector3f(c[0]*radius,c[1]*radius,0).rotate(camera.rotation());out.addVertex(cx+o.x,cy+o.y,cz+o.z).setUv(flip?2*u+.125f-c[2]:c[2],c[3]).setColor(rCol,gCol,bCol,opacity).setLight(getLightColor(partial));}}
}
