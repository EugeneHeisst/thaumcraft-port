package thaumcraft.common.config;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.crafting.CrucibleRecipe;
import dev.eugheist.tc4modern.TC4Modern;

/** Literal starter entries from original ConfigRecipes.initializeAlchemyRecipes. */
public final class ConfigCrucibleRecipes {
    public static Map<String,CrucibleRecipe> create() {
        Map<String,CrucibleRecipe> recipes = new LinkedHashMap<>();
        Aspect[] primals = {Aspect.AIR,Aspect.FIRE,Aspect.WATER,Aspect.EARTH,Aspect.ORDER,Aspect.ENTROPY};
        for (int a=0;a<6;++a) {
            var aspects = new AspectList();
            for (int b=0;b<6;++b) if (a!=b) aspects.add(primals[b],2);
            recipes.put("BalancedShard_"+a,new CrucibleRecipe("CRUCIBLE",new ItemStack(TC4Modern.BALANCED_SHARD.get()),Ingredient.of(TC4Modern.SHARDS.get(a).get()),aspects));
        }
        recipes.put("Alumentum",new CrucibleRecipe("ALUMENTUM",new ItemStack(TC4Modern.ALUMENTUM.get()),Ingredient.of(Items.COAL,Items.CHARCOAL),new AspectList().merge(Aspect.ENERGY,3).merge(Aspect.FIRE,3).merge(Aspect.ENTROPY,3)));
        recipes.put("Nitor",new CrucibleRecipe("NITOR",new ItemStack(TC4Modern.NITOR.get()),Ingredient.of(Items.GLOWSTONE_DUST),new AspectList().merge(Aspect.ENERGY,3).merge(Aspect.FIRE,3).merge(Aspect.LIGHT,3)));
        recipes.put("Thaumium",new CrucibleRecipe("THAUMIUM",new ItemStack(TC4Modern.THAUMIUM.get()),Ingredient.of(Items.IRON_INGOT),new AspectList().merge(Aspect.MAGIC,4)));
        recipes.put("Tallow",new CrucibleRecipe("TALLOW",new ItemStack(TC4Modern.TALLOW.get()),Ingredient.of(Items.ROTTEN_FLESH),new AspectList().merge(Aspect.MAGIC,2)));
        recipes.put("AltGunpowder",new CrucibleRecipe("ALCHEMICALDUPLICATION",new ItemStack(Items.GUNPOWDER,2),Ingredient.of(Items.GUNPOWDER),new AspectList().merge(Aspect.FIRE,4).merge(Aspect.ENTROPY,4)));
        recipes.put("AltSlime",new CrucibleRecipe("ALCHEMICALDUPLICATION",new ItemStack(Items.SLIME_BALL,2),Ingredient.of(Items.SLIME_BALL),new AspectList().merge(Aspect.WATER,2).merge(Aspect.LIFE,2)));
        recipes.put("AltClay",new CrucibleRecipe("ALCHEMICALDUPLICATION",new ItemStack(Items.CLAY_BALL,2),Ingredient.of(Items.CLAY_BALL),new AspectList().merge(Aspect.WATER,1).merge(Aspect.EARTH,2)));
        recipes.put("AltGlowstone",new CrucibleRecipe("ALCHEMICALDUPLICATION",new ItemStack(Items.GLOWSTONE_DUST,2),Ingredient.of(Items.GLOWSTONE_DUST),new AspectList().merge(Aspect.LIGHT,3).merge(Aspect.SENSES,1)));
        recipes.put("AltInk",new CrucibleRecipe("ALCHEMICALDUPLICATION",new ItemStack(Items.INK_SAC,2),Ingredient.of(Items.INK_SAC),new AspectList().merge(Aspect.WATER,2).merge(Aspect.SENSES,2)));
        recipes.put("AltWeb",new CrucibleRecipe("ALCHEMICALMANUFACTURE",new ItemStack(Items.COBWEB,1),Ingredient.of(Items.STRING),new AspectList().merge(Aspect.TRAP,2).merge(Aspect.CLOTH,2)));
        recipes.put("AltMossyCobble",new CrucibleRecipe("ALCHEMICALMANUFACTURE",new ItemStack(Items.MOSSY_COBBLESTONE,1),Ingredient.of(Items.COBBLESTONE),new AspectList().merge(Aspect.PLANT,2).merge(Aspect.MAGIC,1)));
        recipes.put("AltIce",new CrucibleRecipe("ALCHEMICALMANUFACTURE",new ItemStack(Items.ICE,1),Ingredient.of(Items.SNOW_BLOCK),new AspectList().merge(Aspect.ORDER,1).merge(Aspect.COLD,1)));
        recipes.put("AltCrackedBrick",new CrucibleRecipe("ENTROPICPROCESSING",new ItemStack(Items.CRACKED_STONE_BRICKS,1),Ingredient.of(Items.STONE_BRICKS),new AspectList().merge(Aspect.ENTROPY,2)));
        recipes.put("AltBonemeal",new CrucibleRecipe("ENTROPICPROCESSING",new ItemStack(Items.BONE_MEAL,4),Ingredient.of(Items.BONE),new AspectList().merge(Aspect.ENTROPY,1)));
        recipes.put("PureIron",new CrucibleRecipe("PUREIRON",new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tc4modern","iron_cluster"))),Ingredient.of(net.neoforged.neoforge.common.Tags.Items.ORES_IRON),new AspectList().merge(Aspect.METAL,1).merge(Aspect.ORDER,1)));
        recipes.put("TransIron",new CrucibleRecipe("TRANSIRON",new ItemStack(Items.IRON_NUGGET,3),Ingredient.of(Items.IRON_NUGGET),new AspectList().merge(Aspect.METAL,2)));
        recipes.put("PureGold",new CrucibleRecipe("PUREGOLD",new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tc4modern","gold_cluster"))),Ingredient.of(net.neoforged.neoforge.common.Tags.Items.ORES_GOLD),new AspectList().merge(Aspect.METAL,1).merge(Aspect.ORDER,1)));
        recipes.put("TransGold",new CrucibleRecipe("TRANSGOLD",new ItemStack(Items.GOLD_NUGGET,3),Ingredient.of(Items.GOLD_NUGGET),new AspectList().merge(Aspect.METAL,2).merge(Aspect.GREED,1)));
        recipes.put("PureCopper",new CrucibleRecipe("PURECOPPER",new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tc4modern","copper_cluster"))),Ingredient.of(net.neoforged.neoforge.common.Tags.Items.ORES_COPPER),new AspectList().merge(Aspect.METAL,1).merge(Aspect.ORDER,1)));
        recipes.put("TransCopper",new CrucibleRecipe("TRANSCOPPER",new ItemStack(dev.eugheist.tc4modern.WandParts.COPPER_NUGGET.get(),3),Ingredient.of(dev.eugheist.tc4modern.WandParts.COPPER_NUGGET.get()),new AspectList().merge(Aspect.METAL,2).merge(Aspect.EXCHANGE,1)));
        recipes.put("LiquidDeath",new CrucibleRecipe("LIQUIDDEATH",new ItemStack(dev.eugheist.tc4modern.LiquidDeath.BUCKET.get()),Ingredient.of(Items.BUCKET),new AspectList().add(Aspect.DEATH,32).add(Aspect.POISON,32).add(Aspect.ENTROPY,32)));
        recipes.put("BottleTaint",new CrucibleRecipe("BOTTLETAINT",new ItemStack(dev.eugheist.tc4modern.BottleTaint.ITEM.get()),net.neoforged.neoforge.common.crafting.DataComponentIngredient.of(true,dev.eugheist.tc4modern.EssentiaItems.phialOf(Aspect.TAINT,8)),new AspectList().add(Aspect.TAINT,8).add(Aspect.MAGIC,8)));
        recipes.put("EtherealBloom",new CrucibleRecipe("ETHEREALBLOOM",new ItemStack(dev.eugheist.tc4modern.EtherealBloom.ITEM.get()),Ingredient.of(dev.eugheist.tc4modern.SilverwoodContent.SHIMMERLEAF.get()),new AspectList().add(Aspect.MAGIC,16).add(Aspect.PLANT,16).add(Aspect.HEAL,16).add(Aspect.TAINT,8)));
        recipes.put("BathSalts",new CrucibleRecipe("BATHSALTS",new ItemStack(dev.eugheist.tc4modern.Hygiene.SALTS.get()),Ingredient.of(dev.eugheist.tc4modern.InfusionContent.SALIS.get()),new AspectList().add(Aspect.MIND,6).add(Aspect.AURA,6).add(Aspect.ORDER,6).add(Aspect.HEAL,6)));
        recipes.put("SaneSoap",new CrucibleRecipe("SANESOAP",new ItemStack(dev.eugheist.tc4modern.Hygiene.SOAP.get()),Ingredient.of(dev.eugheist.tc4modern.Hygiene.TALLOW.get()),new AspectList().add(Aspect.MIND,16).add(Aspect.ELDRITCH,16).add(Aspect.ORDER,16).add(Aspect.HEAL,16)));
        recipes.put("GolemWood",new CrucibleRecipe("GOLEMWOOD",new ItemStack(dev.eugheist.tc4modern.Golemancy.placer(dev.eugheist.tc4modern.GolemMaterial.WOOD).get()),Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("tc4modern:greatwood_log"))),new AspectList().add(Aspect.MAN,4).add(Aspect.MOTION,4).add(Aspect.SOUL,4)));
        recipes.put("GolemTallow",new CrucibleRecipe("GOLEMTALLOW",new ItemStack(dev.eugheist.tc4modern.Golemancy.placer(dev.eugheist.tc4modern.GolemMaterial.TALLOW).get()),Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("tc4modern:tallow_block"))),new AspectList().add(Aspect.MAN,8).add(Aspect.MOTION,8).add(Aspect.SOUL,8)));
        recipes.put("GolemClay",new CrucibleRecipe("GOLEMCLAY",new ItemStack(dev.eugheist.tc4modern.Golemancy.placer(dev.eugheist.tc4modern.GolemMaterial.CLAY).get()),Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:clay"))),new AspectList().add(Aspect.MAN,4).add(Aspect.MOTION,4).add(Aspect.SOUL,4)));
        recipes.put("GolemFlesh",new CrucibleRecipe("GOLEMFLESH",new ItemStack(dev.eugheist.tc4modern.Golemancy.placer(dev.eugheist.tc4modern.GolemMaterial.FLESH).get()),Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("tc4modern:flesh_block"))),new AspectList().add(Aspect.MAN,8).add(Aspect.MOTION,8).add(Aspect.SOUL,8)));
        recipes.put("GolemStone",new CrucibleRecipe("GOLEMSTONE",new ItemStack(dev.eugheist.tc4modern.Golemancy.placer(dev.eugheist.tc4modern.GolemMaterial.STONE).get()),Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:stone_bricks"))),new AspectList().add(Aspect.MAN,4).add(Aspect.MOTION,4).add(Aspect.SOUL,4)));
        recipes.put("GolemIron",new CrucibleRecipe("GOLEMIRON",new ItemStack(dev.eugheist.tc4modern.Golemancy.placer(dev.eugheist.tc4modern.GolemMaterial.IRON).get()),Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("minecraft:iron_block"))),new AspectList().add(Aspect.MAN,4).add(Aspect.MOTION,4).add(Aspect.SOUL,4)));
        recipes.put("GolemThaumium",new CrucibleRecipe("GOLEMTHAUMIUM",new ItemStack(dev.eugheist.tc4modern.Golemancy.placer(dev.eugheist.tc4modern.GolemMaterial.THAUMIUM).get()),Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("tc4modern:thaumium_block"))),new AspectList().add(Aspect.MAN,8).add(Aspect.MOTION,8).add(Aspect.SOUL,8)));
        recipes.put("CoreFill",new CrucibleRecipe("COREFILL",new ItemStack(dev.eugheist.tc4modern.Golemancy.FILL.get()),Ingredient.of(dev.eugheist.tc4modern.Golemancy.BLANK.get()),new AspectList().add(Aspect.HUNGER,5).add(Aspect.VOID,5)));
        recipes.put("CoreEmpty",new CrucibleRecipe("COREEMPTY",new ItemStack(dev.eugheist.tc4modern.Golemancy.EMPTY.get()),Ingredient.of(dev.eugheist.tc4modern.Golemancy.BLANK.get()),new AspectList().add(Aspect.GREED,5).add(Aspect.VOID,5)));
        recipes.put("GolemStraw",new CrucibleRecipe("GOLEMSTRAW",new ItemStack(dev.eugheist.tc4modern.Golemancy.STRAW.get()),Ingredient.of(Items.HAY_BLOCK),new AspectList().add(Aspect.MAN,4).add(Aspect.MOTION,4).add(Aspect.SOUL,4)));
        recipes.put("CoreLiquid",new CrucibleRecipe("CORELIQUID",new ItemStack(dev.eugheist.tc4modern.Golemancy.LIQUID.get()),Ingredient.of(dev.eugheist.tc4modern.Golemancy.BLANK.get()),new AspectList().add(Aspect.WATER,5).add(Aspect.VOID,5)));
        recipes.put("CoreGuard",new CrucibleRecipe("COREGUARD",new ItemStack(dev.eugheist.tc4modern.Golemancy.GUARD.get()),Ingredient.of(dev.eugheist.tc4modern.Golemancy.BLANK.get()),new AspectList().add(Aspect.WEAPON,5).add(Aspect.TRAP,5)));
        recipes.put("CoreButcher",new CrucibleRecipe("COREBUTCHER",new ItemStack(dev.eugheist.tc4modern.Golemancy.BUTCHER.get()),Ingredient.of(dev.eugheist.tc4modern.Golemancy.GUARD.get()),new AspectList().add(Aspect.FLESH,5).add(Aspect.BEAST,5)));
        recipes.put("CoreHarvest",new CrucibleRecipe("COREHARVEST",new ItemStack(dev.eugheist.tc4modern.Golemancy.HARVEST.get()),Ingredient.of(dev.eugheist.tc4modern.Golemancy.BLANK.get()),new AspectList().add(Aspect.HARVEST,5).add(Aspect.CROP,5)));
        recipes.put("CoreGather",new CrucibleRecipe("COREGATHER",new ItemStack(dev.eugheist.tc4modern.Golemancy.GATHER.get()),Ingredient.of(dev.eugheist.tc4modern.Golemancy.BLANK.get()),new AspectList().add(Aspect.GREED,5).add(Aspect.EARTH,5)));
        /* ConfigRecipes: VoidSeed wheat seeds + Tenebrae8 Vacuos8 Alienis2; VoidMetal void seed + Metallum8 (both research VOIDMETAL) */
        recipes.put("VoidSeed",new CrucibleRecipe("VOIDMETAL",new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("tc4modern:void_seed"))),Ingredient.of(Items.WHEAT_SEEDS),new AspectList().add(Aspect.DARKNESS,8).add(Aspect.VOID,8).add(Aspect.ELDRITCH,2)));
        recipes.put("VoidMetal",new CrucibleRecipe("VOIDMETAL",new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("tc4modern:void_ingot"))),Ingredient.of(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("tc4modern:void_seed"))),new AspectList().add(Aspect.METAL,8)));
        return java.util.Collections.unmodifiableMap(recipes);
    }
}
