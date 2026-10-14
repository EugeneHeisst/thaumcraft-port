package dev.eugheist.tc4modern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record ScanPayload(String status, CompoundTag knowledge) implements CustomPacketPayload {
    public static final Type<ScanPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "scan"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ScanPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, ScanPayload::status, ByteBufCodecs.COMPOUND_TAG, ScanPayload::knowledge, ScanPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
