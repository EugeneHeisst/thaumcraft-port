package dev.eugheist.tc4modern;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** /tc debug scanitems and /tc debug all scan every item for real: pool points, scan marks, repeatable for debug all. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class ScanAllCommandAudit {
 static int ticks,failures;
 static void check(boolean ok,String m){if(!ok)failures++;System.out.println("TC4_SCANALL "+(ok?"PASS ":"FAIL ")+m);}
 static int pool(net.minecraft.server.level.ServerPlayer p){int n=0;var k=ResearchKnowledge.load(p.getPersistentData());for(var a:thaumcraft.api.aspects.Aspect.aspects.values())n+=k.getAspectPoolFor("current",a);return n;}
 static int marks(net.minecraft.server.level.ServerPlayer p){return p.getPersistentData().getCompound(ResearchKnowledge.KEY).getCompound("scanned").size();}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.scanAllAudit"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(++ticks!=40)return;
  server.execute(()->{var p=server.getPlayerList().getPlayer(id);var cmd=server.getCommands();
   p.getPersistentData().remove(ResearchKnowledge.KEY);ResearchKnowledge.initialize(p.getPersistentData());
   int pool0=pool(p);long t=System.currentTimeMillis();
   cmd.performPrefixedCommand(server.createCommandSourceStack(),"tc debug scanitems "+p.getGameProfile().getName());
   int pool1=pool(p),marks1=marks(p);System.out.println("TC4_SCANALL scanitems took "+(System.currentTimeMillis()-t)+" ms pool "+pool0+" -> "+pool1+" marks "+marks1);
   var known=ResearchKnowledge.load(p.getPersistentData());int discovered=0;for(var a:thaumcraft.api.aspects.Aspect.aspects.values())if(known.hasDiscoveredAspect("current",a))discovered++;
   check(marks1>1500,"fresh player: scanitems marks most items scanned ("+marks1+")");
   check(pool1>pool0+1000,"aspect pool grows by real scan points");
   check(discovered>=40,"scanning unlocks compound aspects through repeated passes ("+discovered+")");
   cmd.performPrefixedCommand(server.createCommandSourceStack(),"tc debug scanitems "+p.getGameProfile().getName());
   check(pool(p)==pool1,"second scanitems gives nothing new");
   t=System.currentTimeMillis();cmd.performPrefixedCommand(server.createCommandSourceStack(),"tc debug all "+p.getGameProfile().getName());int pool2=pool(p);
   System.out.println("TC4_SCANALL debug all took "+(System.currentTimeMillis()-t)+" ms pool -> "+pool2);
   check(pool2>pool1,"debug all rescans items for points even after earlier scans");
   check(p.getPersistentData().getCompound(ResearchKnowledge.KEY).getBoolean("debugAllItemScans"),"debug all still marks every item variant scanned");
   cmd.performPrefixedCommand(server.createCommandSourceStack(),"tc debug all "+p.getGameProfile().getName());
   check(pool(p)>pool2,"repeated debug all pays scans again");
   System.out.println("TC4_SCANALL "+(failures==0?"ALL PASS":"FAILURES "+failures));mc.execute(mc::stop);});
 }
}
