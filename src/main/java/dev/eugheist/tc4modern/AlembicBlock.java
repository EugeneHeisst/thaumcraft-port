package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** BlockMetalDevice metadata 1: facing from placer yaw, knock messages, sneak-empty, labels, comparator. */
public final class AlembicBlock extends BaseEntityBlock {
    public static final MapCodec<AlembicBlock> CODEC = simpleCodec(AlembicBlock::new);
    static final SoundEvent KNOCK = SoundEvent.createVariableRangeEvent(AlchemyContent.id("alembicknock"));
    static final SoundEvent PAGE = SoundEvent.createVariableRangeEvent(AlchemyContent.id("page"));
    public AlembicBlock(Properties p) { super(p); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new AlembicEntity(pos, state); }
    /** Legacy yaw quadrant -> side id: 0->2, 1->5, 2->3, 3->4. */
    static int facingFromYaw(LivingEntity placer) {
        int l = net.minecraft.util.Mth.floor(placer.getYRot() * 4.0f / 360.0f + 0.5) & 3;
        return new int[]{2, 5, 3, 4}[l];
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer != null && level.getBlockEntity(pos) instanceof AlembicEntity tile) { tile.state.facing = facingFromYaw(placer); tile.changed(); }
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof AlembicEntity tile)) return InteractionResult.PASS;
        var s = tile.state;
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("§3").append(Component.translatable("tile.alembic.msg." + s.knockMessage())));
                level.playSound(null, pos, KNOCK, SoundSource.BLOCKS, .2f, 1f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (s.aspectFilter != null) {
            s.aspectFilter = null;
            tile.changed();
            if (level.isClientSide) level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, PAGE, SoundSource.BLOCKS, 1f, 1.1f, false);
            else dropLabel(level, pos, hit);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        s.amount = 0; s.aspect = null; tile.changed();
        if (level.isClientSide) {
            level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, KNOCK, SoundSource.BLOCKS, .2f, 1f, false);
            level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, SoundEvents.GENERIC_SWIM, SoundSource.BLOCKS, .5f, 1f + (level.random.nextFloat() - level.random.nextFloat()) * .3f, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    static void dropLabel(Level level, BlockPos pos, BlockHitResult hit) {
        var d = hit.getDirection();
        level.addFreshEntity(new ItemEntity(level, pos.getX() + .5 + d.getStepX() / 3.0, pos.getY() + .5, pos.getZ() + .5 + d.getStepZ() / 3.0, new ItemStack(EssentiaDevices.LABEL.get())));
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!held.is(EssentiaDevices.LABEL.get()) || !(level.getBlockEntity(pos) instanceof AlembicEntity tile) || tile.state.aspectFilter != null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        var labelAspects = EssentiaItems.getAspects(held);
        if (tile.state.amount == 0 && labelAspects == null) return ItemInteractionResult.SUCCESS;
        if (tile.state.amount == 0) tile.state.aspect = labelAspects.getAspects()[0];
        held.shrink(1);
        tile.state.aspectFilter = tile.state.aspect;
        tile.changed();
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
        if (!state.is(replacement.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof AlembicEntity tile && tile.state.aspectFilter != null)
            level.addFreshEntity(new ItemEntity(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, new ItemStack(EssentiaDevices.LABEL.get())));
        super.onRemove(state, level, pos, replacement, moving);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
    @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) { return level.getBlockEntity(pos) instanceof AlembicEntity tile ? tile.state.comparator() : 0; }
}
