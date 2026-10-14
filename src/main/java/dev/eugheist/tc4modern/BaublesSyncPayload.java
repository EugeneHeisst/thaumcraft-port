package dev.eugheist.tc4modern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;

/** Keep client-side worn-item effects and focus-pouch access authoritative outside the expanded menu. */
public record BaublesSyncPayload(ItemStack amulet, ItemStack firstRing, ItemStack secondRing, ItemStack belt)
    implements CustomPacketPayload {
    public static final Type<BaublesSyncPayload> TYPE = new Type<>(AlchemyContent.id("baubles_sync"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BaublesSyncPayload> CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC, BaublesSyncPayload::amulet,
        ItemStack.OPTIONAL_STREAM_CODEC, BaublesSyncPayload::firstRing,
        ItemStack.OPTIONAL_STREAM_CODEC, BaublesSyncPayload::secondRing,
        ItemStack.OPTIONAL_STREAM_CODEC, BaublesSyncPayload::belt, BaublesSyncPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    static void receive(BaublesSyncPayload payload) { Client.receive(payload); }
    private static final class Client {
        static void receive(BaublesSyncPayload payload) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player == null) return;
            var slots = BaublesInventory.of(player);
            slots.setItem(0, payload.amulet());
            slots.setItem(1, payload.firstRing());
            slots.setItem(2, payload.secondRing());
            slots.setItem(3, payload.belt());
        }
    }
}
