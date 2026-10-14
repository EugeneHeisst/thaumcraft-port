package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.sounds.SoundEvent;

/** Stationary original taintacle family. Height determines reach, emergence and sound. */
public class Taintacle extends Monster implements thaumcraft.api.entities.ITaintedMob {
    private static final EntityDataAccessor<Integer> ANGER=SynchedEntityData.defineId(Taintacle.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> AGITATED=SynchedEntityData.defineId(Taintacle.class,EntityDataSerializers.BOOLEAN);
    private final net.minecraft.server.level.ServerBossEvent boss=new net.minecraft.server.level.ServerBossEvent(getDisplayName(),net.minecraft.world.BossEvent.BossBarColor.PURPLE,net.minecraft.world.BossEvent.BossBarOverlay.PROGRESS);
    public int attackTicks; private int lifetime=200; public float flailIntensity=1;
    public Taintacle(EntityType<? extends Taintacle> type,Level level){super(type,level);xpReward=small()?0:giant()?20:10;}
    public boolean small(){return getType()==TaintacleContent.SMALL.get();}
    @Override public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor l,net.minecraft.world.DifficultyInstance d,net.minecraft.world.entity.MobSpawnType reason,net.minecraft.world.entity.SpawnGroupData group){if(giant())Champions.make(this,random.nextInt(Champions.NAMES.length),true);return super.finalizeSpawn(l,d,reason,group);}
    public boolean giant(){return getType()==TaintacleContent.GIANT.get();}
    public static AttributeSupplier.Builder attributes(double hp,double damage){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,hp).add(Attributes.ATTACK_DAMAGE,damage).add(Attributes.FOLLOW_RANGE,42);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(ANGER,0);b.define(AGITATED,false);}
    public int anger(){return entityData.get(ANGER);}
    public boolean agitated(){if(level().isClientSide)return entityData.get(AGITATED);return getTarget()!=null&&distanceToSqr(getTarget())<Math.pow(getBbHeight()*7,2);}
    @Override public void move(MoverType type,Vec3 movement){super.move(type,new Vec3(0,Math.min(0,movement.y),0));}
    @Override public boolean isPushable(){return true;}
    @Override public boolean removeWhenFarAway(double d){return !giant()&&super.removeWhenFarAway(d);}
    @Override public void tick(){
        super.tick();if(attackTicks>0)attackTicks--;
        if(level().isClientSide){
            boolean flail=tickCount>getBbHeight()*10&&(hurtTime>0||attackTicks>0||getTarget()!=null&&distanceTo(getTarget())<getBbHeight());
            flailIntensity=net.minecraft.util.Mth.clamp(flailIntensity+(flail?.2f:-.2f),1,3);
            if(giant()&&anger()>0&&random.nextInt(15)==0)level().addParticle(net.minecraft.core.particles.ParticleTypes.ANGRY_VILLAGER,getX()+(random.nextFloat()-.5)*getBbWidth(),getY()+getBbHeight()+random.nextFloat()*.5,getZ()+(random.nextFloat()-.5)*getBbWidth(),0,0,0);
            if(tickCount<getBbHeight()*10&&onGround())level().addParticle(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK,level().getBlockState(blockPosition().below())),getX()+(random.nextFloat()-.5)*getBbWidth()*2,getY()+.1,getZ()+(random.nextFloat()-.5)*getBbWidth()*2,0,.05,0);
            return;
        }
        if(tickCount%20==0&&!level().getBiome(blockPosition()).is(TaintBiomes.TAINTED_LAND))hurt(damageSources().starve(),1);
        if(small()&&lifetime--<=0)hurt(damageSources().generic(),10);
        if(giant()){boss.setProgress(getHealth()/getMaxHealth());boss.setName(getDisplayName());if(anger()>0)entityData.set(ANGER,anger()-1);if(tickCount%30==0)heal(1);}
        if(isNoAi())return;entityData.set(AGITATED,agitated());
        var target=getTarget();
        if(target==null){
            var candidates=level().getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(getBbHeight()*6,getBbHeight()*3,getBbHeight()*6),e->e!=this&&e.isAlive()&&!(e instanceof thaumcraft.api.entities.ITaintedMob)&&!(e instanceof net.minecraft.world.entity.player.Player p&&(p.isCreative()||p.isSpectator())));
            setTarget(candidates.stream().min(java.util.Comparator.comparingDouble(this::distanceToSqr)).orElse(null));
        }else if(!target.isAlive()||!agitated())setTarget(null);
        else {setYRot(net.minecraft.util.Mth.rotateIfNecessary(getYRot(),(float)(Math.atan2(target.getZ()-getZ(),target.getX()-getX())*180/Math.PI)-90,5));yBodyRot=getYRot();
            if(attackTicks==0&&hasLineOfSight(target)){
                if(distanceTo(target)<=getBbHeight()&&target.getBoundingBox().maxY>getBoundingBox().minY&&target.getBoundingBox().minY<getBoundingBox().maxY){attackTicks=20;level().broadcastEntityEvent(this,(byte)4);doHurtTarget(target);playSound(TaintacleContent.TENTACLE.get(),getSoundVolume(),getVoicePitch());}
                else if(target.onGround()&&!small())spawnTentacles(target);
            }
        }
    }
    @Override public void handleEntityEvent(byte event){if(event==4)attackTicks=20;else super.handleEntityEvent(event);}
    public void spawnTentacles(Entity target){
        var pos=target.blockPosition();if(!level().getBiome(pos).is(TaintBiomes.TAINTED_LAND)||!taintAt(pos)&&!taintAt(pos.below()))return;
        attackTicks=40+random.nextInt(20);var child=new Taintacle(TaintacleContent.SMALL.get(),level());child.moveTo(target.getX()+random.nextFloat()-random.nextFloat(),target.getY(),target.getZ()+random.nextFloat()-random.nextFloat(),0,0);level().addFreshEntity(child);playSound(TaintacleContent.TENTACLE.get(),getSoundVolume(),getVoicePitch());
    }
    private boolean taintAt(BlockPos p){var s=level().getBlockState(p);return s.is(TC4Modern.TAINT_FIBRES.get())||s.is(TC4Modern.TAINT.get());}
    @Override public boolean hurt(DamageSource source,float damage){
        if(!level().isClientSide){
            if(giant()&&damage>35){if(anger()==0){addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,(int)(damage/15)));addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,200,(int)(damage/40)));addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,200,(int)(damage/40)));entityData.set(ANGER,200);}damage=35;}
            if(!small()&&source.getEntity()!=null&&distanceTo(source.getEntity())>16)spawnTentacles(source.getEntity());
        }return super.hurt(source,damage);
    }
    @Override public void startSeenByPlayer(net.minecraft.server.level.ServerPlayer p){super.startSeenByPlayer(p);if(giant())boss.addPlayer(p);}
    @Override public void stopSeenByPlayer(net.minecraft.server.level.ServerPlayer p){super.stopSeenByPlayer(p);boss.removePlayer(p);}
    @Override protected int decreaseAirSupply(int air){return giant()?air:super.decreaseAirSupply(air);}
    @Override protected SoundEvent getAmbientSound(){return TC4Modern.ROOTS.get();}
    @Override protected SoundEvent getHurtSound(DamageSource s){return TaintacleContent.TENTACLE.get();}
    @Override protected SoundEvent getDeathSound(){return TaintacleContent.TENTACLE.get();}
    @Override public int getAmbientSoundInterval(){return 200;}
    @Override protected float getSoundVolume(){return getBbHeight()/8;}
    @Override public float getVoicePitch(){return 1.3f-getBbHeight()/10;}
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel l,DamageSource s,boolean recent){if(giant()&&l.getEntitiesOfClass(Taintacle.class,getBoundingBox().inflate(48),e->e!=this&&e.giant()&&e.isAlive()).isEmpty())spawnAtLocation(new net.minecraft.world.item.ItemStack(TaintacleContent.PEARL.get()),getBbHeight()/2);if(!small()&&!giant())spawnAtLocation(random.nextBoolean()?TC4Modern.TAINTED_GOO.get():TC4Modern.TAINTED_TENDRIL.get(),1);}
}
