package dev.eugheist.tc4modern;

import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original ConfigRecipes JarBrain data, shared by the altar and research book. */
final class BrainJarRecipe {
    static final int INSTABILITY = 4;
    private BrainJarRecipe() {}
    static ItemStack input() { return new ItemStack(EssentiaDevices.JAR.get()); }
    static ItemStack output() { return new ItemStack(BrainJarContent.ITEM.get()); }
    static List<ItemStack> ingredients() {
        return List.of(new ItemStack(ZombieBrainContent.ITEM.get()), new ItemStack(Items.SPIDER_EYE),
            new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.SPIDER_EYE));
    }
    static AspectList cost() {
        return new AspectList().add(Aspect.MIND, 10).add(Aspect.SENSES, 10).add(Aspect.UNDEAD, 20);
    }
}
