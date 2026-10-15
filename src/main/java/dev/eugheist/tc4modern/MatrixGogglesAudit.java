package dev.eugheist.tc4modern;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Goggles over a running infusion show the essentia the matrix still has to draw (TileInfusionMatrix.getAspects). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class MatrixGogglesAudit {
 static int ticks,failures,first=-1;
 static void check(boolean ok,String m){if(!ok)failures++;System.out.println("TC4_MATRIX_GOGGLES "+(ok?"PASS ":"FAIL ")+m);}
 static int shown(net.minecraft.client.Minecraft mc){var tags=GogglesAspectTags.aspects(mc.level.getBlockEntity(InfusionAudit.POS));return tags==null?0:tags.visSize();}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.matrixGogglesAudit"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
  if(mc.screen!=null)mc.setScreen(null);
  if(ticks==40)server.execute(()->{var p=server.getPlayerList().getPlayer(id);check(shown(mc)==0||!InfusionAudit.prepared,"idle matrix shows nothing");InfusionAudit.prepare(p);p.setItemSlot(EquipmentSlot.HEAD,new ItemStack(TC4Modern.GOGGLES.get()));p.getInventory().selected=8;});
  if(ticks>40&&InfusionAudit.prepared)server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null){var c=InfusionAudit.POS.getCenter();p.connection.teleport(c.x,c.y-1.62,c.z-2.6,0,0);}});
  if(ticks==70){first=shown(mc);check(first>0&&first<=18,"goggles read essentia still needed (of 18) ("+first+")");LiquidDeathAudit.shot(mc,"matrix-goggles-start.png");}
  if(ticks==200){int now=shown(mc);int server_=InfusionAudit.matrix.logic.recipeEssentia.visSize();check(now<first&&now==server_,"shown amount drops as the matrix draws essentia ("+first+" -> "+now+", server "+server_+")");LiquidDeathAudit.shot(mc,"matrix-goggles-mid.png");}
  if(ticks==210){System.out.println("TC4_MATRIX_GOGGLES "+(failures==0?"ALL PASS":"FAILURES "+failures));mc.stop();}
 }
}
