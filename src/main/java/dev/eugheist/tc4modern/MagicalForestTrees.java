package dev.eugheist.tc4modern;

import net.minecraft.core.*;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.feature.*;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.lib.world.BigMagicTreeGenerator;

/** BiomeGenMagicalForest: six trees; silverwood1/14, otherwise greatwood1/10, otherwise original big oak. */
public final class MagicalForestTrees extends Feature<NoneFeatureConfiguration> {
    public MagicalForestTrees(){super(NoneFeatureConfiguration.CODEC);}
    static boolean growBigOak(WorldGenLevel level,RandomSource rng,BlockPos at) {
        var world=new BigMagicTreeGenerator.World() {
            public BigMagicTreeGenerator.Block func_147439_a(int x,int y,int z) {
                var s=level.getBlockState(new BlockPos(x,y,z));
                return s.isAir()?BigMagicTreeGenerator.Block.AIR:s.is(net.minecraft.tags.BlockTags.LEAVES)?BigMagicTreeGenerator.Block.LEAVES:s.is(net.minecraft.tags.BlockTags.LOGS)?BigMagicTreeGenerator.Block.LOG:BigMagicTreeGenerator.Block.OTHER;
            }
            public boolean soil(int x,int y,int z){return level.getBlockState(new BlockPos(x,y,z)).is(net.minecraft.tags.BlockTags.DIRT);}
            public void place(int x,int y,int z,BigMagicTreeGenerator.Block b,int md) {
                var s=b==BigMagicTreeGenerator.Block.LEAVES?Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DISTANCE,1):Blocks.OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS,md==4?Direction.Axis.X:md==8?Direction.Axis.Z:Direction.Axis.Y);
                level.setBlock(new BlockPos(x,y,z),s,2);
            }
        };
        var tree=new BigMagicTreeGenerator();tree.func_76487_a(1,1,1);
        return tree.func_76484_a(world,new java.util.Random(rng.nextLong()),at.getX(),at.getY(),at.getZ());
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
        var level=c.level();var rng=c.random();int count=6+(rng.nextInt(10)==0?1:0);boolean placed=false;
        for(int i=0;i<count;i++) {
            var at=level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,c.origin().offset(rng.nextInt(16),0,rng.nextInt(16)));
            if(rng.nextInt(14)==0){placed|=SilverwoodContent.FEATURE.get().grow(level,rng,at,true,8,5);continue;}
            if(rng.nextInt(10)==0){placed|=new GreatwoodFeature().grow(new FeaturePlaceContext<>(java.util.Optional.empty(),level,c.chunkGenerator(),rng,at,c.config()),true,false);continue;}
            placed|=growBigOak(level,rng,at);
        }
        placed|=new ManaPodFeature().place(c);return placed;
    }
}
