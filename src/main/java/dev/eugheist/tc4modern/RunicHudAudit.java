package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Native runic shield HUD: original survival-only gate, translucent runes, empty charge, unequip and relog reset. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class RunicHudAudit {
 static int ticks; static volatile boolean checked;
 static void require(boolean ok,String what){if(!ok)throw new IllegalStateException("TC4_RUNIC_HUD FAIL "+what);System.out.println("TC4_RUNIC_HUD PASS "+what);}
 static int[] client(){try{var c=RunicRingShield.Client.class;var f=c.getDeclaredField("charge");var g=c.getDeclaredField("max");f.setAccessible(true);g.setAccessible(true);return new int[]{f.getInt(null),g.getInt(null)};}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
 static void serverDo(Minecraft mc,java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> action){var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();server.execute(()->action.accept(server.getPlayerList().getPlayer(uuid)));}
 static void charge(net.minecraft.server.level.ServerPlayer p,int value){var max=RunicRingShield.info(p)[0];p.getPersistentData().putInt("TC4RunicCharge",value);p.getPersistentData().putInt("TC4RunicMax",max);RunicRingShield.send(p,value,max);}
 static void mode(Minecraft mc,GameType type){serverDo(mc,p->{p.setGameMode(type);p.getAbilities().flying=type!=GameType.SURVIVAL;p.onUpdateAbilities();});}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.runicHudAuditOnly"))return;
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
  ticks++;
  if(Boolean.getBoolean("tc4modern.runicHudReload")){
   if(ticks==5){var c=client();require(c[0]==0,"relog client charge reset");}
   if(ticks==60){var c=client();require(c[1]==13&&c[0]==0,"relog recomputes capacity without free charge");LiquidDeathAudit.shot(mc,"runic-hud-relog-empty.png");serverDo(mc,p->charge(p,13));}
   if(ticks==90){var c=client();require(c[0]==13,"relog charge syncs");LiquidDeathAudit.shot(mc,"runic-hud-relog-full.png");System.out.println("TC4_RUNIC_HUD RELOAD ALL PASS");mc.stop();}
   return;
  }
  if(ticks==40)serverDo(mc,p->{var level=p.serverLevel();level.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL,true);level.setDayTime(6000);var base=p.blockPosition();for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++){level.setBlock(base.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);for(int y=0;y<3;y++)level.setBlock(base.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);}
   p.setGameMode(GameType.SURVIVAL);p.getAbilities().flying=false;p.onUpdateAbilities();p.getInventory().clearContent();RunicAudit.equip(p,RunicAccessories.Kind.RING,RunicAccessories.Kind.AMULET);p.setHealth(20);p.getFoodData().setFoodLevel(20);});
  if(ticks==70)serverDo(mc,p->{require(RunicRingShield.info(p)[0]==13,"ring+amulet capacity13");charge(p,13);});
  if(ticks==95){var c=client();require(c[0]==13&&c[1]==13,"client full charge synced");require(mc.gameMode.canHurtPlayer(),"survival draws HUD");LiquidDeathAudit.shot(mc,"runic-hud-survival-full.png");mode(mc,GameType.CREATIVE);}
  if(ticks==120){require(!mc.gameMode.canHurtPlayer(),"creative gate active");LiquidDeathAudit.shot(mc,"runic-hud-creative.png");mode(mc,GameType.SPECTATOR);}
  if(ticks==145){require(mc.player.isSpectator(),"spectator gate active");LiquidDeathAudit.shot(mc,"runic-hud-spectator.png");mode(mc,GameType.ADVENTURE);}
  if(ticks==170){require(mc.gameMode.canHurtPlayer(),"adventure draws HUD like original survival-or-adventure");serverDo(mc,p->charge(p,6));}
  if(ticks==190){require(client()[0]==6,"partial charge synced");LiquidDeathAudit.shot(mc,"runic-hud-adventure-partial.png");mode(mc,GameType.SURVIVAL);}
  if(ticks==205)serverDo(mc,p->RunicAudit.hit(p,6));
  if(ticks==225){var c=client();require(c[0]==0&&c[1]==13,"real damage drains shield to zero");require(mc.player.getHealth()==20,"shield absorbed all damage");LiquidDeathAudit.shot(mc,"runic-hud-zero.png");serverDo(mc,p->charge(p,13));}
  if(ticks==245)serverDo(mc,p->{for(int i=0;i<4;i++)BaublesInventory.of(p).setItem(i,ItemStack.EMPTY);RunicRingShield.dirty=true;});
  if(ticks==275){var c=client();require(c[1]==0,"unequip clears capacity");LiquidDeathAudit.shot(mc,"runic-hud-unequipped.png");serverDo(mc,p->{RunicAudit.equip(p,RunicAccessories.Kind.RING,RunicAccessories.Kind.AMULET);RunicRingShield.dirty=true;});}
  if(ticks==300){require(client()[1]==13,"re-equip restores capacity");serverDo(mc,p->charge(p,13));}
  if(ticks==320){mode(mc,GameType.CREATIVE);}
  if(ticks==335){mode(mc,GameType.SURVIVAL);}
  if(ticks==355){require(client()[0]==13,"mode switching keeps charge");LiquidDeathAudit.shot(mc,"runic-hud-survival-after-switch.png");System.out.println("TC4_RUNIC_HUD ALL PASS");mc.stop();}
 }
}
