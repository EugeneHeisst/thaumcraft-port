package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;

/** Client lifetime of the original fog envelope; rendering consumes its density separately. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class WarpClientFog {
    private static WarpFogState state = new WarpFogState();
    private WarpClientFog() {}
    public static WarpFogState state() { return state; }
    @SubscribeEvent public static void tick(ClientTickEvent.Pre event) {
        var mc = Minecraft.getInstance();
        if (mc.player != null && !mc.isPaused()) state.tick();
    }
    @SubscribeEvent public static void renderFog(net.neoforged.neoforge.client.event.ViewportEvent.RenderFog event) {
        if (!state.active() || state.density() <= 0) return;
        event.setNearPlaneDistance(-state.density());
        event.setFarPlaneDistance(-420f);
        // Minecraft 1.7.10 selects GL_NV_fog_distance EYE_RADIAL_NV when available.
        event.setFogShape(com.mojang.blaze3d.shaders.FogShape.SPHERE);
        event.setCanceled(true);
    }
    @SubscribeEvent public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        state = new WarpFogState();
    }
}
