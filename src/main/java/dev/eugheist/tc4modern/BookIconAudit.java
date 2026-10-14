package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Book icon check (flag bookIconAuditOnly via runFocalAudit): the deconstruction table in the BASICS tree and on its arcane recipe page. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class BookIconAudit {
 static int ticks; static volatile boolean granted;
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.bookIconAuditOnly"))return;
  var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
  ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)server.execute(()->{var p=server.getPlayerList().getPlayer(id);thaumcraft.common.config.ConfigResearchData.init();for(var cat:thaumcraft.api.research.ResearchCategories.researchCategories.values())for(var r:cat.research.values())DevCommands.grant(p.getPersistentData(),r.key,new java.util.HashSet<>());ThaumometerItem.sync(p,"");granted=true;});
  if(ticks==70){if(!granted)throw new IllegalStateException("TC4_BOOK_ICON FAIL grant");var inv=mc.player.getInventory();var book=new ThaumonomiconScreen(new ThaumonomiconMenu(0,inv),inv,net.minecraft.network.chat.Component.empty());mc.setScreen(book);}
  if(ticks==75){var r=thaumcraft.api.research.ResearchCategories.getResearch("DECONSTRUCTOR");((ThaumonomiconScreen)mc.screen).auditBrowser("BASICS",r.displayColumn,r.displayRow);}
  if(ticks==85)LiquidDeathAudit.shot(mc,"book-deconstructor-tree.png");
  if(ticks==90)((ThaumonomiconScreen)mc.screen).auditOpen(thaumcraft.api.research.ResearchCategories.getResearch("DECONSTRUCTOR"),0);
  if(ticks==100)LiquidDeathAudit.shot(mc,"book-deconstructor-page.png");
  if(ticks==105){mc.setScreen(null);mc.player.getInventory().selected=0;}
  if(ticks==110)server.execute(()->server.getPlayerList().getPlayer(id).getInventory().setItem(0,new net.minecraft.world.item.ItemStack(BasicsContent.DECONSTRUCTOR.get())));
  if(ticks==125){LiquidDeathAudit.shot(mc,"book-deconstructor-hotbar.png");System.out.println("TC4_BOOK_ICON ALL PASS");mc.stop();}
 }
}
