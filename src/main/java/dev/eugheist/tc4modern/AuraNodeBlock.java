package dev.eugheist.tc4modern;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public class AuraNodeBlock extends BaseEntityBlock {
    public static final MapCodec<AuraNodeBlock> CODEC=simpleCodec(AuraNodeBlock::new);
    public AuraNodeBlock(Properties properties){super(properties);}
    @Override protected MapCodec<? extends BaseEntityBlock> codec(){return CODEC;}
    @Override protected RenderShape getRenderShape(BlockState state){return RenderShape.INVISIBLE;}
    private boolean airy(){return getClass()==AuraNodeBlock.class;}
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){
        return airy()?NodeBlockEffects.SHAPE:super.getShape(state,level,pos,context);
    }
    @Override public BlockState playerWillDestroy(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,net.minecraft.world.entity.player.Player player){
        if(airy()){
            NodeBlockEffects.destroyed(level,pos);
            if(level.getBlockEntity(pos) instanceof AuraNodeEntity node)NodeBlockEffects.dropWispEssence(level,pos,node.reservoir.aspects);
        }
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new AuraNodeEntity(pos,state);}
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(net.minecraft.world.level.Level level,BlockState state,net.minecraft.world.level.block.entity.BlockEntityType<T> type){
        return createTickerHelper(type,TC4Modern.AURA_NODE_ENTITY.get(),AuraNodeEntity::tick);
    }
}
