package dev.eugheist.tc4modern;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.registries.DeferredHolder;

/** Server consequences of the original unnatural hunger and contagious exhaustion. */
public final class WarpEffects {
    private WarpEffects() {}
    public static final DeferredHolder<MobEffect, MobEffect> UNNATURAL_HUNGER =
        TC4Modern.EFFECTS.register("unnatural_hunger", UnnaturalHunger::new);
    public static final DeferredHolder<MobEffect, MobEffect> INFECTIOUS_VIS_EXHAUST =
        TC4Modern.EFFECTS.register("infectious_vis_exhaust", InfectiousVisExhaust::new);
    public static final DeferredHolder<MobEffect, MobEffect> SUN_SCORNED =
        TC4Modern.EFFECTS.register("sun_scorned", SunScorned::new);
    public static final DeferredHolder<MobEffect, MobEffect> THAUMARHIA =
        TC4Modern.EFFECTS.register("thaumarhia", Thaumarhia::new);
    public static final DeferredHolder<MobEffect, MobEffect> DEATH_GAZE =
        TC4Modern.EFFECTS.register("death_gaze", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x664433) {});
    public static final DeferredHolder<MobEffect, MobEffect> BLURRED_VISION =
        TC4Modern.EFFECTS.register("blurred_vision", () -> new MobEffect(MobEffectCategory.HARMFUL, 0x808080) {});
    public static final DeferredHolder<MobEffect, MobEffect> WARP_WARD =
        TC4Modern.EFFECTS.register("warp_ward", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 14742263) {});
    public static void bootstrap() {}

    private static final class UnnaturalHunger extends MobEffect {
        private UnnaturalHunger() { super(MobEffectCategory.HARMFUL, 0x446633); }
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return true; }
        @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
            if (!target.level().isClientSide && target instanceof Player player)
                player.causeFoodExhaustion(0.025f * (amplifier + 1));
            return true;
        }
    }

    private static final class InfectiousVisExhaust extends MobEffect {
        private InfectiousVisExhaust() { super(MobEffectCategory.HARMFUL, 0x665577); }
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 40 == 0; }
        @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
            if (!target.level().isClientSide) {
                for (LivingEntity nearby : target.level().getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(4))) {
                    if (nearby.hasEffect(INFECTIOUS_VIS_EXHAUST)) continue;
                    nearby.addEffect(amplifier > 0
                        ? new MobEffectInstance(INFECTIOUS_VIS_EXHAUST, 6000, amplifier - 1, false, true)
                        : new MobEffectInstance(TC4Modern.VIS_EXHAUST, 6000, 0, false, true));
                }
            }
            return true;
        }
    }

    private static final class SunScorned extends MobEffect {
        private SunScorned() { super(MobEffectCategory.HARMFUL, 16308330); }
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 40 == 0; }
        @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
            if (!target.level().isClientSide) {
                float brightness = target.getLightLevelDependentMagicValue();
                if (brightness > 0.5f
                    && target.level().random.nextFloat() * 30f < (brightness - 0.4f) * 2f
                    && target.level().canSeeSky(target.blockPosition())) {
                    target.igniteForSeconds(4);
                } else if (brightness < 0.25f && target.level().random.nextFloat() > brightness * 2f) {
                    target.heal(1f);
                }
            }
            return true;
        }
    }


    private static final class Thaumarhia extends MobEffect {
        private Thaumarhia() { super(MobEffectCategory.HARMFUL, 0x664477); }
        @Override public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) { return duration % 20 == 0; }
        @Override public boolean applyEffectTick(LivingEntity target, int amplifier) {
            if (!target.level().isClientSide && target.level().random.nextInt(15) == 0
                && target.level().isEmptyBlock(target.blockPosition())) {
                target.level().setBlock(target.blockPosition(), TC4Modern.FLUX_GOO.get().defaultBlockState(), 3);
            }
            return true;
        }
    }

}
