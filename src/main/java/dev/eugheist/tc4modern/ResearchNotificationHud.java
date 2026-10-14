package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Text placement and fade from original REHNotifyHandler.renderNotifyHUD. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class ResearchNotificationHud {
    private ResearchNotificationHud() {}
    static int displayed;
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientKnowledge.notifications.clear();
    }
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) { draw(event.getGuiGraphics()); }
    /** renderNotifyHUD; GuiResearchTable.drawGuiContainerForegroundLayer also draws it over the open table. */
    public static void draw(net.minecraft.client.gui.GuiGraphics gui) {
        displayed=0;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long time = System.nanoTime() / 1_000_000L;
        var entries = ClientKnowledge.notifications.update(time);
        int delay=ClientKnowledge.notifications.delay();
        float shift = -8;
        for (int i = 0; i < entries.size() && i < WarpClientConfig.notificationMax(); i++) {
            displayed++;
            var entry = entries.get(i);
            int alpha = 255;
            if (i == entries.size() - 1 && entry.created() > time)
                alpha = 255 - (int)((float)(entry.created() - time) / (delay / 4) * 240);
            if (entry.expire() < time + delay) {
                alpha = (int)(255 - (float)(time + delay - entry.expire()) / delay * 240);
                shift = -8 * ((float)alpha / 255);
            }
            int size = mc.font.width(entry.text()) / 2;
            gui.pose().pushPose();
            gui.pose().translate(gui.guiWidth() - size - 10, gui.guiHeight() - i * 8 + shift, 0);
            gui.pose().scale(0.5f, 0.5f, 0.5f);
            gui.drawString(mc.font, entry.text(), -4, -8, ((alpha / 2) << 24) | 0xffffff, true);
            gui.pose().popPose();
            if (entry.image() != null) {
                // The image 8px left of the right edge at 1/32 scale, tinted, alpha/511.
                int c = entry.color();
                gui.pose().pushPose();
                gui.pose().translate(gui.guiWidth() - 9, gui.guiHeight() - i * 8 + shift - 6, 0);
                gui.pose().scale(.03125f, .03125f, 1);
                com.mojang.blaze3d.systems.RenderSystem.enableBlend();
                gui.setColor((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, alpha / 511f);
                gui.blit(entry.image(), 0, 0, 0, 0, 256, 256, 256, 256);
                gui.setColor(1, 1, 1, 1);
                gui.pose().popPose();
            }
            if (i == entries.size() - 1 && entry.created() > time) {
                float scale = (float)(entry.created() - time) / (delay / 4);
                int arrivalAlpha = 255 - (int)(scale * 240);
                gui.pose().pushPose();
                gui.pose().translate(gui.guiWidth() - 5 - 8 * scale
                    - (1 - scale) * (1 - scale) * (1 - scale) * size * 3,
                    gui.guiHeight() - i * 8 + shift - 2 - 8 * scale, 0);
                gui.pose().scale(scale, scale, scale);
                gui.setColor(1, 1, 1, 0.5f - (float)arrivalAlpha / 511);
                try {
                    int frame = (mc.player.tickCount + i * 3) % 16;
                    gui.blit(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/misc/particles.png"),
                        0, 0, frame * 16, 80, 16, 16, 256, 256);
                } finally {
                    gui.setColor(1, 1, 1, 1);
                    gui.pose().popPose();
                }
            }
        }
    }
}
