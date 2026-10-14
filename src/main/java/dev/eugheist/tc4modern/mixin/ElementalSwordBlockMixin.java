package dev.eugheist.tc4modern.mixin;
import dev.eugheist.tc4modern.ElementalSword;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Sword blocking used legacy half-damage, not the modern shield's complete directional immunity. */
@Mixin(LivingEntity.class)
public abstract class ElementalSwordBlockMixin {
 @Inject(method="isDamageSourceBlocked",at=@At("HEAD"),cancellable=true)
 private void tc4modern$sword(DamageSource source,CallbackInfoReturnable<Boolean> ci){if(ElementalSword.blocking((LivingEntity)(Object)this))ci.setReturnValue(false);}
}
