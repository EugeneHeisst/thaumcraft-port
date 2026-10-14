package dev.eugheist.tc4modern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Common Baubles packets must load independently of the client inventory screen. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
final class BaublesNetwork {
    /** Original PacketOpenBaublesInventory; wire identifier and codec remain unchanged. */
    public record Open() implements CustomPacketPayload {
        static final Open INSTANCE=new Open();
        public static final Type<Open> TYPE=new Type<>(AlchemyContent.id("open_baubles"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Open> CODEC=StreamCodec.unit(INSTANCE);
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    @SubscribeEvent static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("tc4-baubles-1").playToClient(BaublesSyncPayload.TYPE,BaublesSyncPayload.CODEC,
            (packet,context)->context.enqueueWork(()->BaublesSyncPayload.receive(packet)))
            .playToServer(Open.TYPE,Open.CODEC,(packet,context)->context.enqueueWork(()->{
                if(context.player() instanceof ServerPlayer player)player.openMenu(new SimpleMenuProvider((id,inventory,who)->new BaublesMenu(id,inventory),Component.translatable("container.crafting")));
            }));
    }
}
