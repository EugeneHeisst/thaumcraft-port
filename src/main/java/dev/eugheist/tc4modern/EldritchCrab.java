package dev.eugheist.tc4modern;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.*;
import net.minecraft.network.syncher.*;
public final class EldritchCrab extends Monster {
 private static final EntityDataAccessor<Boolean> HELM=SynchedEntityData.defineId(EldritchCrab.class,EntityDataSerializers.BOOLEAN);private int rideAttack;
 public EldritchCrab(EntityType<? extends EldritchCrab> t,Level l){super(t,l);xpReward=6;}
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,20).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.3);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(HELM,false);}
 public boolean hasHelm(){return entityData.get(HELM);}
 public void setHelm(boolean b){entityData.set(HELM,b);getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(b?.275:.3);getAttribute(Attributes.ARMOR).setBaseValue(b?5:0);}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new LeapAtTargetGoal(this,.63f));goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,net.minecraft.world.entity.player.Player.class,true));targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,CultistEntity.class,true));}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,net.minecraft.world.DifficultyInstance d,MobSpawnType reason,SpawnGroupData group){setHelm(l.getDifficulty()==net.minecraft.world.Difficulty.HARD||random.nextFloat()<.33f);if(group==null){var spider=new Spider.SpiderEffectsGroupData();if(l.getDifficulty()==net.minecraft.world.Difficulty.HARD&&random.nextFloat()<.1f*d.getSpecialMultiplier())spider.setRandomEffect(random);group=spider;}if(group instanceof Spider.SpiderEffectsGroupData spider&&spider.effect!=null)addEffect(new net.minecraft.world.effect.MobEffectInstance(spider.effect,Integer.MAX_VALUE));return super.finalizeSpawn(l,d,reason,group);}
 @Override public void tick(){super.tick();if(tickCount<20)fallDistance=0;if(!level().isClientSide){var target=getTarget();if(!isPassenger()&&target!=null&&target.getPassengers().isEmpty()&&!onGround()&&!hasHelm()&&target.isAlive()&&getY()-target.getY()>=target.getBbHeight()/2&&distanceToSqr(target)<4)startRiding(target,true);if(isPassenger()&&--rideAttack<=0){rideAttack=10+random.nextInt(10);var vehicle=getVehicle();doHurtTarget(vehicle);if(random.nextFloat()<.2f&&vehicle instanceof LivingEntity living)living.knockback(.4,Math.sin(getYRot()*Math.PI/180),-Math.cos(getYRot()*Math.PI/180));}}}
 @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float amount){boolean hit=super.hurt(source,amount);if(!level().isClientSide&&hasHelm()&&getHealth()/getMaxHealth()<=.5f){setHelm(false);spawnAtLocation(PechTrades.mod("cultist_plate_chestplate"));}return hit;}
 @Override public boolean doHurtTarget(Entity target){if(super.doHurtTarget(target)){playSound(CrabContent.CLAW.get(),1,.9f+random.nextFloat()*.2f);return true;}return false;}
 @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect){return !effect.is(net.minecraft.world.effect.MobEffects.POISON)&&super.canBeAffected(effect);}
 @Override public boolean isAlliedTo(Entity e){return e instanceof EldritchCrab||super.isAlliedTo(e);}
 @Override public boolean canBeLeashed(){return false;}
 @Override public int getAmbientSoundInterval(){return 160;}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return CrabContent.TALK.get();}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s){return net.minecraft.sounds.SoundEvents.HOSTILE_HURT;}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return CrabContent.DEATH.get();}
 @Override protected void playStepSound(net.minecraft.core.BlockPos p,net.minecraft.world.level.block.state.BlockState state){playSound(net.minecraft.sounds.SoundEvents.SPIDER_STEP,.15f,1);}
 @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel l,net.minecraft.world.damagesource.DamageSource source,boolean recent){int looting=source.getEntity() instanceof LivingEntity living?net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(l.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),living):0;if(recent&&(random.nextInt(3)==0||random.nextInt(1+looting)>0))spawnAtLocation(net.minecraft.world.item.Items.ENDER_PEARL);}
 @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("Flags",(byte)(hasHelm()?1:0));}
 @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);setHelm((tag.getByte("Flags")&1)!=0);}
}
