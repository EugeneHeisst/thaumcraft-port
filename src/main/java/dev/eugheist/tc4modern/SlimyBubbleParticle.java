package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.*;
/** FXSlimyBubble, original particle frames 144..150 and rise/pop timing. */
final class SlimyBubbleParticle extends Particle {
    final float scale;int frame=144;
    static final ParticleRenderType TYPE=new ParticleRenderType(){public BufferBuilder begin(Tesselator t,net.minecraft.client.renderer.texture.TextureManager textures){RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);RenderSystem.setShaderTexture(0,FociFx.PARTICLES);RenderSystem.enableBlend();RenderSystem.blendFunc(770,771);RenderSystem.depthMask(false);return t.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);}};
    SlimyBubbleParticle(double x,double y,double z,float scale){super(net.minecraft.client.Minecraft.getInstance().level,x,y,z);this.scale=scale;xd=yd=zd=0;lifetime=15+level.random.nextInt(5);alpha=.8f;rCol=.6f-level.random.nextFloat()*.2f;gCol=0;bCol=.6f+level.random.nextFloat()*.2f;hasPhysics=false;setSize(.01f,.01f);}
    @Override public void tick(){xo=x;yo=y;zo=z;if(age++>=lifetime)remove();if(age-1<6){frame=144+age/2;if(age==5)y+=.1;}else if(age<lifetime-4){yd+=.005;frame=147+age%4/2;}else{yd/=2;frame=150-(lifetime-age)/2;}y+=yd;}
    @Override public ParticleRenderType getRenderType(){return TYPE;}
    @Override public void render(VertexConsumer out,net.minecraft.client.Camera c,float partial){float u=frame%16/16f,v=frame/16/16f;var eye=c.getPosition();float x0=(float)(xo+(x-xo)*partial-eye.x),y0=(float)(yo+(y-yo)*partial-eye.y),z0=(float)(zo+(z-zo)*partial-eye.z);float[][] corners={{-1,-1,u+.0625f,v+.0625f},{-1,1,u+.0625f,v},{1,1,u,v},{1,-1,u,v+.0625f}};for(var a:corners){var o=new org.joml.Vector3f(a[0]*scale,a[1]*scale,0).rotate(c.rotation());out.addVertex(x0+o.x,y0+o.y,z0+o.z).setUv(a[2],a[3]).setColor(rCol,gCol,bCol,alpha).setLight(getLightColor(partial));}}
}
