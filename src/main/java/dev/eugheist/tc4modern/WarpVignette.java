package dev.eugheist.tc4modern;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Original frame-smoothed, pulsing death gaze vignette, independent of shader preference. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class WarpVignette {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "textures/misc/warp_vignette.png");
    private static int remaining;
    private static float target, brightness;
    private WarpVignette() {}
    public static void eventPulse() { remaining = 100; }
    @SubscribeEvent
    public static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null) { remaining = 0; target = brightness = 0; return; }
        if (mc.isPaused()) return;
        if (mc.player.hasEffect(WarpEffects.DEATH_GAZE)) remaining = 10;
        target = remaining > 0 ? 1 : 0;
        if (remaining > 0) remaining--;
    }
    @SubscribeEvent
    public static void render(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.CAMERA_OVERLAYS)) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null) return;
        brightness = (float) (brightness + (target - brightness) * 0.01);
        if (brightness <= 0) return;
        float pulse = brightness * (1 + Mth.sin(mc.player.tickCount / 2f) * .1f);
        var graphics = event.getGuiGraphics();
        graphics.flush();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
            GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(pulse, pulse, pulse, 1);
        graphics.blit(TEXTURE, 0, 0, 0, 0, graphics.guiWidth(), graphics.guiHeight(), graphics.guiWidth(), graphics.guiHeight());
        graphics.flush();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
