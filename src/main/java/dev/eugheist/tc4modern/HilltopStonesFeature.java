package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Original WorldGenHilltopStones geometry, spawn checks and eight vine-covered pillars. */
final class HilltopStonesFeature extends Feature<NoneFeatureConfiguration> {
    HilltopStonesFeature(){super(NoneFeatureConfiguration.CODEC);}
    static boolean valid(WorldGenLevel level,BlockPos origin) {
        if(origin.getY()<85||origin.getY()+6>=level.getMaxBuildHeight())return false;
        var pos=origin;
        for(int step=0;step<=2;step++,pos=pos.above())if(level.isEmptyBlock(pos)) {
            var ground=level.getBlockState(pos.below());
            if(ground.is(Blocks.SNOW)||ground.is(Blocks.SHORT_GRASS))ground=level.getBlockState(pos.below(2));
            return ground.is(Blocks.STONE)||ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.DIRT);
        }
        return false;
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if(!WorldgenConfig.structures())return false;
        var level=context.level();
        if(!level.getLevel().dimension().equals(Level.OVERWORLD)||level.getLevel().getChunkSource().getGenerator() instanceof FlatLevelSource)return false;
        return build(level,context.origin(),context.random());
    }
    static boolean build(WorldGenLevel level,BlockPos origin,RandomSource random) {
        for(var offset:new int[][]{{-2,-2},{0,0},{2,0},{2,2},{0,2}})if(!valid(level,origin.offset(offset[0],0,offset[1])))return false;
        // Legacy biome.fillerBlock disappeared; these valid mountain substrates use dirt.
        var filler=Blocks.DIRT.defaultBlockState();
        boolean vines=!level.getBiome(origin).value().coldEnoughToSnow(origin);
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++) {
            if(Math.abs(x)==3&&Math.abs(z)==3)continue;
            var pos=origin.offset(x,0,z);
            level.setBlock(pos,(random.nextBoolean()?CrimsonContent.OBSIDIAN_TILE.get():Blocks.OBSIDIAN).defaultBlockState(),3);
            boolean stop=false;
            for(int y=1;y<5;y++) {
                if(pos.getY()-y<level.getMinBuildHeight())continue;
                var below=pos.below(y);var ground=level.getBlockState(below);
                if(ground.isAir()||ground.is(Blocks.SNOW)||ground.is(Blocks.POPPY)||ground.is(Blocks.DANDELION)||ground.is(Blocks.SHORT_GRASS))level.setBlock(below,filler,3);
                if(x==0&&z==0&&y==1) {
                    level.setBlock(pos.above(),CrimsonContent.OBSIDIAN_TILE.get().defaultBlockState(),3);
                    var chest=pos.above(2);level.setBlock(chest,Blocks.CHEST.defaultBlockState(),3);
                    if(level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity loot)loot.setLootTable(ResourceKey.create(Registries.LOOT_TABLE,AlchemyContent.id("chests/hilltop_stones")),random.nextLong());
                    level.setBlock(pos,Blocks.SPAWNER.defaultBlockState(),3);
                    if(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)spawner.setEntityId(WispContent.WISP.get(),random);
                }
                if(stop||!((Math.abs(x)==3&&Math.abs(z%2)==1)||(Math.abs(z)==3&&Math.abs(x%2)==1)))continue;
                var top=pos.above(y);level.setBlock(top,ObsidianTotems.PLAIN.get().defaultBlockState(),3);
                if(y<2||!random.nextBoolean())continue;
                stop=true;
                if(vines)for(var direction:new Direction[]{Direction.WEST,Direction.EAST,Direction.NORTH,Direction.SOUTH})if(random.nextInt(3)==0&&level.isEmptyBlock(top.relative(direction)))growVines(level,top.relative(direction),direction.getOpposite());
            }
        }
        AuraNodeFeature.createRandomNodeAt(level,origin.above(5),random,false,true,false);
        return true;
    }
    private static void growVines(WorldGenLevel level,BlockPos pos,Direction attached) {
        var vine=Blocks.VINE.defaultBlockState().setValue(VineBlock.getPropertyForFace(attached),true);
        for(int i=0;i<=4&&level.isEmptyBlock(pos.below(i));i++)level.setBlock(pos.below(i),vine,2);
    }
}
