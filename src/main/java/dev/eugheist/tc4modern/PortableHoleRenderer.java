package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.systems.RenderSystem;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/** TileHoleRenderer: inward star-field walls against opaque neighbours, with open tunnel entrances and joins. */
final class PortableHoleRenderer implements BlockEntityRenderer<FociWorld.HoleEntity> {
    PortableHoleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(FociWorld.HoleEntity hole, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = hole.getLevel();
        if (level == null) return;
        var pos = hole.getBlockPos();
        var mc = Minecraft.getInstance();
        var camera = mc.gameRenderer.getMainCamera().getPosition();
        var viewer = mc.getCameraEntity();
        boolean near = (viewer == null ? camera.distanceToSqr(Vec3.atCenterOf(pos)) : viewer.distanceToSqr(Vec3.atCenterOf(pos))) < 512;
        for (var face : Direction.values()) {
            var adjacent = pos.relative(face);
            var neighbour = level.getBlockState(adjacent);
            if (neighbour.is(FociWorld.HOLE.get()) || !neighbour.isSolidRender(level, adjacent)) continue;
            float[][] points = corners(face);
            if (!near) {
                var out = buffers.getBuffer(Types.FAR);
                for (int i = 0; i < 4; i++) out.addVertex(pose.last().pose(), points[i][0], points[i][1], points[i][2])
                    .setUv(i < 2 ? 1 : 0, i == 0 || i == 3 ? 1 : 0).setColor(.5f, .5f, .5f, 1);
                continue;
            }
            // Original object/eye-linear texture generation, expressed as UVs for the modern shader pipeline.
            var axis = face.getAxis();
            double camA = axis == Direction.Axis.X ? camera.z : camera.x;
            double camB = axis == Direction.Axis.Y ? camera.z : camera.y;
            double camN = axis == Direction.Axis.X ? camera.x : axis == Direction.Axis.Y ? camera.y : camera.z;
            double planeN = axis == Direction.Axis.X ? pos.getX() + points[0][0]
                : axis == Direction.Axis.Y ? pos.getY() + points[0][1] : pos.getZ() + points[0][2];
            double distance = Math.max(.01, Math.abs(planeN - camN));
            var random = new Random(31100L);
            double time = System.currentTimeMillis() % 700000L / 250000.0;
            for (int layer = 0; layer < 16; layer++) {
                float depth = layer == 0 ? 65 : 16 - layer;
                float scale = layer == 0 ? .125f : layer == 1 ? .5f : .0625f;
                float brightness = layer == 0 ? .1f : 1f / (depth + 1);
                float red = random.nextFloat() * .5f + .1f, green = random.nextFloat() * .5f + .4f, blue = random.nextFloat() * .5f + .5f;
                if (layer == 0) red = green = blue = 1;
                var out = buffers.getBuffer(layer == 0 ? Types.BASE : Types.STARS);
                double angle = Math.toRadians((layer * layer * 4321 + layer * 9) * 2.0), cos = Math.cos(angle), sin = Math.sin(angle);
                double[][] uv = new double[4][2];
                for (int i = 0; i < 4; i++) {
                    double worldA = axis == Direction.Axis.X ? pos.getZ() + points[i][2] : pos.getX() + points[i][0];
                    double worldB = axis == Direction.Axis.Y ? pos.getZ() + points[i][2] : pos.getY() + points[i][1];
                    // The original eye-linear Q is negative on the visible inner face.
                    double s = -worldA - (worldA - camA) * depth / distance;
                    double t = -worldB - (worldB - camB) * depth / distance;
                    uv[i][0] = (cos * (s - .5) - sin * (t - .5) + .5) * scale;
                    uv[i][1] = (sin * (s - .5) + cos * (t - .5) + .5) * scale + time;
                }
                // Keep repeating UVs precise even at distant world coordinates; every corner shares the same tile offset.
                double u = Math.floor(uv[0][0]), v = Math.floor(uv[0][1]);
                for (int i = 0; i < 4; i++) out.addVertex(pose.last().pose(), points[i][0], points[i][1], points[i][2])
                    .setUv((float)(uv[i][0] - u), (float)(uv[i][1] - v)).setColor(red * brightness, green * brightness, blue * brightness, 1);
            }
        }
    }

    private static float[][] corners(Direction face) {
        return switch (face) {
            case UP -> new float[][]{{0,.999f,1},{0,.999f,0},{1,.999f,0},{1,.999f,1}};
            case DOWN -> new float[][]{{0,.001f,0},{0,.001f,1},{1,.001f,1},{1,.001f,0}};
            case NORTH -> new float[][]{{0,1,.001f},{0,0,.001f},{1,0,.001f},{1,1,.001f}};
            case SOUTH -> new float[][]{{0,0,.999f},{0,1,.999f},{1,1,.999f},{1,0,.999f}};
            case WEST -> new float[][]{{.001f,1,0},{.001f,1,1},{.001f,0,1},{.001f,0,0}};
            case EAST -> new float[][]{{.999f,0,0},{.999f,0,1},{.999f,1,1},{.999f,1,0}};
        };
    }

    private abstract static class Types extends RenderType {
        private Types() { super("tc4_hole", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {}); }
        private static final TransparencyStateShard ADD = new TransparencyStateShard("tc4_hole_add", () -> {
            RenderSystem.enableBlend(); RenderSystem.blendFunc(1, 1);
        }, () -> { RenderSystem.disableBlend(); RenderSystem.defaultBlendFunc(); });
        // The first layer writes depth, just like the original, so later translucent terrain cannot show through the walls.
        private static final RenderType BASE = make("base", "tunnel", false);
        private static final RenderType STARS = make("stars", "particlefield", true);
        private static final RenderType FAR = make("far", "particlefield32", false);
        private static RenderType make(String name, String texture, boolean additive) {
            ResourceLocation path = AlchemyContent.id("textures/misc/" + texture + ".png");
            return create("tc4_hole_" + name, DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 1536, false, false,
                CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader))
                    // particlefield.png.mcmeta enables blur in the original; the tunnel and far texture stay unfiltered.
                    .setTextureState(new TextureStateShard(path, additive, false)).setTransparencyState(additive ? ADD : TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).setWriteMaskState(additive ? COLOR_WRITE : COLOR_DEPTH_WRITE).createCompositeState(false));
        }
    }
}
