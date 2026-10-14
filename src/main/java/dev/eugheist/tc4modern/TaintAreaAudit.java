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
   System.out.printf("TC4_TAINT band=%s tainted=%.2f%% of area (%.2f%% of land), magical_forest=%.2f%%%n",System.getProperty("tc4modern.taintBand",".1"),100.0*taint/total,100.0*taint/Math.max(1,land),100.0*forest/total);
   mc.execute(mc::stop);});}
}
