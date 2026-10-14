package dev.eugheist.tc4modern;

import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Isolated server with nondefault config: exercise real feature gates and node rolls. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class WorldgenConfigAudit {
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_WORLDGEN_CONFIG FAIL "+text);System.out.println("TC4_WORLDGEN_CONFIG PASS "+text);}
    @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if(!Boolean.getBoolean("tc4modern.worldgenConfigAudit"))return;
        var server=event.getServer();var level=server.overworld();
        try {
            check(!WorldgenConfig.aura()&&!WorldgenConfig.structures()&&!WorldgenConfig.cinnabar()&&!WorldgenConfig.amber()&&!WorldgenConfig.infused()&&!WorldgenConfig.trees()&&!WorldgenConfig.taint(),"all seven generation switches read from isolated TOML");
            check(WorldgenConfig.nodeRarity()==1&&WorldgenConfig.specialRarity()==3&&WorldgenConfig.forestWeight()==0&&WorldgenConfig.taintWeight()==0,"nondefault rarity and zero biome weights loaded");
            var possible=level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
            check(possible.stream().noneMatch(b->b.is(MagicalForestContent.BIOME)||b.is(TaintBiomes.TAINTED_LAND)),"zero biome weights remove both natural climate regions");
            var origin=new BlockPos(320,180,320);
            for(int y=-1;y<=7;y++)level.setBlock(origin.above(y),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
            var context=new FeaturePlaceContext<>(Optional.empty(),level,level.getChunkSource().getGenerator(),RandomSource.create(42),origin,NoneFeatureConfiguration.INSTANCE);
            for(Feature<NoneFeatureConfiguration> feature:List.of(new AuraNodeFeature(),new ObsidianTotems.TotemFeature(),new HilltopStonesFeature(),new MoundFeature(),new CrimsonRingFeature(),new NaturalGreatwoodFeature(),SilverwoodContent.FEATURE.get(),new CinderpearlFeature(),new ShardOreFeature(),new CinnabarAmberFeature()))check(!feature.place(context),"actual disabled feature refuses: "+feature.getClass().getSimpleName());
            check(level.isEmptyBlock(origin)&&level.getBlockState(origin.below()).is(Blocks.STONE),"disabled features do not modify prepared terrain");
            WorldgenConfig.AURA.set(true);
            check(new AuraNodeFeature().place(context),"node rarity one generates an actual wild node");WorldgenConfig.AURA.set(false);
            var bounds=new java.util.HashSet<Integer>();
            var random=new net.minecraft.world.level.levelgen.LegacyRandomSource(42){@Override public int nextInt(int bound){bounds.add(bound);return super.nextInt(bound);}};
            var nodePos=origin.offset(10,0,10);level.removeBlock(nodePos,false);
            check(AuraNodeFeature.createRandomNodeAt(level,nodePos,random,false,false,false)&&bounds.contains(3)&&bounds.contains(1),"actual node generator uses configured special rarity three and modifier divisor one");
            WorldgenConfig.STRUCTURES.set(true);boolean tower=false;bounds.clear();
            var towerOrigin=origin.offset(32,0,32);for(int y=-1;y<=7;y++)level.setBlock(towerOrigin.above(y),y==-1?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
            for(int seed=0;seed<100&&!tower;seed++) {
                final int trial=seed;
                var roll=new net.minecraft.world.level.levelgen.LegacyRandomSource(trial){@Override public int nextInt(int bound){bounds.add(bound);return super.nextInt(bound);}};
                tower=new ObsidianTotems.TotemFeature().place(new FeaturePlaceContext<>(Optional.empty(),level,level.getChunkSource().getGenerator(),roll,towerOrigin,NoneFeatureConfiguration.INSTANCE));
            }
            check(tower&&bounds.contains(10),"actual totem rarity uses ten times configured node rarity");
            var end=server.getLevel(Level.END);
            check(!new ObsidianTotems.TotemFeature().place(new FeaturePlaceContext<>(Optional.empty(),end,end.getChunkSource().getGenerator(),RandomSource.create(1),origin,NoneFeatureConfiguration.INSTANCE)),"End rejects natural totems as original generation dispatcher does");
            WorldgenConfig.STRUCTURES.set(false);
            System.out.println("TC4_WORLDGEN_CONFIG ALL PASS real TOML gates, disabled climate regions, configured node rolls and overworld-only totems");
        }catch(Exception ex){throw new IllegalStateException("Worldgen config audit failed",ex);}finally{server.halt(false);}
    }
}
