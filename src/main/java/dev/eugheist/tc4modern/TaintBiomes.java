package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/** Chunk persistence and client synchronization for the original column biome mutation. */
public final class TaintBiomes {
    public static final ResourceKey<Biome> TAINTED_LAND=ResourceKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"tainted_land"));

    public static boolean isTainted(ServerLevel level,BlockPos pos){return level.getBiome(pos).is(TAINTED_LAND);}

    public static void spread(ServerLevel level,BlockPos pos){
        var replacement=level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(TAINTED_LAND);
        setColumn(level,pos,replacement);
    }

    public static void setColumn(ServerLevel level,BlockPos pos,net.minecraft.core.Holder<Biome> replacement){
        int columnX=QuartPos.fromBlock(pos.getX()),columnZ=QuartPos.fromBlock(pos.getZ());
        // BiomeManager's Voronoi zoom can sample a neighbouring quart, including across a
        // chunk boundary. Updating only pos>>2 leaves some converted blocks in the old biome.
        int[] sampled={columnX,columnZ};
        level.getBiomeManager().withDifferentSource((x,y,z)->{sampled[0]=x;sampled[1]=z;return replacement;}).getBiome(pos);
        var columns=new java.util.LinkedHashSet<net.minecraft.world.level.ChunkPos>();
        columns.add(new net.minecraft.world.level.ChunkPos(columnX,columnZ));
        columns.add(new net.minecraft.world.level.ChunkPos(sampled[0],sampled[1]));
        var changed=new java.util.ArrayList<net.minecraft.world.level.chunk.ChunkAccess>();
        for(var column:columns){
            var chunk=level.getChunk(QuartPos.toSection(column.x),QuartPos.toSection(column.z));
            chunk.fillBiomesFromNoise((x,y,z,sampler)->x==column.x&&z==column.z?replacement:chunk.getNoiseBiome(x,y,z),level.getChunkSource().randomState().sampler());
            chunk.setUnsaved(true);if(!changed.contains(chunk))changed.add(chunk);
        }
        level.getChunkSource().chunkMap.resendBiomesForChunks(changed);
    }

    /**
     * World generation: change only the quart column inside the chunk being decorated. Following the Voronoi neighbour into
     * the next, not yet decorated chunk let Tainted Land decorations re-trigger there and spread over the whole world.
     */
    public static void setColumnInChunk(net.minecraft.world.level.WorldGenLevel world,BlockPos pos,net.minecraft.core.Holder<Biome> replacement){
        var chunk=world.getChunk(pos);int x=QuartPos.fromBlock(pos.getX()),z=QuartPos.fromBlock(pos.getZ());
        chunk.fillBiomesFromNoise((qx,qy,qz,sampler)->qx==x&&qz==z?replacement:chunk.getNoiseBiome(qx,qy,qz),world.getLevel().getChunkSource().randomState().sampler());chunk.setUnsaved(true);
    }

    /** Same quart-column mutation during chunk generation, without requesting live server chunks. */
    public static void setColumn(net.minecraft.world.level.WorldGenLevel world,BlockPos pos,net.minecraft.core.Holder<Biome> replacement){
        if(world instanceof ServerLevel server){setColumn(server,pos,replacement);return;}
        int x=QuartPos.fromBlock(pos.getX()),z=QuartPos.fromBlock(pos.getZ());int[] sampled={x,z};
        world.getBiomeManager().withDifferentSource((qx,qy,qz)->{sampled[0]=qx;sampled[1]=qz;return replacement;}).getBiome(pos);
        for(var column:new java.util.LinkedHashSet<>(java.util.List.of(new net.minecraft.world.level.ChunkPos(x,z),new net.minecraft.world.level.ChunkPos(sampled[0],sampled[1])))){
            var chunk=world.getChunk(QuartPos.toSection(column.x),QuartPos.toSection(column.z));
            chunk.fillBiomesFromNoise((qx,qy,qz,sampler)->qx==column.x&&qz==column.z?replacement:chunk.getNoiseBiome(qx,qy,qz),world.getLevel().getChunkSource().randomState().sampler());chunk.setUnsaved(true);
        }
    }

    private TaintBiomes(){}
}
