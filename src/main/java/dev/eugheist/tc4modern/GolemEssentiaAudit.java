package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Hidden native audit for the original essentia core: jar priorities, real jar/alembic transfers, persistence, recipe and visuals. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class GolemEssentiaAudit {
 static final BlockPos BASE=new BlockPos(320,121,-90),J1=BASE.offset(6,0,0),J2=J1.east(),J3=BASE.offset(3,0,4),VOID=BASE.offset(2,0,-3),FAR=BASE.offset(17,0,0),F=BASE.offset(0,0,7),JB=F.offset(4,0,0);
 static StrawGolem golem,alchemist,display;static int ticks;static volatile boolean done;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_ESSENTIA FAIL "+s);System.out.println("TC4_ESSENTIA PASS "+s);}
 static void mark(StrawGolem g,BlockPos p,Direction face){var t=new CompoundTag();t.putLong("pos",p.asLong());t.putByte("side",(byte)face.get3DDataValue());t.putByte("color",(byte)-1);g.markers.add(t);}
 static WardedJarEntity jar(net.minecraft.server.level.ServerLevel l,BlockPos p,boolean isVoid,Aspect filter,Aspect aspect,int amount){l.setBlock(p,(isVoid?AlchemyMachines.VOID_JAR:EssentiaDevices.JAR).get().defaultBlockState(),3);var j=(WardedJarEntity)l.getBlockEntity(p);j.state.aspectFilter=filter;j.state.aspect=amount>0?aspect:null;j.state.amount=amount;j.changed();return j;}
 static WardedJarEntity at(BlockPos p){return (WardedJarEntity)golem.level().getBlockEntity(p);}
 static AlembicEntity alembic(net.minecraft.server.level.ServerLevel l,BlockPos p,Aspect a,int n){l.setBlock(p,EssentiaDevices.ALEMBIC.get().defaultBlockState(),3);var e=(AlembicEntity)l.getBlockEntity(p);e.state.aspect=a;e.state.amount=n;e.changed();return e;}
 static void setup(ServerPlayer p){
  var l=p.serverLevel();p.removeAllEffects();p.setHealth(p.getMaxHealth());p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getInventory().clearContent();p.connection.teleport(BASE.getX()+3.5,BASE.getY()+1,BASE.getZ()-8.5,0,15);p.getAbilities().flying=true;p.onUpdateAbilities();l.setDayTime(6000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(30)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);for(int x=-6;x<19;x++)for(int z=-10;z<12;z++)for(int y=-1;y<8;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);l.getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(30)).forEach(Entity::discard);
  jar(l,BASE,false,null,Aspect.FIRE,40);golem=GolemHarvestAudit.golem(p,GolemMaterial.THAUMIUM,BASE.east(),6);golem.homeFace=Direction.EAST;golem.owner=p.getUUID();golem.paused=true;
  check(golem.carryLimit()==32&&golem.range()==16,"thaumium essentia carry 32 range 16");
  jar(l,J1,false,Aspect.FIRE,Aspect.FIRE,10);jar(l,J2,false,Aspect.FIRE,null,0);jar(l,J3,false,null,null,0);jar(l,VOID,true,Aspect.FIRE,Aspect.FIRE,64);jar(l,FAR,false,Aspect.FIRE,Aspect.FIRE,3);
  for(var m:java.util.List.of(J1,J3,VOID,FAR))mark(golem,m,Direction.UP);
  check(GolemEssentiaGoal.findJarWithRoom(golem)==null,"empty-handed golem never chooses a jar");
  golem.essentia(Aspect.FIRE,10);
  check(J1.equals(GolemEssentiaGoal.findJarWithRoom(golem)),"tier1 labelled partial jar wins over nearer unlabelled jar; out-of-range jar ignored");
  at(J1).state.amount=64;check(J2.equals(GolemEssentiaGoal.findJarWithRoom(golem)),"tier2 unmarked labelled empty jar found through connected marked jar");
  at(J2).state.aspect=Aspect.FIRE;at(J2).state.amount=64;check(VOID.equals(GolemEssentiaGoal.findJarWithRoom(golem)),"tier3 full labelled void jar before unlabelled jars");
  golem.markers.removeIf(t->((CompoundTag)t).getLong("pos")==VOID.asLong());at(J3).state.aspect=Aspect.FIRE;at(J3).state.amount=5;check(J3.equals(GolemEssentiaGoal.findJarWithRoom(golem)),"tier4 unlabelled partial same-aspect jar");
  at(J3).state.aspect=Aspect.WATER;check(GolemEssentiaGoal.findJarWithRoom(golem)==null,"unlabelled jar of another aspect refused");
  at(J3).state.aspect=null;at(J3).state.amount=0;check(J3.equals(GolemEssentiaGoal.findJarWithRoom(golem)),"tier5 unlabelled empty jar");
  at(J3).state.aspectFilter=Aspect.WATER;check(GolemEssentiaGoal.findJarWithRoom(golem)==null,"labelled empty jar of another aspect refused");
  var v2=jar(l,BASE.offset(2,0,-3),true,null,null,0);mark(golem,VOID,Direction.UP);var plain=jar(l,BASE.offset(9,0,0),false,null,null,0);mark(golem,plain.getBlockPos(),Direction.UP);check(plain.getBlockPos().equals(GolemEssentiaGoal.findJarWithRoom(golem)),"plain jar preferred over nearer empty void jar");
  l.removeBlock(plain.getBlockPos(),false);check(VOID.equals(GolemEssentiaGoal.findJarWithRoom(golem)),"tier7 empty void jar as last resort");
  // natural run: home jar 40 Ignis, labelled target 10 Ignis; two trips of 32+8.
  golem.markers.clear();l.removeBlock(VOID,false);l.removeBlock(FAR,false);jar(l,J1,false,Aspect.FIRE,Aspect.FIRE,10);jar(l,J2,false,Aspect.FIRE,null,0);jar(l,J3,false,null,null,0);mark(golem,J1,Direction.UP);mark(golem,J3,Direction.UP);golem.essentia(null,0);golem.paused=false;
  // alembic source: furnace home container with the fuller of two stacked alembics drained first.
  l.setBlock(F,EssentiaDevices.FURNACE.get().defaultBlockState(),3);alembic(l,F.above(),Aspect.WATER,5);alembic(l,F.above(2),Aspect.WATER,12);jar(l,JB,false,null,null,0);
  alchemist=GolemHarvestAudit.golem(p,GolemMaterial.STONE,F.east(),6);alchemist.homeFace=Direction.EAST;alchemist.owner=p.getUUID();mark(alchemist,JB,Direction.UP);check(alchemist.carryLimit()==16,"stone essentia carry 16");
  var probe=new GolemEssentiaGoal(alchemist);check(probe.gather()&&alchemist.essentia==Aspect.WATER&&alchemist.essentiaAmount==12&&((AlembicEntity)l.getBlockEntity(F.above(2))).state.amount==0&&((AlembicEntity)l.getBlockEntity(F.above())).state.amount==5,"fullest alembic above furnace drained first");
  // no GUI for core 6, core install shows jar
  var fresh=GolemHarvestAudit.golem(p,GolemMaterial.WOOD,BASE.offset(-3,0,-3),-1);fresh.getEntityData().set(StrawGolem.CORE,(byte)-1);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Golemancy.ESSENTIA.get()));fresh.mobInteract(p,InteractionHand.MAIN_HAND);check(fresh.core()==6&&fresh.displayCarried().is(EssentiaDevices.JAR.get().asItem()),"essentia core installs and golem always holds a jar");
  p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);var menu=p.containerMenu;fresh.mobInteract(p,InteractionHand.MAIN_HAND);check(p.containerMenu==menu&&!(p.containerMenu instanceof GolemMenu),"essentia core has no GUI like original hasGUI(6)");fresh.discard();
  var r=InfusionItemRecipes.all().stream().filter(x->x.key().equals("CoreAlchemy")).findFirst().orElseThrow();var comps=r.componentStacks();check(r.instability()==2&&r.aspects().visSize()==45&&r.research().equals("COREALCHEMY"),"original CoreAlchemy infusion values");
  check(InfusionItemRecipes.match(comps,new ItemStack(Golemancy.LIQUID.get()),java.util.Set.of("COREALCHEMY"))==r,"liquid core + jar + 3 water bottles matches");check(InfusionItemRecipes.match(comps,new ItemStack(Golemancy.LIQUID.get()),java.util.Set.of())==null,"requires COREALCHEMY research");
  var wrong=new java.util.ArrayList<>(comps);wrong.set(1,new ItemStack(Items.POTION));check(InfusionItemRecipes.match(wrong,new ItemStack(Golemancy.LIQUID.get()),java.util.Set.of("COREALCHEMY"))==null,"non-water potion refused");
  check(BasicsBookRecipes.researchIcon("COREALCHEMY")!=null,"COREALCHEMY research icon");
  display=GolemHarvestAudit.golem(p,GolemMaterial.IRON,BASE.offset(3,0,-5),6);display.paused=true;display.essentia(Aspect.FIRE,32);display.setNoAi(true);display.setYRot(180);display.yBodyRot=180;display.yHeadRot=180;
  p.getInventory().setItem(0,new ItemStack(Golemancy.ESSENTIA.get()));p.getInventory().selected=0;p.containerMenu.broadcastChanges();var drop=new ItemEntity(l,BASE.getX()+4.6,BASE.getY()+1.2,BASE.getZ()-5.5,new ItemStack(Golemancy.ESSENTIA.get()));drop.setDeltaMovement(Vec3.ZERO);drop.setNoGravity(true);drop.setNeverPickUp();l.addFreshEntity(drop);
 }
 static int total(){int n=golem.essentiaAmount;for(var p:java.util.List.of(BASE,J1,J2,J3))n+=at(p).state.amount;return n;}
 static void finish(ServerPlayer p){
  check(at(BASE).state.amount==0&&at(J1).state.amount==50&&at(J1).state.aspect==Aspect.FIRE&&at(J2).state.amount==0&&at(J3).state.amount==0&&golem.essentiaAmount==0,"natural trips move 40 Ignis home jar -> labelled marked jar (J1="+at(J1).state.amount+")");
  check(total()==50,"essentia conserved");
  var l=p.serverLevel();var jb=(WardedJarEntity)l.getBlockEntity(JB);check(jb.state.aspect==Aspect.WATER&&jb.state.amount==17&&alchemist.essentiaAmount==0&&((AlembicEntity)l.getBlockEntity(F.above())).state.amount==0,"alembic golem delivers all 17 Aqua into marked jar (JB="+jb.state.amount+")");
  golem.essentia(Aspect.FIRE,7);var tag=new CompoundTag();golem.addAdditionalSaveData(tag);var restored=new StrawGolem(Golemancy.ENTITY.get(),golem.level());restored.readAdditionalSaveData(tag);check(restored.core()==6&&restored.essentia==Aspect.FIRE&&restored.essentiaAmount==7,"essentia survives entity save");
  check(EssentiaItems.getAspects(restored.displayCarried()).getAmount(Aspect.FIRE)==14,"carried jar shows 64*7/32 Ignis");
  var copy=new StrawGolem(Golemancy.ENTITY.get(),golem.level());copy.restoreItem(golem.asItem());check(copy.essentia==Aspect.FIRE&&copy.essentiaAmount==7,"essentia survives bell item roundtrip");done=true;
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.golemEssentiaAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)server.execute(()->setup(server.getPlayerList().getPlayer(id)));if(ticks>40&&ticks<=200&&ticks%5==0)server.execute(()->{/* entities of interrupted earlier runs load after setup's cleanup; drop stray golems so they cannot steal essentia */var l=server.getPlayerList().getPlayer(id).serverLevel();for(var g:l.getEntitiesOfClass(StrawGolem.class,new net.minecraft.world.phys.AABB(BASE).inflate(40)))if(g!=golem&&g!=alchemist&&g!=display){System.out.println("TC4_ESSENTIA removed stray golem "+g.getId()+" at "+g.blockPosition());g.discard();}});
  if(ticks==140){var model=mc.getItemRenderer().getModel(new ItemStack(Golemancy.ESSENTIA.get()),mc.level,mc.player,0);check(!model.isGui3d(),"flat essentia core icon");for(var context:java.util.List.of(ItemDisplayContext.GUI,ItemDisplayContext.GROUND,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)){var scale=model.getTransforms().getTransform(context).scale;check(scale.x<=1&&scale.y<=1&&scale.z<=1,"bounded essentia core scale "+context);}check(GolemClient.rendered>0,"essentia golems rendered");LiquidDeathAudit.shot(mc,"golem-essentia-carry.png");}
  if(ticks==150)server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.connection.teleport(display.getX(),display.getY()-.6,display.getZ()-1.3,0,8);});if(ticks==175)LiquidDeathAudit.shot(mc,"golem-essentia-closeup.png");if(ticks==180)server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.connection.teleport(display.getX()+2.2,display.getY()+.8,display.getZ()+.2,90,15);});if(ticks==205)LiquidDeathAudit.shot(mc,"golem-essentia-side.png");if(ticks%200==0&&ticks<1400)server.execute(()->System.out.println("TC4_ESSENTIA progress home="+at(BASE).state.amount+" J1="+at(J1).state.amount+" carried="+golem.essentiaAmount+" golem="+golem.position()+" alch="+alchemist.essentiaAmount+" alchPos="+alchemist.position()+" nav="+alchemist.getNavigation().isDone()+" golems="+golem.level().getEntitiesOfClass(StrawGolem.class,new AABB(BASE).inflate(40)).stream().map(g->g.getId()+":"+g.core()+":"+g.essentiaAmount+"@"+g.blockPosition().toShortString()).toList()));
  if(ticks==1400)server.execute(()->finish(server.getPlayerList().getPlayer(id)));
  if(ticks==1420){check(done,"all essentia checks finished");System.out.println("TC4_ESSENTIA ALL PASS");mc.stop();}
 }
}
