package dev.eugheist.tc4modern;
import net.minecraft.world.entity.*;import net.minecraft.world.entity.projectile.ThrowableProjectile;import net.minecraft.world.level.Level;import net.minecraft.world.effect.*;import net.minecraft.network.syncher.SynchedEntityData;
public final class PechBlast extends ThrowableProjectile {
 public int strength,duration;public boolean nightshade;
 public PechBlast(EntityType<? extends PechBlast> type,Level level){super(type,level);}
 public PechBlast(Level level,LivingEntity owner,int strength,int duration,boolean nightshade){super(PechContent.BLAST.get(),owner,level);this.strength=strength;this.duration=duration;this.nightshade=nightshade;}
 @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
 @Override protected double getDefaultGravity(){return .025;}
 @Override public void tick(){super.tick();if(tickCount>500)discard();if(level().isClientSide)for(int i=0;i<3;i++)level().addParticle(net.minecraft.core.particles.ParticleTypes.WITCH,getX(),getY(),getZ(),0,0,0);}
 @Override protected void onHit(net.minecraft.world.phys.HitResult hit){super.onHit(hit);if(level().isClientSide)return;for(var entity:level().getEntities(getOwner(),getBoundingBox().inflate(2))){if(entity instanceof LivingEntity target&&!(entity instanceof PechEntity)){target.hurt(damageSources().indirectMagic(this,getOwner()),strength+2);int effect=random.nextInt(3);if(nightshade||effect==0)target.addEffect(new MobEffectInstance(MobEffects.POISON,100+duration*40,strength));if(nightshade||effect==1)target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,100+duration*40,strength+1));if(nightshade||effect==2)target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,100+duration*40,strength));}}level().broadcastEntityEvent(this,(byte)3);discard();}
 @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Strength",strength);tag.putInt("Duration",duration);tag.putBoolean("Nightshade",nightshade);}
 @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);strength=tag.getInt("Strength");duration=tag.getInt("Duration");nightshade=tag.getBoolean("Nightshade");}
}
