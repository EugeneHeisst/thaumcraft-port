package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.research.ResearchCategories;

/**
 * Reported 0.17.136 issues: research pulse in the browser and on unaffordable aspects,
 * the deconstructor ghost vanishing on the client once its last item is split, the ghost drawn after opaque block entities
 * (hungry chest behind it), the original thaumonomicon creation effect, and the removed advanced table id mapping to the ordinary table.
 */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class DeconstructorFixAudit {
 static final BlockPos BASE=new BlockPos(520,121,-90),SINGLE=BASE.offset(3,0,0),SHELF=BASE.offset(1,0,-1);
 static int ticks,consumedBefore,poolBefore;static volatile boolean ready;static Aspect richest;static float bright=-1,dim=-1,missingBright,missingDim,enoughBright,enoughDim;static thaumcraft.api.research.ResearchItem study;
 static void check(boolean b,String s){if(!b)throw new IllegalStateException("TC4_DECONFIX FAIL "+s);System.out.println("TC4_DECONFIX PASS "+s);}
 static thaumcraft.common.lib.research.PlayerKnowledge knowledge(net.minecraft.server.level.ServerPlayer p){return ResearchKnowledge.load(p.getPersistentData());}
 static int pool(net.minecraft.server.level.ServerPlayer p){int n=0;var k=knowledge(p);for(var a:Aspect.aspects.values())n+=k.getAspectPoolFor("current",a);return n;}
 static DeconstructorEntity table(net.minecraft.world.level.Level l,BlockPos at){return (DeconstructorEntity)l.getBlockEntity(at);}
 static java.util.List<ItemStack> grid(net.minecraft.world.item.Item axe,net.minecraft.world.item.Item pick){var g=new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(9,ItemStack.EMPTY));g.set(1,new ItemStack(TC4Modern.THAUMOMETER.get()));g.set(3,new ItemStack(axe));g.set(4,new ItemStack(TC4Modern.TABLE.get()));g.set(5,new ItemStack(pick));return g;}

 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.deconstructorFixAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==40)server.execute(()->{thaumcraft.common.config.ConfigResearchData.init();var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();l.setDayTime(6000);
   l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(16)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
   for(int x=-8;x<=8;x++)for(int z=-6;z<=6;z++)for(int y=-1;y<7;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
   for(var at:java.util.List.of(BASE,SINGLE))l.setBlock(at,BasicsContent.DECONSTRUCTOR.get().defaultBlockState(),3);
   l.setBlock(BASE.offset(0,0,1),Blocks.STONE.defaultBlockState(),3);l.setBlock(BASE.offset(0,1,1),HungryChest.BLOCK.get().defaultBlockState(),3);
   table(l,BASE).setItem(0,new ItemStack(Items.COBBLESTONE,64));table(l,SINGLE).setItem(0,new ItemStack(Items.OAK_LOG));
   var data=p.getPersistentData();ResearchKnowledge.discoverAll(data);DevCommands.grant(data,"DECONSTRUCTOR",new java.util.HashSet<>());
   study=ResearchCategories.getResearch("GOLEMWOOD");var k=knowledge(p);var sorted=study.tags.getAspectsSortedAmount();for(var a:Aspect.aspects.values())k.setAspectPool("current",a,(short)0);k.setAspectPool("current",sorted[0],(short)100);ResearchKnowledge.save(data,k);
   ThaumometerItem.sync(p,"");p.connection.teleport(BASE.getX()+.5,BASE.getY(),BASE.getZ()-2.6,0,14);ready=true;});
  if(ticks==60){check(ready,"fixture built");server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();
    var old=AlchemyContent.id("advanced_deconstructor");check(net.minecraft.core.registries.BuiltInRegistries.BLOCK.get(old)==BasicsContent.DECONSTRUCTOR.get()&&net.minecraft.core.registries.BuiltInRegistries.ITEM.get(old)==BasicsContent.DECONSTRUCTOR.get().asItem(),"removed advanced table id loads as the ordinary table");
    check(ResearchCategories.getResearch("ADVDECONSTRUCTOR")==null&&ArcaneRecipes.find(grid(Items.NETHERITE_AXE,Items.NETHERITE_PICKAXE),java.util.Set.of("DECONSTRUCTOR"))==null,"advanced table research and recipe removed");
   var gold=ArcaneRecipes.find(grid(Items.GOLDEN_AXE,Items.GOLDEN_PICKAXE),java.util.Set.of("DECONSTRUCTOR"));check(gold!=null&&gold.output().is(BasicsContent.DECONSTRUCTOR.get().asItem())&&gold.cost().getAmount(Aspect.ENTROPY)==20,"golden tools still make the ordinary table for 20");
    });}
  if(ticks==200){var client=(DeconstructorEntity)mc.level.getBlockEntity(SINGLE);check(client!=null&&client.getItem(0).isEmpty(),"client copy of the table drops its last item (hologram gone)");}
  if(ticks==210)LiquidDeathAudit.shot(mc,"deconfix-hologram-hungry-chest.png");
  if(ticks==220)server.execute(()->{var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);l.setBlock(SHELF,Blocks.BOOKSHELF.defaultBlockState(),3);var wand=new ItemStack(TC4Modern.BASIC_WAND.get());p.setItemInHand(InteractionHand.MAIN_HAND,wand);
   var result=wand.getItem().onItemUseFirst(wand,new UseOnContext(p,InteractionHand.MAIN_HAND,new BlockHitResult(SHELF.getCenter(),Direction.UP,SHELF,false)));
   var books=l.getEntitiesOfClass(thaumcraft.common.entities.EntitySpecialItem.class,new AABB(SHELF).inflate(1),e->e.getItem().is(TC4Modern.THAUMONOMICON.get()));
   check(result.consumesAction()&&l.isEmptyBlock(SHELF)&&books.size()==1,"wand on bookshelf: special thaumonomicon item replaces the shelf");});
  if(ticks==223)LiquidDeathAudit.shot(mc,"deconfix-thaumonomicon-sparkles.png");
  if(ticks==240){var inv=mc.player.getInventory();var book=new ThaumonomiconScreen(new ThaumonomiconMenu(0,inv),inv,Component.empty());book.auditBrowser("BASICS",6,3);mc.setScreen(book);}
  if(ticks>=250&&ticks<300&&mc.screen instanceof ThaumonomiconScreen){float pulse=ThaumonomiconScreen.pulse();if(bright<0&&pulse>.96f){bright=pulse;LiquidDeathAudit.shot(mc,"deconfix-book-pulse-bright.png");}if(dim<0&&pulse<.54f){dim=pulse;LiquidDeathAudit.shot(mc,"deconfix-book-pulse-dim.png");}}
  if(ticks==300){check(bright>.96f&&dim<.54f,"available research pulses 0.5..1.0 like GuiResearchBrowser bright="+bright+" dim="+dim);var book=(ThaumonomiconScreen)mc.screen;book.auditOpen(ResearchCategories.getResearch("DECONSTRUCTOR"),0);}
  if(ticks==310)LiquidDeathAudit.shot(mc,"deconfix-deconstructor-page.png");
  if(ticks==320){bright=dim=-1;mc.setScreen(new Tooltip());}
  if(ticks>=330&&ticks<400&&mc.screen instanceof Tooltip){float pulse=ThaumonomiconScreen.pulse();if((bright<0&&pulse>.96f)||(dim<0&&pulse<.54f)){var shot=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget());double scale=mc.getWindow().getGuiScale();float enough=luma(shot,Tooltip.X,Tooltip.Y,scale),missing=luma(shot,Tooltip.X+18,Tooltip.Y,scale);shot.close();
   if(pulse>.96f){bright=pulse;enoughBright=enough;missingBright=missing;LiquidDeathAudit.shot(mc,"deconfix-tooltip-bright.png");}else{dim=pulse;enoughDim=enough;missingDim=missing;LiquidDeathAudit.shot(mc,"deconfix-tooltip-dim.png");}}}
  if(ticks==400){System.out.println("TC4_DECONFIX tooltip enough "+enoughBright+"/"+enoughDim+" missing "+missingBright+"/"+missingDim);check(bright>0&&dim>0,"tooltip captured at both pulse extremes");
   check(Math.abs(enoughBright-enoughDim)<=enoughBright*.03f,"affordable aspect stays steady");check(missingDim<missingBright*.8f,"unaffordable aspect blinks like the original");mc.setScreen(null);}
   if(ticks==410){System.out.println("TC4_DECONFIX ALL PASS");mc.stop();}
 }
 /** Mean r+g+b over a 16x16 GUI icon in the captured frame. */
 static float luma(com.mojang.blaze3d.platform.NativeImage im,int x,int y,double scale){long sum=0;int n=0;for(int px=(int)(x*scale);px<(int)((x+16)*scale);px++)for(int py=(int)(y*scale);py<(int)((y+16)*scale);py++){int c=im.getPixelRGBA(px,py);sum+=(c&255)+(c>>8&255)+(c>>16&255);n++;}return n==0?0:sum/(float)n;}
 /** The research study tooltip component alone, at a fixed spot over black, so icon pixels can be measured. */
 static final class Tooltip extends Screen {
  static final int X=40,Y=40;
  Tooltip(){super(Component.literal("study"));}
  @Override public boolean isPauseScreen(){return false;}
  @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial){gui.fill(0,0,width,height,0xff000000);ClientTooltipComponent.create(new ResearchBrowserTooltip.Study(study,Component.literal("study"))).renderImage(font,X,Y,gui);}
  @Override public void renderBackground(GuiGraphics gui,int mouseX,int mouseY,float partial){}
 }
}
