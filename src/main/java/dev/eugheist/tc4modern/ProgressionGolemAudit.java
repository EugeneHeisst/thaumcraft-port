package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.*;
import thaumcraft.api.aspects.*;

/** The same player's learned golem branch, actual crucible outputs and autonomous gathering. Gathering inputs supplied. */
final class ProgressionGolemAudit {
 static final BlockPos POT=ProgressionAudit.BASE.offset(0,0,10),HOME=ProgressionAudit.BASE.offset(-8,0,10);
 static ServerLevel level;static ServerPlayer player;static int phase,since;static ItemStack blank,placer,core,bell;static StrawGolem golem;static int maxCarried;
 static void check(boolean b,String s){ProgressionAudit.check(b,s);}
 static void toss(ItemStack stack){var e=new ItemEntity(level,POT.getX()+.5,POT.getY()+.6,POT.getZ()+.5,stack);e.setDeltaMovement(Vec3.ZERO);e.setThrower(player);level.addFreshEntity(e);}
 static void begin(){
  level=ProgressionInfusionAudit.level;player=ProgressionInfusionAudit.player;ProgressionInfusionAudit.research("GOLEMSTRAW");
  check(ResearchProgress.completed(player.getPersistentData()).containsAll(Set.of("GOLEMSTRAW","COREGATHER","GOLEMBELL")),"learning straw golems legitimately unlocks gather core and bell siblings");
  var blankRecipe=ArcaneRecipes.entries().stream().filter(r->r.key().equals("CoreBlank")).findFirst().orElseThrow();var inputs=new ArrayList<ItemStack>();
  for(var ingredient:blankRecipe.ingredients()){var item=ingredient.isEmpty()?ItemStack.EMPTY:ingredient.getItems()[0].copyWithCount(1);if(item.is(TC4Modern.NITOR.get()))item=ProgressionAudit.earnedNitor;inputs.add(item);}
  check(ProgressionAudit.earnedNitor.getCount()==1,"the first crucible-made nitor is available for the blank core");blank=ProgressionInfusionAudit.arcane(blankRecipe,inputs);check(ProgressionAudit.earnedNitor.isEmpty(),"blank core consumes that actual first nitor");bell=ProgressionInfusionAudit.arcane("GolemBell");
  for(int x=-5;x<=7;x++)for(int z=-5;z<=5;z++)for(int y=-1;y<=3;y++)level.setBlock(HOME.offset(x,y,z),(y==-1?Blocks.STONE:Blocks.AIR).defaultBlockState(),3);
  level.getEntitiesOfClass(ItemEntity.class,new AABB(HOME).inflate(12)).forEach(ItemEntity::discard);
  level.setBlock(POT,Blocks.CAULDRON.defaultBlockState(),3);ProgressionAudit.use(player,ProgressionInfusionAudit.wand,POT);check(level.getBlockEntity(POT) instanceof CrucibleEntity,"same novice wand creates the golem crucible");
  level.setBlock(POT.below(),Blocks.LAVA.defaultBlockState(),3);var bucket=new ItemStack(Items.WATER_BUCKET);player.setItemInHand(InteractionHand.MAIN_HAND,bucket);level.getBlockState(POT).useItemOn(bucket,level,player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(POT),Direction.UP,POT,false));
  phase=1;since=ProgressionAudit.ticks;
 }
 /** Select ordinary supplied ingredients by their real computed tags, then let the crucible melt actual item entities. */
 static void feed(String key){
  var recipe=CrucibleRecipes.byKey().get(key);var tile=(CrucibleEntity)level.getBlockEntity(POT);var needs=new HashMap<Aspect,Integer>();for(var a:recipe.aspects.getAspects())needs.put(a,Math.max(0,recipe.aspects.getAmount(a)-tile.crucible.aspects.getAmount(a)));
  var candidates=List.of(Items.ROTTEN_FLESH,Items.SOUL_SAND,Items.FEATHER,Items.PISTON,Items.OAK_TRAPDOOR,Items.ENDER_PEARL,Items.SUGAR,Items.REDSTONE,Items.GOLD_INGOT,Items.DIAMOND,Items.EMERALD,Items.DIRT,Items.LEATHER,Items.WHEAT);
  var inputs=new LinkedHashMap<Item,Integer>();
  for(int step=0;needs.values().stream().anyMatch(n->n>0)&&step<64;step++){
   Item chosen=null;double best=0;Map<String,Integer> tags=null;
   for(var item:candidates){var values=RecipeAspects.get(new ItemStack(item),level);int hit=0,total=values.values().stream().mapToInt(Integer::intValue).sum();for(var a:needs.keySet())hit+=Math.min(needs.get(a),values.getOrDefault(a.getTag(),0));double score=hit/(double)Math.max(1,total);if(score>best){chosen=item;tags=values;best=score;}}
   check(chosen!=null,"ordinary ingredients cover "+key+" requirements "+needs);inputs.merge(chosen,1,Integer::sum);for(var a:needs.keySet())needs.put(a,Math.max(0,needs.get(a)-tags.getOrDefault(a.getTag(),0)));
  }
  check(needs.values().stream().allMatch(n->n==0),"bounded ingredient plan covers "+key);inputs.forEach((item,n)->toss(new ItemStack(item,n)));System.out.println("TC4_PROGRESSION GOLEM melted inputs "+key+" "+inputs);since=ProgressionAudit.ticks;
 }
 static boolean ready(String key){var aspects=((CrucibleEntity)level.getBlockEntity(POT)).crucible.aspects;var recipe=CrucibleRecipes.byKey().get(key);return Arrays.stream(recipe.aspects.getAspects()).allMatch(a->aspects.getAmount(a)>=recipe.aspects.getAmount(a));}
 static ItemStack output(Item item){var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(POT).inflate(3));for(var e:drops)if(e.getItem().is(item)){var stack=e.getItem().copy();e.discard();return stack;}return ItemStack.EMPTY;}
 static boolean tick(){
  int tick=ProgressionAudit.ticks;var pot=(CrucibleEntity)level.getBlockEntity(POT);
  if(phase==1&&pot.crucible.boiling()){feed("GolemStraw");phase=2;}
  else if(phase==2&&tick-since>=40&&ready("GolemStraw")){toss(new ItemStack(Items.HAY_BLOCK));phase=3;since=tick;}
  else if(phase==3&&tick-since>=40){placer=output(Golemancy.STRAW.get());check(placer.getCount()==1,"researcher-thrown hay becomes one straw golem in the crucible");feed("CoreGather");phase=4;}
  else if(phase==4&&tick-since>=40&&ready("CoreGather")){check(blank.is(Golemancy.BLANK.get()),"gather recipe uses the previously crafted blank core");toss(blank.copyWithCount(1));blank.shrink(1);phase=5;since=tick;}
  else if(phase==5&&tick-since>=40){core=output(Golemancy.GATHER.get());check(core.getCount()==1,"actual crucible transforms the crafted blank into a gather core");
   ProgressionInfusionAudit.place(HOME,ProgressionInfusionAudit.vanilla(Items.CHEST));player.moveTo(HOME.getX()+3,HOME.getY(),HOME.getZ()+2);player.setItemInHand(InteractionHand.MAIN_HAND,placer);
   var use=new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(HOME),Direction.EAST,HOME,false));check(placer.getItem().onItemUseFirst(placer,use).consumesAction()&&placer.isEmpty(),"crafted golem places on chest and consumes its item");
   golem=level.getEntitiesOfClass(StrawGolem.class,new AABB(HOME).inflate(3)).stream().filter(g->player.getUUID().equals(g.owner)).findFirst().orElseThrow();player.setItemInHand(InteractionHand.MAIN_HAND,core);check(golem.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction()&&core.isEmpty()&&golem.core()==2,"crafted gather core installs in the crafted straw golem");
   var target=new ItemEntity(level,HOME.getX()+4.5,HOME.getY(),HOME.getZ()+.5,new ItemStack(Items.COBBLESTONE,4));target.setNoPickUpDelay();level.addFreshEntity(target);player.moveTo(HOME.getX()-3,HOME.getY(),HOME.getZ()-3);phase=6;since=tick;
  }else if(phase==6){maxCarried=Math.max(maxCarried,golem.carried().getCount());var chest=(ChestBlockEntity)level.getBlockEntity(HOME);int count=0;for(int i=0;i<chest.getContainerSize();i++)if(chest.getItem(i).is(Items.COBBLESTONE))count+=chest.getItem(i).getCount();
   if(count==4){check(maxCarried==1&&golem.carried().isEmpty(),"natural golem AI gathers four supplied cobblestone into chest one at a time");player.setItemInHand(InteractionHand.MAIN_HAND,bell);check(((Golemancy.Bell)bell.getItem()).onLeftClickEntity(bell,player,golem)&&!golem.isAlive(),"previously crafted bell retrieves the working golem");var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(golem.blockPosition()).inflate(2));check(drops.stream().anyMatch(e->e.getItem().is(Golemancy.STRAW.get())&&NoteData.tag(e.getItem()).getByte("core")==2),"retrieved golem item preserves its crafted gather core");System.out.println("TC4_PROGRESSION GOLEM ALL PASS same novice researches, crafts, creates and operates a gather golem; supplied gathering inputs");return true;}
  }
  if(tick-since>1500)throw new IllegalStateException("TC4_PROGRESSION FAIL golem progression timeout phase "+phase);return false;
 }
}
