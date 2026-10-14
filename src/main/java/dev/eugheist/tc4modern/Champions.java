package dev.eugheist.tc4modern;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.damagesource.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;

/** EntityUtils.makeChampion and the thirteen original ChampionModifier effects. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class Champions {
 public interface Boss {}
 public static final String[] NAMES={"bold","spine","armor","mighty","grim","warded","warp","undying","fiery","sickly","venomous","vampiric","infested"};
 public static final DeferredHolder<Attribute,Attribute> MOD=DeferredHolder.create(Registries.ATTRIBUTE,AlchemyContent.id("champion"));
 public static java.util.function.Consumer<Mob> visual=m->{};
 public static int type(LivingEntity mob){return mob.getAttribute(MOD)==null?-1:(int)mob.getAttributeValue(MOD);}
 public static boolean boss(Mob m){return m instanceof Boss;}
 static AttributeModifier modifier(String name,double amount,AttributeModifier.Operation operation){return new AttributeModifier(AlchemyContent.id("champion_"+name),amount,operation);}
 static void apply(Mob mob,net.minecraft.core.Holder<Attribute> attribute,String name,double amount,AttributeModifier.Operation operation){var value=mob.getAttribute(attribute);if(value!=null){var mod=modifier(name,amount,operation);value.removeModifier(mod.id());value.addPermanentModifier(mod);}}
 public static void make(Mob mob,int type,boolean persist){if(type<0||type>=NAMES.length)throw new IllegalArgumentException("Invalid champion "+type);if(mob instanceof Creeper)type=0;mob.getAttribute(MOD).setBaseValue(type);
  if(!boss(mob)){apply(mob,Attributes.MAX_HEALTH,"health",30,AttributeModifier.Operation.ADD_VALUE);apply(mob,Attributes.ATTACK_DAMAGE,"damage",2,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);mob.heal(25);mob.setCustomName(Component.translatable("champion.mod."+NAMES[type]).append(" ").append(mob.getName()));}
  if(mob instanceof ThaumcraftBoss boss)boss.generateName();
  if(persist)mob.setPersistenceRequired();
  switch(type){case 0->apply(mob,Attributes.MOVEMENT_SPEED,"bold",.3,AttributeModifier.Operation.ADD_MULTIPLIED_BASE);case 3->apply(mob,Attributes.ATTACK_DAMAGE,"mighty",3,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);case 5->{/* 1.21 clamps absorption to MAX_ABSORPTION (0 for mobs): raise the cap to the original shield size */int bh=(int)mob.getAttributeBaseValue(Attributes.MAX_HEALTH)/2;apply(mob,Attributes.MAX_ABSORPTION,"warded",bh,AttributeModifier.Operation.ADD_VALUE);mob.setAbsorptionAmount(mob.getAbsorptionAmount()+bh);}}
 }
 static int whitelist(Mob m){if(boss(m))return 200;if(m instanceof InhabitedZombie)return 3;if(m instanceof Taintacle t&&!t.small())return 2;if(m instanceof Wisp||m instanceof Witch||m instanceof CultistEntity||m instanceof PechEntity)return 1;if(m instanceof Zombie||m instanceof Spider||m instanceof Blaze||m instanceof EnderMan||m instanceof AbstractSkeleton||m instanceof EldritchCrab)return 0;return -1;}
 /** Original event roll, including reduced disabled-mode chances and two dangerous maze cell types. */
 static int chance(int roll,net.minecraft.world.Difficulty difficulty,boolean enabled,boolean outer,boolean spooky,boolean dangerous,int weight,boolean boss){
  int chance=roll;
  if(difficulty==net.minecraft.world.Difficulty.EASY||!enabled)chance+=2;
  if(difficulty==net.minecraft.world.Difficulty.HARD)chance-=enabled?2:0;
  if(outer)chance-=3;
  if(spooky)chance-=enabled?2:1;
  if(dangerous)chance-=enabled?10:3;
  if(enabled||boss)chance-=Math.max(0,weight-1);
  return chance;
 }
 static boolean dangerous(ServerLevel level,net.minecraft.core.BlockPos pos){
  if(!level.dimension().location().equals(AlchemyContent.id("outer_lands")))return false;
  var cell=OuterMazeData.get(level).cell(pos.getX()>>4,pos.getZ()>>4);return cell!=null&&(cell.feature==6||cell.feature==8);
 }
 @SubscribeEvent static void join(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e){
  if(!(e.getLevel() instanceof ServerLevel level)||!(e.getEntity() instanceof Mob mob)||mob.getAttribute(MOD)==null||mob.getAttributeValue(MOD)>=-1)return;
  int weight=whitelist(mob);boolean outer=level.dimension().location().equals(AlchemyContent.id("outer_lands"));
  var biome=level.getBiome(mob.blockPosition());boolean spooky=level.dimension()==net.minecraft.world.level.Level.NETHER||level.dimension()==net.minecraft.world.level.Level.END||biome.is(net.minecraft.tags.TagKey.create(Registries.BIOME,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("c","is_spooky")))||biome.is(AlchemyContent.id("eerie"));
  int roll=chance(level.random.nextInt(100),level.getDifficulty(),SpawnConfig.champions(),outer,spooky,dangerous(level,mob.blockPosition()),weight,boss(mob));
  if(weight>=0&&roll<=0&&mob.getAttributeBaseValue(Attributes.MAX_HEALTH)>=10)make(mob,level.random.nextInt(NAMES.length),false);else mob.getAttribute(MOD).setBaseValue(-1);
 }

 @SubscribeEvent static void tick(net.neoforged.neoforge.event.tick.EntityTickEvent.Post e){if(!(e.getEntity() instanceof Mob mob)||!mob.isAlive())return;int type=type(mob);if(type<0)return;if(mob.level().isClientSide){visual.accept(mob);return;}if(type==7&&mob.tickCount%20==0)mob.heal(1);if(type==5&&mob.invulnerableTime<=0&&mob.tickCount%25==0&&mob.getAbsorptionAmount()<(int)mob.getAttributeBaseValue(Attributes.MAX_HEALTH)/2)mob.setAbsorptionAmount(mob.getAbsorptionAmount()+1);}
 @SubscribeEvent static void damage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent e){if(e.getEntity().level().isClientSide)return;var victim=e.getEntity();var source=e.getSource();float amount=e.getAmount();int type=type(victim);
  if(victim instanceof Mob mob&&type>=0){if(type==5&&mob.getAbsorptionAmount()>0){shield(mob,source);}else if(source.getDirectEntity() instanceof LivingEntity attacker){switch(type){case 1->{if(!source.is(DamageTypes.THORNS)){attacker.hurt(mob.damageSources().thorns(mob),1+mob.getRandom().nextInt(3));attacker.playSound(net.minecraft.sounds.SoundEvents.THORNS_HIT,.5f,1);}}case 2->{if(!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR))amount=amount*19/25;}case 12->{if(mob.getRandom().nextFloat()<.4f){var spider=TC4Modern.TAINT_SPIDER.get().create(mob.level());spider.moveTo(mob.getX(),mob.getY()+mob.getBbHeight()/2,mob.getZ(),mob.getRandom().nextFloat()*360,0);mob.level().addFreshEntity(spider);mob.playSound(FociContent.sound("gore"),.5f,1);}}}}}
  if(amount>0&&source.getDirectEntity() instanceof Mob attacker){switch(type(attacker)){case 4->{if(attacker.getRandom().nextFloat()<.4f)victim.addEffect(new MobEffectInstance(MobEffects.WITHER,200));}case 6->{if(attacker.getRandom().nextFloat()<.33f&&victim instanceof net.minecraft.world.entity.player.Player p)PlayerWarp.add(p,WarpState.Kind.TEMPORARY,1+attacker.getRandom().nextInt(3));}case 8->{if(attacker.getRandom().nextFloat()<.4f)victim.igniteForSeconds(4);}case 9->{if(attacker.getRandom().nextFloat()<.4f)victim.addEffect(new MobEffectInstance(MobEffects.HUNGER,500));}case 10->{if(attacker.getRandom().nextFloat()<.4f)victim.addEffect(new MobEffectInstance(MobEffects.POISON,100));}case 11->attacker.heal(Math.max(2,amount/2));}}
  e.setAmount(amount);
 }
 static void shield(Mob mob,DamageSource source){int target=source.getEntity()==null?-1:source.getEntity().getId();if(source.is(DamageTypes.FALL))target=-2;if(source.is(DamageTypes.FALLING_ANVIL)||source.is(DamageTypes.FALLING_BLOCK))target=-3;net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear((ServerLevel)mob.level(),null,mob.getX(),mob.getY(),mob.getZ(),32,new RunicRingShield.Impact(mob.getId(),target));mob.playSound(FociContent.sound("runicshieldeffect"),.66f,1.1f+mob.getRandom().nextFloat()*.1f);}
 static int looting(Mob mob,DamageSource source){return source.getEntity() instanceof LivingEntity killer?net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(mob.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),killer):0;}
 @SubscribeEvent static void drops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent e){if(!(e.getEntity() instanceof Mob mob)||mob.level().isClientSide||boss(mob)||type(mob)<0||!e.isRecentlyHit()||e.getSource().getEntity() instanceof net.neoforged.neoforge.common.util.FakePlayer)return;ExperienceOrb.award((ServerLevel)mob.level(),mob.position(),5+mob.getRandom().nextInt(3));int rarity=Math.min(2,(mob.getRandom().nextInt(9)+looting(mob,e.getSource())+4)/5);e.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(mob.level(),mob.getX(),mob.getEyeY(),mob.getZ(),new net.minecraft.world.item.ItemStack(LootBags.ITEMS.get(rarity).get())));}
 @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)public static final class Registration {
  @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ATTRIBUTE,h->h.register(MOD.getId(),new RangedAttribute("attribute.tc4modern.champion",-2,-2,100).setSyncable(true)));}
  @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent e){for(var type:e.getTypes())e.add(type,MOD);}
 }
}
