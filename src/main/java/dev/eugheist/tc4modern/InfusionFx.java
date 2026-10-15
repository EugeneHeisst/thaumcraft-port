package dev.eugheist.tc4modern;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Vector3f;

/**
 * Client effects for the infusion altar, ported from the 1.7.10 originals:
 * EssentiaHandler.sourceFX + FXEssentiaTrail (jar bubbles), TileInfusionMatrix.sourceFX + drawInfusionParticles1..4
 * (FXBoreParticles item/block bits and FXBoreSparkle), TilePedestal block events 11/12 (blockSparkle -> FXGeneric)
 * and PacketFXBlockZap (ported lightning bolt).
 */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class InfusionFx {
    private InfusionFx() {}
    /** start = consumer (matrix), end = source (jar, pedestal or the matrix itself for the XP drain); color or entity id. */
    private static final class Source { final BlockPos start, end; final int color; int ticks; Source(BlockPos s, BlockPos e, int c, int t) { start = s; end = e; color = c; ticks = t; } }
    private static final Map<String, Source> ESSENTIA = new LinkedHashMap<>(), INFUSION = new LinkedHashMap<>();
    private static int tickCount;
    /** Highest point any essentia trail bubble reached (audits). */
    static double trailMaxY=-1e9;
    static int trailsSpawned, itemBitsSpawned, sparklesSpawned, finishSparkles, boreStuck, boreReached, boreInsideAtDeath, boreLingering;

    static void handle(InfusionContent.FxPayload p) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        switch (p.kind()) {
            case 0 -> { // PacketFXEssentiaSource: 15 ticks, refreshed by every further drain
                BlockPos source = BlockPos.containing(p.x0(), p.y0(), p.z0()), consumer = BlockPos.containing(p.x1(), p.y1(), p.z1());
                String key = consumer.getX() + ":" + consumer.getY() + ":" + consumer.getZ() + ":" + source.getX() + ":" + source.getY() + ":" + source.getZ() + ":" + p.data();
                var old = ESSENTIA.remove(key);
                if (old != null) { old.ticks = 15; ESSENTIA.put(key, old); } else ESSENTIA.put(key, new Source(consumer, source, p.data(), 15));
            }
            case 1, 4 -> { // PacketFXInfusionSource: pedestal 60 ticks, otherwise (player XP) 15
                BlockPos from = BlockPos.containing(p.x0(), p.y0(), p.z0()), matrix = BlockPos.containing(p.x1(), p.y1(), p.z1());
                int color = p.kind() == 4 ? p.data() : 0;
                String key = matrix.asLong() + "|" + from.getX() + ":" + from.getY() + ":" + from.getZ() + ":" + color;
                int count = level.getBlockEntity(from) instanceof InfusionContent.PedestalEntity ? 60 : 15;
                var old = INFUSION.get(key);
                if (old != null) old.ticks = count; else INFUSION.put(key, new Source(matrix, p.kind() == 4 ? matrix : from, color, count));
            }
            case 2 -> bolt(p.x0(), p.y0(), p.z0(), p.x1(), p.y1(), p.z1());
            case 3 -> { // blockSparkle; x1 = outer particleCount base of the pedestal block event (0 = single call)
                int outer = p.x1() <= 0 ? 1 : particleCount((int) p.x1());
                for (int i = 0; i < outer; i++) blockSparkle(level, (int) p.x0(), (int) p.y0(), (int) p.z0(), p.data(), 2);
            }
            default -> {}
        }
    }

    static void bolt(double x0, double y0, double z0, double x1, double y1, double z1) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        long seed = level.random.nextLong();
        Minecraft.getInstance().particleEngine.add(new NodeBoltParticle(level, x0, y0, z0, x1, y1, z1, seed, 0));
        Minecraft.getInstance().particleEngine.add(new NodeBoltParticle(level, x0, y0, z0, x1, y1, z1, seed, 1));
    }

    /** ClientProxy.particleCount: fancy 2x, decreased 1x, minimal none. */
    static int particleCount(int base) {
        return switch (Minecraft.getInstance().options.particles().get()) { case ALL -> base * 2; case DECREASED -> base; case MINIMAL -> 0; };
    }

    /** ClientProxy.blockSparkle: FXGeneric frames 112..120; color -9999 picks a random pastel per particle. */
    static void blockSparkle(ClientLevel level, int x, int y, int z, int c, int count) {
        var r = level.random;
        float cr = (c >> 16 & 255) / 255f, cg = (c >> 8 & 255) / 255f, cb = (c & 255) / 255f;
        for (int a = 0; a < particleCount(count); a++) {
            if (c == -9999) { cr = .33f + r.nextFloat() * .67f; cg = .33f + r.nextFloat() * .67f; cb = .33f + r.nextFloat() * .67f; }
            if (c == -9999) finishSparkles++;
            FociFx.generic(x - .1f + r.nextFloat() * 1.2f, y - .1f + r.nextFloat() * 1.2f, z - .1f + r.nextFloat() * 1.2f, 0, r.nextFloat() * .02, 0,
                    cr - .2f + r.nextFloat() * .4f, cg - .2f + r.nextFloat() * .4f, cb - .2f + r.nextFloat() * .4f, .9f, false, 112, 9, 1, 5 + r.nextInt(8), r.nextInt(10), .7f + r.nextFloat() * .4f);
        }
    }

    @SubscribeEvent static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        var level = mc.level;
        if (level == null) { ESSENTIA.clear(); INFUSION.clear(); return; }
        if (mc.isPaused()) return;
        ++tickCount;
        // ClientTickEventsFML (phase START): one trail per active jar stream, fading out over the last five ticks.
        for (var it = ESSENTIA.values().iterator(); it.hasNext(); ) {
            var fx = it.next();
            if (fx.ticks <= 0) { it.remove(); continue; }
            int mod = level.getBlockEntity(fx.start) instanceof InfusionContent.MatrixEntity ? -1 : 0;
            Vec3 from = Vec3.atCenterOf(fx.end), to = Vec3.atCenterOf(fx.start.offset(0, mod, 0));
            if (fx.ticks > 5) add(new EssentiaTrail(level, from, to, tickCount, fx.color, 1f));
            else add(new EssentiaTrail(level, from, to, tickCount - (5 - fx.ticks), fx.color, fx.ticks * fx.ticks / 25f));
            --fx.ticks;
        }
        // TileInfusionMatrix.doEffects source loop (the matrix updates every tick on the client).
        var r = level.random;
        for (var it = INFUSION.values().iterator(); it.hasNext(); ) {
            var fx = it.next();
            if (fx.ticks <= 0) { it.remove(); continue; }
            var m = fx.start;
            Vec3 target = new Vec3(m.getX() + .5, m.getY() - .5, m.getZ() + .5);
            if (fx.end.equals(m)) {
                Entity player = level.getEntity(fx.color);
                if (player != null) for (int a = 0; a < particleCount(2); a++) {
                    var s = new BoreSparkle(level, new Vec3(player.getX() + (r.nextFloat() - r.nextFloat()) * player.getBbWidth(), player.getBoundingBox().minY + r.nextFloat() * player.getBbHeight(), player.getZ() + (r.nextFloat() - r.nextFloat()) * player.getBbWidth()), target);
                    s.setColor(.2f, .6f + r.nextFloat() * .3f, .3f);
                    add(s);
                }
            } else if (level.getBlockEntity(fx.end) instanceof InfusionContent.PedestalEntity ped) {
                ItemStack is = ped.item();
                var l = fx.end;
                if (!is.isEmpty()) {
                    if (r.nextInt(3) == 0) {
                        var s = new BoreSparkle(level, new Vec3(l.getX() + r.nextFloat(), l.getY() + r.nextFloat() + 1, l.getZ() + r.nextFloat()), target);
                        s.setColor(.4f + r.nextFloat() * .2f, .2f, .6f + r.nextFloat() * .3f);
                        add(s);
                    } else if (is.getItem() instanceof BlockItem block) {
                        for (int a = 0; a < particleCount(2); a++) add(BoreBits.block(level, new Vec3(l.getX() + r.nextFloat(), l.getY() + r.nextFloat() + 1, l.getZ() + r.nextFloat()), target, block));
                    } else {
                        for (int a = 0; a < particleCount(2); a++) add(BoreBits.item(level, new Vec3(l.getX() + .4 + r.nextFloat() * .2, l.getY() + 1.23 + r.nextFloat() * .2, l.getZ() + .4 + r.nextFloat() * .2), target, is));
                    }
                }
            } else fx.ticks = 0;
            --fx.ticks;
        }
    }

    private static void add(Particle p) {
        var camera = Minecraft.getInstance().getCameraEntity();
        // Original constructors: lifetime 0 beyond 64 blocks (32 with fast graphics).
        if (camera != null && camera.distanceToSqr(p.getBoundingBox().getCenter()) > (Minecraft.getInstance().options.graphicsMode().get() == net.minecraft.client.GraphicsStatus.FAST ? 32 * 32 : 64 * 64)) return;
        Minecraft.getInstance().particleEngine.add(p);
    }

    /** Particle.move without the 1.21 permanent stoppedByCollision latch; 1.7 EntityFX kept moving after a vertical hit. */
    static abstract class Homing extends TextureSheetParticle {
        final Vec3 target;
        Homing(ClientLevel level, Vec3 start, Vec3 target) { super(level, start.x, start.y, start.z); this.target = target; hasPhysics = true; }
        @Override public void move(double dx, double dy, double dz) {
            double ox = dx, oy = dy, oz = dz;
            if (hasPhysics && (dx != 0 || dy != 0 || dz != 0) && dx * dx + dy * dy + dz * dz < 10000) {
                var v = Entity.collideBoundingBox(null, new Vec3(dx, dy, dz), getBoundingBox(), level, java.util.List.of());
                dx = v.x; dy = v.y; dz = v.z;
            }
            // 1.7 Entity.moveEntity never let a free particle enter a block; the 1.21 sweep can leave it a hair inside, and
            // pushOut then ejected it through the far face (trails from jars climbing through a solid floor).
            if (hasPhysics && (dx != 0 || dy != 0 || dz != 0) && level.noCollision(null, getBoundingBox()) && !level.noCollision(null, getBoundingBox().move(dx, dy, dz))) { dx = 0; dy = 0; dz = 0; }
            if (dx != 0 || dy != 0 || dz != 0) { setBoundingBox(getBoundingBox().move(dx, dy, dz)); setLocationFromBoundingbox(); }
            onGround = oy != dy && oy < 0;
            if (ox != dx) xd = 0;
            if (oy != dy) yd = 0;
            if (oz != dz) zd = 0;
        }
        /** pushOutOfBlocks: inside any non-air block (no liquid) leave through the closest face whose neighbour is not a full cube. */
        boolean pushOut(boolean requireFullCube) {
            var pos = BlockPos.containing(x, y, z);
            var state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty()) return false;
            if (requireFullCube && !state.isCollisionShapeFullBlock(level, pos)) return false;
            // DEVIATION: the original pushes out of any non-air block and overwrites the velocity every tick, so a bit inside
            // the pillar model above its 0..0.5 collision box fought the homing and hovered there ("stuck in the pillars").
            // Only push when actually inside collision geometry; collisions then slide the bits around the pillars.
            var shape = state.getCollisionShape(level, pos);
            if (shape.isEmpty() || !shape.bounds().move(pos).contains(x, y, z)) return false;
            double fx = x - pos.getX(), fy = y - pos.getY(), fz = z - pos.getZ(), nearest = 9999;
            Direction face = null;
            for (var d : new Direction[]{Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH}) {
                var n = pos.relative(d);
                if (level.getBlockState(n).isCollisionShapeFullBlock(level, n)) continue;
                double dist = switch (d) { case WEST -> fx; case EAST -> 1 - fx; case DOWN -> fy; case UP -> 1 - fy; case NORTH -> fz; case SOUTH -> 1 - fz; };
                if (dist < nearest) { nearest = dist; face = d; }
            }
            float speed = random.nextFloat() * .05f + .025f, other = (random.nextFloat() - random.nextFloat()) * .1f;
            if (face != null) {
                xd = face.getAxis() == Direction.Axis.X ? speed * face.getStepX() : other;
                yd = face.getAxis() == Direction.Axis.Y ? speed * face.getStepY() : other;
                zd = face.getAxis() == Direction.Axis.Z ? speed * face.getStepZ() : other;
            }
            return true;
        }
        /** FXBoreParticles/FXBoreSparkle onUpdate: strong homing, dies on reaching the target block. */
        void boreTick(float[] scale) {
            xo = x; yo = y; zo = z;
            boolean reached = BlockPos.containing(x, y, z).equals(BlockPos.containing(target));
            if (age++ >= lifetime || reached) {
                if (reached) boreReached++;
                else if (!level.getBlockState(BlockPos.containing(x, y, z)).isAir()) boreInsideAtDeath++;
                remove(); return;
            }
            pushOut(false);
            move(xd, yd, zd);
            xd *= .985; yd *= .985; zd *= .985;
            double dx = target.x - x, dy = target.y - y, dz = target.z - z, len = Math.sqrt(dx * dx + dy * dy + dz * dz), acc = .3;
            if (len < 4) { scale[0] *= .9f; acc = .6; }
            if (len > 0) {
                xd = Mth.clamp((float) (xd + dx / len * acc), -.35f, .35f);
                yd = Mth.clamp((float) (yd + dy / len * acc), -.35f, .35f);
                zd = Mth.clamp((float) (zd + dz / len * acc), -.35f, .35f);
            }
        }
        /** EntityFX billboard with the original corner order and fullbright (240) light. */
        void quad(VertexConsumer out, Camera camera, float partial, float size, float u0, float u1, float v0, float v1, float alpha) {
            var eye = camera.getPosition();
            float cx = (float) (Mth.lerp(partial, xo, x) - eye.x), cy = (float) (Mth.lerp(partial, yo, y) - eye.y), cz = (float) (Mth.lerp(partial, zo, z) - eye.z);
            float[][] corners = {{-1, -1, u1, v1}, {-1, 1, u1, v0}, {1, 1, u0, v0}, {1, -1, u0, v1}};
            for (var c : corners) {
                var o = new Vector3f(-c[0] * size, c[1] * size, 0).rotate(camera.rotation());
                out.addVertex(cx + o.x, cy + o.y, cz + o.z).setUv(c[2], c[3]).setColor(rCol, gCol, bCol, alpha).setLight(0xF000F0);
            }
        }
        static int boreLife(ClientLevel level, Vec3 a, Vec3 b) { int base = Math.max(1, (int) (a.distanceTo(b) * 3)); return base / 2 + level.random.nextInt(base); }
    }

    /** FXBoreParticles via drawInfusionParticles1 (items) / drawInfusionParticles2 (blocks): a quarter of the item or block sprite. */
    static final class BoreBits extends Homing {
        private final float uo = random.nextFloat() * 3, vo = random.nextFloat() * 3;
        private final float[] scale = {random.nextFloat() * .3f + .4f};
        private BoreBits(ClientLevel level, Vec3 start, Vec3 target) {
            super(level, start, target);
            lifetime = boreLife(level, start, target);
            xd = level.random.nextGaussian() * .01; yd = level.random.nextGaussian() * .01; zd = level.random.nextGaussian() * .01;
            gravity = 0;
            setColor(.6f, .6f, .6f);
            itemBitsSpawned++;
        }
        static BoreBits item(ClientLevel level, Vec3 start, Vec3 target, ItemStack stack) {
            var p = new BoreBits(level, start, target);
            p.setSprite(Minecraft.getInstance().getItemRenderer().getModel(stack, level, null, 0).getParticleIcon());
            // func_70596_a: tint by getColorFromItemStack(pass 0). setAlpha(0.3) is ignored by the original opaque tessellator colour.
            int tint = Minecraft.getInstance().getItemColors().getColor(stack, 0);
            if (tint != -1) p.setColor(.6f * (tint >> 16 & 255) / 255f, .6f * (tint >> 8 & 255) / 255f, .6f * (tint & 255) / 255f);
            p.xd = level.random.nextGaussian() * .03; p.yd = level.random.nextGaussian() * .03; p.zd = level.random.nextGaussian() * .03;
            return p;
        }
        /** Original quirk: func_70596_a compares against the matrix block, falls into the item branch with a null item and keeps 0.6 grey. */
        static BoreBits block(ClientLevel level, Vec3 start, Vec3 target, BlockItem item) {
            var p = new BoreBits(level, start, target);
            p.setSprite(Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(item.getBlock().defaultBlockState()));
            return p;
        }
        private int still, inside;
        @Override public void tick() {
            boreTick(scale); quadSize = .1f * scale[0];
            if (isAlive() && Math.abs(x - xo) + Math.abs(y - yo) + Math.abs(z - zo) < 1e-4 && ++still == 4) boreStuck++;
            if (isAlive() && !level.getBlockState(BlockPos.containing(x, y, z)).isAir() && ++inside == 6) boreLingering++;
        }
        @Override public float getQuadSize(float partial) { return .1f * scale[0]; }
        @Override protected float getU0() { return sprite.getU(uo / 4f); }
        @Override protected float getU1() { return sprite.getU((uo + 1) / 4f); }
        @Override protected float getV0() { return sprite.getV(vo / 4f); }
        @Override protected float getV1() { return sprite.getV((vo + 1) / 4f); }
        @Override public ParticleRenderType getRenderType() { return ParticleRenderType.TERRAIN_SHEET; }
    }

    /** FXBoreSparkle (ParticleEngine layer 0, additive): four-frame twinkle from particles.png row 0.25, fullbright. */
    static final class BoreSparkle extends Homing {
        private final float[] scale = {random.nextFloat() * .5f + .5f};
        BoreSparkle(ClientLevel level, Vec3 start, Vec3 target) {
            super(level, start, target);
            lifetime = boreLife(level, start, target);
            xd = random.nextGaussian() * .01; yd = random.nextGaussian() * .01; zd = random.nextGaussian() * .01;
            setColor(.2f, .6f + random.nextFloat() * .3f, .2f);
            sparklesSpawned++;
        }
        @Override public void tick() { boreTick(scale); }
        @Override public ParticleRenderType getRenderType() { return FociFx.ADDITIVE; }
        @Override public void render(VertexConsumer out, Camera camera, float partial) {
            float bob = Mth.sin(age / 3f) * .5f + 1, u0 = age % 4 / 16f, u1 = u0 + .0624375f, v0 = .25f, v1 = v0 + .0624375f;
            quad(out, camera, partial, .1f * scale[0] * bob, u0, u1, v0, v1, 1f);
        }
    }

    /** FXEssentiaTrail (ParticleEngine layer 1, SRC_ALPHA/ONE_MINUS_SRC_ALPHA): the white bubble cell at 144,16 tinted by the aspect. */
    static final class EssentiaTrail extends Homing {
        private final int count;
        private float scale;
        EssentiaTrail(ClientLevel level, Vec3 start, Vec3 target, int count, int color, float scale) {
            super(level, start, target);
            this.count = count;
            this.scale = (Mth.sin(count / 2f) * .1f + 1) * scale;
            int base = Math.max(1, (int) (start.distanceTo(target) * 30));
            lifetime = base / 2 + random.nextInt(base);
            xd = Mth.sin(count / 4f) * .015f + random.nextGaussian() * .002f;
            yd = .1f + Mth.sin(count / 3f) * .01f;
            zd = Mth.sin(count / 2f) * .015f + random.nextGaussian() * .002f;
            float r = (color >> 16 & 255) / 255f, g = (color >> 8 & 255) / 255f, b = (color & 255) / 255f;
            setColor(r - r * .2f + random.nextFloat() * r * .2f, g - g * .2f + random.nextFloat() * g * .2f, b - b * .2f + random.nextFloat() * b * .2f);
            gravity = .2f;
            trailsSpawned++;
        }
        @Override public void tick() {
            xo = x; yo = y; zo = z;
            if (age++ >= lifetime) { remove(); return; }
            trailMaxY = Math.max(trailMaxY, y);
            yd += .01 * gravity; // original adds "gravity" upwards
            pushOut(true);
            move(xd, yd, zd);
            xd = Mth.clamp((float) (xd * .985), -.05f, .05f);
            yd = Mth.clamp((float) (yd * .985), -.05f, .05f);
            zd = Mth.clamp((float) (zd * .985), -.05f, .05f);
            double dx = target.x - x, dy = target.y - y, dz = target.z - z, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 2) scale *= .98f;
            if (scale < .2f) { remove(); return; }
            if (len > 0) { double acc = .01 / Math.min(1, len); xd += dx / len * acc; yd += dy / len * acc; zd += dz / len * acc; }
        }
        @Override public ParticleRenderType getRenderType() { return TRANSLUCENT; }
        @Override public void render(VertexConsumer out, Camera camera, float partial) {
            float s = Mth.sin((age - count) / 5f) * .25f + 1;
            quad(out, camera, partial, .1f * scale * s, .5625f, .625f, .0625f, .125f, .5f);
        }
    }

    /** ParticleEngine layer 1: particles.png with SRC_ALPHA/ONE_MINUS_SRC_ALPHA. */
    static final ParticleRenderType TRANSLUCENT = new ParticleRenderType() {
        @Override public BufferBuilder begin(Tesselator tessellator, net.minecraft.client.renderer.texture.TextureManager textures) {com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();com.mojang.blaze3d.systems.RenderSystem.depthFunc(515);
            RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getParticleShader);
            RenderSystem.setShaderTexture(0, AlchemyContent.id("textures/misc/particles.png"));
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(770, 771);
            RenderSystem.depthMask(false);
            return tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.PARTICLE);
        }
    };
}
