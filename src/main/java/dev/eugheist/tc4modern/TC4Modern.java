package dev.eugheist.tc4modern;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.Item;

@Mod(TC4Modern.MODID)
public class TC4Modern {
    public static final String MODID = "tc4modern";
    public static final DeferredRegister<net.minecraft.world.item.CreativeModeTab> CREATIVE_TABS=DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB,MODID);
    public static final java.util.function.Supplier<net.minecraft.world.item.CreativeModeTab> THAUMCRAFT_TAB=CREATIVE_TABS.register("thaumcraft",()->net.minecraft.world.item.CreativeModeTab.builder()
        .title(net.minecraft.network.chat.Component.translatable("itemGroup.tc4modern.thaumcraft"))
        .icon(()->new net.minecraft.world.item.ItemStack(TC4Modern.THAUMONOMICON.get()))
        .displayItems((parameters,output)->ModCreativeTab.fill(output))
        .build());
    public static final net.neoforged.neoforge.registries.DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final net.neoforged.neoforge.registries.DeferredBlock<GreatwoodSaplingBlock> GREATWOOD_SAPLING=BLOCKS.register("greatwood_sapling",()->new GreatwoodSaplingBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollission().instabreak().sound(net.minecraft.world.level.block.SoundType.GRASS).randomTicks()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<GreatwoodLeavesBlock> GREATWOOD_LEAVES=BLOCKS.register("greatwood_leaves",()->new GreatwoodLeavesBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(.2f).sound(net.minecraft.world.level.block.SoundType.GRASS).noOcclusion().randomTicks()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<GreatwoodLogBlock> GREATWOOD_LOG=BLOCKS.register("greatwood_log",()->new GreatwoodLogBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.5f).sound(net.minecraft.world.level.block.SoundType.WOOD)));
    public static final net.neoforged.neoforge.registries.DeferredBlock<TableBlock> TABLE = BLOCKS.register("table", () -> new TableBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.5f).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<ResearchTableBlock> RESEARCH_TABLE = BLOCKS.register("research_table", () -> new ResearchTableBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.5f).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<AuraNodeBlock> AURA_NODE=BLOCKS.register("aura_node",()->new AuraNodeBlock(NodeBlockEffects.properties()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<VisRelayBlock> VIS_RELAY=BLOCKS.register("vis_relay",()->new VisRelayBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3f,TC4SoundTypes.res(17)).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<NodeConverterBlock> NODE_CONVERTER=BLOCKS.register("node_converter",()->new NodeConverterBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3f,TC4SoundTypes.res(25)).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.STONE).noOcclusion()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<NodeStabilizerBlock> NODE_STABILIZER=BLOCKS.register("node_stabilizer",()->new NodeStabilizerBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3f,TC4SoundTypes.res(25)).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.STONE).noOcclusion()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<NodeStabilizerBlock> NODE_STABILIZER_ADV=BLOCKS.register("node_stabilizer_advanced",()->new NodeStabilizerBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3f,TC4SoundTypes.res(25)).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.STONE).noOcclusion()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<EnergizedNodeBlock> ENERGIZED_NODE=BLOCKS.register("energized_node",()->new EnergizedNodeBlock(NodeBlockEffects.properties()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<ArcaneWorkbenchBlock> ARCANE_WORKBENCH=BLOCKS.register("arcane_workbench",()->new ArcaneWorkbenchBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.5f).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.Block> ARCANE_STONE=BLOCKS.register("arcane_stone",()->new net.minecraft.world.level.block.Block(stoneProperties().strength(4,20)));
    public static final net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.Block> ARCANE_STONE_BRICKS=BLOCKS.register("arcane_stone_bricks",()->new net.minecraft.world.level.block.Block(stoneProperties().strength(4,20)));
    public static final net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.StairBlock> ARCANE_STONE_STAIRS=BLOCKS.register("arcane_stone_stairs",()->new net.minecraft.world.level.block.StairBlock(ARCANE_STONE_BRICKS.get().defaultBlockState(),stoneProperties()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.SlabBlock> ARCANE_STONE_SLAB=BLOCKS.register("arcane_stone_slab",()->new net.minecraft.world.level.block.SlabBlock(stoneProperties()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<NitorBlock> NITOR_BLOCK=BLOCKS.register("nitor",()->new NitorBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().lightLevel(state->15).noCollission().noOcclusion().sound(net.minecraft.world.level.block.SoundType.EMPTY)));
    public static final net.neoforged.neoforge.registries.DeferredBlock<InfernalFurnaceBlock> INFERNAL_FURNACE=BLOCKS.register("infernal_furnace",()->new InfernalFurnaceBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(10,TC4SoundTypes.res(500)).requiresCorrectToolForDrops().noOcclusion().lightLevel(state->state.getValue(InfernalFurnaceBlock.PART)==0||state.getValue(InfernalFurnaceBlock.PART)==10?13:3)));
    public static final net.neoforged.neoforge.registries.DeferredBlock<BellowsBlock> BELLOWS=BLOCKS.register("bellows",()->new BellowsBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2.5f,TC4SoundTypes.res(10)).sound(net.minecraft.world.level.block.SoundType.WOOD).noOcclusion()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<CrucibleBlock> CRUCIBLE=BLOCKS.register("crucible",()->new CrucibleBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(3,TC4SoundTypes.res(17)).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.METAL).noOcclusion()));
    public static final net.neoforged.neoforge.registries.DeferredBlock<FluxBlock> FLUX_GAS=BLOCKS.register("flux_gas",()->new FluxBlock(fluxProperties(),1));
    public static final net.neoforged.neoforge.registries.DeferredBlock<FluxBlock> FLUX_GOO=BLOCKS.register("flux_goo",()->new FluxBlock(fluxProperties().sound(TC4SoundTypes.GORE),-1));
    public static final net.neoforged.neoforge.registries.DeferredBlock<TaintFibresBlock> TAINT_FIBRES=BLOCKS.register("taint_fibres",()->new TaintFibresBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1,TC4SoundTypes.res(5)).sound(TC4SoundTypes.TAINT).noCollission().noOcclusion().noLootTable().randomTicks().lightLevel(state->state.getValue(TaintFibresBlock.VARIANT)==2?8:state.getValue(TaintFibresBlock.VARIANT)==4?10:0)));
    public static final net.neoforged.neoforge.registries.DeferredBlock<TaintBlock> TAINT=BLOCKS.register("taint",()->new TaintBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2,TC4SoundTypes.res(10)).sound(TC4SoundTypes.TAINT).randomTicks()));
    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties fluxProperties(){return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().noCollission().noOcclusion().noLootTable().strength(100).randomTicks().lightLevel(state->(state.getValue(FluxBlock.AMOUNT)-1)*7/8);}
    private static net.minecraft.world.level.block.state.BlockBehaviour.Properties stoneProperties(){return net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(2,TC4SoundTypes.res(10)).sound(net.minecraft.world.level.block.SoundType.STONE).requiresCorrectToolForDrops();}
    public static final DeferredRegister<net.minecraft.world.entity.EntityType<?>> ENTITIES=DeferredRegister.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,MODID);
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<thaumcraft.common.entities.EntitySpecialItem>> SPECIAL_ITEM=ENTITIES.register("special_item",()->net.minecraft.world.entity.EntityType.Builder.<thaumcraft.common.entities.EntitySpecialItem>of(thaumcraft.common.entities.EntitySpecialItem::new,net.minecraft.world.entity.MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(6).updateInterval(20).build("tc4modern:special_item"));
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<thaumcraft.common.entities.projectile.EntityAlumentum>> ALUMENTUM_PROJECTILE=ENTITIES.register("alumentum",()->net.minecraft.world.entity.EntityType.Builder.<thaumcraft.common.entities.projectile.EntityAlumentum>of(thaumcraft.common.entities.projectile.EntityAlumentum::new,net.minecraft.world.entity.MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(10).build("tc4modern:alumentum"));
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<thaumcraft.common.entities.monster.EntityThaumicSlime>> THAUMIC_SLIME=ENTITIES.register("thaumic_slime",()->net.minecraft.world.entity.EntityType.Builder.<thaumcraft.common.entities.monster.EntityThaumicSlime>of(thaumcraft.common.entities.monster.EntityThaumicSlime::new,net.minecraft.world.entity.MobCategory.MONSTER).sized(.5f,.5f).clientTrackingRange(8).updateInterval(3).build("tc4modern:thaumic_slime"));
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<thaumcraft.common.entities.monster.EntityTaintSpider>> TAINT_SPIDER=ENTITIES.register("taint_spider",()->net.minecraft.world.entity.EntityType.Builder.<thaumcraft.common.entities.monster.EntityTaintSpider>of(thaumcraft.common.entities.monster.EntityTaintSpider::new,net.minecraft.world.entity.MobCategory.MONSTER).sized(.4f,.3f).clientTrackingRange(8).updateInterval(3).build("tc4modern:taint_spider"));
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<thaumcraft.common.entities.EntityFallingTaint>> FALLING_TAINT=ENTITIES.register("falling_taint",()->net.minecraft.world.entity.EntityType.Builder.<thaumcraft.common.entities.EntityFallingTaint>of(thaumcraft.common.entities.EntityFallingTaint::new,net.minecraft.world.entity.MobCategory.MISC).sized(.98f,.98f).clientTrackingRange(10).updateInterval(20).build("tc4modern:falling_taint"));
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<thaumcraft.common.entities.monster.EntityTaintSpore>> TAINT_SPORE=ENTITIES.register("taint_spore",()->net.minecraft.world.entity.EntityType.Builder.<thaumcraft.common.entities.monster.EntityTaintSpore>of(thaumcraft.common.entities.monster.EntityTaintSpore::new,net.minecraft.world.entity.MobCategory.MONSTER).sized(.5f,.5f).clientTrackingRange(8).updateInterval(3).build("tc4modern:taint_spore"));
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<thaumcraft.common.entities.monster.EntityTaintSwarm>> TAINT_SWARM=ENTITIES.register("taint_swarm",()->net.minecraft.world.entity.EntityType.Builder.<thaumcraft.common.entities.monster.EntityTaintSwarm>of(thaumcraft.common.entities.monster.EntityTaintSwarm::new,net.minecraft.world.entity.MobCategory.MONSTER).sized(2,2).clientTrackingRange(8).updateInterval(3).build("tc4modern:taint_swarm"));
    public static final java.util.function.Supplier<net.minecraft.world.entity.EntityType<thaumcraft.common.entities.monster.EntityTaintSporeSwarmer>> TAINT_SWARMER=ENTITIES.register("taint_swarmer",()->net.minecraft.world.entity.EntityType.Builder.<thaumcraft.common.entities.monster.EntityTaintSporeSwarmer>of(thaumcraft.common.entities.monster.EntityTaintSporeSwarmer::new,net.minecraft.world.entity.MobCategory.MONSTER).sized(1,1).clientTrackingRange(8).updateInterval(3).build("tc4modern:taint_swarmer"));
    public static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(net.minecraft.core.registries.Registries.MENU, MODID);
    public static final java.util.function.Supplier<net.minecraft.world.inventory.MenuType<ResearchTableMenu>> RESEARCH_MENU = MENUS.register("research_table", () -> new net.minecraft.world.inventory.MenuType<>(ResearchTableMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    public static final java.util.function.Supplier<net.minecraft.world.inventory.MenuType<ThaumonomiconMenu>> BOOK_MENU = MENUS.register("thaumonomicon", () -> new net.minecraft.world.inventory.MenuType<>(ThaumonomiconMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    public static final java.util.function.Supplier<net.minecraft.world.inventory.MenuType<ArcaneWorkbenchMenu>> ARCANE_MENU=MENUS.register("arcane_workbench",()->new net.minecraft.world.inventory.MenuType<>(ArcaneWorkbenchMenu::new,net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    public static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<NitorEntity>> NITOR_ENTITY=BLOCK_ENTITIES.register("nitor",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(NitorEntity::new,NITOR_BLOCK.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<InfernalFurnaceEntity>> INFERNAL_ENTITY=BLOCK_ENTITIES.register("infernal_furnace",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(InfernalFurnaceEntity::new,INFERNAL_FURNACE.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<BellowsEntity>> BELLOWS_ENTITY=BLOCK_ENTITIES.register("bellows",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(BellowsEntity::new,BELLOWS.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<CrucibleEntity>> CRUCIBLE_ENTITY=BLOCK_ENTITIES.register("crucible",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(CrucibleEntity::new,CRUCIBLE.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<ResearchTableEntity>> RESEARCH_TABLE_ENTITY = BLOCK_ENTITIES.register("research_table", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(ResearchTableEntity::new, RESEARCH_TABLE.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<AuraNodeEntity>> AURA_NODE_ENTITY=BLOCK_ENTITIES.register("aura_node",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(AuraNodeEntity::new,AURA_NODE.get(),ObsidianTotems.CHARGED.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<VisRelayEntity>> VIS_RELAY_ENTITY=BLOCK_ENTITIES.register("vis_relay",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of((pos,state)->new VisRelayEntity(VIS_RELAY_ENTITY_TYPE(),pos,state),VIS_RELAY.get()).build(null));
    private static net.minecraft.world.level.block.entity.BlockEntityType<VisRelayEntity> VIS_RELAY_ENTITY_TYPE(){return VIS_RELAY_ENTITY.get();}
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<NodeConverterEntity>> NODE_CONVERTER_ENTITY=BLOCK_ENTITIES.register("node_converter",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(NodeConverterEntity::new,NODE_CONVERTER.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<NodeStabilizerEntity>> NODE_STABILIZER_ENTITY=BLOCK_ENTITIES.register("node_stabilizer",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(NodeStabilizerEntity::new,NODE_STABILIZER.get(),NODE_STABILIZER_ADV.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<EnergizedNodeEntity>> ENERGIZED_NODE_ENTITY=BLOCK_ENTITIES.register("energized_node",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(EnergizedNodeEntity::new,ENERGIZED_NODE.get()).build(null));
    public static final java.util.function.Supplier<net.minecraft.world.level.block.entity.BlockEntityType<ArcaneWorkbenchEntity>> ARCANE_ENTITY=BLOCK_ENTITIES.register("arcane_workbench",()->net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(ArcaneWorkbenchEntity::new,ARCANE_WORKBENCH.get()).build(null));
    public static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES=DeferredRegister.create(net.minecraft.core.registries.Registries.FEATURE,MODID);
    public static final java.util.function.Supplier<GreatwoodFeature> GREATWOOD_FEATURE=FEATURES.register("greatwood",GreatwoodFeature::new);
    public static final java.util.function.Supplier<NaturalGreatwoodFeature> NATURAL_GREATWOOD_FEATURE=FEATURES.register("natural_greatwood",NaturalGreatwoodFeature::new);
    public static final java.util.function.Supplier<ShardOreFeature> ORE_FEATURE=FEATURES.register("infused_stone",ShardOreFeature::new);
    public static final java.util.function.Supplier<AuraNodeFeature> NODE_FEATURE=FEATURES.register("aura_node",AuraNodeFeature::new);
    public static final DeferredRegister<net.minecraft.world.item.ArmorMaterial> ARMOR_MATERIALS=DeferredRegister.create(net.minecraft.core.registries.Registries.ARMOR_MATERIAL,MODID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.ArmorMaterial,net.minecraft.world.item.ArmorMaterial> GOGGLES_MATERIAL=ARMOR_MATERIALS.register("goggles",()->new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(net.minecraft.world.item.ArmorItem.Type.HELMET,thaumcraft.common.items.armor.ItemGoggles.HEAD_DEFENSE),thaumcraft.common.items.armor.ItemGoggles.ENCHANTMENT_VALUE,net.minecraft.core.Holder.direct(net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER.value()),()->net.minecraft.world.item.crafting.Ingredient.of(net.minecraft.world.item.Items.GOLD_INGOT),java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"goggles"))),0,0));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.ArmorMaterial,net.minecraft.world.item.ArmorMaterial> SPECIAL_MATERIAL=ARMOR_MATERIALS.register("special",()->new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(net.minecraft.world.item.ArmorItem.Type.HELMET,1,net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,2,net.minecraft.world.item.ArmorItem.Type.LEGGINGS,3,net.minecraft.world.item.ArmorItem.Type.BOOTS,1),25,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,()->net.minecraft.world.item.crafting.Ingredient.EMPTY,java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"special"))),0,0));
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.item.ArmorMaterial,net.minecraft.world.item.ArmorMaterial> ROBE_MATERIAL=ARMOR_MATERIALS.register("robe",()->new net.minecraft.world.item.ArmorMaterial(java.util.Map.of(net.minecraft.world.item.ArmorItem.Type.CHESTPLATE,2,net.minecraft.world.item.ArmorItem.Type.LEGGINGS,3,net.minecraft.world.item.ArmorItem.Type.BOOTS,1),25,net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER,()->net.minecraft.world.item.crafting.Ingredient.of(TC4Modern.ENCHANTED_FABRIC.get()),java.util.List.of(new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"robes"),"",true),new net.minecraft.world.item.ArmorMaterial.Layer(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"robes"),"_overlay",false)),0,0));
    public static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS=DeferredRegister.create(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER,MODID);
    public static final java.util.function.Supplier<net.minecraft.world.item.crafting.RecipeSerializer<thaumcraft.common.items.armor.RecipesRobeArmorDyes>> ROBE_DYE_SERIALIZER=RECIPE_SERIALIZERS.register("robe_dye",()->new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(thaumcraft.common.items.armor.RecipesRobeArmorDyes::new));
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredItem<ThaumometerItem> THAUMOMETER = ITEMS.register("thaumometer",
        () -> new ThaumometerItem(new Item.Properties().stacksTo(1)));
    // No research table item: like the original it only exists as two joined tables (ItemInkwell) and breaks back into tables.
    public static final DeferredItem<ResonatorItem> RESONATOR = ITEMS.register("resonator", () -> new ResonatorItem(new Item.Properties()));
    public static final DeferredItem<ScribingToolsItem> SCRIBING_TOOLS = ITEMS.register("scribing_tools", () -> new ScribingToolsItem(new Item.Properties().durability(100)));
    public static final DeferredItem<ResearchNotesItem> RESEARCH_NOTES = ITEMS.register("research_notes", () -> new ResearchNotesItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<ThaumonomiconItem> THAUMONOMICON = ITEMS.register("thaumonomicon", () -> new ThaumonomiconItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<ThaumonomiconItem> THAUMONOMICON_CHEAT = ITEMS.register("thaumonomicon_cheat", () -> new ThaumonomiconItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.EPIC),true));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> TABLE_ITEM = ITEMS.registerSimpleBlockItem("table",TABLE);
    /** blockTable:15 - the arcane worktable item: in the creative tab and dropped by the worktable (damageDropped 15). */
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ARCANE_WORKBENCH_ITEM = ITEMS.registerSimpleBlockItem("arcane_workbench",ARCANE_WORKBENCH);
    public static final DeferredItem<Item> IRON_CAP = ITEMS.register("iron_cap", () -> new Item(new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> GREATWOOD_LOG_ITEM=ITEMS.registerSimpleBlockItem("greatwood_log",GREATWOOD_LOG);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> GREATWOOD_SAPLING_ITEM=ITEMS.registerSimpleBlockItem("greatwood_sapling",GREATWOOD_SAPLING);
    public static final DeferredItem<Item> GREATWOOD_ROD=ITEMS.register("greatwood_rod",()->new Item(new Item.Properties()));
    public static final DeferredItem<Item> GOLD_CAP = ITEMS.register("gold_cap", () -> new Item(new Item.Properties()));
    public static final DeferredItem<BasicWandItem> BASIC_WAND = ITEMS.register("iron_capped_wooden_wand", () -> new BasicWandItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<BasicWandItem> GOLD_GREATWOOD_WAND=ITEMS.register("gold_capped_greatwood_wand",()->new BasicWandItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON),5000,1.0f));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> VIS_RELAY_ITEM=ITEMS.registerSimpleBlockItem("vis_relay",VIS_RELAY);
    public static final DeferredItem<AuraNodeItem> AURA_NODE_ITEM=ITEMS.register("aura_node",()->new AuraNodeItem(AURA_NODE.get(),new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> NODE_STABILIZER_ITEM=ITEMS.registerSimpleBlockItem("node_stabilizer",NODE_STABILIZER);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> NODE_STABILIZER_ADV_ITEM=ITEMS.registerSimpleBlockItem("node_stabilizer_advanced",NODE_STABILIZER_ADV);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> NODE_CONVERTER_ITEM=ITEMS.registerSimpleBlockItem("node_converter",NODE_CONVERTER);
    public static final DeferredItem<BellowsItem> BELLOWS_ITEM=ITEMS.register("bellows",()->new BellowsItem(BELLOWS.get(),new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> CRUCIBLE_ITEM=ITEMS.registerSimpleBlockItem("crucible",CRUCIBLE);
    public static final DeferredItem<thaumcraft.common.items.ItemAlumentum> ALUMENTUM=ITEMS.register("alumentum",()->new thaumcraft.common.items.ItemAlumentum(new Item.Properties()));
    public static final DeferredItem<thaumcraft.common.items.ItemNitor> NITOR=ITEMS.register("nitor",()->new thaumcraft.common.items.ItemNitor(NITOR_BLOCK.get(),new Item.Properties()));
    public static final DeferredItem<thaumcraft.common.items.ItemNuggetEdible> CHICKEN_NUGGET=ITEMS.register("chicken_nugget",()->new thaumcraft.common.items.ItemNuggetEdible("nuggetchicken"));
    public static final DeferredItem<thaumcraft.common.items.ItemNuggetEdible> BEEF_NUGGET=ITEMS.register("beef_nugget",()->new thaumcraft.common.items.ItemNuggetEdible("nuggetbeef"));
    public static final DeferredItem<thaumcraft.common.items.ItemNuggetEdible> PORK_NUGGET=ITEMS.register("pork_nugget",()->new thaumcraft.common.items.ItemNuggetEdible("nuggetpork"));
    public static final DeferredItem<thaumcraft.common.items.ItemNuggetEdible> FISH_NUGGET=ITEMS.register("fish_nugget",()->new thaumcraft.common.items.ItemNuggetEdible("nuggetfish"));
    public static final DeferredItem<thaumcraft.common.items.ItemTripleMeatTreat> TRIPLE_MEAT_TREAT=ITEMS.register("triple_meat_treat",thaumcraft.common.items.ItemTripleMeatTreat::new);
    public static final DeferredItem<Item> THAUMIUM=ITEMS.register("thaumium_ingot",()->new Item(new Item.Properties()));
    public static final DeferredItem<Item> TALLOW=ITEMS.register("tallow",()->new Item(new Item.Properties()));
    public static final DeferredItem<Item> BALANCED_SHARD=ITEMS.register("balanced_shard",()->new Item(new Item.Properties()));
    public static final DeferredItem<Item> ENCHANTED_FABRIC=ITEMS.register("enchanted_fabric",()->new Item(new Item.Properties()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ARCANE_STONE_ITEM=ITEMS.registerSimpleBlockItem("arcane_stone",ARCANE_STONE);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ARCANE_STONE_BRICKS_ITEM=ITEMS.registerSimpleBlockItem("arcane_stone_bricks",ARCANE_STONE_BRICKS);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ARCANE_STONE_STAIRS_ITEM=ITEMS.registerSimpleBlockItem("arcane_stone_stairs",ARCANE_STONE_STAIRS);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ARCANE_STONE_SLAB_ITEM=ITEMS.registerSimpleBlockItem("arcane_stone_slab",ARCANE_STONE_SLAB);
    public static final DeferredItem<thaumcraft.common.items.armor.ItemGoggles> GOGGLES=ITEMS.register("goggles",()->new thaumcraft.common.items.armor.ItemGoggles(GOGGLES_MATERIAL));
    public static final DeferredItem<thaumcraft.common.items.armor.ItemRobeArmor> ROBE_CHEST=ITEMS.register("robe_chest",()->new thaumcraft.common.items.armor.ItemRobeArmor(ROBE_MATERIAL,net.minecraft.world.item.ArmorItem.Type.CHESTPLATE));
    public static final DeferredItem<thaumcraft.common.items.armor.ItemRobeArmor> ROBE_LEGS=ITEMS.register("robe_legs",()->new thaumcraft.common.items.armor.ItemRobeArmor(ROBE_MATERIAL,net.minecraft.world.item.ArmorItem.Type.LEGGINGS));
    public static final DeferredItem<thaumcraft.common.items.armor.ItemRobeArmor> ROBE_BOOTS=ITEMS.register("robe_boots",()->new thaumcraft.common.items.armor.ItemRobeArmor(ROBE_MATERIAL,net.minecraft.world.item.ArmorItem.Type.BOOTS));
    public static final String[] SHARD_NAMES={"air","fire","water","earth","order","entropy"};
    public static final java.util.List<DeferredItem<Item>> SHARDS=new java.util.ArrayList<>();
    public static final java.util.List<net.neoforged.neoforge.registries.DeferredBlock<ShardOreBlock>> SHARD_ORES=new java.util.ArrayList<>();
    public static final java.util.List<net.neoforged.neoforge.registries.DeferredItem<net.minecraft.world.item.BlockItem>> SHARD_ORE_ITEMS=new java.util.ArrayList<>();
    /** tc4modern: deepslate infused stones (same variants/drops) for the 1.21 depth range. */
    public static final java.util.List<net.neoforged.neoforge.registries.DeferredBlock<ShardOreBlock>> DEEPSLATE_SHARD_ORES=new java.util.ArrayList<>();
    static {
        for(int i=0;i<SHARD_NAMES.length;i++){
            int variant=i;String name=SHARD_NAMES[i];
            SHARDS.add(ITEMS.register(name+"_shard",()->new Item(new Item.Properties())));
            var ore=BLOCKS.register(name+"_infused_stone",()->new ShardOreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().strength(1.5f,TC4SoundTypes.res(5)).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.STONE),variant));
            SHARD_ORES.add(ore);SHARD_ORE_ITEMS.add(ITEMS.registerSimpleBlockItem(name+"_infused_stone",ore));
            var deep=BLOCKS.register("deepslate_"+name+"_infused_stone",()->new ShardOreBlock(net.minecraft.world.level.block.state.BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.DEEPSLATE).strength(3f,TC4SoundTypes.res(5)).requiresCorrectToolForDrops().sound(net.minecraft.world.level.block.SoundType.DEEPSLATE),variant));
            DEEPSLATE_SHARD_ORES.add(deep);ITEMS.registerSimpleBlockItem("deepslate_"+name+"_infused_stone",deep);
        }
    }
    public static final DeferredRegister<net.minecraft.world.effect.MobEffect> EFFECTS=DeferredRegister.create(net.minecraft.core.registries.Registries.MOB_EFFECT,MODID);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect,thaumcraft.api.potions.PotionVisExhaust> VIS_EXHAUST=EFFECTS.register("vis_exhaust",thaumcraft.api.potions.PotionVisExhaust::new);
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.effect.MobEffect,thaumcraft.api.potions.PotionFluxTaint> FLUX_TAINT=EFFECTS.register("flux_taint",thaumcraft.api.potions.PotionFluxTaint::new);
    public static final net.minecraft.resources.ResourceKey<net.minecraft.world.damagesource.DamageType> TAINT_DAMAGE=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"taint"));
    public static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS=DeferredRegister.create(net.minecraft.core.registries.Registries.SOUND_EVENT,MODID);
    public static final java.util.function.Supplier<net.minecraft.sounds.SoundEvent> BUBBLE=SOUNDS.register("bubble",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"bubble")));
    public static final java.util.function.Supplier<net.minecraft.sounds.SoundEvent> SPILL=SOUNDS.register("spill",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"spill")));
    public static final DeferredItem<Item> TAINTED_GOO=ITEMS.register("tainted_goo",()->new Item(new Item.Properties()));
    public static final java.util.function.Supplier<net.minecraft.sounds.SoundEvent> GORE=SOUNDS.register("gore",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"gore")));
    public static final DeferredItem<Item> TAINTED_TENDRIL=ITEMS.register("tainted_tendril",()->new Item(new Item.Properties()));
    public static final java.util.function.Supplier<net.minecraft.sounds.SoundEvent> SWARM=SOUNDS.register("swarm",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"swarm")));
    public static final java.util.function.Supplier<net.minecraft.sounds.SoundEvent> ROOTS=SOUNDS.register("roots",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"roots")));
    public static final java.util.function.Supplier<net.minecraft.sounds.SoundEvent> SWARM_ATTACK=SOUNDS.register("swarmattack",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"swarmattack")));
    public static final java.util.function.Supplier<net.minecraft.sounds.SoundEvent> FLY=SOUNDS.register("fly",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"fly")));
    public static final java.util.function.Supplier<net.minecraft.sounds.SoundEvent> LEARN=SOUNDS.register("learn",()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MODID,"learn")));
    public TC4Modern(IEventBus bus, net.neoforged.fml.ModContainer container) {
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.COMMON, WarpConfig.SPEC);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.CLIENT, WarpClientConfig.SPEC);
        SOUNDS.register(bus);
        AspectContainerHosts.register();
        thaumcraft.common.entities.monster.EntityTaintSwarm.goo=TAINTED_GOO;
        thaumcraft.common.entities.monster.EntityTaintSwarm.recentlyHit=target->target.lastHurtByPlayerTime=100;
        thaumcraft.common.entities.monster.EntityTaintSwarm.attackSound=SWARM_ATTACK;
        thaumcraft.common.entities.monster.EntityTaintSwarm.taintedBiome=(level,pos)->level.getBiome(pos).is(TaintBiomes.TAINTED_LAND);
        thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.roots=ROOTS;
        thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.pushOut=LegacyBlockEscapeBridge::escape;
        thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.swarm=level->new thaumcraft.common.entities.monster.EntityTaintSwarm(TAINT_SWARM.get(),level);
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)->event.put(TAINT_SWARM.get(),thaumcraft.common.entities.monster.EntityTaintSwarm.attributes().build()));
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)->event.put(TAINT_SWARMER.get(),thaumcraft.common.entities.monster.EntityTaintSporeSwarmer.attributes().build()));
        thaumcraft.api.potions.PotionFluxTaint.taintDamage=target->new net.minecraft.world.damagesource.DamageSource(target.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(TAINT_DAMAGE));
        thaumcraft.common.entities.monster.EntityTaintSpore.goo=TAINTED_GOO;
        thaumcraft.common.entities.monster.EntityTaintSpore.tendril=TAINTED_TENDRIL;
        thaumcraft.common.entities.monster.EntityTaintSpore.gore=GORE;
        thaumcraft.common.entities.EntityFallingTaint.world=TaintFallingBridge::new;
        thaumcraft.common.entities.EntityFallingTaint.state=metadata->TAINT.get().defaultBlockState().setValue(TaintBlock.VARIANT,metadata);
        thaumcraft.common.entities.monster.EntityTaintSpore.swarmSound=SWARM;
        thaumcraft.common.entities.monster.EntityTaintSpore.spiderling=level->new thaumcraft.common.entities.monster.EntityTaintSpider(TAINT_SPIDER.get(),level);
        thaumcraft.common.entities.monster.EntityTaintSpore.supportingStalk=(level,pos)->{var state=level.getBlockState(pos);return state.is(TAINT_FIBRES.get())&&state.getValue(TaintFibresBlock.VARIANT)==4;};
        thaumcraft.common.entities.monster.EntityTaintSpore.resetStalk=(level,pos)->level.setBlock(pos,level.getBlockState(pos).setValue(TaintFibresBlock.VARIANT,3),3);
        thaumcraft.common.entities.monster.EntityTaintSpore.taintedBiome=(level,pos)->level.getBiome(pos).is(TaintBiomes.TAINTED_LAND);
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)->event.put(TAINT_SPORE.get(),thaumcraft.common.entities.monster.EntityTaintSpore.attributes().build()));
        thaumcraft.common.entities.monster.EntityTaintSpider.goo=TAINTED_GOO;
        thaumcraft.common.entities.monster.EntityTaintSpider.tendril=TAINTED_TENDRIL;
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)->event.put(TAINT_SPIDER.get(),thaumcraft.common.entities.monster.EntityTaintSpider.attributes().build()));
        thaumcraft.common.entities.monster.EntityThaumicSlime.taintedGoo=TAINTED_GOO;
        thaumcraft.common.entities.monster.EntityThaumicSlime.gore=GORE;
        bus.addListener((net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event)->event.put(THAUMIC_SLIME.get(),net.minecraft.world.entity.monster.Monster.createMonsterAttributes().build()));
        thaumcraft.common.items.ItemAlumentum.projectile=(level,player)->new thaumcraft.common.entities.projectile.EntityAlumentum(ALUMENTUM_PROJECTILE.get(),player,level);
        WarpEffects.bootstrap();
        EFFECTS.register(bus);
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event)->event.registerBlockEntity(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,CRUCIBLE_ENTITY.get(),(tile,side)->tile.fluid));
        ENTITIES.register(bus);
        ARMOR_MATERIALS.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        ITEMS.register(bus);
        CreativeBlocks.ITEMS.register(bus);
        FEATURES.register(bus);
        BLOCKS.register(bus);
        MENUS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener((net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) ->
            event.registrar("1").playToClient(ScanPayload.TYPE, ScanPayload.CODEC,
                (payload, context) -> ClientKnowledge.receive(payload))
                .playToClient(ResearchCluePayload.TYPE, ResearchCluePayload.CODEC,
                    (payload, context) -> ClientKnowledge.receiveClue(payload))
                .playToServer(InventoryScanPayload.TYPE, InventoryScanPayload.CODEC, InventoryScanPayload::handle)
                .playToServer(CombinePayload.TYPE, CombinePayload.CODEC, CombinePayload::handle)
                .playToServer(PlaceAspectPayload.TYPE, PlaceAspectPayload.CODEC, PlaceAspectPayload::handle)
                .playToServer(ResearchRequestPayload.TYPE, ResearchRequestPayload.CODEC, ResearchRequestPayload::handle));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) ThaumometerItem.sync(player, "");
        });
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player) ThaumometerItem.sync(player, "");
        });
        CREATIVE_TABS.register(bus);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload event)->{
            if(event.getLevel() instanceof net.minecraft.server.level.ServerLevel level)VisNetwork.unload(level);
        });
        NeoForge.EVENT_BUS.addListener(this::clonePlayer);
    }
    private void clonePlayer(PlayerEvent.Clone event) {
        ResearchKnowledge.copy(event.getOriginal().getPersistentData(), event.getEntity().getPersistentData());
    }
}
