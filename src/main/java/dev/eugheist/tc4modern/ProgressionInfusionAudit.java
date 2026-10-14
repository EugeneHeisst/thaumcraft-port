package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.common.lib.utils.HexUtils;

/** Continuation of the same novice: supplied gathering targets/materials, never granted research, vis or essentia. */
final class ProgressionInfusionAudit {
 static ServerLevel level;static ServerPlayer player;static ResearchTableEntity table;static ResearchTableMenu menu;
 static ItemStack wand;static int nodes,phase,start,now;static InfusionContent.MatrixEntity matrix;
 static final BlockPos ALTAR=ProgressionAudit.BASE.offset(20,2,0),FURNACE=ProgressionAudit.BASE.offset(10,0,8),SMELTER=ProgressionAudit.BASE.offset(12,0,8);
 static final List<BlockPos> COMPONENTS=List.of(ALTAR.offset(-3,-2,0),ALTAR.offset(3,-2,0),ALTAR.offset(0,-2,3));
 static void check(boolean b,String s){ProgressionAudit.check(b,s);}
 static int pool(Aspect a){return ResearchKnowledge.load(player.getPersistentData()).getAspectPoolFor("current",a);}
 static void atTable(){var p=ProgressionAudit.BASE.offset(-2,0,0);player.moveTo(p.getX()+.5,p.getY(),p.getZ()+2.5);player.containerMenu=menu;}
 /** Every replenishment is an actual distinct wild node reservoir, with the production scan/clue rules. */
 static void scanNode(){
  check(nodes<1024,"bounded exploration supplies another wild node");var at=ProgressionAudit.BASE.offset(-8,3,-8);level.removeBlock(at,false);
  check(AuraNodeFeature.createRandomNodeAt(level,at,net.minecraft.util.RandomSource.create(1000+nodes++),false,false,false),"ordinary node generation succeeds");
  var node=(AuraNodeEntity)level.getBlockEntity(at);var aspects=node.reservoir.scanAspects(node.nodeType);var entries=new LinkedHashMap<String,Integer>();for(var a:aspects.getAspects())entries.put(a.getTag(),aspects.getAmount(a));
  var result=ResearchKnowledge.scan(player.getPersistentData(),"node:"+node.nodeId,entries,awarded->ResearchClues.revealClue(player,"node:"+node.nodeId,null,awarded));
  check(result.equals("scanned"),"a distinct generated node scan awards ordinary research points");
 }
 static void points(Aspect aspect,int need){
  if(pool(aspect)>=need)return;
  if(aspect.isPrimal()){for(int i=0;i<128&&pool(aspect)<need;i++)scanNode();check(pool(aspect)>=need,"exploration supplies "+aspect.getTag());return;}
  var parts=aspect.getComponents();while(pool(aspect)<need){for(int tries=0;tries<32;tries++){points(parts[0],parts[0]==parts[1]?2:1);points(parts[1],1);if(pool(parts[0])>=(parts[0]==parts[1]?2:1)&&pool(parts[1])>=1)break;}atTable();int before=pool(aspect),gain=ResearchKnowledge.load(player.getPersistentData()).hasDiscoveredAspect("current",aspect)?1:3;CombinePayload.handle(new CombinePayload(menu.containerId,parts[0].getTag(),parts[1].getTag()),ProgressionAudit.context(player));check(pool(aspect)==before+gain,"table combines "+aspect.getTag()+" and spends its parents ("+parts[0].getTag()+"="+pool(parts[0])+","+parts[1].getTag()+"="+pool(parts[1])+", valid="+menu.stillValid(player)+")");}
 }
 /** Unlock only through requests, puzzle placements and reading the discovery, including all actual prerequisites. */
 static void research(String key){
  if(ResearchProgress.completed(player.getPersistentData()).contains(key))return;
  var r=ResearchCategories.getResearch(key);check(r!=null,"research exists: "+key);
  if(r.parents!=null)for(var parent:r.parents)research(parent);if(r.parentsHidden!=null)for(var parent:r.parentsHidden)research(parent);
  if(r.isHidden()||r.isLost())check(ResearchProgress.completed(player.getPersistentData()).contains("@"+key),"ordinary scans revealed the clue for "+key);
  for(var a:r.tags.getAspects())points(a,Math.max(8,r.tags.getAmount(a)));
  atTable();player.getInventory().add(new ItemStack(Items.PAPER));player.getInventory().add(new ItemStack(TC4Modern.SCRIBING_TOOLS.get()));
  player.containerMenu=new ThaumonomiconMenu(12,player.getInventory());ResearchRequestPayload.handle(new ResearchRequestPayload(12,key),ProgressionAudit.context(player));
  if(!ResearchProgress.completed(player.getPersistentData()).contains(key)){
   ItemStack notes=ItemStack.EMPTY;for(int i=0;i<player.getInventory().getContainerSize();i++){var stack=player.getInventory().getItem(i);var n=NoteData.read(stack);if(n!=null&&n.key.equals(key)){notes=stack;break;}}
   check(!notes.isEmpty(),"book produces prerequisite-gated notes for "+key);table.inventory.setItem(1,notes.copy());notes.shrink(1);atTable();
   // Discover intermediate aspects through real combinations before asking the graph for a route.
   for(var a:Aspect.getCompoundAspects())points(a,1);
   var known=new HashSet<Aspect>(Arrays.asList(ResearchKnowledge.load(player.getPersistentData()).getAspectsDiscovered("current").getAspects()));
   var plan=ProgressionAudit.solve(NoteData.read(table.inventory.getItem(1)),known);check(plan!=null,"a legal hex route exists for "+key);
   for(var step:plan){var a=(Aspect)step[1];points(a,1);atTable();var hex=(HexUtils.Hex)step[0];PlaceAspectPayload.handle(new PlaceAspectPayload(menu.containerId,hex.q,hex.r,a.getTag()),ProgressionAudit.context(player));}
   var done=NoteData.read(table.inventory.getItem(1));check(done!=null&&done.complete,"real placements solve "+key);
   var discovery=table.inventory.removeItemNoUpdate(1);player.setItemInHand(InteractionHand.MAIN_HAND,discovery);discovery.getItem().use(level,player,InteractionHand.MAIN_HAND);
  }
  check(ResearchProgress.completed(player.getPersistentData()).contains(key),"novice learns "+key+" without research grants");
 }
 /** Drawing from several generated nodes stands in for searching the world; no direct WandData.add. */
 static void charge(){
  int capacity=WandData.capacity(wand);
  for(int attempt=0;attempt<128;attempt++){
   boolean full=true;for(var a:Aspect.getPrimalAspects())if(WandData.get(wand,a)<capacity)full=false;if(full)return;
   var at=ProgressionAudit.BASE.offset(-8,3,-8);level.removeBlock(at,false);check(AuraNodeFeature.createRandomNodeAt(level,at,net.minecraft.util.RandomSource.create(4000+nodes++),false,false,false),"a charging node generates");
   player.moveTo(at.getX()+.5,at.getY()-1.1,at.getZ()+3.5,180,0);player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,Vec3.atCenterOf(at));player.setItemInHand(InteractionHand.MAIN_HAND,wand);
   wand.getItem().use(level,player,InteractionHand.MAIN_HAND);check(player.isUsingItem(),"the upgraded progression wand draws from a real node");for(int remaining=400;remaining>0;remaining--)wand.getItem().onUseTick(level,player,wand,remaining);player.stopUsingItem();
  }
  throw new IllegalStateException("TC4_PROGRESSION FAIL unable to fill wand from bounded nodes");
 }
 static ItemStack arcane(String key){var e=ArcaneRecipes.entries().stream().filter(x->x.key().equals(key)).findFirst().orElseThrow();return arcane(e);}
 static ItemStack arcane(ArcaneRecipes.Entry e){return arcane(e,null);}
 static ItemStack arcane(ArcaneRecipes.Entry e,List<ItemStack> earned){
  charge();var pos=ProgressionAudit.benchAt;player.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+1.5);var bench=(ArcaneWorkbenchEntity)level.getBlockEntity(pos);
  for(int i=0;i<10;i++)bench.inventory.setItem(i,ItemStack.EMPTY);
  var crafting=new ArcaneWorkbenchMenu(13,player.getInventory(),ContainerLevelAccess.create(level,pos),bench.inventory);player.containerMenu=crafting;
  bench.inventory.setItem(9,wand);player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
  for(int i=0;i<e.ingredients().size();i++){var ing=e.ingredients().get(i);if(!ing.isEmpty())bench.inventory.setItem(i/e.width()*3+i%e.width(),earned==null?ing.getItems()[0].copyWithCount(1):earned.get(i));}
  check(ItemStack.isSameItemSameComponents(crafting.getSlot(0).getItem(),e.output()),"real arcane result unlocks "+e.key());
  int before=ProgressionAudit.vis(wand);crafting.clicked(0,0,ClickType.PICKUP,player);var result=crafting.getCarried().copy();crafting.setCarried(ItemStack.EMPTY);
  check(ItemStack.matches(result,e.output())&&ProgressionAudit.vis(wand)<before,"arcane click consumes vis and gives "+e.key());
  for(int i=0;i<9;i++)check(bench.inventory.getItem(i).isEmpty(),"arcane ingredients consumed "+e.key()+" slot "+i);
  wand=bench.inventory.removeItemNoUpdate(9);crafting.removed(player);player.containerMenu=player.inventoryMenu;return result;
 }
 static ItemStack vanilla(Item item){
  for(var holder:level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)){
   var r=holder.value();if(!r.getResultItem(level.registryAccess()).is(item))continue;
   int w=r instanceof ShapedRecipe s?s.getWidth():3,h=r instanceof ShapedRecipe s?s.getHeight():3;var slots=new ArrayList<ItemStack>();
   for(var ing:r.getIngredients())slots.add(ing.isEmpty()?ItemStack.EMPTY:ing.getItems()[0].copyWithCount(1));while(slots.size()<w*h)slots.add(ItemStack.EMPTY);
   var input=CraftingInput.of(w,h,slots);if(r.matches(input,level)){var output=r.assemble(input,level.registryAccess());check(output.is(item),"ordinary recipe assembles "+item);return output;}
  }throw new IllegalStateException("TC4_PROGRESSION FAIL no craft for "+item);
 }
 static void place(BlockPos pos,ItemStack stack){
  if(level.getBlockState(pos.below()).isAir())level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);level.removeBlock(pos,false);player.moveTo(pos.getX()+2,pos.getY(),pos.getZ()+2);player.setItemInHand(InteractionHand.MAIN_HAND,stack);
  var result=stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0,.5,0),Direction.UP,pos.below(),false)));
  check(result.consumesAction()&&!level.getBlockState(pos).isAir(),"crafted block places through its item at "+pos);
 }
 static void put(BlockPos pos,ItemStack item){player.setItemInHand(InteractionHand.MAIN_HAND,item);level.getBlockState(pos).useItemOn(item,level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));check(item.isEmpty(),"pedestal interaction consumes its one input");}
 static void click(){player.setItemInHand(InteractionHand.MAIN_HAND,wand);level.getBlockState(ALTAR).useItemOn(wand,level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(ALTAR),Direction.UP,ALTAR,false));}
 static void begin(ServerLevel l){
  level=l;player=ProgressionAudit.novice;wand=ProgressionAudit.heldWand;table=(ResearchTableEntity)l.getBlockEntity(ProgressionAudit.BASE.offset(-2,0,0));menu=new ResearchTableMenu(11,player.getInventory(),ContainerLevelAccess.create(l,table.getBlockPos()),table.inventory,table.bonus.bonusAspects);
  research("CAP_gold");research("ROD_greatwood");research("INFUSION");research("CAP_thaumium");
  var cap=arcane("WandCapGold");var cap2=arcane("WandCapGold");var rod=arcane("WandRodGreatwood");
  var grid=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));grid.set(2,cap);grid.set(6,cap2);grid.set(4,rod);
  var recipe=WandParts.recipe(grid,ResearchProgress.completed(player.getPersistentData()));check(recipe!=null,"crafted gold caps and greatwood rod match the wand recipe");wand=arcane(recipe,grid);check(cap.isEmpty()&&cap2.isEmpty()&&rod.isEmpty(),"upgraded wand consumes the actual previously crafted caps and rod");check(WandData.capacity(wand)==5000&&ProgressionAudit.vis(wand)==0,"crafted upgraded wand holds 50 vis and starts empty");
  var matrixItem=arcane("InfusionMatrix");var pedestals=new ArrayList<ItemStack>();for(int i=0;i<2;i++){var result=arcane("ArcanePedestal");pedestals.add(result.copyWithCount(1));pedestals.add(result.copyWithCount(1));}
  var stone=arcane("ArcaneStone1");check(stone.getCount()>=4,"arcane stone provides four upper corners");
  var bricks=vanilla(TC4Modern.ARCANE_STONE_BRICKS.get().asItem());check(bricks.getCount()>=4,"ordinary stone brick recipe provides four lower corners");
  for(int x=-7;x<=7;x++)for(int z=-7;z<=7;z++)for(int y=-3;y<=4;y++)level.setBlock(ALTAR.offset(x,y,z),(y==-3?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
  place(ALTAR.below(2),pedestals.removeFirst());for(int x:new int[]{-1,1})for(int z:new int[]{-1,1}){place(ALTAR.offset(x,-2,z),bricks.copyWithCount(1));place(ALTAR.offset(x,-1,z),stone.copyWithCount(1));}
  place(ALTAR,matrixItem); // remove the temporary placement support to match the original blueprint
  level.removeBlock(ALTAR.below(),false);charge();player.moveTo(ALTAR.getX()+2,ALTAR.getY()-1,ALTAR.getZ()+2);int before=ProgressionAudit.vis(wand);click();matrix=(InfusionContent.MatrixEntity)level.getBlockEntity(ALTAR);
  check(matrix.logic.active&&matrix.logic.validLocation(matrix)&&ProgressionAudit.vis(wand)==before-15000,"same novice activates crafted altar with exactly 25 of all six primals");
  for(var pos:COMPONENTS)place(pos,pedestals.removeFirst());
  // Actual skull blocks are supplied stabilising materials, as the ordinary gathering inputs are supplied above.
  for(int x=-6;x<=6;x++)for(int z=-6;z<=6;z++){var pos=ALTAR.offset(x,-2,z);if((x!=0||z!=0)&&level.getBlockState(pos).isAir())level.setBlock(pos,Blocks.SKELETON_SKULL.defaultBlockState(),3);}
  put(ALTAR.below(2),arcane("WandCapThaumiumInert"));
  place(SMELTER,vanilla(Items.FURNACE));var smelter=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)level.getBlockEntity(SMELTER);smelter.setItem(0,new ItemStack(TC4Modern.BALANCED_SHARD.get(),3));smelter.setItem(1,new ItemStack(Items.COAL,8));
  place(FURNACE,arcane("AlchemyFurnace"));for(int i=1;i<=5;i++)place(FURNACE.above(i),arcane("Alembic"));
  // Alembic placement supports already have the preceding device, so place() must not overwrite it.
  place(ALTAR.offset(-1,-2,-4),arcane("WardedJar"));place(ALTAR.offset(1,-2,-4),arcane("WardedJar"));
  var furnace=(AlchemicalFurnaceEntity)level.getBlockEntity(FURNACE);check(furnace!=null,"crafted distillation furnace survives setup");furnace.setItem(0,new ItemStack(Items.COAL,8));furnace.setItem(1,new ItemStack(Items.COAL,8));phase=1;start=ProgressionAudit.ticks;
 }
 static int transfer(Aspect wanted,BlockPos jar){
  int n=0;for(int i=1;i<=5;i++)while(level.getBlockEntity(FURNACE.above(i)) instanceof AlembicEntity a&&a.state.aspect==wanted&&a.state.amount>=8){
   int before=a.state.amount;var empty=vanilla(EssentiaDevices.PHIAL.get()).copyWithCount(1);ProgressionAudit.use(player,empty,FURNACE.above(i));check(a.state.amount==before-8,"empty phial extracts eight distilled "+wanted.getTag());
   ItemStack filled=ItemStack.EMPTY;for(int slot=0;slot<player.getInventory().getContainerSize();slot++){var candidate=player.getInventory().getItem(slot);var contents=EssentiaItems.getAspects(candidate);if(candidate.is(EssentiaDevices.ESSENTIA_PHIAL.get())&&contents!=null&&contents.getAmount(wanted)==8){filled=candidate;break;}}
   check(!filled.isEmpty(),"distillation yields a filled phial");var target=(WardedJarEntity)level.getBlockEntity(jar);int had=target.state.amount;ProgressionAudit.use(player,filled,jar);check(target.state.amount==had+8,"phial fills jar with distilled "+wanted.getTag());n+=8;
  }return n;
 }
 static boolean tick(){
  now=ProgressionAudit.ticks;var furnace=(AlchemicalFurnaceEntity)level.getBlockEntity(FURNACE);
  if(phase==1&&furnace.getItem(0).isEmpty()&&furnace.state.vis==0){transfer(Aspect.ENERGY,ALTAR.offset(-1,-2,-4));check(((WardedJarEntity)level.getBlockEntity(ALTAR.offset(-1,-2,-4))).state.amount>=12,"coal distillation supplies cap potentia");furnace.setItem(0,new ItemStack(PechTrades.mod("wisp_essence").getItem(),4));phase=2;}
  if(phase==2&&furnace.getItem(0).isEmpty()&&furnace.state.vis==0&&((net.minecraft.world.level.block.entity.FurnaceBlockEntity)level.getBlockEntity(SMELTER)).getItem(2).getCount()>=3){transfer(Aspect.AURA,ALTAR.offset(1,-2,-4));check(((WardedJarEntity)level.getBlockEntity(ALTAR.offset(1,-2,-4))).state.amount>=6,"wisp drops distil into cap auram");var smelter=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)level.getBlockEntity(SMELTER);check(smelter.getItem(2).is(InfusionContent.SALIS.get()),"ordinary furnace smelts supplied balanced shards into salis mundus");for(var pos:COMPONENTS)put(pos,smelter.removeItem(2,1));player.moveTo(ALTAR.getX()+2,ALTAR.getY()-1,ALTAR.getZ()+2);click();check(matrix.logic.crafting&&matrix.logic.recipeEssentia.visSize()==18,"same researcher starts the 18-essentia infusion");phase=3;}
  if(phase==3&&!matrix.logic.crafting){var center=(InfusionContent.PedestalEntity)level.getBlockEntity(ALTAR.below(2));check(center.item().is(WandParts.THAUMIUM_CAP.get()),"natural ticks finish the novice's first infusion");for(var pos:COMPONENTS)check(((InfusionContent.PedestalEntity)level.getBlockEntity(pos)).item().isEmpty(),"infusion consumes pedestal component");check(((WardedJarEntity)level.getBlockEntity(ALTAR.offset(-1,-2,-4))).state.amount==4&&((WardedJarEntity)level.getBlockEntity(ALTAR.offset(1,-2,-4))).state.amount==2,"infusion consumes exactly 12 potentia and 6 auram from actually distilled jars");System.out.println("TC4_PROGRESSION INFUSION ALL PASS same novice, real research/crafting/node charging/distillation/phials/altar/infusion; supplied gathering inputs");return true;}
  if(now-start>3000)throw new IllegalStateException("TC4_PROGRESSION FAIL distillation/infusion timeout phase "+phase+" furnace "+furnace.state.vis+" input "+furnace.getItem(0));return false;
 }
}
