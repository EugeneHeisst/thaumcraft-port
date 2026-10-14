package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.*;
/** Original FXBurst and non-shrinking, aspect-colored FXWisp used by EntityWisp. */
final class WispMoteParticle extends Particle {
 private final boolean burst;private final float size;
 private static final ParticleRenderType BURST=new ParticleRenderType(){public BufferBuilder begin(Tesselator t,net.minecraft.client.renderer.texture.TextureManager textures){RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,AlchemyContent.id("textures/misc/nodes.png"));RenderSystem.enableBlend();RenderSystem.blendFunc(770,1);RenderSystem.depthMask(false);return t.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}};
 WispMoteParticle(net.minecraft.client.multiplayer.ClientLevel l,double x,double y,double z,int color,boolean burst){super(l,x,y,z);this.burst=burst;xd=yd=zd=0;hasPhysics=!burst;size=(random.nextFloat()*.5f+.5f)*2*(burst?1:.1f);lifetime=burst?31:(int)(36/(Math.random()*.3+.7));rCol=burst?1:(color>>16&255)/255f;if(!burst&&rCol==0)rCol=1;gCol=burst?1:(color>>8&255)/255f;bCol=burst?1:(color&255)/255f;setSize(burst?.01f:.1f,burst?.01f:.1f);}
 @Override public ParticleRenderType getRenderType(){return burst?BURST:FociFx.ADDITIVE;}
 @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();if(!burst){yd-=.04*.02;move(xd,yd,zd);xd*=.98f;yd*=.98f;zd*=.98f;if(onGround){xd*=.7f;zd*=.7f;}}}
 @Override public void render(VertexConsumer out,net.minecraft.client.Camera camera,float partial){float scale=burst?1:age/(float)(lifetime/2);if(!burst&&scale>1)scale=2-scale;float radius=size*scale*(burst?1:.5f);float u=burst?(age%32)/32f:0,v=burst?31/32f:7/8f,step=burst?1/32f:1/8f;var eye=camera.getPosition();float cx=(float)(xo+(x-xo)*partial-eye.x),cy=(float)(yo+(y-yo)*partial-eye.y),cz=(float)(zo+(z-zo)*partial-eye.z);float[][] corners={{-1,-1,u+step,v+step},{1,-1,u,v+step},{1,1,u,v},{-1,1,u+step,v}};for(var c:corners){var o=new org.joml.Vector3f(c[0]*radius,c[1]*radius,0).rotate(camera.rotation());out.addVertex(cx+o.x,cy+o.y,cz+o.z).setUv(c[2],c[3]).setColor(rCol,gCol,bCol,1).setLight(240);}}
 static void tick(Wisp w){var mc=net.minecraft.client.Minecraft.getInstance();var l=(net.minecraft.client.multiplayer.ClientLevel)w.level();var r=l.random;if(w.tickCount==1||w.deathTime==1)mc.particleEngine.add(new WispMoteParticle(l,w.getX(),w.getY()+.45,w.getZ(),0xFFFFFF,true));if(w.isAlive()&&r.nextBoolean()&&w.aspect()!=null)mc.particleEngine.add(new WispMoteParticle(l,w.getX()+(r.nextFloat()-r.nextFloat())*.7,w.getY()+.45+(r.nextFloat()-r.nextFloat())*.7,w.getZ()+(r.nextFloat()-r.nextFloat())*.7,w.aspect().getColor(),false));}
}
