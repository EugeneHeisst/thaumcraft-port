package dev.eugheist.tc4modern;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** A huge stabiliser field (very negative instability) must not throw the active matrix cubes apart. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class MatrixSpreadAudit {
 static int ticks;
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.matrixSpreadAudit"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
  if(mc.screen!=null)mc.setScreen(null);
  if(ticks==60)server.execute(()->{var p=server.getPlayerList().getPlayer(id);InfusionAudit.prepare(p);});
  if(ticks>60&&ticks<400&&InfusionAudit.prepared)server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p==null)return;InfusionAudit.matrix.logic.instability=-100;InfusionAudit.matrix.sync();var c=InfusionAudit.POS.getCenter();p.connection.teleport(c.x,c.y-1.62,c.z-2.4,0,0);});
  if(ticks==200||ticks==230){LiquidDeathAudit.shot(mc,"matrix-spread-"+ticks+".png");var t=(InfusionContent.MatrixEntity)mc.level.getBlockEntity(InfusionAudit.POS);System.out.println("TC4_MATRIX_SPREAD client instability "+(t==null?"?":t.logic.instability)+" startUp "+(t==null?"?":t.startUp)+" craftCount "+(t==null?"?":t.craftCount));}
  if(ticks==235)mc.stop();
 }
}
