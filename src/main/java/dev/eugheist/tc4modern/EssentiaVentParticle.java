package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.resources.ResourceLocation;

/** Original FXVent: growing, drifting translucent smoke from particles.png layer 1. */
public final class EssentiaVentParticle extends Particle {
    public static net.minecraft.client.renderer.ShaderInstance shader;
    private static final ParticleRenderType TYPE = new ParticleRenderType() {
        public BufferBuilder begin(Tesselator tessellator, net.minecraft.client.renderer.texture.TextureManager textures) {com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderTexture(0, ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/misc/particles.png"));
            RenderSystem.enableBlend(); RenderSystem.blendFunc(770, 771); RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
    };
    private float scale;
    private EssentiaVentParticle(net.minecraft.client.multiplayer.ClientLevel level, EssentiaTubeEntity.Vent vent) {
        super(level, vent.x(), vent.y(), vent.z());
        setSize(.02f, .02f); hasPhysics = false;
        scale = random.nextFloat() * .1f + .05f;
        rCol = (vent.color() >> 16 & 255) / 255f; gCol = (vent.color() >> 8 & 255) / 255f; bCol = (vent.color() & 255) / 255f;
        double length = (float)Math.sqrt(vent.dx()*vent.dx()+vent.dy()*vent.dy()+vent.dz()*vent.dz());
        xd = heading(vent.dx()/length); yd = heading(vent.dy()/length); zd = heading(vent.dz()/length);
        var camera = Minecraft.getInstance().getCameraEntity();
        if (camera != null && camera.distanceToSqr(x,y,z) > Math.pow(Minecraft.useFancyGraphics()?50:25,2)) remove();
    }
    private double heading(double direction) { return (direction + random.nextGaussian() * (random.nextBoolean()?-1:1) * (double).0075f * 5) * .125f; }
    public static void spawn(EssentiaTubeEntity.Vent vent) {
        if (vent.level() instanceof net.minecraft.client.multiplayer.ClientLevel level)
            Minecraft.getInstance().particleEngine.add(new EssentiaVentParticle(level, vent));
    }
    @Override public void tick() {
        xo=x; yo=y; zo=z; age++;
        if (scale>1) remove();
        yd+=.0025; move(xd,yd,zd);
        xd*=.8500000190734863; yd*=.8500000190734863; zd*=.8500000190734863;
        if(scale<1) scale=(float)(scale*1.15);
        if(onGround){xd*=.7f;zd*=.7f;}
    }
    @Override public ParticleRenderType getRenderType(){return TYPE;}
    @Override public void render(VertexConsumer vertices, net.minecraft.client.Camera camera, float partial) {
        int part=(int)(1+scale*4); float u=part%16/16f,v=part/16/16f;
        var eye=camera.getPosition(); float px=(float)(xo+(x-xo)*partial-eye.x),py=(float)(yo+(y-yo)*partial-eye.y),pz=(float)(zo+(z-zo)*partial-eye.z);
        float[] xs={1,1,-1,-1},ys={-1,1,1,-1},us={u+.0624375f,u+.0624375f,u,u},vs={v+.0624375f,v,v,v+.0624375f};
        for(int i=0;i<4;i++){
            var point=new org.joml.Vector3f(xs[i]*.3f*scale,ys[i]*.3f*scale,0).rotate(camera.rotation());
            vertices.addVertex(px+point.x,py+point.y,pz+point.z).setUv(us[i],vs[i]).setColor(rCol,gCol,bCol,.4f*(1-scale)).setLight(getLightColor(partial));
        }
    }
}
