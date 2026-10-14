package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;

/** Same novice's late unlocks. Supplied zombie brains and spawned cultists stand in for gathering/exploration. */
final class ProgressionOuterAudit {
 static ServerLevel level;static ServerPlayer player;static int phase,brains,foodTicks,kills,since;static ItemStack food=ItemStack.EMPTY;static boolean minor,ritesRead;static final List<ItemStack> seeds=new ArrayList<>();
 static final BlockPos ENCOUNTER=ProgressionAudit.BASE.offset(-8,0,-12);
 static void check(boolean b,String text){ProgressionAudit.check(b,text);}
 static int lastingWarp(){var s=PlayerWarp.load(player);return s.get(WarpState.Kind.PERMANENT)+s.get(WarpState.Kind.STICKY);}
 static void begin(){
  level=ProgressionInfusionAudit.level;player=ProgressionInfusionAudit.player;level.getServer().tickRateManager().setTickRate(100f);
  check(!ResearchProgress.completed(player.getPersistentData()).contains("ELDRITCHMINOR")&&!ResearchProgress.completed(player.getPersistentData()).contains("ELDRITCHMAJOR"),"the same novice has not been granted late warp unlocks");
  player.moveTo(ENCOUNTER.getX()+.5,ENCOUNTER.getY(),ENCOUNTER.getZ()+.5);phase=1;since=ProgressionAudit.ticks;
 }
 static void eat(){
  if(food.isEmpty()){
   if(brains>=1200)check(false,"bounded supplied zombie brains for actual food-based warp");player.getFoodData().setFoodLevel(0);food=new ItemStack(ZombieBrainContent.ITEM.get());player.setItemInHand(InteractionHand.MAIN_HAND,food);food.getItem().use(level,player,InteractionHand.MAIN_HAND);if(!player.isUsingItem())check(false,"hungry novice starts eating a zombie brain");foodTicks=0;
  }
  if(++foodTicks<food.getUseDuration(player))return;
  var remaining=food.getItem().finishUsingItem(food,level,player);player.stopUsingItem();if(!remaining.isEmpty())check(false,"finished eating consumes one supplied brain");food=ItemStack.EMPTY;brains++;if(brains%50==0)System.out.println("TC4_PROGRESSION consumed "+brains+" actual brains; lasting warp "+lastingWarp());
 }
 static ItemStack takeInventory(Item item){for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i).is(item))return player.getInventory().removeItem(i,1);return ItemStack.EMPTY;}
 static void preparePortalWand(){
  var earned=ProgressionLateInfusionAudit.earned;var grid=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));grid.set(2,earned.get(0));grid.set(6,earned.get(1));grid.set(4,earned.get(2));
  var recipe=WandParts.recipe(grid,ResearchProgress.completed(player.getPersistentData()));check(recipe!=null,"earned infused caps and rod assemble the portal wand");ProgressionInfusionAudit.wand=ProgressionInfusionAudit.arcane(recipe,grid);check(earned.get(0).isEmpty()&&earned.get(1).isEmpty()&&earned.get(2).isEmpty(),"portal wand consumes the actual infused parts");check(WandData.capacity(ProgressionInfusionAudit.wand)==10000,"silverwood wand holds 100 vis");
  var milk=new ItemStack(Items.MILK_BUCKET);milk.getItem().finishUsingItem(milk,level,player);ProgressionInfusionAudit.charge();
 }
 static void enterOuter(){
  var earned=ProgressionLateInfusionAudit.earned;var wand=ProgressionInfusionAudit.wand;var at=ProgressionAudit.BASE.offset(12,0,-8);for(int y=0;y<=2;y++)level.removeBlock(at.above(y),false);level.setBlockAndUpdate(at,CrimsonContent.ALTAR.get().defaultBlockState());level.setBlockAndUpdate(at.above(2),CrimsonContent.OBELISK.get().defaultBlockState());
  check(AuraNodeFeature.createRandomNodeAt(level,at.above(),net.minecraft.util.RandomSource.create(8901),false,false,false),"exploration fixture creates an ordinary obelisk node");((AuraNodeEntity)level.getBlockEntity(at.above())).nodeType=thaumcraft.api.nodes.NodeType.DARK;
  var altar=(CrimsonAltarEntity)level.getBlockEntity(at);for(int i=3;i<7;i++){var eye=earned.get(i);player.setItemInHand(InteractionHand.MAIN_HAND,eye);level.getBlockState(at).useItemOn(eye,level,player,InteractionHand.MAIN_HAND,new BlockHitResult(at.getCenter(),Direction.UP,at,false));check(eye.isEmpty()&&altar.getEyes()==i-2,"insert and consume earned infused eye "+(i-2));}
  var expected=wand.copy();var cost=new thaumcraft.api.aspects.AspectList();for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())cost.add(a,100);check(WandData.consumeCrafting(expected,cost,true,player),"earned fully charged wand can afford portal cost");check(ProgressionAudit.use(player,wand,at)&&altar.isOpen()&&level.getBlockEntity(at.above()) instanceof OuterPortal.Portal,"same novice opens the portal with four earned eyes and the earned wand");for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())check(WandData.get(wand,a)==1000&&WandData.get(wand,a)==WandData.get(expected,a),"opening consumes exactly 90 of 100 vis with thaumium caps for "+a.getTag());
  // Fake players are absent from entity collision queries; exercise the unchanged production transfer adapter.
  OuterPortal.transfer(player,at.above());check(player.level().dimension()==OuterDimension.KEY&&ResearchProgress.completed(player.getPersistentData()).contains("ENTEROUTER"),"the same novice enters the real Outer dimension and earns ENTEROUTER");scanOuterLibrary(at);OuterPortal.transfer(player,new BlockPos(at.getX(),53,at.getZ()));check(player.level().dimension()==net.minecraft.world.level.Level.OVERWORLD,"the same novice returns through the real portal transfer");
  ProgressionInfusionAudit.research("OUTERREV");
  System.out.println("TC4_PROGRESSION OUTER ALL PASS same novice, natural warp gates, actual cultist loot/research, six late infusions, distilled essentia, earned 100-vis wand, four consumed eyes, portal entry, actual generated-stone scan/OUTERREV and return; supplied gathering/structure fixtures");
 }
 static void scanOuterLibrary(BlockPos entrance){
  var outer=player.serverLevel();var data=OuterMazeData.get(outer);BlockPos library=null;int cx=entrance.getX()>>4,cz=entrance.getZ()>>4;
  search:for(int radius=0;radius<=30;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;var cell=data.cell(cx+dx,cz+dz);if(cell!=null&&cell.feature==8){outer.getChunk(cx+dx,cz+dz);library=new BlockPos((cx+dx)*16+8,53,(cz+dz)*16+8);break search;}}
  BlockPos beside=library==null?null:library.north(2);
  // Original labyrinth generation does not guarantee a library; corridor decoration also provides valid clue stones.
  if(library==null){int loaded=0;find:for(int radius=0;radius<=30;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
   if(Math.max(Math.abs(dx),Math.abs(dz))!=radius||data.cell(cx+dx,cz+dz)==null)continue;if(++loaded>64)break find;outer.getChunk(cx+dx,cz+dz);
   for(int x=0;x<16;x++)for(int z=0;z<16;z++)for(int y=51;y<=62;y++){var pos=new BlockPos((cx+dx)*16+x,y,(cz+dz)*16+z);var state=outer.getBlockState(pos);if(!state.is(OuterContent.GLYPHED.get())&&!state.is(OuterDevices.TRAP.get()))continue;for(var direction:Direction.values())if(outer.isEmptyBlock(pos.relative(direction))){library=pos;beside=pos.relative(direction);break find;}}
  }}
  check(library!=null,"the opened labyrinth naturally contains a generated clue stone");
  var eye=Vec3.atCenterOf(beside);player.moveTo(eye.x,eye.y-player.getEyeHeight(),eye.z);player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,library.getCenter());var target=ScanTarget.look(player);check(target!=null&&(target.knowledgeKey().equals("tc4modern:glyphed_stone")||target.knowledgeKey().equals("tc4modern:runed_stone")),"the novice actually targets the generated Outer clue stone");
  var scope=ProgressionInfusionAudit.vanilla(TC4Modern.THAUMOMETER.get());player.setItemInHand(InteractionHand.MAIN_HAND,scope);scope.getItem().use(outer,player,InteractionHand.MAIN_HAND);check(player.isUsingItem(),"actual thaumometer begins the Outer scan");scope.getItem().onUseTick(outer,player,scope,scope.getUseDuration(player)-ThaumometerItem.SCAN_TICKS);check(ResearchProgress.completed(player.getPersistentData()).contains("@OUTERREV"),"the generated stone scan reveals OUTERREV without granting a clue");
 }
 static boolean tick(){
  int tick=ProgressionAudit.ticks;player.tickCount++;player.baseTick();
  if(tick%2000==0)System.out.println("TC4_PROGRESSION late status phase="+phase+" playerTicks="+player.tickCount+" lasting="+lastingWarp()+" counter="+PlayerWarp.load(player).counter()+" events="+WarpRuntime.events);
  // A fake player is not in the server's player tick list: dispatch the unchanged 2000-tick event gate explicitly.
  WarpRuntime.tick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(player));
  var completed=ResearchProgress.completed(player.getPersistentData());
  if(phase==1){
   if(lastingWarp()<=25)eat();
   else if(!minor){if(completed.contains("ELDRITCHMAJOR"))check(false,"temporary food warp does not substitute for the major lasting-warp threshold");if(completed.contains("ELDRITCHMINOR")){minor=true;System.out.println("TC4_PROGRESSION PASS ordinary gated warp event discovers ELDRITCHMINOR at lasting warp "+lastingWarp());}else if(lastingWarp()<50)eat();}
   else if(lastingWarp()<=50)eat();
   else if(completed.contains("ELDRITCHMAJOR")){check(minor,"minor warp discovery precedes major");System.out.println("TC4_PROGRESSION PASS ordinary gated warp event discovers ELDRITCHMAJOR after "+brains+" consumed brains; lasting warp "+lastingWarp());phase=2;since=tick;}
  }else if(phase==2&&tick%10==0){
   check(kills++<512,"bounded actual cultist kills for Crimson Rites drop");var cultist=CrimsonContent.KNIGHT.get().create(level);cultist.moveTo(ENCOUNTER.getX()+3.5,ENCOUNTER.getY(),ENCOUNTER.getZ()+.5);cultist.setNoAi(true);level.addFreshEntity(cultist);cultist.invulnerableTime=0;cultist.hurt(level.damageSources().playerAttack(player),1000);
   for(var drop:level.getEntitiesOfClass(ItemEntity.class,new AABB(ENCOUNTER).inflate(6))){var stack=drop.getItem();
    if(stack.is(PechTrades.mod("void_seed").getItem())){while(!stack.isEmpty())seeds.add(stack.split(1));drop.discard();}
    else if(stack.is(BasicsContent.CRIMSON_RITES.get())&&!ritesRead){var rites=stack.copy();drop.discard();player.setItemInHand(InteractionHand.MAIN_HAND,rites);rites.getItem().use(level,player,InteractionHand.MAIN_HAND);check(ResearchProgress.completed(player.getPersistentData()).contains("CRIMSON"),"reading a real player-kill cultist drop discovers CRIMSON after "+kills+" kills");ritesRead=true;}
   }
   if(ritesRead&&seeds.size()>=4){ProgressionInfusionAudit.research("OCULUS");ProgressionInfusionAudit.research("ROD_silverwood");System.out.println("TC4_PROGRESSION LATE_UNLOCK ALL PASS same novice earns lasting food warp, gated minor/major events, cultist book and four void seeds, researched OCULUS/silverwood; supplied encounter/food inputs");ProgressionLateInfusionAudit.begin();phase=3;since=tick;}

  }
  if(phase==3&&ProgressionLateInfusionAudit.tick()){preparePortalWand();phase=4;since=tick;}
  if(phase==4){var cost=new thaumcraft.api.aspects.AspectList();for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())cost.add(a,100);if(WandData.consumeCrafting(ProgressionInfusionAudit.wand,cost,false,player)){enterOuter();phase=5;return true;}}
  if(tick-since>60000)throw new IllegalStateException("TC4_PROGRESSION FAIL late unlock timeout phase "+phase+" lastingWarp="+lastingWarp()+" brains="+brains+" playerTicks="+player.tickCount);return false;
 }
}
