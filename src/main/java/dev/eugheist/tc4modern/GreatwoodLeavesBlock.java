package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/** Original decay flag and four-block source connectivity, independent of vanilla seven-block decay. */
public final class GreatwoodLeavesBlock extends LeavesBlock {
    public static final BooleanProperty CHECK_DECAY=BooleanProperty.create("check_decay");
    public GreatwoodLeavesBlock(Properties properties){super(properties);registerDefaultState(defaultBlockState().setValue(CHECK_DECAY,false));}
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block,BlockState> builder){super.createBlockStateDefinition(builder);builder.add(CHECK_DECAY);}
    @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){return state;}
    @Override protected void tick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){}
    @Override public boolean isRandomlyTicking(BlockState state){return state.getValue(CHECK_DECAY)&&!state.getValue(PERSISTENT);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        if(!state.getValue(CHECK_DECAY)||state.getValue(PERSISTENT)||!level.hasChunksAt(pos.offset(-5,-5,-5),pos.offset(5,5,5)))return;
        boolean supported=thaumcraft.common.lib.world.GreatwoodLeafDecay.supported(new thaumcraft.common.lib.world.GreatwoodLeafDecay.View(){
            public boolean sustains(int x,int y,int z){return level.getBlockState(pos.offset(x,y,z)).is(net.minecraft.tags.BlockTags.LOGS);}
            public boolean leaves(int x,int y,int z){return level.getBlockState(pos.offset(x,y,z)).is(net.minecraft.tags.BlockTags.LEAVES);}
        });
        if(supported)level.setBlock(pos,state.setValue(CHECK_DECAY,false),3);
        else {if(random.nextInt(200)==0)popResource(level,pos,new net.minecraft.world.item.ItemStack(TC4Modern.GREATWOOD_SAPLING_ITEM.get()));level.removeBlock(pos,false);}
    }
    @Override public int getFlammability(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,Direction face){return 60;}
    @Override public int getFireSpreadSpeed(BlockState state,net.minecraft.world.level.BlockGetter level,BlockPos pos,Direction face){return 30;}
    static void beginDecay(Level level,BlockPos pos,int radius){
        if(level.isClientSide||!level.hasChunksAt(pos.offset(-radius-1,-radius-1,-radius-1),pos.offset(radius+1,radius+1,radius+1)))return;
        for(var p:BlockPos.betweenClosed(pos.offset(-radius,-radius,-radius),pos.offset(radius,radius,radius))){var state=level.getBlockState(p);if(state.getBlock() instanceof GreatwoodLeavesBlock)level.setBlock(p,state.setValue(CHECK_DECAY,true),4);}
    }
    @Override protected void onRemove(BlockState state,Level level,BlockPos pos,BlockState replacement,boolean moving){if(!state.is(replacement.getBlock()))beginDecay(level,pos,1);super.onRemove(state,level,pos,replacement,moving);}
}
