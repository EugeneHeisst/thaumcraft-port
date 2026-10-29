package dev.eugheist.tc4modern.mixin;

import dev.eugheist.tc4modern.EldritchCrab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Hostile head crabs are an exception to vanilla's protection for the shooter's passengers. */
@Mixin(Projectile.class)
public abstract class CrabProjectileHitMixin {
    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void tc4modern$hitAttachedCrab(Entity target, CallbackInfoReturnable<Boolean> cir) {
        var projectile = (Projectile) (Object) this;
        if (target instanceof EldritchCrab && target.getVehicle() != null
                && target.getVehicle() == projectile.getOwner() && target.canBeHitByProjectile()) {
            cir.setReturnValue(true);
        }
    }
}
