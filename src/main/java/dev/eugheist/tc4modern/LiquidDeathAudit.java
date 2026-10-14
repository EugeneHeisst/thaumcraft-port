package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
/** Actual world damage, drops, finite-volume ticks and projectile effects in FocalInspection. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class LiquidDeathAudit {
 static final BlockPos BASE=FocalAudit.BASE.offset(3,0,6);static volatile boolean passed,volumePassed;static int ticks;static volatile int impacts;
 static void check(boolean ok,String message){if(!ok)throw new IllegalStateException("TC4_DEATH FAIL "+message);System.out.println("TC4_DEATH PASS "+message);}
 static void prepare(ServerPlayer player){
  var level=player.serverLevel();player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();player.getInventory().clearContent();
  for(var cat:thaumcraft.api.research.ResearchCategories.researchCategories.values())for(var r:cat.research.values())DevCommands.grant(player.getPersistentData(),r.key,new java.util.HashSet<>());
  ThaumometerItem.sync(player,"");AlchemyStarterAudit.run(player);
  var bottle=CrucibleRecipes.byKey().get("BottleTaint");
  check(bottle.catalystMatches(EssentiaItems.phialOf(Aspect.TAINT,8))&&!bottle.catalystMatches(EssentiaItems.phialOf(Aspect.AIR,8))&&!bottle.catalystMatches(EssentiaItems.phialOf(Aspect.TAINT,1)),"bottle catalyst checks exact vitium and amount");
  for(int amount=1;amount<=4;amount++){
   var cow=EntityType.COW.create(level);cow.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(20);cow.moveTo(BASE.getCenter());cow.setNoAi(true);level.addFreshEntity(cow);
   var state=LiquidDeath.BLOCK.get().defaultBlockState().setValue(LiquidDeath.AMOUNT,amount);
   LiquidDeath.BLOCK.get().entityInside(state,level,BASE,cow);
   check(state.getLightEmission()==(amount-1)*2&&state.getFluidState().getOwnHeight()==amount*.875f/4,"original scaled light and fluid height "+amount);
   check(cow.getHealth()==cow.getMaxHealth()-amount,"dissolve damage equals volume "+amount);cow.discard();
  }
  var drops=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
  var cow=EntityType.COW.create(level);cow.moveTo(BASE.getCenter());
  var source=new net.minecraft.world.damagesource.DamageSource(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(LiquidDeath.DAMAGE));
  check(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)&&!source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE),"dissolve bypasses armour without fire damage");
  for(int i=0;i<30;i++)LiquidDeathDrops.drops(new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(cow,source,drops,false));
  check(!drops.isEmpty()&&drops.stream().allMatch(e->e.getItem().is(LiquidDeath.CRYSTAL.get())&&EssentiaItems.getAspects(e.getItem()).visSize()==1),"dissolve produces aspect crystals");
  for(int i=0;i<16;i++){var victim=EntityType.COW.create(level);victim.moveTo(BASE.getCenter());victim.setHealth(1);level.addFreshEntity(victim);victim.hurt(source,4);}
  var actual=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(BASE).inflate(3));
  check(actual.stream().anyMatch(e->e.getItem().is(LiquidDeath.CRYSTAL.get()))&&actual.stream().anyMatch(e->e.getItem().is(Items.BEEF)),"actual dissolved mobs retain normal loot alongside crystals");for(var item:actual)item.discard();
  var full=LiquidDeath.BLOCK.get().defaultBlockState();
  for(boolean creative:new boolean[]{false,true}){
   level.setBlock(BASE,Blocks.AIR.defaultBlockState(),3);player.setGameMode(creative?net.minecraft.world.level.GameType.CREATIVE:net.minecraft.world.level.GameType.SURVIVAL);player.connection.teleport(BASE.getX()+.5,BASE.getY()+2,BASE.getZ()+.5,0,90);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(LiquidDeath.BUCKET.get()));
   var result=LiquidDeath.BUCKET.get().use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);
   check(level.getBlockState(BASE).is(LiquidDeath.BLOCK.get())&&level.getBlockState(BASE).getValue(LiquidDeath.AMOUNT)==4&&result.getObject().is(creative?LiquidDeath.BUCKET.get():Items.BUCKET),"actual bucket placement and remainder creative="+creative);
  }
  player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
  for(boolean creative:new boolean[]{false,true}){
   player.setGameMode(creative?net.minecraft.world.level.GameType.CREATIVE:net.minecraft.world.level.GameType.SURVIVAL);player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new ItemStack(BottleTaint.ITEM.get(),8));
   var result=BottleTaint.ITEM.get().use(level,player,net.minecraft.world.InteractionHand.MAIN_HAND);
   var thrown=level.getEntitiesOfClass(BottleTaint.Projectile.class,new net.minecraft.world.phys.AABB(player.blockPosition()).inflate(10));
   check(result.getObject().getCount()==(creative?8:7)&&thrown.size()==1&&thrown.getFirst().getOwner()==player&&thrown.getFirst().getDeltaMovement().length()>.45&&thrown.getFirst().getDeltaMovement().length()<.55,"actual bottle use consumes one and launches owned projectile creative="+creative);
   for(var entity:thrown)entity.discard();
  }
  player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);player.getAbilities().flying=true;player.onUpdateAbilities();
  check(RecipeAspects.get(LiquidDeath.crystal(Aspect.FIRE,1),level).equals(java.util.Map.of("ignis",1)),"crystal carries exactly its own aspect for scanning and dissolution");
  var empty=new ItemStack(LiquidDeath.CRYSTAL.get());empty.getItem().inventoryTick(empty,level,player,0,false);check(EssentiaItems.getAspects(empty)!=null,"original blank crystal receives random aspect in inventory");
  level.setBlock(BASE,full,3);
  check(LiquidDeath.BLOCK.get().pickupBlock(player,level,BASE,full).is(LiquidDeath.BUCKET.get())&&level.isEmptyBlock(BASE),"full finite block fills bucket");
  var partial=full.setValue(LiquidDeath.AMOUNT,3);level.setBlock(BASE,partial,3);
  check(LiquidDeath.BLOCK.get().pickupBlock(player,level,BASE,partial).isEmpty(),"partial fluid cannot multiply buckets");
  level.setBlock(BASE,full,3);
  var zombie=EntityType.HUSK.create(level);zombie.moveTo(BASE.offset(0,0,3).getCenter());zombie.setNoAi(true);level.addFreshEntity(zombie);
  var target=EntityType.COW.create(level);target.moveTo(BASE.offset(0,0,2).getCenter());target.setNoAi(true);level.addFreshEntity(target);
  // Reset only the isolated fixture's biome cells so repeated tests exercise conversion.
  var plains=level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BIOME).getHolderOrThrow(net.minecraft.world.level.biome.Biomes.PLAINS);
  for(int cx=0;cx<=1;cx++)for(int cz=0;cz<=1;cz++){var chunk=level.getChunk(cx,cz);chunk.fillBiomesFromNoise((x,y,z,sampler)->plains,level.getChunkSource().randomState().sampler());chunk.setUnsaved(true);}
  level.random.setSeed(10);
  var projectile=new BottleTaint.Projectile(BottleTaint.ENTITY.get(),level);projectile.setPos(BASE.getX()+.5,BASE.getY()+.1,BASE.getZ()+3.5);level.addFreshEntity(projectile);projectile.impact();
  check(target.hasEffect(TC4Modern.FLUX_TAINT)&&target.getEffect(TC4Modern.FLUX_TAINT).getDuration()==100&&!zombie.hasEffect(TC4Modern.FLUX_TAINT),"bottle poisons living targets for 100 ticks and excludes undead");target.discard();zombie.discard();
  int tainted=0,fibres=0;for(int dx=-5;dx<=5;dx++)for(int dz=-2;dz<=8;dz++){var cell=BASE.offset(dx,0,dz);if(TaintBiomes.isTainted(level,cell))tainted++;if(level.getBlockState(cell).is(TC4Modern.TAINT_FIBRES.get()))fibres++;}
  check(tainted>0&&fibres>0,"bottle changes biome and grows taint fibres on solid ground");
  for(int dx=-3;dx<=7;dx++)for(int dz=-3;dz<=7;dz++)for(int dy=0;dy<=3;dy++){var cell=BASE.offset(dx,dy,dz);if(level.getBlockState(cell).is(LiquidDeath.BLOCK.get()))level.setBlock(cell,Blocks.AIR.defaultBlockState(),3);}
  for(var entity:level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(BASE).inflate(20)))entity.discard();
  player.getInventory().clearContent();player.removeAllEffects();
  // Keep one full cell in a closed basin for visible bubbling, and one free bucket to spread.
  for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)if(dx!=0||dz!=0)level.setBlock(BASE.offset(dx,0,dz),Blocks.GLASS.defaultBlockState(),3);
  level.setBlock(BASE,full,3);level.setBlock(BASE.offset(4,0,0),full,3);
  player.getInventory().setItem(0,new ItemStack(LiquidDeath.BUCKET.get()));player.getInventory().setItem(1,new ItemStack(BottleTaint.ITEM.get(),8));player.getInventory().setItem(2,LiquidDeath.crystal(Aspect.FIRE,8));player.getInventory().selected=8;
  player.connection.teleport(BASE.getX()+3,BASE.getY()+3,BASE.getZ()-4,0,35);passed=true;
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.deathAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;var server=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks==80)server.execute(()->prepare(server.getPlayerList().getPlayer(id)));
  if(ticks==140){mc.player.getInventory().selected=8;check(passed,"server mechanics completed");shot(mc,"death-fluid.png");}
  if(ticks==160)server.execute(()->{var level=server.overworld();var projectile=new BottleTaint.Projectile(BottleTaint.ENTITY.get(),level);projectile.setPos(BASE.getX()+2,BASE.getY()+1,BASE.getZ()+2);level.addFreshEntity(projectile);projectile.setDeltaMovement(0,-.1,0);});
  if(ticks>=170&&ticks<=190&&ticks%5==0)shot(mc,"taint-impact-"+ticks+".png");
  if(ticks==230)server.execute(()->{var level=server.overworld();int sum=0;for(int dx=2;dx<=6;dx++)for(int dz=-3;dz<=3;dz++){var state=level.getBlockState(BASE.offset(dx,0,dz));if(state.is(LiquidDeath.BLOCK.get()))sum+=state.getValue(LiquidDeath.AMOUNT);}System.out.println("TC4_DEATH_REGION "+sum);int all=0;for(int dx=-3;dx<=7;dx++)for(int dz=-3;dz<=7;dz++)for(int dy=0;dy<=3;dy++){var cell=BASE.offset(dx,dy,dz);var state=level.getBlockState(cell);if(state.is(LiquidDeath.BLOCK.get())){all+=state.getValue(LiquidDeath.AMOUNT);System.out.println("TC4_DEATH_CELL "+cell+" amount="+state.getValue(LiquidDeath.AMOUNT));}}check(all==8,"both buckets together conserve eight quanta across all heights");volumePassed=true;});
  if(ticks==250){shot(mc,"death-items.png");mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));}
  if(ticks==260)shot(mc,"death-inventory.png");
  if(ticks==280){check(volumePassed,"server volume check completed");check(impacts>0,"actual projectile impact reaches client particles and sound");check(AlchemyStarterAudit.passed,"crucible recipes completed");mc.stop();}
 }
 static void shot(net.minecraft.client.Minecraft mc,String name){try{var out=java.nio.file.Path.of("focal-audit");java.nio.file.Files.createDirectories(out);try(var im=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){im.writeToFile(out.resolve(name));}}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}}
}
