package dev.eugheist.tc4modern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Persistent player bridge; original warp keys share the player's NeoForge data. */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class PlayerWarp {
    private PlayerWarp() {}
    public static WarpState load(Player player) { return WarpState.load(player.getPersistentData()); }
    public static void save(Player player, WarpState state) { save(player.getPersistentData(), state); }
    public static void save(CompoundTag playerData, WarpState state) {
        CompoundTag values = state.save();
        for (String key : values.getAllKeys()) playerData.put(key, values.get(key).copy());
    }
    public static boolean add(Player player, WarpState.Kind kind, int amount) {
        if (!(player instanceof net.minecraft.server.level.ServerPlayer server)) return false;
        var state = load(player);
        if (!state.add(kind, amount)) return false;
        save(player, state);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server, new WarpSyncPayload(state.get(kind), kind.ordinal()));
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server, new WarpMessagePayload(amount, kind.ordinal()));
        return true;
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) WarpNetwork.sync(player);
    }
    @SubscribeEvent public static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) WarpNetwork.sync(player);
    }
    @SubscribeEvent public static void clonePlayer(PlayerEvent.Clone event) {
        if (!event.getEntity().level().isClientSide)
            save(event.getEntity(), load(event.getOriginal()));
    }
}
