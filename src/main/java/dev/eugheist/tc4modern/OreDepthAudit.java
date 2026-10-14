package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Fresh far-away chunks: infused stones and cinnabar span the whole height, deepslate variants below, depth-weighted primals. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class OreDepthAudit {
 static int ticks;static boolean failed;
 static void check(boolean b,String s){System.out.println((b?"TC4_ORES PASS ":"TC4_ORES FAIL ")+s);if(!b)failed=true;}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.oreDepthAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;if(++ticks!=40)return;
  var server=mc.getSingleplayerServer();server.execute(()->{var l=server.overworld();
   String[] names=TC4Modern.SHARD_NAMES;int[][] band=new int[2][6];int[] deepslate=new int[6],stone=new int[6];int cinStone=0,cinDeep=0,cinBelow0=0,infBelow0=0;
   int cx0=12000+(int)(System.nanoTime()%400)*16,cz0=12000;int chunks=0;
   for(int cx=0;cx<12;cx++)for(int cz=0;cz<12;cz++){var chunk=l.getChunk(cx0+cx,cz0+cz,ChunkStatus.FULL,true);chunks++;
    var base=chunk.getPos();
    for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=l.getMinBuildHeight();y<l.getMaxBuildHeight();y++){
     var st=chunk.getBlockState(new BlockPos(base.getMinBlockX()+x,y,base.getMinBlockZ()+z));var b=st.getBlock();
     if(b==AlchemyContent.CINNABAR_ORE.get()){cinStone++;if(y<0)cinBelow0++;}
     else if(b==AlchemyContent.DEEPSLATE_CINNABAR_ORE.get()){cinDeep++;if(y<0)cinBelow0++;}
     else if(b instanceof ShardOreBlock ore){boolean deep=TC4Modern.DEEPSLATE_SHARD_ORES.get(ore.variant).get()==b;if(deep)deepslate[ore.variant]++;else stone[ore.variant]++;band[y<0?0:1][ore.variant]++;if(y<0)infBelow0++;}
    }}
   System.out.println("TC4_ORES chunks="+chunks+" cinnabar stone="+cinStone+" deepslate="+cinDeep+" below0="+cinBelow0);
   for(int v=0;v<6;v++)System.out.println("TC4_ORES "+names[v]+": stone="+stone[v]+" deepslate="+deepslate[v]+" below0="+band[0][v]+" above0="+band[1][v]);
   check(cinDeep>0&&cinBelow0>0,"deepslate cinnabar generates below y=0");
   check(infBelow0>0&&java.util.Arrays.stream(deepslate).sum()>0,"deepslate infused stones generate below y=0");
   int deepLow=band[0][1]+band[0][3]+band[0][5],highLow=band[0][0]+band[0][2]+band[0][4],deepHigh=band[1][1]+band[1][3]+band[1][5],highHigh=band[1][0]+band[1][2]+band[1][4];
   check(deepLow>highLow,"below y=0 fire+earth+entropy ("+deepLow+") outnumber air+water+order ("+highLow+")");
   check(highHigh>deepHigh,"above y=0 air+water+order ("+highHigh+") outnumber fire+earth+entropy ("+deepHigh+")");
   var p=server.getPlayerList().getPlayers().get(0);eye=p.blockPosition().above(40);
   for(int x=-6;x<=6;x++)for(int y=-2;y<=4;y++)for(int z=-1;z<=6;z++)l.setBlock(eye.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),2);
   var row=new java.util.ArrayList<net.minecraft.world.level.block.Block>();row.add(AlchemyContent.CINNABAR_ORE.get());row.add(AlchemyContent.DEEPSLATE_CINNABAR_ORE.get());
   for(int v=0;v<6;v++){row.add(TC4Modern.SHARD_ORES.get(v).get());row.add(TC4Modern.DEEPSLATE_SHARD_ORES.get(v).get());}
   for(int i=0;i<row.size();i++)l.setBlock(eye.offset(-7+i,i%2==0?1:0,4),row.get(i).defaultBlockState(),2);
   p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);p.teleportTo(l,eye.getX()+.5,eye.getY()+1,eye.getZ()-5.5,0,10);
   System.out.println(failed?"TC4_ORES DONE WITH FAILURES":"TC4_ORES ALL PASS");ready=true;});
  }
 static int shotAt=-1;static BlockPos eye;static volatile boolean ready;
 @SubscribeEvent static void shot(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.oreDepthAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null)return;if(mc.screen!=null&&ticks>40)mc.setScreen(null);++frame;if(ready&&shotAt<0)shotAt=frame+60;if(eye!=null&&shotAt>0&&frame<shotAt){var srv=mc.getSingleplayerServer();var id=mc.player.getUUID();srv.execute(()->{var pl=srv.getPlayerList().getPlayer(id);if(pl!=null)pl.teleportTo(srv.overworld(),eye.getX()+.5,eye.getY()+1,eye.getZ()-5.5,0,10);});}if(frame!=shotAt)return;LiquidDeathAudit.shot(mc,"deepslate-ores.png");mc.stop();}
 static int frame;
 static void unused(){
  }
}
