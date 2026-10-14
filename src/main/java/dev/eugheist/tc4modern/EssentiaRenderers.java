package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import thaumcraft.api.aspects.Aspect;

/** Ports of TileAlembicRenderer, TileJarRenderer and BlockTubeRenderer. */
public final class EssentiaRenderers {
    private EssentiaRenderers() {}
    static float lastLabelRotation;
    static int labelRenderCalls;
    static final ResourceLocation ALEMBIC_TEX = AlchemyContent.id("textures/models/alembic.png");
    static final ResourceLocation LABEL_TEX = AlchemyContent.id("textures/models/label.png");
    static final ObjParts ALEMBIC = new ObjParts(AlchemyContent.id("models/alembic.obj"));
    static TextureAtlasSprite sprite(String path) { return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(AlchemyContent.id(path)); }

    /** UtilsFX.renderQuadCenteredFromTexture with brightness -99 (world light) and blend 771. */
    static void labelQuad(PoseStack pose, MultiBufferSource buffers, float scale, int light) {
        var out = buffers.getBuffer(RenderType.entityTranslucent(LABEL_TEX));
        var last = pose.last();
        float h = .5f * scale;
        float[][] v = {{-h, h, 0, 1}, {h, h, 1, 1}, {h, -h, 1, 0}, {-h, -h, 0, 0}};
        for (float[] p : v) out.addVertex(last, p[0], p[1], 0).setColor(0xFFFFFFFF).setUv(p[2], p[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
    }
    /** UtilsFX.drawTag(x, y, aspect): bw silhouette (0.1 grey, alpha 0.8) used on labels. */
    static void labelTag(PoseStack pose, MultiBufferSource buffers, Aspect aspect, int light) {
        var out = buffers.getBuffer(RenderType.entityTranslucent(aspect.getImage()));
        var last = pose.last();
        int c = (int) (0.8f * 255) << 24 | 0x1A1A1A;
        float[][] v = {{-8, 8, 0, 1}, {8, 8, 1, 1}, {8, -8, 1, 0}, {-8, -8, 0, 0}};
        for (float[] p : v) out.addVertex(last, p[0], p[1], 0).setColor(c).setUv(p[2], p[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
    }

    /** Axis-aligned box from block-space bounds, UVs taken from the bounds like RenderBlocks.renderStandardBlock. */
    static void box(PoseStack pose, VertexConsumer out, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light) {
        var last = pose.last();
        quad(out, last, s, color, light, 0, -1, 0, new float[][]{{x0, y0, z1, x0, z1}, {x0, y0, z0, x0, z0}, {x1, y0, z0, x1, z0}, {x1, y0, z1, x1, z1}});
        quad(out, last, s, color, light, 0, 1, 0, new float[][]{{x0, y1, z0, x0, z0}, {x0, y1, z1, x0, z1}, {x1, y1, z1, x1, z1}, {x1, y1, z0, x1, z0}});
        quad(out, last, s, color, light, 0, 0, -1, new float[][]{{x1, y1, z0, 1 - x1, 1 - y1}, {x1, y0, z0, 1 - x1, 1 - y0}, {x0, y0, z0, 1 - x0, 1 - y0}, {x0, y1, z0, 1 - x0, 1 - y1}});
        quad(out, last, s, color, light, 0, 0, 1, new float[][]{{x0, y1, z1, x0, 1 - y1}, {x0, y0, z1, x0, 1 - y0}, {x1, y0, z1, x1, 1 - y0}, {x1, y1, z1, x1, 1 - y1}});
        quad(out, last, s, color, light, -1, 0, 0, new float[][]{{x0, y1, z0, z0, 1 - y1}, {x0, y0, z0, z0, 1 - y0}, {x0, y0, z1, z1, 1 - y0}, {x0, y1, z1, z1, 1 - y1}});
        quad(out, last, s, color, light, 1, 0, 0, new float[][]{{x1, y1, z1, 1 - z1, 1 - y1}, {x1, y0, z1, 1 - z1, 1 - y0}, {x1, y0, z0, 1 - z0, 1 - y0}, {x1, y1, z0, 1 - z0, 1 - y1}});
    }
    private static void quad(VertexConsumer out, PoseStack.Pose last, TextureAtlasSprite s, int color, int light, float nx, float ny, float nz, float[][] v) {
        for (float[] p : v) {
            float u = s.getU(clamp01(p[3])), w = s.getV(clamp01(p[4]));
            out.addVertex(last, p[0], p[1], p[2]).setColor(color).setUv(u, w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, nx, ny, nz);
        }
    }
    private static float clamp01(float f) { return f - (float) Math.floor(f == 1f ? 0 : f) ; }

    public static final class AlembicRenderer implements BlockEntityRenderer<AlembicEntity> {
        public AlembicRenderer(BlockEntityRendererProvider.Context context) {}
        @Override public void render(AlembicEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) { renderAlembic(tile, pose, buffers, light, overlay); }
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(AlembicEntity tile) { return tile.getRenderBoundingBox(); }
    }
    static void renderAlembic(AlembicEntity tile, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        pose.translate(.5f, 0, .5f);
        pose.pushPose();
        pose.mulPose(Axis.XN.rotationDegrees(90));
        if (tile != null) {
            switch (tile.state.facing) {
                case 5 -> pose.mulPose(Axis.ZP.rotationDegrees(180));
                case 3 -> pose.mulPose(Axis.ZP.rotationDegrees(90));
                case 2 -> pose.mulPose(Axis.ZP.rotationDegrees(270));
                default -> {}
            }
            if (tile.aboveFurnace()) { ALEMBIC.render("TubeMain", ALEMBIC_TEX, pose, buffers, light, overlay, 0xFFFFFFFF); ALEMBIC.render("Legs", ALEMBIC_TEX, pose, buffers, light, overlay, 0xFFFFFFFF); }
            else if (tile.aboveAlembic()) { ALEMBIC.render("TubeMain", ALEMBIC_TEX, pose, buffers, light, overlay, 0xFFFFFFFF); ALEMBIC.render("TubeSmall", ALEMBIC_TEX, pose, buffers, light, overlay, 0xFFFFFFFF); }
            else ALEMBIC.render("Legs", ALEMBIC_TEX, pose, buffers, light, overlay, 0xFFFFFFFF);
        } else {
            pose.translate(0, 0, -.4f);
            ALEMBIC.render("Legs", ALEMBIC_TEX, pose, buffers, light, overlay, 0xFFFFFFFF);
        }
        ALEMBIC.render("Pot", ALEMBIC_TEX, pose, buffers, light, overlay, 0xFFFFFFFF);
        ALEMBIC.render("Panel", ALEMBIC_TEX, pose, buffers, light, overlay, 0xFFFFFFFF);
        pose.popPose();
        if (tile != null && tile.state.aspectFilter != null) {
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(90));
            switch (tile.state.facing) {
                case 5 -> pose.mulPose(Axis.YP.rotationDegrees(180));
                case 3 -> pose.mulPose(Axis.YP.rotationDegrees(90));
                case 2 -> pose.mulPose(Axis.YP.rotationDegrees(270));
                default -> {}
            }
            pose.pushPose();
            pose.translate(0, .468f, -.409f);
            labelQuad(pose, buffers, .27f, light);
            pose.popPose();
            pose.pushPose();
            pose.translate(0, .468f, -.41f);
            pose.scale(.013f, .013f, .013f);
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            labelTag(pose, buffers, tile.state.aspectFilter, light);
            pose.popPose();
            pose.popPose();
        }
        pose.popPose();
    }

    public static final class JarRenderer implements BlockEntityRenderer<WardedJarEntity> {
        public JarRenderer(BlockEntityRendererProvider.Context context) {}
        @Override public void render(WardedJarEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            renderJarContents(tile.state.aspect, tile.state.amount, tile.state.maxAmount, tile.state.aspectFilter, tile.state.facing, tile.getBlockPos().getX(), pose, buffers, light);
        }
    }
    /** TileJarRenderer.renderLiquid and label; the GL 180-degree X flips cancel for the liquid. */
    static void renderJarContents(Aspect aspect, int amount, int max, Aspect filter, int facing, int x, PoseStack pose, MultiBufferSource buffers, int light) {
        if (amount > 0) {
            float level = (float) amount / max * .625f;
            int color = aspect == null ? 0xFFFFFFFF : 0xFF000000 | aspect.getColor();
            int bright = Math.max(200 & 0xFFFF, light & 0xFFFF) | (light & 0xFFFF0000);
            box(pose, buffers.getBuffer(RenderType.entityTranslucent(InventoryMenu.BLOCK_ATLAS)), sprite("blocks/animatedglow"), .25f, .0625f, .25f, .75f, .0625f + level, .75f, color, bright);
        }
        if (filter != null) {
            pose.pushPose();
            pose.translate(.5f, .01f, .5f);
            pose.mulPose(Axis.XP.rotationDegrees(180));
            switch (facing) {
                case 3 -> pose.mulPose(Axis.YP.rotationDegrees(180));
                case 5 -> pose.mulPose(Axis.YP.rotationDegrees(90));
                case 4 -> pose.mulPose(Axis.YP.rotationDegrees(270));
                default -> {}
            }
            float rot = WarpClientConfig.crooked()?(filter.getTag().hashCode() + x + facing) % 4 - 2:0;
            lastLabelRotation=rot;labelRenderCalls++;
            pose.pushPose();
            pose.translate(0, -.4f, .315f);
            pose.mulPose(Axis.ZP.rotationDegrees(rot));
            labelQuad(pose, buffers, .5f, light);
            pose.popPose();
            pose.pushPose();
            pose.translate(0, -.4f, .316f);
            pose.mulPose(Axis.ZP.rotationDegrees(rot));
            pose.scale(.021f, .021f, .021f);
            pose.mulPose(Axis.YP.rotationDegrees(180));
            labelTag(pose, buffers, filter, light);
            pose.popPose();
            pose.popPose();
        }
    }

    public static final class TubeRenderer implements BlockEntityRenderer<EssentiaTubeEntity> {
        public TubeRenderer(BlockEntityRendererProvider.Context context) {}
        @Override public void render(EssentiaTubeEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var kind = tile.getBlockState().getBlock() instanceof EssentiaTubes.TubeBlock block ? block.kind : EssentiaTubes.Kind.TUBE;
            int filter = tile.state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Filter f && f.aspectFilter != null ? f.aspectFilter.getColor() : 0xFFFFFF;
            tubeBody(tile, tile.getLevel(), tile.getBlockPos(), kind, filter, pose, buffers, light);
            TubeDecorations.render(tile, kind, pose, buffers, light, overlay);
        }
    }
    public static final class BufferRenderer implements BlockEntityRenderer<EssentiaBufferEntity> {
        public BufferRenderer(BlockEntityRendererProvider.Context context) {}
        @Override public void render(EssentiaBufferEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            tubeBody(tile, tile.getLevel(), tile.getBlockPos(), EssentiaTubes.Kind.BUFFER, 0xFFFFFF, pose, buffers, light);
            TubeDecorations.buffer(tile, pose, buffers, light, overlay);
        }
    }
    /**
     * BlockTubeRenderer.renderWorldBlock: one arm per axis using the block icon (buffer and restricted tubes have their own),
     * then the junction: filter frame + aspect-tinted core, buffer cube 4..12, valve/end cap pipe_2, else the small pipe_3 knot.
     */
    static void tubeBody(thaumcraft.api.aspects.IEssentiaTransport tube, net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, EssentiaTubes.Kind kind, int filterColor, PoseStack pose, MultiBufferSource buffers, int light) {
        float W4 = 4 / 16f, W6 = 6 / 16f, W7 = 7 / 16f, W9 = 9 / 16f, W10 = 10 / 16f, W12 = 12 / 16f, e = .03125f;
        float[] ax = {W7, W7, W7, W9, W9, W9}, ay = {W7, W7, W7, W9, W9, W9}, az = {W7, W7, W7, W9, W9, W9};
        boolean drawX = false, drawY = false, drawZ = false, notConduit = false;
        for (Direction d : Direction.values()) {
            if (!tube.isConnectable(d)) continue;
            var te = EssentiaTransport.connectable(level, pos, d);
            if (te == null) continue;
            if (!(te instanceof EssentiaTubeEntity)) notConduit = true;
            float ext = te.renderExtendedTube() ? W6 : 0;
            switch (d) {
                case DOWN -> { ay[1] = -ext; drawY = true; }
                case UP -> { ay[4] = 1 + ext; drawY = true; }
                case NORTH -> { az[2] = -ext; drawZ = true; }
                case SOUTH -> { az[5] = 1 + ext; drawZ = true; }
                case WEST -> { ax[0] = -ext; drawX = true; }
                case EAST -> { ax[3] = 1 + ext; drawX = true; }
            }
        }
        var out = buffers.getBuffer(RenderType.entityCutout(InventoryMenu.BLOCK_ATLAS));
        var arm = sprite(kind == EssentiaTubes.Kind.BUFFER ? "blocks/pipe_buffer" : kind == EssentiaTubes.Kind.RESTRICT ? "blocks/pipe_restrict" : "blocks/pipe_1");
        int drawn = 0;
        if (drawX) { drawn++; box(pose, out, arm, ax[0], ax[1], ax[2], ax[3], ax[4], ax[5], 0xFFFFFFFF, light); }
        if (drawY) { drawn++; box(pose, out, arm, ay[0], ay[1], ay[2], ay[3], ay[4], ay[5], 0xFFFFFFFF, light); }
        if (drawZ) { drawn++; box(pose, out, arm, az[0], az[1], az[2], az[3], az[4], az[5], 0xFFFFFFFF, light); }
        if (kind == EssentiaTubes.Kind.FILTER) {
            box(pose, out, sprite("blocks/pipe_filter"), W6 - e, W6 - e, W6 - e, W10 + e, W10 + e, W10 + e, 0xFFFFFFFF, light);
            box(pose, out, sprite("blocks/pipe_filter_core"), W6 - e, W6 - e, W6 - e, W10 + e, W10 + e, W10 + e, 0xFF000000 | filterColor, light);
        } else if (kind == EssentiaTubes.Kind.BUFFER) box(pose, out, arm, W4, W4, W4, W12, W12, W12, 0xFFFFFFFF, light);
        else if (drawn == 0 || notConduit || kind == EssentiaTubes.Kind.VALVE) box(pose, out, sprite("blocks/pipe_2"), W6, W6, W6, W10, W10, W10, 0xFFFFFFFF, light);
        else box(pose, out, kind == EssentiaTubes.Kind.RESTRICT ? arm : sprite("blocks/pipe_3"), W7 - e, W7 - e, W7 - e, W9 + e, W9 + e, W9 + e, 0xFFFFFFFF, light);
    }
}
