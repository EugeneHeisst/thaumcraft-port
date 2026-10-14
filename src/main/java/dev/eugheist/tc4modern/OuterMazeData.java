package dev.eugheist.tc4modern;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import thaumcraft.common.lib.world.dim.Cell;
import thaumcraft.common.lib.world.dim.MazeGenerator;

/** Original labyrinth cells, owned by one save rather than a static map shared across servers. */
@net.neoforged.fml.common.EventBusSubscriber(modid=TC4Modern.MODID)
public final class OuterMazeData extends SavedData {
    private static final Factory<OuterMazeData> FACTORY=new Factory<>(OuterMazeData::new,OuterMazeData::load,null);
    private volatile Map<Long,Short> cells=Map.of();
    private static final Map<net.minecraft.server.MinecraftServer,OuterMazeData> SERVERS=new java.util.concurrent.ConcurrentHashMap<>();
    @net.neoforged.bus.api.SubscribeEvent static void starting(net.neoforged.neoforge.event.server.ServerStartingEvent e){get(e.getServer().overworld());}
    @net.neoforged.bus.api.SubscribeEvent static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e){SERVERS.remove(e.getServer());}

    public static OuterMazeData get(ServerLevel level){
        return SERVERS.computeIfAbsent(level.getServer(),server->{
            if(!server.isSameThread())throw new IllegalStateException("Maze data must load before terrain workers start");
            return server.overworld().getDataStorage().computeIfAbsent(FACTORY,"tc4modern_labyrinth");
        });
    }

    public Cell cell(int x,int z){Short value=cells.get(ChunkPos.asLong(x,z));return value==null?null:new Cell(value);}
    private int bossCount;
    public int nextBoss(net.minecraft.util.RandomSource random){bossCount++;if(random.nextFloat()<.25f)bossCount++;setDirty();return bossCount%4;}
    public int size(){return cells.size();}
    public boolean mazesInRange(int x,int z,int width,int height){
        for(int dx=-width;dx<=width;dx++)for(int dz=-height;dz<=height;dz++)
            if(cells.containsKey(ChunkPos.asLong(x+dx,z+dz)))return true;
        return false;
    }

    /** Original MazeThread mapping; generation itself takes milliseconds and never mutates terrain here. */
    public synchronized boolean create(int x,int z,int width,int height,long seed){
        if(width<15||width>29||height<15||height>29||(width&1)==0||(height&1)==0)
            throw new IllegalArgumentException("Original labyrinth dimensions must be odd, 15..29");
        if(mazesInRange(x,z,width,height))return false;
        MazeGenerator generator=new MazeGenerator(width,height,seed++);
        int attempts=0;
        while(!generator.generate()){
            if(++attempts>=10000)throw new IllegalStateException("Unable to generate labyrinth");
            generator=new MazeGenerator(width,height,seed++);
        }
        int column=x-(1+width/2),row=z-(1+height/2);
        // Readers on terrain workers see one complete immutable snapshot.
        Map<Long,Short> next=new LinkedHashMap<>(cells);
        for(int a=0;a<width;a++)for(int b=0;b<height;b++)
            if(generator.grid[b][a]>0)next.put(ChunkPos.asLong(column+a,row+b),(short)generator.grid[b][a]);
        cells=Map.copyOf(next);setDirty();return true;
    }

    static OuterMazeData load(CompoundTag tag,HolderLookup.Provider lookup){
        OuterMazeData data=new OuterMazeData();Map<Long,Short> loaded=new LinkedHashMap<>();ListTag list=tag.getList("cells",10);
        for(int i=0;i<list.size();i++){var cell=list.getCompound(i);short value=cell.getShort("cell");
            if(value>0)loaded.put(ChunkPos.asLong(cell.getInt("x"),cell.getInt("z")),value);}
        data.cells=Map.copyOf(loaded);data.bossCount=tag.getInt("bossCount");return data;
    }

    @Override public CompoundTag save(CompoundTag tag,HolderLookup.Provider lookup){
        ListTag list=new ListTag();cells.forEach((key,value)->{var cell=new CompoundTag();
            var pos=new ChunkPos(key);cell.putInt("x",pos.x);cell.putInt("z",pos.z);cell.putShort("cell",value);list.add(cell);});
        tag.put("cells",list);tag.putInt("bossCount",bossCount);return tag;
    }
}
