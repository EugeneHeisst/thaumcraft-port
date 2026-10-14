package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * InfusionRecipe (item output) port: original ConfigRecipes.initializeInfusionRecipes entries whose items exist.
 * Matching follows InfusionRecipe.matches: research, central item, and every component consumed exactly once.
 */
public final class InfusionItemRecipes {
    private InfusionItemRecipes() {}
    public record Recipe(String key, String research, Supplier<Item> output, int instability, AspectList aspects, Supplier<Item> central, List<Supplier<Item>> components) {
        public boolean augment(){return key.equals("HelmGoggles")||key.startsWith("Mask")||key.equals("AdvancedGolem");}
        public ItemStack outputStack(ItemStack input){
            if(!augment())return new ItemStack(output.get());
            var result=input.copyWithCount(1);var data=NoteData.tag(result);
            if(key.equals("AdvancedGolem"))data.putByte("advanced",(byte)1);
            else if(key.equals("HelmGoggles"))data.putByte("goggles",(byte)1);
            else data.putInt("mask",switch(key){case "MaskGrinningDevil"->0;case "MaskAngryGhost"->1;default->2;});
            result.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));return result;
        }
        public List<ItemStack> componentStacks(){var stacks=new ArrayList<ItemStack>();for(var c:components)stacks.add(new ItemStack(c.get()));
            var potion=switch(key){case "RunicRingCharged"->net.minecraft.world.item.alchemy.Potions.STRONG_SWIFTNESS;case "RunicRingHealing"->net.minecraft.world.item.alchemy.Potions.LONG_REGENERATION;case "RunicAmuletEmergency"->net.minecraft.world.item.alchemy.Potions.STRONG_STRENGTH;case "RunicGirdleKinetic"->net.minecraft.world.item.alchemy.Potions.STRONG_HARMING;default->null;};
            if(key.equals("BootsTraveller")){var data=new net.minecraft.nbt.CompoundTag();data.putBoolean("TC4AnyRawFish",true);stacks.get(5).set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));}
            if(key.equals("CoreAlchemy"))for(int i=1;i<4;i++)stacks.set(i,net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION,net.minecraft.world.item.alchemy.Potions.WATER));
            if(potion!=null)stacks.set(3,net.minecraft.world.item.alchemy.PotionContents.createItemStack(key.equals("RunicGirdleKinetic")?Items.SPLASH_POTION:Items.POTION,potion));return stacks;}
    }
    private static List<Recipe> recipes;
    private static AspectList a(Object... kv) { var l = new AspectList(); for (int i = 0; i < kv.length; i += 2) l.add((Aspect) kv[i], (Integer) kv[i + 1]); return l; }
    private static Supplier<Item> shard(int i) { return i == 6 ? () -> TC4Modern.BALANCED_SHARD.get() : () -> TC4Modern.SHARDS.get(i).get(); }
    private static Supplier<Item> v(Item i) { return () -> i; }
    /** itemResource 8, the vis filter. */
    private static Supplier<Item> filter() { return () -> net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("vis_filter")); }
    /** blockCrystal 6, the mixed crystal cluster. */
    private static Supplier<Item> mixed() { return () -> BasicsContent.CLUSTERS.get(6).get().asItem(); }
    /** Original recipes followed by addon recipes registered through TC4Addons. */
    public static List<Recipe> all() {
        var addon = dev.eugheist.tc4modern.api.TC4Addons.infusionRecipes();
        if (addon.isEmpty()) return base();
        var all = new ArrayList<>(base()); all.addAll(addon); return List.copyOf(all);
    }
    private static List<Recipe> base() {
        if (recipes != null) return recipes;
        Supplier<Item> salis = () -> InfusionContent.SALIS.get();
        int cap6 = 6, rod6 = 6, blaze6 = 6, sw9 = 9;
        recipes = new ArrayList<>(List.of(
            new Recipe("CoreAlchemy","COREALCHEMY",()->Golemancy.ESSENTIA.get(),2,a(Aspect.MAGIC,15,Aspect.WATER,15,Aspect.MOTION,15),()->Golemancy.LIQUID.get(),List.of(()->EssentiaDevices.JAR.get().asItem(),v(Items.POTION),v(Items.POTION),v(Items.POTION))),
            /* TravelTrunk: hungry chest + iron ingot, greatwood planks x2, wooden golem */
            new Recipe("TravelTrunk","TRAVELTRUNK",()->TravelingTrunk.ITEM.get(),3,a(Aspect.MOTION,4,Aspect.SOUL,4,Aspect.TRAVEL,4,Aspect.VOID,16),()->HungryChest.BLOCK.get().asItem(),List.of(v(Items.IRON_INGOT),()->AlchemyContent.GREATWOOD_PLANKS.get().asItem(),()->Golemancy.placer(GolemMaterial.WOOD).get(),()->AlchemyContent.GREATWOOD_PLANKS.get().asItem())),
            /* SanityCheck: thaumometer + mirrored glass, zombie brain, diamond */
            new Recipe("SanityCheck","SANITYCHECK",()->SanityChecker.ITEM.get(),4,a(Aspect.MIND,24,Aspect.SENSES,24,Aspect.ELDRITCH,8),()->TC4Modern.THAUMOMETER.get(),List.of(()->ArtificeDevices.MIRROR_GLASS.get(),()->ZombieBrainContent.ITEM.get(),v(Items.DIAMOND))),
            /* ARMORVOIDFORTRESS: void armour fused with goggles / thaumaturge robes */
            new Recipe("VoidRobeHelm","ARMORVOIDFORTRESS",()->VoidRobe.HELM.get(),6,a(Aspect.METAL,16,Aspect.SENSES,16,Aspect.ARMOR,16,Aspect.CLOTH,16,Aspect.MAGIC,16,Aspect.ELDRITCH,16,Aspect.VOID,16),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_helmet")),List.of(()->TC4Modern.GOGGLES.get(),()->TC4Modern.ENCHANTED_FABRIC.get(),()->TC4Modern.ENCHANTED_FABRIC.get(),()->InfusionContent.SALIS.get(),()->TC4Modern.ENCHANTED_FABRIC.get(),()->TC4Modern.ENCHANTED_FABRIC.get())),
            new Recipe("VoidRobeChest","ARMORVOIDFORTRESS",()->VoidRobe.CHEST.get(),6,a(Aspect.METAL,24,Aspect.ARMOR,24,Aspect.CLOTH,24,Aspect.MAGIC,16,Aspect.ELDRITCH,16,Aspect.VOID,24),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_chestplate")),List.of(()->TC4Modern.ROBE_CHEST.get(),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_ingot")),()->ThaumiumContent.ingot(),()->InfusionContent.SALIS.get(),()->TC4Modern.ENCHANTED_FABRIC.get(),v(Items.LEATHER))),
            new Recipe("VoidRobeLegs","ARMORVOIDFORTRESS",()->VoidRobe.LEGS.get(),6,a(Aspect.METAL,20,Aspect.ARMOR,20,Aspect.CLOTH,20,Aspect.MAGIC,16,Aspect.ELDRITCH,16,Aspect.VOID,20),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_leggings")),List.of(()->TC4Modern.ROBE_LEGS.get(),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_ingot")),()->ThaumiumContent.ingot(),()->InfusionContent.SALIS.get(),()->TC4Modern.ENCHANTED_FABRIC.get(),v(Items.LEATHER))),
            /* PrimalCrusher: primordial pearl + 2 primal charms, void pick and shovel, elemental pick and shovel */
            new Recipe("EldritchEye","OCULUS",()->WarpEntities.ELDRITCH_EYE.get(),5,a(Aspect.ELDRITCH,64,Aspect.VOID,16,Aspect.DARKNESS,16,Aspect.TRAVEL,16),()->Items.ENDER_EYE,List.of(()->PechTrades.mod("void_seed").getItem(),()->Items.GOLD_INGOT)),
            new Recipe("PrimalCrusher","PRIMALCRUSHER",()->PrimalCrusher.ITEM.get(),6,a(Aspect.MINE,24,Aspect.TOOL,24,Aspect.ENTROPY,16,Aspect.VOID,16,Aspect.WEAPON,16,Aspect.ELDRITCH,16,Aspect.GREED,16),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("primordial_pearl")),List.of(()->WandPedestal.PRIMAL_CHARM.get(),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_pickaxe")),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_shovel")),()->WandPedestal.PRIMAL_CHARM.get(),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("elemental_pickaxe")),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("elemental_shovel")))),
            /* EssentiaReservoir: essentia buffer + void ingot, 2 jars, void ingot, 2 jars */
            new Recipe("EssentiaReservoir","ESSENTIARESERVOIR",()->EssentiaReservoir.ITEM.get(),6,a(Aspect.WATER,8,Aspect.VOID,8,Aspect.MAGIC,8,Aspect.EXCHANGE,8),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("essentia_buffer")),List.of(()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_ingot")),()->EssentiaDevices.JAR.get().asItem(),()->EssentiaDevices.JAR.get().asItem(),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_ingot")),()->EssentiaDevices.JAR.get().asItem(),()->EssentiaDevices.JAR.get().asItem())),
            new Recipe("CoreSorting","CORESORTING",()->Golemancy.SORTING.get(),3,a(Aspect.VOID,16,Aspect.EXCHANGE,16,Aspect.HUNGER,16,Aspect.GREED,16),()->ZombieBrainContent.ITEM.get(),List.of(()->Golemancy.FILL.get(),v(Items.COMPARATOR),()->Golemancy.EMPTY.get(),v(Items.PAPER))),
            new Recipe("CoreFishing","COREFISHING",()->Golemancy.FISHING.get(),3,a(Aspect.WATER,16,Aspect.HARVEST,16,Aspect.BEAST,16),()->Golemancy.HARVEST.get(),List.of(v(Items.FISHING_ROD),v(Items.COD),v(Items.PUFFERFISH),v(Items.SALMON))),
            new Recipe("CoreUse","COREUSE",()->Golemancy.USE.get(),3,a(Aspect.TOOL,20,Aspect.MECHANISM,20,Aspect.MAN,20),()->Golemancy.EMPTY.get(),List.of(v(Items.COMPARATOR),v(Items.FLINT_AND_STEEL),v(Items.SHEARS),v(Items.LEVER))),
            new Recipe("CoreLumber","CORELUMBER",()->Golemancy.LUMBER.get(),2,a(Aspect.TOOL,16,Aspect.TREE,16,Aspect.HARVEST,16),()->Golemancy.HARVEST.get(),List.of(()->ElementalTools.item("axe"),v(Items.IRON_AXE),v(Items.IRON_AXE),v(Items.IRON_AXE))),
            new Recipe("ThaumiumFortressHelm","ARMORFORTRESS",()->FortressArmor.item("helmet"),3,a(Aspect.METAL,24,Aspect.ARMOR,16,Aspect.MAGIC,16),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("thaumium_helmet")),List.of(ThaumiumContent::ingot,ThaumiumContent::ingot,v(Items.GOLD_INGOT),v(Items.GOLD_INGOT),v(Items.EMERALD))),
            new Recipe("ThaumiumFortressChest","ARMORFORTRESS",()->FortressArmor.item("chestplate"),3,a(Aspect.METAL,24,Aspect.ARMOR,24,Aspect.MAGIC,16),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("thaumium_chestplate")),List.of(ThaumiumContent::ingot,ThaumiumContent::ingot,ThaumiumContent::ingot,ThaumiumContent::ingot,v(Items.GOLD_INGOT),v(Items.LEATHER))),
            new Recipe("ThaumiumFortressLegs","ARMORFORTRESS",()->FortressArmor.item("leggings"),3,a(Aspect.METAL,24,Aspect.ARMOR,20,Aspect.MAGIC,16),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("thaumium_leggings")),List.of(ThaumiumContent::ingot,ThaumiumContent::ingot,ThaumiumContent::ingot,v(Items.GOLD_INGOT),v(Items.LEATHER))),
            new Recipe("HelmGoggles","HELMGOGGLES",()->FortressArmor.item("helmet"),5,a(Aspect.SENSES,32,Aspect.AURA,16,Aspect.ARMOR,16),()->FortressArmor.item("helmet"),List.of(v(Items.SLIME_BALL),()->TC4Modern.GOGGLES.get())),
            new Recipe("MaskGrinningDevil","MASKGRINNINGDEVIL",()->FortressArmor.item("helmet"),8,a(Aspect.MIND,64,Aspect.HEAL,64,Aspect.ARMOR,16),()->FortressArmor.item("helmet"),List.of(v(Items.INK_SAC),v(Items.IRON_INGOT),v(Items.LEATHER),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("shimmerleaf")),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("zombie_brain")),v(Items.IRON_INGOT))),
            new Recipe("MaskAngryGhost","MASKANGRYGHOST",()->FortressArmor.item("helmet"),8,a(Aspect.ENTROPY,64,Aspect.DEATH,64,Aspect.ARMOR,16),()->FortressArmor.item("helmet"),List.of(v(Items.BONE_MEAL),v(Items.IRON_INGOT),v(Items.LEATHER),v(Items.POISONOUS_POTATO),v(Items.WITHER_SKELETON_SKULL),v(Items.IRON_INGOT))),
            new Recipe("MaskSippingFiend","MASKSIPPINGFIEND",()->FortressArmor.item("helmet"),8,a(Aspect.UNDEAD,64,Aspect.LIFE,64,Aspect.ARMOR,16),()->FortressArmor.item("helmet"),List.of(v(Items.RED_DYE),v(Items.IRON_INGOT),v(Items.LEATHER),v(Items.GHAST_TEAR),v(Items.MILK_BUCKET),v(Items.IRON_INGOT))),
            new Recipe("ArcaneBore","ARCANEBORE",()->ArcaneBore.BORE.get().asItem(),4,a(Aspect.ENERGY,16,Aspect.MINE,32,Aspect.MECHANISM,32,Aspect.VOID,16,Aspect.MOTION,16),v(Items.PISTON),List.of(()->AlchemyContent.GREATWOOD_PLANKS.get().asItem(),()->AlchemyContent.GREATWOOD_PLANKS.get().asItem(),v(Items.GOLD_INGOT),v(Items.GOLD_INGOT),v(Items.DIAMOND_PICKAXE),v(Items.DIAMOND_SHOVEL),shard(0),shard(3))),
            new Recipe("ElementalShovel","ELEMENTALSHOVEL",()->ElementalTools.item("shovel"),1,a(Aspect.EARTH,16,Aspect.CRAFT,8),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("thaumium_shovel")),List.of(shard(3),shard(3),v(Items.DIAMOND),()->TC4Modern.GREATWOOD_LOG_ITEM.get())),
            new Recipe("ElementalHoe","ELEMENTALHOE",()->ElementalTools.item("hoe"),1,a(Aspect.HARVEST,8,Aspect.PLANT,8,Aspect.EARTH,8),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("thaumium_hoe")),List.of(shard(4),shard(5),v(Items.DIAMOND),()->TC4Modern.GREATWOOD_LOG_ITEM.get())),
            new Recipe("ElementalSword","ELEMENTALSWORD",()->ElementalTools.item("sword"),1,a(Aspect.AIR,8,Aspect.MOTION,8,Aspect.ENERGY,8),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("thaumium_sword")),List.of(shard(0),shard(0),v(Items.DIAMOND),()->TC4Modern.GREATWOOD_LOG_ITEM.get())),
            new Recipe("ElementalAxe","ELEMENTALAXE",()->ElementalTools.item("axe"),1,a(Aspect.WATER,16,Aspect.TREE,8),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("thaumium_axe")),List.of(shard(2),shard(2),v(Items.DIAMOND),()->TC4Modern.GREATWOOD_LOG_ITEM.get())),
            new Recipe("ElementalPick","ELEMENTALPICK",()->ElementalTools.item("pickaxe"),1,a(Aspect.FIRE,8,Aspect.MINE,8,Aspect.SENSES,8),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("thaumium_pickaxe")),List.of(shard(1),shard(1),v(Items.DIAMOND),()->TC4Modern.GREATWOOD_LOG_ITEM.get())),
            new Recipe("Mirror","MIRROR",()->MagicMirrors.ITEM.get().asItem(),1,a(Aspect.TRAVEL,8,Aspect.DARKNESS,8,Aspect.EXCHANGE,8),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("mirror_glass")),List.of(v(Items.GOLD_INGOT),v(Items.GOLD_INGOT),v(Items.GOLD_INGOT),v(Items.ENDER_PEARL))),
            new Recipe("MirrorHand","MIRRORHAND",HandMirror::item,5,a(Aspect.TOOL,16,Aspect.TRAVEL,16),()->MagicMirrors.ITEM.get().asItem(),List.of(v(Items.STICK),v(Items.COMPASS),v(Items.MAP))),
            new Recipe("MirrorEssentia","MIRRORESSENTIA",()->MagicMirrors.ESSENTIA.get().asItem(),2,a(Aspect.TRAVEL,8,Aspect.WATER,8,Aspect.EXCHANGE,8),()->net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("mirror_glass")),List.of(v(Items.IRON_INGOT),v(Items.IRON_INGOT),v(Items.IRON_INGOT),v(Items.ENDER_PEARL))),
            new Recipe("HoverHarness","HOVERHARNESS",HoverHarness::harness,6,a(Aspect.FLIGHT,32,Aspect.ENERGY,32,Aspect.MECHANISM,32,Aspect.TRAVEL,16),v(Items.LEATHER_CHESTPLATE),List.of(shard(0),shard(0),()->AlchemyContent.GREATWOOD_PLANKS.get().asItem(),()->AlchemyContent.GREATWOOD_PLANKS.get().asItem(),v(Items.COMPARATOR),v(Items.GOLD_INGOT),v(Items.GOLD_INGOT),v(Items.IRON_INGOT),v(Items.IRON_INGOT))),
            new Recipe("HoverGirdle","HOVERGIRDLE",HoverHarness::girdle,8,a(Aspect.FLIGHT,16,Aspect.ENERGY,32,Aspect.AIR,32,Aspect.TRAVEL,16),()->WandPedestal.MUNDANE_BELT.get(),List.of(shard(0),v(Items.FEATHER),v(Items.GOLD_INGOT),shard(3),v(Items.FEATHER),v(Items.GOLD_INGOT))),
            new Recipe("BootsTraveller","BOOTSTRAVELLER",TravellerBoots::item,1,a(Aspect.FLIGHT,25,Aspect.TRAVEL,25),v(Items.LEATHER_BOOTS),List.of(shard(0),shard(0),()->TC4Modern.ENCHANTED_FABRIC.get(),()->TC4Modern.ENCHANTED_FABRIC.get(),v(Items.FEATHER),v(Items.COD))),
            new Recipe("RunicRing","RUNICARMOR",RunicAccessories.Kind.RING::item,3,a(Aspect.ARMOR,10,Aspect.MAGIC,25,Aspect.ENERGY,25),()->WandPedestal.MUNDANE_RING.get(),List.of(()->WandPedestal.PRIMAL_CHARM.get(),()->AlchemyContent.AMBER.get(),()->TC4Modern.ENCHANTED_FABRIC.get(),()->TC4Modern.NITOR.get(),()->TC4Modern.SCRIBING_TOOLS.get())),
            new Recipe("RunicAmulet","RUNICARMOR",RunicAccessories.Kind.AMULET::item,4,a(Aspect.ARMOR,20,Aspect.MAGIC,35,Aspect.ENERGY,35),()->WandPedestal.MUNDANE_AMULET.get(),List.of(()->WandPedestal.PRIMAL_CHARM.get(),()->AlchemyContent.AMBER.get(),()->TC4Modern.ENCHANTED_FABRIC.get(),()->TC4Modern.NITOR.get(),()->TC4Modern.NITOR.get(),()->TC4Modern.SCRIBING_TOOLS.get())),
            new Recipe("RunicGirdle","RUNICARMOR",RunicAccessories.Kind.GIRDLE::item,4,a(Aspect.ARMOR,30,Aspect.MAGIC,50,Aspect.ENERGY,50),()->WandPedestal.MUNDANE_BELT.get(),List.of(()->WandPedestal.PRIMAL_CHARM.get(),()->AlchemyContent.AMBER.get(),()->TC4Modern.ENCHANTED_FABRIC.get(),()->TC4Modern.NITOR.get(),()->TC4Modern.NITOR.get(),()->TC4Modern.NITOR.get(),()->TC4Modern.SCRIBING_TOOLS.get())),
            new Recipe("RunicRingCharged","RUNICCHARGED",RunicAccessories.Kind.CHARGED::item,6,a(Aspect.ARMOR,16,Aspect.MAGIC,16,Aspect.ENERGY,64),RunicAccessories.Kind.RING::item,List.of(shard(6),shard(1),shard(1),v(Items.POTION),shard(1),shard(1))),
            new Recipe("RunicRingHealing","RUNICHEALING",RunicAccessories.Kind.HEALING::item,6,a(Aspect.ARMOR,16,Aspect.MAGIC,16,Aspect.WATER,32,Aspect.HEAL,32),RunicAccessories.Kind.RING::item,List.of(shard(6),shard(2),shard(2),v(Items.POTION),shard(2),shard(2))),
            new Recipe("RunicAmuletEmergency","RUNICEMERGENCY",RunicAccessories.Kind.EMERGENCY::item,7,a(Aspect.ARMOR,20,Aspect.MAGIC,35,Aspect.EARTH,32,Aspect.VOID,32),RunicAccessories.Kind.AMULET::item,List.of(shard(6),shard(3),shard(3),v(Items.POTION),shard(3),shard(3))),
            new Recipe("RunicGirdleKinetic","RUNICKINETIC",RunicAccessories.Kind.KINETIC::item,7,a(Aspect.ARMOR,33,Aspect.MAGIC,55,Aspect.AIR,64),RunicAccessories.Kind.GIRDLE::item,List.of(shard(6),shard(0),shard(0),v(Items.SPLASH_POTION),shard(0),shard(0))),
            new Recipe("LampGrowth","LAMPGROWTH",()->ArcaneLamps.GROWTH.get().asItem(),4,a(Aspect.PLANT,16,Aspect.LIGHT,8,Aspect.LIFE,16),()->ArcaneLamps.LAMP.get().asItem(),List.of(v(Items.GOLD_INGOT),v(Items.BONE_MEAL),shard(3),v(Items.GOLD_INGOT),v(Items.BONE_MEAL),shard(3))),
            new Recipe("LampFertility","LAMPFERTILITY",()->ArcaneLamps.FERTILITY.get().asItem(),4,a(Aspect.BEAST,16,Aspect.LIFE,16,Aspect.LIGHT,8),()->ArcaneLamps.LAMP.get().asItem(),List.of(v(Items.GOLD_INGOT),v(Items.WHEAT),shard(1),v(Items.GOLD_INGOT),v(Items.CARROT),shard(1))),
            new Recipe("SinStone", "SINSTONE", () -> SinisterStone.ITEM.get(), 5, a(Aspect.SENSES,8,Aspect.DARKNESS,8,Aspect.ELDRITCH,8,Aspect.AURA,8), v(Items.FLINT), List.of(() -> TC4Modern.NITOR.get(),shard(4),() -> BasicsContent.KNOWLEDGE_FRAGMENT.get(),shard(5))),
            /* WandCapVoid: inert void cap + 4 salis mundus; potentia/vacuos/alienis/auram each 2*craftCost(9) */
            new Recipe("WandCapVoid", "CAP_void", () -> WandParts.VOID_CAP.get(), 8, a(Aspect.ENERGY, 18, Aspect.VOID, 18, Aspect.ELDRITCH, 18, Aspect.AURA, 18), () -> WandParts.VOID_CAP_INERT.get(), List.of(salis, salis, salis, salis)),
            new Recipe("WandCapThaumium", "CAP_thaumium", () -> WandParts.THAUMIUM_CAP.get(), 5, a(Aspect.ENERGY, cap6 * 2, Aspect.AURA, cap6), () -> WandParts.THAUMIUM_CAP_INERT.get(), List.of(salis, salis, salis)),
            rod("WandRodObsidian", "obsidian", Aspect.EARTH, Aspect.DARKNESS, v(Items.OBSIDIAN), 3),
            rod("WandRodIce", "ice", Aspect.WATER, Aspect.COLD, v(Items.ICE), 2),
            rod("WandRodQuartz", "quartz", Aspect.ORDER, Aspect.CRYSTAL, v(Items.QUARTZ_BLOCK), 4),
            rod("WandRodReed", "reed", Aspect.AIR, Aspect.MOTION, v(Items.SUGAR_CANE), 0),
            rod("WandRodBlaze", "blaze", Aspect.FIRE, Aspect.BEAST, v(Items.BLAZE_ROD), 1),
            rod("WandRodBone", "bone", Aspect.ENTROPY, Aspect.UNDEAD, v(Items.BONE), 5),
            new Recipe("WandRodSilverwood", "ROD_silverwood", () -> WandParts.RODS.get("silverwood").get(), 5,
                a(Aspect.AIR, sw9, Aspect.FIRE, sw9, Aspect.WATER, sw9, Aspect.EARTH, sw9, Aspect.ORDER, sw9, Aspect.ENTROPY, sw9, Aspect.MAGIC, sw9),
                () -> SilverwoodContent.LOG.get().asItem(), List.of(shard(6), shard(0), shard(1), shard(2), shard(3), shard(4), shard(5))),
            new Recipe("NodeStabilizerAdv", "NODESTABILIZERADV", () -> TC4Modern.NODE_STABILIZER_ADV_ITEM.get(), 10,
                a(Aspect.AURA,32,Aspect.MAGIC,16,Aspect.ORDER,16,Aspect.ENERGY,16), () -> TC4Modern.NODE_STABILIZER_ITEM.get(),
                List.of(() -> TC4Modern.NITOR.get(),v(Items.REDSTONE_BLOCK),() -> TC4Modern.ALUMENTUM.get(),v(Items.REDSTONE_BLOCK),() -> TC4Modern.NITOR.get(),v(Items.REDSTONE_BLOCK),() -> TC4Modern.ALUMENTUM.get(),v(Items.REDSTONE_BLOCK))),
            new Recipe("WandRodPrimalStaff", "ROD_primal_staff", () -> WandParts.RODS.get("primal_staff").get(), 8,
                a(Aspect.AIR,32,Aspect.FIRE,32,Aspect.WATER,32,Aspect.EARTH,32,Aspect.ORDER,32,Aspect.ENTROPY,32,Aspect.MAGIC,64),
                () -> WandParts.RODS.get("silverwood").get(), List.of(() -> WandPedestal.PRIMAL_CHARM.get(), () -> WandParts.RODS.get("obsidian").get(), () -> WandParts.RODS.get("ice").get(), () -> WandParts.RODS.get("quartz").get(), () -> WandPedestal.PRIMAL_CHARM.get(), () -> WandParts.RODS.get("reed").get(), () -> WandParts.RODS.get("blaze").get(), () -> WandParts.RODS.get("bone").get())),
            new Recipe("FocusPortableHole", "FOCUSPORTABLEHOLE", () -> FociWorld.PORTABLE_HOLE.get(), 3,
                a(Aspect.TRAVEL, 25, Aspect.ELDRITCH, 10, Aspect.EXCHANGE, 10, Aspect.ENTROPY, 25), v(Items.ENDER_PEARL),
                List.of(v(Items.QUARTZ), shard(3), v(Items.QUARTZ), shard(0), v(Items.QUARTZ), shard(5))),
            new Recipe("FocusWarding", "FOCUSWARDING", () -> Warding.FOCUS.get(), 4,
                a(Aspect.EARTH, 25, Aspect.ARMOR, 25, Aspect.ORDER, 25, Aspect.MIND, 10), v(Items.NETHER_STAR),
                List.of(() -> AlchemyContent.QUICKSILVER.get(), shard(3), v(Items.QUARTZ), shard(4), () -> AlchemyContent.QUICKSILVER.get(), shard(3), v(Items.QUARTZ), shard(4))),
            new Recipe("FocusHellbat", "FOCUSHELLBAT", () -> Hellbat.FOCUS.get(), 3,
                a(Aspect.FIRE, 25, Aspect.AIR, 15, Aspect.BEAST, 15, Aspect.ENTROPY, 25), v(Items.MAGMA_CREAM),
                List.of(v(Items.QUARTZ), shard(1), v(Items.QUARTZ), shard(0), v(Items.QUARTZ), shard(5))),
            new Recipe("WandPed", "WANDPED", () -> WandPedestal.PEDESTAL.get().asItem(), 3, a(Aspect.AURA, 10, Aspect.MAGIC, 15, Aspect.EXCHANGE, 15),
                () -> InfusionContent.PEDESTAL.get().asItem(), List.of(v(Items.GOLD_INGOT), v(Items.DIAMOND), () -> WandPedestal.PRIMAL_CHARM.get(), v(Items.DIAMOND))),
            new Recipe("WandPedFocus", "WANDPEDFOC", () -> WandPedestal.FOCUS.get().asItem(), 4, a(Aspect.ORDER, 10, Aspect.MAGIC, 15, Aspect.EXCHANGE, 10), v(Items.COMPARATOR),
                List.of(shard(3), filter(), shard(3), filter(), shard(3), filter(), shard(3), filter())),
            new Recipe("VisAmulet", "VISAMULET", () -> WandPedestal.VIS_AMULET.get(), 6, a(Aspect.AURA, 24, Aspect.ENERGY, 64, Aspect.MAGIC, 64, Aspect.VOID, 24),
                () -> WandPedestal.MUNDANE_AMULET.get(), List.of(() -> WandPedestal.PRIMAL_CHARM.get(), mixed(), mixed(), () -> WandPedestal.PRIMAL_CHARM.get(), mixed(), mixed()))));
        /* AdvancedGolem: any golem placer (original wildcard metadata) keeps its data and gains advanced. */
        for(var m:GolemMaterial.values())recipes.add(new Recipe("AdvancedGolem","ADVANCEDGOLEM",()->Golemancy.placer(m).get(),3,a(Aspect.MIND,8,Aspect.SENSES,8,Aspect.LIFE,8),()->Golemancy.placer(m).get(),List.of(v(Items.REDSTONE),v(Items.GLOWSTONE_DUST),v(Items.GUNPOWDER),()->EssentiaDevices.JAR.get().asItem(),()->ZombieBrainContent.ITEM.get())));
        /* ConfigRecipes: the three mirror recipes are only added when Config.allowMirrors */
        if (!WarpConfig.allowMirrors()) recipes.removeIf(r -> r.research().startsWith("MIRROR"));
        return recipes;
    }
    /** Elemental rods: primal x2*cost, Praecantatio x cost, secondary aspect at the blaze rod cost; balanced + matching shard. */
    private static Recipe rod(String key, String tag, Aspect primal, Aspect second, Supplier<Item> central, int shardIndex) {
        int c = 6;
        return new Recipe(key, "ROD_" + tag, () -> WandParts.RODS.get(tag).get(), 3, a(primal, c * 2, Aspect.MAGIC, c, second, 6), central, List.of(shard(6), shard(shardIndex)));
    }
    public static Recipe match(List<ItemStack> components, ItemStack central, java.util.Set<String> completed) {
        for (var r : all()) {
            if (!r.research().isEmpty() && !completed.contains(r.research())) continue;
            if (!central.is(r.central().get())) continue;
            if(r.key().startsWith("ThaumiumFortress")&&central.getDamageValue()!=0)continue;
            List<ItemStack> rest = new ArrayList<>(components);
            boolean ok = true;
            for (var comp : r.componentStacks()) {
                int idx = -1;
                for (int i = 0; i < rest.size(); i++) if (thaumcraft.common.tiles.infusion.InfusionMatrixLogic.sameIngredient(rest.get(i),comp)) { idx = i; break; }
                if (idx < 0) { ok = false; break; }
                rest.remove(idx);
            }
            if (ok && rest.isEmpty()) return r;
        }
        return null;
    }
}
