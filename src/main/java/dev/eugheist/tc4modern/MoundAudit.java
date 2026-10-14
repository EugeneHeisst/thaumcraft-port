package dev.eugheist.tc4modern;

import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.phys.AABB;

/** Native source-blueprint, dungeon loot/spawner and actual trapped-chest activation checks. */
final class MoundAudit {
    static BlockPos gallery(BlockPos base){return base.offset(128,-50,64);}
    static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_MOUND FAIL "+text);System.out.println("TC4_MOUND PASS "+text);}
    static void prepare(net.minecraft.server.level.ServerLevel level,BlockPos origin) {
        for(int x=-2;x<=20;x++)for(int z=-2;z<=20;z++)for(int y=-2;y<=18;y++)level.setBlock(origin.offset(x,y,z),y<=8?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
    }
    static void setup(ServerPlayer player,BlockPos base) {
        var level=player.serverLevel();var origin=gallery(base);prepare(level,origin);
        check(MoundFeature.build(level,origin,RandomSource.create(42)),"actual original mound builds on valid terrain");
        int compared=0,stairs=0,ladders=0;var stairVariants=new HashSet<String>();
        for(var block:MoundFeature.BLUEPRINT) {
            var pos=origin.offset(block.x(),block.y(),block.z());
            if(pos.equals(origin.offset(9,8,9)))continue;
            var actual=level.getBlockState(pos);var expected=block.state();
            checkBlock(actual.getBlock()==expected.getBlock(),pos,expected,actual);
            if(expected.is(Blocks.COBBLESTONE_STAIRS)) {
                checkBlock(actual.getValue(StairBlock.FACING)==expected.getValue(StairBlock.FACING)&&actual.getValue(StairBlock.HALF)==expected.getValue(StairBlock.HALF),pos,expected,actual);
                stairVariants.add(actual.getValue(StairBlock.FACING)+":"+actual.getValue(StairBlock.HALF));stairs++;
            }
            if(expected.is(Blocks.LADDER)){checkBlock(actual.getValue(net.minecraft.world.level.block.LadderBlock.FACING)==net.minecraft.core.Direction.EAST,pos,expected,actual);ladders++;}
            compared++;
        }
        check(compared>=2440&&stairs>40&&stairVariants.size()==8&&ladders==4,"all 2441 source placements, eight stair orientations and four ladders survive runtime updates");
        for(int z:new int[]{4,14}) {
            var spawner=(SpawnerBlockEntity)level.getBlockEntity(origin.offset(4,5,z));
            var entity=spawner.saveWithoutMetadata(level.registryAccess()).getCompound("SpawnData").getCompound("entity").getString("id");
            check(entity.equals(z==4?"minecraft:skeleton":"minecraft:zombie"),"original spawner entity "+entity);
        }
        var chest=(RandomizableContainerBlockEntity)level.getBlockEntity(origin.offset(10,1,9));chest.unpackLootTable(player);
        check(!chest.isEmpty(),"underground chest generates real dungeon loot");
        var node=(AuraNodeEntity)level.getBlockEntity(origin.offset(9,8,9));
        check(node!=null&&node.nodeType==thaumcraft.api.nodes.NodeType.DARK,"original eerie node over dungeon shaft");
        var test=origin.offset(48,0,0);boolean trap=false,normal=false;var rarities=new HashSet<Integer>();
        for(int seed=0;seed<24;seed++) {
            prepare(level,test);check(MoundFeature.build(level,test,RandomSource.create(seed)),"dynamic contents seed "+seed);
            for(int z:new int[]{7,11})for(int rarity=0;rarity<3;rarity++)if(level.getBlockState(test.offset(9,1,z)).is(OuterLoot.URNS.get(rarity).get())||level.getBlockState(test.offset(9,1,z)).is(OuterLoot.CRATES.get(rarity).get()))rarities.add(rarity);
            var chestPos=test.offset(10,1,9);
            if(level.getBlockState(chestPos).is(Blocks.TRAPPED_CHEST)&&!trap) {
                trap=true;var tnt=test.offset(10,-1,9);
                check(level.getBlockState(tnt).is(Blocks.TNT),"trapped chest hides original TNT two blocks below");
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                var trapped=(net.minecraft.world.level.block.entity.ChestBlockEntity)level.getBlockEntity(chestPos);
                trapped.startOpen(player);
                var explosives=level.getEntitiesOfClass(PrimedTnt.class,new AABB(tnt).inflate(2));
                check(level.isEmptyBlock(tnt)&&!explosives.isEmpty(),"opening actual trapped chest primes hidden TNT through floor");
                explosives.forEach(net.minecraft.world.entity.Entity::discard);trapped.stopOpen(player);
            }else if(level.getBlockState(chestPos).is(Blocks.CHEST))normal=true;
        }
        check(trap&&normal&&rarities.size()==3,"normal/trapped chests and all three urn/crate rarities occur");
        prepare(level,test);level.setBlock(test.offset(18,8,18),Blocks.SAND.defaultBlockState(),3);
        check(!MoundFeature.build(level,test,RandomSource.create(42))&&level.getBlockState(test.offset(0,8,4)).is(Blocks.STONE),"invalid far corner refuses before any blueprint writes");
        System.out.println("TC4_MOUND ALL PASS original blueprint, stairs, ladders, spawners, loot, rarity rolls, dark node and trapped-chest TNT");
    }
    private static void checkBlock(boolean good,BlockPos pos,Object expected,Object actual){if(!good)throw new IllegalStateException("TC4_MOUND FAIL original block "+pos+" expected "+expected+" actual "+actual);}
}
