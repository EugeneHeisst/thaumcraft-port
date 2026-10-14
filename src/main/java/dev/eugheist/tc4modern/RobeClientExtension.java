package dev.eugheist.tc4modern;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

/** Original ItemRobeArmor uses the default armor mesh; only its original dye fallback needs adapting. */
public final class RobeClientExtension implements IClientItemExtensions {
    @Override public int getDefaultDyeColor(ItemStack stack){
        return 0xFF000000|thaumcraft.common.items.armor.ItemRobeArmor.color(stack);
    }
}
