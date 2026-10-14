package dev.eugheist.tc4modern;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

/** Exercise the actual structure builder, spawner, generated loot and rejection rules. */
final class HilltopAudit {
    static BlockPos gallery(BlockPos base){return base.offset(32,-4,24);}
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_HILLTOP FAIL "+text);System.out.println("TC4_HILLTOP PASS "+text);}
    static void setup(ServerPlayer player,BlockPos base) {
        var level=player.serverLevel();var origin=gallery(base);
        for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)for(int y=-4;y<=7;y++)level.setBlock(origin.offset(x,y,z),y<0?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        level.setBlock(origin.offset(-1,-1,-1),Blocks.AIR.defaultBlockState(),3);
        check(HilltopStonesFeature.build(level,origin,RandomSource.create(42)),"actual high mountain structure builds");
        int floors=0,pillars=0;
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++) {
            if(Math.abs(x)==3&&Math.abs(z)==3){check(level.isEmptyBlock(origin.offset(x,0,z)),"original floor corner excluded "+x+","+z);continue;}
            var state=level.getBlockState(origin.offset(x,0,z));
            check(x==0&&z==0?state.is(Blocks.SPAWNER):state.is(Blocks.OBSIDIAN)||state.is(CrimsonContent.OBSIDIAN_TILE.get()),"floor original palette "+x+","+z);floors++;
            if((Math.abs(x)==3&&Math.abs(z%2)==1)||(Math.abs(z)==3&&Math.abs(x%2)==1)) {
                int height=0;for(int y=1;y<=4;y++)if(level.getBlockState(origin.offset(x,y,z)).is(ObsidianTotems.PLAIN.get()))height++;
                check(height>=2&&height<=4,"pillar height at "+x+","+z);pillars++;
            }
        }
        check(floors==45&&pillars==8,"45 floor cells and eight original pillars");
        check(level.getBlockState(origin.offset(-1,-1,-1)).is(Blocks.DIRT),"air pocket filled beneath floor");
        var spawner=(SpawnerBlockEntity)level.getBlockEntity(origin);
        var spawn=spawner.saveWithoutMetadata(level.registryAccess()).getCompound("SpawnData").getCompound("entity").getString("id");
        check(spawn.equals("tc4modern:wisp"),"actual spawner NBT targets Thaumcraft wisp");
        check(level.getBlockState(origin.above()).is(CrimsonContent.OBSIDIAN_TILE.get()),"tile pedestal below chest");
        var chest=(RandomizableContainerBlockEntity)level.getBlockEntity(origin.above(2));
        check(chest.getLootTable()!=null&&chest.getLootTable().location().equals(AlchemyContent.id("chests/hilltop_stones")),"double dungeon loot table assigned");
        chest.unpackLootTable(player);int filled=0;for(int slot=0;slot<chest.getContainerSize();slot++)if(!chest.getItem(slot).isEmpty())filled++;
        check(filled>0,"real chest loot generates usable stacks");
        var node=(AuraNodeEntity)level.getBlockEntity(origin.above(5));
        check(node!=null&&node.nodeType==thaumcraft.api.nodes.NodeType.DARK&&node.reservoir.aspects.visSize()>0,"eerie aura node floats above shrine");
        var refused=origin.offset(24,0,0);for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)for(int y=-1;y<=3;y++)level.setBlock(refused.offset(x,y,z),y<0?Blocks.SAND.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
        check(!HilltopStonesFeature.build(level,refused,RandomSource.create(42))&&level.isEmptyBlock(refused),"sand substrate refuses without writing structure");
        var low=new BlockPos(origin.getX(),84,origin.getZ());level.setBlock(low.below(),Blocks.STONE.defaultBlockState(),3);level.removeBlock(low,false);
        check(!HilltopStonesFeature.valid(level,low),"below original mountain height 85 refuses");
        System.out.println("TC4_HILLTOP ALL PASS geometry, pillars, substrate, wisp spawner, actual chest loot and dark node");
    }
}
