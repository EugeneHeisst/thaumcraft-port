package dev.eugheist.tc4modern.mixin;
import dev.eugheist.tc4modern.ElementalTools;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** NeoForge no longer supplies 1.7.10's Item.onBlockStartBreak hook. */
@Mixin(ServerPlayerGameMode.class)
public abstract class ElementalAxeBreakMixin {
 @Shadow protected ServerPlayer player;
 @Inject(method="destroyBlock",at=@At("HEAD"),cancellable=true)
 private void tc4modern$axe(BlockPos pos,CallbackInfoReturnable<Boolean> ci){var stack=player.getMainHandItem();if(stack.getItem() instanceof dev.eugheist.tc4modern.ElementalShovel shovel)shovel.capture(player,pos);if(stack.getItem() instanceof ElementalTools.Axe axe&&axe.onBlockStartBreak(stack,pos,player))ci.setReturnValue(false);}
}
