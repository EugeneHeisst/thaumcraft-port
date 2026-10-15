package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Crystal clusters placed, dropped and held: item sizes and the original slight translucency. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class CrystalViewAudit {
 static int ticks;static BlockPos base;
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.crystalViewAudit"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();ticks++;
  if(mc.screen!=null)mc.setScreen(null);
  if(ticks==40){var r=new java.util.Random();base=new BlockPos(28000+r.nextInt(3000),200,28000+r.nextInt(3000));server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();l.setDayTime(6000);
   for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)for(int y=-1;y<=4;y++)l.setBlock(base.offset(x,y,z),y==-1?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState(),3);
   for(int i=0;i<7;i++)l.setBlock(base.offset(i-3,0,-2),BasicsContent.CLUSTERS.get(i).get().defaultBlockState(),3);
   l.setBlock(base.offset(0,0,-4),Blocks.GOLD_BLOCK.defaultBlockState(),3);
   for(int i=0;i<3;i++){var item=new ItemEntity(l,base.getX()-1+i+.5,base.getY()+.2,base.getZ()+.5,new ItemStack(BasicsContent.CLUSTERS.get(i*2).get()));item.setDeltaMovement(0,0,0);item.setPickUpDelay(32767);item.setNoGravity(false);l.addFreshEntity(item);}
   p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();p.getInventory().setItem(0,new ItemStack(BasicsContent.CLUSTERS.get(2).get()));p.getInventory().selected=0;});}
  if(ticks>40&&ticks<300&&base!=null)server.execute(()->{var p=server.getPlayerList().getPlayer(id);if(p!=null)p.connection.teleport(base.getX()+.5,base.getY()+.3,base.getZ()+2.6,180,25);});
  if(ticks==160)LiquidDeathAudit.shot(mc,"crystal-view.png");
  if(ticks==165){System.out.println("TC4_CRYSTAL_VIEW shot");mc.stop();}
 }
}
