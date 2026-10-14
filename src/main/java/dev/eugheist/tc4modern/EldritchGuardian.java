package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import thaumcraft.api.aspects.Aspect;
/** Original EntityEldritchGuardian, spawned by warp mist without spawn finalization. */
public final class EldritchGuardian extends Monster implements RangedAttackMob, thaumcraft.api.entities.IEldritchMob {
 public float armLiftL,armLiftR;boolean lastBlast;
 public EldritchGuardian(EntityType<? extends EldritchGuardian> type,Level level){super(type,level);xpReward=20;getNavigation().setCanFloat(true);}
 static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,50).add(Attributes.FOLLOW_RANGE,40).add(Attributes.MOVEMENT_SPEED,.28).add(Attributes.ATTACK_DAMAGE,7).add(Attributes.ARMOR,4);}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new LongRange(this));goalSelector.addGoal(3,new MeleeAttackGoal(this,1,false));goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,.8));goalSelector.addGoal(7,new RandomStrollGoal(this,1));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,0,true,false,null));targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,CultistEntity.class,0,true,false,null));}
 @Override public boolean checkSpawnRules(net.minecraft.world.level.LevelAccessor l,MobSpawnType reason){return level().getEntitiesOfClass(EldritchGuardian.class,getBoundingBox().inflate(32,16,32),e->e!=this).isEmpty()&&super.checkSpawnRules(l,reason);}
 static final class LongRange extends RangedAttackGoal {final EldritchGuardian mob;LongRange(EldritchGuardian mob){super(mob,1,20,40,24);this.mob=mob;}@Override public boolean canUse(){if(!super.canUse())return false;var t=mob.getTarget();if(t==null)return false;if(!t.isAlive()){mob.setTarget(null);return false;}return mob.distanceToSqr(t.getX(),t.getBoundingBox().minY,t.getZ())>=64;}}
 @Override public boolean hurt(DamageSource source,float amount){return super.hurt(source,FortressArmor.magic(source)?amount/2:amount);}
 @Override protected float getDamageAfterArmorAbsorb(DamageSource source,float amount){return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)?amount:amount*.84f;}
 @Override public boolean doHurtTarget(Entity target){boolean hit=super.doHurtTarget(target);int difficulty=level().getDifficulty().getId();if(hit&&getMainHandItem().isEmpty()&&isOnFire()&&random.nextFloat()<difficulty*.3f)target.igniteForSeconds(2*difficulty);return hit;}
 @Override public void tick(){super.tick();if(level().isClientSide){if(armLiftL>0)armLiftL-=.05f;if(armLiftR>0)armLiftR-=.05f;GuardianClient.wisp(this);}else if(tickCount%100==0&&level().getDifficulty()!=Difficulty.EASY){double range=level().getDifficulty()==Difficulty.HARD?576:256;for(var p:((ServerLevel)level()).players())if(p.isAlive()&&distanceToSqr(p)<range)PacketDistributor.sendToPlayer(p,new WarpMiscPayload(2));}}
 @Override public void handleEntityEvent(byte event){if(event==15)armLiftL=.5f;else if(event==16)armLiftR=.5f;else if(event==17)armLiftL=armLiftR=.9f;else super.handleEntityEvent(event);}
 static SoundEvent sound(String name){return SoundEvent.createVariableRangeEvent(AlchemyContent.id(name));}
 @Override protected SoundEvent getAmbientSound(){return sound("egidle");}
 @Override protected SoundEvent getDeathSound(){return sound("egdeath");}
 @Override public int getAmbientSoundInterval(){return 500;}
 @Override protected float getSoundVolume(){return 1.5f;}
 @Override public boolean removeWhenFarAway(double distance){return !hasRestriction();}
 @Override public boolean requiresCustomPersistence(){return hasRestriction()||super.requiresCustomPersistence();}
 @Override public boolean canBeLeashed(){return false;}
 @Override public boolean isAlliedTo(Entity other){return other instanceof EldritchGuardian||super.isAlliedTo(other);}
 @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(hasRestriction()){var p=getRestrictCenter();tag.putInt("HomeD",(int)getRestrictRadius());tag.putInt("HomeX",p.getX());tag.putInt("HomeY",p.getY());tag.putInt("HomeZ",p.getZ());}}
 @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.contains("HomeD"))restrictTo(new BlockPos(tag.getInt("HomeX"),tag.getInt("HomeY"),tag.getInt("HomeZ")),tag.getInt("HomeD"));}
 @Override protected void dropCustomDeathLoot(ServerLevel level,DamageSource source,boolean recent){if(random.nextBoolean())spawnAtLocation(WispEssenceItem.of(Aspect.UNDEAD,2),1);if(random.nextBoolean())spawnAtLocation(WispEssenceItem.of(Aspect.ELDRITCH,2),1);super.dropCustomDeathLoot(level,source,recent);int looting=source.getEntity() instanceof LivingEntity killer?net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING),killer):0;if(recent&&random.nextInt(200)-looting<5)spawnAtLocation(WarpEntities.ELDRITCH_EYE.get());}
 @Override public void performRangedAttack(LivingEntity target,float distance){if(random.nextFloat()>.1f){var orb=new EldritchOrb(level(),this);lastBlast=!lastBlast;level().broadcastEntityEvent(this,lastBlast?(byte)16:(byte)15);int rr=lastBlast?90:180;float angle=(getYRot()+rr)%360/180f*(float)Math.PI;orb.setPos(orb.getX()-Mth.cos(angle)*.5,orb.getY()-.057777777*getBbHeight(),orb.getZ()-Mth.sin(angle)*.5);orb.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,target.getZ()+target.getDeltaMovement().z-getZ(),1,2);playSound(sound("egattack"),2,1+random.nextFloat()*.1f);level().addFreshEntity(orb);}else if(hasLineOfSight(target)){PacketDistributor.sendToPlayersNear((ServerLevel)level(),null,getX(),getY(),getZ(),32,new GuardianSonicPayload(getId()));target.addEffect(new MobEffectInstance(MobEffects.WITHER,400));if(target instanceof ServerPlayer p)PlayerWarp.add(p,WarpState.Kind.TEMPORARY,1+level().random.nextInt(3));playSound(sound("egscreech"),3,1+random.nextFloat()*.1f);}}
}
