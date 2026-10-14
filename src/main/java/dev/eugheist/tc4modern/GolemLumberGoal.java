package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.state.BlockState;

/** AIHarvestLogs: range/3 search, work at the base, one furthest log per work cycle. */
final class GolemLumberGoal extends Goal {
 final StrawGolem g;BlockPos target;BlockState expected;int work=-1,total,timeout;
 GolemLumberGoal(StrawGolem g){this.g=g;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 boolean active(){return g.core()==7&&g.working();}
 boolean log(BlockPos p){return g.level().hasChunkAt(p)&&g.level().getBlockState(p).is(BlockTags.LOGS)&&g.level().getBlockState(p).getDestroySpeed(g.level(),p)>=0;}
 @Override public boolean canUse(){if(!active()||g.tickCount%GolemConfig.delay()!=0||!g.getNavigation().isDone())return false;int range=(int)Math.ceil(g.range()/3.0);var random=g.getRandom();for(int n=0;n<range*4;n++){var p=new BlockPos((int)(g.home.getX()+random.nextInt(1+range*2)-range),(int)(g.home.getY()+random.nextInt(1+range)-range/2f),(int)(g.home.getZ()+random.nextInt(1+range*2)-range));if(!log(p))continue;double distance=g.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5);while(log(p.below())&&g.distanceToSqr(p.getX()+.5,p.getY()-.5,p.getZ()+.5)<distance){p=p.below();distance=g.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5);}target=p;expected=g.level().getBlockState(p);return true;}return false;}
 void approach(){GolemHarvestGoal.approach(g,target);}
 @Override public void start(){timeout=200;work=-1;approach();}
 @Override public boolean canContinueToUse(){return active()&&target!=null&&timeout>0&&g.level().getBlockState(target).equals(expected)&&log(target);}
 @Override public boolean requiresUpdateEveryTick(){return true;}
 @Override public void stop(){if(target!=null)g.level().destroyBlockProgress(g.getId(),target,-1);target=null;work=-1;g.getNavigation().stop();}
 static boolean harvest(StrawGolem g,BlockPos p){if(!(g.level() instanceof net.minecraft.server.level.ServerLevel level))return false;var player=net.neoforged.neoforge.common.util.FakePlayerFactory.get(level,new com.mojang.authlib.GameProfile(java.util.UUID.nameUUIDFromBytes("FakeThaumcraftGolem".getBytes(java.nio.charset.StandardCharsets.UTF_8)),"FakeThaumcraftGolem"));var oldHand=player.getMainHandItem();var oldPosition=player.position();boolean creative=player.getAbilities().instabuild;try{player.setPos(g.position());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);player.getAbilities().instabuild=false;boolean success=player.gameMode.destroyBlock(p);if(success)for(int x=-3;x<=3;x++)for(int y=-3;y<=3;y++)for(int z=-3;z<=3;z++){var at=p.offset(x,y,z);level.scheduleTick(at,level.getBlockState(at).getBlock(),150+level.random.nextInt(150));}return success;}finally{player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,oldHand);player.setPos(oldPosition);player.getAbilities().instabuild=creative;}}
 @Override public void tick(){timeout--;if(target==null)return;g.getLookControl().setLookAt(target.getX()+.5,target.getY()+.5,target.getZ()+.5,30,30);if(g.distanceToSqr(target.getX()+.5,target.getY()+.5,target.getZ()+.5)>4){if(g.tickCount%10==0)approach();return;}g.getNavigation().stop();if(work<0)work=total=(int)Math.max(5,(20-g.strength()*3)*expected.getDestroySpeed(g.level(),target));if(--work>0){int interval=Math.max(1,total/Math.max(1,Math.round(total/6f)));if(work%interval==0){g.act();g.level().destroyBlockProgress(g.getId(),target,(int)(9*(1-work/(float)total)));var sound=expected.getSoundType(g.level(),target,g);g.playSound(sound.getHitSound(),(sound.getVolume()+.7f)/8,sound.getPitch()*.5f);}return;}
  var furthest=ElementalTools.furthest(g.level(),target,expected.getBlock());harvest(g,furthest);g.act();g.level().destroyBlockProgress(g.getId(),target,-1);timeout=200;work=-1;if(!log(target)){int range=(int)Math.ceil(g.range()/3.0);BlockPos next=null;search:for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=-1;y<=1;y++){var p=target.offset(x,y,z);if(Math.abs(p.getX()-g.home.getX())<=range&&Math.abs(p.getY()-g.home.getY())<=range&&Math.abs(p.getZ()-g.home.getZ())<=range&&log(p)){next=p;break search;}}target=next;if(next!=null){expected=g.level().getBlockState(next);approach();}}else expected=g.level().getBlockState(target);
 }
}
