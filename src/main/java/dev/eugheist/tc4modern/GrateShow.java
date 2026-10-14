package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Visible check of the iron grate (BlockMetalDevice 5/6): open and closed in the world, held, dropped and in the inventory. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class GrateShow {
 static final BlockPos BASE=new BlockPos(680,121,-90);static int ticks;static volatile String failed;
 static void check(boolean b,String s){if(!b){failed=s;System.out.println("TC4_GRATE FAIL "+s);}else System.out.println("TC4_GRATE PASS "+s);}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.grateShowOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;var s=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)s.execute(()->{var p=s.getPlayerList().getPlayer(id);var l=p.serverLevel();if(p.level()!=s.overworld())p.teleportTo(s.overworld(),BASE.getX(),BASE.getY(),BASE.getZ(),0,0);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();l.setDayTime(6000);
   l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(12)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
   for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=-1;y<5;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.GRASS_BLOCK:Blocks.AIR).defaultBlockState(),3);
   l.setBlock(BASE.offset(-1,1,0),ArtificeDevices.GRATE.get().defaultBlockState(),3);l.setBlock(BASE.offset(1,1,0),ArtificeDevices.GRATE.get().defaultBlockState().setValue(ArtificeDevices.GrateBlock.CLOSED,true),3);
   l.setBlock(BASE.offset(-1,0,0),Blocks.GOLD_BLOCK.defaultBlockState(),3);l.setBlock(BASE.offset(1,0,0),Blocks.GOLD_BLOCK.defaultBlockState(),3);
   p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(ArtificeDevices.GRATE.get()));p.getInventory().selected=0;
   var drop=new ItemEntity(l,BASE.getX()+.5,BASE.getY()+.3,BASE.getZ()-1.5,new ItemStack(ArtificeDevices.GRATE.get()));drop.setDeltaMovement(Vec3.ZERO);drop.setNoGravity(true);drop.setNeverPickUp();l.addFreshEntity(drop);
   p.connection.teleport(BASE.getX()+.5,BASE.getY()+2.6,BASE.getZ()-2.2,0,55);p.containerMenu.broadcastChanges();});
  if(ticks==80){var m=mc.getItemRenderer().getModel(new ItemStack(ArtificeDevices.GRATE.get()),mc.level,mc.player,0);check(m!=mc.getModelManager().getMissingModel(),"grate item model");LiquidDeathAudit.shot(mc,"grate-top.png");}
  if(ticks==85)s.execute(()->s.getPlayerList().getPlayer(id).connection.teleport(BASE.getX()+.5,BASE.getY()+1.7,BASE.getZ()-2.6,0,15));
  if(ticks==110)LiquidDeathAudit.shot(mc,"grate-side.png");
  if(ticks==115)s.execute(()->s.getPlayerList().getPlayer(id).connection.teleport(BASE.getX()-1+.5,BASE.getY()+1.1,BASE.getZ()+.5,0,-80));
  if(ticks==140)LiquidDeathAudit.shot(mc,"grate-from-below.png");
  if(ticks==145)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(ticks==160){LiquidDeathAudit.shot(mc,"grate-inventory.png");mc.setScreen(null);s.execute(()->{var p=s.getPlayerList().getPlayer(id);p.getInventory().setItem(1,new ItemStack(SanityChecker.ITEM.get()));p.getInventory().selected=1;var st=PlayerWarp.load(p);st.set(WarpState.Kind.PERMANENT,12);st.set(WarpState.Kind.STICKY,9);st.set(WarpState.Kind.TEMPORARY,15);PlayerWarp.save(p,st);WarpNetwork.sync(p);p.containerMenu.broadcastChanges();p.connection.teleport(BASE.getX()+.5,BASE.getY()+1,BASE.getZ()-3,0,10);});}
  if(ticks==170)mc.player.getInventory().selected=1;
  if(ticks==190){check(SanityChecker.Hud.rendered>0,"sanity checker HUD drawn "+SanityChecker.Hud.rendered);LiquidDeathAudit.shot(mc,"sanity-checker-hud.png");System.out.println(failed==null?"TC4_GRATE ALL PASS":"TC4_GRATE FAILED "+failed);mc.stop();}
 }
}
