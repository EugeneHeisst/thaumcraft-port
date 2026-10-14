package dev.eugheist.tc4modern.mixin;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Legacy bone-bow punch ignores knockback resistance, as vanilla 1.7.10 arrows did. */
@Mixin(AbstractArrow.class)
public abstract class BoneArrowKnockbackMixin {
 @Inject(method="doKnockback",at=@At("HEAD"),cancellable=true)
 private void tc4modern$bonePunch(LivingEntity target,DamageSource damage,CallbackInfo ci){var arrow=(AbstractArrow)(Object)this;var data=arrow.getPersistentData();if(!data.contains("TC4BonePunch"))return;int punch=data.getInt("TC4BonePunch");var movement=arrow.getDeltaMovement();double speed=movement.horizontalDistance();if(punch>0&&speed>0)target.push(movement.x*punch*.6/speed,.1,movement.z*punch*.6/speed);ci.cancel();}
}
