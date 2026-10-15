package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Linked wall mirrors photographed from three camera spots: pane coverage and the star field's parallax. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class MirrorViewAudit {
 static int ticks;static BlockPos base;
 static final double[][] VIEWS={{0,0},{-1.2,0},{1.2,0},{0,.9}};
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.mirrorViewAudit"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
  if(mc.screen!=null)mc.setScreen(null);
  if(ticks==40){var r=new java.util.Random();base=new BlockPos(-30000-r.nextInt(3000),220,-30000-r.nextInt(3000));server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();
   p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);l.setDayTime(6000);
   for(int x=-6;x<=6;x++)for(int y=-3;y<=4;y++)for(int z=-6;z<=6;z++)l.setBlock(base.offset(x,y,z),z==-1?Blocks.GRASS_BLOCK.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
   var a=MagicMirrorsAudit.put(l,base,false,Direction.SOUTH);var b=MagicMirrorsAudit.put(l,base.offset(-4,0,0),false,Direction.SOUTH);MagicMirrorsAudit.pair(a,b);
   var c=MagicMirrorsAudit.put(l,base.offset(2,0,0),true,Direction.SOUTH);var d=MagicMirrorsAudit.put(l,base.offset(4,0,0),true,Direction.SOUTH);MagicMirrorsAudit.pair(c,d);
   for(var m:new BlockPos[]{base,base.offset(2,0,0)}){l.setBlock(m.north(),Blocks.GLASS.defaultBlockState(),3);l.setBlock(m.north(2),Blocks.GOLD_BLOCK.defaultBlockState(),3);}});}
  int view=(ticks-140)/30;
  if(ticks>40&&base!=null&&view<VIEWS.length){var v=VIEWS[Math.max(0,view)];server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null)p.connection.teleport(base.getX()+1.5+v[0],base.getY()-1.12+v[1],base.getZ()+2.6,180f+(float)Math.toDegrees(Math.atan2(v[0],2.6)),(float)(v[1]*15));});}
  if(ticks>=140&&(ticks-140)%30==25&&view<VIEWS.length)LiquidDeathAudit.shot(mc,"mirror-view-"+view+".png");
  if(ticks==140+30*VIEWS.length){System.out.println("TC4_MIRROR_VIEW shots taken");mc.stop();}
 }
}
