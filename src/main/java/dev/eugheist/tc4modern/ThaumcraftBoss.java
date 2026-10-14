package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.*;
/** Shared original boss damage cap, enrage, regeneration, persistent home and reward. */
public abstract class ThaumcraftBoss extends Monster implements Champions.Boss {
 private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(ThaumcraftBoss.class,EntityDataSerializers.INT);
 protected int spawnTimer;private final Map<Integer,Integer> aggro=new HashMap<>();
 private final ServerBossEvent bar=new ServerBossEvent(getName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
 protected ThaumcraftBoss(EntityType<? extends Monster> type,Level level){super(type,level);xpReward=50;}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ANGER,0);}
 public int anger(){return entityData.get(ANGER);}public int spawnTimer(){return spawnTimer;}protected void anger(int n){entityData.set(ANGER,n);}
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.FOLLOW_RANGE,40);}
 protected Component championTitle(){int type=Champions.type(this);return type>=0?Component.translatable("champion.mod."+Champions.NAMES[type]):Component.empty();}
 @Override protected Component getTypeName(){return Component.translatable(getType().getDescriptionId(),championTitle());}
 public void generateName(){setCustomName(getTypeName());}
 @Override public boolean isEffectiveAi(){return spawnTimer<=0&&super.isEffectiveAi();}
 @Override public boolean isInvulnerableTo(DamageSource source){return spawnTimer>0||super.isInvulnerableTo(source);}
 @Override public boolean isPushable(){return spawnTimer<=0&&super.isPushable();}
 @Override public boolean isAlliedTo(Entity other){return other instanceof thaumcraft.api.entities.IEldritchMob;}
 @Override public boolean removeWhenFarAway(double distance){return false;}
 @Override protected int decreaseAirSupply(int air){return air;}
 @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);}
 @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
 @Override public void setCustomName(Component name){super.setCustomName(name);if(bar!=null)bar.setName(getDisplayName());}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data){restrictTo(blockPosition(),24);return data;}
 @Override public void tick(){super.tick();if(spawnTimer>0)spawnTimer--;if(anger()>0)anger(anger()-1);
  if(level().isClientSide){if(anger()>0&&random.nextInt(15)==0)level().addParticle(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,getRandomX(1),getY()+getBbHeight()+random.nextFloat()*.5,getRandomZ(1),random.nextGaussian()*.02,random.nextGaussian()*.02,random.nextGaussian()*.02);return;}
  if(tickCount%30==0)heal(1);if(getTarget()!=null&&!getTarget().isAlive())setTarget(null);if(getTarget()!=null&&tickCount%20==0)updateAggro();bar.setProgress(getHealth()/getMaxHealth());bar.setName(getDisplayName());
 }
 private void updateAggro(){int current=aggro.getOrDefault(getTarget().getId(),0),largest=current,players=0;LivingEntity next=null;var remove=new ArrayList<Integer>();
  for(var entry:aggro.entrySet()){int amount=entry.getValue();if(amount<=current+25||amount<=current*1.1||amount<=largest)continue;var entity=level().getEntity(entry.getKey());if(!(entity instanceof LivingEntity living)||!living.isAlive()||distanceToSqr(living)>16384){remove.add(entry.getKey());continue;}next=living;largest=amount;if(living instanceof net.minecraft.world.entity.player.Player)players++;}
  remove.forEach(aggro::remove);if(next!=null)setTarget(next);float old=getMaxHealth();
  for(int i=0;i<5;i++){var id=AlchemyContent.id("boss_players_"+i);getAttribute(Attributes.MAX_HEALTH).removeModifier(id);getAttribute(Attributes.ATTACK_DAMAGE).removeModifier(id);if(i<Math.min(5,players-1)){getAttribute(Attributes.MAX_HEALTH).addTransientModifier(new AttributeModifier(id,50,AttributeModifier.Operation.ADD_VALUE));getAttribute(Attributes.ATTACK_DAMAGE).addTransientModifier(new AttributeModifier(id,.5,AttributeModifier.Operation.ADD_VALUE));}}
  if(old>0)setHealth(getHealth()*getMaxHealth()/old);
 }
 @Override public boolean hurt(DamageSource source,float damage){if(!level().isClientSide){if(source.getEntity() instanceof LivingEntity attacker)aggro.merge(attacker.getId(),(int)damage,Integer::sum);if(damage>35){if(anger()==0){addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,(int)(damage/15)));addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,(int)(damage/40)));addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,(int)(damage/40)));anger(200);if(source.getEntity() instanceof ServerPlayer p)p.sendSystemMessage(getName().copy().append(" ").append(Component.translatable("tc.boss.enrage")));}damage=35;}}return super.hurt(source,damage);}
 @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recentlyHit){super.dropCustomDeathLoot(level,source,recentlyHit);var item=TC4Modern.SPECIAL_ITEM.get().create(level);item.setPos(getX(),getY()+getBbHeight()/2,getZ());item.setItem(new ItemStack(TaintacleContent.PEARL.get()));level.addFreshEntity(item);spawnAtLocation(new ItemStack(LootBags.ITEMS.get(2).get()),1.5f);}
 @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);if(hasRestriction()){var p=getRestrictCenter();n.putInt("HomeX",p.getX());n.putInt("HomeY",p.getY());n.putInt("HomeZ",p.getZ());n.putInt("HomeD",(int)getRestrictRadius());}}
 @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);if(n.contains("HomeD"))restrictTo(new BlockPos(n.getInt("HomeX"),n.getInt("HomeY"),n.getInt("HomeZ")),n.getInt("HomeD"));}
}
