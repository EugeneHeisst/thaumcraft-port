package baubles.api;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/** Baubles 1.0.1.10 item contract; worn ticks run once per player tick while the item sits in a bauble slot. */
public interface IBauble {
    BaubleType getBaubleType(ItemStack stack);
    default void onWornTick(ItemStack stack, LivingEntity player) {}
    default void onEquipped(ItemStack stack, LivingEntity player) {}
    default void onUnequipped(ItemStack stack, LivingEntity player) {}
    default boolean canEquip(ItemStack stack, LivingEntity player) { return true; }
    default boolean canUnequip(ItemStack stack, LivingEntity player) { return true; }
}
