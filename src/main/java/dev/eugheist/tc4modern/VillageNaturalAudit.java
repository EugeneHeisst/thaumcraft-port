package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.StructureTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Fresh standard world, actual locate/start/chunk generation with the normal biome predicate. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class VillageNaturalAudit {
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_VILLAGE_NATURAL FAIL "+text);System.out.println("TC4_VILLAGE_NATURAL PASS "+text);}
    @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event){
        if(!Boolean.getBoolean("tc4modern.villageNaturalAudit"))return;
        var server=event.getServer();try{
            var level=server.overworld();int towers=0,homes=0;
            for(int n=0;n<8&&(towers==0||homes==0);n++){
                var found=level.findNearestMapStructure(StructureTags.VILLAGE,new BlockPos(n*4096,64,0),64,false);check(found!=null,"actual village locate near "+n+" returned "+found);
                var chunk=level.getChunkAt(found);var starts=chunk.getAllStarts().values().stream().filter(s->s.isValid()&&s.getPieces().stream().anyMatch(p->p instanceof PoolElementStructurePiece)).toList();check(!starts.isEmpty(),"actual generated village has persisted structure pieces");
                for(var start:starts){int w=0,b=0;for(var piece:start.getPieces()){
                    if(!(piece instanceof PoolElementStructurePiece pool)||!(pool.getElement() instanceof VillagePoolElement element))continue;
                    var box=piece.getBoundingBox();for(int x=box.minX()>>4;x<=box.maxX()>>4;x++)for(int z=box.minZ()>>4;z<=box.maxZ()>>4;z++)level.getChunk(x,z);
                    var local=element.wizard?new BlockPos(3,5,3):new BlockPos(0,2,2);var pos=local.rotate(pool.getRotation()).offset(pool.getPosition());
                    check(level.getBlockState(pos).is(element.wizard?Blocks.GLOWSTONE:Blocks.IRON_BARS),"actual natural "+(element.wizard?"tower":"banker home")+" survived normal multi-chunk placement at "+pos+" rotation="+pool.getRotation()+" state="+level.getBlockState(pos));
                    var door=(element.wizard?new BlockPos(3,1,1):new BlockPos(1,1,0)).rotate(pool.getRotation()).offset(pool.getPosition());check(level.getBlockState(door).is(Blocks.OAK_DOOR),"natural building retains its original entrance door");
                    if(!element.wizard){var state=level.getBlockState(pos);int connections=0;for(var property:java.util.List.of(net.minecraft.world.level.block.state.properties.BlockStateProperties.NORTH,net.minecraft.world.level.block.state.properties.BlockStateProperties.SOUTH,net.minecraft.world.level.block.state.properties.BlockStateProperties.EAST,net.minecraft.world.level.block.state.properties.BlockStateProperties.WEST))if(state.getValue(property))connections++;check(connections==2,"natural banker iron bars connect to both original plank walls");}
                    if(element.wizard)w++;else b++;
                }check(w<=1&&b<=1,"actual persisted village respects original zero-or-one quotas: "+w+"/"+b);towers+=w;homes+=b;}
            }
            check(towers>0&&homes>0,"fresh natural world includes both original buildings: "+towers+" towers / "+homes+" homes");
            System.out.println("TC4_VILLAGE_NATURAL ALL PASS actual fresh-world village locate, persisted pieces, normal biome gates, rotations, chunk placement and quotas");
        }finally{server.halt(false);}
    }
}
