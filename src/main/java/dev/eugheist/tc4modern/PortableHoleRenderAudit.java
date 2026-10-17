package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Native renderer, framebuffer and restoration checks in an isolated, throwaway audit world. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
final class PortableHoleRenderAudit {
    private static final BlockPos BASE = new BlockPos(0, 180, 0);
    private static final Direction[] FACES = Direction.values();
    private static final Path OUT = Path.of("hole-render-audit");
    private static int ticks, checks, failures;
    private static volatile int prepared = -1;

    private static void check(boolean good, String message) {
        checks++;
        if (!good) failures++;
        System.out.println("TC4_HOLE_RENDER " + (good ? "PASS " : "FAIL ") + message);
    }

    @SubscribeEvent
    static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tc4modern.holeRenderAuditOnly")) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        ticks++;
        if (mc.screen != null) mc.setScreen(null);
        if (ticks == 1) {
            mc.options.renderDistance().set(3);
            mc.options.fov().set(60);
            mc.options.bobView().set(false);
            mc.options.hideGui = true;
            mc.options.setCameraType(CameraType.FIRST_PERSON);
        }
        if (ticks == 20) mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            if (player == null) return;
            player.setGameMode(GameType.SPECTATOR);
            player.getInventory().clearContent();
            player.removeAllEffects();
            var level = player.serverLevel();
            level.setDayTime(6000);
            level.setWeatherParameters(60000, 0, false, false);
            player.connection.teleport(BASE.getX() + .5, BASE.getY() + 2, BASE.getZ() - 4, 0, 0);
        });
        if (ticks == 80) mc.getSingleplayerServer().execute(() -> {
            var level = mc.getSingleplayerServer().overworld();
            for (BlockPos pos : BlockPos.betweenClosed(BASE.offset(-6, -6, -6), BASE.offset(6, 6, 6))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            prepared = 0;
        });

        // Each entrance is AIR, the other five neighbours are solid gold. The old missing
        // BER exposes the gold backing; the actual star field must cover those pixels.
        for (int i = 0; i < FACES.length; i++) {
            final int index = i;
            int start = 120 + i * 70;
            if (ticks == start) prepare(mc, index + 1, FACES[i], false, false);
            if (ticks == start + 50) {
                Direction entrance = FACES[i];
                check(prepared == index + 1, entrance + " scene was created by the integrated server");
                check(mc.level.getBlockState(BASE.relative(entrance)).isAir(), entrance + " entrance remains open air");
                var expected = EnumSet.allOf(Direction.class);
                expected.remove(entrance);
                geometry(mc, "outside-" + entrance.getName(), expected);
                capture(mc, "outside-" + entrance.getName(), true);
            }
        }

        if (ticks == 560) {
            prepare(mc, 7, Direction.NORTH, false, false);
            look(mc, Vec3.atCenterOf(BASE).add(0, 0, -.20), Vec3.atCenterOf(BASE).add(0, 0, 2));
        }
        if (ticks == 610) {
            check(prepared == 7, "inside view scene loaded");
            capture(mc, "inside", true);
        }
        if (ticks == 640) prepare(mc, 8, Direction.NORTH, true, false);
        if (ticks == 690) {
            check(prepared == 8 && mc.level.getBlockState(BASE.east()).is(FociWorld.HOLE.get()), "two adjacent actual holes loaded");
            geometry(mc, "connected-left", EnumSet.of(Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.WEST));
            geometryAt(mc, BASE.east(), "connected-right", EnumSet.of(Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.EAST));
            capture(mc, "connected", true);
        }
        if (ticks == 700) {
            Vec3 target = Vec3.atCenterOf(BASE).add(.5, 0, 0);
            look(mc, target.add(0, 0, -26), target);
        }
        if (ticks == 710) {
            farGeometry(mc);
            shot(mc, "far");
        }
        if (ticks == 720) prepare(mc, 9, Direction.NORTH, false, true);
        if (ticks == 730) {
            check(prepared == 9 && mc.level.getBlockEntity(BASE) instanceof FociWorld.HoleEntity, "short-lived hole exists before its native countdown expires");
        }
        if (ticks == 800) {
            check(mc.level.getBlockState(BASE).is(Blocks.GOLD_BLOCK) && mc.level.getBlockEntity(BASE) == null,
                "native HoleEntity countdown restored its remembered gold block and removed the block entity");
            capture(mc, "restored", false);
        }
        if (ticks == 830) {
            System.out.println("TC4_HOLE_RENDER DONE " + checks + " checks, " + failures + " failures; actual six-face, inside, connected and restoration screenshots in " + OUT.toAbsolutePath());
        }
        if (ticks == 850) mc.stop();
    }

    private static void prepare(Minecraft mc, int scene, Direction entrance, boolean connected, boolean shortLived) {
        mc.getSingleplayerServer().execute(() -> {
            ServerLevel level = mc.getSingleplayerServer().overworld();
            // Reset the small fixture, not any real player world. Its centre becomes a
            // hole through createHole, so its saved block and native restoration are real.
            for (BlockPos pos : BlockPos.betweenClosed(BASE.offset(-2, -2, -2), BASE.offset(3, 2, 2))) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            }
            for (BlockPos pos : BlockPos.betweenClosed(BASE.offset(-1, -1, -1), BASE.offset(connected ? 2 : 1, 1, 1))) {
                level.setBlock(pos, Blocks.GOLD_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            }
            level.setBlock(BASE.relative(entrance), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            if (connected) level.setBlock(BASE.east().relative(entrance), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            FociWorld.createHole(level, BASE, -1, (byte) 1, (short) (shortLived ? 35 : 30000));
            if (connected) FociWorld.createHole(level, BASE.east(), -1, (byte) 1, (short) 30000);
            prepared = scene;
        });
        Vec3 target = Vec3.atCenterOf(BASE).add(connected ? .5 : 0, 0, 0);
        Vec3 eye = target.add(entrance.getStepX() * 2.75, entrance.getStepY() * 2.75, entrance.getStepZ() * 2.75);
        look(mc, eye, target);
    }

    private static void look(Minecraft mc, Vec3 eye, Vec3 target) {
        Vec3 direction = target.subtract(eye);
        float yaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(direction.y, Math.sqrt(direction.x * direction.x + direction.z * direction.z)));
        var id = mc.player.getUUID();
        mc.getSingleplayerServer().execute(() -> {
            var player = mc.getSingleplayerServer().getPlayerList().getPlayer(id);
            if (player != null) player.connection.teleport(eye.x, eye.y - player.getEyeHeight(), eye.z, yaw, pitch);
        });
    }

    private static void geometry(Minecraft mc, String label, EnumSet<Direction> expected) {
        geometryAt(mc, BASE, label, expected);
    }

    private static void geometryAt(Minecraft mc, BlockPos pos, String label, EnumSet<Direction> expected) {
        if (!(mc.level.getBlockEntity(pos) instanceof FociWorld.HoleEntity hole)) {
            check(false, label + " actual HoleEntity is present");
            return;
        }
        var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(hole);
        check(renderer != null, label + " actual HoleEntity has a registered native renderer");
        if (renderer == null) {
            check(false, label + " actual rendered face set is " + expected + " (renderer absent)");
            return;
        }
        RecordingConsumer vertices = new RecordingConsumer();
        try {
            renderer.render(hole, 0, new PoseStack(), type -> vertices, 0x00f000f0, 0);
            var actual = vertices.faces();
            check(actual.equals(expected), label + " actual submitted faces " + actual + " match solid neighbours " + expected);
            check(vertices.vertices.size() > 0 && vertices.vertices.size() % 4 == 0,
                label + " native BER emitted complete quads (" + vertices.vertices.size() + " vertices)");
        } catch (RuntimeException ex) {
            check(false, label + " native renderer threw " + ex);
        }
    }

    private static void capture(Minecraft mc, String label, boolean expectStarField) {
        try {
            Files.createDirectories(OUT);
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(OUT.resolve(label + ".png"));
                int radius = Math.max(12, Math.min(image.getWidth(), image.getHeight()) / 18);
                int dark = 0, warm = 0, total = 0;
                for (int x = image.getWidth() / 2 - radius; x < image.getWidth() / 2 + radius; x++) {
                    for (int y = image.getHeight() / 2 - radius; y < image.getHeight() / 2 + radius; y++) {
                        int pixel = image.getPixelRGBA(x, y);
                        int r = pixel & 255, g = pixel >>> 8 & 255, b = pixel >>> 16 & 255;
                        if (Math.max(r, Math.max(g, b)) < 100) dark++;
                        // Gold stays yellow on shaded faces too (native baseline RGB can
                        // be only 44/36/9), so identify its colour rather than brightness.
                        if (r > 25 && g > 15 && b < r * .5 && r > b + 15) warm++;
                        total++;
                    }
                }
                double darkRatio = dark / (double) total, warmRatio = warm / (double) total;
                System.out.printf(java.util.Locale.ROOT, "TC4_HOLE_RENDER PIXELS %s dark=%.4f gold=%.4f sampled=%d%n", label, darkRatio, warmRatio, total);
                check(expectStarField ? darkRatio > .55 && warmRatio < .20 : warmRatio > .60,
                    label + (expectStarField ? " actual framebuffer shows the dark star field over the gold backing" : " actual framebuffer shows restored gold"));
            }
        } catch (IOException | RuntimeException ex) {
            check(false, label + " screenshot failed: " + ex);
        }
    }

    private static void farGeometry(Minecraft mc) {
        if (!(mc.level.getBlockEntity(BASE) instanceof FociWorld.HoleEntity hole)) {
            check(false, "far actual HoleEntity is present");
            return;
        }
        var renderer = mc.getBlockEntityRenderDispatcher().getRenderer(hole);
        check(renderer != null, "far actual HoleEntity has a registered native renderer");
        if (renderer == null) return;
        var buffers = new LinkedHashMap<net.minecraft.client.renderer.RenderType, RecordingConsumer>();
        try {
            check(mc.getCameraEntity() != null && mc.getCameraEntity().distanceToSqr(Vec3.atCenterOf(BASE)) >= 512,
                "far native camera entity is beyond the original squared-distance threshold 512");
            renderer.render(hole, 0, new PoseStack(), type -> buffers.computeIfAbsent(type, key -> new RecordingConsumer()), 0x00f000f0, 0);
            check(buffers.size() == 1 && buffers.keySet().stream().allMatch(type -> type.toString().contains("tc4_hole_far") && type.toString().contains("particlefield32")),
                "far native BER uses only the particlefield32 far render type: " + buffers.keySet());
            var expected = EnumSet.of(Direction.DOWN, Direction.UP, Direction.SOUTH, Direction.WEST);
            int vertices = buffers.values().stream().mapToInt(buffer -> buffer.vertices.size()).sum();
            var faces = EnumSet.noneOf(Direction.class);
            buffers.values().forEach(buffer -> faces.addAll(buffer.faces()));
            check(vertices == expected.size() * 4 && faces.equals(expected),
                "far native BER emits one quad per solid face, no AIR or HOLE partition (" + vertices + " vertices, " + faces + ")");
            check(buffers.values().stream().allMatch(buffer -> buffer.colors.size() == buffer.vertices.size()
                    && buffer.colors.stream().allMatch(color -> color[0] == 127 && color[1] == 127 && color[2] == 127 && color[3] == 255)),
                "far native BER vertices use the original opaque half-gray colour (127,127,127,255)");
        } catch (RuntimeException ex) {
            check(false, "far native renderer threw " + ex);
        }
    }

    private static void shot(Minecraft mc, String label) {
        try {
            Files.createDirectories(OUT);
            try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                image.writeToFile(OUT.resolve(label + ".png"));
            }
            System.out.println("TC4_HOLE_RENDER SHOT " + label + ".png");
        } catch (IOException | RuntimeException ex) {
            check(false, label + " screenshot failed: " + ex);
        }
    }

    /** Captures the real BER output before GPU submission, including its pose transforms. */
    private static final class RecordingConsumer implements VertexConsumer {
        private final List<float[]> vertices = new ArrayList<>();
        private final List<int[]> colors = new ArrayList<>();

        @Override public VertexConsumer addVertex(float x, float y, float z) { vertices.add(new float[] { x, y, z }); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { colors.add(new int[] { r, g, b, a }); return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { return this; }

        private EnumSet<Direction> faces() {
            var result = EnumSet.noneOf(Direction.class);
            for (int i = 0; i + 3 < vertices.size(); i += 4) {
                for (Direction direction : Direction.values()) {
                    int axis = switch (direction.getAxis()) { case X -> 0; case Y -> 1; case Z -> 2; };
                    float plane = direction.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
                    boolean onFace = true;
                    for (int j = 0; j < 4; j++) if (Math.abs(vertices.get(i + j)[axis] - plane) > .025f) onFace = false;
                    if (onFace) result.add(direction);
                }
            }
            return result;
        }
    }
}
