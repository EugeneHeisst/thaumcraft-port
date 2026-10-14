package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Real thaumometer scans with fresh knowledge: original discovery/unknown hints arrive as notifications, no overlay text. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class ThaumometerHintAudit {
 static int ticks,failures;static BlockPos pos;
 static void check(boolean ok,String m){if(!ok)failures++;System.out.println("TC4_HINT "+(ok?"PASS ":"FAIL ")+m);}
 static boolean noted(String text){return ClientKnowledge.notifications.update(System.nanoTime()/1_000_000L).stream().anyMatch(e->e.text().equals(text));}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.thaumometerHintAudit"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
  if(mc.screen!=null)mc.setScreen(null);
  if(ticks==40){var r=new java.util.Random();pos=new BlockPos(20000+r.nextInt(5000),250,20000+r.nextInt(5000));server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();
   l.setBlockAndUpdate(pos,Blocks.IRON_BLOCK.defaultBlockState());p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();
   p.getPersistentData().remove(ResearchKnowledge.KEY);ResearchKnowledge.initialize(p.getPersistentData());p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(TC4Modern.THAUMOMETER.get()));p.getInventory().selected=0;
   ThaumometerItem.sync(p,"");});}
  if(ticks>=40&&ticks<200&&pos!=null)server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null)p.connection.teleport(pos.getX()+.5,pos.getY()+1.2,pos.getZ()+.5,0,90);});
  if(ticks==70)LiquidDeathAudit.shot(mc,"hint-hold-idle.png");
  if(ticks==90){check(mc.player.blockPosition().distManhattan(pos.above())<=2,"player at test block");mc.gui.getChat().clearMessages(false);mc.options.keyUse.setDown(true);}
  if(ticks==140){mc.options.keyUse.setDown(false);check(mc.gui.getChat().getRecentChat().isEmpty()&&((java.util.List<?>)net.neoforged.fml.util.ObfuscationReflectionHelper.getPrivateValue(net.minecraft.client.gui.components.ChatComponent.class,mc.gui.getChat(),"allMessages")).isEmpty(),"scanning writes nothing to chat");LiquidDeathAudit.shot(mc,"hint-discoveryerror.png");
   var want=net.minecraft.network.chat.Component.translatable("tc.discoveryerror",net.minecraft.network.chat.Component.translatable("tc.aspect.help.vitreus").getString()).getString();
   check(want.contains("crystals")||want.contains("кристаллы"),"original text resolved: "+want);
   check(noted(want),"iron block scan with unknown vitreus shows original notification");
   check(ClientKnowledge.status.isEmpty(),"no overlay status line ("+ClientKnowledge.status+")");}
  if(ticks==150)server.execute(()->ThaumometerItem.sync(server.getPlayerList().getPlayer(id),"unknown"));
  if(ticks==170){var want=net.minecraft.network.chat.Component.translatable("tc.unknownobject").getString();check(noted(want),"unknown object notification: "+want);LiquidDeathAudit.shot(mc,"hint-unknown.png");}
  if(ticks==180){System.out.println("TC4_HINT "+(failures==0?"ALL PASS":"FAILURES "+failures));mc.stop();}
 }
}
