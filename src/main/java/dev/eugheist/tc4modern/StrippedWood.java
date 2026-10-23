package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.DeferredBlock;

/** Modern building variants; aura-bearing silverwood knots are deliberately never stripped. */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class StrippedWood {
    public static final DeferredBlock<WoodBlock> GREATWOOD_LOG = wood("stripped_greatwood_log");
    public static final DeferredBlock<WoodBlock> GREATWOOD_WOOD = wood("stripped_greatwood_wood");
    public static final DeferredBlock<WoodBlock> SILVERWOOD_LOG = wood("stripped_silverwood_log");
    public static final DeferredBlock<WoodBlock> SILVERWOOD_WOOD = wood("stripped_silverwood_wood");
    private static DeferredBlock<WoodBlock> wood(String id) {
        var block = TC4Modern.BLOCKS.register(id, () -> new WoodBlock(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.WOOD)));
        TC4Modern.ITEMS.registerSimpleBlockItem(id, block);
        return block;
    }
    public static void bootstrap() {} // Force registration before the deferred registers attach to the mod bus.
    @SubscribeEvent public static void strip(BlockEvent.BlockToolModificationEvent event) {
        if (event.getItemAbility() != ItemAbilities.AXE_STRIP || !event.getHeldItemStack().canPerformAction(ItemAbilities.AXE_STRIP)) return;
        BlockState state = event.getState();
        if (state.is(TC4Modern.GREATWOOD_LOG.get())) event.setFinalState(GREATWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)));
        else if (state.is(SilverwoodContent.LOG.get())) event.setFinalState(SILVERWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS)));
    }
    public static final class WoodBlock extends RotatedPillarBlock {
        WoodBlock(Properties properties) { super(properties); }
        @Override public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 5; }
        @Override public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) { return 5; }
    }
}
