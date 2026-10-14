package dev.eugheist.tc4modern;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.server.level.ServerPlayer;

@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class WarpNetwork {
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> WHISPERS =
        net.neoforged.neoforge.registries.DeferredHolder.create(net.minecraft.core.registries.Registries.SOUND_EVENT, AlchemyContent.id("whispers"));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> HEARTBEAT =
        net.neoforged.neoforge.registries.DeferredHolder.create(net.minecraft.core.registries.Registries.SOUND_EVENT, AlchemyContent.id("heartbeat"));
    @SubscribeEvent public static void sounds(net.neoforged.neoforge.registries.RegisterEvent event) {
        event.register(net.minecraft.core.registries.Registries.SOUND_EVENT, helper -> {
            helper.register(AlchemyContent.id("whispers"), net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("whispers")));
            helper.register(AlchemyContent.id("heartbeat"), net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("heartbeat")));
        });
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(GuardianSonicPayload.TYPE,GuardianSonicPayload.CODEC,(payload,context)->GuardianClient.sonic(payload.entity())).playToClient(ResearchCompletedPayload.TYPE, ResearchCompletedPayload.CODEC,
            (payload, context) -> ResearchCompletedPayload.receive(payload))
            .playToClient(WarpMiscPayload.TYPE, WarpMiscPayload.CODEC,
            (payload, context) -> WarpMiscPayload.receive(payload))
            .playToClient(WarpSyncPayload.TYPE, WarpSyncPayload.CODEC,
            (payload, context) -> WarpSyncPayload.receive(payload))
            .playToClient(WarpMessagePayload.TYPE, WarpMessagePayload.CODEC, (payload, context) -> WarpMessagePayload.receive(payload));
    }
    public static void sync(ServerPlayer player) {
        var state = PlayerWarp.load(player);
        for (var kind : WarpState.Kind.values())
            PacketDistributor.sendToPlayer(player, new WarpSyncPayload(state.get(kind), kind.ordinal()));
    }
}
