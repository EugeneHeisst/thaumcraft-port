package dev.eugheist.tc4modern;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.*;
import thaumcraft.common.tiles.essentia.*;

/** Original essentia core 6: AIEssentiaEmpty(1)/Gather(2)/Goto(3) + AIReturnHome, home source -> marked jars. Millisecond delays use 20 tick/s game time. */
final class GolemEssentiaGoal extends Goal {
 final StrawGolem g;long delay;int count;BlockPos prev;
 GolemEssentiaGoal(StrawGolem g){this.g=g;setFlags(EnumSet.of(Flag.MOVE,Flag.LOOK));}
 @Override public boolean canUse(){return g.core()==6&&g.working();}
 @Override public boolean canContinueToUse(){return canUse();}
 @Override public boolean requiresUpdateEveryTick(){return true;}
 @Override public void stop(){count=0;g.getNavigation().stop();}
 BlockPos container(){return g.home.relative(g.homeFace.getOpposite());}
 boolean carrying(){return g.essentia!=null&&g.essentiaAmount>0;}
 /** TileEntity.getDistanceFrom(home): block centre against the integer home corner. */
 static double fromHome(BlockPos p,BlockPos home){double x=p.getX()+.5-home.getX(),y=p.getY()+.5-home.getY(),z=p.getZ()+.5-home.getZ();return x*x+y*y+z*z;}
 static boolean admits(IEssentiaTransport t,Direction side,Aspect a){return t.canInputFrom(side)&&t.getSuctionAmount(side)>0&&(t.getSuctionType(side)==null||t.getSuctionType(side)==a);}
 /** GolemHelper.getConnectedJars: every jar touching a marked jar, recursively, marked or not. */
 static void connect(Level l,BlockPos p,Map<BlockPos,WardedJarEntity> found){var pending=new ArrayDeque<BlockPos>();pending.add(p);while(!pending.isEmpty()){var at=pending.removeFirst();for(var d:Direction.values()){var next=at.relative(d);if(found.containsKey(next)||!l.hasChunkAt(next)||!(l.getBlockEntity(next) instanceof WardedJarEntity jar))continue;found.put(next,jar);pending.add(next);}}}
 /** GolemHelper.findJarWithRoom: marked transports and labelled partial jars, then labelled empty, labelled full void, unlabelled partial, unlabelled empty, unlabelled void, empty void; void jars rank one range² further. */
 static BlockPos findJarWithRoom(StrawGolem g){
  Aspect e=g.essentia;if(e==null||g.essentiaAmount<=0)return null;var l=g.level();double dmod=g.range()*(double)g.range();
  var marked=new ArrayList<WardedJarEntity>();var out=new ArrayList<BlockPos>();
  for(var value:g.markers){var m=(CompoundTag)value;var p=BlockPos.of(m.getLong("pos"));if(!l.hasChunkAt(p))continue;var te=l.getBlockEntity(p);
   if(te instanceof WardedJarEntity jar){if(fromHome(p,g.home)<=dmod)marked.add(jar);continue;}
   if(te instanceof IEssentiaTransport t&&admits(t,Direction.from3DDataValue(m.getByte("side")),e)&&fromHome(p,g.home)<=dmod)out.add(p);}
  if(marked.isEmpty()&&out.isEmpty())return null;
  var jars=new LinkedHashMap<BlockPos,WardedJarEntity>();for(var jar:marked){jars.put(jar.getBlockPos(),jar);connect(l,jar.getBlockPos(),jars);}
  List<Predicate<JarState>> tiers=List.of(
   s->s.aspect!=null&&s.amount>0&&s.amount<s.maxAmount&&s.aspectFilter!=null&&s.aspect.equals(e)&&s.doesContainerAccept(e),
   s->(s.aspect==null||s.amount==0)&&s.aspectFilter!=null&&s.doesContainerAccept(e),
   s->s.aspect!=null&&s.amount>=s.maxAmount&&s instanceof VoidJarState&&s.aspectFilter!=null&&s.aspect.equals(e)&&s.doesContainerAccept(e),
   s->s.aspect!=null&&s.amount>0&&s.amount<s.maxAmount&&s.aspectFilter==null&&s.aspect.equals(e)&&s.doesContainerAccept(e),
   s->(s.aspect==null||s.amount==0)&&s.aspectFilter==null&&!(s instanceof VoidJarState)&&s.doesContainerAccept(e),
   s->s.aspect!=null&&s instanceof VoidJarState&&s.aspectFilter==null&&s.aspect.equals(e)&&s.doesContainerAccept(e),
   s->(s.aspect==null||s.amount==0)&&s.aspectFilter==null&&s instanceof VoidJarState&&s.doesContainerAccept(e));
  for(int i=0;i<tiers.size()&&(i==0||out.isEmpty());i++)for(var jar:jars.values())if(tiers.get(i).test(jar.state))out.add(jar.getBlockPos());
  BlockPos dest=null;double dist=Double.MAX_VALUE;
  for(var p:out){double d=fromHome(p,g.home);if(l.getBlockEntity(p) instanceof WardedJarEntity jar&&jar.state instanceof VoidJarState)d+=dmod;if(d<dist){dist=d;dest=p;}}
  return dest;
 }
 void splash(float volume){g.playSound(net.minecraft.sounds.SoundEvents.GENERIC_SWIM,volume,1+(g.getRandom().nextFloat()-g.getRandom().nextFloat())*.3f);g.act();}
 /** AIEssentiaEmpty: navigation idle, within 2 blocks of the chosen jar or marked transport side. */
 boolean empty(){
  if(!g.getNavigation().isDone()||!carrying())return false;var p=findJarWithRoom(g);if(p==null||g.distanceToSqr(p.getX()+.5,p.getY()+.5,p.getZ()+.5)>4)return false;var te=g.level().getBlockEntity(p);
  if(te instanceof WardedJarEntity jar){g.essentia(g.essentia,jar.state.addToContainer(g.essentia,g.essentiaAmount));splash(.2f);jar.changed();}
  else if(te instanceof IEssentiaTransport t)for(var value:g.markers){var m=(CompoundTag)value;if(m.getLong("pos")!=p.asLong())continue;var side=Direction.from3DDataValue(m.getByte("side"));if(!admits(t,side,g.essentia))continue;int added=t.addEssentia(g.essentia,g.essentiaAmount,side);if(added<=0)continue;g.essentia(g.essentia,g.essentiaAmount-added);splash(.2f);break;}
  return true;
 }
 boolean compatible(Aspect type,Aspect unknown){return g.essentiaAmount==0||(g.essentia==null||g.essentia.equals(type)||g.essentia.equals(unknown))&&g.essentiaAmount<g.carryLimit();}
 /** AIEssentiaGather: the home container itself when it transports essentia, otherwise the fullest alembic stacked up to five above it. */
 boolean gather(){
  long now=g.level().getGameTime();if(!g.getNavigation().isDone()||delay>now)return false;var c=container();var face=g.homeFace;
  if(g.distanceToSqr(c.getX()+.5,c.getY()+.5,c.getZ()+.5)>6)return false;var te=g.level().getBlockEntity(c);if(te==null)return false;int start=-1;
  if(te instanceof IEssentiaTransport t){if(!(te instanceof WardedJarEntity||te instanceof EssentiaReservoir.ReservoirEntity||t.canOutputTo(face))||t.getEssentiaAmount(face)<=0||!compatible(t.getEssentiaType(face),t.getEssentiaType(null)))return false;start=0;}
  else{int prevTot=-1;for(int a=5;a>=0;a--){if(!(g.level().getBlockEntity(c.above(a)) instanceof AlembicEntity alembic))continue;if(g.essentiaAmount!=0&&(g.essentia!=null&&!g.essentia.equals(alembic.state.aspect)||g.essentiaAmount>=g.carryLimit())||alembic.state.amount<=prevTot)continue;start=a;prevTot=alembic.state.amount;}if(start<0)return false;}
  delay=now+20;
  if(!(g.level().getBlockEntity(c.above(start)) instanceof IEssentiaTransport ta))return true;
  if(ta instanceof AlembicEntity||ta instanceof WardedJarEntity)face=Direction.UP;
  // Original AIEssentiaGather: reservoirs expose their own connection face, independent of the golem home face.
  if(ta instanceof EssentiaReservoir.ReservoirEntity reservoir)face=reservoir.facing;
  if(ta.getEssentiaAmount(face)==0||!ta.canOutputTo(face)||!compatible(ta.getEssentiaType(face),ta.getEssentiaType(null)))return true;
  Aspect a=ta.getEssentiaType(face);if(a==null)a=ta.getEssentiaType(null);int available=ta instanceof EssentiaReservoir.ReservoirEntity reservoir?reservoir.containerContains(a):ta.getEssentiaAmount(face);
  int taken=ta.takeEssentia(a,Math.min(available,g.carryLimit()-g.essentiaAmount),face);
  if(taken>0){g.essentia(a,g.essentiaAmount+taken);splash(.05f);}
  delay=now+2;return true;
 }
 @Override public void tick(){
  if(count>0){if(!g.getNavigation().isDone()){if(--count==0&&g.blockPosition().equals(prev)){var v=DefaultRandomPos.getPos(g,2,1);if(v!=null){count=20;g.getNavigation().moveTo(v.x,v.y,v.z,1);}}return;}count=0;}
  if(empty()||gather())return;
  if(carrying()){if(g.tickCount%GolemConfig.delay()!=0)return;var jar=findJarWithRoom(g);if(jar!=null){count=200;prev=g.blockPosition();GolemHarvestGoal.approach(g,jar);return;}}
  if(g.tickCount%20==0&&g.distanceToSqr(g.home.getX()+.5,g.home.getY()+.5,g.home.getZ()+.5)>=3)g.getNavigation().moveTo(g.home.getX()+.5,g.home.getY(),g.home.getZ()+.5,1);
 }
}
