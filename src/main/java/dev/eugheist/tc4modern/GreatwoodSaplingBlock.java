package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Source BlockCustomPlant greatwood: light9, one-in25 random tick, restore on failed generation. */
public final class GreatwoodSaplingBlock extends BushBlock {
    public static final com.mojang.serialization.MapCodec<GreatwoodSaplingBlock> CODEC=simpleCodec(GreatwoodSaplingBlock::new);
    @Override protected com.mojang.serialization.MapCodec<GreatwoodSaplingBlock> codec(){return CODEC;}
    public GreatwoodSaplingBlock(Properties properties){super(properties);}
    @Override protected void randomTick(BlockState state,ServerLevel level,BlockPos pos,RandomSource random){
        // Old BlockBush tick checks the supporting soil before attempting growth.
        if(!state.canSurvive(level,pos)){
            dropResources(state,level,pos);
            level.removeBlock(pos,false);
            return;
        }
        if(level.getMaxLocalRawBrightness(pos.above())<9||random.nextInt(25)!=0)return;
        level.removeBlock(pos,false);
        var context=new net.minecraft.world.level.levelgen.feature.FeaturePlaceContext<net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration>(java.util.Optional.empty(),level,level.getChunkSource().getGenerator(),random,pos,net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration.INSTANCE);
        if(!TC4Modern.GREATWOOD_FEATURE.get().grow(context,false))level.setBlock(pos,state,2);
    }
}
