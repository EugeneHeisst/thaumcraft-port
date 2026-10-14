package dev.eugheist.tc4modern;

import java.util.List;
import java.util.Map;
import thaumcraft.api.crafting.CrucibleRecipe;

/** Modern lookup bridge for literal original alchemy registrations. */
public final class CrucibleRecipes {
    private static Map<String,CrucibleRecipe> recipes;
    private CrucibleRecipes() {}
    public static Map<String,CrucibleRecipe> byKey() {
        if (recipes == null) recipes = thaumcraft.common.config.ConfigCrucibleRecipes.create();
        var addon = dev.eugheist.tc4modern.api.TC4Addons.crucibleRecipes();
        if (addon.isEmpty()) return recipes;
        var all = new java.util.LinkedHashMap<String,CrucibleRecipe>(recipes); all.putAll(addon); return all;
    }
    public static List<CrucibleRecipe> entries() { return List.copyOf(byKey().values()); }
}
