package dev.eugheist.tc4modern.mixin;

import dev.eugheist.tc4modern.EldritchCrab;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla's arrow ray misses targets when the arrow starts inside their inflated bounds. */
@Mixin(AbstractArrow.class)
public abstract class CrabArrowCollisionMixin {
    @Shadow protected abstract boolean canHitEntity(Entity target);

    @Inject(method = "findHitEntity", at = @At("HEAD"), cancellable = true)
    private void tc4modern$hitOverlappingCrab(Vec3 start, Vec3 end,
            CallbackInfoReturnable<EntityHitResult> cir) {
        var arrow = (AbstractArrow) (Object) this;
        var owner = arrow.getOwner();
        if (owner == null) return;
        for (var passenger : owner.getPassengers()) {
            if (passenger instanceof EldritchCrab && canHitEntity(passenger)
                    && passenger.getBoundingBox().inflate(0.3F).contains(start)) {
                cir.setReturnValue(new EntityHitResult(passenger, start));
                return;
            }
        }
    }
}
