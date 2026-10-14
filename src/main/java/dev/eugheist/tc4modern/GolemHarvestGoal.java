package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;

/** AIHarvestCrops: original home range/4, mature crops, ten-tick minimum work and dropped harvest. */
final class GolemHarvestGoal extends Goal {
 final StrawGolem g;BlockPos target;BlockState expected;int work=-1,total,timeout;final List<BlockPos> columns=new ArrayList<>();final ArrayDeque<BlockPos> adjacent=new ArrayDeque<>();BlockPos scanHome;int scanRange;
 GolemHarvestGoal(StrawGolem g){this.g=g;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 static boolean grown(net.minecraft.world.level.Level l,BlockPos p){var s=l.getBlockState(p);var b=s.getBlock();
  if(b instanceof CropBlock crop)return crop.isMaxAge(s);
  if(b instanceof NetherWartBlock)return s.getValue(NetherWartBlock.AGE)==3;
  if(b instanceof CocoaBlock)return s.getValue(CocoaBlock.AGE)==2;
  if(b instanceof ManaPodBlock)return s.getValue(ManaPodBlock.AGE)==7;
  if(b==Blocks.MELON||b==Blocks.PUMPKIN)return true;
  return (b instanceof SugarCaneBlock||b instanceof CactusBlock)&&l.getBlockState(p.below()).is(b);
 }
 boolean active(){return g.core()==3&&g.working();}
 @Override public boolean canUse(){
  if(!active()||g.tickCount%GolemConfig.delay()!=0)return false;int range=(int)Math.ceil(g.range()/4.0);
  if(!g.home.equals(scanHome)||range!=scanRange){columns.clear();adjacent.clear();scanHome=g.home;scanRange=range;}
  while(!adjacent.isEmpty()){var p=adjacent.removeFirst();if(grown(g.level(),p)){target=p;expected=g.level().getBlockState(p);return true;}}
  if(columns.isEmpty()){for(int x=-range;x<=range;x++)for(int z=-range;z<=range;z++)columns.add(g.home.offset(x,0,z));Collections.shuffle(columns,new Random(g.getRandom().nextLong()));}
  var column=columns.remove(columns.size()-1);for(int y=-3;y<=3;y++){var pos=column.offset(0,y,0);if(g.level().hasChunkAt(pos)&&grown(g.level(),pos)){target=pos;expected=g.level().getBlockState(pos);return true;}}return false;
 }
 void approach(){approach(g,target);}
 static void approach(StrawGolem g,BlockPos target){
  int feet=Math.min(target.getY(),(int)Math.floor(g.getY()+.1));net.minecraft.world.level.pathfinder.Path best=null;double closest=Double.MAX_VALUE;
  for(var side:net.minecraft.core.Direction.Plane.HORIZONTAL){var p=new BlockPos(target.getX()+side.getStepX(),feet,target.getZ()+side.getStepZ());if(!g.level().getBlockState(p).getCollisionShape(g.level(),p).isEmpty()||g.level().getBlockState(p.below()).getCollisionShape(g.level(),p.below()).isEmpty())continue;var path=g.getNavigation().createPath(p,0);double d=g.distanceToSqr(p.getX()+.5,p.getY(),p.getZ()+.5);if(path!=null&&path.canReach()&&d<closest){best=path;closest=d;}}
  if(best!=null)g.getNavigation().moveTo(best,1);else g.getNavigation().moveTo(target.getX()+.5,feet,target.getZ()+.5,1);
 }
 @Override public void start(){timeout=200;work=-1;approach();}
 @Override public boolean canContinueToUse(){return active()&&target!=null&&timeout>0&&g.level().getBlockState(target).equals(expected)&&grown(g.level(),target);}
 @Override public boolean requiresUpdateEveryTick(){return true;}
 @Override public void stop(){if(target!=null)g.level().destroyBlockProgress(g.getId(),target,-1);target=null;work=-1;g.getNavigation().stop();}
 /** AIHarvestCrops uses nearby real drops and normal planting interactions, consuming the seed. */
 void replant(){
  if(!(g.level() instanceof net.minecraft.server.level.ServerLevel level))return;
  var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);var previous=player.getMainHandItem();var previousPosition=player.position();boolean creative=player.getAbilities().instabuild;
  try{player.setPos(g.position());player.getAbilities().instabuild=false;
   for(var item:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,g.getBoundingBox().inflate(6))){var stack=item.getItem();if(stack.isEmpty())continue;boolean bean=stack.getItem() instanceof ManaBeanItem;boolean cocoa=stack.is(net.minecraft.world.item.Items.COCOA_BEANS);if(!bean&&!cocoa&&!(stack.getItem() instanceof net.minecraft.world.item.BlockItem blockItem&&blockItem.getBlock() instanceof BushBlock))continue;
    if(item.tickCount<2){var direction=item.position().subtract(g.position()).normalize();item.setDeltaMovement(direction.scale(-.25).add(0,.075,0));}
    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack);
    if(bean||cocoa){var face=bean?net.minecraft.core.Direction.DOWN:expected.hasProperty(CocoaBlock.FACING)?expected.getValue(CocoaBlock.FACING).getOpposite():null;if(face!=null){var support=bean?target.above():target.relative(face.getOpposite());plant(player,stack,support,face);}}
    else{int[][] offsets={{0,0},{0,1},{1,0},{1,1},{-1,0},{0,-1},{-1,-1},{-1,1},{1,-1}};for(var offset:offsets){if(stack.isEmpty())break;plant(player,stack,target.offset(offset[0],-1,offset[1]),net.minecraft.core.Direction.UP);}}
    if(stack.isEmpty())item.discard();else item.setItem(stack);
    if(bean||cocoa)break;
   }
  }finally{player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,previous);player.setPos(previousPosition);player.getAbilities().instabuild=creative;}
 }
 void plant(net.minecraft.server.level.ServerPlayer player,net.minecraft.world.item.ItemStack stack,BlockPos support,net.minecraft.core.Direction face){var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(support).add(net.minecraft.world.phys.Vec3.atLowerCornerOf(face.getNormal()).scale(.5)),face,support,false));stack.useOn(context);}
 @Override public void tick(){
  timeout--;if(target==null)return;g.getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5,30,30);
  if(g.distanceToSqr(target.getX()+.5,target.getY()+.5,target.getZ()+.5)>4){if(g.tickCount%10==0)approach();return;}
  g.getNavigation().stop();if(work<0)work=total=(int)Math.max(10,(20-g.strength()*2)*expected.getDestroySpeed(g.level(),target));
  if(--work>0){if(work%2==0){g.act();g.level().destroyBlockProgress(g.getId(),target,(int)(9*(1-work/(float)total)));}return;}
  if(grown(g.level(),target)&&g.level().getBlockState(target).equals(expected)){g.level().destroyBlock(target,true,g);if(g.upgrades(4)>0)replant();}
  g.act();g.level().destroyBlockProgress(g.getId(),target,-1);
  for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)for(int y=-1;y<=1;y++){var p=target.offset(x,y,z);if(Math.abs(p.getX()-g.home.getX())<=scanRange&&Math.abs(p.getZ()-g.home.getZ())<=scanRange&&Math.abs(p.getY()-g.home.getY())<=scanRange&&grown(g.level(),p)&&!adjacent.contains(p))adjacent.addLast(p);}
  target=null;
 }
}
