package dev.eugheist.tc4modern;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/** Original per-instance effect construction; contagious copies retain their own default cures. */
public final class WarpEffectApplication {
    private WarpEffectApplication() {}
    public static boolean apply(LivingEntity target, WarpEventPlan.Plan plan) {
        if (target.level().isClientSide) return false;
        Holder<MobEffect> effect = switch (plan.kind()) {
            case VIS_EXHAUST -> TC4Modern.VIS_EXHAUST;
            case INFECTIOUS_VIS_EXHAUST -> WarpEffects.INFECTIOUS_VIS_EXHAUST;
            case THAUMARHIA -> WarpEffects.THAUMARHIA;
            case UNNATURAL_HUNGER -> WarpEffects.UNNATURAL_HUNGER;
            case BLURRED_VISION -> WarpEffects.BLURRED_VISION;
            case SUN_SCORNED -> WarpEffects.SUN_SCORNED;
            case DEATH_GAZE -> WarpEffects.DEATH_GAZE;
            case MINING_FATIGUE -> MobEffects.DIG_SLOWDOWN;
            case BLINDNESS -> MobEffects.BLINDNESS;
            case NIGHT_VISION -> MobEffects.NIGHT_VISION;
            default -> null;
        };
        if (effect == null) return false;
        var instance = new MobEffectInstance(effect, plan.duration(), plan.amplifier(),
            plan.kind() != WarpEventPlan.Kind.INFECTIOUS_VIS_EXHAUST, true);
        switch (plan.kind()) {
            case VIS_EXHAUST, INFECTIOUS_VIS_EXHAUST, THAUMARHIA, UNNATURAL_HUNGER, SUN_SCORNED, DEATH_GAZE -> instance.getCures().clear();
            default -> {}
        }
        target.addEffect(instance);
        return true;
    }
}
