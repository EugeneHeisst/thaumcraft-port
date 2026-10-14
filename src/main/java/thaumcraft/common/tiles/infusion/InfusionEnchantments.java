package thaumcraft.common.tiles.infusion;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
/**
 * ConfigRecipes InfEnch0..21 (TC4 4.2.3.5) with MCP field names mapped to modern ids; the two Thaumcraft
 * enchantments (InfEnchRepair/Haste) use their modern data-driven holders. Salis mundus is itemResource 14.
 */
public final class InfusionEnchantments {
    private InfusionEnchantments() {}
    public record Recipe(String key, ResourceKey<Enchantment> enchantment, int instability, AspectList aspects, List<java.util.function.Supplier<Item>> components) {
        /** InfusionEnchantmentRecipe.recipeXP = max(1, minEnchantability(1) / 3). */
        public int baseXP(Holder<Enchantment> e) { return Math.max(1, e.value().getMinCost(1) / 3); }
        public int calcInstability(ItemStack input) {
            int i = 0;
            for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(input).entrySet()) i += entry.getIntValue();
            return i / 2 + instability;
        }
        public int calcXP(ItemStack input, Holder<Enchantment> e) { return baseXP(e) * (1 + EnchantmentHelper.getItemEnchantmentLevel(e, input)); }
        public float getEssentiaMod(ItemStack input, Holder<Enchantment> e) {
            float mod = EnchantmentHelper.getItemEnchantmentLevel(e, input);
            for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(input).entrySet()) {
                if (entry.getKey().equals(e)) continue;
                mod += entry.getIntValue() * 0.1f;
            }
            return mod;
        }
        /** matches(): enchantable target, below max level, compatible with all existing enchantments. */
        public boolean acceptsCentral(ItemStack central, Holder<Enchantment> e) {
            if (!e.value().canEnchant(central) || !central.isEnchantable() && !central.isEnchanted()) return false;
            for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(central).entrySet()) {
                var ench = entry.getKey();
                if (ench.equals(e) && entry.getIntValue() >= ench.value().getMaxLevel()) return false;
                if (ench.equals(e) || Enchantment.areCompatible(e, ench)) continue;
                return false;
            }
            return true;
        }
    }
    private static AspectList a(Object... kv) { var l = new AspectList(); for (int i = 0; i < kv.length; i += 2) l.add((Aspect) kv[i], (Integer) kv[i + 1]); return l; }
    @SafeVarargs private static List<java.util.function.Supplier<Item>> c(java.util.function.Supplier<Item>... items) { return List.of(items); }
    private static java.util.function.Supplier<Item> v(Item item) { return () -> item; }
    private static List<Recipe> recipes;
    public static List<Recipe> all(java.util.function.Supplier<Item> salis) {
        if (recipes != null) return recipes;
        var s = salis;
        recipes = List.of(
            new Recipe("InfEnchRepair", dev.eugheist.tc4modern.BasicsEnchantments.REPAIR, 4, a(Aspect.MAGIC,8,Aspect.CRAFT,10,Aspect.ORDER,10),c(v(Items.ANVIL),s)),
            new Recipe("InfEnchHaste", dev.eugheist.tc4modern.BasicsEnchantments.HASTE, 3, a(Aspect.MAGIC,4,Aspect.TRAVEL,8,Aspect.FLIGHT,8),c(()->dev.eugheist.tc4modern.TC4Modern.NITOR.get(),s)),
            new Recipe("InfEnch0", Enchantments.PROTECTION, 1, a(Aspect.MAGIC, 4, Aspect.ARMOR, 8), c(v(Items.IRON_INGOT), s)),
            new Recipe("InfEnch1", Enchantments.FIRE_PROTECTION, 1, a(Aspect.MAGIC, 4, Aspect.ARMOR, 4, Aspect.FIRE, 4), c(v(Items.IRON_INGOT), v(Items.MAGMA_CREAM), s)),
            new Recipe("InfEnch2", Enchantments.BLAST_PROTECTION, 1, a(Aspect.MAGIC, 4, Aspect.ARMOR, 4, Aspect.ENTROPY, 4), c(v(Items.IRON_INGOT), v(Items.GUNPOWDER), s)),
            new Recipe("InfEnch3", Enchantments.PROJECTILE_PROTECTION, 1, a(Aspect.MAGIC, 4, Aspect.ARMOR, 4, Aspect.FLIGHT, 4), c(v(Items.IRON_INGOT), v(Items.ARROW), s)),
            new Recipe("InfEnch4", Enchantments.FEATHER_FALLING, 1, a(Aspect.MAGIC, 4, Aspect.AIR, 4, Aspect.FLIGHT, 4), c(v(Items.FEATHER), s)),
            new Recipe("InfEnch5", Enchantments.RESPIRATION, 2, a(Aspect.MAGIC, 4, Aspect.AIR, 8, Aspect.WATER, 8), c(v(Items.SUGAR_CANE), s)),
            new Recipe("InfEnch6", Enchantments.AQUA_AFFINITY, 2, a(Aspect.MAGIC, 4, Aspect.MOTION, 8, Aspect.WATER, 8), c(v(Items.SUGAR_CANE), v(Items.SLIME_BALL), s)),
            new Recipe("InfEnch7", Enchantments.THORNS, 2, a(Aspect.MAGIC, 4, Aspect.WEAPON, 8, Aspect.PLANT, 8), c(v(Items.DEAD_BUSH), v(Items.QUARTZ), s)),
            new Recipe("InfEnch8", Enchantments.SHARPNESS, 2, a(Aspect.MAGIC, 4, Aspect.WEAPON, 8), c(v(Items.IRON_SWORD), s)),
            new Recipe("InfEnch9", Enchantments.SMITE, 2, a(Aspect.MAGIC, 4, Aspect.WEAPON, 4, Aspect.UNDEAD, 4), c(v(Items.IRON_SWORD), v(Items.GLOWSTONE_DUST), s)),
            new Recipe("InfEnch10", Enchantments.BANE_OF_ARTHROPODS, 2, a(Aspect.MAGIC, 4, Aspect.WEAPON, 4, Aspect.BEAST, 4), c(v(Items.IRON_SWORD), () -> net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("tc4modern", "amber")), s)),
            new Recipe("InfEnch11", Enchantments.KNOCKBACK, 1, a(Aspect.MAGIC, 4, Aspect.WEAPON, 3, Aspect.MOTION, 3), c(v(Items.PISTON), s)),
            new Recipe("InfEnch12", Enchantments.FIRE_ASPECT, 3, a(Aspect.MAGIC, 4, Aspect.WEAPON, 4, Aspect.FIRE, 8), c(v(Items.IRON_SWORD), v(Items.BLAZE_POWDER), s)),
            new Recipe("InfEnch13", Enchantments.LOOTING, 3, a(Aspect.MAGIC, 4, Aspect.WEAPON, 4, Aspect.GREED, 8), c(v(Items.IRON_SWORD), v(Items.DIAMOND), s)),
            new Recipe("InfEnch14", Enchantments.EFFICIENCY, 2, a(Aspect.MAGIC, 4, Aspect.TOOL, 4, Aspect.ORDER, 4), c(v(Items.IRON_PICKAXE), s)),
            new Recipe("InfEnch15", Enchantments.SILK_TOUCH, 5, a(Aspect.MAGIC, 16, Aspect.TOOL, 16, Aspect.ORDER, 16, Aspect.HARVEST, 16, Aspect.MINE, 16), c(v(Items.IRON_PICKAXE), v(Items.COBWEB), s)),
            new Recipe("InfEnch16", Enchantments.UNBREAKING, 2, a(Aspect.MAGIC, 4, Aspect.TOOL, 4, Aspect.ORDER, 8), c(v(Items.IRON_PICKAXE), v(Items.OBSIDIAN), s)),
            new Recipe("InfEnch17", Enchantments.FORTUNE, 3, a(Aspect.MAGIC, 4, Aspect.TOOL, 4, Aspect.GREED, 8), c(v(Items.IRON_PICKAXE), v(Items.DIAMOND), s)),
            new Recipe("InfEnch18", Enchantments.POWER, 2, a(Aspect.MAGIC, 4, Aspect.WEAPON, 8), c(v(Items.BOW), s)),
            new Recipe("InfEnch19", Enchantments.PUNCH, 2, a(Aspect.MAGIC, 4, Aspect.WEAPON, 3, Aspect.MOTION, 3), c(v(Items.PISTON), s)),
            new Recipe("InfEnch20", Enchantments.FLAME, 3, a(Aspect.MAGIC, 4, Aspect.WEAPON, 4, Aspect.FIRE, 8), c(v(Items.BOW), v(Items.BLAZE_POWDER), s)),
            new Recipe("InfEnch21", Enchantments.INFINITY, 5, a(Aspect.MAGIC, 8, Aspect.WEAPON, 16, Aspect.VOID, 16, Aspect.EXCHANGE, 16), c(v(Items.BOW), v(Items.ARROW), s))
        );
        return recipes;
    }
    public static Holder<Enchantment> holder(HolderLookup.Provider registries, ResourceKey<Enchantment> key) { return registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key); }
}
