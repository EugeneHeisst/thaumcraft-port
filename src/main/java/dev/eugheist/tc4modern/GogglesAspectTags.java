package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import thaumcraft.api.aspects.*;

/** RenderEventHandler.blockHighlight/drawTagsOnContainer: IGoggles popups over every aspect container in view. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class GogglesAspectTags {
    private GogglesAspectTags() {}
    static float tagscale;

    /** IAspectContainer.getAspects of the ported tiles that implement it in the original. */
    static AspectList aspects(BlockEntity tile) {
        if (tile instanceof AuraNodeEntity node) return node.reservoir.aspects;
        if (tile instanceof EnergizedNodeEntity node) return node.energy.visBase;
        if (tile instanceof NodeJarEntity jar) { var list = new AspectList(); if (jar.snapshot != null) list.readFromNBT(jar.snapshot); return list; }
        if (tile instanceof EssentiaBufferEntity buffer) return buffer.state.aspects.size() > 0 ? buffer.state.aspects : null;
        // TileTubeFilter.getAspects: the filter aspect with amount -1.
        if (tile instanceof EssentiaTubeEntity tube && tube.state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Filter f) return f.aspectFilter == null ? null : new AspectList().add(f.aspectFilter, -1);
        if (tile instanceof WardedJarEntity jar) return jar.state.aspect != null && jar.state.amount > 0 ? new AspectList().add(jar.state.aspect, jar.state.amount) : null;
        if (tile instanceof AlembicEntity alembic) return alembic.state.aspect != null ? new AspectList().add(alembic.state.aspect, alembic.state.amount) : null;
        if (tile instanceof EssentiaReservoir.ReservoirEntity reservoir) return reservoir.getAspects();
        if (tile instanceof CrucibleEntity crucible) return crucible.crucible.aspects;
        if (tile instanceof ManaPodEntity pod) return pod.aspect != null && pod.getBlockState().getValue(ManaPodBlock.AGE) == 7 ? new AspectList().add(pod.aspect, 1) : null;
        return null;
    }

    @SubscribeEvent static void highlight(RenderHighlightEvent.Block event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        var helmet = mc.player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof thaumcraft.api.IGoggles goggles) || !goggles.showIngamePopups(helmet, mc.player)) return;
        var hit = event.getTarget();
        var pos = hit.getBlockPos();
        var tags = aspects(mc.level.getBlockEntity(pos));
        if (tags == null || tags.size() == 0) return;
        boolean spaceAbove = mc.level.isEmptyBlock(pos.above());
        if (tagscale < .3f) tagscale += .031f - tagscale / 10;
        draw(event.getPoseStack(), event.getMultiBufferSource(), event.getCamera().getPosition(),
            pos.getX(), pos.getY() + (spaceAbove ? .4 : 0), pos.getZ(), tags, spaceAbove ? Direction.UP : hit.getDirection());
    }

    /** renderLast decay, once per frame. */
    @SubscribeEvent static void frame(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL && tagscale > 0) tagscale = Math.max(0, tagscale - .005f);
    }

    static void draw(PoseStack pose, MultiBufferSource buffers, net.minecraft.world.phys.Vec3 eye, double x, double y, double z, AspectList tags, Direction dir) {
        var font = Minecraft.getInstance().font;
        var known = EssentiaItems.ClientKnowledgeView.discovered();
        int rowsize = 5, current = 0, left = tags.size();
        float shifty = 0;
        for (Aspect tag : tags.getAspects()) {
            int div = Math.min(left, rowsize);
            if (current >= rowsize) {
                current = 0;
                shifty -= tagscale * 1.05f;
                if ((left -= rowsize) < rowsize) div = left % rowsize;
            }
            float shift = (current - div / 2f + .5f) * tagscale * 4 * tagscale;
            int color = tag.getColor();
            boolean discovered = known.contains(tag.getTag());
            pose.pushPose();
            pose.translate(x + .5 + tagscale * 2 * dir.getStepX() - eye.x, y - shifty + .5 + tagscale * 2 * dir.getStepY() - eye.y, z + .5 + tagscale * 2 * dir.getStepZ() - eye.z);
            float rotYaw = (float) Math.toDegrees(Math.atan2(eye.x - (x + .5), eye.z - (z + .5)));
            pose.mulPose(Axis.YP.rotationDegrees(rotYaw + 180));
            pose.translate(shift, 0, 0);
            pose.mulPose(Axis.ZP.rotationDegrees(180));
            pose.scale(tagscale, tagscale, tagscale);
            var texture = ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/aspects/" + (discovered ? tag.getTag() : "_unknown") + ".png");
            var quad = buffers.getBuffer(Types.tag(texture));
            var m = pose.last().pose();
            int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255, a = (int) (.75f * 255);
            quad.addVertex(m, -.5f, .5f, 0).setColor(r, g, b, a).setUv(0, 1);
            quad.addVertex(m, .5f, .5f, 0).setColor(r, g, b, a).setUv(1, 1);
            quad.addVertex(m, .5f, -.5f, 0).setColor(r, g, b, a).setUv(1, 0);
            quad.addVertex(m, -.5f, -.5f, 0).setColor(r, g, b, a).setUv(0, 0);
            if (tags.getAmount(tag) >= 0) {
                String amount = String.valueOf(tags.getAmount(tag));
                pose.scale(.04f, .04f, .04f);
                pose.translate(0, 6, -.1);
                int sw = font.width(amount);
                font.drawInBatch(amount, 14 - sw, 1, 0x111111, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
                pose.translate(0, 0, -.1);
                font.drawInBatch(amount, 13 - sw, 0, 0xFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.SEE_THROUGH, 0, LightTexture.FULL_BRIGHT);
            }
            pose.popPose();
            current++;
        }
    }

    /** renderQuadCenteredFromTexture with GL_DEPTH_TEST disabled and blend 770/771. */
    private abstract static class Types extends RenderType {
        private Types() { super("tc4_goggles", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {}); }
        private static final java.util.Map<ResourceLocation, RenderType> CACHE = new java.util.HashMap<>();
        static RenderType tag(ResourceLocation texture) {
            return CACHE.computeIfAbsent(texture, t -> create("tc4_goggles_tag", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, true,
                CompositeState.builder().setShaderState(new ShaderStateShard(GameRenderer::getPositionTexColorShader)).setTextureState(new TextureStateShard(t, false, false))
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDepthTestState(NO_DEPTH_TEST).setCullState(NO_CULL)
                    .setWriteMaskState(COLOR_WRITE).createCompositeState(false)));
        }
    }
}
