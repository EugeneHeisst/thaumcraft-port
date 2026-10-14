package dev.eugheist.tc4modern;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.vis.BiomeAura;
/** Original eight infused-stone vein attempts, adapted to modern feature placement. */
public final class ShardOreFeature extends Feature<NoneFeatureConfiguration> {
    public ShardOreFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        if(!WorldgenConfig.infused())return false;
        var world=context.level();var source=context.random();var chunk=new ChunkPos(context.origin());
        var random=new java.util.Random(){@Override public int nextInt(int bound){return source.nextInt(bound);}};
        Aspect[] primals={Aspect.AIR,Aspect.FIRE,Aspect.WATER,Aspect.EARTH,Aspect.ORDER,Aspect.ENTROPY};
        boolean placed=false;
        for(int i=0;i<8;i++){
            int x=chunk.getMinBlockX()+source.nextInt(16),z=chunk.getMinBlockZ()+source.nextInt(16);
            int y=source.nextInt(Math.max(5,world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z)-5));
            int variant=source.nextInt(6);
            if(source.nextInt(3)==0){
                Aspect tag=BiomeAura.tag(BiomeAspectBridge.types(world.getBiome(new BlockPos(x,y,z))),random);
                if(tag==null)variant=source.nextInt(6);
                else for(int a=0;a<primals.length;a++)if(primals[a]==tag)variant=a;
            }
            var config=new OreConfiguration(new BlockMatchTest(Blocks.STONE),TC4Modern.SHARD_ORES.get(variant).get().defaultBlockState(),6);
            placed|=Feature.ORE.place(new FeaturePlaceContext<>(context.topFeature(),world,context.chunkGenerator(),source,new BlockPos(x,y,z),config));
        }
        return placed;
    }
}
