package dev.eugheist.tc4modern;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.particle.*;
/** Modern particle submission of original stationary Alumentum FXSparkle. */
public final class AlumentumSpark extends Particle {
    private final thaumcraft.client.fx.particles.AlumentumSparkle state;
    private static final ParticleRenderType TYPE=new ParticleRenderType(){
        public BufferBuilder begin(Tesselator tessellator,net.minecraft.client.renderer.texture.TextureManager textures){
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"textures/misc/particles.png"));
            RenderSystem.enableBlend();RenderSystem.blendFunc(770,1);RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.PARTICLE);
        }
    };
    public AlumentumSpark(net.minecraft.client.multiplayer.ClientLevel level,double x,double y,double z){
        super(level,x,y,z);hasPhysics=false;setSize(.01f,.01f);
        state=new thaumcraft.client.fx.particles.AlumentumSparkle(new java.util.random.RandomGenerator(){public long nextLong(){return random.nextLong();}public float nextFloat(){return random.nextFloat();}});
    }
    @Override public void tick(){if(!state.tick())remove();}
    @Override public ParticleRenderType getRenderType(){return TYPE;}
    private static boolean loggedRender;
    @Override public void render(VertexConsumer vertices,net.minecraft.client.Camera camera,float partial){
        if(Boolean.getBoolean("tc4modern.debugAlumentum")&&!loggedRender){loggedRender=true;org.slf4j.LoggerFactory.getLogger("tc4modern.Alumentum").info("AlumentumSpark render pos=({}, {}, {}) eye={} box={}",x,y,z,camera.getPosition(),getBoundingBox());}
        var eye=camera.getPosition();float cx=(float)(x-eye.x),cy=(float)(y-eye.y),cz=(float)(z-eye.z),u=state.u();
        float[][] corners={{-1,-1,u+.0624375f,.3124375f},{-1,1,u+.0624375f,.25f},{1,1,u,.25f},{1,-1,u,.3124375f}};
        for(var c:corners){var o=new org.joml.Vector3f(-c[0]*state.radius(),c[1]*state.radius(),0).rotate(camera.rotation());
            vertices.addVertex(cx+o.x,cy+o.y,cz+o.z).setUv(c[2],c[3]).setColor(state.red,state.green,state.blue,1f).setLight(240);
        }
    }
}
