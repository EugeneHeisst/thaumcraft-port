package dev.eugheist.tc4modern.mixin;
import dev.eugheist.tc4modern.TravellerBoots;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** 1.21 replaced the mutable jumpMovementFactor field with this method. */
@Mixin(Player.class)
abstract class TravellerAirSpeedMixin {
 @Inject(method="getFlyingSpeed",at=@At("RETURN"),cancellable=true)
 private void tc4modern$travellerAirSpeed(CallbackInfoReturnable<Float> ci){var p=(Player)(Object)this;if(TravellerBoots.moving(p))ci.setReturnValue(TravellerBoots.airSpeed(p));}
}
