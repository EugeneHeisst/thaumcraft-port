package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.*;
/** Original metal-device14 relay attachment and selection bounds. */
public class VisRelayBlock extends BaseEntityBlock {
 public static final MapCodec<VisRelayBlock> CODEC=simpleCodec(VisRelayBlock::new);
 public VisRelayBlock(Properties properties){super(properties);registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.FACING,Direction.UP));}
 protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder){builder.add(BlockStateProperties.FACING);}
 public BlockState getStateForPlacement(BlockPlaceContext context){return defaultBlockState().setValue(BlockStateProperties.FACING,context.getClickedFace());}
 protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
 public BlockEntity newBlockEntity(BlockPos pos,BlockState state){var tile=new VisRelayEntity(TC4Modern.VIS_RELAY_ENTITY.get(),pos,state);tile.relay.orientation=(short)state.getValue(BlockStateProperties.FACING).get3DDataValue();return tile;}
 protected VoxelShape getShape(BlockState state,BlockGetter world,BlockPos pos,CollisionContext context){
  return switch(state.getValue(BlockStateProperties.FACING).getOpposite()){
   case UP->box(5,8,5,11,16,11);case DOWN->box(5,0,5,11,8,11);
   case EAST->box(8,5,5,16,11,11);case WEST->box(0,5,5,8,11,11);
   case SOUTH->box(5,5,8,11,11,16);case NORTH->box(5,5,0,11,11,8);
  };
 }
 protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
  super.neighborChanged(state,level,pos,block,from,moving);
  if(!level.isClientSide&&level.isEmptyBlock(pos.relative(state.getValue(BlockStateProperties.FACING).getOpposite())))level.destroyBlock(pos,true);
 }
 protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack,BlockState state,Level level,BlockPos pos,net.minecraft.world.entity.player.Player player,net.minecraft.world.InteractionHand hand,net.minecraft.world.phys.BlockHitResult hit){
  if(!player.isShiftKeyDown()&&level.getBlockEntity(pos) instanceof VisRelayEntity relay){
   int color=stack.is(TC4Modern.BALANCED_SHARD.get())?6:-1;
   for(int i=0;i<TC4Modern.SHARDS.size();i++)if(stack.is(TC4Modern.SHARDS.get(i).get()))color=i;
   if(color>=0){if(!level.isClientSide)relay.setShardAttunement(color);return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);}
  }
  return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
 }
 public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,TC4Modern.VIS_RELAY_ENTITY.get(),VisRelayEntity::tick);}
}
