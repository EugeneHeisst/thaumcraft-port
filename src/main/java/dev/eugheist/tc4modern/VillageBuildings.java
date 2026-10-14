package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.util.RandomSource;

/** Original ComponentWizardTower / ComponentBankerHome placements; only world-coordinate and block-state adapters. */
final class VillageBuildings {
    final WorldGenLevel world;final BlockPos origin;final Rotation rotation;final BoundingBox clip;
    VillageBuildings(WorldGenLevel world,BlockPos origin,Rotation rotation,BoundingBox clip){this.world=world;this.origin=origin;this.rotation=rotation;this.clip=clip;}
    BlockPos pos(int x,int y,int z){return new BlockPos(x,y,z).rotate(rotation).offset(origin);}
    void set(int x,int y,int z,BlockState state){var p=pos(x,y,z);if(clip.isInside(p))world.setBlock(p,(state.getBlock() instanceof net.minecraft.world.level.block.CrossCollisionBlock||state.getBlock() instanceof net.minecraft.world.level.block.WallBlock)?net.minecraft.world.level.block.Block.updateFromNeighbourShapes(state.rotate(rotation),world,p):state.rotate(rotation),3);}
    void fill(int x1,int y1,int z1,int x2,int y2,int z2,BlockState state){for(int x=x1;x<=x2;x++)for(int y=y1;y<=y2;y++)for(int z=z1;z<=z2;z++)set(x,y,z,state);}
    void support(int x,int z,int clearY){
        var p=pos(x,0,z);if(p.getX()<clip.minX()||p.getX()>clip.maxX()||p.getZ()<clip.minZ()||p.getZ()>clip.maxZ())return;
        for(int y=clearY;origin.getY()+y<world.getMaxBuildHeight()&&!world.isEmptyBlock(pos(x,y,z));y++)set(x,y,z,Blocks.AIR.defaultBlockState());
        for(int y=-1;origin.getY()+y>world.getMinBuildHeight()+1;y--){var state=world.getBlockState(pos(x,y,z));if(!state.isAir()&&state.getFluidState().isEmpty())break;set(x,y,z,Blocks.COBBLESTONE.defaultBlockState());}
    }
    void door(int x,int z){var state=Blocks.OAK_DOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH);set(x,1,z,state);set(x,2,z,state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER));}
    void stairs(int x,int z){if(world.isEmptyBlock(pos(x,0,z))&&!world.isEmptyBlock(pos(x,-1,z)))set(x,0,z,Blocks.COBBLESTONE_STAIRS.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));}
    void villager(boolean wizard,int x,int z){var p=pos(x,1,z);if(!clip.isInside(p))return;var villager=(wizard?VillageContent.WIZARD:VillageContent.BANKER).get().create(world.getLevel());if(villager!=null){villager.moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,0,0);villager.finalizeSpawn(world,world.getCurrentDifficultyAt(p),net.minecraft.world.entity.MobSpawnType.STRUCTURE,null);world.addFreshEntity(villager);}}
    void wizard(RandomSource random){
        var air=Blocks.AIR.defaultBlockState();var planks=Blocks.OAK_PLANKS.defaultBlockState();var cobble=Blocks.COBBLESTONE.defaultBlockState();
        fill(2,1,2,4,11,4,air);for(int y:new int[]{0,5,10})fill(2,y,2,4,y,4,planks);
        fill(1,0,2,1,11,4,cobble);fill(2,0,1,4,11,1,cobble);fill(5,0,2,5,11,4,cobble);fill(2,0,5,4,11,5,cobble);
        for(int y:new int[]{0,5,10})for(int x:new int[]{1,5})for(int z:new int[]{1,5})set(x,y,z,cobble);
        for(int y:new int[]{7,8})for(int z:new int[]{1,5})set(3,y,z,Blocks.GLASS_PANE.defaultBlockState());for(int y:new int[]{2,3})set(3,y,5,Blocks.GLASS_PANE.defaultBlockState());
        for(int y=1;y<=9;y++)set(4,y,3,Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.WEST));
        set(4,10,3,Blocks.OAK_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.SOUTH).setValue(BlockStateProperties.OPEN,true));
        set(3,5,3,Blocks.GLOWSTONE.defaultBlockState());
        var chestPos=pos(2,6,2);if(clip.isInside(chestPos)){
            set(2,6,2,Blocks.CHEST.defaultBlockState());if(world.getBlockEntity(chestPos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest){
                chest.clearContent();for(int roll=4+random.nextInt(5);roll>0;roll--){int choice=random.nextInt(81),index=0;int[] weights={3,10,5,5,3,20,5,5,5,20};while(choice>=weights[index])choice-=weights[index++];
                    net.minecraft.world.item.ItemStack stack=switch(index){case 0->new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLOWSTONE_DUST,1+random.nextInt(3));case 1->new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE,1+random.nextInt(5));case 2->new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET,1+random.nextInt(3));case 3->new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.FIRE_CHARGE);case 4->new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SKELETON_SKULL);case 5->VillageTrades.mod("knowledge_fragment",1+random.nextInt(3));case 6->VillageTrades.mod("alumentum",1);case 7->VillageTrades.mod("nitor",1);case 8->VillageTrades.mod("thaumium_ingot",1+random.nextInt(2));default->new net.minecraft.world.item.ItemStack(TC4Modern.THAUMONOMICON.get());};chest.setItem(random.nextInt(chest.getContainerSize()),stack);
                }chest.setChanged();
            }
        }
        set(3,1,1,air);set(3,2,1,air);door(3,1);stairs(3,0);
        for(int z=0;z<12;z++)for(int x=0;x<5;x++)support(x,z,12);villager(true,7,1);
    }
    void banker(RandomSource random){
        boolean tall=random.nextBoolean();int table=random.nextInt(3);var air=Blocks.AIR.defaultBlockState();var cobble=Blocks.COBBLESTONE.defaultBlockState();var log=Blocks.OAK_LOG.defaultBlockState();var planks=Blocks.OAK_PLANKS.defaultBlockState();
        fill(1,1,1,3,5,4,air);fill(0,0,0,3,0,4,cobble);fill(1,0,1,2,0,3,Blocks.DIRT.defaultBlockState());fill(1,tall?4:5,1,2,tall?4:5,3,log);
        for(int x:new int[]{1,2})for(int z:new int[]{0,4})set(x,4,z,log);for(int x:new int[]{0,3})for(int z=1;z<=3;z++)set(x,4,z,log);
        for(int x:new int[]{0,3})for(int z:new int[]{0,4})fill(x,1,z,x,3,z,log);
        fill(0,1,1,0,3,3,planks);fill(3,1,1,3,3,3,planks);fill(1,1,0,2,3,0,planks);fill(1,1,4,2,3,4,planks);
        set(0,2,2,Blocks.IRON_BARS.defaultBlockState());set(3,2,2,Blocks.IRON_BARS.defaultBlockState());
        if(table>0){set(table,1,3,Blocks.COBBLESTONE_WALL.defaultBlockState());set(table,2,3,Blocks.STONE_PRESSURE_PLATE.defaultBlockState());}
        set(1,1,0,air);set(1,2,0,air);door(1,0);stairs(1,-1);
        for(int z=0;z<5;z++)for(int x=0;x<4;x++)support(x,z,6);villager(false,1,2);
    }
    static boolean place(WorldGenLevel world,BlockPos origin,Rotation rotation,BoundingBox clip,boolean wizard){
        // Stable per-piece choices are required because modern pieces are processed once per intersecting chunk.
        var random=RandomSource.create(world.getSeed()^origin.asLong()^(wizard?0x54433457495aL:0x54433442414e4bL));
        var building=new VillageBuildings(world,origin,rotation,clip);if(wizard)building.wizard(random);else building.banker(random);return true;
    }
}
