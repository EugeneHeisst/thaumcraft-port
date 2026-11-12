package thaumcraft.api.potions;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
/** Original PotionFluxTaint: periodic lethal taint damage, undead immunity and tainted-mob healing. */
public final class PotionFluxTaint extends MobEffect {
    public static java.util.function.Function<LivingEntity,net.minecraft.world.damagesource.DamageSource> taintDamage;
    public PotionFluxTaint(){super(MobEffectCategory.HARMFUL,0x663377);}
    @Override public boolean shouldApplyEffectTickThisTick(int duration,int amplifier){int interval=40>>amplifier;return interval>0?duration%interval==0:true;}
    @Override public boolean applyEffectTick(LivingEntity target,int amplifier){
        if(target instanceof thaumcraft.api.entities.ITaintedMob)target.heal(1);
        else if(!target.isInvertedHealAndHarm()&&!(target instanceof net.minecraft.world.entity.player.Player))target.hurt(taintDamage.apply(target),1);
        else if(!target.isInvertedHealAndHarm()&&(target.getMaxHealth()>1||target instanceof net.minecraft.world.entity.player.Player))target.hurt(taintDamage.apply(target),1);
        return true;
    }
}
