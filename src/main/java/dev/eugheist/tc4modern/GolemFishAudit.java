package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Visible audit: fishing golem core (AIFish + EntityGolemBobber). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class GolemFishAudit {
 static final BlockPos BASE=new BlockPos(460,121,-90);static int ticks;static volatile boolean ready;static StrawGolem plain,fire;static final java.util.List<GolemBobber> hooked=new java.util.ArrayList<>();
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_FISH FAIL "+s);System.out.println("TC4_FISH PASS "+s);}
 static GolemFishGoal goal(StrawGolem g){return g.goalSelector.getAvailableGoals().stream().map(w->w.getGoal()).filter(x->x instanceof GolemFishGoal).map(x->(GolemFishGoal)x).findFirst().orElseThrow();}
 static void setup(ServerPlayer p){
  var l=p.serverLevel();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();l.setDayTime(6000);
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(30)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=-5;y<6;y++)l.setBlock(BASE.offset(x,y,z),(y==-5?Blocks.STONE:y<0?(Math.abs(x)<=12&&z>=-12&&z<=12&&!(Math.abs(x)<=1&&Math.abs(z)<=1)?Blocks.WATER:Blocks.STONE):Blocks.AIR).defaultBlockState(),3);
  /* 3x3 stone island in a 25x25, 4-deep pond: every probe within the core's range lands on open water */
  plain=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE,11);fire=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE.offset(1,0,0),11);check(fire.installUpgrade(3),"fire upgrade installed");
  check(Golemancy.coreId(new ItemStack(Golemancy.FISHING.get()))==11&&Golemancy.core(11)==Golemancy.FISHING.get(),"fishing core is core 11");
  var copy=new StrawGolem(Golemancy.ENTITY.get(),l);copy.restoreItem(plain.asItem());check(copy.core()==11,"core 11 survives bell item");
  var gl=goal(plain);gl.target=BASE.offset(5,-1,5);check(Math.abs(gl.quality(gl.target,3e-5f,3e-5f,1.5e-5f)-4.2e-4f)<1e-9,"open sky pond quality 4*(3e-5+3e-5+3*1.5e-5)="+gl.quality(gl.target,3e-5f,3e-5f,1.5e-5f));
  int fish=0,junk=0,rare=0,enchanted=0;var names=new java.util.TreeSet<String>();for(int i=0;i<4000;i++){var s=gl.result();names.add(s.getItem().toString());if(GolemFishGoal.FISH.stream().anyMatch(e->e.stack().is(s.getItem())))fish++;else if(GolemFishGoal.RARE.stream().anyMatch(e->e.stack().is(s.getItem()))||s.is(Items.ENCHANTED_BOOK)){rare++;if(s.isEnchanted()||s.is(Items.ENCHANTED_BOOK))enchanted++;}else junk++;}
  gl.target=null;System.out.println("TC4_FISH loot fish="+fish+" junk="+junk+" rare="+rare+" enchanted="+enchanted+" kinds="+names);
  /* junk .1-.04=.06, treasure .05+.02=.07 next to four open-sky 3-deep water blocks */
  check(fish>3200&&fish<3700&&junk>150&&junk<350&&rare>180&&rare<380&&enchanted>0,"loot shares fish~87% junk~6% treasure~7%");
  ready=true;
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.golemFishAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)server.execute(()->{var p=server.getPlayerList().getPlayer(id);setup(p);p.connection.teleport(BASE.getX()-4.5,BASE.getY()+3,BASE.getZ()-6.5,-30,25);});
  if(ticks==160)server.execute(()->{var bobbers=server.overworld().getEntitiesOfClass(GolemBobber.class,new AABB(BASE).inflate(20));check(ready&&GolemFishGoal.casts>=2&&bobbers.size()>=2,"both golems cast bobbers casts="+GolemFishGoal.casts+" live="+bobbers.size());for(var b:bobbers)if(b.tickCount>=40)check(b.level().getFluidState(b.blockPosition()).isEmpty()?b.level().getFluidState(b.blockPosition().below()).is(net.minecraft.tags.FluidTags.WATER):true,"bobber floats on the water surface at "+b.position());});
  if(ticks==170){check(GolemClient.BobberRenderer.rendered>0,"bobber sprite and line rendered");LiquidDeathAudit.shot(mc,"golem-fish-cast.png");server.execute(()->{var b=server.overworld().getEntitiesOfClass(GolemBobber.class,new AABB(BASE).inflate(20)).stream().max(java.util.Comparator.comparingInt(x->x.tickCount)).orElseThrow();var p=server.getPlayerList().getPlayer(id);double dx=b.getX()-BASE.getX(),dz=b.getZ()-BASE.getZ(),len=Math.max(1,Math.sqrt(dx*dx+dz*dz));double cx=b.getX()+dz/len*2.5,cz=b.getZ()-dx/len*2.5;p.connection.teleport(cx,b.getY()+1.2,cz,(float)Math.toDegrees(Math.atan2(-(b.getX()-cx),b.getZ()-cz)),30);});}
  if(ticks==178)LiquidDeathAudit.shot(mc,"golem-fish-bobber-closeup.png");
  if(ticks==180)server.execute(()->{for(var g:java.util.List.of(plain,fire)){var gl=goal(g);check(gl.target!=null,"golem waiting on its bobber");hooked.add(gl.bobber);gl.quality=1;}});
  if(ticks==200)server.execute(()->{check(GolemFishGoal.catches>=2,"forced bite throws the catch catches="+GolemFishGoal.catches);var drops=server.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(20));System.out.println("TC4_FISH drops "+drops.stream().map(e->e.getItem()+"@"+e.blockPosition()).toList());check(!drops.isEmpty(),"catch landed as items");check(hooked.size()==2&&hooked.stream().allMatch(Entity::isRemoved),"hooked bobbers removed after the catch (golems may recast)");});
  if(ticks==205)LiquidDeathAudit.shot(mc,"golem-fish-catch.png");
  if(ticks==210)server.execute(()->{var gl=goal(fire);for(int i=0;i<200;i++){gl.target=BASE.offset(5,-1,5);gl.quality=1;gl.tick();}var cooked=server.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(25)).stream().filter(e->e.getItem().is(Items.COOKED_COD)||e.getItem().is(Items.COOKED_SALMON)).count();var raw=server.overworld().getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(25)).stream().filter(e->e.getItem().is(Items.COD)||e.getItem().is(Items.SALMON)).count();check(cooked>50,"fire upgrade cooks cod/salmon cooked="+cooked+" raw-left(from plain golem)="+raw);});
  if(ticks==260){System.out.println("TC4_FISH ALL PASS");mc.stop();}
 }
}
