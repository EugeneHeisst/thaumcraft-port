package dev.eugheist.tc4modern.mixin;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
/** Modern equivalent of original Utils.resetFloatCounter while the harness pays its fuel. */
@Mixin(ServerGamePacketListenerImpl.class)
public interface HoverFloatCounterAccessor {
 @Accessor("aboveGroundTickCount") void tc4modern$floatCounter(int value);
}
