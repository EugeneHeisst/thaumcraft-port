package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;

/** TileRunicMatrixRenderer, TileInfusionPillarRenderer and TilePedestalRenderer ports. */
public final class InfusionRenderers {
    private InfusionRenderers() {}
    static final net.minecraft.resources.ResourceLocation INFUSER = AlchemyContent.id("textures/models/infuser.png");
    static final net.minecraft.resources.ResourceLocation PILLAR_TEX = AlchemyContent.id("textures/models/pillar.png");
    static final ObjParts PILLAR = new ObjParts(AlchemyContent.id("models/pillar.obj"));
    /** ModelCube(shift): 16^3 box at texture offset (0, shift) of a 64x64 sheet, mirrored. */
    static ModelPart cube(int shift) {
        var mesh = new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("cube", CubeListBuilder.create().texOffs(0, shift).mirror().addBox(-8, -8, -8, 16, 16, 16), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64).bakeRoot();
    }
    /**
     * The active-matrix overlay: original glBlendFunc(SRC_ALPHA, ONE) with glColor4f(.8,.1,1, (sin/4*.1+.2)*startUp).
     * RenderType.eyes blends ONE/ONE and ignored that 0.2 alpha, making the cubes about five times too bright.
     */
    /** Audit-only comparison with the previous ONE/ONE overlay. */
    static boolean auditLegacyGlow;
    static final class Glow extends RenderType {
        private Glow() { super("", null, null, 0, false, false, null, null); }
        private static final TransparencyStateShard SRC_ALPHA_ONE = new TransparencyStateShard("tc4_matrix_glow", () -> {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend(); com.mojang.blaze3d.systems.RenderSystem.blendFunc(770, 1);
        }, () -> { com.mojang.blaze3d.systems.RenderSystem.disableBlend(); com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc(); });
        static final RenderType TYPE = create("tc4_matrix_glow", com.mojang.blaze3d.vertex.DefaultVertexFormat.NEW_ENTITY, com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS, 1536, false, true,
                CompositeState.builder().setShaderState(RENDERTYPE_EYES_SHADER).setTextureState(new TextureStateShard(INFUSER, false, false))
                        .setTransparencyState(SRC_ALPHA_ONE).setWriteMaskState(COLOR_WRITE).createCompositeState(false));
    }
    public static final class MatrixRenderer implements BlockEntityRenderer<InfusionContent.MatrixEntity> {
        private final ModelPart model = cube(0), over = cube(32);
        public MatrixRenderer(BlockEntityRendererProvider.Context c) {}
        @Override public void render(InfusionContent.MatrixEntity is, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            var mc = Minecraft.getInstance();
            float ticks = (mc.getCameraEntity() == null ? 0 : mc.getCameraEntity().tickCount) + partial;
            pose.pushPose();
            pose.translate(.5f, .5f, .5f);
            pose.mulPose(Axis.YP.rotationDegrees(ticks % 360f * is.startUp));
            pose.mulPose(Axis.XP.rotationDegrees(35f * is.startUp));
            pose.mulPose(Axis.ZP.rotationDegrees(45f * is.startUp));
            // DEVIATION: the original only capped this at 6. A large stabiliser field drives symmetry (and so instability) far
            // below zero, and the negative factor made the eight cubes fly apart; bound it symmetrically.
            float instability = Mth.clamp(1f + is.logic.instability * .66f * (Math.min(is.craftCount, 50) / 50f), -6f, 6f);
            for (int pass = 0; pass < (is.logic.active ? 2 : 1); pass++) {
                for (int a = 0; a < 2; ++a) for (int b = 0; b < 2; ++b) for (int c = 0; c < 2; ++c) {
                    float b1 = 0, b2 = 0, b3 = 0;
                    if (is.logic.active) {
                        b1 = Mth.sin((ticks + a * 10) / (15f - instability / 2f)) * .01f * is.startUp * instability;
                        b2 = Mth.sin((ticks + b * 10) / (14f - instability / 2f)) * .01f * is.startUp * instability;
                        b3 = Mth.sin((ticks + c * 10) / (13f - instability / 2f)) * .01f * is.startUp * instability;
                    }
                    pose.pushPose();
                    pose.translate(b1 + (a == 0 ? -1 : 1) * .25f, b2 + (b == 0 ? -1 : 1) * .25f, b3 + (c == 0 ? -1 : 1) * .25f);
                    if (a > 0) pose.mulPose(Axis.XP.rotationDegrees(90));
                    if (b > 0) pose.mulPose(Axis.YP.rotationDegrees(90));
                    if (c > 0) pose.mulPose(Axis.ZP.rotationDegrees(90));
                    pose.scale(.45f, .45f, .45f);
                    if (pass == 0) model.render(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(INFUSER)), light, overlay);
                    else {
                        float alpha = (Mth.sin((ticks + a * 2 + b * 3 + c * 4) / 4f) * .1f + .2f) * is.startUp;
                        int col = (int) (alpha * 255) << 24 | (int) (.8f * 255) << 16 | (int) (.1f * 255) << 8 | 255;
                        over.render(pose, buffers.getBuffer(auditLegacyGlow ? RenderType.eyes(INFUSER) : Glow.TYPE), 0xF000F0, overlay, col);
                    }
                    pose.popPose();
                }
            }
            pose.popPose();
            if (is.logic.crafting) drawHalo(pose, buffers, is.craftCount);
        }
        /** drawHalo: 20 additive triangle fans in white-to-purple, growing with craftCount. */
        private static void drawHalo(PoseStack pose, MultiBufferSource buffers, int count) {
            pose.pushPose();
            pose.translate(.5f, .5f, .5f);
            var out = buffers.getBuffer(RenderType.lightning());
            var rnd = new java.util.Random(245L);
            float f1 = count / 500f;
            for (int i = 0; i < 20; ++i) {
                pose.mulPose(Axis.XP.rotationDegrees(rnd.nextFloat() * 360f));
                pose.mulPose(Axis.YP.rotationDegrees(rnd.nextFloat() * 360f));
                pose.mulPose(Axis.ZP.rotationDegrees(rnd.nextFloat() * 360f));
                pose.mulPose(Axis.XP.rotationDegrees(rnd.nextFloat() * 360f));
                pose.mulPose(Axis.YP.rotationDegrees(rnd.nextFloat() * 360f));
                pose.mulPose(Axis.ZP.rotationDegrees(rnd.nextFloat() * 360f + f1 * 360f));
                float fa = rnd.nextFloat() * 20f + 5f, f4 = rnd.nextFloat() * 2f + 1f;
                float div = 20f / (Math.min(count, 50) / 50f);
                fa /= div; f4 /= div;
                var m = pose.last().pose();
                float[][] tip = {{-.866f * f4, fa, -.5f * f4}, {.866f * f4, fa, -.5f * f4}, {0, fa, f4}, {-.866f * f4, fa, -.5f * f4}};
                for (int t = 0; t < 3; t++) {
                    out.addVertex(m, 0, 0, 0).setColor(255, 255, 255, 255);
                    out.addVertex(m, tip[t][0], tip[t][1], tip[t][2]).setColor(0xCC, 0, 0xFF, 0);
                    out.addVertex(m, tip[t + 1][0], tip[t + 1][1], tip[t + 1][2]).setColor(0xCC, 0, 0xFF, 0);
                    out.addVertex(m, 0, 0, 0).setColor(255, 255, 255, 255);
                }
            }
            pose.popPose();
        }
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(InfusionContent.MatrixEntity t) { return new net.minecraft.world.phys.AABB(t.getBlockPos()).inflate(1); }
    }
    public static final class PillarRenderer implements BlockEntityRenderer<InfusionContent.PillarEntity> {
        public PillarRenderer(BlockEntityRendererProvider.Context c) {}
        @Override public void render(InfusionContent.PillarEntity tile, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            pose.pushPose();
            pose.translate(.5f, 0, .5f);
            pose.mulPose(Axis.XN.rotationDegrees(90));
            if (tile.orientation == 3) pose.mulPose(Axis.ZP.rotationDegrees(90));
            else if (tile.orientation == 4) pose.mulPose(Axis.ZP.rotationDegrees(270));
            else if (tile.orientation == 5) pose.mulPose(Axis.ZP.rotationDegrees(180));
            // Opaque stone: the translucent entity batch is drawn after particles, so essentia trails showed through it.
            PILLAR.render("Box001", net.minecraft.client.renderer.RenderType.entityCutoutNoCull(PILLAR_TEX), pose, buffers, light, overlay, 0xFFFFFFFF);
            pose.popPose();
        }
        @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(InfusionContent.PillarEntity t) { return new net.minecraft.world.phys.AABB(t.getBlockPos()).inflate(1).expandTowards(0, 1, 0); }
    }
    public static final class PedestalRenderer implements BlockEntityRenderer<InfusionContent.PedestalEntity> {
        public PedestalRenderer(BlockEntityRendererProvider.Context c) {}
        /** Floating, bobbing and spinning item 1.15 above the pedestal; blocks drawn at double scale. */
        @Override public void render(InfusionContent.PedestalEntity ped, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            if (ped.item().isEmpty()) return;
            var mc = Minecraft.getInstance();
            float ticks = (mc.getCameraEntity() == null ? 0 : mc.getCameraEntity().tickCount) + partial;
            float h = Mth.sin(ticks % 32767f / 16f) * .05f;
            pose.pushPose();
            pose.translate(.5f, 1.15f + h, .5f);
            pose.mulPose(Axis.YP.rotationDegrees(ticks % 360f));
            // EntityItem rendering: dropped-item transform at 0.25 block scale; ItemBlock doubled.
            float s = ped.item().getItem() instanceof BlockItem ? .5f : .5f;
            pose.scale(s * 2, s * 2, s * 2);
            mc.getItemRenderer().renderStatic(ped.item(), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, ped.getLevel(), 0);
            pose.popPose();
        }
    }
}
