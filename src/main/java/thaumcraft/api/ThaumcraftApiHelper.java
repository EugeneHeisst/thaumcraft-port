package thaumcraft.api;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Modern registry adapter for the ported AspectList(ItemStack) constructor. */
public final class ThaumcraftApiHelper {
    public static AspectList getObjectAspects(ItemStack stack) {
        AspectList result = new AspectList();
        dev.eugheist.tc4modern.AspectTable.get(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
            .forEach((tag, amount) -> { Aspect aspect = Aspect.getAspect(tag); if (aspect != null) result.add(aspect, amount); });
        return result;
    }
}
