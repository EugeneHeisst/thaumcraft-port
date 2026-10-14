package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Fresh normal-world biome sampling and real terrain generation, separate from the user's worlds. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class TaintedLandAudit {
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_TAINTED_LAND FAIL "+text);System.out.println("TC4_TAINTED_LAND PASS "+text);}
    @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if(!Boolean.getBoolean("tc4modern.taintedLandAudit"))return;
        var server=event.getServer();var level=server.overworld();
        try {
            var biome=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(TaintBiomes.TAINTED_LAND);
            check(biome.value().getGrassColor(0,0)==7160201&&biome.value().getFoliageColor()==8154503&&biome.value().getWaterColor()==0xCC1188,"original tainted biome colours");
            check(biome.value().getMobSettings().getMobs(MobCategory.CREATURE).isEmpty(),"original absence of passive animals");
            var monsters=biome.value().getMobSettings().getMobs(MobCategory.MONSTER).unwrap();
            check(monsters.stream().anyMatch(m->m.type==TaintacleContent.NORMAL.get())&&monsters.stream().anyMatch(m->m.type==PechContent.PECH.get())&&monsters.stream().noneMatch(m->net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.type).getNamespace().equals("minecraft")),"original tentacle and magical-biome Pech, no vanilla monster list");
            check(BiomeAspectBridge.types(biome).containsAll(java.util.List.of("MAGICAL","WASTELAND")),"original magical and wasteland biome aspect types");
            var found=level.findClosestBiome3d(b->b.is(TaintBiomes.TAINTED_LAND),new BlockPos(0,70,0),6400,64,128);
            check(found!=null,"tainted land occurs in actual normal-world climate source");
            var center=found.getFirst();int crust=0,fibres=0,attached=0,unsupported=0,logs=0,ores=0;var changed=level.getChunkSource().getGenerator().getBiomeSource().possibleBiomes();
            check(changed.stream().anyMatch(b->b.is(MagicalForestContent.BIOME)),"magical forest remains in climate source");
            for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++) {
                var chunk=level.getChunk((center.getX()>>4)+dx,(center.getZ()>>4)+dz);
                for(int sectionIndex=0;sectionIndex<chunk.getSections().length;sectionIndex++){var section=chunk.getSection(sectionIndex);if(!section.hasOnlyAir())for(int x=0;x<16;x++)for(int y=0;y<16;y++)for(int z=0;z<16;z++) {
                    var state=section.getBlockState(x,y,z);
                    if(state.is(TC4Modern.TAINT.get()))crust++;
                    if(state.is(TC4Modern.TAINT_FIBRES.get())){fibres++;if(TaintFibresBlock.FACES.values().stream().anyMatch(state::getValue))attached++;else {var pos=new BlockPos(chunk.getPos().getMinBlockX()+x,chunk.getSectionYFromSectionIndex(sectionIndex)*16+y,chunk.getPos().getMinBlockZ()+z);var refreshed=((TaintFibresBlock)state.getBlock()).attach(state,level,pos);if(TaintFibresBlock.FACES.values().stream().anyMatch(refreshed::getValue))throw new IllegalStateException("Generated fibre has stale invisible faces at "+pos);unsupported++;System.out.println("TC4_TAINTED_LAND NOTE unsupported fibre awaiting normal cleanup at "+pos);}}
                    if(state.is(Blocks.OAK_LOG))logs++;
                    if(state.getBlock() instanceof ShardOreBlock)ores++;
                }}
            }
            check(crust>0&&fibres>0&&attached+unsupported==fibres&&attached>fibres*.95,"natural crust blobs and visible attached fibres generate: "+crust+" / "+fibres+" / "+attached);
            check(logs>0,"ordinary/big oak trees generate in tainted terrain");
            check(ores>0,"overworld shard ores remain in naturally placed tainted biome");
            var text="TC4_TAINTED_LAND ALL PASS natural climate/25 chunks at "+center+" crust="+crust+" fibres="+fibres+" attached="+attached+" oakLogs="+logs+" shardOres="+ores+" original colours/spawns\n";
            java.nio.file.Files.writeString(java.nio.file.Path.of("tainted-land-audit.txt"),text);System.out.print(text);
        }catch(Exception ex){throw new IllegalStateException("Natural tainted land audit failed",ex);}finally{server.halt(false);}
    }
}
