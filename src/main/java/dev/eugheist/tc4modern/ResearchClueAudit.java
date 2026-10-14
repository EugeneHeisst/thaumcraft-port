package dev.eugheist.tc4modern;

import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Dedicated checks that every original setItemTriggers research has a port clue, through the real candidate search. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class ResearchClueAudit {
 static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_RESEARCH_CLUE FAIL "+text);System.out.println("TC4_RESEARCH_CLUE PASS "+text);}
 static boolean offers(String item,String research){return ResearchClues.candidates(Set.of(),null,item,null).contains(research);}
 @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e){if(!Boolean.getBoolean("tc4modern.researchClueAudit"))return;try{
  thaumcraft.common.config.ConfigResearchData.init();
  /* scanned blocks report their block id; both Outer stones must also carry aspects or the scan never awards a clue */
  for(var block:java.util.List.of(OuterContent.GLYPHED.get(),OuterDevices.TRAP.get())){var state=block.defaultBlockState();check(BlockAspectBridge.key(state).equals(BuiltInRegistries.BLOCK.getKey(block).toString())&&!BlockAspectBridge.get(state,e.getServer().overworld()).isEmpty(),"scanning "+BlockAspectBridge.key(state)+" gives its block id and aspects "+BlockAspectBridge.get(state,e.getServer().overworld()));}
  for(var id:Set.of("tc4modern:focus_primal","tc4modern:primordial_pearl","tc4modern:crimson_banner","tc4modern:quicksilver"))check(BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(id)),"trigger item exists "+id);
  check(offers("tc4modern:glyphed_stone","OUTERREV")&&offers("tc4modern:runed_stone","OUTERREV")&&!offers("tc4modern:crusted_opening","OUTERREV"),"glyphed and runed stone (blockEldritch 5 and 10) reveal OUTERREV");
  check(offers("tc4modern:focus_primal","ROD_primal_staff"),"the primal focus reveals ROD_primal_staff");
  check(offers("tc4modern:primordial_pearl","PRIMPEARL")&&offers("tc4modern:crimson_banner","BANNERS")&&offers("minecraft:bone","BONEBOW")&&offers("tc4modern:quicksilver","JARBRAIN")&&offers("minecraft:ender_pearl","MIRROR")&&offers("minecraft:red_wool","TINYHAT"),"the other original item triggers still match");
  check(ResearchClues.candidates(Set.of(),"Thaumcraft.PrimalOrb",null,null).contains("ROD_primal_staff"),"the primal orb entity trigger still matches");
  System.out.println("TC4_RESEARCH_CLUE ALL PASS");
 }finally{e.getServer().halt(false);}}
}
