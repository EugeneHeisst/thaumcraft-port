package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Hidden audit: essentia reservoir (ESSENTIARESERVOIR). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class ReservoirAudit {
 static final BlockPos BASE=new BlockPos(800,121,-90),RES=BASE.offset(0,1,2),SHOW1=BASE.offset(-2,0,2),SHOW2=BASE.offset(2,0,2),BOOM=BASE.offset(6,0,6);static int ticks;static volatile String failed;
 static void check(boolean b,String s){if(!b){failed=s;System.out.println("TC4_RESERVOIR FAIL "+s);}else System.out.println("TC4_RESERVOIR PASS "+s);}
 static EssentiaReservoir.ReservoirEntity at(net.minecraft.world.level.Level l,BlockPos p){return (EssentiaReservoir.ReservoirEntity)l.getBlockEntity(p);}
 static void place(ServerPlayer p,BlockPos on,Direction face){var stack=new ItemStack(EssentiaReservoir.ITEM.get());p.setItemInHand(InteractionHand.MAIN_HAND,stack);stack.useOn(new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(on),face,on,false)));}
 static void setup(ServerPlayer p){
  var l=p.serverLevel();if(p.level()!=p.server.overworld())p.teleportTo(p.server.overworld(),BASE.getX(),BASE.getY(),BASE.getZ(),0,0);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();l.setDayTime(6000);
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(16)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-8;x<=8;x++)for(int z=-4;z<=10;z++)for(int y=-1;y<6;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.SMOOTH_STONE:Blocks.AIR).defaultBlockState(),3);
  // alembic below, reservoir placed on top of it -> faces DOWN into the alembic
  l.setBlock(RES.below(),EssentiaDevices.ALEMBIC.get().defaultBlockState(),3);var alembic=(AlembicEntity)l.getBlockEntity(RES.below());alembic.state.aspect=Aspect.FIRE;alembic.state.amount=20;alembic.setChanged();
  place(p,RES.below(),Direction.UP);var r=at(l,RES);check(r!=null&&r.facing==Direction.DOWN,"placed on the alembic it faces down "+(r==null?null:r.facing));
  r.addToContainer(Aspect.WATER,30);r.addToContainer(Aspect.MAGIC,10);check(r.essentia.visSize()==40&&r.addToContainer(Aspect.AIR,300)==300-216&&r.essentia.visSize()==256,"holds 256 essentia of any mix");
  r.essentia=new thaumcraft.api.aspects.AspectList().add(Aspect.WATER,30);check(r.getSuctionAmount(Direction.DOWN)==24&&r.getMinimumSuction()==24&&r.isConnectable(Direction.DOWN)&&!r.isConnectable(Direction.UP),"suction 24 on its one connectable face");
  check(l.getBlockState(RES).getAnalogOutputSignal(l,RES)==net.minecraft.util.Mth.floor(30/256f*14)+1,"comparator output");
  var copy=new EssentiaReservoir.ReservoirEntity(RES,l.getBlockState(RES));copy.loadWithComponents(r.saveWithoutMetadata(l.registryAccess()),l.registryAccess());check(copy.essentia.getAmount(Aspect.WATER)==30&&copy.facing==Direction.DOWN,"save/load keeps essentia and facing");
  // infusion-style drain treats it as a source
  l.setBlock(RES.above(3),Blocks.STONE.defaultBlockState(),3);
  var recipe=InfusionItemRecipes.all().stream().filter(x->x.key().equals("EssentiaReservoir")).findFirst().orElseThrow();check(recipe.instability()==6&&InfusionItemRecipes.match(recipe.componentStacks(),new ItemStack(recipe.central().get()),Set.of("ESSENTIARESERVOIR"))==recipe,"original infusion EssentiaReservoir");
  check(BasicsBookRecipes.researchIcon("ESSENTIARESERVOIR")!=null,"research icon");
  // display reservoirs with other facings; one to break full
  place(p,SHOW1.below(),Direction.UP);var s1=at(l,SHOW1);s1.addToContainer(Aspect.LIFE,200);
  place(p,SHOW2.below(),Direction.UP);var s2=at(l,SHOW2);s2.addToContainer(Aspect.ENERGY,60);s2.addToContainer(Aspect.ELDRITCH,60);
  place(p,BOOM.below(),Direction.UP);at(l,BOOM).addToContainer(Aspect.POISON,200);
  p.getInventory().setItem(0,new ItemStack(EssentiaReservoir.ITEM.get()));var drop=new ItemEntity(l,BASE.getX()+.5,BASE.getY()+.4,BASE.getZ()+.5,new ItemStack(EssentiaReservoir.ITEM.get()));drop.setDeltaMovement(Vec3.ZERO);drop.setNoGravity(true);drop.setNeverPickUp();l.addFreshEntity(drop);
  p.connection.teleport(BASE.getX()+.5,BASE.getY()+1.2,BASE.getZ()-2.5,0,15);p.containerMenu.broadcastChanges();
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.reservoirAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;var s=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==100)s.execute(()->{try{setup(s.getPlayerList().getPlayer(id));}catch(RuntimeException ex){failed=String.valueOf(ex);ex.printStackTrace();}});
  if(ticks==200)s.execute(()->{var p=s.getPlayerList().getPlayer(id);var l=p.serverLevel();var r=at(l,RES);var alembic=(AlembicEntity)l.getBlockEntity(RES.below());
   check(r.essentia.getAmount(Aspect.FIRE)>=15&&alembic.state.amount<=5,"pulls essentia from the alembic below, 1 per 5 ticks: ignis="+r.essentia.getAmount(Aspect.FIRE)+" left="+alembic.state.amount);
   var wand=WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood"));p.setItemInHand(InteractionHand.MAIN_HAND,wand);var hit=new BlockHitResult(Vec3.atCenterOf(RES),Direction.NORTH,RES,false);
   l.getBlockState(RES).useItemOn(wand,l,p,InteractionHand.MAIN_HAND,hit);check(r.facing==Direction.SOUTH,"wand turns it to the opposite of the clicked side "+r.facing);
   p.setShiftKeyDown(true);l.getBlockState(RES).useItemOn(wand,l,p,InteractionHand.MAIN_HAND,hit);p.setShiftKeyDown(false);check(r.facing==Direction.NORTH,"sneaking wand turns it to the clicked side "+r.facing);
   int before=r.essentia.getAmount(Aspect.WATER);var drainer=l.getBlockEntity(RES.below());check(EssentiaSources.drain(drainer,Aspect.WATER,8)&&r.essentia.getAmount(Aspect.WATER)==before-1,"acts as an essentia source for infusion-style drains");
   l.destroyBlock(BOOM,false);int flux=0;for(var at:BlockPos.betweenClosed(BOOM.offset(-5,-5,-5),BOOM.offset(5,5,5)))if(l.getBlockState(at).is(TC4Modern.FLUX_GOO.get())||l.getBlockState(at).is(TC4Modern.FLUX_GAS.get()))flux++;check(flux>=1,"breaking a full reservoir spills flux goo/gas: "+flux);});
  if(ticks==215){check(EssentiaReservoirClient.rendered>0,"reservoir renderer drew "+EssentiaReservoirClient.rendered);var m=mc.getItemRenderer().getModel(new ItemStack(EssentiaReservoir.ITEM.get()),mc.level,mc.player,0);check(m!=mc.getModelManager().getMissingModel(),"item model");LiquidDeathAudit.shot(mc,"reservoir-world.png");}
  if(ticks==220)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(ticks==235){LiquidDeathAudit.shot(mc,"reservoir-inventory.png");mc.setScreen(null);System.out.println(failed==null?"TC4_RESERVOIR ALL PASS":"TC4_RESERVOIR FAILED "+failed);mc.stop();}
 }
}
