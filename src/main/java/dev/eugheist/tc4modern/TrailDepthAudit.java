package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Essentia trail bubbles behind a stone wall must be hidden by it; in front of it they must show (screenshot pixel check). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class TrailDepthAudit {
 static int ticks;static BlockPos eye;
 static int red(net.minecraft.client.Minecraft mc){try(var img=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){int w=img.getWidth(),h=img.getHeight(),red=0;
  for(int x=w/4;x<w*3/4;x+=2)for(int y=h/4;y<h*3/4;y+=2){int c=img.getPixelRGBA(x,y);int r=c&255,g=c>>8&255,b=c>>16&255;if(r>g+40&&r>b+40)red++;}return red;}}
 static void spawn(net.minecraft.client.Minecraft mc,double dist){for(int i=0;i<12;i++){var start=new Vec3(eye.getX()-2.5+i*.45,eye.getY()+1.6,eye.getZ()+dist);var target=new Vec3(eye.getX()+3,eye.getY()+1.7,eye.getZ()+dist);mc.particleEngine.add(new InfusionFx.EssentiaTrail(mc.level,start,target,ticks+i,0xFF2020,1.5f));}}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.trailDepthAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;
  var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  String[] walls={"minecraft:stone","minecraft:oak_leaves","tc4modern:air_crystal_cluster","minecraft:glass","tc4modern:jar"};
  int phase=(ticks-40)/120,t=(ticks-40)%120;
  if(ticks==30)eye=mc.player.blockPosition().above(60);
  if(mc.screen!=null&&ticks>30)mc.setScreen(null);
  if(ticks>=40&&phase<walls.length&&t==0){String wall=walls[phase];server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();l.setDayTime(6000);
   var state=net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(net.minecraft.resources.ResourceLocation.parse(wall)).defaultBlockState();
   for(int x=-10;x<=10;x++)for(int y=-4;y<=8;y++)for(int z=-2;z<=12;z++)l.setBlock(eye.offset(x,y,z),Blocks.AIR.defaultBlockState(),2);
   for(int x=-9;x<=9;x++)for(int y=-4;y<=8;y++)l.setBlock(eye.offset(x,y,5),state,2);
   p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);p.teleportTo(l,eye.getX()+.5,eye.getY(),eye.getZ()+.5,0,0);});}
  if(ticks>=40&&phase<walls.length&&t>=50&&t<=80&&t%2==0)spawn(mc,9);
  if(ticks>=40&&phase<walls.length&&t==82){LiquidDeathAudit.shot(mc,"trail-behind-"+walls[phase].replace(':','_')+".png");System.out.println("TC4_TRAIL behind "+walls[phase]+" red="+red(mc));}
  if(ticks==40+walls.length*120)mc.stop();}}
