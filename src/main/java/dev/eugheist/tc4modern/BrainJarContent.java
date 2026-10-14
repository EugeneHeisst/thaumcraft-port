package dev.eugheist.tc4modern;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class BrainJarContent {
    public static final DeferredHolder<Block, Block> BLOCK = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("brain_jar"));
    public static final DeferredHolder<Item, Item> ITEM = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("brain_jar"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrainJarEntity>> ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("brain_jar"));
    public static final DeferredHolder<net.minecraft.sounds.SoundEvent, net.minecraft.sounds.SoundEvent> SIGH = DeferredHolder.create(Registries.SOUND_EVENT, AlchemyContent.id("brain"));
    @SubscribeEvent static void register(RegisterEvent event) {
        event.register(Registries.SOUND_EVENT, helper -> helper.register(AlchemyContent.id("brain"), net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("brain"))));
        event.register(Registries.BLOCK, helper -> helper.register(AlchemyContent.id("brain_jar"),
            new BrainJarBlock(BlockBehaviour.Properties.of().strength(.3f).sound(TC4SoundTypes.JAR).lightLevel(s -> 9).noOcclusion())));
        event.register(Registries.ITEM, helper -> helper.register(AlchemyContent.id("brain_jar"), new BlockItem(BLOCK.get(), new Item.Properties())));
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> helper.register(AlchemyContent.id("brain_jar"),
            BlockEntityType.Builder.of((pos, state) -> new BrainJarEntity(ENTITY.get(), pos, state), BLOCK.get()).build(null)));
    }
}
