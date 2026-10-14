package dev.eugheist.tc4modern;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Registration hub for the alchemy/essentia slice: original resources (cinnabar, amber, quicksilver,
 * magical planks) first, devices later. Kept separate from TC4Modern so parallel work merges cleanly.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class AlchemyContent {
    private AlchemyContent() {}
    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, path); }
    private static DeferredHolder<Block, Block> block(String name) { return DeferredHolder.create(Registries.BLOCK, id(name)); }
    private static DeferredHolder<Item, Item> item(String name) { return DeferredHolder.create(Registries.ITEM, id(name)); }

    // BlockCustomOre metadata 0 / 7: hardness 1.5, resistance 5, stone sound.
    public static final DeferredHolder<Block, Block> CINNABAR_ORE = block("cinnabar_ore");
    public static final DeferredHolder<Block, Block> AMBER_ORE = block("amber_ore");
    // BlockWoodenDevice metadata 6 / 7: hardness 2.5, resistance 10, wood.
    public static final DeferredHolder<Block, Block> GREATWOOD_PLANKS = block("greatwood_planks");
    public static final DeferredHolder<Block, Block> SILVERWOOD_PLANKS = block("silverwood_planks");
    public static final DeferredHolder<Item, Item> QUICKSILVER = item("quicksilver");
    public static final DeferredHolder<Item, Item> AMBER = item("amber");
    public static final DeferredHolder<Item, Item> QUICKSILVER_DROP = item("quicksilver_drop");

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            var ore = BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(1.5f, TC4SoundTypes.res(5)).sound(SoundType.STONE).requiresCorrectToolForDrops();
            helper.register(id("cinnabar_ore"), new Block(ore));
            helper.register(id("amber_ore"), new DropExperienceBlock(UniformInt.of(1, 4), ore));
            var planks = BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f, TC4SoundTypes.res(10)).sound(SoundType.WOOD).ignitedByLava();
            helper.register(id("greatwood_planks"), new Block(planks));
            helper.register(id("silverwood_planks"), new Block(planks));
        });
        event.register(Registries.ITEM, helper -> {
            for (var b : java.util.List.of(CINNABAR_ORE, AMBER_ORE, GREATWOOD_PLANKS, SILVERWOOD_PLANKS))
                helper.register(b.getId(), new BlockItem(b.get(), new Item.Properties()));
            helper.register(id("quicksilver"), new Item(new Item.Properties()));
            helper.register(id("amber"), new Item(new Item.Properties()));
            helper.register(id("quicksilver_drop"), new Item(new Item.Properties()));
        });
        event.register(Registries.FEATURE, helper -> helper.register(id("cinnabar_amber"), new CinnabarAmberFeature()));
    }

}
