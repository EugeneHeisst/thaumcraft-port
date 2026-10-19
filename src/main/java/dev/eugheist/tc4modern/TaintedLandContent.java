package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.BlockStateConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Original naturally occurring Tainted Land, ordinary trees and special crust/fibre decoration. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
final class TaintedLandContent {
    @SubscribeEvent static void register(RegisterEvent event) {
        event.register(Registries.FEATURE,h->{h.register(AlchemyContent.id("tainted_trees"),new Trees());h.register(AlchemyContent.id("tainted_decorations"),new Decorations());});
    }
    @SubscribeEvent static void setup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent event){event.enqueueWork(()->{if(WorldgenConfig.taint()&&WorldgenConfig.taintWeight()>0)terrablender.api.Regions.register(new Region());});}
    static final class Region extends terrablender.api.Region {
        Region(){super(AlchemyContent.id("tainted_land"),terrablender.api.RegionType.OVERWORLD,WorldgenConfig.taintWeight());}
        /**
         * The original Tainted Land was one ordinary biome among many. Replacing every plains/forest/taiga of the region made
         * whole regions tainted (3% of the world, 500+ block blobs), so only the middle third of those biomes' humidity range
         * becomes Tainted Land (tc4modern: narrowed to a 10% band after players found the third far too large).
         */
        @Override public void addBiomes(net.minecraft.core.Registry<net.minecraft.world.level.biome.Biome> registry,java.util.function.Consumer<com.mojang.datafixers.util.Pair<net.minecraft.world.level.biome.Climate.ParameterPoint,net.minecraft.resources.ResourceKey<net.minecraft.world.level.biome.Biome>>> mapper) {
            if(Boolean.getBoolean("tc4modern.taintLegacyRegion")){addModifiedVanillaOverworldBiomes(mapper,b->{b.replaceBiome(net.minecraft.world.level.biome.Biomes.PLAINS,TaintBiomes.TAINTED_LAND);b.replaceBiome(net.minecraft.world.level.biome.Biomes.FOREST,TaintBiomes.TAINTED_LAND);b.replaceBiome(net.minecraft.world.level.biome.Biomes.TAIGA,TaintBiomes.TAINTED_LAND);});return;}
            var hosts=java.util.Set.of(net.minecraft.world.level.biome.Biomes.PLAINS,net.minecraft.world.level.biome.Biomes.FOREST,net.minecraft.world.level.biome.Biomes.TAIGA);
            // An unmodified TerraBlender builder yields the vanilla overworld parameter points.
            new terrablender.api.ModifiedVanillaOverworldBuilder().build().forEach(pair->{
                var point=pair.getFirst();
                if(!hosts.contains(pair.getSecond())){mapper.accept(pair);return;}
                for(var part:split(point))mapper.accept(com.mojang.datafixers.util.Pair.of(part.getFirst(),part.getSecond()?TaintBiomes.TAINTED_LAND:pair.getSecond()));
            });
        }
        static java.util.List<com.mojang.datafixers.util.Pair<net.minecraft.world.level.biome.Climate.ParameterPoint,Boolean>> split(net.minecraft.world.level.biome.Climate.ParameterPoint p){
            var out=new java.util.ArrayList<com.mojang.datafixers.util.Pair<net.minecraft.world.level.biome.Climate.ParameterPoint,Boolean>>();
            // Middle third of the host humidity range: patches about the size of a cherry grove.
            var hum=slices(p.humidity(),Double.parseDouble(System.getProperty("tc4modern.taintBand",".3334")));var weird=java.util.List.of(p.weirdness());
            for(int h=0;h<hum.size();h++)for(int w=0;w<weird.size();w++){
                var point=new net.minecraft.world.level.biome.Climate.ParameterPoint(p.temperature(),hum.get(h),p.continentalness(),p.erosion(),p.depth(),weird.get(w),p.offset());
                boolean centre=hum.size()==3&&h==1;
                out.add(com.mojang.datafixers.util.Pair.of(point,centre));
            }
            return out;
        }
        /** Three contiguous slices: the middle 1/parts..(parts-1)/parts band and the two edges; a degenerate range stays whole. */
        static java.util.List<net.minecraft.world.level.biome.Climate.Parameter> slices(net.minecraft.world.level.biome.Climate.Parameter r,double middle){
            long min=r.min(),max=r.max(),w=max-min,a=min+(long)(w*(1-middle)/2),b=max-(long)(w*(1-middle)/2);
            if(w<3||a>=b)return java.util.List.of(r);
            return java.util.List.of(new net.minecraft.world.level.biome.Climate.Parameter(min,a),new net.minecraft.world.level.biome.Climate.Parameter(a,b),new net.minecraft.world.level.biome.Climate.Parameter(b,max));
        }
    }
    static final class Trees extends Feature<NoneFeatureConfiguration> {
        Trees(){super(NoneFeatureConfiguration.CODEC);}
        @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            var level=c.level();var rng=c.random();int count=2+(rng.nextInt(10)==0?1:0);boolean placed=false;
            for(int i=0;i<count;i++) {
                var pos=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,c.origin().offset(rng.nextInt(16),0,rng.nextInt(16)));
                if(rng.nextInt(8)==0)placed|=MagicalForestTrees.growBigOak(level,rng,pos);
                // The heightmap stops on water; the vanilla oak feature has no soil check and would lay its own dirt block
                // there. The original WorldGenTrees needed sustaining soil below the trunk.
                else if(!level.getBlockState(pos.below()).is(net.minecraft.tags.BlockTags.DIRT)||!level.getFluidState(pos).isEmpty())continue;
                else placed|=level.registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE).getOrThrow(net.minecraft.data.worldgen.features.TreeFeatures.OAK).place(level,c.chunkGenerator(),rng,pos);
            }
            return placed;
        }
    }
    static void placeFibre(WorldGenLevel level,BlockPos pos) {
        var block=(TaintFibresBlock)TC4Modern.TAINT_FIBRES.get();
        var state=block.attach(block.defaultBlockState(),level,pos);
        if(TaintFibresBlock.FACES.values().stream().anyMatch(state::getValue))level.setBlock(pos,state,2);
    }
    static final class Decorations extends Feature<NoneFeatureConfiguration> {
        Decorations(){super(NoneFeatureConfiguration.CODEC);}
        @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> c) {
            var level=c.level();var rng=c.random();var origin=c.origin();
            int blobs=rng.nextInt(3);
            for(int i=0;i<blobs;i++) {
                var pos=level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG,origin.offset(rng.nextInt(16)+8,0,rng.nextInt(16)+8));
                Feature.FOREST_ROCK.place(new BlockStateConfiguration(TC4Modern.TAINT.get().defaultBlockState()),level,c.chunkGenerator(),rng,pos);
            }
            for(int i=0;i<10;i++) {
                var pos=level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG,origin.offset(rng.nextInt(16),0,rng.nextInt(16))).below();
                var ground=level.getBlockState(pos);
                if(ground.is(Blocks.GRASS_BLOCK))placeFibre(level,pos.above());
                else if(ground.canBeReplaced()&&level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK))placeFibre(level,pos);
            }
            var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(TaintBiomes.TAINTED_LAND);
            for(int i=0;i<8;i++) {
                int x=origin.getX()+rng.nextInt(16),z=origin.getZ()+rng.nextInt(16);
                // The legacy helper starts at 10 and finds the first uncovered space, including caves.
                var pos=new BlockPos(x,Math.max(11,level.getMinBuildHeight()+1),z);
                while(pos.getY()<level.getMaxBuildHeight()-1&&!level.isEmptyBlock(pos))pos=pos.above();
                if(!level.getBiome(pos).is(TaintBiomes.TAINTED_LAND))TaintBiomes.setColumnInChunk(level,pos,biome);
                if(!level.isEmptyBlock(pos))continue;
                for(var face:Direction.values())if(level.getBlockState(pos.relative(face)).isSolidRender(level,pos.relative(face))) {
                    placeFibre(level,pos);break;
                }
            }
            return true;
        }
    }
}
