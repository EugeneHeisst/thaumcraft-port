package dev.eugheist.tc4modern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Original PacketWarpMessage branches, including silent temporary decay. */
public record WarpMessagePayload(int amount, int kind) implements CustomPacketPayload {
    public static final Type<WarpMessagePayload> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "warp_message"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarpMessagePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, WarpMessagePayload::amount, ByteBufCodecs.INT, WarpMessagePayload::kind, WarpMessagePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public String notificationKey() {
        if (amount == 0 || kind < 0 || kind > 2) return null;
        if (kind == 0) return amount > 0 ? "tc.addwarp" : null;
        if (kind == 1) return amount > 0 ? "tc.addwarpsticky" : "tc.removewarpsticky";
        return amount > 0 ? "tc.addwarptemp" : null;
    }
    public boolean whispers() { return amount > 0 && (kind == 0 || kind == 1); }
    public static void receive(WarpMessagePayload payload) {
        ClientReceiver.receive(payload);
    }
    private static final class ClientReceiver {
        static void receive(WarpMessagePayload payload) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            String key = payload.notificationKey();
            if (player == null || key == null) return;
            ClientKnowledge.notifications.add(net.minecraft.network.chat.Component.translatable(key).getString(), System.nanoTime() / 1_000_000L);
            if (payload.whispers()) player.playSound(WarpNetwork.WHISPERS.get(), .5f, 1f);
        }
    }
}
