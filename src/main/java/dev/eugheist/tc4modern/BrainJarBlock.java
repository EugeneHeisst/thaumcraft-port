package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class BrainJarBlock extends BaseEntityBlock {
    public static final MapCodec<BrainJarBlock> CODEC = simpleCodec(BrainJarBlock::new);
    private static final VoxelShape SHAPE = Block.box(3, 0, 3, 13, 12, 13);
    public BrainJarBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BrainJarEntity(BrainJarContent.ENTITY.get(), pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, BrainJarContent.ENTITY.get(), level.isClientSide ? BrainJarEntity::clientTick : BrainJarEntity::serverTick);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof BrainJarEntity jar)) return InteractionResult.PASS;
        if (level.isClientSide) level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
            WardedJarBlock.JAR, net.minecraft.sounds.SoundSource.BLOCKS, .2f, 1, false);
        else jar.release();
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack held,
            BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        var result = useWithoutItem(state, level, pos, player, hit);
        return result.consumesAction() ? net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide)
            : net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
    @Override public float getEnchantPowerBonus(BlockState state, net.minecraft.world.level.LevelReader level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BrainJarEntity ? 2f : 0f;
    }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, net.minecraft.util.RandomSource random) {
        if (level.getBlockEntity(pos) instanceof BrainJarEntity jar && jar.storage.experience() >= 2000)
            BrainJarParticle.spawn(level, pos.getX() + .3 + random.nextFloat() * .4f, pos.getY() + .9, pos.getZ() + .3 + random.nextFloat() * .4f);
    }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof BrainJarEntity jar ? jar.storage.comparatorSignal() : 0;
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof BrainJarEntity jar)
            jar.spawnExperience(jar.storage.experience(), net.minecraft.world.phys.Vec3.atLowerCornerOf(pos));
        super.onRemove(state, level, pos, replacement, moving);
    }
}
