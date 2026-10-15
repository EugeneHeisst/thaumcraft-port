package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.*;
/** Original FXVent growth, drag, rise, texture frames and fading alpha. */
final class VentParticle extends Particle {
 float scale;
 static final ParticleRenderType TYPE=new ParticleRenderType(){public BufferBuilder begin(Tesselator tessellator,net.minecraft.client.renderer.texture.TextureManager textures){com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,FociFx.PARTICLES);RenderSystem.enableBlend();RenderSystem.blendFunc(770,771);RenderSystem.depthMask(false);return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}};
 VentParticle(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,double vx,double vy,double vz){super(level,x,y,z);hasPhysics=false;setSize(.02f,.02f);scale=(random.nextFloat()*.1f+.05f)*2;double length=Math.sqrt(vx*vx+vy*vy+vz*vz);if(length>0){xd=(vx/length+random.nextGaussian()*(random.nextBoolean()?-1:1)*.0075*5)*.125;yd=(vy/length+random.nextGaussian()*(random.nextBoolean()?-1:1)*.0075*5)*.125;zd=(vz/length+random.nextGaussian()*(random.nextBoolean()?-1:1)*.0075*5)*.125;}rCol=153/255f;gCol=136/255f;bCol=170/255f;}
 @Override public ParticleRenderType getRenderType(){return TYPE;}
 @Override public void tick(){xo=x;yo=y;zo=z;age++;if(scale>2){remove();return;}yd+=.0025;move(xd,yd,zd);xd*=.850000019;yd*=.850000019;zd*=.850000019;if(scale<2)scale*=1.15f;}
 @Override public void render(VertexConsumer out,net.minecraft.client.Camera camera,float partial){int frame=(int)(1+scale/2*4);float u=frame/16f,v=0,size=.3f*scale,alpha=.4f*((2-scale)/2);var eye=camera.getPosition();float cx=(float)(xo+(x-xo)*partial-eye.x),cy=(float)(yo+(y-yo)*partial-eye.y),cz=(float)(zo+(z-zo)*partial-eye.z);float[][] corners={{-1,-1,u+.0624375f,v+.0624375f},{1,-1,u,v+.0624375f},{1,1,u,v},{-1,1,u+.0624375f,v}};for(var c:corners){var o=new org.joml.Vector3f(c[0]*size,c[1]*size,0).rotate(camera.rotation());out.addVertex(cx+o.x,cy+o.y,cz+o.z).setUv(c[2],c[3]).setColor(rCol,gCol,bCol,Math.max(0,alpha)).setLight(getLightColor(partial));}}
}
