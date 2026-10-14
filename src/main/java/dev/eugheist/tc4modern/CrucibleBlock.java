package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;

/** Original BlockMetalDevice metadata 0 hollow collision bounds. */
public final class CrucibleBlock extends BaseEntityBlock {
    public static final MapCodec<CrucibleBlock> CODEC = simpleCodec(CrucibleBlock::new);
    private static final VoxelShape SHAPE = Shapes.or(box(0,0,0,16,5,16),
            box(0,0,0,2,13.6,16),box(0,0,0,16,13.6,2),
            box(14,0,0,16,13.6,16),box(0,0,14,16,13.6,16));
    private int contactDelay; // Original BlockMetalDevice singleton delay shared across crucibles.
    public CrucibleBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new CrucibleEntity(pos,state); }
    @Override protected net.minecraft.world.level.block.RenderShape getRenderShape(BlockState state) {
        return net.minecraft.world.level.block.RenderShape.MODEL;
    }
    @Override protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override protected net.minecraft.world.ItemInteractionResult useItemOn(
            net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (!stack.is(net.minecraft.world.item.Items.WATER_BUCKET))
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.getBlockEntity(pos) instanceof CrucibleEntity tile && !level.isClientSide
                && tile.crucible.water < thaumcraft.common.tiles.CrucibleState.CAPACITY) {
            tile.crucible.fill(1000);
            // Original consumes the filled container in creative too.
            stack.shrink(1);
            var empty = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BUCKET);
            if (stack.isEmpty()) player.setItemInHand(hand, empty);
            else if (!player.getInventory().add(empty)) player.drop(empty,false);
            tile.changed();
            player.containerMenu.broadcastChanges();
            level.playSound(null,pos,net.minecraft.sounds.SoundEvents.GENERIC_SWIM,
                    net.minecraft.sounds.SoundSource.BLOCKS,.33f,
                    1f+(level.random.nextFloat()-level.random.nextFloat())*.3f);
        }
        return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void entityInside(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.Entity entity) {
        if(!level.isClientSide && level.getBlockEntity(pos) instanceof CrucibleEntity tile){
            boolean boiling=tile.crucible.boiling()&&tile.crucible.water>0;
            if(entity instanceof net.minecraft.world.entity.item.ItemEntity item
                    && !(item instanceof thaumcraft.common.entities.EntitySpecialItem)&&boiling)tile.attemptSmelt(item);
            else if(++contactDelay>=10){
                contactDelay=0;
                if(entity instanceof net.minecraft.world.entity.LivingEntity && boiling){
                    entity.hurt(level.damageSources().inFire(),1);
                    level.playSound(null,pos,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.4f,2+level.random.nextFloat()*.4f);
                }
            }
        }
        super.entityInside(state,level,pos,entity);
    }
    @Override protected boolean hasAnalogOutputSignal(BlockState state){return true;}
    @Override protected int getAnalogOutputSignal(BlockState state,Level level,BlockPos pos){return level.getBlockEntity(pos) instanceof CrucibleEntity tile?tile.crucible.comparator():0;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
            BlockEntityType<T> type) { return createTickerHelper(type, TC4Modern.CRUCIBLE_ENTITY.get(), CrucibleEntity::tick); }
}
