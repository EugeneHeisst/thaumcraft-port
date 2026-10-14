package dev.eugheist.tc4modern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Original PacketMiscEvent codes: heartbeat, full mist, guardian mist pulse. */
public record WarpMiscPayload(int event) implements CustomPacketPayload {
    public static final Type<WarpMiscPayload> TYPE = new Type<>(AlchemyContent.id("warp_misc"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WarpMiscPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.INT, WarpMiscPayload::event, WarpMiscPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void receive(WarpMiscPayload payload) {
        ClientReceiver.receive(payload);
    }
    private static final class ClientReceiver {
        static void receive(WarpMiscPayload payload) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return;
            switch (payload.event()) {
                case 0 -> {
                    WarpVignette.eventPulse();
                    player.playSound(WarpNetwork.HEARTBEAT.get(), 1f, 1f);
                }
                case 1 -> WarpClientFog.state().mist();
                case 2 -> WarpClientFog.state().guardianPulse();
            }
        }
    }
}
