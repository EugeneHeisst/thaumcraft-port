package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Jars under a solid floor feeding a matrix above it: the essentia trail must stay hidden below the floor (looked at from above). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class FloorTrailAudit {
 static int ticks,start=-1;static BlockPos base;static int maxAbove;
 static int red(net.minecraft.client.Minecraft mc){try(var img=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){int w=img.getWidth(),h=img.getHeight(),red=0;
  for(int x=0;x<w;x+=2)for(int y=0;y<h;y+=2){int c=img.getPixelRGBA(x,y);int r=c&255,g=c>>8&255,b=c>>16&255;if(r>g+60&&r>b+60)red++;}return red;}}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.floorTrailAudit"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
  if(mc.screen!=null)mc.setScreen(null);
  if(ticks==40){var r=new java.util.Random();base=new BlockPos(25000+r.nextInt(3000),200,-25000-r.nextInt(3000));server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();l.setDayTime(6000);
   for(int x=-8;x<=8;x++)for(int z=-8;z<=8;z++)for(int y=-5;y<=6;y++)l.setBlock(base.offset(x,y,z),y==0?Blocks.STONE_BRICKS.defaultBlockState():y==-5?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
   for(int i=-1;i<=1;i++)l.setBlock(base.offset(i*3,-2,-3),EssentiaDevices.JAR.get().defaultBlockState(),3);
   p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);});}
  if(ticks>40&&ticks<1000&&base!=null)server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null)p.connection.teleport(base.getX()+.5,base.getY()+4,base.getZ()+5.5,180,40);});
  if(start<0&&ticks>=100&&base!=null&&mc.level.getBlockState(base).is(Blocks.STONE_BRICKS)&&mc.level.getBlockState(base.offset(0,-2,-3)).is(EssentiaDevices.JAR.get()))start=ticks;
  if(start>0&&ticks>=start&&ticks<=start+120&&ticks%2==0)for(int i=-1;i<=1;i++){var from=Vec3.atCenterOf(base.offset(i*3,-2,-3));var to=Vec3.atCenterOf(base.offset(0,3,0));mc.particleEngine.add(new InfusionFx.EssentiaTrail(mc.level,from,to,ticks,0xFF2020,1.5f));}
  if(start>0&&(ticks==start+60||ticks==start+100||ticks==start+130)){LiquidDeathAudit.shot(mc,"floor-trail-"+ticks+".png");System.out.println("TC4_FLOOR_TRAIL tick "+ticks+" red pixels "+red(mc));}
  if(start>0&&ticks==start+145||ticks==900){System.out.println("TC4_FLOOR_TRAIL highest trail y "+(InfusionFx.trailMaxY-base.getY())+" (floor top at 1.0)");mc.stop();}
 }
}
