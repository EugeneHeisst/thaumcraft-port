package dev.eugheist.tc4modern;

import java.util.*;
import java.util.concurrent.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkType;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * EventHandlerWorld.chunkSave/chunkLoad + ServerTickEventsFML.tickChunkRegeneration: every saved chunk carries
 * Thaumcraft.{regen_key}; a full chunk loaded without it is queued while any World_Regeneration switch is on, and up to
 * ten queued chunks per dimension and tick re-run the TC generators with newGen=false. As in the original, only the
 * switched-on generators run (WorldgenConfig.REGENERATING), obsidian totems never regenerate, and the Outer Lands are
 * left alone (their maze has its own generator here).
 */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class Retrogen {
    private Retrogen() {}
    static final Map<ResourceKey<Level>,Queue<ChunkPos>> QUEUE=new ConcurrentHashMap<>();
    static final org.slf4j.Logger LOGGER=com.mojang.logging.LogUtils.getLogger();
    static int queued,regenerated;
    /** ThaumcraftWorldGenerator.generateSurface order: vegetation, ores, aura nodes, structures. */
    static final List<String> SURFACE=List.of("silverwood","natural_greatwood","cinderpearl","cinnabar_amber","infused_stone","aura_node","mound","hilltop_stones","eldritch_ring");
    /** generateNether: the totem needs newGen, so only wild nodes. */
    static final List<String> NETHER=List.of("aura_node_nether");

    @SubscribeEvent static void save(ChunkDataEvent.Save event){
        if(!(event.getLevel() instanceof ServerLevel))return;
        var data=event.getData();var tc=data.getCompound("Thaumcraft");tc.putBoolean(WorldgenConfig.regenKey(),true);data.put("Thaumcraft",tc);
    }
    static boolean needsRegen(CompoundTag data){return !data.getCompound("Thaumcraft").contains(WorldgenConfig.regenKey())&&WorldgenConfig.anyRegen();}
    @SubscribeEvent static void load(ChunkDataEvent.Load event){
        if(event.getType()!=ChunkType.LEVELCHUNK||!(event.getLevel() instanceof ServerLevel level)||!needsRegen(event.getData()))return;
        var pos=event.getChunk().getPos();
        LOGGER.warn("[Thaumcraft] World gen was never run for chunk at {}. Adding to queue for regeneration.",pos);
        QUEUE.computeIfAbsent(level.dimension(),k->new ConcurrentLinkedQueue<>()).add(pos);queued++;
    }
    @SubscribeEvent static void tick(LevelTickEvent.Post event){
        if(!(event.getLevel() instanceof ServerLevel level))return;var queue=QUEUE.get(level.dimension());if(queue==null)return;
        for(int a=0;a<10;a++){var pos=queue.poll();if(pos==null)return;regenerate(level,pos);}
    }
    static void regenerate(ServerLevel level,ChunkPos pos){
        if(level.dimension().equals(OuterDimension.KEY))return;
        var names=level.dimension().equals(Level.NETHER)?NETHER:level.dimension().equals(Level.END)?List.<String>of():SURFACE;
        var chunk=level.getChunk(pos.x,pos.z);var generator=level.getChunkSource().getGenerator();var registry=level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
        var random=new WorldgenRandom(new net.minecraft.world.level.levelgen.LegacyRandomSource(level.getSeed()));long seed=random.setDecorationSeed(level.getSeed(),pos.getMinBlockX(),pos.getMinBlockZ());
        WorldgenConfig.REGENERATING.set(true);
        try{for(int i=0;i<names.size();i++){var feature=registry.getHolder(ResourceKey.create(Registries.PLACED_FEATURE,AlchemyContent.id(names.get(i))));if(feature.isEmpty())continue;
            random.setFeatureSeed(seed,i,net.minecraft.world.level.levelgen.GenerationStep.Decoration.TOP_LAYER_MODIFICATION.ordinal());
            feature.get().value().placeWithBiomeCheck(level,generator,random,pos.getWorldPosition());}}
        finally{WorldgenConfig.REGENERATING.set(false);}
        chunk.setUnsaved(true);regenerated++;
    }
}
