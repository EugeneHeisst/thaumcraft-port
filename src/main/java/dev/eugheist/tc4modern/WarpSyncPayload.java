package dev.eugheist.tc4modern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Original PacketSyncWarp: one bucket's value and type (0/1/2). */
public record WarpSyncPayload(int value, int kind) implements CustomPacketPayload {
    public static final Type<WarpSyncPayload> TYPE = new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "warp_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarpSyncPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, WarpSyncPayload::value, ByteBufCodecs.INT, WarpSyncPayload::kind, WarpSyncPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void receive(WarpSyncPayload payload) {
        ClientReceiver.receive(payload);
    }
    private static final class ClientReceiver {
        static void receive(WarpSyncPayload payload) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null || payload.kind < 0 || payload.kind > 2) return;
            var state = PlayerWarp.load(player);
            state.set(WarpState.Kind.values()[payload.kind], payload.value);
            PlayerWarp.save(player, state);
        }
    }
}
