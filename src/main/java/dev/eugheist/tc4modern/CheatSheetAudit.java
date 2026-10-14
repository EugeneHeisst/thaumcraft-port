package dev.eugheist.tc4modern;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;

/** Dedicated checks of the original Thaumonomicon: cheat sheet behind allow_cheat_sheet, sibling repair and the creative tab entry. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class CheatSheetAudit {
 static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_CHEAT_SHEET FAIL "+text);System.out.println("TC4_CHEAT_SHEET PASS "+text);}
 static java.util.List<ItemStack> tab(){var out=new java.util.ArrayList<ItemStack>();ModCreativeTab.fill(new net.minecraft.world.item.CreativeModeTab.Output(){public void accept(ItemStack s,net.minecraft.world.item.CreativeModeTab.TabVisibility v){out.add(s);}});return out;}
 @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e){if(!Boolean.getBoolean("tc4modern.cheatSheetAudit"))return;var server=e.getServer();try{
  check(ResearchConfig.cheatSheet(),"actual TOML enables allow_cheat_sheet");
  var l=server.overworld();var p=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(l);var data=p.getPersistentData();
  thaumcraft.common.config.ConfigResearchData.init();int all=0;for(var c:ResearchCategories.researchCategories.values())all+=c.research.size();
  var items=tab();int book=-1,cheat=-1;for(int i=0;i<items.size();i++){if(items.get(i).is(TC4Modern.THAUMONOMICON.get()))book=i;if(items.get(i).is(TC4Modern.THAUMONOMICON_CHEAT.get()))cheat=i;}
  check(book>=0&&cheat==book+1,"creative tab lists the cheat sheet right after the book while enabled");
  var stack=new ItemStack(TC4Modern.THAUMONOMICON_CHEAT.get());check(stack.getRarity()==net.minecraft.world.item.Rarity.EPIC&&new ItemStack(TC4Modern.THAUMONOMICON.get()).getRarity()==net.minecraft.world.item.Rarity.UNCOMMON&&stack.getHoverName().getString().equals(new ItemStack(TC4Modern.THAUMONOMICON.get()).getHoverName().getString()),"cheat sheet keeps the book name with epic rarity, the book is uncommon");
  thaumcraft.common.lib.events.CommandThaumcraft.resetResearch(data);var knowledge=ResearchKnowledge.load(data);int undiscovered=0;for(var a:Aspect.aspects.values())if(!knowledge.hasDiscoveredAspect("current",a))undiscovered++;
  int before=ResearchProgress.completed(data).size();p.setItemInHand(InteractionHand.MAIN_HAND,stack);stack.getItem().use(l,p,InteractionHand.MAIN_HAND);
  int after=ResearchProgress.completed(data).size();check(before<all&&after==all,"enabled cheat sheet completes every research ("+before+" -> "+after+" of "+all+")");
  knowledge=ResearchKnowledge.load(data);boolean aspects=true;for(var a:Aspect.aspects.values())aspects&=knowledge.hasDiscoveredAspect("current",a);check(undiscovered>0&&aspects,"enabled cheat sheet discovers all "+undiscovered+" missing aspects");
  var granted=knowledge;check(Aspect.aspects.values().stream().filter(a->!a.isPrimal()).allMatch(a->granted.getAspectPoolFor("current",a)>=50),"each previously undiscovered aspect receives the original 50 points");
  ResearchConfig.CHEAT_SHEET.set(false);check(tab().stream().noneMatch(s->s.is(TC4Modern.THAUMONOMICON_CHEAT.get())),"disabled option hides the cheat sheet from the creative tab");
  thaumcraft.common.lib.events.CommandThaumcraft.resetResearch(data);before=ResearchProgress.completed(data).size();stack.getItem().use(l,p,InteractionHand.MAIN_HAND);check(ResearchProgress.completed(data).size()==before,"disabled cheat sheet behaves like the ordinary book");
  thaumcraft.api.research.ResearchItem withSibling=null;for(var c:ResearchCategories.researchCategories.values())for(var r:c.research.values())if(withSibling==null&&r.siblings!=null&&r.siblings.length>0&&!r.isAutoUnlock()&&java.util.Arrays.stream(r.siblings).noneMatch(s->ResearchCategories.getResearch(s)==null||ResearchCategories.getResearch(s).isAutoUnlock()))withSibling=r;
  check(withSibling!=null,"found research with siblings: "+(withSibling==null?"none":withSibling.key+" -> "+String.join(",",withSibling.siblings)));
  ResearchProgress.add(data,withSibling.key);var done=ResearchProgress.completed(data);boolean missing=java.util.Arrays.stream(withSibling.siblings).noneMatch(done::contains);
  var book2=new ItemStack(TC4Modern.THAUMONOMICON.get());p.setItemInHand(InteractionHand.MAIN_HAND,book2);book2.getItem().use(l,p,InteractionHand.MAIN_HAND);done=ResearchProgress.completed(data);
  check(missing&&java.util.Arrays.stream(withSibling.siblings).allMatch(done::contains),"opening the book repairs missing siblings of completed research");
  ResearchConfig.CHEAT_SHEET.set(true);thaumcraft.common.lib.events.CommandThaumcraft.resetResearch(data);
  System.out.println("TC4_CHEAT_SHEET ALL PASS original cheat sheet, creative entry and book sibling repair");
 }finally{server.halt(false);}}
}
