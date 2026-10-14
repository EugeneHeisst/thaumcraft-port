package dev.eugheist.tc4modern.mixin;
import dev.eugheist.tc4modern.FortressArmor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class FortressArmorMixin {
 @Inject(method="getDamageAfterArmorAbsorb",at=@At("HEAD"),cancellable=true)
 private void tc4modern$fortress(DamageSource source,float damage,CallbackInfoReturnable<Float> callback){var entity=(LivingEntity)(Object)this;if(FortressArmor.equipped(entity))callback.setReturnValue(FortressArmor.absorb(entity,source,damage));}
}
