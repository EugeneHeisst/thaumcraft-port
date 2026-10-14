package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.phys.BlockHitResult;

public class ResearchTableBlock extends net.minecraft.world.level.block.BaseEntityBlock {
    public static final MapCodec<ResearchTableBlock> CODEC = simpleCodec(ResearchTableBlock::new);
    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING=net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty SECOND=net.minecraft.world.level.block.state.properties.BooleanProperty.create("second");
    public ResearchTableBlock(Properties properties) { super(properties);registerDefaultState(stateDefinition.any().setValue(FACING,net.minecraft.core.Direction.EAST).setValue(SECOND,false)); }
    @Override protected void createBlockStateDefinition(net.minecraft.world.level.block.state.StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,SECOND); }
    @Override protected BlockState updateShape(BlockState state,net.minecraft.core.Direction direction,BlockState adjacent,net.minecraft.world.level.LevelAccessor level,BlockPos pos,BlockPos other) {
        var paired=state.getValue(SECOND) ? state.getValue(FACING).getOpposite() : state.getValue(FACING);
        if(direction==paired && (!adjacent.is(this) || adjacent.getValue(SECOND)==state.getValue(SECOND) || adjacent.getValue(FACING)!=state.getValue(FACING))) {
            // BlockTable.onNeighborBlockChange: the surviving half becomes a plain table (metadata 0), dropping the tools and paper.
            return TC4Modern.TABLE.get().defaultBlockState();
        }
        return super.updateShape(state,direction,adjacent,level,pos,other);
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context) { return Block.box(0,0,0,16,16,16); }

    @Override protected MapCodec<? extends net.minecraft.world.level.block.BaseEntityBlock> codec() { return CODEC; }
    /** getDamageValue/damageDropped: both halves are BlockTable items of the plain table. */
    @Override public net.minecraft.world.item.ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) { return new net.minecraft.world.item.ItemStack(TC4Modern.TABLE.get()); }
    @Override public net.minecraft.world.level.block.entity.BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return state.getValue(SECOND) ? null : new ResearchTableEntity(pos, state); }
    @Override public <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, TC4Modern.RESEARCH_TABLE_ENTITY.get(), ResearchTableEntity::tick);
    }
    @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) { return net.minecraft.world.level.block.RenderShape.MODEL; }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moving) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof ResearchTableEntity table) net.minecraft.world.Containers.dropContents(level, pos, table.inventory);
        super.onRemove(state, level, pos, next, moving);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if(state.getValue(SECOND)) pos=pos.relative(state.getValue(FACING).getOpposite());
        final BlockPos root=pos;
        if (player instanceof ServerPlayer server && level.getBlockEntity(root) instanceof ResearchTableEntity table) {
            ThaumometerItem.sync(server, "");
            server.openMenu(new SimpleMenuProvider((id, inventory, user) -> new ResearchTableMenu(id, inventory, ContainerLevelAccess.create(level, root), table.inventory, table.bonus.bonusAspects), Component.translatable("block.tc4modern.research_table")));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
