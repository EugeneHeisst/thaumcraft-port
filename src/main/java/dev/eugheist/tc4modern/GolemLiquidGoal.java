package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.*;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/** Original liquid core: marked sources -> home tank, filled-container filters and entropy pumping. */
final class GolemLiquidGoal extends Goal {
 final StrawGolem g;Source source;boolean returning;int work;
 final Map<BlockPos,ArrayDeque<BlockPos>> queues=new HashMap<>();
 record Source(BlockPos pos,Direction face,FluidStack fluid,boolean tank){}
 GolemLiquidGoal(StrawGolem g){this.g=g;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 @Override public boolean canUse(){return g.core()==5&&g.working();}
 @Override public boolean canContinueToUse(){return canUse();}
 @Override public boolean requiresUpdateEveryTick(){return true;}
 @Override public void start(){returning=!g.fluidCarried.isEmpty();work=0;source=null;}
 @Override public void stop(){source=null;work=0;g.getNavigation().stop();}
 IFluidHandler tank(BlockPos p,Direction face){return g.level().getCapability(Capabilities.FluidHandler.BLOCK,p,face);}
 IFluidHandler home(){return tank(g.home.relative(g.homeFace.getOpposite()),g.homeFace);}
 boolean accepts(FluidStack fluid){boolean empty=true;for(int i=0;i<g.filterCount();i++){var stack=g.filters.getItem(i);if(stack.isEmpty())continue;empty=false;var filter=FluidUtil.getFluidContained(stack);if(filter.isPresent()&&FluidStack.isSameFluidSameComponents(filter.get(),fluid))return true;}return empty;}
 boolean wanted(FluidStack fluid,IFluidHandler home){return !fluid.isEmpty()&&accepts(fluid)&&(g.fluidCarried.isEmpty()||FluidStack.isSameFluidSameComponents(g.fluidCarried,fluid))&&home!=null&&home.fill(fluid,FluidAction.SIMULATE)>0;}
 Source candidate(BlockPos p,Direction face,IFluidHandler home){var handler=tank(p,face);int room=g.fluidCarryLimit()-g.fluidCarried.getAmount();if(handler!=null){for(int i=0;i<handler.getTanks();i++){var available=handler.getFluidInTank(i);if(available.isEmpty())continue;var request=available.copyWithAmount(room);var drained=handler.drain(request,FluidAction.SIMULATE);if(wanted(drained,home))return new Source(p,face,drained,true);}}var state=g.level().getBlockState(p);var fluid=state.getFluidState();if(room>=1000&&state.getBlock() instanceof LiquidBlock&&fluid.isSource()){var request=new FluidStack(fluid.getType(),1000);if(wanted(request,home))return new Source(p,face,request,false);}return null;}
 Source find(IFluidHandler home){Source best=null;double nearest=Double.MAX_VALUE;boolean preferTank=false;var homePos=g.home.relative(g.homeFace.getOpposite());for(var value:g.markers){var mark=(CompoundTag)value;var p=BlockPos.of(mark.getLong("pos"));if(p.equals(homePos)||!g.level().hasChunkAt(p))continue;double distance=g.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5);if(distance>g.range()*g.range())continue;var candidate=candidate(p,Direction.from3DDataValue(mark.getByte("side")),home);if(candidate!=null&&((candidate.tank&&!preferTank)||(candidate.tank==preferTank&&distance<nearest))){best=candidate;nearest=distance;preferTank=candidate.tank;}}return best;}
 boolean approach(BlockPos p,double reach){if(g.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5)<=reach){g.getNavigation().stop();return true;}if(g.tickCount%10==0)GolemHarvestGoal.approach(g,p);return false;}
 boolean sameFluid(BlockPos p,FluidStack fluid){return g.level().hasChunkAt(p)&&g.level().getFluidState(p).getType().isSame(fluid.getFluid());}
 BlockPos pump(Source source){if(g.upgrades(5)==0)return source.pos;var queue=queues.get(source.pos);if(queue==null||queue.isEmpty()){var found=new ArrayList<BlockPos>();var pending=new ArrayDeque<BlockPos>();var seen=new HashSet<BlockPos>();pending.add(source.pos);seen.add(source.pos);double radius=g.range();while(!pending.isEmpty()){var at=pending.removeFirst();if(g.level().getFluidState(at).isSource()&&g.level().getBlockState(at).getBlock() instanceof LiquidBlock)found.add(at);for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++){var next=at.offset(x,y,z);if(next.distSqr(source.pos)>radius*radius||seen.contains(next)||!sameFluid(next,source.fluid))continue;seen.add(next);pending.addLast(next);}}found.sort(Comparator.comparingDouble((BlockPos p)->p.distSqr(source.pos)).reversed());queue=new ArrayDeque<>(found);queues.put(source.pos,queue);}while(!queue.isEmpty()){var p=queue.removeFirst();if(sameFluid(p,source.fluid)&&g.level().getFluidState(p).isSource())return p;}return source.pos;}
 FluidStack drain(Source source){int room=g.fluidCarryLimit()-g.fluidCarried.getAmount();if(source.tank){var handler=tank(source.pos,source.face);return handler==null?FluidStack.EMPTY:handler.drain(source.fluid.copyWithAmount(room),FluidAction.EXECUTE);}if(room<1000)return FluidStack.EMPTY;var pos=pump(source);var state=g.level().getBlockState(pos);if(!(state.getBlock() instanceof BucketPickup pickup)||!state.getFluidState().isSource()||!sameFluid(pos,source.fluid))return FluidStack.EMPTY;var bucket=pickup.pickupBlock(null,g.level(),pos,state);return bucket.isEmpty()?FluidStack.EMPTY:source.fluid.copyWithAmount(1000);}
 @Override public void tick(){var home=home();if(returning){if(g.fluidCarried.isEmpty()){returning=false;return;}if(!approach(g.home,5)||g.tickCount%5!=0||home==null)return;int filled=home.fill(g.fluidCarried.copy(),FluidAction.EXECUTE);if(filled>0){var left=g.fluidCarried.copy();left.shrink(filled);g.fluidCarried(left);g.act();g.playSound(net.minecraft.sounds.SoundEvents.GENERIC_SWIM,.2f,1);}return;}
  if(home==null)return;if(source==null){if(g.tickCount%GolemConfig.delay()!=0)return;source=find(home);work=0;if(source==null){returning=!g.fluidCarried.isEmpty();return;}}
  var current=candidate(source.pos,source.face,home);if(current==null){source=null;returning=!g.fluidCarried.isEmpty();return;}source=current;g.getLookControl().setLookAt(source.pos.getX()+.5,source.pos.getY()+.5,source.pos.getZ()+.5,30,30);if(!approach(source.pos,4)){work=0;return;}if(++work<10)return;work=0;var drained=drain(source);if(drained.isEmpty()){source=null;returning=!g.fluidCarried.isEmpty();return;}var carried=g.fluidCarried.isEmpty()?drained:g.fluidCarried.copy();if(!g.fluidCarried.isEmpty())carried.grow(drained.getAmount());g.fluidCarried(carried);g.act();g.playSound(net.minecraft.sounds.SoundEvents.GENERIC_SWIM,.2f,1);if(g.fluidCarried.getAmount()>=g.fluidCarryLimit()){returning=true;source=null;}
 }
}
