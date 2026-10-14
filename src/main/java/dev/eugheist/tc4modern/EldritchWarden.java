package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.sounds.*;
import net.minecraft.util.Mth;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.*;
public final class EldritchWarden extends ThaumcraftBoss implements RangedAttackMob,thaumcraft.api.entities.IEldritchMob {
 static final String[] TITLES={"Aphoom-Zhah","Basatan","Chaugnar Faugn","Mnomquah","Nyogtha","Oorn","Shaikorth","Rhan-Tegoth","Rhogog","Shudde M'ell","Vulthoom","Yag-Kosha","Yibb-Tstll","Zathog","Zushakon"};
 private static final EntityDataAccessor<Integer> TITLE=SynchedEntityData.defineId(EldritchWarden.class,EntityDataSerializers.INT);
 boolean fieldFrenzy,lastBlast;int fieldFrenzyCounter;public float armLiftL,armLiftR;
 public EldritchWarden(EntityType<? extends EldritchWarden> type,Level l){super(type,l);getNavigation().setCanFloat(true);}
 public static AttributeSupplier.Builder attributes(){return ThaumcraftBoss.attributes().add(Attributes.MAX_HEALTH,200).add(Attributes.MOVEMENT_SPEED,.33).add(Attributes.ATTACK_DAMAGE,10).add(Attributes.ARMOR,4).add(Attributes.MAX_ABSORPTION,132);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(TITLE,0);}
 String title(){return TITLES[Mth.clamp(entityData.get(TITLE),0,TITLES.length-1)];}
 @Override protected net.minecraft.network.chat.Component getTypeName(){return net.minecraft.network.chat.Component.translatable(getType().getDescriptionId(),title(),championTitle());}
 @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new LongRange(this));goalSelector.addGoal(3,new MeleeAttackGoal(this,1.1,false));goalSelector.addGoal(5,new MoveTowardsRestrictionGoal(this,.8));goalSelector.addGoal(7,new RandomStrollGoal(this,1));goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8));goalSelector.addGoal(8,new RandomLookAroundGoal(this));targetSelector.addGoal(1,new HurtByTargetGoal(this));targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,Player.class,true));targetSelector.addGoal(3,new NearestAttackableTargetGoal<>(this,CultistEntity.class,true));}
 private static final class LongRange extends RangedAttackGoal {final EldritchWarden mob;LongRange(EldritchWarden m){super(m,1,20,40,24);mob=m;}@Override public boolean canUse(){return super.canUse()&&mob.getTarget()!=null&&mob.getTarget().isAlive()&&mob.distanceToSqr(mob.getTarget())>=9;}}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor w,DifficultyInstance d,MobSpawnType reason,SpawnGroupData data){spawnTimer=150;entityData.set(TITLE,random.nextInt(TITLES.length));setAbsorptionAmount(getAbsorptionAmount()+(float)getAttributeBaseValue(Attributes.MAX_HEALTH)*.66f);return super.finalizeSpawn(w,d,reason,data);}
 @Override public boolean isEffectiveAi(){return fieldFrenzyCounter<=0&&super.isEffectiveAi();}
 @Override public boolean isInvulnerableTo(DamageSource s){return fieldFrenzyCounter>0||super.isInvulnerableTo(s);}
 @Override protected float getDamageAfterArmorAbsorb(DamageSource source,float amount){return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)?amount:amount*(1-Math.min(20,getArmorValue())/25f);}
 @Override public boolean hurt(DamageSource source,float amount){if(isInvulnerableTo(source)||source.is(DamageTypes.IN_WALL)||source.is(DamageTypes.WITHER))return false;boolean hit=super.hurt(source,amount);if(!level().isClientSide&&hit&&!fieldFrenzy&&getAbsorptionAmount()<=0){fieldFrenzy=true;fieldFrenzyCounter=150;}return hit;}
 @Override public void tick(){if(spawnTimer==150)level().broadcastEntityEvent(this,(byte)18);super.tick();if(level().isClientSide){armLiftL=Math.max(0,armLiftL-.05f);armLiftR=Math.max(0,armLiftR-.05f);OuterBossClient.wardenEffects(this);return;}
  if(invulnerableTime<=0&&tickCount%25==0&&getAbsorptionAmount()<(int)(getAttributeBaseValue(Attributes.MAX_HEALTH)*.66))setAbsorptionAmount(getAbsorptionAmount()+1);
  for(int i=0;i<4;i++){var p=BlockPos.containing(getX()+(i%2*2-1)*.25,getY(),getZ()+(i/2%2*2-1)*.25);if(level().isEmptyBlock(p))level().setBlock(p,OuterHazard.BLOCK.get().defaultBlockState(),3);}
  if(fieldFrenzyCounter>0){if(fieldFrenzyCounter==150)teleportHome();performFieldFrenzy();}
 }
 void performFieldFrenzy(){if(fieldFrenzyCounter<121&&fieldFrenzyCounter%10==0){level().broadcastEntityEvent(this,(byte)17);double radius=(150-fieldFrenzyCounter)/8.0;int spacing=1+fieldFrenzyCounter/8;var base=blockPosition();for(int i=0;i<180/spacing;i++){double angle=Math.toRadians(i*2*spacing);var p=base.offset((int)(radius*Math.cos(angle)),0,(int)(radius*Math.sin(angle)));if(!level().isEmptyBlock(p)||!level().getBlockState(p.below()).isCollisionShapeFullBlock(level(),p.below()))continue;level().setBlock(p,OuterHazard.BLOCK.get().defaultBlockState(),3);level().scheduleTick(p,OuterHazard.BLOCK.get(),250+random.nextInt(150));((ServerLevel)level()).sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(.5f,0,.5f),1),p.getX()+.5,p.getY()+.5,p.getZ()+.5,4,.3,.3,.3,0);}playSound(FociContent.sound("zap"),1,.9f+random.nextFloat()*.1f);}fieldFrenzyCounter--;}
 void teleportHome(){if(!hasRestriction())return;var event=new net.neoforged.neoforge.event.entity.EntityTeleportEvent.EnderEntity(this,getRestrictCenter().getX(),getRestrictCenter().getY(),getRestrictCenter().getZ());if(net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled())return;var old=position();var target=BlockPos.containing(event.getTargetX(),event.getTargetY(),event.getTargetZ());if(!level().hasChunkAt(target))return;boolean success=false;for(int i=0;i<20;i++){var p=i==0?target:target.offset(random.nextInt(8)-random.nextInt(8),0,random.nextInt(8)-random.nextInt(8));if(!level().getBlockState(p.below()).blocksMotion()||level().getBlockState(p).blocksMotion())continue;setPos(p.getX()+.5,p.getY()+.1,p.getZ()+.5);if(level().noCollision(this)){success=true;break;}}if(!success){setPos(old);return;}
  var server=(ServerLevel)level();for(int i=0;i<128;i++){double fraction=i/127.0;var at=old.lerp(position(),fraction);server.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,at.x+(random.nextDouble()-.5)*getBbWidth()*2,at.y+random.nextDouble()*getBbHeight(),at.z+(random.nextDouble()-.5)*getBbWidth()*2,1,(random.nextFloat()-.5)*.2,(random.nextFloat()-.5)*.2,(random.nextFloat()-.5)*.2,0);}level().playSound(null,old.x,old.y,old.z,SoundEvents.ENDERMAN_TELEPORT,SoundSource.HOSTILE,1,1);playSound(SoundEvents.ENDERMAN_TELEPORT,1,1);
 }
 @Override public void performRangedAttack(LivingEntity target,float distance){if(random.nextFloat()>.2f){var orb=new EldritchOrb(level(),this);lastBlast=!lastBlast;level().broadcastEntityEvent(this,lastBlast?(byte)16:(byte)15);int rr=lastBlast?90:180;float angle=(getYRot()+rr)%360*Mth.DEG_TO_RAD;orb.setPos(orb.getX()-Mth.cos(angle)*.5,orb.getY()-.13,orb.getZ()-Mth.sin(angle)*.5);orb.shoot(target.getX()+target.getDeltaMovement().x-getX(),target.getY()-getY()-target.getBbHeight()/2,target.getZ()+target.getDeltaMovement().z-getZ(),1,2);playSound(FociContent.sound("egattack"),2,1+random.nextFloat()*.1f);level().addFreshEntity(orb);}else if(hasLineOfSight(target)){net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear((ServerLevel)level(),null,getX(),getY(),getZ(),32,new GuardianSonicPayload(getId()));target.push(-Mth.sin(getYRot()*Mth.DEG_TO_RAD)*1.5,.1,Mth.cos(getYRot()*Mth.DEG_TO_RAD)*1.5);target.addEffect(new MobEffectInstance(MobEffects.WITHER,400));target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,400));if(target instanceof ServerPlayer p)PlayerWarp.add(p,WarpState.Kind.TEMPORARY,3+random.nextInt(3));playSound(FociContent.sound("egscreech"),4,1+random.nextFloat()*.1f);}}
 @Override public void handleEntityEvent(byte id){if(id==15)armLiftL=.5f;else if(id==16)armLiftR=.5f;else if(id==17)armLiftL=armLiftR=.9f;else if(id==18)spawnTimer=150;else super.handleEntityEvent(id);}
 @Override protected SoundEvent getAmbientSound(){return FociContent.sound("egidle");}@Override protected SoundEvent getDeathSound(){return FociContent.sound("egdeath");}@Override public int getAmbientSoundInterval(){return 500;}
 @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.putByte("title",(byte)(int)entityData.get(TITLE));}@Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);entityData.set(TITLE,Mth.clamp(n.getByte("title"),0,TITLES.length-1));}
}
