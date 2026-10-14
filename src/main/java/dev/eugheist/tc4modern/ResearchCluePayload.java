package dev.eugheist.tc4modern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Sent only after the server persists a newly discovered clue, never during knowledge sync. */
public record ResearchCluePayload(String key) implements CustomPacketPayload {
    public static final Type<ResearchCluePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "research_clue"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchCluePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ResearchCluePayload::key, ResearchCluePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
