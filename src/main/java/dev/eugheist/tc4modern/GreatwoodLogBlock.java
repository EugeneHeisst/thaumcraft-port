package dev.eugheist.tc4modern;
public final class GreatwoodLogBlock extends net.minecraft.world.level.block.RotatedPillarBlock {
    public GreatwoodLogBlock(Properties properties){super(properties);}
    @Override public int getFlammability(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos,net.minecraft.core.Direction face){return 5;}
    @Override public int getFireSpreadSpeed(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.BlockGetter level,net.minecraft.core.BlockPos pos,net.minecraft.core.Direction face){return 5;}
    @Override protected void onRemove(net.minecraft.world.level.block.state.BlockState state,net.minecraft.world.level.Level level,net.minecraft.core.BlockPos pos,net.minecraft.world.level.block.state.BlockState replacement,boolean moving){
        if(!state.is(replacement.getBlock()))GreatwoodLeavesBlock.beginDecay(level,pos,4);
        super.onRemove(state,level,pos,replacement,moving);
    }
}
