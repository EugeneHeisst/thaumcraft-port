package dev.eugheist.tc4modern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Original research completion notification, distinct from discovery clues. */
public record ResearchCompletedPayload(String key) implements CustomPacketPayload {
    public static final Type<ResearchCompletedPayload> TYPE = new Type<>(AlchemyContent.id("research_completed"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchCompletedPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ResearchCompletedPayload::key, ResearchCompletedPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void receive(ResearchCompletedPayload payload) {
        ClientReceiver.receive(payload);
    }
    private static final class ClientReceiver {
        static void receive(ResearchCompletedPayload payload) {
            if (net.minecraft.client.Minecraft.getInstance().player == null) return;
            var research = thaumcraft.api.research.ResearchCategories.getResearch(payload.key());
            if (research != null && !research.isVirtual()) ResearchPopup.enqueue(research);
        }
    }
}
