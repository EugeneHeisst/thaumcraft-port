package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class ThaumometerHud {
    @SubscribeEvent public static void render(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.screen != null || mc.options.hideGui) return;
        if (!mc.player.getMainHandItem().is(TC4Modern.THAUMOMETER.get()) && !mc.player.getOffhandItem().is(TC4Modern.THAUMOMETER.get())) return;
        var target = ScanTarget.look(mc.player);
        var gui = event.getGuiGraphics();
        int cx = mc.getWindow().getGuiScaledWidth() / 2;
        int cy = mc.getWindow().getGuiScaledHeight() / 2;
        // In first person the original scanner screen carries the readout; the overlay is only a fallback.
        boolean device = ThaumometerFirstPerson.readoutVisible && mc.options.getCameraType().isFirstPerson() && mc.player.getMainHandItem().is(TC4Modern.THAUMOMETER.get());
        if (target != null && !device) {
            var aspects = target.aspects();
            boolean studied = ResearchKnowledge.isScanned(ClientKnowledge.knowledge,target.knowledgeKey());
            if (studied && !aspects.isEmpty()) {
                var sorted = aspects.entrySet().stream().sorted(java.util.Map.Entry.comparingByKey()).toList();
                int x = cx - sorted.size() * 28 / 2;
                for (var entry : sorted) {
                    var aspect = thaumcraft.api.aspects.Aspect.getAspect(entry.getKey());
                    int color = aspect == null ? 0xFFFFFF : aspect.getColor();
                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(((color >> 16) & 255) / 255f, ((color >> 8) & 255) / 255f, (color & 255) / 255f, 1);
                    gui.blit(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/aspects/" + entry.getKey() + ".png"), x, cy + 38, 0, 0, 16, 16, 16, 16);
                    com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1, 1, 1, 1);
                    gui.drawString(mc.font, Integer.toString(entry.getValue()), x + 17, cy + 44, 0xFFFFFF);
                    x += 28;
                }
            } else {
                gui.drawCenteredString(mc.font, Component.translatable("hud.tc4modern.hold"), cx, cy + 40, 0xC9B887);
            }
        }
        if (!device && mc.player.isUsingItem() && mc.player.getUseItem().is(TC4Modern.THAUMOMETER.get())) {
            int progress = Math.min(80, mc.player.getTicksUsingItem() * 80 / ThaumometerItem.SCAN_TICKS);
            gui.fill(cx - 41, cy + 62, cx + 41, cy + 68, 0xCC241C35);
            gui.fill(cx - 40, cy + 63, cx - 40 + progress, cy + 67, 0xFFB597E8);
        }
        if (!ClientKnowledge.status.isEmpty() && System.nanoTime() < ClientKnowledge.messageUntil)
            gui.drawCenteredString(mc.font, ClientKnowledge.status.startsWith("missing:")
                ? Component.translatable("hud.tc4modern.missing", ClientKnowledge.status.substring(8))
                : ClientKnowledge.status.startsWith("combined:")
                    ? Component.translatable("hud.tc4modern.combined", ClientKnowledge.status.substring(9))
                    : Component.translatable("hud.tc4modern." + ClientKnowledge.status), cx, cy + 80, 0xEADCB4);
    }
}
