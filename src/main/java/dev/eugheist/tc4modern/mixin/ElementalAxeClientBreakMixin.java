package dev.eugheist.tc4modern.mixin;
import dev.eugheist.tc4modern.ElementalTools;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(MultiPlayerGameMode.class)
public abstract class ElementalAxeClientBreakMixin {
 @Inject(method="destroyBlock",at=@At("HEAD"),cancellable=true)
 private void tc4modern$axe(BlockPos pos,CallbackInfoReturnable<Boolean> ci){var mc=Minecraft.getInstance();if(mc.player!=null&&mc.player.getMainHandItem().getItem() instanceof dev.eugheist.tc4modern.ElementalShovel shovel)shovel.capture(mc.player,pos);if(mc.player!=null&&mc.level!=null&&mc.player.getMainHandItem().getItem() instanceof ElementalTools.Axe&&!mc.player.isShiftKeyDown()&&mc.level.getBlockState(pos).is(BlockTags.LOGS))ci.setReturnValue(false);}
}
