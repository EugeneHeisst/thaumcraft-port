package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.research.ResearchCategories;

/**
 * 0.17.137 crash with Thaumic Tinkerer: its uppercase cap key ICHOR became the item id tc4modern:ICHOR_cap in the browser.
 * With every research granted (addon ones included), every entry icon resolves and every category tab renders.
 */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class AddonBookAudit {
 static int ticks,tab,grid,hover;static volatile boolean ready;static java.util.List<String> categories;static ThaumonomiconScreen book;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_ADDONBOOK FAIL "+s);System.out.println("TC4_ADDONBOOK PASS "+s);}
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.addonBookAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==60)server.execute(()->{thaumcraft.common.config.ConfigResearchData.init();var p=server.getPlayerList().getPlayer(id);for(var cat:ResearchCategories.researchCategories.values())for(var r:cat.research.values())DevCommands.grant(p.getPersistentData(),r.key,new java.util.HashSet<>());ThaumometerItem.sync(p,"");ready=true;});
  if(ticks==100){check(ready,"all research granted");int total=0;
   for(var cat:ResearchCategories.researchCategories.values())for(var r:cat.research.values()){total++;try{BasicsBookRecipes.researchIcon(r.key);}catch(RuntimeException e){check(false,"icon "+r.category+"/"+r.key+": "+e);}}
   check(ResearchCategories.getResearch("CAP_ICHOR")!=null,"Thaumic Tinkerer CAP_ICHOR is registered");
   var inkItem=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse("ttinkerer:infused_inkwell"));var ink=new net.minecraft.world.item.ItemStack(inkItem);
   check(ink.getMaxDamage()==800&&ink.isDamageableItem()&&ink.hasFoil()&&ScribingToolsItem.isScribe(ink)&&ScribingToolsItem.hasInk(ink),"infused inkwell: 800-use glinting IScribeTools");
   var table=new ResearchTableMenu(0,new net.minecraft.world.entity.player.Inventory(mc.player));check(table.getSlot(0).mayPlace(ink),"research table ink slot accepts the infused inkwell");
   var empty=ink.copy();empty.setDamageValue(800);check(!ScribingToolsItem.hasInk(empty),"empty infused inkwell has no ink");
   var refill=mc.level.getRecipeManager().byKey(net.minecraft.resources.ResourceLocation.parse("ttinkerer:infused_inkwell_refill"));check(refill.isPresent(),"original 8-ink-sac refill recipe");
   var infusion=InfusionItemRecipes.all().stream().filter(r->r.key().equals("INFUSED_INKWELL")).findFirst();check(infusion.isPresent()&&infusion.get().research().equals("INFUSED_INKWELL")&&ResearchCategories.getResearch("INFUSED_INKWELL")==null,"infusion kept behind the never-registered INFUSED_INKWELL research like the original");
   check(mc.getItemRenderer().getModel(ink,mc.level,mc.player,0)!=mc.getModelManager().getMissingModel(),"infused inkwell uses the TC4 inkwell model");
   var ichor=BasicsBookRecipes.researchIcon("CAP_ICHOR");check(ichor!=null&&!ichor.stack().isEmpty(),"CAP_ICHOR icon is the addon cap item "+(ichor==null?null:ichor.stack()));
   check(true,"all "+total+" research icons resolve without exceptions");
   for(var r:ResearchCategories.getResearchList("THAUMICTINKERER").research.values())check(!r.getName().startsWith("tc.research_name.")&&!r.getName().startsWith("ttresearch.")&&!r.getText().startsWith("tc.research_text.")&&!r.getText().startsWith("ttresearch."),"translated name/text "+r.key+" = "+r.getName()+" / "+r.getText());
   for(var r:ResearchCategories.getResearchList("THAUMICTINKERER").research.values()){String icon=BasicsBookRecipes.researchIcon(r.key)!=null?"basics":r.icon_item!=null&&!r.icon_item.isEmpty()?net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(r.icon_item.getItem())+"#"+r.icon_item.getComponentsPatch():r.icon_resource!=null?r.icon_resource+(mc.getResourceManager().getResource(r.icon_resource).isPresent()?"":" MISSING"):"NONE";System.out.println("TC4_ADDONBOOK ICON "+r.key+" = "+icon);}
   categories=new java.util.ArrayList<>(ResearchCategories.researchCategories.keySet());var inv=mc.player.getInventory();book=new ThaumonomiconScreen(new ThaumonomiconMenu(0,inv),inv,Component.empty());mc.setScreen(book);}
  if(ticks>100&&categories!=null&&tab<categories.size()&&ticks%10==5){var cat=categories.get(tab);book.auditBrowser(cat,0,0);}
  if(ticks>100&&categories!=null&&tab<categories.size()&&ticks%10==9){LiquidDeathAudit.shot(mc,"addonbook-"+categories.get(tab).toLowerCase(java.util.Locale.ROOT)+".png");System.out.println("TC4_ADDONBOOK PASS rendered tab "+categories.get(tab));tab++;}
  if(categories!=null&&tab==categories.size()){tab++;book.auditBrowser("BASICS",0,0);double sc=mc.getWindow().getGuiScale();mouse(mc,(book.auditLeft()-12)*sc,(book.auditTop()+24+12)*sc);hover=ticks;}
  if(hover>0&&ticks==hover+5){LiquidDeathAudit.shot(mc,"addonbook-tab-hover.png");mouse(mc,0,0);
   // Reopen on the last tab: pick ALCHEMY at a panned spot, close, open a fresh book.
   book.auditBrowser("ALCHEMY",3,2);var pan=book.auditPan();mc.setScreen(null);var inv=mc.player.getInventory();book=new ThaumonomiconScreen(new ThaumonomiconMenu(0,inv),inv,Component.empty());mc.setScreen(book);
   check(book.auditCategory().equals("ALCHEMY")&&book.auditPan()[0]==pan[0]&&book.auditPan()[1]==pan[1],"reopened book shows the last closed tab ALCHEMY at the same map position");hover=-ticks;}
  if(hover<0&&ticks==-hover+5){LiquidDeathAudit.shot(mc,"addonbook-reopen-last-tab.png");mc.setScreen(new Grid());grid=ticks;hover=0;}
  if(grid>0&&ticks==grid+10){LiquidDeathAudit.shot(mc,"addonbook-tt-all-icons.png");System.out.println("TC4_ADDONBOOK ALL PASS");mc.stop();}
 }
 static void mouse(Minecraft mc,double x,double y){try{for(var name:new String[]{"xpos","ypos"}){var f=net.minecraft.client.MouseHandler.class.getDeclaredField(name);f.setAccessible(true);f.setDouble(mc.mouseHandler,name.equals("xpos")?x:y);}}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
 /** Every THAUMICTINKERER entry with the same icon the browser draws, labelled, for a one-shot visual check. */
 static final class Grid extends net.minecraft.client.gui.screens.Screen {
  Grid(){super(Component.literal("TT"));}
  @Override public boolean isPauseScreen(){return false;}
  @Override public void renderBackground(net.minecraft.client.gui.GuiGraphics gui,int mx,int my,float p){}
  @Override public void render(net.minecraft.client.gui.GuiGraphics gui,int mx,int my,float p){gui.fill(0,0,width,height,0xff1d1724);var list=new java.util.ArrayList<>(ResearchCategories.getResearchList("THAUMICTINKERER").research.values());list.sort(java.util.Comparator.comparing(r->r.key));int cols=6,rows=(list.size()+cols-1)/cols;
   for(int i=0;i<list.size();i++){var r=list.get(i);int x=4+(i%cols)*(width-8)/cols,y=4+(i/cols)*((height-8)/rows);var basics=BasicsBookRecipes.researchIcon(r.key);
    if(basics!=null)BasicsBookRecipes.icon(gui,basics,x,y);else if(r.icon_item!=null&&!r.icon_item.isEmpty())gui.renderItem(r.icon_item,x,y);else if(r.icon_resource!=null)gui.blit(r.icon_resource,x,y,0,0,16,16,16,16);
    gui.pose().pushPose();gui.pose().translate(x+18,y+4,0);gui.pose().scale(.5f,.5f,1);gui.drawString(font,r.key,0,0,0xd5c6e7,false);gui.pose().popPose();}}
 }
}
