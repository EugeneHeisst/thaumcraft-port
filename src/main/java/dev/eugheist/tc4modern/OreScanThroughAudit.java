package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Fire pickaxe ore scan (RenderEventHandler.startScan): ore markers hidden inside stone must render through it. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class OreScanThroughAudit {
 static int ticks;static BlockPos base;
 static int warm(net.minecraft.client.Minecraft mc){try(var img=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){int w=img.getWidth(),h=img.getHeight(),n=0;
  for(int x=0;x<w;x+=2)for(int y=0;y<h;y+=2){int c=img.getPixelRGBA(x,y);int r=c&255,g=c>>8&255,b=c>>16&255;if(r>b+60&&r>120)n++;}return n;}}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.oreScanThroughAudit"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
  if(mc.screen!=null)mc.setScreen(null);
  if(ticks==40){var r=new java.util.Random();base=new BlockPos(-25000-r.nextInt(3000),200,25000+r.nextInt(3000));server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();l.setDayTime(6000);
   for(int x=-4;x<=4;x++)for(int y=-4;y<=4;y++)for(int z=-4;z<=4;z++)l.setBlock(base.offset(x,y,z),Blocks.STONE.defaultBlockState(),3);
   for(var o:new BlockPos[]{base,base.offset(1,1,0),base.offset(-1,0,1),base.offset(0,-1,-1)})l.setBlock(o,Blocks.DIAMOND_ORE.defaultBlockState(),3);
   p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);});}
  if(ticks>40&&ticks<400&&base!=null)server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null)p.connection.teleport(base.getX()+.5,base.getY()-1.1,base.getZ()+7.5,180,0);});
  if(ticks==150&&mc.level.getBlockState(base).is(Blocks.DIAMOND_ORE)){ElementalToolsClient.startScan(base.offset(0,0,4));}
  if(ticks==160){LiquidDeathAudit.shot(mc,"ore-scan-through.png");System.out.println("TC4_ORE_SCAN scanned "+ElementalToolsClient.scanned.size()+" warm pixels "+warm(mc));}
  if(ticks==165)mc.stop();
 }
}
