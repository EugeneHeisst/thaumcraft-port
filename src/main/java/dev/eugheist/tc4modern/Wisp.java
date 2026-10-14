package dev.eugheist.tc4modern;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.*;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import thaumcraft.api.aspects.Aspect;
/** EntityWisp's original roaming flight, aspect selection, retaliation and probabilistic lightning. */
public final class Wisp extends FlyingMob implements net.minecraft.world.entity.monster.Enemy {
 private static final EntityDataAccessor<String> ASPECT=SynchedEntityData.defineId(Wisp.class,EntityDataSerializers.STRING);
 static final EntityDataAccessor<Integer> ZAP_TARGET=SynchedEntityData.defineId(Wisp.class,EntityDataSerializers.INT),ZAP_SERIAL=SynchedEntityData.defineId(Wisp.class,EntityDataSerializers.INT);
 public static java.util.function.Consumer<Wisp> fx=e->{};
 public double waypointX,waypointY,waypointZ;public int courseChangeCooldown,attackCounter,aggroCooldown,zapSeen;
 public Wisp(EntityType<? extends Wisp> t,Level l){super(t,l);xpReward=5;}
 public Aspect aspect(){return Aspect.getAspect(entityData.get(ASPECT));}
 public void aspect(Aspect a){entityData.set(ASPECT,a.getTag());}
 public static AttributeSupplier.Builder attributes(){return Mob.createMobAttributes().add(Attributes.MAX_HEALTH,22).add(Attributes.ATTACK_DAMAGE,3);}
 @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ASPECT,"");b.define(ZAP_TARGET,-1);b.define(ZAP_SERIAL,0);}
 @Override public int getMaxSpawnClusterSize(){return 2;}
 @Override protected boolean shouldDespawnInPeaceful(){return true;}
 @Override protected int decreaseAirSupply(int air){return air;}
 @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource s,float amount){if(s.getEntity() instanceof LivingEntity target){setTarget(target);aggroCooldown=200;}else if(s.getDirectEntity() instanceof LivingEntity target){setTarget(target);aggroCooldown=200;}return super.hurt(s,amount);}
 @Override public void tick(){super.tick();if(level().isClientSide)fx.accept(this);}
 @Override protected void customServerAiStep(){
  if(aspect()==null){var biome=level().getBiome(blockPosition());java.util.List<Aspect> choices;if(biome.is(AlchemyContent.id("eerie")))choices=java.util.List.of(Aspect.DARKNESS,Aspect.UNDEAD,Aspect.ENTROPY,Aspect.ELDRITCH,Aspect.POISON,Aspect.DEATH);else choices=random.nextInt(10)!=0?Aspect.getPrimalAspects():Aspect.getCompoundAspects();aspect(choices.get(random.nextInt(choices.size())));}
  Vec3 delta=new Vec3(waypointX-getX(),waypointY-getY(),waypointZ-getZ());double distance=delta.lengthSqr();
  if(distance<1||distance>3600){waypointX=getX()+(random.nextFloat()*2-1)*16;waypointY=getY()+(random.nextFloat()*2-1)*16;waypointZ=getZ()+(random.nextFloat()*2-1)*16;delta=new Vec3(waypointX-getX(),waypointY-getY(),waypointZ-getZ());distance=delta.lengthSqr();}
  if(courseChangeCooldown--<=0){courseChangeCooldown+=random.nextInt(5)+2;if(traversable(new Vec3(waypointX,waypointY,waypointZ)))setDeltaMovement(getDeltaMovement().add(delta.normalize().scale(.1)));else {waypointX=getX();waypointY=getY();waypointZ=getZ();}}
  if(getTarget()!=null&&!getTarget().isAlive())setTarget(null);--aggroCooldown;
  if(random.nextInt(1000)==0&&(getTarget()==null||aggroCooldown--<=0)){var p=level().getNearestPlayer(this,16);if(p!=null&&!p.isCreative()&&!p.isSpectator()){setTarget(p);aggroCooldown=50;}}
  var target=getTarget();if(target!=null&&distanceToSqr(target)<256){setYRot(-(float)Math.atan2(target.getX()-getX(),target.getZ()-getZ())*180/(float)Math.PI);yBodyRot=getYRot();if(hasLineOfSight(target)){if(++attackCounter==20){playSound(FociContent.sound("zap"),1,1.1f);entityData.set(ZAP_TARGET,target.getId());entityData.set(ZAP_SERIAL,entityData.get(ZAP_SERIAL)+1);var velocity=target.getDeltaMovement();boolean moving=Math.abs(velocity.x)>.1||Math.abs(velocity.y)>.1||Math.abs(velocity.z)>.1;if(random.nextFloat()<(moving?.4f:.66f))target.hurt(damageSources().mobAttack(this),(float)getAttributeValue(Attributes.ATTACK_DAMAGE)+(moving?0:1));attackCounter=-20+random.nextInt(20);}}else if(attackCounter>0)attackCounter--;}
  else {setYRot(-(float)Math.atan2(getDeltaMovement().x,getDeltaMovement().z)*180/(float)Math.PI);yBodyRot=getYRot();if(attackCounter>0)attackCounter--;}
 }
 public boolean traversable(Vec3 end){var delta=end.subtract(position());double length=delta.length();var step=delta.normalize();var box=getBoundingBox();for(int i=1;i<length;i++){box=box.move(step);if(!level().noCollision(this,box))return false;}var p=BlockPos.containing((int)end.x,(int)end.y,(int)end.z);if(!level().getFluidState(p).isEmpty())return false;for(int i=0;i<11;i++)if(!level().isEmptyBlock(p.below(i)))return true;return false;}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return WispContent.LIVE.get();}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s){return net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH;}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return WispContent.DEAD.get();}
 @Override protected float getSoundVolume(){return .25f;}
 @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel l,net.minecraft.world.damagesource.DamageSource source,boolean recent){if(aspect()!=null)spawnAtLocation(WispEssenceItem.of(aspect(),2));}
 @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putString("Type",entityData.get(ASPECT));}
 @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(ASPECT,tag.getString("Type"));}
}
