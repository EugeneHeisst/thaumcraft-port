package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
/** Original twelve-stage Crimson Cult invasion and its destructible portal. */
public final class CultistPortal extends Monster {
 int stage,stagecounter=200;public int pulse;
 private final ServerBossEvent bar=new ServerBossEvent(getName(),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
 public CultistPortal(EntityType<? extends Monster> type,Level level){super(type,level);xpReward=30;}
 public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,500).add(Attributes.ATTACK_DAMAGE,0).add(Attributes.KNOCKBACK_RESISTANCE,1).add(Attributes.ARMOR,5);}
 @Override protected void registerGoals(){}
 @Override public void move(MoverType type,Vec3 motion){}
 @Override public boolean isPushable(){return false;}
 @Override public boolean canBeCollidedWith(){return true;}
 @Override public boolean canBeAffected(net.minecraft.world.effect.MobEffectInstance effect){return false;}
 @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
 @Override protected float getDamageAfterArmorAbsorb(DamageSource source,float amount){return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR)?amount:amount*.8f;}
 @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);bar.addPlayer(p);}
 @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
 @Override public void tick(){super.tick();if(pulse>0)pulse--;if(level().isClientSide)return;
  if(stagecounter>0){--stagecounter;if(stage==0&&stagecounter==160){level().broadcastEntityEvent(this,(byte)16);for(var d:Direction.Plane.HORIZONTAL){var pos=new BlockPos((int)getX()-d.getStepX()*6,(int)getY(),(int)getZ()+d.getStepZ()*6);level().setBlockAndUpdate(pos,CrimsonContent.BANNER.get().defaultBlockState());if(level().getBlockEntity(pos) instanceof CrimsonBanner.Entity banner)banner.setFacing((byte)switch(d){case NORTH->8;case SOUTH->0;case WEST->12;default->4;});playSound(FociContent.sound("wandfail"),1,1);}}
   if(stage==0&&stagecounter>20&&stagecounter<150&&stagecounter%13==0){int x=(int)getX()+random.nextInt(5)-random.nextInt(5),z=(int)getZ()+random.nextInt(5)-random.nextInt(5);var p=new BlockPos(x,(int)getY(),z);if(x!=(int)getX()&&z!=(int)getZ()&&level().isEmptyBlock(p)){level().broadcastEntityEvent(this,(byte)16);float r=random.nextFloat();level().setBlockAndUpdate(p,OuterLoot.CRATES.get(r<.05f?2:r<.2f?1:0).get().defaultBlockState());playSound(FociContent.sound("wandfail"),1,1);}}
  }else if(level().getNearestPlayer(this,48)!=null){level().broadcastEntityEvent(this,(byte)16);if(stage<=4){stagecounter=15+random.nextInt(10-stage)-stage;spawnMinion();}else if(stage==12){stagecounter=50+timing()*2+random.nextInt(50);spawnCultist(OuterBossContent.LEADER.get().create(level()));}else{int t=timing();stagecounter=t+random.nextInt(5+t/3);spawnMinion();}stage++;}else stagecounter=30+random.nextInt(30);
  if(stage<12)heal(1);bar.setProgress(getHealth()/getMaxHealth());
 }
 int timing(){return level().getEntitiesOfClass(CultistEntity.class,getBoundingBox().inflate(32)).size()*20;}
 void spawnMinion(){spawnCultist((random.nextFloat()>.33?CrimsonContent.KNIGHT:CrimsonContent.CLERIC).get().create(level()));if(stage>12)hurt(damageSources().fellOutOfWorld(),5+random.nextInt(5));}
 void spawnCultist(Mob cultist){if(cultist==null)return;cultist.moveTo(getX()+random.nextFloat()-random.nextFloat(),getY()+.25,getZ()+random.nextFloat()-random.nextFloat());var world=(ServerLevel)level();cultist.finalizeSpawn(world,world.getCurrentDifficultyAt(cultist.blockPosition()),MobSpawnType.MOB_SUMMONED,null);cultist.restrictTo(blockPosition(),32);world.addFreshEntity(cultist);cultist.spawnAnim();cultist.playSound(FociContent.sound("wandfail"),1,1);}
 @Override public void playerTouch(net.minecraft.world.entity.player.Player p){if(!level().isClientSide&&distanceToSqr(p)<3&&p.hurt(damageSources().indirectMagic(this,this),8))playSound(FociContent.sound("zap"),1,1+(random.nextFloat()-random.nextFloat())*.1f);}
 @Override public void handleEntityEvent(byte event){if(event==16){pulse=10;spawnAnim();}else super.handleEntityEvent(event);}
 @Override public int getAmbientSoundInterval(){return 540;}
 @Override protected float getSoundVolume(){return .75f;}
 @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return FociContent.sound("monolith");}
 @Override protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source){return FociContent.sound("zap");}
 @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return FociContent.sound("shock");}
 @Override public void die(DamageSource source){if(!level().isClientSide)level().explode(this,getX(),getY(),getZ(),2,Level.ExplosionInteraction.NONE);super.die(source);}
 @Override protected void dropCustomDeathLoot(ServerLevel world,DamageSource source,boolean hit){super.dropCustomDeathLoot(world,source,hit);var item=TC4Modern.SPECIAL_ITEM.get().create(world);item.setPos(getX(),getY()+1.5,getZ());item.setItem(new ItemStack(TaintacleContent.PEARL.get()));world.addFreshEntity(item);}
 @Override public void addAdditionalSaveData(CompoundTag n){super.addAdditionalSaveData(n);n.putInt("stage",stage);}
 @Override public void readAdditionalSaveData(CompoundTag n){super.readAdditionalSaveData(n);stage=n.getInt("stage");stagecounter=200;}
}
