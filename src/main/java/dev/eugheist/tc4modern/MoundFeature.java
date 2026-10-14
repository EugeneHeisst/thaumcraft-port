package dev.eugheist.tc4modern;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

/** Literal 2,441-block WorldGenMound blueprint plus original randomized dungeon contents. */
final class MoundFeature extends Feature<NoneFeatureConfiguration> {
    record Placement(int x,int y,int z,BlockState state){}
    static final List<Placement> BLUEPRINT=load();
    MoundFeature(){super(NoneFeatureConfiguration.CODEC);}
    static BlockState legacyState(String name,int metadata) {
        if(name.equals("stone_stairs"))return Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(StairBlock.FACING,switch(metadata&3){case 0->Direction.EAST;case 1->Direction.WEST;case 2->Direction.SOUTH;default->Direction.NORTH;}).setValue(StairBlock.HALF,(metadata&4)==0?Half.BOTTOM:Half.TOP);
        if(name.equals("ladder")) {
            if(metadata!=5)throw new IllegalArgumentException("Unexpected legacy ladder metadata "+metadata);
            return Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING,Direction.EAST);
        }
        return (switch(name) {
            case "air"->Blocks.AIR;case "dirt"->Blocks.DIRT;case "grass"->Blocks.GRASS_BLOCK;
            case "cobblestone"->Blocks.COBBLESTONE;case "mossy_cobblestone"->Blocks.MOSSY_COBBLESTONE;
            case "iron_bars"->Blocks.IRON_BARS;case "stonebrick"->Blocks.CHISELED_STONE_BRICKS;
            case "tallgrass"->Blocks.SHORT_GRASS;
            default->throw new IllegalArgumentException("Unsupported original mound block "+name);
        }).defaultBlockState();
    }
    private static List<Placement> load() {
        try(var input=MoundFeature.class.getResourceAsStream("/data/tc4modern/structures/mound.json")) {
            if(input==null)throw new IllegalStateException("Missing original mound blueprint");
            var root=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            var palette=new ArrayList<BlockState>();for(var entry:root.getAsJsonArray("palette")){var pair=entry.getAsJsonArray();palette.add(legacyState(pair.get(0).getAsString(),pair.get(1).getAsInt()));}
            var result=new ArrayList<Placement>();for(var entry:root.getAsJsonArray("blocks")){var p=entry.getAsJsonArray();result.add(new Placement(p.get(0).getAsInt(),p.get(1).getAsInt(),p.get(2).getAsInt(),palette.get(p.get(3).getAsInt())));}
            if(result.size()!=2441||root.get("placement_count").getAsInt()!=2441)throw new IllegalStateException("Incomplete original mound blueprint");
            return List.copyOf(result);
        }catch(java.io.IOException ex){throw new IllegalStateException("Cannot read original mound",ex);}
    }
    static boolean valid(WorldGenLevel level,BlockPos origin) {
        if(origin.getY()-1<level.getMinBuildHeight()||origin.getY()+16>=level.getMaxBuildHeight())return false;
        for(var offset:new int[][]{{9,9},{0,0},{18,0},{18,18},{0,18}}) {
            var pos=origin.offset(offset[0],9,offset[1]);boolean accepted=false;
            for(int up=0;up<=2;up++,pos=pos.above())if(level.isEmptyBlock(pos)) {
                var ground=level.getBlockState(pos.below());if(ground.is(Blocks.SNOW)||ground.is(Blocks.SHORT_GRASS))ground=level.getBlockState(pos.below(2));
                accepted=ground.is(Blocks.STONE)||ground.is(Blocks.GRASS_BLOCK)||ground.is(Blocks.DIRT);break;
            }
            if(!accepted)return false;
        }
        return true;
    }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if(!WorldgenConfig.structures())return false;
        var level=context.level();
        if(!level.getLevel().dimension().equals(Level.OVERWORLD)||level.getLevel().getChunkSource().getGenerator() instanceof FlatLevelSource)return false;
        // Width 19 must remain inside the writable center/neighbor chunks of WorldGenRegion.
        var anchor=new BlockPos((context.origin().getX()&~15)+context.random().nextInt(14),0,(context.origin().getZ()&~15)+context.random().nextInt(14));
        var origin=level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG,anchor).below(9);
        return build(level,origin,context.random());
    }
    static boolean build(WorldGenLevel level,BlockPos origin,RandomSource random) {
        if(!valid(level,origin))return false;
        for(var block:BLUEPRINT)level.setBlock(origin.offset(block.x,block.y,block.z),block.state,3);
        for(int z:new int[]{7,11}) {
            float roll=random.nextFloat();int rarity=roll<.1?2:roll<.33?1:0;
            var group=level.getRandom().nextFloat()<.3?OuterLoot.CRATES:OuterLoot.URNS;
            level.setBlock(origin.offset(9,1,z),group.get(rarity).get().defaultBlockState(),3);
        }
        boolean trapped=random.nextInt(3)==0;var chest=origin.offset(10,1,9);
        level.setBlock(chest,(trapped?Blocks.TRAPPED_CHEST:Blocks.CHEST).defaultBlockState(),3);
        if(trapped)level.setBlock(origin.offset(10,-1,9),Blocks.TNT.defaultBlockState(),3);
        if(level.getBlockEntity(chest) instanceof RandomizableContainerBlockEntity loot) {
            var table=random.nextInt(5)==0?ResourceKey.create(Registries.LOOT_TABLE,AlchemyContent.id("chests/hilltop_stones")):BuiltInLootTables.SIMPLE_DUNGEON;
            loot.setLootTable(table,random.nextLong());
        }
        for(int z:new int[]{4,14}) {
            var pos=origin.offset(4,5,z);level.setBlock(pos,Blocks.SPAWNER.defaultBlockState(),3);
            if(level.getBlockEntity(pos) instanceof SpawnerBlockEntity spawner)spawner.setEntityId(z==4?EntityType.SKELETON:EntityType.ZOMBIE,random);
        }
        AuraNodeFeature.createRandomNodeAt(level,origin.offset(9,8,9),random,false,true,false);
        return true;
    }
}
