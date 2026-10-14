package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
public final class NodeConverterBlock extends BaseEntityBlock {
 public static final MapCodec<NodeConverterBlock> CODEC=simpleCodec(NodeConverterBlock::new);
 public NodeConverterBlock(Properties properties){super(properties);}
 @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
 @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.ENTITYBLOCK_ANIMATED;}
 @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new NodeConverterEntity(pos,state);}
 @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,Block block,BlockPos from,boolean moving){
  super.neighborChanged(state,level,pos,block,from,moving);
  if(level.getBlockEntity(pos) instanceof NodeConverterEntity converter)converter.refresh();
 }
 @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return createTickerHelper(type,TC4Modern.NODE_CONVERTER_ENTITY.get(),NodeConverterEntity::tick);}
}
