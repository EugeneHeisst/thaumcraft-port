package dev.eugheist.tc4modern;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Share of Tainted Land / Magical Forest by area in a 4096x4096 square of fresh terrain (biome source sampling at surface height). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class TaintAreaAudit {
 static int ticks;
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.taintAreaAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;if(++ticks!=40)return;
  var server=mc.getSingleplayerServer();server.execute(()->{var l=server.overworld();var cs=l.getChunkSource();var src=cs.getGenerator().getBiomeSource();var sampler=cs.randomState().sampler();
   int taint=0,forest=0,total=0,land=0;int ox=40000,oz=-40000;
   for(int x=0;x<4096;x+=16)for(int z=0;z<4096;z+=16){var b=src.getNoiseBiome((ox+x)>>2,64>>2,(oz+z)>>2,sampler);total++;
    if(b.is(TaintBiomes.TAINTED_LAND))taint++;else if(b.unwrapKey().map(k->k.location().getPath().equals("magical_forest")).orElse(false))forest++;
    if(!b.is(net.minecraft.tags.BiomeTags.IS_OCEAN)&&!b.is(net.minecraft.tags.BiomeTags.IS_RIVER))land++;}
   for(String key:new String[]{"tc4modern:tainted_land","minecraft:cherry_grove"}){
    boolean[][] g=new boolean[256][256];
    for(int x=0;x<256;x++)for(int z=0;z<256;z++)g[x][z]=src.getNoiseBiome((ox+x*16)>>2,64>>2,(oz+z*16)>>2,sampler).unwrapKey().map(k->k.location().toString().equals(key)).orElse(false);
    var sizes=new java.util.ArrayList<Integer>();boolean[][] seen=new boolean[256][256];
    for(int x=0;x<256;x++)for(int z=0;z<256;z++)if(g[x][z]&&!seen[x][z]){int n=0;var st=new java.util.ArrayDeque<int[]>();st.push(new int[]{x,z});seen[x][z]=true;
     while(!st.isEmpty()){var q=st.pop();n++;for(int[] d:new int[][]{{1,0},{-1,0},{0,1},{0,-1}}){int a=q[0]+d[0],b=q[1]+d[1];if(a>=0&&b>=0&&a<256&&b<256&&g[a][b]&&!seen[a][b]){seen[a][b]=true;st.push(new int[]{a,b});}}}
     sizes.add(n);}
    sizes.sort(null);int sum=sizes.stream().mapToInt(Integer::intValue).sum();
    System.out.printf("TC4_TAINT %s patches=%d total=%d chunks median=%d max=%d chunks%n",key,sizes.size(),sum,sizes.isEmpty()?0:sizes.get(sizes.size()/2),sizes.isEmpty()?0:sizes.get(sizes.size()-1));}
   {int t=0,c=0,n=0;for(int x=0;x<16384;x+=64)for(int z=0;z<16384;z+=64){var b=src.getNoiseBiome((x-8192)>>2,64>>2,(z-8192)>>2,sampler);n++;if(b.is(TaintBiomes.TAINTED_LAND))t++;else if(b.unwrapKey().map(k->k.location().getPath().equals("cherry_grove")).orElse(false))c++;}
    System.out.printf("TC4_TAINT wide 16384^2: tainted=%.2f%% cherry_grove=%.2f%%%n",100.0*t/n,100.0*c/n);}
   System.out.printf("TC4_TAINT band=%s tainted=%.2f%% of area (%.2f%% of land), magical_forest=%.2f%%%n",System.getProperty("tc4modern.taintBand",".3334"),100.0*taint/total,100.0*taint/Math.max(1,land),100.0*forest/total);
   // Real chunk generation around a predicted patch: generated Tainted Land must not exceed the biome source's patch.
   int fx=0,fz=0;boolean found=false;int base=200000+(int)(System.currentTimeMillis()/1000%5000)*64;
   for(int x=0;x<16384&&!found;x+=64)for(int z=0;z<16384&&!found;z+=64)if(src.getNoiseBiome((x+base)>>2,64>>2,(z+base)>>2,sampler).is(TaintBiomes.TAINTED_LAND)){fx=x+base;fz=z+base;found=true;}
   if(found){int predicted=0,generated=0,R=15;int cx0=fx>>4,cz0=fz>>4;
    for(int cx=-R;cx<=R;cx++)for(int cz=-R;cz<=R;cz++){boolean p=false;for(int qx=0;qx<4;qx++)for(int qz=0;qz<4;qz++)p|=src.getNoiseBiome(((cx0+cx)<<2)+qx,64>>2,((cz0+cz)<<2)+qz,sampler).is(TaintBiomes.TAINTED_LAND);if(p)predicted++;}
    for(int cx=-R;cx<=R;cx++)for(int cz=-R;cz<=R;cz++){var ch=l.getChunk(cx0+cx,cz0+cz,net.minecraft.world.level.chunk.status.ChunkStatus.FULL,true);boolean g=false;for(int qx=0;qx<4;qx++)for(int qz=0;qz<4;qz++)g|=ch.getNoiseBiome(((cx0+cx)<<2)+qx,64>>2,((cz0+cz)<<2)+qz).is(TaintBiomes.TAINTED_LAND);if(g)generated++;}
    System.out.printf("TC4_TAINT generation check near %d,%d: predicted tainted chunks=%d generated=%d of %d -> %s%n",fx,fz,predicted,generated,(2*R+1)*(2*R+1),generated<=predicted*1.3+4?"TC4_TAINT PASS no cascade":"TC4_TAINT FAIL taint cascades");}
   else System.out.println("TC4_TAINT no patch found for the generation check");
   mc.execute(mc::stop);});}
}
