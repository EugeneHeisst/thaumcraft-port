package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.*;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import thaumcraft.api.aspects.Aspect;

/** Continues the same altar with supplied gathering inputs, but genuinely distilled essentia and consumed outputs. */
final class ProgressionLateInfusionAudit {
 static final List<String> JOBS=List.of("WandCapThaumium","WandRodSilverwood","EldritchEye","EldritchEye","EldritchEye","EldritchEye");
 static final Map<Aspect,BlockPos> jars=new LinkedHashMap<>();
 static final List<BlockPos> peds=new ArrayList<>(ProgressionInfusionAudit.COMPONENTS);
 static final List<ItemStack> earned=new ArrayList<>();
 static final Map<Aspect,Integer> before=new LinkedHashMap<>();
 static int job,phase,started;static InfusionItemRecipes.Recipe recipe;
 static void check(boolean b,String s){ProgressionAudit.check(b,s);}
 static WardedJarEntity jar(Aspect a){return (WardedJarEntity)ProgressionInfusionAudit.level.getBlockEntity(jars.get(a));}
 static void emptyHand(BlockPos pos,boolean shift){var p=ProgressionInfusionAudit.player;var l=ProgressionInfusionAudit.level;p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(shift);l.getBlockState(pos).useWithoutItem(l,p,new BlockHitResult(pos.getCenter(),Direction.UP,pos,false));p.setShiftKeyDown(false);}
 static ItemStack collect(Item item){emptyHand(ProgressionInfusionAudit.ALTAR.below(2),false);var out=ProgressionOuterAudit.takeInventory(item);check(!out.isEmpty(),"actual pedestal retrieval yields "+item);return out;}
 static void begin(){
  var at=ProgressionInfusionAudit.ALTAR;earned.add(collect(WandParts.THAUMIUM_CAP.get()));
  for(var offset:List.of(new BlockPos(0,-2,-3),new BlockPos(-3,-2,-3),new BlockPos(3,-2,-3),new BlockPos(-3,-2,3)))peds.add(at.offset(offset));
  for(int i=3;i<peds.size();i+=2){var pair=ProgressionInfusionAudit.arcane("ArcanePedestal");for(int n=i;n<Math.min(i+2,peds.size());n++)ProgressionInfusionAudit.place(peds.get(n),pair.split(1));}
  jars.put(Aspect.ENERGY,at.offset(-1,-2,-4));jars.put(Aspect.AURA,at.offset(1,-2,-4));
  var needed=new LinkedHashSet<Aspect>();for(var key:JOBS)needed.addAll(Arrays.asList(InfusionItemRecipes.all().stream().filter(r->r.key().equals(key)).findFirst().orElseThrow().aspects().getAspects()));
  int slot=0;for(var a:needed)if(!jars.containsKey(a)){var pos=at.offset(-5+(slot%6)*2,-2,slot++<6?-5:5);ProgressionInfusionAudit.place(pos,ProgressionInfusionAudit.arcane("WardedJar"));jars.put(a,pos);}
  var smelter=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)ProgressionInfusionAudit.level.getBlockEntity(ProgressionInfusionAudit.SMELTER);smelter.setItem(0,new ItemStack(TC4Modern.BALANCED_SHARD.get(),3));smelter.setItem(1,new ItemStack(Items.COAL,8));next();
 }
 static void next(){recipe=InfusionItemRecipes.all().stream().filter(r->r.key().equals(JOBS.get(job))).findFirst().orElseThrow();phase=0;started=ProgressionAudit.ticks;}
 static void transfer(){
  var l=ProgressionInfusionAudit.level;var p=ProgressionInfusionAudit.player;
  for(int i=1;i<=5;i++){var at=ProgressionInfusionAudit.FURNACE.above(i);var a=(AlembicEntity)l.getBlockEntity(at);while(a.state.amount>=8&&jars.containsKey(a.state.aspect)&&jar(a.state.aspect).state.amount<=56){var wanted=a.state.aspect;var empty=ProgressionInfusionAudit.vanilla(EssentiaDevices.PHIAL.get()).copyWithCount(1);check(ProgressionAudit.use(p,empty,at),"late distillation extracts an actual phial");ItemStack filled=ItemStack.EMPTY;for(int s=0;s<p.getInventory().getContainerSize();s++){var candidate=p.getInventory().getItem(s);var tags=EssentiaItems.getAspects(candidate);if(candidate.is(EssentiaDevices.ESSENTIA_PHIAL.get())&&tags!=null&&tags.getAmount(wanted)==8){filled=candidate;break;}}check(!filled.isEmpty(),"late distilled phial exists");int had=jar(wanted).state.amount;ProgressionAudit.use(p,filled,jars.get(wanted));check(jar(wanted).state.amount==had+8,"late jar receives eight "+wanted.getTag());}}
 }
 static void supply(Aspect wanted,int missing){
  Item best=null;int count=0;double score=-1;var l=ProgressionInfusionAudit.level;
  var candidates=new ArrayList<Item>(List.of(Items.COAL,Items.REDSTONE,Items.ENDER_PEARL,Items.GLASS_BOTTLE,Items.BOWL,Items.BUCKET,Items.OBSIDIAN,Items.GLASS,Items.BONE,Items.ROTTEN_FLESH,Items.FEATHER,Items.SAND,Items.ICE,Items.CLAY_BALL,Items.GLOWSTONE_DUST,Items.NETHERRACK,Items.SOUL_SAND,PechTrades.mod("wisp_essence").getItem()));for(var shard:TC4Modern.SHARDS)candidates.add(shard.get());
  for(var item:candidates){var tags=RecipeAspects.get(new ItemStack(item),l);int amount=tags.getOrDefault(wanted.getTag(),0);if(amount<=0||tags.size()>5)continue;int n=(missing+amount-1)/amount;if(n>Math.min(64,new ItemStack(item).getMaxStackSize())||tags.values().stream().anyMatch(v->v*n>64))continue;double value=(double)amount/tags.values().stream().mapToInt(Integer::intValue).sum();if(value>score){score=value;best=item;count=n;}}
  check(best!=null,"ordinary supplied gathering item distils "+wanted.getTag());var f=(AlchemicalFurnaceEntity)l.getBlockEntity(ProgressionInfusionAudit.FURNACE);f.setItem(0,new ItemStack(best,count));check(f.getItem(0).getCount()==count,"distillation input respects actual container/stack limits");f.setItem(1,new ItemStack(Items.COAL,64));System.out.println("TC4_PROGRESSION distilling "+count+" "+best+" for "+wanted.getTag());
 }
 static boolean tick(){
  if(ProgressionAudit.ticks-started>12000)throw new IllegalStateException("TC4_PROGRESSION FAIL late infusion timeout "+recipe.key()+" phase "+phase);
  var l=ProgressionInfusionAudit.level;var at=ProgressionInfusionAudit.ALTAR;var f=(AlchemicalFurnaceEntity)l.getBlockEntity(ProgressionInfusionAudit.FURNACE);transfer();
  if(phase==0&&f.getItem(0).isEmpty()&&f.state.vis==0){
   for(int i=1;i<=5;i++)emptyHand(ProgressionInfusionAudit.FURNACE.above(i),true);
   for(var a:recipe.aspects().getAspects())if(jar(a).state.amount<recipe.aspects().getAmount(a)){int missing=recipe.aspects().getAmount(a)-jar(a).state.amount;supply(a,((missing+7)/8)*8);return false;}
   var components=recipe.componentStacks();ItemStack center;
   if(job==0){var smelter=(net.minecraft.world.level.block.entity.FurnaceBlockEntity)l.getBlockEntity(ProgressionInfusionAudit.SMELTER);if(smelter.getItem(2).getCount()<3)return false;components.clear();for(int i=0;i<3;i++)components.add(smelter.removeItem(2,1));center=ProgressionInfusionAudit.arcane("WandCapThaumiumInert");}
   else center=job==1?new ItemStack(recipe.central().get()):ProgressionInfusionAudit.vanilla(Items.ENDER_EYE).copyWithCount(1);
   if(job>=2){var seed=ProgressionOuterAudit.seeds.get(job-2);check(seed.is(PechTrades.mod("void_seed").getItem())&&seed.getCount()==1,"eye infusion uses its actual cultist-dropped void seed");components.set(0,seed);}
   ProgressionInfusionAudit.put(at.below(2),center);for(int i=0;i<components.size();i++)ProgressionInfusionAudit.put(peds.get(i),components.get(i));before.clear();jars.forEach((a,pos)->before.put(a,jar(a).state.amount));
   ProgressionInfusionAudit.player.moveTo(at.getX()+2,at.getY()-1,at.getZ()+2);ProgressionInfusionAudit.click();check(ProgressionInfusionAudit.matrix.logic.crafting,"earned research starts late infusion "+recipe.key());phase=1;
  }
  if(phase==1&&!ProgressionInfusionAudit.matrix.logic.crafting){earned.add(collect(recipe.output().get()));for(var pos:peds)check(((InfusionContent.PedestalEntity)l.getBlockEntity(pos)).item().isEmpty(),"late infusion consumes each component");jars.forEach((a,pos)->check(jar(a).state.amount==before.get(a)-recipe.aspects().getAmount(a),"late infusion exact essentia consumption "+a.getTag()));System.out.println("TC4_PROGRESSION earned late infusion "+recipe.key()+" job "+job);if(++job==JOBS.size())return true;next();}
  if(ProgressionAudit.ticks-started>12000)throw new IllegalStateException("TC4_PROGRESSION FAIL late infusion timeout "+recipe.key()+" phase "+phase+" furnaceVis "+f.state.vis);return false;
 }
}
