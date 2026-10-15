package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;

/** Minecraft 1.7.10 EntitySpellParticleFX used by the full brain jar. */
public final class BrainJarParticle extends Particle {
    private static final ParticleRenderType TYPE = new ParticleRenderType() {
        public BufferBuilder begin(Tesselator tessellator, net.minecraft.client.renderer.texture.TextureManager textures) {com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
            RenderSystem.setShader(GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, AlchemyContent.id("textures/misc/legacy_spell_particles.png"));
            RenderSystem.enableBlend(); RenderSystem.blendFunc(770, 771); RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
    };
    private final float scale;
    private int frame = 128;
    private BrainJarParticle(net.minecraft.client.multiplayer.ClientLevel level, double x, double y, double z) {
        super(level, x, y, z);
        setSize(.2f, .2f);
        scale = (random.nextFloat() * .5f + .5f) * 2f * .75f;
        double dx = (float)(Math.random() * 2 - 1) * .4f;
        double dy = (float)(Math.random() * 2 - 1) * .4f;
        double dz = (float)(Math.random() * 2 - 1) * .4f;
        float speed = (float)(Math.random() + Math.random() + 1) * .15f;
        float length = (float)Math.sqrt(dx * dx + dy * dy + dz * dz);
        xd = dx / length * speed * .4f * .1f;
        yd = (dy / length * speed * .4f + .1f) * .2f;
        zd = dz / length * speed * .4f * .1f;
        lifetime = (int)(8 / (Math.random() * .8 + .2));
        rCol = 0; gCol = .4f + level.random.nextFloat() * .1f; bCol = .3f + level.random.nextFloat() * .2f; alpha = .5f;
    }
    public static void spawn(net.minecraft.world.level.Level level, double x, double y, double z) {
        if (level instanceof net.minecraft.client.multiplayer.ClientLevel client)
            Minecraft.getInstance().particleEngine.add(new BrainJarParticle(client, x, y, z));
    }
    @Override public void tick() {
        xo = x; yo = y; zo = z;
        if (age++ >= lifetime) remove();
        frame = 128 + 7 - age * 8 / lifetime;
        yd += .004;
        move(xd, yd, zd);
        if (y == yo) { xd *= 1.1; zd *= 1.1; }
        xd *= .96f; yd *= .96f; zd *= .96f;
        if (onGround) { xd *= .7f; zd *= .7f; }
    }
    @Override public ParticleRenderType getRenderType() { return TYPE; }
    @Override public void render(VertexConsumer vertices, net.minecraft.client.Camera camera, float partial) {
        float u = frame % 16 / 16f, v = frame / 16 / 16f;
        var eye = camera.getPosition();
        float px = (float)(xo + (x - xo) * partial - eye.x), py = (float)(yo + (y - yo) * partial - eye.y), pz = (float)(zo + (z - zo) * partial - eye.z);
        float[] xs = {1, 1, -1, -1}, ys = {-1, 1, 1, -1}, us = {u+.0624375f,u+.0624375f,u,u}, vs = {v+.0624375f,v,v,v+.0624375f};
        for (int i = 0; i < 4; i++) {
            var point = new org.joml.Vector3f(xs[i]*.1f*scale, ys[i]*.1f*scale, 0).rotate(camera.rotation());
            vertices.addVertex(px+point.x,py+point.y,pz+point.z).setUv(us[i],vs[i]).setColor(rCol,gCol,bCol,alpha).setLight(getLightColor(partial));
        }
    }
}
