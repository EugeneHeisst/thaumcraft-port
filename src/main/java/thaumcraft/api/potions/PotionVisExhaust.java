package thaumcraft.api.potions;
/** Original harmful, non-ticking Vis Exhaust effect, with empty original curative list. */
public final class PotionVisExhaust extends net.minecraft.world.effect.MobEffect {
    public PotionVisExhaust(){super(net.minecraft.world.effect.MobEffectCategory.HARMFUL,0x664477);}
    @Override public void fillEffectCures(java.util.Set<net.neoforged.neoforge.common.EffectCure> cures,net.minecraft.world.effect.MobEffectInstance instance){}
}
