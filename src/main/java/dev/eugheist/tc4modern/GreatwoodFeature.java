package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import thaumcraft.common.lib.world.GreatwoodTreeGenerator;

/** World adapter for the original two-pass greatwood generator. */
public final class GreatwoodFeature extends Feature<NoneFeatureConfiguration> {
    public GreatwoodFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        return grow(context,true);
    }
    public boolean grow(FeaturePlaceContext<NoneFeatureConfiguration> context,boolean natural){return grow(context,natural,natural);}
    public boolean grow(FeaturePlaceContext<NoneFeatureConfiguration> context,boolean natural,boolean spiderChance){
        var level=context.level();var random=context.random();
        var incoming=new java.util.Random(){
            @Override public long nextLong(){return random.nextLong();}
            @Override public int nextInt(int bound){return random.nextInt(bound);}
        };
        var world=new GreatwoodTreeGenerator.World(){
            public GreatwoodTreeGenerator.Block func_147439_a(int x,int y,int z){
                var state=level.getBlockState(new BlockPos(x,y,z));
                return state.isAir()?GreatwoodTreeGenerator.Block.AIR:state.is(TC4Modern.GREATWOOD_LEAVES.get())?GreatwoodTreeGenerator.Block.LEAVES:state.is(TC4Modern.GREATWOOD_LOG.get())?GreatwoodTreeGenerator.Block.LOG:GreatwoodTreeGenerator.Block.OTHER;
            }
            public boolean canReplaceLeaves(int x,int y,int z){var state=level.getBlockState(new BlockPos(x,y,z));return state.isAir()||state.is(TC4Modern.GREATWOOD_LEAVES.get());}
            public boolean soil(int x,int y,int z){return level.getBlockState(new BlockPos(x,y,z)).is(net.minecraft.tags.BlockTags.DIRT);}
            public void place(int x,int y,int z,GreatwoodTreeGenerator.Block block,int metadata){
                var state=block==GreatwoodTreeGenerator.Block.LEAVES?TC4Modern.GREATWOOD_LEAVES.get().defaultBlockState():TC4Modern.GREATWOOD_LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS,metadata==4?Direction.Axis.X:metadata==8?Direction.Axis.Z:Direction.Axis.Y);
                level.setBlock(new BlockPos(x,y,z),state,natural?2:3);
            }
            public void spiderDecorations(int x,int y,int z,java.util.Random incomingRandom,java.util.Random treeRandom){
                var spawner=new BlockPos(x,y-1,z);level.setBlock(spawner,Blocks.SPAWNER.defaultBlockState(),3);
                if(level.getBlockEntity(spawner) instanceof SpawnerBlockEntity tile){
                    tile.setEntityId(EntityType.CAVE_SPIDER,random);
                    for(int a=0;a<50;a++){
                        var p=new BlockPos(x-7+incomingRandom.nextInt(14),y+incomingRandom.nextInt(10),z-7+incomingRandom.nextInt(14));
                        if(!level.isEmptyBlock(p))continue;
                        boolean touching=false;
                        for(var direction:Direction.values()){var state=level.getBlockState(p.relative(direction));if(state.is(TC4Modern.GREATWOOD_LEAVES.get())||state.is(TC4Modern.GREATWOOD_LOG.get())){touching=true;break;}}
                        if(touching)level.setBlock(p,Blocks.COBWEB.defaultBlockState(),3);
                    }
                    var chest=new BlockPos(x,y-2,z);level.setBlock(chest,Blocks.CHEST.defaultBlockState(),3);
                    if(level.getBlockEntity(chest) instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity loot)loot.setLootTable(net.minecraft.world.level.storage.loot.BuiltInLootTables.SIMPLE_DUNGEON,treeRandom.nextLong());
                }
            }
        };
        return new GreatwoodTreeGenerator().generate(world,incoming,context.origin().getX(),context.origin().getY(),context.origin().getZ(),spiderChance&&incoming.nextInt(8)==0);
    }
}
