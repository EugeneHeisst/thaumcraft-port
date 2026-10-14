package dev.eugheist.tc4modern;

import java.util.ArrayDeque;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import thaumcraft.api.research.ResearchItem;

/** Original upper-left research popup, three-second quartic slide and legacy achievement frame. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class ResearchPopup {
    private static final ResourceLocation FRAME = ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/gui/original_achievement_background.png");
    private static final ArrayDeque<ResearchItem> queue = new ArrayDeque<>();
    private static long started;
    public static final java.util.Set<String> highlighted = new java.util.HashSet<>();
    private ResearchPopup() {}
    public static void enqueue(ResearchItem research) {
        if (queue.isEmpty()) started = System.nanoTime();
        queue.add(research);
        highlighted.add(research.key);
        highlighted.add(research.category);
    }
    public static int slide(double progress) {
        double triangle = progress * 2;
        if (triangle > 1) triangle = 2 - triangle;
        double edge = Math.max(0, 1 - triangle * 4);
        return -(int) (edge * edge * edge * edge * 36);
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || queue.isEmpty()) return;
        long now = System.nanoTime();
        double progress = (now - started) / 3_000_000_000.0;
        if (progress < 0 || progress > 1) {
            queue.remove(); started = queue.isEmpty() ? 0 : now; return;
        }
        var research = queue.peek();
        var gui = event.getGuiGraphics();
        int y = slide(progress);
        gui.blit(FRAME, 0, y, 96, 202, 160, 32, 256, 256);
        gui.drawString(mc.font, "Research Completed!", 30, y + 7, 0xFFFFFF00, false);
        String name = research.getName();
        int width = mc.font.width(name);
        if (width <= 125) gui.drawString(mc.font, name, 30, y + 18, 0xFFFFFFFF, false);
        else {
            float scale = 125f / width;
            gui.pose().pushPose();
            gui.pose().translate(30, y + 16 + 2f / scale, 0);
            gui.pose().scale(scale, scale, 1);
            gui.drawString(mc.font, name, 0, 0, 0xFFFFFFFF, false);
            gui.pose().popPose();
        }
        ThaumonomiconScreen.drawResearchIcon(gui, research, 8, y + 8);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) { queue.clear(); highlighted.clear(); started = 0; }
}
