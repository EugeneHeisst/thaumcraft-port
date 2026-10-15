package dev.eugheist.tc4modern;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import org.joml.Vector3f;

/** Client ports of FXSparkle, FXBeamWand and the excavation crack overlay used by the wand foci. */
public final class FociFx {
    private FociFx() {}
    static final ResourceLocation PARTICLES = AlchemyContent.id("textures/misc/particles.png");

    /** ParticleEngine layer 0: particles.png with additive blending, no depth writes. */
    static final ParticleRenderType ADDITIVE = new ParticleRenderType() {
        @Override public BufferBuilder begin(Tesselator tessellator, net.minecraft.client.renderer.texture.TextureManager textures) {com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, PARTICLES);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 1);
            RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
    };

    /** FXSparkle: four-frame twinkle from particles.png row 0.25, shrinking over 3*multiplier ticks. */
    public static final class Sparkle extends Particle {
        private final int multiplier;
        private final float scale;
        private float gravityValue;
        Sparkle(ClientLevel level, double x, double y, double z, float size, int type, int multiplier) {
            super(level, x, y, z);
            this.multiplier = multiplier;
            lifetime = 3 * multiplier;
            scale = (random.nextFloat() * .5f + .5f) * 2 * size;
            hasPhysics = false;
            setSize(.01f, .01f);
            switch (type) {
                case 0 -> setColor(.75f + random.nextFloat() * .25f, .25f + random.nextFloat() * .25f, .75f + random.nextFloat() * .25f);
                case 1 -> setColor(.5f + random.nextFloat() * .3f, .5f + random.nextFloat() * .3f, .2f);
                case 2 -> setColor(.2f, .2f, .7f + random.nextFloat() * .3f);
                case 3 -> setColor(.2f, .7f + random.nextFloat() * .3f, .2f);
                case 4 -> setColor(.7f + random.nextFloat() * .3f, .2f, .2f);
                case 6 -> setColor(.8f + random.nextFloat() * .2f, .8f + random.nextFloat() * .2f, .8f + random.nextFloat() * .2f);
                case 7 -> setColor(.2f, .5f + random.nextFloat() * .3f, .6f + random.nextFloat() * .3f);
                default -> setColor(1, 1, 1);
            }
        }
        Sparkle rgb(int color) { setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f); return this; }
        Sparkle motion(double dx, double dy, double dz) { xd = dx; yd = dy; zd = dz; return this; }
        Sparkle gravity(float g) { gravityValue = g; return this; }
        @Override public void tick() {
            xo = x; yo = y; zo = z;
            if (age++ >= lifetime) { remove(); return; }
            yd -= .04 * gravityValue;
            x += xd; y += yd; z += zd;
            xd *= .908; yd *= .908; zd *= .908;
        }
        @Override public ParticleRenderType getRenderType() { return ADDITIVE; }
        private void quad(VertexConsumer out, Camera camera, float partial, float size, float u0, float u1, float v0, float v1, float r, float g, float b, float a) {
            var eye = camera.getPosition();
            float cx = (float) (Mth.lerp(partial, xo, x) - eye.x), cy = (float) (Mth.lerp(partial, yo, y) - eye.y), cz = (float) (Mth.lerp(partial, zo, z) - eye.z);
            float[][] corners = {{-1, -1, u1, v1}, {-1, 1, u1, v0}, {1, 1, u0, v0}, {1, -1, u0, v1}};
            for (var c : corners) {
                var o = new Vector3f(-c[0] * size, c[1] * size, 0).rotate(camera.rotation());
                out.addVertex(cx + o.x, cy + o.y, cz + o.z).setUv(c[2], c[3]).setColor(r, g, b, a).setLight(240);
            }
        }
        @Override public void render(VertexConsumer out, Camera camera, float partial) {
            int part = 16 + age / multiplier;
            float u0 = (part % 4) / 16f, u1 = u0 + .0624375f, v0 = .25f, v1 = v0 + .0624375f;
            float size = .1f * scale * (lifetime - age + 1) / (float) lifetime;
            quad(out, camera, partial, size, u0, u1, v0, v1, rCol, gCol, bCol, 1);
        }
    }

    /** ClientProxy.sparkle(x, y, z, colour type): 1.5 size, 6-tick frames, two in six chance at normal particle settings. */
    public static void sparkle(double x, double y, double z, int type) {
        var level = Minecraft.getInstance().level;
        if (level == null || level.random.nextInt(6) >= particleCount(2)) return;
        Minecraft.getInstance().particleEngine.add(new Sparkle(level, x, y, z, 1.5f, type, 6));
    }

    /** ClientProxy.sparkle(x, y, z, size, colour type, gravity). */
    public static void sparkle(double x, double y, double z, float size, int type, float gravity) {
        var level = Minecraft.getInstance().level;
        if (level == null || level.random.nextInt(6) >= particleCount(2)) return;
        Minecraft.getInstance().particleEngine.add(new Sparkle(level, x, y, z, size, type, 6).gravity(gravity));
    }

    /**
     * ItemFocusShock.shootLightning: a type 2 bolt (duration 6, multiplier .5, speed 8, width .125) from just in front of the
     * caster's eyes, or from the middle of another player's body, to the target point.
     */
    public static void shootLightning(Player p, double tx, double ty, double tz) {
        var level = (net.minecraft.client.multiplayer.ClientLevel) p.level();
        boolean self = p == Minecraft.getInstance().player;
        double px = p.getX() - Mth.cos(p.getYRot() / 180f * 3.141593f) * .06f;
        double py = self ? p.getEyeY() - .06f : p.getBoundingBox().minY + p.getBbHeight() / 2 + .25;
        double pz = p.getZ() - Mth.sin(p.getYRot() / 180f * 3.141593f) * .06f;
        var look = p.getViewVector(1f);
        NodeBoltParticle.spawn(level, px + look.x * .3, py + look.y * .3, pz + look.z * .3, tx, ty, tz, 6, .5f, 8, 2, .125f);
    }

    /** PacketFXZap: the same bolt between the centres of two entities. */
    public static void zap(int source, int target) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var a = level.getEntity(source);
        var b = level.getEntity(target);
        if (a == null || b == null) return;
        NodeBoltParticle.spawn(level, a.getX(), a.getBoundingBox().minY + a.getBbHeight() / 2, a.getZ(), b.getX(), b.getBoundingBox().minY + b.getBbHeight() / 2, b.getZ(), 6, .5f, 8, 2, .125f);
    }

    /**
     * FXGeneric (ParticleEngine layer 0): a particles.png cell animation from startParticle over numParticles cells,
     * hidden for the delay ticks and drawn at half alpha on its first and last ticks.
     */
    public static final class Generic extends Particle {
        private final int start, num, inc, delay;
        private final boolean loop;
        private final float scale;
        private int cell;
        Generic(ClientLevel level, double x, double y, double z, double mx, double my, double mz, float r, float g, float b, float alpha,
                boolean loop, int start, int num, int inc, int age, int delay, float scale) {
            super(level, x, y, z);
            xd = mx; yd = my; zd = mz;
            setColor(r, g, b);
            this.alpha = alpha;
            this.loop = loop; this.start = start; this.num = num; this.inc = inc; this.delay = delay; this.scale = scale;
            lifetime = age + delay;
            cell = start;
            hasPhysics = false;
            setSize(.1f, .1f);
        }
        @Override public void tick() {
            xo = x; yo = y; zo = z;
            if (age++ >= lifetime) { remove(); return; }
            move(xd, yd, zd);
            xd *= .98; yd *= .98; zd *= .98;
            if (onGround) { xd *= .7; zd *= .7; }
            if (loop) cell = start + age / inc % num;
            else cell = (int) (start + Math.min(num * (age / (float) lifetime), num - 1));
        }
        @Override public ParticleRenderType getRenderType() { return ADDITIVE; }
        @Override public void render(VertexConsumer out, Camera camera, float partial) {
            if (age < delay) return;
            float a = age <= 1 || age >= lifetime - 1 ? alpha / 2 : alpha;
            float u0 = (cell % 16) / 16f, u1 = u0 + .0624375f, v0 = (cell / 16) / 16f, v1 = v0 + .0624375f;
            var eye = camera.getPosition();
            float cx = (float) (Mth.lerp(partial, xo, x) - eye.x), cy = (float) (Mth.lerp(partial, yo, y) - eye.y), cz = (float) (Mth.lerp(partial, zo, z) - eye.z);
            float size = .1f * scale;
            int light = getLightColor(partial);
            float[][] corners = {{-1, -1, u1, v1}, {-1, 1, u1, v0}, {1, 1, u0, v0}, {1, -1, u0, v1}};
            for (var c : corners) {
                var o = new Vector3f(-c[0] * size, c[1] * size, 0).rotate(camera.rotation());
                out.addVertex(cx + o.x, cy + o.y, cz + o.z).setUv(c[2], c[3]).setColor(rCol, gCol, bCol, a).setLight(light);
            }
        }
    }
    /** ClientProxy.drawGenericParticles. */
    public static void generic(double x, double y, double z, double mx, double my, double mz, float r, float g, float b, float alpha,
                               boolean loop, int start, int num, int inc, int age, int delay, float scale) {
        var level = Minecraft.getInstance().level;
        if (level != null) Minecraft.getInstance().particleEngine.add(new Generic(level, x, y, z, mx, my, mz, r, g, b, alpha, loop, start, num, inc, age, delay, scale));
    }

    static final ResourceLocation PARTICLES2 = AlchemyContent.id("textures/misc/particles2.png");
    /** ParticleEngine layer 2: particles2.png, additive. */
    static final ParticleRenderType ADDITIVE2 = new ParticleRenderType() {
        @Override public BufferBuilder begin(Tesselator tessellator, net.minecraft.client.renderer.texture.TextureManager textures) {com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, PARTICLES2);
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 1);
            RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
    };
    /** FXSpark: a seven-frame crackle from one of three 8x8-grid rows of particles2.png, randomly mirrored, 5-9 ticks. */
    public static final class Spark extends Particle {
        private final int row;
        private final boolean flip;
        private final float size;
        Spark(ClientLevel level, double x, double y, double z, float size) {
            super(level, x, y, z);
            this.size = size;
            lifetime = 5 + level.random.nextInt(5);
            row = level.random.nextInt(3) * 8;
            flip = level.random.nextBoolean();
            setSize(.01f, .01f);
            hasPhysics = false;
        }
        void alpha(float a) { setAlpha(a); }
        @Override public void tick() { xo = x; yo = y; zo = z; if (age++ >= lifetime) remove(); }
        @Override public ParticleRenderType getRenderType() { return ADDITIVE2; }
        @Override public void render(VertexConsumer out, Camera camera, float partial) {
            int part = row + (int) (age / (float) lifetime * 7);
            float u0 = (part % 8) / 8f, u1 = u0 + .125f, v0 = (part / 8) / 8f, v1 = v0 + .125f;
            if (flip) { float t = u0; u0 = u1; u1 = t; }
            var eye = camera.getPosition();
            float cx = (float) (Mth.lerp(partial, xo, x) - eye.x), cy = (float) (Mth.lerp(partial, yo, y) - eye.y), cz = (float) (Mth.lerp(partial, zo, z) - eye.z);
            int light = getLightColor(partial);
            float[][] corners = {{-1, -1, u1, v1}, {-1, 1, u1, v0}, {1, 1, u0, v0}, {1, -1, u0, v1}};
            for (var c : corners) {
                var o = new Vector3f(-c[0] * size, c[1] * size, 0).rotate(camera.rotation());
                out.addVertex(cx + o.x, cy + o.y, cz + o.z).setUv(c[2], c[3]).setColor(rCol, gCol, bCol, alpha).setLight(light);
            }
        }
    }
    /** new FXSpark(...) with colour and alpha set, added to ParticleEngine. */
    public static void spark(double x, double y, double z, float size, float r, float g, float b, float a) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var s = new Spark(level, x, y, z, size);
        s.setColor(r, g, b);
        s.alpha(a);
        Minecraft.getInstance().particleEngine.add(s);
    }

    /** ClientProxy.blockSparkle: rising tinted twinkles around a block. */
    public static void blockSparkle(BlockPos pos, int color, int count) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var r = level.random;
        float red = (color >> 16 & 255) / 255f, green = (color >> 8 & 255) / 255f, blue = (color & 255) / 255f;
        for (int a = 0; a < particleCount(count); a++) {
            // ClientProxy.blockSparkle: colour -9999 is a random pastel per particle (thaumonomicon creation).
            if (color == -9999) { red = .33f + r.nextFloat() * .67f; green = .33f + r.nextFloat() * .67f; blue = .33f + r.nextFloat() * .67f; }
            var s = new Sparkle(level, pos.getX() - .1f + r.nextFloat() * 1.2f, pos.getY() - .1f + r.nextFloat() * 1.2f, pos.getZ() - .1f + r.nextFloat() * 1.2f,
                .7f + r.nextFloat() * .4f, -1, 5 + r.nextInt(8)).motion(0, r.nextFloat() * .02f, 0);
            s.setColor(Mth.clamp(red - .2f + r.nextFloat() * .4f, 0, 1), Mth.clamp(green - .2f + r.nextFloat() * .4f, 0, 1), Mth.clamp(blue - .2f + r.nextFloat() * .4f, 0, 1));
            Minecraft.getInstance().particleEngine.add(s);
        }
    }

    /** Thaumcraft.proxy.particleCount: the setting-scaled count (all/decreased/minimal). */
    static int particleCount(int base) {
        return switch (Minecraft.getInstance().options.particles().get()) {
            case ALL -> base * 2;
            case DECREASED -> base;
            case MINIMAL -> base / 2;
        };
    }

    /** ClientProxy.excavateFX: the vanilla crack overlay keyed by the player's id. */
    public static void excavate(Player player, BlockPos pos, int progress) {
        Minecraft.getInstance().levelRenderer.destroyBlockProgress(player.getId(), pos, progress);
    }

    private static final Map<Integer, WandBeam> BEAMS = new HashMap<>();
    /** ClientProxy.beamCont: keep one beam per player alive and steer it. */
    public static void beam(Player player, double tx, double ty, double tz, int type, int color, float endMod, int impact) {
        var beam = BEAMS.get(player.getId());
        if (beam == null || !beam.isAlive()) {
            beam = new WandBeam((ClientLevel) player.level(), player, tx, ty, tz, color, 8);
            beam.type = type;
            beam.endMod = endMod;
            BEAMS.put(player.getId(), beam);
            Minecraft.getInstance().particleEngine.add(beam);
        } else {
            beam.update(tx, ty, tz);
            beam.endMod = endMod;
            beam.impact = impact;
        }
    }
    public static void stopBeam(Player player) { BEAMS.remove(player.getId()); }

    /** FXBeamWand: three crossed additive ribbons from the wand tip, sliding texture, impact flare at the target. */
    public static final class WandBeam extends Particle {
        private final Player player;
        private final double offset;
        private double tX, tY, tZ, ptX, ptY, ptZ;
        private float length, rotYaw, rotPitch, prevYaw, prevPitch, prevSize;
        int type, impact;
        float endMod = 1;
        WandBeam(ClientLevel level, Player player, double tx, double ty, double tz, int color, int age) {
            super(level, player.getX(), player.getY(), player.getZ());
            this.player = player;
            offset = player == Minecraft.getInstance().getCameraEntity() ? player.getEyeHeight() : player.getBbHeight() / 2 + .25;
            setColor((color >> 16 & 255) / 255f, (color >> 8 & 255) / 255f, (color & 255) / 255f);
            hasPhysics = false;
            tX = ptX = tx; tY = ptY = ty; tZ = ptZ = tz;
            aim();
            prevYaw = rotYaw; prevPitch = rotPitch;
            lifetime = age;
            var viewer = Minecraft.getInstance().getCameraEntity();
            int visible = Minecraft.getInstance().options.graphicsMode().get() == net.minecraft.client.GraphicsStatus.FAST ? 25 : 50;
            if (viewer != null && viewer.distanceTo(player) > visible) lifetime = 0;
        }
        private void aim() {
            float xd = (float) (player.getX() - tX), yd = (float) (player.getY() + offset - tY), zd = (float) (player.getZ() - tZ);
            length = Mth.sqrt(xd * xd + yd * yd + zd * zd);
            double flat = Math.sqrt(xd * xd + zd * zd);
            rotYaw = (float) (Math.atan2(xd, zd) * 180 / Math.PI);
            rotPitch = (float) (Math.atan2(yd, flat) * 180 / Math.PI);
        }
        void update(double x, double y, double z) {
            tX = x; tY = y; tZ = z;
            while (lifetime - age < 4) lifetime++;
        }
        @Override public void tick() {
            ptX = tX; ptY = tY; ptZ = tZ;
            prevYaw = rotYaw; prevPitch = rotPitch;
            aim();
            while (rotPitch - prevPitch < -180) prevPitch -= 360;
            while (rotPitch - prevPitch >= 180) prevPitch += 360;
            while (rotYaw - prevYaw < -180) prevYaw -= 360;
            while (rotYaw - prevYaw >= 180) prevYaw += 360;
            if (impact > 0) impact--;
            if (age++ >= lifetime) remove();
        }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.CUSTOM; }
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(float partial) { return net.minecraft.world.phys.AABB.INFINITE; }

        @Override public void render(VertexConsumer unused, Camera camera, float partial) {
            var mc = Minecraft.getInstance();
            var buffers = mc.renderBuffers().bufferSource();
            var eye = camera.getPosition();
            float size = Mth.lerp(partial, prevSize, Math.min(age / 4f, 1));
            float op = lifetime - age <= 4 ? .4f - (4 - (lifetime - age)) * .1f : .4f;
            float slide = (mc.player == null ? 0 : mc.player.tickCount) + partial;
            float shift = -slide * .2f - Mth.floor(-slide * .1f);
            int rotationSpeed = 5;
            float rot = level.getDayTime() % (360 / rotationSpeed) * rotationSpeed + rotationSpeed * partial;
            // Start at the wand tip: 0.3 along the look, nudged right and down.
            var look = player.getViewVector(1);
            double pyaw = Mth.lerp(partial, player.yRotO, player.getYRot()) * Mth.DEG_TO_RAD;
            double sx = Mth.lerp(partial, player.xo, player.getX()) - Math.cos(pyaw) * .066 + look.x * .3;
            double sy = Mth.lerp(partial, player.yo, player.getY()) + offset - .06 + look.y * .3;
            double sz = Mth.lerp(partial, player.zo, player.getZ()) - Math.sin(pyaw) * .04 + look.z * .3;
            var pose = new PoseStack();
            pose.translate(sx - eye.x, sy - eye.y, sz - eye.z);
            float ry = Mth.lerp(partial, prevYaw, rotYaw), rp = Mth.lerp(partial, prevPitch, rotPitch);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZN.rotationDegrees(180 + ry));
            pose.mulPose(Axis.XP.rotationDegrees(rp));
            pose.mulPose(Axis.YP.rotationDegrees(rot));
            float w = .15f * size, wEnd = .15f * size * endMod;
            var out = buffers.getBuffer(NodeRenderTypes.additive(AlchemyContent.id("textures/misc/" + (type == 0 ? "beam" : "beam" + type) + ".png")));
            int alpha = (int) (Math.max(op, 0) * 255);
            int r = (int) (rCol * 255), g = (int) (gCol * 255), b = (int) (bCol * 255);
            for (int t = 0; t < 3; t++) {
                float len = length * size, v0 = -1 + shift + t / 3f, v1 = len + v0;
                pose.mulPose(Axis.YP.rotationDegrees(60));
                var m = pose.last().pose();
                out.addVertex(m, -wEnd, len, 0).setUv(1, v1).setColor(r, g, b, alpha);
                out.addVertex(m, -w, 0, 0).setUv(1, v0).setColor(r, g, b, alpha);
                out.addVertex(m, w, 0, 0).setUv(0, v0).setColor(r, g, b, alpha);
                out.addVertex(m, wEnd, len, 0).setUv(0, v1).setColor(r, g, b, alpha);
            }
            if (impact > 0) {
                // renderImpact: one of 16 frames from particles.png row 0.3125, sized by the remaining impact ticks.
                var flare = buffers.getBuffer(NodeRenderTypes.additive(PARTICLES));
                float u0 = (age % 16) / 16f, u1 = u0 + .0624375f, v0 = .3125f, v1 = v0 + .0624375f;
                float s = endMod / 2 / (6 - impact);
                double ix = Mth.lerp(partial, ptX, tX) - eye.x, iy = Mth.lerp(partial, ptY, tY) - eye.y, iz = Mth.lerp(partial, ptZ, tZ) - eye.z;
                float[][] corners = {{-1, -1, u1, v1}, {-1, 1, u1, v0}, {1, 1, u0, v0}, {1, -1, u0, v1}};
                var identity = new PoseStack().last().pose();
                for (var c : corners) {
                    var o = new Vector3f(-c[0] * s, c[1] * s, 0).rotate(camera.rotation());
                    flare.addVertex(identity, (float) ix + o.x, (float) iy + o.y, (float) iz + o.z).setUv(c[2], c[3]).setColor(r, g, b, (int) (.66f * 255));
                }
            }
            buffers.endBatch();
            prevSize = size;
        }
    }
}
