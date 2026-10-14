package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * PacketAspectPool with amount > 0 (scans, knowledge fragments, deconstruction, mana beans, warp): the "Gained N research
 * point(s) for X" notification with the aspect icon, one flying aspect per point (REHNotifyHandler.renderAspectHUD) and a
 * throttled random.orb. The pool itself still arrives with the regular knowledge sync.
 */
public final class AspectGain {
    private AspectGain() {}
    public record Payload(CompoundTag aspects) implements CustomPacketPayload {
        public static final Type<Payload> TYPE = new Type<>(AlchemyContent.id("aspect_gain"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Payload> CODEC = StreamCodec.composite(ByteBufCodecs.COMPOUND_TAG, Payload::aspects, Payload::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    /** Server side, after the knowledge has been saved. */
    public static void send(ServerPlayer player, AspectList gained) {
        if (gained == null || gained.size() == 0) return;
        var tag = new CompoundTag();
        for (Aspect a : gained.getAspects()) if (gained.getAmount(a) > 0) tag.putInt(a.getTag(), gained.getAmount(a));
        if (!tag.isEmpty()) PacketDistributor.sendToPlayer(player, new Payload(tag));
    }
    public static void send(ServerPlayer player, Aspect aspect, int amount) { send(player, new AspectList().add(aspect, amount)); }

    // ---- client ----
    record Flying(Aspect aspect, float startX, float startY, long created, long expire) {}
    static List<Flying> flying = new ArrayList<>();
    static long lastSound;
    static void receive(Payload payload) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        for (String key : payload.aspects().getAllKeys()) {
            Aspect aspect = Aspect.getAspect(key);
            int amount = payload.aspects().getInt(key);
            if (aspect == null || amount <= 0) continue;
            String text = net.minecraft.network.chat.Component.translatable("tc.addaspectpool").getString().replace("%s", String.valueOf(amount)).replace("%n", aspect.getName());
            ClientKnowledge.notifications.add(text, System.nanoTime() / 1_000_000L, aspect.getImage(), aspect.getColor());
            for (int a = 0; a < amount; a++) {
                long time = System.nanoTime() / 1_000_000L + mc.level.random.nextInt(1000);
                flying.add(new Flying(aspect, .4f + mc.level.random.nextFloat() * .2f, .4f + mc.level.random.nextFloat() * .2f, time, time + 1500));
            }
            if (System.currentTimeMillis() > lastSound) {
                mc.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, .1f, .9f + mc.level.random.nextFloat() * .2f);
                lastSound = System.currentTimeMillis() + 100;
            }
        }
    }
    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
    static final class Hud {
        @SubscribeEvent static void logout(net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut event) { flying.clear(); }
        /** renderAspectHUD: quadratic bezier from the screen centre area to the top-right corner, alpha ramps .3/.66, Thaumonomicon icon fades with them. */
        @SubscribeEvent static void render(RenderGuiEvent.Post event) {
            if (flying.isEmpty()) return;
            var gui = event.getGuiGraphics();
            long time = System.nanoTime() / 1_000_000L;
            flying.removeIf(f -> f.expire() < time);
            int k = gui.guiWidth(), l = gui.guiHeight();
            float mainAlpha = 0;
            gui.flush();
            com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
            for (var li : flying) {
                if (li.created() > time) continue;
                int startX = (int) (k * li.startX()), startY = (int) (l * li.startY()), endX = k, endY = -8;
                int bezierX = (int) (k * (.25f + li.startX())), bezierY = (int) (l * li.startY());
                double t = (double) (time - li.created()) / (li.expire() - li.created());
                double x = (1 - t) * (1 - t) * startX + 2 * (1 - t) * t * bezierX + t * t * endX;
                double y = (1 - t) * (1 - t) * startY + 2 * (1 - t) * t * bezierY + t * t * endY;
                float alpha = 1;
                if (t < .3f) alpha = (float) (t / .3f); else if (t > .66f) alpha = (float) (1 - (t - .66f) / .34f);
                mainAlpha = Math.max(mainAlpha, alpha);
                int c = li.aspect().getColor();
                gui.pose().pushPose();
                gui.pose().translate(x, y, 0);
                gui.pose().scale(.075f * alpha, .075f * alpha, 1);
                gui.setColor((c >> 16 & 255) / 255f, (c >> 8 & 255) / 255f, (c & 255) / 255f, alpha * .66f);
                gui.blit(li.aspect().getImage(), 0, 0, 0, 0, 256, 256, 256, 256);
                gui.pose().popPose();
            }
            if (mainAlpha > 0) {
                gui.setColor(1, 1, 1, mainAlpha);
                gui.pose().pushPose();
                gui.pose().translate(k - 16, 0, 0);
                gui.pose().scale(.0625f, .0625f, 1);
                gui.blit(AlchemyContent.id("textures/items/thaumonomicon.png"), 0, 0, 0, 0, 256, 256, 256, 256);
                gui.pose().popPose();
            }
            gui.setColor(1, 1, 1, 1);
            com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
        }
    }
    @EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
    static final class Network {
        @SubscribeEvent static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
            event.registrar("tc4-aspect-gain-1").playToClient(Payload.TYPE, Payload.CODEC, (p, c) -> c.enqueueWork(() -> receive(p)));
        }
    }
}
