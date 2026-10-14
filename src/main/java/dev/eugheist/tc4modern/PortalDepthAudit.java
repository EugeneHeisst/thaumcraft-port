package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Actual portal and cult banner, alternately behind and in front, on a hidden display. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
final class PortalDepthAudit {
 static final BlockPos BASE=new BlockPos(1040,161,-160);static int ticks;static volatile boolean ready;
 static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_PORTAL_DEPTH FAIL "+text);System.out.println("TC4_PORTAL_DEPTH PASS "+text);}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.portalDepthAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==1)mc.options.renderDistance().set(3);
  if(ticks==20)server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.connection.teleport(BASE.getX()+.5,BASE.getY()-1.12,BASE.getZ()-5,0,0);});
  if(ticks==130)server.execute(()->{var l=server.overworld();for(int x=-6;x<=6;x++)for(int z=-7;z<=6;z++)for(int y=-3;y<=4;y++)l.setBlock(BASE.offset(x,y,z),(y==-3?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);l.setDayTime(6000);l.setBlock(BASE.above(),CrimsonContent.OBELISK_BODY.get().defaultBlockState(),3);l.setBlock(BASE.below(),CrimsonContent.OBELISK_BODY.get().defaultBlockState(),3);l.setBlock(BASE,OuterPortal.BLOCK.get().defaultBlockState(),3);l.setBlock(BASE.south(),CrimsonContent.BANNER.get().defaultBlockState(),3);ready=true;});
  if(ticks==250){check(ready&&mc.level.getBlockEntity(BASE) instanceof OuterPortal.Portal,"actual open portal and banner behind it loaded");mc.options.hideGui=true;LiquidDeathAudit.shot(mc,"portal-banner-behind"+(Boolean.getBoolean("tc4modern.portalBaseline")?"-baseline":"")+".png");}
  if(ticks==280)server.execute(()->{var l=server.overworld();l.removeBlock(BASE.south(),false);l.setBlock(BASE.north(),CrimsonContent.BANNER.get().defaultBlockState(),3);});
  if(ticks==350)LiquidDeathAudit.shot(mc,"portal-banner-front"+(Boolean.getBoolean("tc4modern.portalBaseline")?"-baseline":"")+".png");
  if(ticks==380)server.execute(()->{var l=server.overworld();l.removeBlock(BASE.north(),false);l.setBlock(BASE.south().east(),CrimsonContent.BANNER.get().defaultBlockState(),3);});
  if(ticks==450)LiquidDeathAudit.shot(mc,"portal-banner-edge"+(Boolean.getBoolean("tc4modern.portalBaseline")?"-baseline":"")+".png");
  if(ticks==550){mc.options.hideGui=false;System.out.println("TC4_PORTAL_DEPTH ALL PASS actual behind/front/transparent-edge scenes captured");}
  if(ticks==650)mc.stop();
 }
}
