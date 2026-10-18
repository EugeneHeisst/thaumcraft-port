package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.biome.FeatureSorter;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Locate legacy features without migrating, duplicating or changing their generation.
 * The decoration RNG rejects impossible chunks; only real generated blocks can produce a result.
 * Searches are incremental to avoid a single command generating an entire search area in one tick.
 */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class FeatureLocate {
    static final List<String> IDS=List.of("mound","hilltop_stones","eldritch_ring","obsidian_totem");
    private static final net.minecraft.server.level.TicketType<net.minecraft.world.level.ChunkPos> TICKET=
        net.minecraft.server.level.TicketType.create("tc4modern_locate",Comparator.comparingLong(net.minecraft.world.level.ChunkPos::toLong),40);
    private static final Map<net.minecraft.server.MinecraftServer,Job> JOBS=new IdentityHashMap<>();
    @SubscribeEvent static void register(RegisterCommandsEvent e) {
        var structures=Commands.literal("structure");
        for(String id:IDS)structures.then(Commands.literal("tc4modern:"+id).executes(c->start(c.getSource(),id)));
        e.getDispatcher().register(Commands.literal("locate").requires(s->s.hasPermission(2)).then(structures));
    }
    private static int start(CommandSourceStack source,String id) {
        var level=source.getLevel();
        if(!WorldgenConfig.structures()||!level.dimension().equals(Level.OVERWORLD)
            ||level.getChunkSource().getGenerator() instanceof FlatLevelSource) {
            source.sendFailure(Component.literal("Thaumcraft structure generation is disabled or unavailable in this dimension/world."));return 0;
        }
        if(JOBS.containsKey(level.getServer())) {source.sendFailure(Component.literal("A Thaumcraft structure search is already running."));return 0;}
        var job=new Job(source,id);
        if(job.index<0) {source.sendFailure(Component.literal("This feature is absent from this world's biome generation settings."));return 0;}
        JOBS.put(level.getServer(),job);
        source.sendSuccess(()->Component.literal("Searching for tc4modern:"+id+" within 100 chunks (generates candidate chunks; may take a while)…"),false);
        return 1;
    }
    @SubscribeEvent static void tick(ServerTickEvent.Post e) {
        var job=JOBS.get(e.getServer());if(job==null)return;
        try {if(job.step()){job.release();JOBS.remove(e.getServer());}}
        catch(RuntimeException ex) {job.release();JOBS.remove(e.getServer());job.source.sendFailure(Component.literal("Thaumcraft search failed: "+ex.getMessage()));com.mojang.logging.LogUtils.getLogger().error("Thaumcraft locate failed",ex);}
    }
    @SubscribeEvent static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e){JOBS.remove(e.getServer());}

    static final class Job {
        final CommandSourceStack source;final ServerLevel level;final String id;final BlockPos from;
        final PlacedFeature feature;final int index;final List<net.minecraft.world.level.ChunkPos> chunks=new ArrayList<>();
        int cursor,generated;
        final List<net.minecraft.world.level.ChunkPos> pendingChunks=new ArrayList<>();
        final List<java.util.concurrent.CompletableFuture<net.minecraft.server.level.ChunkResult<net.minecraft.world.level.chunk.ChunkAccess>>> pending=new ArrayList<>();
        void release(){for(var p:pendingChunks)level.getChunkSource().removeRegionTicket(TICKET,p,0,p);pendingChunks.clear();pending.clear();}
        Job(CommandSourceStack source,String id) {
            this.source=source;this.level=source.getLevel();this.id=id;from=BlockPos.containing(source.getPosition());
            var generator=level.getChunkSource().getGenerator();
            var steps=FeatureSorter.buildFeaturesPerStep(List.copyOf(generator.getBiomeSource().possibleBiomes()),b->generator.getBiomeGenerationSettings(b).features(),true);
            feature=level.registryAccess().registryOrThrow(Registries.PLACED_FEATURE).get(AlchemyContent.id(id));
            int step=GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal();
            index=step<steps.size()?steps.get(step).features().indexOf(feature):-1;
            if(index<0)return;
            int cx=from.getX()>>4,cz=from.getZ()>>4;
            for(int x=cx-100;x<=cx+100;x++)for(int z=cz-100;z<=cz+100;z++) {
                if(!level.getWorldBorder().isWithinBounds(new BlockPos(x*16,64,z*16)))continue;
                if(candidate(x,z))chunks.add(new net.minecraft.world.level.ChunkPos(x,z));
            }
            chunks.sort(Comparator.comparingDouble(p->distance(p.getMiddleBlockX(),p.getMiddleBlockZ())));
        }
        double distance(int x,int z){return (double)(x-from.getX())*(x-from.getX())+(double)(z-from.getZ())*(z-from.getZ());}
        boolean candidate(int x,int z) {
            var random=new WorldgenRandom(new XoroshiroRandomSource(0));
            long seed=random.setDecorationSeed(level.getSeed(),x*16,z*16);
            random.setFeatureSeed(seed,index,GenerationStep.Decoration.SURFACE_STRUCTURES.ordinal());
            // Evaluate the real leading rarity modifier, so datapack chance changes are respected.
            if(!feature.placement().isEmpty() && feature.placement().getFirst() instanceof net.minecraft.world.level.levelgen.placement.RarityFilter filter)
                return filter.getPositions(null,random,BlockPos.ZERO).findAny().isPresent();
            // The standard totem has no rarity placement: its own roll follows InSquarePlacement.
            // Unknown datapack layouts conservatively scan all chunks rather than miss buildings.
            if(id.equals("obsidian_totem") && feature.feature().value().feature() instanceof ObsidianTotems.TotemFeature && feature.placement().size()==3
                && feature.placement().get(0) instanceof net.minecraft.world.level.levelgen.placement.InSquarePlacement
                && feature.placement().get(1) instanceof net.minecraft.world.level.levelgen.placement.HeightmapPlacement
                && feature.placement().get(2) instanceof net.minecraft.world.level.levelgen.placement.BiomeFilter) {
                random.nextInt(16);random.nextInt(16);
                return random.nextInt(WorldgenConfig.nodeRarity()*16)==0;
            }
            return true;
        }
        boolean step() {
            if(!WorldgenConfig.structures()) {source.sendFailure(Component.literal("Thaumcraft structure generation was disabled."));return true;}
            if(pending.isEmpty()) {
                if(cursor>=chunks.size()) {source.sendFailure(Component.literal("No intact tc4modern:"+id+" found within 100 chunks; checked "+generated+" seed candidates."));return true;}
                var chunk=chunks.get(cursor++);generated++;
                if(generated%25==0)source.sendSuccess(()->Component.literal("Still searching for tc4modern:"+id+" ("+generated+"/"+chunks.size()+" seed candidates)…"),false);
                // Finish east/south neighbors too: mound width is 19, anchor is 0..13.
                for(int dx=0;dx<=1;dx++)for(int dz=0;dz<=1;dz++) {
                    var p=new net.minecraft.world.level.ChunkPos(chunk.x+dx,chunk.z+dz);pendingChunks.add(p);
                    level.getChunkSource().addRegionTicket(TICKET,p,0,p);
                    // Off-thread entry into vanilla's future API queues generation on its own scheduler.
                    // Calling this API directly on the server thread would managedBlock until completion.
                    pending.add(java.util.concurrent.CompletableFuture.supplyAsync(()->level.getChunkSource()
                        .getChunkFuture(p.x,p.z,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true)).thenCompose(f->f));
                }
                return false;
            }
            for(var p:pendingChunks)level.getChunkSource().addRegionTicket(TICKET,p,0,p);
            if(pending.stream().anyMatch(f->!f.isDone()))return false;
            var positions=new ArrayList<BlockPos>();
            for(var future:pending)positions.addAll(future.join().orElseThrow(()->new IllegalStateException("Candidate chunk unavailable")).getBlockEntitiesPos());
            BlockPos found=positions.stream().filter(p->matches(level,id,p)).min(Comparator.comparingDouble(p->distance(p.getX(),p.getZ()))).orElse(null);
            release();
            if(found==null)return false;
            int distance=(int)Math.sqrt(distance(found.getX(),found.getZ()));
            int safeY=level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,found.getX(),found.getZ())+1;
            var coords=Component.literal("["+found.getX()+", "+found.getY()+", "+found.getZ()+"]")
                .withStyle(Style.EMPTY.withColor(net.minecraft.ChatFormatting.GREEN).withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/tp @s "+found.getX()+" "+safeY+" "+found.getZ())));
            source.sendSuccess(()->Component.literal("Found tc4modern:"+id+" at ").append(coords).append(" ("+distance+" blocks away; verified generated blocks)"),false);
            return true;
        }
    }
    private static net.minecraft.world.level.block.state.BlockState stateAt(ServerLevel level,BlockPos p) {
        var chunk=level.getChunkSource().getChunkNow(p.getX()>>4,p.getZ()>>4);
        return chunk==null?Blocks.AIR.defaultBlockState():chunk.getBlockState(p);
    }
    static boolean matches(ServerLevel level,String id,BlockPos p) {
        var state=stateAt(level,p);
        return switch(id) {
            case "mound" -> (state.is(Blocks.CHEST)||state.is(Blocks.TRAPPED_CHEST))
                &&stateAt(level,p.offset(-6,4,-5)).is(Blocks.SPAWNER)
                &&stateAt(level,p.offset(-6,4,5)).is(Blocks.SPAWNER)
                &&stateAt(level,p.offset(-1,7,0)).is(TC4Modern.AURA_NODE.get());
            case "hilltop_stones" -> state.is(Blocks.SPAWNER)
                &&stateAt(level,p.above()).is(CrimsonContent.OBSIDIAN_TILE.get())
                &&stateAt(level,p.above(2)).is(Blocks.CHEST)
                &&stateAt(level,p.offset(3,1,1)).is(ObsidianTotems.PLAIN.get());
            case "eldritch_ring" -> state.is(CrimsonContent.ALTAR.get())
                &&stateAt(level,p.above(2)).is(CrimsonContent.OBELISK.get())
                &&stateAt(level,p.above(3)).is(CrimsonContent.OBELISK_BODY.get());
            case "obsidian_totem" -> state.is(ObsidianTotems.CHARGED.get())&&stateAt(level,p.below()).is(ObsidianTotems.PLAIN.get());
            default -> false;
        };
    }
}
