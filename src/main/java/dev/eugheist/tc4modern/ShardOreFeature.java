package dev.eugheist.tc4modern;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.vis.BiomeAura;
/**
 * Original infused-stone vein attempts (biome aspect bias 1 in 3), adapted to the 1.21 world: veins span the whole height
 * down to the bottom of the world, deepslate gets the deepslate variant, and (tc4modern) the primal mix depends on depth:
 * fire, earth and entropy grow more common towards the bottom, air, water and order towards the surface.
 */
public final class ShardOreFeature extends Feature<NoneFeatureConfiguration> {
    static final int ATTEMPTS=16;
    /** Variant indices: 0 air, 1 fire, 2 water, 3 earth, 4 order, 5 entropy. */
    static final int[] DEEP={1,3,5},HIGH={0,2,4};
    public ShardOreFeature(){super(NoneFeatureConfiguration.CODEC);}
    /** Depth-weighted primal: t=0 at the world bottom, 1 at the surface; deep group weight 1+4(1-t), high group 1+4t (5:1 at the bottom, 1:5 at the surface). */
    static int depthVariant(net.minecraft.util.RandomSource source,double t){
        double deep=1+4*(1-t),high=1+4*t,roll=source.nextDouble()*(3*deep+3*high);
        if(roll<3*deep)return DEEP[(int)(roll/deep)];
        return HIGH[Math.min(2,(int)((roll-3*deep)/high))];
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        if(!WorldgenConfig.infused())return false;
        var world=context.level();var source=context.random();var chunk=new ChunkPos(context.origin());
        var random=new java.util.Random(){@Override public int nextInt(int bound){return source.nextInt(bound);}};
        Aspect[] primals={Aspect.AIR,Aspect.FIRE,Aspect.WATER,Aspect.EARTH,Aspect.ORDER,Aspect.ENTROPY};
        int minY=world.getMinBuildHeight();
        boolean placed=false;
        for(int i=0;i<ATTEMPTS;i++){
            int x=chunk.getMinBlockX()+source.nextInt(16),z=chunk.getMinBlockZ()+source.nextInt(16);
            int surface=world.getHeight(Heightmap.Types.WORLD_SURFACE_WG,x,z);
            int y=minY+source.nextInt(Math.max(5,surface-5-minY));
            int variant=depthVariant(source,Math.max(0,Math.min(1,(y-minY)/(double)Math.max(1,surface-minY))));
            // Original 1-in-3 biome bias, only above y=0: the surface biome does not reach the deep layers.
            if(y>=0&&source.nextInt(3)==0){
                Aspect tag=BiomeAura.tag(BiomeAspectBridge.types(world.getBiome(new BlockPos(x,y,z))),random);
                if(tag!=null)for(int a=0;a<primals.length;a++)if(primals[a]==tag)variant=a;
            }
            var targets=java.util.List.of(
                OreConfiguration.target(new net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES),TC4Modern.SHARD_ORES.get(variant).get().defaultBlockState()),
                OreConfiguration.target(new net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest(net.minecraft.tags.BlockTags.DEEPSLATE_ORE_REPLACEABLES),TC4Modern.DEEPSLATE_SHARD_ORES.get(variant).get().defaultBlockState()));
            var config=new OreConfiguration(targets,6);
            placed|=Feature.ORE.place(new FeaturePlaceContext<>(context.topFeature(),world,context.chunkGenerator(),source,new BlockPos(x,y,z),config));
        }
        return placed;
    }
}
