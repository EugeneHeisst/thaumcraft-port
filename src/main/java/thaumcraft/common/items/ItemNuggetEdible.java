package thaumcraft.common.items;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;

/** Original ItemNuggetEdible constructor values and ten-tick use duration, adapted to 1.21.1. */
public class ItemNuggetEdible extends Item {
    public final int field_77855_a = 10;
    public final String iconName;
    public ItemNuggetEdible(String iconName) {
        super(new Item.Properties().food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.3f).build()));
        this.iconName = iconName;
    }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) {return this.field_77855_a;}
}
