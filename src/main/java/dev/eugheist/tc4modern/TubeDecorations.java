package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import org.joml.Quaternionf;

/** TileTubeValveRenderer, TileTubeOnewayRenderer and TileTubeBufferRenderer (all built on ModelTubeValve's ValveRod). */
final class TubeDecorations {
    private TubeDecorations() {}
    static final net.minecraft.resources.ResourceLocation VALVE_TEX = AlchemyContent.id("textures/models/valve.png");
    static final net.minecraft.resources.ResourceLocation VALVE_WHEEL = AlchemyContent.id("textures/blocks/pipe_valve.png");
    /** ModelTubeValve.ValveRod: box (-1, 2, -1) 2x2x2 at texture (0, 10) of 64x32, mirrored. */
    static final ModelPart ROD = rod();
    static ModelPart rod() {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("rod", CubeListBuilder.create().texOffs(0, 10).mirror().addBox(-1, 2, -1, 2, 2, 2), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32).bakeRoot();
    }
    /** The shared GL rotation that points the model's +Y along the given side. */
    static void orient(PoseStack pose, Direction fd) {
        if (fd.getStepY() == 0) pose.mulPose(Axis.YP.rotationDegrees(90));
        else { pose.mulPose(Axis.XN.rotationDegrees(90)); pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(90), fd.getStepY(), 0, 0)); }
        pose.mulPose(new Quaternionf().rotationAxis((float) Math.toRadians(90), fd.getStepX(), fd.getStepY(), fd.getStepZ()));
    }
    static void rod(PoseStack pose, MultiBufferSource buffers, int light, int overlay, int color) { ROD.render(pose, buffers.getBuffer(RenderType.entityCutout(VALVE_TEX)), light, overlay, color); }

    static void render(EssentiaTubeEntity tile, EssentiaTubes.Kind kind, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        if (tile.state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Valve valve) {
            pose.pushPose();
            pose.translate(.5, .5, .5);
            orient(pose, valve.facing);
            pose.mulPose(Axis.YP.rotationDegrees(-valve.rotation * 1.5f));
            pose.translate(0, -(valve.rotation / 360f) * .12f, 0);
            rod(pose, buffers, light, overlay, 0xFFFFFFFF);
            // renderValve: the pipe_valve icon as a 2D-extruded item, 0.5 scale, 0.1 thick.
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.translate(-.25, -.25, -.25);
            pose.scale(.5f, .5f, .5f);
            item2D(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(VALVE_WHEEL)), .1f, light, overlay);
            pose.popPose();
            pose.popPose();
        } else if (kind == EssentiaTubes.Kind.ONEWAY) {
            var level = tile.getLevel();
            if (level != null && EssentiaTransport.connectable(level, tile.getBlockPos(), tile.state.facing.getOpposite()) == null) return;
            pose.pushPose();
            pose.translate(.5, .5, .5);
            orient(pose, tile.state.facing);
            pose.scale(1.1f, .5f, 1.1f);
            pose.translate(0, -.5, 0);
            int blue = 0xFF000000 | (int) (.45f * 255) << 16 | (int) (.5f * 255) << 8 | 255;
            for (int i = 0; i < 3; i++) { if (i > 0) pose.translate(0, -.25, 0); rod(pose, buffers, light, overlay, blue); }
            pose.popPose();
        }
    }
    static void buffer(EssentiaBufferEntity tile, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var level = tile.getLevel();
        if (level == null) return;
        for (Direction dir : Direction.values()) {
            int i = dir.get3DDataValue();
            if (tile.state.chokedSides[i] == 0 || !tile.state.openSides[i] || EssentiaTransport.connectable(level, tile.getBlockPos(), dir) == null) continue;
            pose.pushPose();
            pose.translate(.5, .5, .5);
            orient(pose, dir.getOpposite());
            pose.scale(1.2f, 1, 1.2f);
            pose.translate(0, -.5, 0);
            int c = tile.state.chokedSides[i] == 2 ? 0xFFFF4C4C : 0xFF4C4CFF;
            rod(pose, buffers, light, overlay, c);
            pose.popPose();
        }
    }
    /** ItemRenderer.renderItemIn2D(maxU, minV, minU, maxV, 16, 16, depth): front, back and one edge strip per pixel. */
    static void item2D(PoseStack pose, VertexConsumer out, float depth, int light, int overlay) {
        var last = pose.last();
        face(out, last, light, overlay, 0, 0, 1, new float[][]{{0, 0, 0, 1, 1}, {1, 0, 0, 0, 1}, {1, 1, 0, 0, 0}, {0, 1, 0, 1, 0}});
        face(out, last, light, overlay, 0, 0, -1, new float[][]{{0, 1, -depth, 1, 0}, {1, 1, -depth, 0, 0}, {1, 0, -depth, 0, 1}, {0, 0, -depth, 1, 1}});
        for (int i = 0; i < 16; i++) {
            float a = i / 16f, b = (i + 1) / 16f, u = 1 - (i + .5f) / 16f, v = (i + .5f) / 16f;
            face(out, last, light, overlay, -1, 0, 0, new float[][]{{a, 0, -depth, u, 1}, {a, 0, 0, u, 1}, {a, 1, 0, u, 0}, {a, 1, -depth, u, 0}});
            face(out, last, light, overlay, 1, 0, 0, new float[][]{{b, 1, -depth, u, 0}, {b, 1, 0, u, 0}, {b, 0, 0, u, 1}, {b, 0, -depth, u, 1}});
            face(out, last, light, overlay, 0, 1, 0, new float[][]{{0, 1 - a, 0, 1, v}, {1, 1 - a, 0, 0, v}, {1, 1 - a, -depth, 0, v}, {0, 1 - a, -depth, 1, v}});
            face(out, last, light, overlay, 0, -1, 0, new float[][]{{0, 1 - b, -depth, 1, v}, {1, 1 - b, -depth, 0, v}, {1, 1 - b, 0, 0, v}, {0, 1 - b, 0, 1, v}});
        }
    }
    private static void face(VertexConsumer out, PoseStack.Pose pose, int light, int overlay, float nx, float ny, float nz, float[][] vertices) {
        for (float[] v : vertices) out.addVertex(pose, v[0], v[1], v[2]).setColor(0xFFFFFFFF).setUv(v[3], v[4]).setOverlay(overlay).setLight(light).setNormal(pose, nx, ny, nz);
    }
}
