package dev.eugheist.tc4modern;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** TileWandPedestalRenderer: the item hovers and spins above the pedestal; while charging, a wispy line runs to the node. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class WandPedestalRenderer implements BlockEntityRenderer<WandPedestal.PedestalEntity> {
    private static final ResourceLocation WISPY = AlchemyContent.id("textures/misc/wispy.png");
    @SubscribeEvent static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(WandPedestal.PEDESTAL_ENTITY.get(), WandPedestalRenderer::new);
    }
    public WandPedestalRenderer(BlockEntityRendererProvider.Context context) {}

    @Override public void render(WandPedestal.PedestalEntity ped, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var item = ped.item();
        if (item.isEmpty() || ped.getLevel() == null) return;
        var viewer = Minecraft.getInstance().getCameraEntity();
        float ticks = (viewer == null ? 0 : viewer.tickCount) + partial;
        float h = Mth.sin(ticks % 32767 / 16) * .05f;
        pose.pushPose();
        // An EntityItem with hoverStart 0 also lifts by 0.1 before its ground transform.
        pose.translate(.5f, 1.15f + h + .1f, .5f);
        pose.mulPose(Axis.YP.rotationDegrees(ticks % 360));
        Minecraft.getInstance().getItemRenderer().renderStatic(item.copyWithCount(1), ItemDisplayContext.GROUND, light, OverlayTexture.NO_OVERLAY, pose, buffers, ped.getLevel(), 0);
        pose.popPose();
        if (ped.draining) {
            var pos = ped.getBlockPos();
            floatyLine(pose, buffers, pos, pos.getX() + .5, pos.getY() + 1.65 - h * 2, pos.getZ() + .5,
                ped.drain.getX() + .5, ped.drain.getY() + .5, ped.drain.getZ() + .5, ped.drainColor, -.02f, Math.min(ticks, 10) / 10f, .15f);
        }
    }

    /** UtilsFX.drawFloatyLine: two crossed additive ribbons that wave along the line, fading at both ends. */
    static void floatyLine(PoseStack pose, MultiBufferSource buffers, net.minecraft.core.BlockPos origin, double x, double y, double z,
                           double x2, double y2, double z2, int color, float speed, float growth, float width) {
        float time = System.nanoTime() / 30000000L;
        float r = (color >> 16 & 255) / 255f, g = (color >> 8 & 255) / 255f, b = (color & 255) / 255f;
        double cx = x - x2, cy = y - y2, cz = z - z2;
        float dist = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
        float length = Math.round(dist) * 8f;
        if (length <= 0) return;
        pose.pushPose();
        pose.translate(x2 - origin.getX(), y2 - origin.getY(), z2 - origin.getZ());
        var out = buffers.getBuffer(NodeRenderTypes.additive(WISPY));
        var m = pose.last().pose();
        for (int strip = 0; strip < 2; strip++) {
            float[] prev = null;
            for (int i = 0; i <= length * growth; i++) {
                float f2 = i / length, f3 = 1 - Math.abs(i - length / 2) / (length / 2);
                float phase = dist * (1 - f2) * 8 - time % 32767f / 5;
                float dx = (float) (cx + Mth.sin((float) ((z % 16 + phase) / 4)) * .5f * f3);
                float dy = (float) (cy + Mth.sin((float) ((x % 16 + phase) / 3)) * .5f * f3);
                float dz = (float) (cz + Mth.sin((float) ((y % 16 + phase) / 2)) * .5f * f3);
                float u = (1 - f2) * dist - time * speed;
                float px = dx * f2, py = dy * f2, pz = dz * f2;
                float[] cur = strip == 0 ? new float[]{px, py - width, pz, px, py + width, pz, u, f3} : new float[]{px - width, py, pz, px + width, py, pz, u, f3};
                if (prev != null) {
                    out.addVertex(m, prev[0], prev[1], prev[2]).setUv(prev[6], 1).setColor(r, g, b, prev[7]);
                    out.addVertex(m, prev[3], prev[4], prev[5]).setUv(prev[6], 0).setColor(r, g, b, prev[7]);
                    out.addVertex(m, cur[3], cur[4], cur[5]).setUv(cur[6], 0).setColor(r, g, b, cur[7]);
                    out.addVertex(m, cur[0], cur[1], cur[2]).setUv(cur[6], 1).setColor(r, g, b, cur[7]);
                }
                prev = cur;
            }
        }
        pose.popPose();
    }

    @Override public net.minecraft.world.phys.AABB getRenderBoundingBox(WandPedestal.PedestalEntity ped) {
        return new net.minecraft.world.phys.AABB(ped.getBlockPos()).inflate(9);
    }
}
