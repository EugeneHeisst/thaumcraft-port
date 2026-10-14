package dev.eugheist.tc4modern;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Registration of the distillation slice: alchemical furnace, alembic, warded jar, phials, label, filter. */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class EssentiaDevices {
    private EssentiaDevices() {}
    private static DeferredHolder<Block, Block> block(String n) { return DeferredHolder.create(Registries.BLOCK, AlchemyContent.id(n)); }
    private static DeferredHolder<Item, Item> item(String n) { return DeferredHolder.create(Registries.ITEM, AlchemyContent.id(n)); }
    public static final DeferredHolder<Block, Block> FURNACE = block("alchemical_furnace");
    public static final DeferredHolder<Block, Block> ALEMBIC = block("alembic");
    public static final DeferredHolder<Block, Block> JAR = block("warded_jar");
    public static final DeferredHolder<Item, Item> PHIAL = item("phial");
    public static final DeferredHolder<Item, Item> ESSENTIA_PHIAL = item("essentia_phial");
    public static final DeferredHolder<Item, Item> LABEL = item("jar_label");
    public static final DeferredHolder<Item, Item> FILTER = item("vis_filter");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlchemicalFurnaceEntity>> FURNACE_ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("alchemical_furnace"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlembicEntity>> ALEMBIC_ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("alembic"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WardedJarEntity>> JAR_ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("warded_jar"));
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<LabelRecipes.Aspect>> LABEL_ASPECT_SERIALIZER = DeferredHolder.create(Registries.RECIPE_SERIALIZER, AlchemyContent.id("jar_label_aspect"));
    public static final DeferredHolder<net.minecraft.world.item.crafting.RecipeSerializer<?>, net.minecraft.world.item.crafting.RecipeSerializer<LabelRecipes.Clear>> LABEL_CLEAR_SERIALIZER = DeferredHolder.create(Registries.RECIPE_SERIALIZER, AlchemyContent.id("jar_label_clear"));
    public static final DeferredHolder<MenuType<?>, MenuType<AlchemicalFurnaceMenu>> FURNACE_MENU = DeferredHolder.create(Registries.MENU, AlchemyContent.id("alchemical_furnace"));

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, h -> {
            // BlockStoneDevice: hardness 3, resistance 25, stone; burning light 12.
            h.register(FURNACE.getId(), new AlchemicalFurnaceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, TC4SoundTypes.res(25)).sound(SoundType.STONE).requiresCorrectToolForDrops()
                .lightLevel(s -> s.getValue(AlchemicalFurnaceBlock.LIT) ? 12 : 0)));
            // BlockMetalDevice: hardness 3, resistance 17, metal.
            h.register(ALEMBIC.getId(), new AlembicBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3f, TC4SoundTypes.res(17)).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
            // BlockJar: hardness 0.3, custom jar step sound.
            h.register(JAR.getId(), new WardedJarBlock(BlockBehaviour.Properties.of().strength(.3f).sound(TC4SoundTypes.JAR).lightLevel(s -> 9).noOcclusion()));
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, h -> {
            h.register(FURNACE_ENTITY.getId(), BlockEntityType.Builder.of(AlchemicalFurnaceEntity::new, FURNACE.get()).build(null));
            h.register(ALEMBIC_ENTITY.getId(), BlockEntityType.Builder.of(AlembicEntity::new, ALEMBIC.get()).build(null));
            h.register(JAR_ENTITY.getId(), BlockEntityType.Builder.of(WardedJarEntity::new, JAR.get(), AlchemyMachines.VOID_JAR.get()).build(null));
        });
        event.register(Registries.RECIPE_SERIALIZER, h -> {
            h.register(LABEL_ASPECT_SERIALIZER.getId(), new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(LabelRecipes.Aspect::new));
            h.register(LABEL_CLEAR_SERIALIZER.getId(), new net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer<>(LabelRecipes.Clear::new));
        });
        event.register(Registries.MENU, h -> h.register(FURNACE_MENU.getId(), new MenuType<>(AlchemicalFurnaceMenu::new, FeatureFlags.VANILLA_SET)));
        event.register(Registries.ITEM, h -> {
            h.register(FURNACE.getId(), new BlockItem(FURNACE.get(), new Item.Properties()));
            h.register(ALEMBIC.getId(), new BlockItem(ALEMBIC.get(), new Item.Properties()));
            h.register(JAR.getId(), new WardedJarBlock.JarItem(JAR.get(), new Item.Properties()));
            h.register(PHIAL.getId(), new EssentiaItems.Phial(new Item.Properties(), false));
            h.register(ESSENTIA_PHIAL.getId(), new EssentiaItems.Phial(new Item.Properties(), true));
            h.register(LABEL.getId(), new EssentiaItems.Label(new Item.Properties()));
            h.register(FILTER.getId(), new Item(new Item.Properties()));
        });
    }

}
