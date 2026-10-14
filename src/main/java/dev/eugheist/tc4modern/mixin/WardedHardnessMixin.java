package dev.eugheist.tc4modern.mixin;

import dev.eugheist.tc4modern.WardedArcana;
import dev.eugheist.tc4modern.WarpConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Original getBlockHardness of warded blocks reads Config.wardedStone: -1 while warded, ordinary hardness when cosmetic. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class WardedHardnessMixin {
    @Shadow public abstract Block getBlock();
    @Inject(method="getDestroySpeed",at=@At("HEAD"),cancellable=true)
    private void tc4modern$cosmeticWarding(BlockGetter level,BlockPos pos,CallbackInfoReturnable<Float> result){if(getBlock() instanceof WardedArcana.Hardened hardened&&!WarpConfig.wardedStone())result.setReturnValue(hardened.cosmeticHardness());}
}
