package dev.eugheist.tc4modern;

import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** Original BlockCosmeticStairs and BlockCosmeticWoodSlab variants. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
final class MagicalWoodShapes {
    static final DeferredHolder<Block,Block> GREAT_STAIRS=ref("greatwood_stairs"),SILVER_STAIRS=ref("silverwood_stairs"),GREAT_SLAB=ref("greatwood_slab"),SILVER_SLAB=ref("silverwood_slab");
    static DeferredHolder<Block,Block> ref(String name){return DeferredHolder.create(Registries.BLOCK,AlchemyContent.id(name));}
    static BlockBehaviour.Properties wood(boolean slab){return BlockBehaviour.Properties.of().mapColor(net.minecraft.world.level.material.MapColor.WOOD).strength(slab?2:2.5f,TC4SoundTypes.res(slab?5:10)).sound(SoundType.WOOD).ignitedByLava();}
    @SubscribeEvent(priority=net.neoforged.bus.api.EventPriority.LOWEST) static void register(RegisterEvent event) {
        event.register(Registries.BLOCK,h->{
            h.register(GREAT_STAIRS.getId(),new StairBlock(AlchemyContent.GREATWOOD_PLANKS.get().defaultBlockState(),wood(false)));
            h.register(SILVER_STAIRS.getId(),new StairBlock(AlchemyContent.SILVERWOOD_PLANKS.get().defaultBlockState(),wood(false)));
            h.register(GREAT_SLAB.getId(),new SlabBlock(wood(true)));h.register(SILVER_SLAB.getId(),new SlabBlock(wood(true)));
        });
        event.register(Registries.ITEM,h->{for(var block:List.of(GREAT_STAIRS,SILVER_STAIRS,GREAT_SLAB,SILVER_SLAB))h.register(block.getId(),new BlockItem(block.get(),new Item.Properties()));});
    }
}
