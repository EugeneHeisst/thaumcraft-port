package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.*;

/**
 * Original fill/empty inventory jobs. All transfers use the marked face and server capabilities.
 * Empty core (1) follows AIHomeTake/AIHomeReplace: a home item with no marked destination (findSomethingEmptyCore) is
 * ignored for golemIgnoreDelay wall-clock milliseconds, and a carried load that lost its destination goes back home.
 */
final class GolemInventoryGoal extends Goal {
 record Target(BlockPos pos,Direction side){}
 final StrawGolem g;final Map<ItemStack,Long> timeout=new HashMap<>();boolean returning;static int returned;
 GolemInventoryGoal(StrawGolem g){this.g=g;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 public boolean canUse(){return g.working()&&(g.core()==0||g.core()==1);}
 public boolean canContinueToUse(){return canUse();}
 public boolean requiresUpdateEveryTick(){return true;}
 public void stop(){g.getNavigation().stop();}
 IItemHandler inventory(BlockPos p,Direction face){return g.level().getCapability(Capabilities.ItemHandler.BLOCK,p,face);}
 BlockPos home(){return g.home.relative(g.homeFace.getOpposite());}
 boolean onTimeout(ItemStack s){long now=System.currentTimeMillis();timeout.entrySet().removeIf(e->e.getValue()<=now);return timeout.keySet().stream().anyMatch(k->g.matches(k,s));}
 boolean ignore(ItemStack s){timeout.put(s.copyWithCount(1),System.currentTimeMillis()+GolemConfig.ignoreDelay());return true;}
 int needed(ItemStack sample,IItemHandler home){int n=sample.getCount();if(g.fillAny)return g.carryLimit();if(home!=null)for(int i=0;i<home.getSlots();i++)if(g.matches(sample,home.getStackInSlot(i)))n-=home.getStackInSlot(i).getCount();return Math.max(0,n);}
 static final int ANY_MARKER=Integer.MIN_VALUE;
 ItemStack extract(IItemHandler from,IItemHandler home,boolean simulate){return extract(from,home,simulate,ANY_MARKER);}
 /** AIFillTake: only items whose matching filter colours accept the marked face colour. */
 ItemStack extract(IItemHandler from,IItemHandler home,boolean simulate,int markerColor){
  if(from==null)return ItemStack.EMPTY;
  for(int i=0;i<from.getSlots();i++){var s=from.getStackInSlot(i);if(s.isEmpty()||markerColor!=ANY_MARKER&&!g.colorAllows(s,markerColor))continue;int amount=0;
   if(g.core()==1){if(g.accepts(s))amount=g.carryLimit();}
   else for(int f=0;f<g.filterCount();f++){var filter=g.filters.getItem(f);if(!filter.isEmpty()&&g.matches(filter,s))amount=Math.max(amount,needed(filter,home));}
   if(amount>0){var result=from.extractItem(i,Math.min(amount,g.carryLimit()),simulate);if(!result.isEmpty())return result;}
  }return ItemStack.EMPTY;
 }
 /** AIEmptyGoto: nearest marked container (not home, within range) with room, otherwise the nearest non-container drop marker. */
 Target destination(ItemStack load){
  Target best=null;double distance=Double.MAX_VALUE;boolean bestContainer=false;
  for(var value:g.markers){var mark=(net.minecraft.nbt.CompoundTag)value;var pos=BlockPos.of(mark.getLong("pos"));if(pos.equals(home())||!g.level().hasChunkAt(pos))continue;var side=Direction.from3DDataValue(mark.getByte("side"));if(!g.colorAllows(load,mark.getByte("color")))continue;var inv=inventory(pos,side);double d=g.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);if(d>g.range()*g.range())continue;
   boolean valid=inv==null||ItemHandlerHelper.insertItemStacked(inv,load.copy(),true).getCount()<load.getCount();
   if(valid&&((inv!=null&&!bestContainer)||(inv!=null)==bestContainer&&d<distance)){best=new Target(pos,side);distance=d;bestContainer=inv!=null;}
  }return best;
 }
 /** AIHomeTake with getFirstItemUsingTimeout/validTargetForItem: first accepted home stack that is not ignored and has somewhere to go. */
 ItemStack take(IItemHandler home){
  if(home==null)return ItemStack.EMPTY;
  for(int i=0;i<home.getSlots();i++){var s=home.getStackInSlot(i);if(s.isEmpty()||!g.accepts(s)||onTimeout(s))continue;
   if(destination(s.copyWithCount(Math.min(s.getCount(),g.carryLimit())))==null){ignore(s);continue;}
   var result=home.extractItem(i,g.carryLimit(),false);if(!result.isEmpty())return result;
  }return ItemStack.EMPTY;
 }
 boolean approach(BlockPos p){if(g.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5)<4){g.getNavigation().stop();return true;}if(g.tickCount%10==0)g.getNavigation().moveTo(p.getX()+.5,p.getY(),p.getZ()+.5,1);return false;}
 void deposit(IItemHandler inv,BlockPos at){if(g.tickCount%GolemConfig.delay()!=0)return;if(inv==null){g.dropCarried();g.act();return;}var left=ItemHandlerHelper.insertItemStacked(inv,g.carried().copy(),false);if(left.getCount()!=g.carried().getCount()){g.carried(left);g.act();GolemChests.interact(g.level(),at);}}
 /** AIHomeReplace: unlike deposit, a missing or full home container keeps the load instead of dropping it. */
 void replace(IItemHandler home){if(home==null)return;var left=ItemHandlerHelper.insertItemStacked(home,g.carried().copy(),false);if(left.getCount()!=g.carried().getCount()){g.carried(left);g.act();GolemChests.interact(g.level(),home());returned++;}}
 public void tick(){
  var homeInv=inventory(home(),g.homeFace);
  if(g.core()==0&&!g.carried().isEmpty()){if(approach(g.home))deposit(homeInv,home());return;}
  if(g.core()==1&&g.carried().isEmpty()){returning=false;if(approach(g.home)&&g.tickCount%GolemConfig.delay()==0){var s=take(homeInv);if(!s.isEmpty()){g.carried(s);g.act();GolemChests.interact(g.level(),home());}}return;}
  if(g.core()==1){
   if(g.tickCount%GolemConfig.delay()==0)returning=onTimeout(g.carried())||destination(g.carried())==null&&ignore(g.carried());
   if(returning){if(approach(g.home)&&g.tickCount%GolemConfig.delay()==0)replace(homeInv);return;}
  }
  if(g.tickCount%GolemConfig.delay()!=0)return;
  BlockPos best=null;Direction face=null;int bestColor=-1;
  if(g.core()==1){var t=destination(g.carried());if(t!=null){best=t.pos();face=t.side();}}
  else{double distance=Double.MAX_VALUE;
   for(var value:g.markers){var mark=(net.minecraft.nbt.CompoundTag)value;var pos=BlockPos.of(mark.getLong("pos"));if(pos.equals(home())||!g.level().hasChunkAt(pos))continue;var side=Direction.from3DDataValue(mark.getByte("side"));var inv=inventory(pos,side);double d=g.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5);if(d>g.range()*g.range())continue;
    if(inv!=null&&d<distance&&!extract(inv,homeInv,true,mark.getByte("color")).isEmpty()){best=pos;face=side;distance=d;bestColor=mark.getByte("color");}
   }
  }
  if(best!=null&&approach(best.relative(face))&&g.tickCount%GolemConfig.delay()==0){var inv=inventory(best,face);if(g.core()==0){var s=extract(inv,homeInv,false,bestColor);if(!s.isEmpty()){g.carried(s);g.act();GolemChests.interact(g.level(),best);}}else deposit(inv,best);}
 }
}
