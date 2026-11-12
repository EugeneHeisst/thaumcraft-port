package thaumcraft.common.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Original food values, always-edible flag and regeneration probability. */
public class ItemTripleMeatTreat extends Item {
    public ItemTripleMeatTreat() {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(6).saturationModifier(0.8f)
            .alwaysEdible().effect(() -> new MobEffectInstance(MobEffects.REGENERATION, 100, 0), 0.66f).build()));
    }
}
