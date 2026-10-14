package dev.eugheist.tc4modern;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import thaumcraft.api.aspects.Aspect;

/**
 * ClientTickEventsFML.renderCastingWandHud: the top-left dial with one vial per primal (fill = vis/capacity),
 * focus cost markers, up/down arrows against the value one second ago, sneak-only numbers and the focus icon
 * with its cooldown in the centre.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class WandHud {
    private WandHud() {}
    static final ResourceLocation HUD = AlchemyContent.id("textures/gui/hud.png");
    private static final Map<Integer, int[]> OLD = new HashMap<>();
    private static long nextSync;
    static int lastDialY=-1;
    private static void quad(GuiGraphics g, int x, int y, int u, int v, int w, int h) { g.blit(HUD, x, y, u, v, w, h, 256, 256); }
    @SubscribeEvent static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.options.hideGui || mc.screen != null) return;
        var wand = player.getMainHandItem();
        if (!WandData.isWand(wand)) return;
        var primals = Aspect.getPrimalAspects();
        int[] now = new int[primals.size()];
        for (int i = 0; i < now.length; i++) now[i] = WandData.get(wand, primals.get(i));
        int slot = player.getInventory().selected;
        long time = System.currentTimeMillis();
        if (!OLD.containsKey(slot) || nextSync <= time) { OLD.put(slot, now.clone()); nextSync = time + 1000L; }
        int[] old = OLD.get(slot);
        GuiGraphics g = event.getGuiGraphics();
        var pose = g.pose();
        pose.pushPose();
        lastDialY=WarpClientConfig.dialBottom()?g.guiHeight()-32:0;
        pose.translate(0,lastDialY,0);
        RenderSystem.enableBlend();
        pose.pushPose();
        pose.scale(.5f, .5f, .5f);
        quad(g, 0, 0, 0, 0, 64, 64);
        pose.popPose();
        pose.translate(16, 16, 0);
        int max = WandData.capacity(wand);
        var focusItem = FociContent.getFocusItem(wand);
        var focus = focusItem.getItem() instanceof FociContent.Focus f ? f : null;
        for (int count = 0; count < primals.size(); count++) {
            Aspect aspect = primals.get(count);
            int amt = now[count];
            pose.pushPose();
            if(!WarpClientConfig.dialBottom())pose.mulPose(Axis.ZP.rotationDegrees(90));
            pose.mulPose(Axis.ZP.rotationDegrees(-15 + count * 24));
            pose.translate(0, -32, 0);
            pose.scale(.5f, .5f, .5f);
            int loc = max <= 0 ? 0 : (int) (30f * amt / max);
            int c = aspect.getColor();
            g.flush();
            RenderSystem.enableBlend();
            g.setColor((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, .8f);
            quad(g, -4, 35 - loc, 104, 0, 8, loc);
            g.setColor(1, 1, 1, 1);
            quad(g, -8, -3, 72, 0, 16, 42);
            int sh = 0;
            boolean costs = focus != null && focus.getVisCost(focusItem).getAmount(aspect) > 0;
            if (costs) { quad(g, -4, -8, 136, 0, 8, 8); sh = 8; }
            if (old[count] > amt) quad(g, -4, -8 - sh, 128, 0, 8, 8);
            else if (old[count] < amt) quad(g, -4, -8 - sh, 120, 0, 8, 8);
            if (player.isShiftKeyDown()) {
                pose.pushPose();
                pose.mulPose(Axis.ZP.rotationDegrees(-90));
                g.drawString(mc.font, amt / 100 + "", -32, -4, 0xFFFFFF);
                g.flush();
                pose.popPose();
                if (costs) {
                    float mod = WandData.craftingModifier(wand, player, aspect);
                    pose.pushPose();
                    pose.mulPose(Axis.ZP.rotationDegrees(-90));
                    g.drawString(mc.font, new java.text.DecimalFormat("#####.##").format(focus.getVisCost(focusItem).getAmount(aspect) * mod / 100f), 8, -4, 0xFFFFFF);
                    pose.popPose();
                }
            }
            pose.popPose();
        }
        if (focus != null) {
            pose.pushPose();
            pose.translate(-24, -24, 90);
            g.renderItem(focusItem, 16, 16);
            pose.popPose();
            float cd = FociContent.cooldownSeconds(player);
            if (cd > 0) {
                pose.pushPose();
                pose.translate(0, 0, 150);
                pose.scale(.5f, .5f, .5f);
                String secs = new java.text.DecimalFormat("#######.#").format(cd) + "s";
                g.drawString(mc.font, secs, -mc.font.width(secs) / 2, -4, 0xFFFFFF);
                pose.popPose();
            }
        }
        RenderSystem.disableBlend();
        pose.popPose();
    }
}
