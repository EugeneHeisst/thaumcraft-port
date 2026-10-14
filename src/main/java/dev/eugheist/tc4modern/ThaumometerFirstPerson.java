package dev.eugheist.tc4modern;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderHandEvent;

/**
 * First-person port of the original ItemThaumometerRenderer (EQUIPPED_FIRST_PERSON branch).
 * The legacy GL chain is replayed literally: vanilla 1.7.10 ItemRenderer item placement,
 * Forge's -0.5 render-helper offset, then the scanner renderer's own transforms, two copies of
 * the right first-person arm at scale 5, the unchanged scanner OBJ and the scanscreen readout.
 * Left main arm (modern-only option) renders the mirror image F·M of the whole chain; only the
 * screen readout is flipped back so its text stays readable.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class ThaumometerFirstPerson {
    private static final ResourceLocation SCANNER_OBJ = ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "models/scanner_original.obj");
    private static final ResourceLocation SCANNER = ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/models/scanner.png");
    private static final ResourceLocation SCREEN = ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/models/scanscreen.png");
    private static final ObjParts MODEL = new ObjParts(SCANNER_OBJ);
    private static final java.util.Random FLICKER = new java.util.Random();
    /** True while the device screen readout is visible; the modern HUD then stays out of the way. */
    public static boolean readoutVisible;

    private ThaumometerFirstPerson() {}

    @SubscribeEvent
    public static void render(RenderHandEvent event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || !player.getMainHandItem().is(TC4Modern.THAUMOMETER.get())) return;
        // The original grip occupies both hands, so nothing is drawn for the second hand.
        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Chain c = new Chain(event.getPoseStack(), player.getMainArm() == HumanoidArm.LEFT);
        float partial = event.getPartialTick();
        float swing = event.getSwingProgress();
        float lowered = event.getEquipProgress(); // 1 - legacy equippedProgress
        c.pose.pushPose();
        // ItemRenderer.renderItemInFirstPerson (1.7.10), ordinary item branch.
        float var13 = 0.8f;
        float f6 = Mth.sin(swing * Mth.PI), f7 = Mth.sin(Mth.sqrt(swing) * Mth.PI);
        c.translate(-f7 * 0.4f, Mth.sin(Mth.sqrt(swing) * Mth.PI * 2) * 0.2f, -f6 * 0.2f);
        c.translate(0.7f * var13, -0.65f * var13 - lowered * 0.6f, -0.9f * var13);
        c.rotY(45);
        f6 = Mth.sin(swing * swing * Mth.PI);
        c.rotY(-f6 * 20);
        c.rotZ(-f7 * 20);
        c.rotX(-f7 * 80);
        c.pose.scale(0.4f, 0.4f, 0.4f);
        // ForgeHooksClient.renderEquippedItem with shouldUseRenderHelper == true.
        c.translate(-0.5f, -0.5f, -0.5f);
        // ItemThaumometerRenderer.renderItem.
        float var7 = 0.8f;
        c.translate(1, 0.75f, -1);
        c.rotY(-135);
        float armPitch = Mth.lerp(partial, player.xBobO, player.xBob);
        float armYaw = Mth.lerp(partial, player.yBobO, player.yBob);
        c.rotX((player.getViewXRot(partial) - armPitch) * 0.1f);
        c.rotY((player.getViewYRot(partial) - armYaw) * 0.1f);
        c.translate(-0.7f * var7, 0.65f * var7 + lowered * 1.5f, 0.9f * var7);
        c.rotY(90);
        c.translate(0, 0, -0.9f * var7);
        c.rotY(90);
        var buffers = event.getMultiBufferSource();
        int light = event.getPackedLight();
        if (!player.isInvisible() && mc.getEntityRenderDispatcher().getRenderer(player) instanceof PlayerRenderer renderer) {
            c.pose.pushPose();
            c.pose.scale(5, 5, 5);
            for (int var9 = 0; var9 < 2; var9++) {
                int var22 = var9 * 2 - 1;
                c.pose.pushPose();
                c.translate(0, -0.6f, 1.1f * var22);
                c.rotX(-45 * var22);
                c.rotZ(-90);
                c.rotZ(59);
                c.rotY(-65 * var22);
                // RenderPlayer.renderFirstPersonArm draws bipedRightArm for both grips; under the
                // conjugated chain the modern left arm is exactly that mesh's mirror image.
                if (c.mirror) renderer.renderLeftHand(c.pose, buffers, light, player);
                else renderer.renderRightHand(c.pose, buffers, light, player);
                c.pose.popPose();
            }
            c.pose.popPose();
        }
        c.rotZ(90);
        c.translate(0.4f, -0.4f, 0);
        c.pose.scale(2, 2, 2);
        c.geometry();
        MODEL.render("scanner", SCANNER, c.pose, buffers, light, OverlayTexture.NO_OVERLAY, 0xFFFFFFFF);
        c.pose.pushPose();
        c.pose.translate(0, 0.11f, 0);
        c.pose.mulPose(Axis.XP.rotationDegrees(90));
        c.pose.mulPose(Axis.ZP.rotationDegrees(90));
        int brightness = flicker(player, 0);
        // UtilsFX.renderQuadCenteredFromTexture leaves its glScalef on the matrix for the readout.
        c.pose.scale(2.5f, 2.5f, 2.5f);
        quad(c.pose, buffers.getBuffer(RenderType.entityTranslucent(SCREEN)), brightness);
        // A mirrored screen would show mirrored, back-face-culled glyphs; flip them within the screen plane.
        if (c.mirror) c.pose.scale(-1, 1, 1);
        readout(mc, player, c.pose, buffers);
        c.pose.popPose();
        c.pose.popPose();
    }

    /** Original 190 + sin(ticks - rand(2)) * 10 + 10 lightmap flicker, already in packed-light layout. */
    private static int flicker(AbstractClientPlayer player, int offset) {
        return (int) (190 + Mth.sin(offset + player.tickCount - FLICKER.nextInt(2)) * 10 + 10);
    }

    private static void quad(PoseStack pose, com.mojang.blaze3d.vertex.VertexConsumer out, int light) {
        var last = pose.last();
        float[][] v = {{-0.5f, 0.5f, 0, 1}, {0.5f, 0.5f, 1, 1}, {0.5f, -0.5f, 1, 0}, {-0.5f, -0.5f, 0, 0}};
        for (float[] p : v)
            out.addVertex(last, p[0], p[1], 0).setColor(0xFFFFFFFF).setUv(p[2], p[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 0, 1);
    }

    private static void readout(Minecraft mc, AbstractClientPlayer player, PoseStack pose, MultiBufferSource buffers) {
        readoutVisible = false;
        if (!mc.options.getCameraType().isFirstPerson()) return;
        ScanTarget scan = ScanTarget.look(player);
        if (scan == null) return;
        readoutVisible = true;
        pose.translate(0, 0, -0.01f);
        boolean scanned = ResearchKnowledge.isScanned(ClientKnowledge.knowledge,scan.knowledgeKey());
        String text = name(mc, player, scan);
        java.util.Map<String, Integer> aspects = scanned ? scan.aspects() : null;
        int light = flicker(player, 0);
        Font font = mc.font;
        if (scanned && scan.identity().startsWith("node:")) {
            var hit = mc.hitResult;
            if (hit instanceof net.minecraft.world.phys.BlockHitResult block && player.level().getBlockEntity(block.getBlockPos()) instanceof AuraNodeEntity node) {
                String t = Component.translatable("nodetype." + node.nodeType + ".name").getString();
                if (node.reservoir.modifier != null) t += ", " + Component.translatable("nodemod." + node.reservoir.modifier + ".name").getString();
                pose.pushPose();
                pose.scale(0.004f, 0.004f, 0.004f);
                font.drawInBatch(t, -font.width(t) / 2, -40, 0xFF000000 | 15642134, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
                pose.popPose();
            }
        }
        if (aspects != null && !aspects.isEmpty()) {
            var sorted = aspects.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).toList();
            int posX = 0, posY = 0, aa = sorted.size();
            int baseX = Math.min(5, aa) * 8;
            for (var entry : sorted) {
                pose.pushPose();
                pose.scale(0.0075f, 0.0075f, 0.0075f);
                drawTag(font, pose, buffers, -baseX + posX * 16, -8 + posY * 16, entry.getKey(), entry.getValue(), flicker(player, posX));
                pose.popPose();
                if (++posX < 5 - posY) continue;
                posX = 0;
                baseX = Math.min(5 - posY, aa -= 5 - ++posY) * 8;
            }
        }
        if (text == null) text = "?";
        if (!text.isEmpty()) {
            pose.pushPose();
            pose.translate(0, -0.25f, 0);
            int sw = font.width(text);
            float scale = 0.005f;
            if (sw > 90) scale -= 2.5E-5f * (sw - 90);
            pose.scale(scale, scale, scale);
            font.drawInBatch(text, -sw / 2, 0, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
            pose.popPose();
        }
    }

    /** UtilsFX.drawTag(x, y, aspect, amount, 0, 0.01, blend 1, alpha 1, false). */
    private static void drawTag(Font font, PoseStack pose, MultiBufferSource buffers, int x, int y, String tag, int amount, int light) {
        var aspect = thaumcraft.api.aspects.Aspect.getAspect(tag);
        int color = aspect == null ? 0xFFFFFF : aspect.getColor();
        var out = buffers.getBuffer(NodeRenderTypes.additive(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/aspects/" + tag + ".png")));
        var m = pose.last().pose();
        int argb = 0xFF000000 | color;
        out.addVertex(m, x, y + 16, 0.01f).setUv(0, 1).setColor(argb);
        out.addVertex(m, x + 16, y + 16, 0.01f).setUv(1, 1).setColor(argb);
        out.addVertex(m, x + 16, y, 0.01f).setUv(1, 0).setColor(argb);
        out.addVertex(m, x, y, 0.01f).setUv(0, 0).setColor(argb);
        if (amount > 0) {
            pose.pushPose();
            pose.scale(0.5f, 0.5f, 0.5f);
            String am = Integer.toString(amount);
            int sw = font.width(am);
            font.drawInBatch(am, 32 - sw + x * 2, 32 - font.lineHeight + y * 2, 0xFFFFFFFF, false, pose.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
            pose.popPose();
        }
    }

    private static String name(Minecraft mc, AbstractClientPlayer player, ScanTarget scan) {
        String id = scan.identity();
        try {
            if (id.startsWith("entity:")) {
                var entity = player.level().getEntity(Integer.parseInt(id.substring(7)));
                return entity == null ? "?" : entity.getName().getString();
            }
            if (id.startsWith("item:")) {
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id.substring(5)));
                if (mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit && hit.getEntity() instanceof net.minecraft.world.entity.item.ItemEntity entity)
                    return entity.getItem().getHoverName().getString();
                return new ItemStack(item).getHoverName().getString();
            }
            if (id.startsWith("block:")) {
                BlockPos pos = ScanTarget.blockPos(id);
                var state = player.level().getBlockState(pos);
                ItemStack stack = state.getCloneItemStack(new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false), player.level(), pos, player);
                return stack.isEmpty() ? state.getBlock().getName().getString() : stack.getHoverName().getString();
            }
            if (id.startsWith("node:") && mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)
                return player.level().getBlockState(hit.getBlockPos()).getBlock().getName().getString();
        } catch (RuntimeException ignored) {
        }
        return "?";
    }

    /** Legacy GL call adapter; mirror conjugates every step by an X reflection. */
    private static final class Chain {
        final PoseStack pose;
        final boolean mirror;
        Chain(PoseStack pose, boolean mirror) { this.pose = pose; this.mirror = mirror; }
        void translate(float x, float y, float z) { pose.translate(mirror ? -x : x, y, z); }
        void rotX(float degrees) { pose.mulPose(Axis.XP.rotationDegrees(degrees)); }
        void rotY(float degrees) { pose.mulPose(Axis.YP.rotationDegrees(mirror ? -degrees : degrees)); }
        void rotZ(float degrees) { pose.mulPose(Axis.ZP.rotationDegrees(mirror ? -degrees : degrees)); }
        /** Trailing reflection: conjugated steps give F·M·F, local meshes then need F·M. */
        void geometry() { if (mirror) pose.scale(-1, 1, 1); }
    }
}
