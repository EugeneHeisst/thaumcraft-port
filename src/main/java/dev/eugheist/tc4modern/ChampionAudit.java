package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Visible audit: champion mobs (EntityUtils.makeChampion + 13 ChampionModifiers), loot bags and void metal. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=Dist.CLIENT)
public final class ChampionAudit {
 static final BlockPos BASE=new BlockPos(640,121,-90);static int ticks;static volatile String failed;static volatile boolean prepared;static Zombie[] row=new Zombie[13];static float undyingStart;
 static void check(boolean b,String s){if(!b){failed=s;System.out.println("TC4_CHAMPION FAIL "+s);throw new IllegalStateException("TC4_CHAMPION FAIL "+s);}System.out.println("TC4_CHAMPION PASS "+s);}
 static Zombie zombie(ServerPlayer p,double x,double z,int champion){var l=p.serverLevel();var zb=EntityType.ZOMBIE.create(l);zb.moveTo(x,BASE.getY(),z,180,0);zb.setNoAi(true);zb.setPersistenceRequired();zb.getAttribute(Champions.MOD).setBaseValue(-1);l.addFreshEntity(zb);if(champion>=0)Champions.make(zb,champion,true);return zb;}
 static void hit(LivingEntity victim,LivingEntity attacker,float amount){victim.invulnerableTime=0;victim.hurt(victim.damageSources().mobAttack(attacker),amount);}
 static void setup(ServerPlayer p){
  p.setInvulnerable(false);var l=p.serverLevel();if(p.level()!=p.server.overworld())p.teleportTo(p.server.overworld(),BASE.getX()+.5,BASE.getY(),BASE.getZ()-6,0,10);
  p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.getInventory().clearContent();l.setDayTime(13000);l.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING).set(false,l.getServer());
  l.getEntitiesOfClass(Entity.class,new AABB(BASE).inflate(40)).stream().filter(e->!(e instanceof net.minecraft.world.entity.player.Player)).forEach(Entity::discard);
  for(int x=-16;x<=16;x++)for(int z=-12;z<=12;z++)for(int y=-1;y<6;y++)l.setBlock(BASE.offset(x,y,z),(y==-1?Blocks.POLISHED_DEEPSLATE:Blocks.AIR).defaultBlockState(),3);
  p.connection.teleport(BASE.getX()+.5,BASE.getY()+1.5,BASE.getZ()-7,0,12);
  // all thirteen modifiers on zombies (base 20 hp, 3 attack, 2 armour)
  for(int i=0;i<13;i++)row[i]=zombie(p,BASE.getX()-12+i*2+.5,BASE.getZ()+.5,i);
  for(int i=0;i<13;i++){var z=row[i];check(Champions.type(z)==i,"modifier "+Champions.NAMES[i]+" stored");check(Math.abs(z.getMaxHealth()-50)<1e-4&&z.getHealth()>=45,"+30 health "+Champions.NAMES[i]+" hp="+z.getHealth()+"/"+z.getMaxHealth());
   double dmg=z.getAttributeValue(Attributes.ATTACK_DAMAGE);check(Math.abs(dmg-(i==3?36:9))<1e-4,"attack x3"+(i==3?" and mighty x4":"")+" = "+dmg);
   check(z.hasCustomName()&&z.getCustomName().getString().endsWith(z.getType().getDescription().getString()),"localized champion name "+z.getCustomName().getString());}
  check(Math.abs(row[0].getAttributeValue(Attributes.MOVEMENT_SPEED)-row[1].getAttributeValue(Attributes.MOVEMENT_SPEED)*1.3)<1e-6,"bold +30% base speed");
  check(Math.abs(row[5].getAbsorptionAmount()-10)<1e-4,"warded shield = half base health (10)");
  var creeper=EntityType.CREEPER.create(l);creeper.moveTo(BASE.getX()+14,BASE.getY(),BASE.getZ()+5);creeper.setNoAi(true);creeper.getAttribute(Champions.MOD).setBaseValue(-1);l.addFreshEntity(creeper);Champions.make(creeper,7,true);check(Champions.type(creeper)==0,"creeper champions are always bold");creeper.discard();
  var copy=EntityType.ZOMBIE.create(l);var tag=new net.minecraft.nbt.CompoundTag();row[9].saveWithoutId(tag);copy.load(tag);check(Champions.type(copy)==9&&Math.abs(copy.getMaxHealth()-50)<1e-4,"champion survives save/load");
  // attacked effects (type 2): spined, armored, infested
  var punch=zombie(p,BASE.getX()+.5,BASE.getZ()+6.5,-1);
  float before=punch.getHealth();hit(row[1],punch,4);check(punch.getHealth()<before,"spined reflects 1-3 thorns damage to the attacker "+before+"->"+punch.getHealth());punch.setHealth(punch.getMaxHealth());
  var plain=zombie(p,BASE.getX()+3.5,BASE.getZ()+6.5,-1);plain.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);plain.setHealth(100);row[2].getAttribute(Attributes.MAX_HEALTH).setBaseValue(70);row[2].setHealth(100);
  float a0=row[2].getHealth(),p0=plain.getHealth();hit(row[2],punch,10);hit(plain,punch,10);float ar=a0-row[2].getHealth(),pl=p0-plain.getHealth();check(Math.abs(ar/pl-19f/25f)<.02,"armored takes 19/25 of non-bypassing damage "+ar+" vs "+pl);
  int spiders=0;for(int i=0;i<40;i++){row[12].setHealth(row[12].getMaxHealth());hit(row[12],punch,1);}spiders=l.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSpider.class,new AABB(row[12].blockPosition()).inflate(4)).size();check(spiders>=8&&spiders<=28,"infested spawns taint spiderlings ~40% "+spiders+"/40");l.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSpider.class,new AABB(BASE).inflate(30)).forEach(Entity::discard);
  // attacking effects (type 1): grim wither, fiery fire, sickly hunger, venomous poison, vampiric heal, warp
  /* a pig: undead are immune to poison */var target=EntityType.PIG.create(l);target.moveTo(BASE.getX()-3.5,BASE.getY(),BASE.getZ()+6.5);target.setNoAi(true);l.addFreshEntity(target);target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);target.setHealth(1000);
  int wither=0,fire=0,hunger=0,poison=0;for(int i=0;i<60;i++){target.removeAllEffects();target.clearFire();hit(target,row[4],1);if(target.hasEffect(MobEffects.WITHER))wither++;hit(target,row[8],1);if(target.isOnFire())fire++;hit(target,row[9],1);if(target.hasEffect(MobEffects.HUNGER))hunger++;hit(target,row[10],1);if(target.hasEffect(MobEffects.POISON))poison++;}
  check(wither>10&&wither<40,"grim withers ~40% "+wither+"/60");check(fire>10&&fire<40,"fiery ignites ~40% "+fire+"/60");check(hunger>10&&hunger<40,"sickly hungers ~40% "+hunger+"/60");check(poison>10&&poison<40,"venomous poisons ~40% "+poison+"/60");
  row[11].setHealth(20);hit(target,row[11],1);check(row[11].getHealth()>=22,"vampiric heals max(2,dmg/2) on hit "+row[11].getHealth());
  /* peaceful drops mob damage to players before any damage event */l.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);var warpBefore=PlayerWarp.load(p).get(WarpState.Kind.TEMPORARY);p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,4));int landed=0;for(int i=0;i<40;i++){p.invulnerableTime=0;p.setHealth(p.getMaxHealth());if(p.hurt(p.damageSources().mobAttack(row[6]),1))landed++;}System.out.println("TC4_CHAMPION debug warp hits landed="+landed+" difficulty="+l.getDifficulty()+" mode="+p.gameMode.getGameModeForPlayer()+" invul="+p.getAbilities().invulnerable+" warded="+row[6].isAlive());int warpGain=PlayerWarp.load(p).get(WarpState.Kind.TEMPORARY)-warpBefore;check(warpGain>0,"warp champion adds temporary warp to players +"+warpGain);
  var st=PlayerWarp.load(p);st.add(WarpState.Kind.TEMPORARY,-warpGain);PlayerWarp.save(p,st);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();p.removeAllEffects();
  // passive: undying regen, warded shield regen
  row[7].setHealth(10);undyingStart=10;row[5].setAbsorptionAmount(0);
  // natural selection (normal difficulty: 1%), whitelist, base health >= 10
  l.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL,true);int champs=0,cows=0;
  for(int i=0;i<1500;i++){var z=EntityType.ZOMBIE.create(l);z.moveTo(BASE.getX()+10,BASE.getY()-30,BASE.getZ()+10);z.setNoAi(true);l.addFreshEntity(z);if(Champions.type(z)>=0)champs++;z.discard();var c=EntityType.COW.create(l);c.moveTo(BASE.getX()+10,BASE.getY()-30,BASE.getZ()+10);l.addFreshEntity(c);if(Champions.type(c)>=0)cows++;c.discard();}
  check(champs>=3&&champs<=35,"natural zombie champions about 1% on normal: "+champs+"/1500");check(cows==0,"non-whitelisted mobs never become champions");
  // drops: player kill -> loot bag + xp
  var victim=row[3];victim.setLastHurtByPlayer(p);victim.invulnerableTime=0;victim.hurt(l.damageSources().playerAttack(p),1000);
  var bags=l.getEntitiesOfClass(ItemEntity.class,new AABB(victim.blockPosition()).inflate(3),e->LootBags.ITEMS.stream().anyMatch(h->e.getItem().is(h.get())));check(!victim.isAlive()&&bags.size()==1,"player-killed champion drops one loot bag");
  check(!l.getEntitiesOfClass(ExperienceOrb.class,new AABB(victim.blockPosition()).inflate(3)).isEmpty(),"and bonus experience");bags.forEach(Entity::discard);
  // loot bags: 8..12 items, consumed
  for(int r=0;r<3;r++){p.getInventory().clearContent();var bag=new ItemStack(LootBags.ITEMS.get(r).get());p.setItemInHand(InteractionHand.MAIN_HAND,bag);bag.use(l,p,InteractionHand.MAIN_HAND);int n=0;for(int s=0;s<p.getInventory().getContainerSize();s++)if(!p.getInventory().getItem(s).isEmpty())n++;
   int ground=l.getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(3)).size();check(p.getMainHandItem().isEmpty()&&n+ground>=8,"loot bag "+r+" consumed and gave "+(n+ground)+" stacks (8..12 rolls)");l.getEntitiesOfClass(ItemEntity.class,p.getBoundingBox().inflate(3)).forEach(Entity::discard);}
  p.getInventory().clearContent();
  // void metal
  var sword=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_sword")));var helm=new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("void_helmet")));
  check(!sword.isEmpty()&&!helm.isEmpty(),"void sword and helmet registered");sword.setDamageValue(10);helm.setDamageValue(10);p.getInventory().setItem(0,sword);p.setItemSlot(EquipmentSlot.HEAD,helm);
  var sr=CrucibleRecipes.byKey().entrySet().stream().filter(e->e.getKey().equals("VoidMetal")||e.getKey().equals("VoidSeed")).count();check(sr==2,"VoidSeed and VoidMetal crucible recipes present");
  sword.hurtEnemy(target,p);check(target.hasEffect(MobEffects.WEAKNESS),"void weapon weakens the target");
 }
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
  if(!Boolean.getBoolean("tc4modern.championAuditOnly"))return;var mc=Minecraft.getInstance();if(mc.player==null)return;ticks++;if(ticks==10&&mc.player.isDeadOrDying())mc.player.respawn();var s=mc.getSingleplayerServer();var id=mc.player.getUUID();
  if(ticks>100&&(!prepared||row[7].tickCount<80)){ticks=101;return;}
  if(ticks==100)s.execute(()->{try{setup(s.getPlayerList().getPlayer(id));prepared=true;}catch(RuntimeException ex){if(failed==null)failed=String.valueOf(ex);ex.printStackTrace();}});
  if(ticks==190)s.execute(()->{var p=s.getPlayerList().getPlayer(id);check(row[7].getHealth()>=undyingStart+3,"undying regenerates 1 hp per second "+row[7].getHealth());check(row[5].getAbsorptionAmount()>=2,"warded shield regrows 1 per 25 ticks "+row[5].getAbsorptionAmount());
   check(p.getInventory().getItem(0).getDamageValue()<10&&p.getItemBySlot(EquipmentSlot.HEAD).getDamageValue()<10,"void sword in inventory and worn void helmet repair themselves");});
  if(ticks==200){check(ChampionClient.particles>50,"client champion FX spawned particles="+ChampionClient.particles);LiquidDeathAudit.shot(mc,"champion-row.png");}
  if(ticks==210)s.execute(()->s.getPlayerList().getPlayer(id).connection.teleport(BASE.getX()-8.5,BASE.getY()+.3,BASE.getZ()-2.5,0,5));
  if(ticks==235)LiquidDeathAudit.shot(mc,"champion-closeup.png");
  if(ticks==250)s.execute(()->{var p=s.getPlayerList().getPlayer(id);var l=p.serverLevel();p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);p.getInventory().clearContent();
   String[] ids={"void_sword","void_pickaxe","void_ingot","loot_bag_common","loot_bag_uncommon","loot_bag_rare","primal_ring_aer","primal_ring_ignis","void_helmet"};
   for(int i=0;i<ids.length;i++)p.getInventory().setItem(i,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id(ids[i]))));p.getInventory().selected=0;
   var stand=EntityType.ARMOR_STAND.create(l);stand.moveTo(BASE.getX()+.5,BASE.getY(),BASE.getZ()-3.5,180,0);for(var slot:new EquipmentSlot[]{EquipmentSlot.HEAD,EquipmentSlot.CHEST,EquipmentSlot.LEGS,EquipmentSlot.FEET})stand.setItemSlot(slot,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id(switch(slot){case HEAD->"void_helmet";case CHEST->"void_chestplate";case LEGS->"void_leggings";default->"void_boots";}))));l.addFreshEntity(stand);
   for(int i=0;i<4;i++){var drop=new ItemEntity(l,BASE.getX()-1+i*.7,BASE.getY()+.4,BASE.getZ()-4.8,new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id(ids[i*2]))));drop.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);drop.setNoGravity(true);drop.setNeverPickUp();l.addFreshEntity(drop);}
   p.connection.teleport(BASE.getX()+.5,BASE.getY(),BASE.getZ()-6.5,0,15);p.containerMenu.broadcastChanges();});
  if(ticks==270){for(var name:new String[]{"void_sword","void_pickaxe","void_axe","void_shovel","void_hoe","void_ingot","void_helmet","void_chestplate","void_leggings","void_boots","loot_bag_common","loot_bag_uncommon","loot_bag_rare","primal_ring_aer","primal_ring_aqua","primal_ring_ignis","primal_ring_ordo","primal_ring_perditio","primal_ring_terra"}){var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id(name));var model=mc.getItemRenderer().getModel(new ItemStack(item),mc.level,mc.player,0);check(model!=mc.getModelManager().getMissingModel(),"item model "+name);for(var c:java.util.List.of(ItemDisplayContext.GUI,ItemDisplayContext.GROUND,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND)){var sc=model.getTransforms().getTransform(c).scale;check(sc.x<=1&&sc.y<=1&&sc.z<=1,"bounded "+c+" "+name);}}
   LiquidDeathAudit.shot(mc,"champion-loot-hand-ground-armor.png");}
  if(ticks==275)mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
  if(ticks==290){LiquidDeathAudit.shot(mc,"champion-loot-inventory.png");mc.setScreen(null);}
  if(ticks==300){System.out.println(failed==null?"TC4_CHAMPION ALL PASS":"TC4_CHAMPION FAILED "+failed);mc.stop();}
 }
}
