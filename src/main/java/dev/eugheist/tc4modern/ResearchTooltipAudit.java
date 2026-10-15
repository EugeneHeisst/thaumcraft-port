package dev.eugheist.tc4modern;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.research.ResearchCategories;
/** Screenshot of the original-style research hover box in its states: unlockable with warp, purchasable, locked. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class ResearchTooltipAudit {
 static int ticks;
 static final class Board extends Screen {
  Board(){super(Component.literal("tooltips"));}
  @Override public boolean isPauseScreen(){return false;}
  @Override public void render(GuiGraphics gui,int mx,int my,float partial){
   gui.fill(0,0,width,height,0xFF3A2A4A);
   var all=new java.util.HashSet<String>();for(var c:ResearchCategories.researchCategories.values())all.addAll(c.research.keySet());
   String warpKey=all.contains("ICHORCLOTH_ARMOR")?"ICHORCLOTH_ARMOR":all.stream().filter(k->ResearchWarp.amount(k)>2).sorted().findFirst().orElse("ELDRITCHMINOR");
   var inv=minecraft.player.getInventory();
   var w=ResearchCategories.getResearch(warpKey);var done=new java.util.HashSet<>(all);done.remove(warpKey);
   ResearchBrowserTooltip.render(gui,font,w,done,inv,10,20);
   var g=ResearchCategories.getResearch("GOGGLES");var d2=new java.util.HashSet<>(all);d2.remove("GOGGLES");
   ResearchBrowserTooltip.render(gui,font,g,d2,inv,10,90);
   ResearchBrowserTooltip.render(gui,font,ResearchCategories.getResearch("INFUSION"),java.util.Set.of(),inv,10,170);
   var t=ResearchCategories.getResearch("THAUMIUM");ResearchBrowserTooltip.render(gui,font,t,all,inv,200,20);
  }
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.researchTooltipAudit"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;
  if(ticks==40){thaumcraft.common.config.ConfigResearchData.init();mc.setScreen(new Board());}
  if(ticks==60)LiquidDeathAudit.shot(mc,"research-tooltips.png");
  if(ticks==65){System.out.println("TC4_RESEARCH_TOOLTIP shot taken");mc.stop();}
 }
}
