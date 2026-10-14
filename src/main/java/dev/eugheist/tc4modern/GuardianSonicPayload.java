package dev.eugheist.tc4modern;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
public record GuardianSonicPayload(int entity) implements CustomPacketPayload {
 public static final Type<GuardianSonicPayload> TYPE=new Type<>(AlchemyContent.id("guardian_sonic"));
 public static final StreamCodec<RegistryFriendlyByteBuf,GuardianSonicPayload> CODEC=StreamCodec.composite(ByteBufCodecs.INT,GuardianSonicPayload::entity,GuardianSonicPayload::new);
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
