package dev.eugheist.tc4modern;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.AspectList;

/** ConfigRecipes JarLabel&lt;n&gt; (label + phial of essentia -> aspect label) and JarLabelNull (marked label -> blank). */
public final class LabelRecipes {
    private LabelRecipes() {}
    public static final class Aspect extends CustomRecipe {
        public Aspect(CraftingBookCategory category) { super(category); }
        @Override public boolean matches(CraftingInput input, Level level) { return result(input) != null; }
        private static ItemStack result(CraftingInput input) {
            ItemStack label = null, phial = null;
            for (int i = 0; i < input.size(); i++) {
                var s = input.getItem(i);
                if (s.isEmpty()) continue;
                if (s.is(EssentiaDevices.LABEL.get()) && label == null) label = s;
                else if (s.is(EssentiaDevices.ESSENTIA_PHIAL.get()) && phial == null) phial = s;
                else return null;
            }
            if (label == null || phial == null) return null;
            var aspects = EssentiaItems.getAspects(phial);
            // The original NBT recipe matches a phial holding exactly 8 of one aspect.
            if (aspects == null || aspects.size() != 1 || aspects.getAmount(aspects.getAspects()[0]) != 8) return null;
            var out = new ItemStack(EssentiaDevices.LABEL.get());
            EssentiaItems.setAspects(out, new AspectList().add(aspects.getAspects()[0], 0));
            return out;
        }
        @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { var r = result(input); return r == null ? ItemStack.EMPTY : r; }
        @Override public boolean canCraftInDimensions(int w, int h) { return w * h >= 2; }
        @Override public RecipeSerializer<?> getSerializer() { return EssentiaDevices.LABEL_ASPECT_SERIALIZER.get(); }
    }
    public static final class Clear extends CustomRecipe {
        public Clear(CraftingBookCategory category) { super(category); }
        @Override public boolean matches(CraftingInput input, Level level) {
            ItemStack label = null;
            for (int i = 0; i < input.size(); i++) {
                var s = input.getItem(i);
                if (s.isEmpty()) continue;
                if (label != null || !s.is(EssentiaDevices.LABEL.get())) return false;
                label = s;
            }
            return label != null && EssentiaItems.getAspects(label) != null;
        }
        @Override public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) { return new ItemStack(EssentiaDevices.LABEL.get()); }
        @Override public boolean canCraftInDimensions(int w, int h) { return w * h >= 1; }
        @Override public RecipeSerializer<?> getSerializer() { return EssentiaDevices.LABEL_CLEAR_SERIALIZER.get(); }
    }
}
