package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.blockentity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.*;

/** TileDeconstructionTableRenderer: thaumometer lying on the table, the ghostly spinning item and the aspect being split off. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class DeconstructorRenderer implements BlockEntityRenderer<DeconstructorEntity> {
    private static final ObjParts SCANNER = new ObjParts(AlchemyContent.id("models/scanner_original.obj"));
    private static final ResourceLocation SCANNER_TEX = AlchemyContent.id("textures/models/scanner.png");
    private static final ResourceLocation SCREEN_TEX = AlchemyContent.id("textures/models/scanscreen.png");

    @SubscribeEvent static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(BasicsContent.DECONSTRUCTOR_ENTITY.get(), DeconstructorRenderer::new);
    }
    @SubscribeEvent static void items(RegisterClientExtensionsEvent event) {
        for (var block : java.util.List.of(BasicsContent.DECONSTRUCTOR.get()))
            event.registerItem(new IClientItemExtensions() {
                private BlockEntityWithoutLevelRenderer renderer;
                /** BlockTableRenderer.renderInventoryBlock metadata 14: the whole table renderer, thaumometer included. */
                @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                    if (renderer == null) renderer = new BlockEntityWithoutLevelRenderer(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels()) {
                        @Override public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(block.defaultBlockState(), pose, buffers, light, overlay,
                                net.neoforged.neoforge.client.model.data.ModelData.EMPTY, null);
                            renderScanner(pose, buffers, light, overlay, 0);
                        }
                    };
                    return renderer;
                }
            }, block.asItem());
    }

    public DeconstructorRenderer(BlockEntityRendererProvider.Context context) {}

    private record Hologram(net.minecraft.core.BlockPos pos, ItemStack stack, float ticks) {}
    private static final java.util.List<Hologram> HOLOGRAMS = new java.util.ArrayList<>();

    @EventBusSubscriber(modid = TC4Modern.MODID, value = net.neoforged.api.distmarker.Dist.CLIENT)
    static final class Late {
        @SubscribeEvent static void render(net.neoforged.neoforge.client.event.RenderLevelStageEvent event) {
            if (event.getStage() != net.neoforged.neoforge.client.event.RenderLevelStageEvent.Stage.AFTER_LEVEL || HOLOGRAMS.isEmpty()) return;
            var mc = Minecraft.getInstance();
            var buffers = mc.renderBuffers().bufferSource();
            var type = RenderType.eyes(TextureAtlas.LOCATION_BLOCKS);
            var pose = new PoseStack();
            pose.mulPose(event.getModelViewMatrix());
            var eye = event.getCamera().getPosition();
            pose.translate(-eye.x, -eye.y, -eye.z);
            for (var h : HOLOGRAMS) {
                // EntityItem with hoverStart = sin(t/14)*.2+.2, drawn in-frame with blend 770/1 at alpha .75.
                float hover = (float) Math.sin(h.ticks / 14) * .2f + .2f;
                pose.pushPose();
                pose.translate(h.pos.getX() + .5f, h.pos.getY() + 1.15f, h.pos.getZ() + .5f);
                pose.mulPose(Axis.YP.rotationDegrees(h.ticks % 360));
                pose.translate(0, Math.sin(hover) * .1f + .1f, 0);
                pose.mulPose(Axis.YP.rotationDegrees((float) Math.toDegrees(hover)));
                // In-frame size: blocks 1.25*.25 of a block, flat items .5 (FIXED gives .5 and 1).
                float size = h.stack.getItem() instanceof BlockItem ? .625f : .5f;
                pose.scale(size, size, size);
                mc.getItemRenderer().renderStatic(h.stack, ItemDisplayContext.FIXED, net.minecraft.client.renderer.LightTexture.FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, pose, t -> new Ghost(buffers.getBuffer(type)), mc.level, 0);
                pose.popPose();
            }
            HOLOGRAMS.clear();
            buffers.endBatch(type);
        }
    }

    @Override public void render(DeconstructorEntity table, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var player = Minecraft.getInstance().player;
        float ticks = (player == null ? 0 : player.tickCount) + partial;
        renderScanner(pose, buffers, light, overlay, player == null ? 0 : player.tickCount);
        ItemStack held = table.getItem(0);
        // The ghost is additive without depth writes; drawn here, opaque block-entity models flushed later
        // (a hungry chest behind the table) would paint over it, so it is queued until the level is done.
        if (!held.isEmpty()) HOLOGRAMS.add(new Hologram(table.getBlockPos(), held.copyWithCount(1), ticks));
        var aspect = table.state.aspect;
        if (aspect != null) {
            // UtilsFX.drawTag(-8, -8, aspect, 0, 0, 0, 1, .8f): additive, lying on the scanner screen.
            pose.pushPose();
            pose.translate(.5f, 1.081f, .5f);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(ticks % 360));
            pose.scale(.024f, .024f, .024f);
            var out = buffers.getBuffer(NodeRenderTypes.additive(aspect.getImage()));
            var m = pose.last().pose();
            int c = aspect.getColor(), r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255, a = (int) (.8f * 255);
            out.addVertex(m, -8, 8, 0).setUv(0, 1).setColor(r, g, b, a);
            out.addVertex(m, 8, 8, 0).setUv(1, 1).setColor(r, g, b, a);
            out.addVertex(m, 8, -8, 0).setUv(1, 0).setColor(r, g, b, a);
            out.addVertex(m, -8, -8, 0).setUv(0, 0).setColor(r, g, b, a);
            pose.popPose();
        }
    }

    /**
     * Thaumometer as an in-frame EntityItem: table translate (.5,.92,.5) and scale .8, RenderItem bob .1,
     * ForgeHooksClient in-frame scale 1.25/lift .05/yaw -90 and item scale .5, then ItemThaumometerRenderer ENTITY scale .5.
     */
    static void renderScanner(PoseStack pose, MultiBufferSource buffers, int light, int overlay, int playerTicks) {
        pose.pushPose();
        pose.translate(.5f, .92f, .5f);
        pose.scale(.8f, .8f, .8f);
        pose.translate(0, .1f, 0);
        pose.scale(1.25f, 1.25f, 1.25f);
        pose.translate(0, .05f, 0);
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.scale(.25f, .25f, .25f);
        SCANNER.render("scanner", SCANNER_TEX, pose, buffers, light, overlay, 0xFFFFFFFF);
        pose.translate(0, .11f, 0);
        pose.mulPose(Axis.XP.rotationDegrees(90));
        pose.mulPose(Axis.ZP.rotationDegrees(90));
        pose.scale(2.5f, 2.5f, 2.5f);
        int screenLight = (int) (190 + Math.sin(playerTicks) * 10 + 10);
        var out = buffers.getBuffer(RenderType.entityTranslucent(SCREEN_TEX));
        var last = pose.last();
        for (float[] v : new float[][]{{-.5f, -.5f, 0, 0}, {.5f, -.5f, 1, 0}, {.5f, .5f, 1, 1}, {-.5f, .5f, 0, 1}})
            out.addVertex(last, v[0], v[1], 0).setColor(0xFFFFFFFF).setUv(v[2], v[3]).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(screenLight).setNormal(last, 0, 0, 1);
        pose.popPose();
    }

    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(DeconstructorEntity table) {
        return new net.minecraft.world.phys.AABB(table.getBlockPos()).expandTowards(0, 1, 0);
    }

    /** Fades every vertex to alpha .75 for the additive ghost of the item being broken down. */
    private record Ghost(VertexConsumer out) implements VertexConsumer {
        @Override public VertexConsumer addVertex(float x, float y, float z) { out.addVertex(x, y, z); return this; }
        @Override public VertexConsumer setColor(int r, int g, int b, int a) { out.setColor((int) (r * .75f), (int) (g * .75f), (int) (b * .75f), (int) (a * .75f)); return this; }
        @Override public VertexConsumer setUv(float u, float v) { out.setUv(u, v); return this; }
        @Override public VertexConsumer setUv1(int u, int v) { out.setUv1(u, v); return this; }
        @Override public VertexConsumer setUv2(int u, int v) { out.setUv2(u, v); return this; }
        @Override public VertexConsumer setNormal(float x, float y, float z) { out.setNormal(x, y, z); return this; }
    }
}
