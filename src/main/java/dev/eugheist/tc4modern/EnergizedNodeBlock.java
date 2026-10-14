package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
/** Native host of the original airy energized-node block. */
public final class EnergizedNodeBlock extends BaseEntityBlock {
    public static final MapCodec<EnergizedNodeBlock> CODEC=simpleCodec(EnergizedNodeBlock::new);
    public EnergizedNodeBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    @Override protected void neighborChanged(BlockState state,Level level,BlockPos pos,net.minecraft.world.level.block.Block block,BlockPos from,boolean moving){
        NodeExplosion.validateSupport(level,pos);super.neighborChanged(state,level,pos,block,from,moving);
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return NodeBlockEffects.SHAPE;}
    /** Not an INode in the original: breaking gives the burst and sound but no wisp essence. */
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.player.Player player){
        NodeBlockEffects.destroyed(level,pos);return super.playerWillDestroy(level,pos,state,player);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new EnergizedNodeEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return level.isClientSide?null:createTickerHelper(type,TC4Modern.ENERGIZED_NODE_ENTITY.get(),EnergizedNodeEntity::tick);}
}
