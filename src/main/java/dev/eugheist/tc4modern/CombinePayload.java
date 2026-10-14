package dev.eugheist.tc4modern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public record CombinePayload(int menuId, String first, String second) implements CustomPacketPayload {
    public static final Type<CombinePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "combine"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CombinePayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, CombinePayload::menuId, ByteBufCodecs.STRING_UTF8, CombinePayload::first,
        ByteBufCodecs.STRING_UTF8, CombinePayload::second, CombinePayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public static void handle(CombinePayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) return;
        if (!(player.containerMenu instanceof ResearchTableMenu menu) || menu.containerId != payload.menuId() || !menu.stillValid(player)) return;
        String result = ResearchKnowledge.combine(player.getPersistentData(), payload.first(), payload.second(), menu.bonus);
        if (result.startsWith("combined:") || result.equals("no_combination")) menu.table.setChanged();
        menu.broadcastChanges();
        ThaumometerItem.sync(player, result);
    }
}
