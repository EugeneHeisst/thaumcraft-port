package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** BlockStoneDevice metadata 0: front texture on all four sides (on while burning), filled top while vis > 0. */
public final class AlchemicalFurnaceBlock extends BaseEntityBlock {
    public static final MapCodec<AlchemicalFurnaceBlock> CODEC = simpleCodec(AlchemicalFurnaceBlock::new);
    public static final BooleanProperty LIT = BooleanProperty.create("lit");
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    public AlchemicalFurnaceBlock(Properties p) { super(p); registerDefaultState(stateDefinition.any().setValue(LIT, false).setValue(FILLED, false)); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(LIT, FILLED); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AlchemicalFurnaceEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, EssentiaDevices.FURNACE_ENTITY.get(), AlchemicalFurnaceEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof AlchemicalFurnaceEntity furnace) player.openMenu(furnace);
        return InteractionResult.CONSUME;
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
        if (level.getBlockEntity(pos) instanceof AlchemicalFurnaceEntity furnace) furnace.state.bellows = furnace.countBellows();
        super.neighborChanged(state, level, pos, block, from, moving);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof AlchemicalFurnaceEntity furnace) Containers.dropContents(level, pos, furnace);
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { return AbstractContainerMenu.getRedstoneSignalFromBlockEntity(level.getBlockEntity(pos)); }
    /** randomDisplayTick: smoke and flame on all four sides while burning. */
    @Override public void animateTick(BlockState state, Level w, BlockPos pos, RandomSource r) {
        if (!state.getValue(LIT)) return;
        float f = pos.getX() + .5f, f1 = pos.getY() + .2f + r.nextFloat() * 5f / 16f, f2 = pos.getZ() + .5f, f3 = .52f, f4 = r.nextFloat() * .5f - .25f;
        float[][] at = {{f - f3, f2 + f4}, {f + f3, f2 + f4}, {f + f4, f2 - f3}, {f + f4, f2 + f3}};
        for (float[] p : at) { w.addParticle(ParticleTypes.SMOKE, p[0], f1, p[1], 0, 0, 0); w.addParticle(ParticleTypes.FLAME, p[0], f1, p[1], 0, 0, 0); }
    }
}
