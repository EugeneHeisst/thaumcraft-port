package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.network.PacketDistributor;

/** Real client/server container clicks, not direct mutations of the accessory slots. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class BaublesClickAudit {
 static int ticks; static volatile boolean checked;
 static void require(boolean ok,String what){if(!ok)throw new IllegalStateException("TC4_BAUBLES FAIL "+what);System.out.println("TC4_BAUBLES PASS "+what);}
 static void click(Minecraft mc,int slot,ClickType type){mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId,slot,0,type,mc.player);}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.baublesClickAuditOnly"))return;
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
  ticks++;var server=mc.getSingleplayerServer();var uuid=mc.player.getUUID();
  if(Boolean.getBoolean("tc4modern.baublesReload")) {
   if(ticks==50)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);var b=BaublesInventory.of(p);require(!b.getItem(0).isEmpty()&&!b.getItem(1).isEmpty()&&!b.getItem(3).isEmpty(),"saved accessories survived actual client restart");require(b.getItem(0).getHoverName().getString().equals("Baubles sync audit"),"saved component survived restart");require(p.getInventory().isEmpty(),"no duplicated inventory items after restart");checked=true;});
   if(ticks==80){require(checked,"restart server checks completed");require(BaublesInventory.of(mc.player).getItem(0).getHoverName().getString().equals("Baubles sync audit"),"login sync without opening menu");PacketDistributor.sendToServer(BaublesNetwork.Open.INSTANCE);}
   if(ticks==110){require(mc.player.containerMenu.getSlot(9).getItem().getHoverName().getString().equals("Baubles sync audit"),"reopened menu authoritative");LiquidDeathAudit.shot(mc,"baubles-after-restart.png");}
   if(ticks==125){System.out.println("TC4_BAUBLES RESTART ALL PASS");mc.stop();}return;
  }
  if(ticks==50)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getInventory().clearContent();BaublesInventory.of(p).clearContent();p.getInventory().setItem(9,new ItemStack(BuiltInRegistries.ITEM.get(AlchemyContent.id("vis_amulet_lesser"))));p.getInventory().setItem(10,new ItemStack(BuiltInRegistries.ITEM.get(AlchemyContent.id("focus_pouch"))));p.getInventory().setItem(11,new ItemStack(MinersRingItem.RING.get()));p.inventoryMenu.broadcastChanges();require(BaublesInventory.of(p)!=BaublesInventory.of(mc.player),"client and server storage have distinct identity");});
  if(ticks==80)PacketDistributor.sendToServer(BaublesNetwork.Open.INSTANCE);
  if(ticks==110){require(mc.player.containerMenu instanceof BaublesMenu,"server opened expanded inventory");click(mc,13,ClickType.PICKUP);}
  if(ticks==120)click(mc,9,ClickType.PICKUP);
  if(ticks==130)click(mc,14,ClickType.PICKUP);
  if(ticks==140)click(mc,12,ClickType.PICKUP);
  if(ticks==165){require(mc.player.containerMenu.getSlot(13).getItem().isEmpty()&&mc.player.containerMenu.getSlot(14).getItem().isEmpty(),"client source slots empty");require(!mc.player.containerMenu.getSlot(9).getItem().isEmpty()&&!mc.player.containerMenu.getSlot(12).getItem().isEmpty(),"client accessory slots equipped");LiquidDeathAudit.shot(mc,"baubles-click-equipped.png");server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);require(p.getInventory().getItem(9).isEmpty()&&p.getInventory().getItem(10).isEmpty(),"server source slots empty");require(!BaublesInventory.of(p).getItem(0).isEmpty()&&!BaublesInventory.of(p).getItem(3).isEmpty(),"server accessory slots equipped");checked=true;});}
  if(ticks==185){require(checked,"server checks completed");click(mc,9,ClickType.QUICK_MOVE);click(mc,12,ClickType.QUICK_MOVE);}
  if(ticks==210){require(mc.player.containerMenu.getSlot(9).getItem().isEmpty()&&mc.player.containerMenu.getSlot(12).getItem().isEmpty(),"shift-click unequip");LiquidDeathAudit.shot(mc,"baubles-click-unequipped.png");click(mc,13,ClickType.QUICK_MOVE);click(mc,14,ClickType.QUICK_MOVE);click(mc,15,ClickType.QUICK_MOVE);}
  if(ticks==235){require(!mc.player.containerMenu.getSlot(9).getItem().isEmpty()&&!mc.player.containerMenu.getSlot(10).getItem().isEmpty()&&!mc.player.containerMenu.getSlot(12).getItem().isEmpty(),"survival shift-click equips amulet ring and belt");click(mc,9,ClickType.QUICK_MOVE);click(mc,10,ClickType.QUICK_MOVE);click(mc,12,ClickType.QUICK_MOVE);}
  if(ticks==260)server.execute(()->server.getPlayerList().getPlayer(uuid).setGameMode(net.minecraft.world.level.GameType.CREATIVE));
  if(ticks==280){click(mc,13,ClickType.QUICK_MOVE);click(mc,14,ClickType.QUICK_MOVE);click(mc,15,ClickType.QUICK_MOVE);}
  if(ticks==300){require(mc.player.getInventory().isEmpty(),"creative transfer consumes source rather than duplicating");mc.player.closeContainer();mc.setScreen(null);}
  if(ticks==320)server.execute(()->{var p=server.getPlayerList().getPlayer(uuid);var b=BaublesInventory.of(p);require(p.getInventory().isEmpty(),"server inventory remains empty in creative");b.getItem(0).set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Baubles sync audit"));b.setChanged();});
  if(ticks==345){require(BaublesInventory.of(mc.player).getItem(0).getHoverName().getString().equals("Baubles sync audit"),"server component change syncs outside menu");System.out.println("TC4_BAUBLES ALL PASS");mc.stop();}
 }
}
