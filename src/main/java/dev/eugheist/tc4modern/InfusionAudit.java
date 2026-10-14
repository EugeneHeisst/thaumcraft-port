package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import thaumcraft.api.aspects.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Real altar formation, jar draining, ingredient consumption, persistence and research pages. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class InfusionAudit {
 static final BlockPos POS=FocalAudit.BASE.offset(40,3,40);
 static int ticks;static volatile boolean prepared,finished;static InfusionContent.MatrixEntity matrix;
 static final List<BlockPos> components=List.of(POS.offset(-3,-2,0),POS.offset(3,-2,0),POS.offset(0,-2,3));
 static void check(boolean value,String message){if(!value)throw new IllegalStateException("TC4_INFUSION FAIL "+message);System.out.println("TC4_INFUSION PASS "+message);}
 static void click(ServerPlayer p,ItemStack wand){InfusionContent.MATRIX.get().defaultBlockState().useItemOn(wand,p.level(),p,InteractionHand.MAIN_HAND,new BlockHitResult(POS.getCenter(),Direction.UP,POS,false));}
 static void prepare(ServerPlayer p){var level=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();p.removeAllEffects();
  for(var cat:thaumcraft.api.research.ResearchCategories.researchCategories.values())for(var r:cat.research.values())DevCommands.grant(p.getPersistentData(),r.key,new HashSet<>());ThaumometerItem.sync(p,"");
  for(int x=-12;x<=12;x++)for(int z=-12;z<=12;z++)for(int y=-5;y<=5;y++)level.setBlock(POS.offset(x,y,z),y==-3?Blocks.STONE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
  level.setBlock(POS,InfusionContent.MATRIX.get().defaultBlockState(),3);level.setBlock(POS.below(2),InfusionContent.PEDESTAL.get().defaultBlockState(),3);
  for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){level.setBlock(POS.offset(x,-1,z),TC4Modern.ARCANE_STONE.get().defaultBlockState(),3);level.setBlock(POS.offset(x,-2,z),TC4Modern.ARCANE_STONE_BRICKS.get().defaultBlockState(),3);}
  matrix=(InfusionContent.MatrixEntity)level.getBlockEntity(POS);var wand=WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood"));for(var a:Aspect.getPrimalAspects())WandData.add(wand,a,50,true);p.setItemInHand(InteractionHand.MAIN_HAND,wand);
  check(InfusionContent.fitAltar(level,POS.getX()-1,POS.getY()-2,POS.getZ()-1),"original3x3x3 altar blueprint");var empty=WandParts.make(WandParts.CAPS.get("gold"),WandParts.RODDEFS.get("greatwood"));check(!InfusionContent.createAltar(empty,p,level,POS),"insufficient vis cannot form altar");var research=p.getPersistentData().getCompound(ResearchKnowledge.KEY);var savedResearch=research.getList("Research",8).copy();research.getList("Research",8).removeIf(tag->tag.getAsString().equals("INFUSION"));check(!InfusionContent.createAltar(wand,p,level,POS),"unresearched player cannot form altar with a charged wand");research.put("Research",savedResearch);int before=WandData.get(wand,Aspect.ORDER);click(p,wand);check(matrix.logic.active&&matrix.logic.validLocation(matrix)&&WandData.get(wand,Aspect.ORDER)<before,"actual wand interaction forms four pillars and spends all-primal vis");
  for(var key:List.of("InfusionMatrix","ArcanePedestal")){var recipe=ArcaneRecipes.entries().stream().filter(e->e.key().equals(key)).findFirst().orElseThrow();var grid=recipe.ingredients().stream().map(i->i.isEmpty()?ItemStack.EMPTY:i.getItems()[0].copy()).toList();check(recipe.matches(grid,Set.of("INFUSION"))&&!recipe.matches(grid,Set.of()),"arcane recipe research gate "+key);}
  for(var pos:components){level.setBlock(pos,InfusionContent.PEDESTAL.get().defaultBlockState(),3);((InfusionContent.PedestalEntity)level.getBlockEntity(pos)).setItem(new ItemStack(InfusionContent.SALIS.get()));}
  var center=(InfusionContent.PedestalEntity)level.getBlockEntity(POS.below(2));center.setItem(new ItemStack(WandParts.THAUMIUM_CAP_INERT.get()));
  matrix.logic.getSurroundings(matrix);check(matrix.logic.symmetry>0,"unpaired occupied pedestal penalises stability");
  int skulls=0;for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++)if((x!=0||z!=0)&&level.getBlockState(POS.offset(x,-2,z)).isAir()){level.setBlock(POS.offset(x,-2,z),Blocks.SKELETON_SKULL.defaultBlockState(),2);skulls++;}
  matrix.logic.getSurroundings(matrix);check(matrix.logic.symmetry<-5&&skulls>100,"original opposite skull pairs stabilise altar");click(p,wand);check(matrix.logic.crafting&&matrix.logic.recipeEssentia.visSize()==18&&matrix.logic.recipeIngredients.size()==3,"actual wand starts researched18essentia three-component cap recipe");
  for(int i=0;i<2;i++){var pos=POS.offset(i*2-1,-2,-4);level.setBlock(pos,EssentiaDevices.JAR.get().defaultBlockState(),3);var jar=(WardedJarEntity)level.getBlockEntity(pos);jar.state.aspect=i==0?Aspect.ENERGY:Aspect.AURA;jar.state.amount=i==0?12:6;jar.changed();}
  var saved=matrix.saveWithFullMetadata(level.registryAccess());var copy=new InfusionContent.MatrixEntity(POS,matrix.getBlockState());copy.loadWithComponents(saved,level.registryAccess());check(copy.logic.crafting&&copy.logic.recipeEssentia.visSize()==18&&copy.logic.recipeIngredients.size()==3&&copy.logic.recipeInput.is(WandParts.THAUMIUM_CAP_INERT.get()),"in-progress infusion saves recipe input components aspects and player");
  p.connection.teleport(POS.getX()+6,POS.getY()+2,POS.getZ()-7,40,25);p.getInventory().setItem(1,new ItemStack(TC4Modern.THAUMONOMICON.get()));p.getInventory().selected=8;prepared=true;
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){if(!Boolean.getBoolean("tc4modern.infusionAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==80)server.execute(()->prepare(server.getPlayerList().getPlayer(id)));if(ticks==120){check(prepared,"server setup complete");LiquidDeathAudit.shot(mc,"infusion-altar.png");}if(ticks==200||ticks==330)LiquidDeathAudit.shot(mc,"infusion-crafting-"+ticks+".png");
  if(ticks==500)server.execute(()->{var level=server.overworld();var center=(InfusionContent.PedestalEntity)level.getBlockEntity(POS.below(2));check(!matrix.logic.crafting&&center.item().is(WandParts.THAUMIUM_CAP.get()),"natural server ticks finish infusion with actual output");check(components.stream().allMatch(pos->((InfusionContent.PedestalEntity)level.getBlockEntity(pos)).item().isEmpty()),"all three pedestal ingredients consumed");for(int i=0;i<2;i++)check(((WardedJarEntity)level.getBlockEntity(POS.offset(i*2-1,-2,-4))).state.amount==0,"exact original essentia drained from jar"+i);finished=true;});
  if(ticks==510){check(finished,"server crafting finished");LiquidDeathAudit.shot(mc,"infusion-finished.png");var inv=mc.player.getInventory();mc.setScreen(new ThaumonomiconScreen(new ThaumonomiconMenu(0,inv),inv,net.minecraft.network.chat.Component.empty()));}
  if(ticks>=520&&ticks<=550&&ticks%10==0){var book=(ThaumonomiconScreen)mc.screen;var r=thaumcraft.api.research.ResearchCategories.getResearch("INFUSION");book.auditOpen(r,(ticks-520)/10*2);}
  if(ticks>=525&&ticks<=555&&ticks%10==5)LiquidDeathAudit.shot(mc,"infusion-book-"+(ticks-525)/10+".png");if(ticks==560)mc.stop();
 }
}
