package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import thaumcraft.common.lib.world.dim.Cell;
/** Tile-for-tile adapter for the original GenCommon pattern opcodes and decoration queues. */
class OuterGenCommon {
 static final int[][] PAT_CONNECT={{0,1,1,1,1,1,1,1,1,1,0},{1,8,8,8,8,8,8,8,8,8,1},{1,8,8,2,2,2,2,2,8,8,1},{1,8,2,5,9,9,9,6,2,8,1},{1,8,2,9,9,9,9,9,2,8,1},{1,8,2,9,9,9,9,9,2,8,1},{1,8,2,9,9,9,9,9,2,8,1},{1,8,2,3,9,9,9,4,2,8,1},{1,8,8,2,2,2,2,2,8,8,1},{1,8,8,8,8,8,8,8,8,8,1},{0,1,1,1,1,1,1,1,1,1,0}};
 static final class Decorations {final List<BlockPos> common=new ArrayList<>(),crabs=new ArrayList<>(),urns=new ArrayList<>();void remove(BlockPos p){common.remove(p);crabs.remove(p);urns.remove(p);}}
 static final ThreadLocal<Decorations> QUEUES=ThreadLocal.withInitial(Decorations::new);
 static boolean cosmetic(Block block){return block==OuterContent.STONE.get()||block==OuterContent.NO_SPAWN.get()||block==OuterContent.ROCK.get()||block==OuterContent.CRUST.get()||block==OuterContent.PEDESTAL.get();}
 static BlockState stairs(int metadata){var facing=switch(metadata&3){case 0->Direction.EAST;case 1->Direction.WEST;case 2->Direction.SOUTH;default->Direction.NORTH;};return OuterContent.STAIRS.get().defaultBlockState().setValue(StairBlock.FACING,facing).setValue(StairBlock.HALF,(metadata&4)!=0?Half.TOP:Half.BOTTOM);}
 static void placeBlock(WorldGenLevel world,int x,int y,int z,int code,Cell cell){placeBlock(world,x,y,z,code,null,cell);}
 static void placeBlock(WorldGenLevel world,int x,int y,int z,int code,Direction direction,Cell cell){
  var pos=new BlockPos(x,y,z);var old=world.getBlockState(pos);var r=world.getRandom();var q=QUEUES.get();BlockState state;
  switch(code){
   case 0:return;
   case 1:if(!old.isAir())return;state=Blocks.BEDROCK.defaultBlockState();break;
   case 15:case 16:q.remove(pos);state=(code==15?OuterGate.FRAME.get():OuterGate.LOCK.get()).defaultBlockState();break;
   case 17:state=OuterGate.BARRIER.get().defaultBlockState();break;
   case 99:state=Blocks.BEDROCK.defaultBlockState();break;
   case 8:state=OuterContent.VOID.get().defaultBlockState();break;
   case 9:q.remove(pos);state=Blocks.AIR.defaultBlockState();break;
   case 2:if(cell.feature==7&&r.nextInt(3)==0){placeBlock(world,x,y,z,21,direction,cell);return;}if(old.is(OuterContent.VOID.get()))return;if(r.nextInt(25)==0){boolean crab=cell.feature==7||r.nextInt(50)==0;if(crab&&(cell.feature==0||cell.feature==7))q.crabs.add(pos);else q.common.add(pos);}state=OuterContent.STONE.get().defaultBlockState();break;
   case 21:if(old.is(OuterContent.VOID.get()))return;state=OuterContent.CRUST.get().defaultBlockState();if(r.nextInt(25)==0){state=OuterContent.GLOWING.get().defaultBlockState();break;}if(r.nextInt(25)==0){boolean crab=cell.feature==7||(cell.feature==12&&r.nextBoolean())||r.nextInt(25)==0;if(crab&&(cell.feature==0||cell.feature==7||cell.feature==12))q.crabs.add(pos);}break;
   case 18:case 19:case 20:if(old.is(OuterContent.VOID.get()))return;state=(code==18?OuterContent.ROCK.get():code==19?OuterContent.NO_SPAWN.get():OuterDevices.TRAP.get()).defaultBlockState();break;
   case 7:state=OuterContent.GLOWING.get().defaultBlockState();break;
   case 10:case 11:state=OuterContent.STAIRS.get().defaultBlockState().setValue(StairBlock.FACING,direction==null?Direction.EAST:direction).setValue(StairBlock.HALF,code==11?Half.TOP:Half.BOTTOM);break;
   case 3:case 4:case 5:case 6:int metadata=0;if(direction!=null){boolean ns=direction.getAxis()==Direction.Axis.Z;boolean ew=direction.getAxis()==Direction.Axis.X;if(ns)metadata=(code==3?1:code==4?0:code==5?5:4);else if(ew)metadata=(code==3?3:code==4?2:code==5?7:6);}if((code==3||code==4)&&r.nextFloat()<.005)q.urns.add(pos);state=stairs(metadata);break;
   default:throw new IllegalArgumentException("Unsupported original maze opcode "+code);
  }
  world.setBlock(pos,state,3);
 }
 static boolean adjacentDevice(WorldGenLevel world,BlockPos pos){for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){if(x==0&&y==0&&z==0)continue;var b=world.getBlockState(pos.offset(x,y,z)).getBlock();if(EldritchBlock.isStructure(b)||b instanceof OuterDevices.DeviceBlock||b instanceof OuterGate.LockBlock||b==OuterGate.FRAME.get()||b==OuterContent.GLOWING.get()||b==OuterContent.GLYPHED.get())return true;}return false;}
 static int exposed(WorldGenLevel world,BlockPos pos){int count=0;for(var d:Direction.values())if(world.isEmptyBlock(pos.relative(d)))count++;return count;}
 static boolean isBedrockShowing(WorldGenLevel world,int x,int y,int z){var pos=new BlockPos(x,y,z);for(var d:Direction.values()){var other=world.getBlockState(pos.relative(d.getOpposite()));if(!world.getBlockState(pos.relative(d)).canOcclude()&&(other.is(Blocks.BEDROCK)||other.is(OuterContent.VOID.get())))return true;}return false;}
 static void genObelisk(WorldGenLevel world,int x,int y,int z){var pos=new BlockPos(x,y,z);world.setBlock(pos,CrimsonContent.OBELISK.get().defaultBlockState(),3);for(int i=1;i<=4;i++)world.setBlock(pos.above(i),CrimsonContent.OBELISK_BODY.get().defaultBlockState(),3);}
 static void processDecorations(WorldGenLevel world){var q=QUEUES.get();var r=world.getRandom();try{
  for(var p:q.urns)if(world.isEmptyBlock(p.above())){world.setBlock(p,OuterContent.PEDESTAL.get().defaultBlockState(),3);float roll=r.nextFloat();world.setBlock(p.above(),OuterLoot.URNS.get(roll<.025?2:roll<.1?1:0).get().defaultBlockState(),3);}
  for(var p:q.common){int count=exposed(world,p);if(count<=0||(count!=1&&isBedrockShowing(world,p.getX(),p.getY(),p.getZ()))||adjacentDevice(world,p))continue;int type=r.nextInt(3)!=0?4:r.nextInt(8)!=0?5:10;world.setBlock(p,(type==4?OuterContent.GLOWING.get():type==5?OuterContent.GLYPHED.get():OuterDevices.TRAP.get()).defaultBlockState(),3);if(type==4&&r.nextInt(12)==0)for(var d:Direction.values())if(world.isEmptyBlock(p.relative(d))){world.setBlock(p.relative(d),OuterCrystal.BLOCK.get().defaultBlockState().setValue(BlockStateProperties.FACING,d),3);break;}}
  for(var p:q.crabs){if(exposed(world,p)!=1||adjacentDevice(world,p))continue;for(var d:Direction.values())if(world.isEmptyBlock(p.relative(d))){world.setBlock(p,OuterDevices.VENT.get().defaultBlockState().setValue(BlockStateProperties.FACING,d),3);break;}}
 }finally{QUEUES.remove();}}
 static void generateConnections(WorldGenLevel world,Random random,int cx,int cz,int y,Cell cell,int depth,boolean tip){int x=cx*16,z=cz*16;for(var direction:List.of(Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST)){boolean connected=switch(direction){case NORTH->cell.north;case SOUTH->cell.south;case EAST->cell.east;default->cell.west;};if(!connected)continue;for(int d=0;d<=depth;d++){int start=tip&&d==depth?2:tip&&d==depth-1?1:0,end=tip&&d==depth?9:tip&&d==depth-1?10:11;for(int w=start;w<end;w++)for(int h=start;h<end;h++){int code=PAT_CONNECT[h][w];if(d==depth&&tip&&code==8)continue;int px=switch(direction){case EAST->x+16-d;case WEST->x+d;default->x+3+w;};int pz=switch(direction){case NORTH->z+d;case SOUTH->z+16-d;default->z+3+w;};placeBlock(world,px,y+10-h,pz,code,direction,cell);}}}}
}
