package thaumcraft.common.entities.monster;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.network.syncher.*;

/** Original EntityThaumicSlime adapted from EntityMob to modern Monster and synchronized entity data. */
public final class EntityThaumicSlime extends net.minecraft.world.entity.monster.Monster implements thaumcraft.api.entities.ITaintedMob {
    private static final EntityDataAccessor<Byte> SIZE=SynchedEntityData.defineId(EntityThaumicSlime.class,EntityDataSerializers.BYTE);
    public static java.util.function.Supplier<net.minecraft.world.item.Item> taintedGoo;
    public static java.util.function.Supplier<net.minecraft.sounds.SoundEvent> gore;
    public float squish,targetSquish,previousSquish;
    private int jumpDelay,spitCounter=100;
    public int launched=10;
    private boolean split;
    public EntityThaumicSlime(EntityType<? extends EntityThaumicSlime> type,Level level){
        super(type,level);jumpDelay=random.nextInt(20)+10;setSlimeSize(1<<random.nextInt(3));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(SIZE,(byte)1);}
    public int getSlimeSize(){return entityData.get(SIZE);}
    public void setSlimeSize(int size){
        entityData.set(SIZE,(byte)size);getAttribute(Attributes.MAX_HEALTH).setBaseValue(size);
        setHealth(getMaxHealth());xpReward=ThaumicSlimeState.experience(size);refreshDimensions();
    }
    @Override public EntityDimensions getDefaultDimensions(Pose pose){float width=level().isClientSide?ThaumicSlimeState.clientWidth(getSlimeSize()):ThaumicSlimeState.serverWidth(getSlimeSize());return EntityDimensions.scalable(width,width);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(SIZE.equals(key))refreshDimensions();}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Size",getSlimeSize()-1);}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);setSlimeSize(tag.getInt("Size")+1);}
    @Override public void tick(){
        squish+=(targetSquish-squish)*.5f;previousSquish=squish;boolean grounded=onGround();
        super.tick();if(launched>0)launched--;
        if(onGround()&&!grounded){targetSquish=-.5f;if(ThaumicSlimeState.landSound(getSlimeSize()))playSound(slimeSound(),getSoundVolume(),((random.nextFloat()-random.nextFloat())*.2f+1)/.8f);}
        else if(!onGround()&&grounded)targetSquish=1;
        targetSquish*=.6f;
    }
    private EntityThaumicSlime closestMerge(){
        EntityThaumicSlime closest=null;double distance=Double.MAX_VALUE;
        for(var slime:level().getEntitiesOfClass(EntityThaumicSlime.class,new net.minecraft.world.phys.AABB(position(),position()).inflate(16,8,16))){
            if(slime.getId()!=getId()&&slime.tickCount>100&&slime.getSlimeSize()<100&&distanceToSqr(slime)<distance)closest=slime;
            distance=distanceToSqr(slime); // Original assignment intentionally lies outside the condition.
        }
        return closest;
    }
    private void face(LivingEntity target){
        double dx=target.getX()-getX(),dz=target.getZ()-getZ(),dy=target.getEyeY()-getEyeY();
        float yaw=(float)(Math.atan2(dz,dx)*180/Math.PI)-90,pitch=(float)(-Math.atan2(dy,Math.sqrt(dx*dx+dz*dz))*180/Math.PI);
        setYRot(net.minecraft.util.Mth.approachDegrees(getYRot(),yaw,10));setXRot(net.minecraft.util.Mth.approachDegrees(getXRot(),pitch,20));
    }
    @Override protected void customServerAiStep(){
        super.customServerAiStep();var player=level().getNearestPlayer(this,16);
        if(player!=null){
            if(spitCounter>0)spitCounter--;face(player);
            if(distanceTo(player)>4&&spitCounter<=0&&getSlimeSize()>3){
                spitCounter=101;var child=new EntityThaumicSlime((EntityType<? extends EntityThaumicSlime>)getType(),level());child.setSlimeSize(1);
                double dx=player.getX()-getX(),dz=player.getZ()-getZ(),horizontal=Math.sqrt(dx*dx+dz*dz),yy=(getBoundingBox().minY+getBoundingBox().maxY)/2;
                double dy=player.getBoundingBox().minY+player.getBbHeight()/3-yy;
                if(horizontal>=1e-7){child.setPos(getX()+dx/horizontal,yy,getZ()+dz/horizontal);double up=dy+horizontal*.2,length=Math.sqrt(dx*dx+up*up+dz*dz);
                    child.setDeltaMovement((dx/length+random.nextGaussian()*.0075f)*1.5,(up/length+random.nextGaussian()*.0075f)*1.5,(dz/length+random.nextGaussian()*.0075f)*1.5);}
                level().addFreshEntity(child);playSound(gore.get(),1,((random.nextFloat()-random.nextFloat())*.2f+1)*.8f);setSlimeSize(getSlimeSize()-1);
            }
        }else{
            var other=closestMerge();if(other!=null){face(other);if(distanceTo(other)<getBbWidth()+other.getBbWidth()){other.setSlimeSize(ThaumicSlimeState.merge(other.getSlimeSize(),getSlimeSize()));discard();}}
        }
        if(onGround()&&jumpDelay--<=0){jumpDelay=random.nextInt(16)+8;if(player!=null)jumpDelay/=3;
            setJumping(true);if(ThaumicSlimeState.jumpSound(getSlimeSize()))playSound(slimeSound(),getSoundVolume(),((random.nextFloat()-random.nextFloat())*.2f+1)*.8f);
            xxa=1-random.nextFloat()*2;zza=(float)Math.sqrt(getSlimeSize());
        }else{setJumping(false);if(onGround()){zza=0;xxa=0;}}
    }
    @Override public void playerTouch(net.minecraft.world.entity.player.Player player){
        if(hasLineOfSight(player)&&distanceToSqr(player)<ThaumicSlimeState.attackDistanceSquared(getSlimeSize(),launched)
                &&player.hurt(damageSources().mobAttack(this),getSlimeSize()))playSound(net.minecraft.sounds.SoundEvents.PLAYER_ATTACK_STRONG,1,(random.nextFloat()-random.nextFloat())*.2f+1);
    }
    @Override public void remove(RemovalReason reason){
        if(!split&&!level().isClientSide&&getHealth()<=0){split=true;int count=ThaumicSlimeState.deathChildren(getSlimeSize());
            for(int i=0;i<count;i++){var child=new EntityThaumicSlime((EntityType<? extends EntityThaumicSlime>)getType(),level());child.setSlimeSize(1);child.moveTo(getX()+ThaumicSlimeState.childX(i,getSlimeSize()),getY()+.5,getZ()+ThaumicSlimeState.childZ(i,getSlimeSize()),random.nextFloat()*360,0);level().addFreshEntity(child);}}
        super.remove(reason);
    }
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean recent){if(getSlimeSize()<3&&random.nextInt(3)==0)spawnAtLocation(new net.minecraft.world.item.ItemStack(taintedGoo.get()),getBbHeight()/2);}
    private net.minecraft.sounds.SoundEvent slimeSound(){return getSlimeSize()>3?net.minecraft.sounds.SoundEvents.SLIME_HURT:net.minecraft.sounds.SoundEvents.SLIME_HURT_SMALL;}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return slimeSound();}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return slimeSound();}
    @Override protected float getSoundVolume(){return ThaumicSlimeState.volume(getSlimeSize());}
}
