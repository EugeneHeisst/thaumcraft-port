package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

/** Original ordered effect shaders, applied before the HUD. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class WarpPostEffects {
    private static final Logger LOG = LogUtils.getLogger();
    private static final String[] NAMES = {"desaturatetc", "blurtc", "hunger", "sunscorned"};
    private static final PostChain[] CHAINS = new PostChain[4];
    private static final boolean[] FAILED = new boolean[4];
    private static int width, height;
    private WarpPostEffects() {}
    @SubscribeEvent
    public static void render(RenderGuiEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !WarpClientConfig.shaders()) { reset(); return; }
        boolean[] wanted = {mc.player.hasEffect(WarpEffects.DEATH_GAZE), mc.player.hasEffect(WarpEffects.BLURRED_VISION),
            mc.player.hasEffect(WarpEffects.UNNATURAL_HUNGER), mc.player.hasEffect(WarpEffects.SUN_SCORNED)};
        int nextWidth = mc.getWindow().getWidth(), nextHeight = mc.getWindow().getHeight();
        for (int i = 0; i < CHAINS.length; i++) {
            if (!wanted[i]) { close(i); FAILED[i] = false; continue; }
            if (CHAINS[i] == null && !FAILED[i]) {
                try {
                    CHAINS[i] = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(),
                        ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "shaders/post/" + NAMES[i] + ".json"));
                    CHAINS[i].resize(nextWidth, nextHeight);
                } catch (Exception exception) {
                    close(i); FAILED[i] = true;
                    LOG.error("Failed to load original warp shader {}", NAMES[i], exception);
                }
            }
            if (CHAINS[i] != null) {
                if (width != nextWidth || height != nextHeight) CHAINS[i].resize(nextWidth, nextHeight);
                CHAINS[i].process(event.getPartialTick().getGameTimeDeltaPartialTick(true));
            }
        }
        width = nextWidth; height = nextHeight;
        mc.getMainRenderTarget().bindWrite(true);
    }
    @SubscribeEvent
    public static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) {
        reset();
    }
    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Reload {
        @SubscribeEvent
        public static void register(net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((net.minecraft.server.packs.resources.ResourceManagerReloadListener) manager -> reset());
        }
    }
    private static void close(int index) {
        if (CHAINS[index] != null) { CHAINS[index].close(); CHAINS[index] = null; }
    }
    public static void reset() {
        for (int i = 0; i < CHAINS.length; i++) { close(i); FAILED[i] = false; }
        width = height = 0;
    }
}
