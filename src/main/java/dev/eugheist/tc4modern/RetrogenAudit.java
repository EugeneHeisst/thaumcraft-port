package dev.eugheist.tc4modern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Dedicated audit of World_Regeneration: real chunk save marks, real unload/reload queueing and the switched-on generators only. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class RetrogenAudit {
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_RETROGEN FAIL "+text);System.out.println("TC4_RETROGEN PASS "+text);}
    static final ChunkPos CENTER=new ChunkPos(1400,-1400);static int stage,wait,afterFirst;static volatile boolean unloaded,centerQueued;static volatile int centerLoads;
    @SubscribeEvent static void loaded(net.neoforged.neoforge.event.level.ChunkDataEvent.Load e){if(Boolean.getBoolean("tc4modern.retrogenAudit")&&e.getChunk().getPos().equals(CENTER)){centerLoads++;centerQueued=Retrogen.needsRegen(e.getData());}}
    @SubscribeEvent static void unload(net.neoforged.neoforge.event.level.ChunkEvent.Unload e){if(Boolean.getBoolean("tc4modern.retrogenAudit")&&e.getChunk().getPos().equals(CENTER))unloaded=true;}static String failed;
    static int nodes(net.minecraft.server.level.ServerLevel l){int n=0;for(var be:l.getChunk(CENTER.x,CENTER.z).getBlockEntities().values())if(be instanceof AuraNodeEntity)n++;return n;}
    static void force(net.minecraft.server.level.ServerLevel l,boolean on){for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)l.setChunkForced(CENTER.x+x,CENTER.z+z,on);}
    static CompoundTag stored(net.minecraft.server.level.ServerLevel l){return l.getChunkSource().chunkMap.read(CENTER).join().orElseThrow();}
    @SubscribeEvent static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        if(!Boolean.getBoolean("tc4modern.retrogenAudit"))return;var server=event.getServer();var l=server.overworld();
        try{switch(stage){
            case 0->{
                check(!WorldgenConfig.anyRegen()&&WorldgenConfig.regenKey().equals("DEFAULT"),"original World_Regeneration defaults: key DEFAULT, every switch off");
                var tag=new CompoundTag();check(!Retrogen.needsRegen(tag),"with every switch off an unmarked chunk is not queued");
                WorldgenConfig.REGEN_KEY.set("first");WorldgenConfig.NODE_RARITY.set(1);force(l,true);
                check(nodes(l)>=1,"freshly generated chunk got its normal node at node_rarity 1: "+nodes(l));
                for(var be:java.util.List.copyOf(l.getChunk(CENTER.x,CENTER.z).getBlockEntities().values()))if(be instanceof AuraNodeEntity)l.removeBlock(be.getBlockPos(),false);
                check(nodes(l)==0,"nodes removed from the centre chunk");
                server.saveAllChunks(true,true,true);check(stored(l).getCompound("Thaumcraft").getBoolean("first"),"a real chunk save writes Thaumcraft.first");
                WorldgenConfig.REGEN_KEY.set("second");WorldgenConfig.REGEN_AURA.set(true);
                check(Retrogen.needsRegen(stored(l))&&!Retrogen.needsRegen(new CompoundTag(){{put("Thaumcraft",new CompoundTag(){{putBoolean("second",true);}});}}),"changing regen_key with aura_nodes on queues chunks without the new key only");
                WorldgenConfig.REGENERATING.set(true);check(WorldgenConfig.aura()&&!WorldgenConfig.structures()&&!WorldgenConfig.trees()&&!WorldgenConfig.cinnabar()&&!WorldgenConfig.amber()&&!WorldgenConfig.infused(),"while regenerating only switched-on generators run");WorldgenConfig.REGENERATING.set(false);
                check(WorldgenConfig.structures()&&WorldgenConfig.trees(),"ordinary generation keeps every generator");
                /* the reload below exercises every regenerating generator on the live level */for(var v:java.util.List.of(WorldgenConfig.REGEN_STRUCTURES,WorldgenConfig.REGEN_TREES,WorldgenConfig.REGEN_CINNABAR,WorldgenConfig.REGEN_AMBER,WorldgenConfig.REGEN_INFUSED))v.set(true);
                unloaded=false;force(l,false);stage=1;wait=0;}
            case 1->{if(unloaded){System.out.println("TC4_RETROGEN PASS centre chunk unloaded after "+wait+" ticks");stage=2;Retrogen.queued=0;Retrogen.regenerated=0;force(l,true);wait=0;}else if(++wait>1200)throw new IllegalStateException("chunk never unloaded");}
            case 2->{if(++wait<40)return;
                check(centerLoads==1&&centerQueued&&Retrogen.queued>=1,"reloading the saved chunk queued it for regeneration: "+Retrogen.queued);
                check(Retrogen.regenerated>=1&&Retrogen.QUEUE.values().stream().allMatch(java.util.Queue::isEmpty),"queued chunks regenerated within ticks: "+Retrogen.regenerated);
                check(nodes(l)>=1,"retrogen placed a wild node again in the centre chunk: "+nodes(l));
                server.saveAllChunks(true,true,true);check(stored(l).getCompound("Thaumcraft").getBoolean("second"),"the regenerated chunk is saved with the new key");
                afterFirst=Retrogen.queued;unloaded=false;force(l,false);stage=3;wait=0;}
            case 3->{if(unloaded){force(l,true);stage=4;wait=0;}else if(++wait>1200)throw new IllegalStateException("chunk never unloaded twice");}
            case 4->{if(++wait<40)return;check(centerLoads==2&&!centerQueued,"reloading the regenerated chunk from disk does not queue it again (loads "+centerLoads+")");
                for(var v:java.util.List.of(WorldgenConfig.REGEN_AURA,WorldgenConfig.REGEN_STRUCTURES,WorldgenConfig.REGEN_TREES,WorldgenConfig.REGEN_CINNABAR,WorldgenConfig.REGEN_AMBER,WorldgenConfig.REGEN_INFUSED))v.set(false);WorldgenConfig.REGEN_KEY.set("DEFAULT");WorldgenConfig.NODE_RARITY.set(36);WarpConfig.SPEC.save();
                System.out.println("TC4_RETROGEN ALL PASS");stage=5;server.halt(false);}
            default->{}
        }}catch(RuntimeException e){System.out.println("TC4_RETROGEN FAILED "+e);e.printStackTrace();stage=5;server.halt(false);}
    }
}
