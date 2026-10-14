package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
/** Original two-stage ancient golem: melee shell, then recharging homing-orb cannon. */
public final class EldritchGolem extends ThaumcraftBoss implements RangedAttackMob,thaumcraft.api.entities.IEldritchMob {
 private static final EntityDataAccessor<Boolean> HEADLESS=SynchedEntityData.defineId(EldritchGolem.class,EntityDataSerializers.BOOLEAN);
 int beamCharge,attackTimer,arcing;boolean chargingBeam;BlockPos arcTarget;
 public EldritchGolem(EntityType<? extends EldritchGolem> type,Level level){super(type,level);}
 public static AttributeSupplier.Builder attributes(){return ThaumcraftBoss.attributes().add(Attributes.MAX_HEALTH,250).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.ARMOR,6);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(HEADLESS,false);}
 @Override public EntityDimensions getDefaultDimensions(Pose pose){return super.getDefaultDimensions(pose).withEyeHeight(isHeadless()?3.33f:3);}
 @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(key.equals(HEADLESS))refreshDimensions();}
 public boolean isHeadless(){return entityData.get(HEADLESS);}
 void makeHeadless(){entityData.set(HEADLESS,true);goalSelector.addGoal(2,new LongRange(this));}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));goalSelector.addGoal(6,new MoveTowardsRestrictionGoal(this,.8));goalSelector.addGoal(7,new RandomStrollGoal(this,.8));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this).setAlertOthers());targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));}
 private static final class LongRange extends RangedAttackGoal {final EldritchGolem mob;LongRange(EldritchGolem m){super(m,1,5,5,24);mob=m;}@Override public boolean canUse(){return super.canUse()&&mob.getTarget()!=null&&mob.getTarget().isAlive()&&mob.distanceToSqr(mob.getTarget())>=9;}}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor world,DifficultyInstance difficulty,MobSpawnType reason,SpawnGroupData data){spawnTimer=100;return super.finalizeSpawn(world,difficulty,reason,data);}
 @Override protected float getDamageAfterArmorAbsorb(DamageSource source,float amount){return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)?amount:amount*(1-Math.min(20,getArmorValue())/25f);}
 @Override public boolean hurt(DamageSource source,float amount){if(!level().isClientSide&&amount>getHealth()&&!isHeadless()){makeHeadless();spawnTimer=100;float angle=getYRot()%360*Mth.DEG_TO_RAD;level().explode(this,getX()+Mth.cos(angle)*.75,getY()+getEyeHeight(),getZ()+Mth.sin(angle)*.75,2,Level.ExplosionInteraction.NONE);return false;}return super.hurt(source,amount);}
 @Override public boolean doHurtTarget(Entity target){if(attackTimer>0)return false;attackTimer=10;level().broadcastEntityEvent(this,(byte)4);boolean hit=target.hurt(damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)*.75f);if(hit){target.setDeltaMovement(target.getDeltaMovement().add(0,.2,0));if(isHeadless())target.push(-Mth.sin(getYRot()*Mth.DEG_TO_RAD)*1.5,.1,Mth.cos(getYRot()*Mth.DEG_TO_RAD)*1.5);}return hit;}
 @Override public void performRangedAttack(LivingEntity target,float distance){if(!hasLineOfSight(target)||chargingBeam||beamCharge<=0)return;beamCharge-=15+random.nextInt(5);getLookControl().setLookAt(target.getX(),target.getY()+target.getBbHeight()/2,target.getZ(),30,30);var orb=new GolemOrb(level(),this,target,false);var look=getLookAngle();orb.setPos(orb.getX()+look.x,orb.getY(),orb.getZ()+look.z);orb.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,target.getZ()+target.getDeltaMovement().z-getZ(),.66f,5);playSound(FociContent.sound("egattack"),1,1+random.nextFloat()*.1f);level().addFreshEntity(orb);}
 @Override public void aiStep(){super.aiStep();if(attackTimer>0)attackTimer--;var v=getDeltaMovement();if(v.x*v.x+v.z*v.z>2.500000277905201E-7&&random.nextInt(5)==0){var p=BlockPos.containing(getX(),getY()-.2,getZ());var s=level().getBlockState(p);if(!s.isAir())level().addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,s),getRandomX(1),getBoundingBox().minY+.1,getRandomZ(1),4*(random.nextFloat()-.5),.5,4*(random.nextFloat()-.5));if(!level().isClientSide&&s.getBlock() instanceof OuterLoot.Container)level().destroyBlock(p,true,this);}
  if(!level().isClientSide){var p=BlockPos.containing(getX()+v.x,getBoundingBox().minY,getZ()+v.z);float hard=level().getBlockState(p).getDestroySpeed(level(),p);if(hard>=0&&hard<=.15)level().destroyBlock(p,true,this);}
 }
 @Override public void tick(){if(spawnTimer==150)level().broadcastEntityEvent(this,(byte)18);if(spawnTimer>0)heal(2);super.tick();if(level().isClientSide){if(isHeadless())OuterBossClient.golemEffects(this);return;}if(isHeadless()&&beamCharge<=0)chargingBeam=true;if(isHeadless()&&chargingBeam){beamCharge++;level().broadcastEntityEvent(this,(byte)19);if(beamCharge==150)chargingBeam=false;}}
 @Override public void handleEntityEvent(byte event){if(event==4){attackTimer=10;playSound(SoundEvents.IRON_GOLEM_ATTACK,1,1);}else if(event==18)spawnTimer=150;else if(event==19)OuterBossClient.golemArc(this);else super.handleEntityEvent(event);}
 @Override protected SoundEvent getHurtSound(DamageSource s){return SoundEvents.IRON_GOLEM_HURT;}
 @Override protected SoundEvent getDeathSound(){return SoundEvents.IRON_GOLEM_DEATH;}
 @Override protected void playStepSound(BlockPos p,net.minecraft.world.level.block.state.BlockState s){playSound(SoundEvents.IRON_GOLEM_STEP,1,1);}
 @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.putBoolean("headless",isHeadless());}
 @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);if(n.getBoolean("headless"))makeHeadless();}
}
