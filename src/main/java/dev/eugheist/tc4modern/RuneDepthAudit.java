package dev.eugheist.tc4modern;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** A crafting infusion altar behind a stone wall: its pedestal runes and essentia trails must not show through the wall. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class RuneDepthAudit {
 static int ticks;
 static int purple(net.minecraft.client.Minecraft mc){try(var img=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){int w=img.getWidth(),h=img.getHeight(),n=0;
  for(int x=0;x<w;x+=2)for(int y=0;y<h;y+=2){int c=img.getPixelRGBA(x,y);int r=c&255,g=c>>8&255,b=c>>16&255;if(r>g+50&&b>g+50)n++;}return n;}}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.runeDepthAudit"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
  if(mc.screen!=null)mc.setScreen(null);
  if(ticks==80)server.execute(()->{var p=server.getPlayerList().getPlayer(id);InfusionAudit.prepare(p);var l=p.serverLevel();
   for(int x=-6;x<=6;x++)for(int y=-3;y<=4;y++)l.setBlock(InfusionAudit.POS.offset(x,y,-5),Blocks.STONE.defaultBlockState(),3);
   var jarPos=InfusionAudit.POS.offset(0,0,-7);l.setBlock(jarPos,EssentiaDevices.JAR.get().defaultBlockState(),3);var jar=(WardedJarEntity)l.getBlockEntity(jarPos);jar.state.aspect=thaumcraft.api.aspects.Aspect.WATER;jar.state.amount=32;jar.changed();
   p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(TC4Modern.GOGGLES.get()));});
  if(ticks>80&&ticks<400)server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null){p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();var c=InfusionAudit.POS;p.connection.teleport(c.getX()+.5,c.getY()-1.3,c.getZ()-8.5,0,8);}});
  if(ticks==200||ticks==260||ticks==320){LiquidDeathAudit.shot(mc,"rune-depth-"+ticks+".png");System.out.println("TC4_RUNE_DEPTH tick "+ticks+" crafting="+(InfusionAudit.matrix!=null&&InfusionAudit.matrix.logic.crafting)+" purple pixels "+purple(mc));}
  if(ticks==330)mc.stop();
 }
}
