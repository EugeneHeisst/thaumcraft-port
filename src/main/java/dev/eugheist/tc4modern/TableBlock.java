package dev.eugheist.tc4modern;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * BlockTable metadata 0/1: onBlockPlacedBy picks metadata from the placer's yaw quadrant (south/north 0, west/east 1)
 * and TileTableRenderer turns metadata 1 by 90 degrees.
 */
public class TableBlock extends Block {
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public TableBlock(Properties properties) { super(properties); registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z)); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(AXIS); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
    }
    @Override protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return rotation == net.minecraft.world.level.block.Rotation.CLOCKWISE_90 || rotation == net.minecraft.world.level.block.Rotation.COUNTERCLOCKWISE_90
            ? state.setValue(AXIS, state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X) : state;
    }
}
