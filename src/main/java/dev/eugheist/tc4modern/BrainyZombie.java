package dev.eugheist.tc4modern;

import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** TC4 brainy zombies; giant anger changes collision size and attack strength independently of render scale. */
public class BrainyZombie extends Zombie {
 private static final EntityDataAccessor<Float> ANGER=SynchedEntityData.defineId(BrainyZombie.class,EntityDataSerializers.FLOAT);
 public BrainyZombie(EntityType<? extends BrainyZombie> type,Level level){super(type,level);if(giant()){xpReward=15;goalSelector.addGoal(2,new LeapAtTargetGoal(this,.4f));}refreshDimensions();}
 @Override protected void addBehaviourGoals(){super.addBehaviourGoals();targetSelector.getAvailableGoals().stream().map(net.minecraft.world.entity.ai.goal.WrappedGoal::getGoal).filter(g->g instanceof net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal).toList().forEach(targetSelector::removeGoal);targetSelector.addGoal(1,new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this));}
 public boolean giant(){return getType()==BrainyContent.GIANT.get();}
 public static boolean canSpawn(EntityType<? extends net.minecraft.world.entity.monster.Monster> type,net.minecraft.world.level.ServerLevelAccessor level,MobSpawnType reason,net.minecraft.core.BlockPos pos,net.minecraft.util.RandomSource random){return level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&level.getBrightness(net.minecraft.world.level.LightLayer.SKY,pos)<=random.nextInt(32)&&level.getMaxLocalRawBrightness(pos)<=random.nextInt(8)&&Mob.checkMobSpawnRules(type,level,reason,pos,random);}
 public float anger(){return entityData.get(ANGER);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ANGER,1f);}
 public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(boolean giant){return Zombie.createAttributes().add(Attributes.MAX_HEALTH,giant?60:25).add(Attributes.ATTACK_DAMAGE,giant?7:5).add(Attributes.ARMOR,5).add(Attributes.SPAWN_REINFORCEMENTS_CHANCE,0);}
 @Override public EntityDimensions getDefaultDimensions(Pose pose){return giant()?EntityDimensions.scalable(.6f*(1.2f+anger()),1.8f*(1.2f+anger())):super.getDefaultDimensions(pose);}
 @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key.equals(ANGER))refreshDimensions();}
 @Override public void aiStep(){super.aiStep();if(giant()&&!level().isClientSide){if(anger()>1)entityData.set(ANGER,anger()-.002f);getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(7+(anger()-1)*5);}}
 @Override protected float getDamageAfterArmorAbsorb(DamageSource source,float amount){return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)?amount:amount*(1-Math.min(20,getArmorValue())/25f);}
 @Override public boolean hurt(DamageSource source,float amount){if(giant()&&!level().isClientSide)entityData.set(ANGER,Math.min(2,anger()+.1f));return super.hurt(source,amount);}
 @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent){int looting=Champions.looting(this,source);for(int i=0;i<(giant()?12:3);i++)if(random.nextBoolean())spawnAtLocation(Items.ROTTEN_FLESH,giant()?2:1);if(random.nextInt(10)-looting<=4)spawnAtLocation(PechTrades.mod("zombie_brain"),giant()?2f:1.5f);if(recent&&random.nextInt(200)-looting<5){if(giant())switch(random.nextInt(4)){case 0->spawnAtLocation(PechTrades.mod("thaumium_ingot"),2);case 1->spawnAtLocation(Items.CARROT);case 2->spawnAtLocation(Items.POTATO);default->spawnAtLocation(PechTrades.mod("amber"),2);}else switch(random.nextInt(3)){case 0->spawnAtLocation(Items.IRON_INGOT);case 1->spawnAtLocation(Items.CARROT);default->spawnAtLocation(Items.POTATO);}}}
}
