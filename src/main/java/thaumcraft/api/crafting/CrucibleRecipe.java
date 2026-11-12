package thaumcraft.api.crafting;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original TC4 CrucibleRecipe: Ingredient replaces legacy ItemStack/OreDictionary matching. */
public final class CrucibleRecipe {
    private final ItemStack recipeOutput;
    public final Ingredient catalyst;
    public final AspectList aspects;
    public final String key;
    public CrucibleRecipe(String researchKey, ItemStack result, Ingredient catalyst, AspectList tags) {
        this.recipeOutput = result;
        this.catalyst = catalyst;
        this.aspects = tags;
        this.key = researchKey;
    }
    public boolean matches(AspectList tags, ItemStack stack) {
        if (!catalystMatches(stack) || tags == null) return false;
        for (Aspect aspect : aspects.getAspects()) {
            if (tags.getAmount(aspect) < aspects.getAmount(aspect)) return false;
        }
        return true;
    }
    public boolean catalystMatches(ItemStack stack) { return catalyst.test(stack); }
    public AspectList removeMatching(AspectList tags) {
        AspectList remaining = new AspectList();
        remaining.aspects.putAll(tags.aspects);
        for (Aspect aspect : aspects.getAspects()) remaining.remove(aspect, aspects.getAmount(aspect));
        return remaining;
    }
    /** Original crafting manager prioritizes the number of distinct aspects, preserving first tie. */
    public static CrucibleRecipe findMatching(java.util.List<CrucibleRecipe> recipes,
            java.util.Set<String> completed, AspectList aspects, ItemStack lastDrop) {
        int highest = 0;
        CrucibleRecipe selected = null;
        for (CrucibleRecipe recipe : recipes) {
            ItemStack catalyst = lastDrop.copyWithCount(1);
            if (!completed.contains(recipe.key) || !recipe.matches(aspects, catalyst)
                    || recipe.aspects.size() <= highest) continue;
            highest = recipe.aspects.size();
            selected = recipe;
        }
        return selected;
    }
    public ItemStack getRecipeOutput() { return recipeOutput; }
}
