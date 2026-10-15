package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.*;
/** Modern particle submission of original stationary FXBurst. */
public final class NodeConversionBurst extends Particle {
    private final float radius;
    private static final ParticleRenderType TYPE=new ParticleRenderType(){
        public BufferBuilder begin(Tesselator tessellator,net.minecraft.client.renderer.texture.TextureManager textures){com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/nodes.png"));
            RenderSystem.enableBlend();RenderSystem.blendFunc(770,1);RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
    };
    public NodeConversionBurst(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z){this(level,x,y,z,1);}
    public NodeConversionBurst(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z,float size){
        super(level,x,y,z);hasPhysics=false;setSize(.01f,.01f);
        radius=(random.nextFloat()*.5f+.5f)*2f*size;
        lifetime=31;
    }
    @Override public void tick(){if(age++>=lifetime)remove();}
    @Override public ParticleRenderType getRenderType(){return TYPE;}
    @Override public void render(VertexConsumer vertices,net.minecraft.client.Camera camera,float partial){
        var eye=camera.getPosition();float cx=(float)(x-eye.x),cy=(float)(y-eye.y),cz=(float)(z-eye.z),u=(age%32)/32f;
        float[][] corners={{-1,-1,u+.03125f,1},{-1,1,u+.03125f,.96875f},{1,1,u,.96875f},{1,-1,u,1}};
        for(var c:corners){var o=new org.joml.Vector3f(-c[0]*radius,c[1]*radius,0).rotate(camera.rotation());
            vertices.addVertex(cx+o.x,cy+o.y,cz+o.z).setUv(c[2],c[3]).setColor(1f,1f,1f,1f).setLight(240);
        }
    }
}
