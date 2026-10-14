package dev.eugheist.tc4modern;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.level.*;
public final class InhabitedZombie extends Zombie {
 public InhabitedZombie(EntityType<? extends InhabitedZombie> t,Level l){super(t,l);}
 public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){return Zombie.createAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,30).add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,5).add(net.minecraft.world.entity.ai.attributes.Attributes.SPAWN_REINFORCEMENTS_CHANCE,0);}
 @Override protected void addBehaviourGoals(){super.addBehaviourGoals();targetSelector.addGoal(3,new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(this,CultistEntity.class,true));}
 @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor l,net.minecraft.world.DifficultyInstance d,MobSpawnType reason,SpawnGroupData group){float chance=l.getDifficulty()==net.minecraft.world.Difficulty.HARD?.9f:.6f;setItemSlot(EquipmentSlot.HEAD,PechTrades.mod("cultist_plate_helmet"));if(random.nextFloat()<=chance)setItemSlot(EquipmentSlot.CHEST,PechTrades.mod("cultist_plate_chestplate"));if(random.nextFloat()<=chance)setItemSlot(EquipmentSlot.LEGS,PechTrades.mod("cultist_plate_leggings"));return group;}
 @Override public boolean killedEntity(net.minecraft.server.level.ServerLevel l,LivingEntity killed){return true;}
 @Override public void die(net.minecraft.world.damagesource.DamageSource source){}
 @Override protected void tickDeath(){if(!level().isClientSide){var crab=CrabContent.CRAB.get().create(level());crab.moveTo(getX(),getY()+getEyeHeight(),getZ(),getYRot(),getXRot());crab.setHelm(true);level().addFreshEntity(crab);if((lastHurtByPlayerTime>0||isAlwaysExperienceDropper())&&shouldDropExperience()&&level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT))ExperienceOrb.award((net.minecraft.server.level.ServerLevel)level(),position(),getExperienceReward((net.minecraft.server.level.ServerLevel)level(),lastHurtByPlayer));((net.minecraft.server.level.ServerLevel)level()).sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,getX(),getY()+getBbHeight()/2,getZ(),20,getBbWidth(),getBbHeight()/2,getBbWidth(),.02);}discard();}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return CrabContent.TALK.get();}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource s){return net.minecraft.sounds.SoundEvents.HOSTILE_HURT;}
 @Override public boolean checkSpawnRules(LevelAccessor l,MobSpawnType reason){return level().getEntitiesOfClass(InhabitedZombie.class,getBoundingBox().inflate(32,16,32),e->e!=this).isEmpty()&&super.checkSpawnRules(l,reason);}
}
