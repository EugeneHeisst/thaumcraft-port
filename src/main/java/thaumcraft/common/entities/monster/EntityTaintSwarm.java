package thaumcraft.common.entities.monster;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.*;
import net.minecraft.core.BlockPos;
/** Original taint swarm flight and attack logic with modern living-entity transport. */
public final class EntityTaintSwarm extends net.minecraft.world.entity.monster.Monster implements thaumcraft.api.entities.ITaintedMob {
    private static final EntityDataAccessor<Byte> FLAGS=SynchedEntityData.defineId(EntityTaintSwarm.class,EntityDataSerializers.BYTE);
    public static java.util.function.BiPredicate<Level,BlockPos> taintedBiome;
    public static java.util.function.Supplier<net.minecraft.sounds.SoundEvent> attackSound;
    public static java.util.function.Supplier<net.minecraft.world.item.Item> goo;
    public static java.util.function.Consumer<EntityTaintSwarm> particles;
    public static java.util.function.Consumer<LivingEntity> recentlyHit=e->{};
    private TaintSwarmFlight.Target flightTarget;
    private int attackCooldown;
    public int damBonus;
    public EntityTaintSwarm(EntityType<? extends EntityTaintSwarm> type,Level level){super(type,level);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,30).add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,2);}
    @Override protected void registerGoals(){}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(FLAGS,(byte)0);}
    public boolean getIsSummoned(){return (entityData.get(FLAGS)&2)!=0;}
    public void setIsSummoned(boolean value){entityData.set(FLAGS,TaintSwarmFlight.summoned(entityData.get(FLAGS),value));}
    @Override public boolean isPushable(){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,net.minecraft.world.damagesource.DamageSource source){return false;}
    @Override protected void checkFallDamage(double y,boolean ground,net.minecraft.world.level.block.state.BlockState state,BlockPos pos){}
    @Override public void tick(){super.tick();setDeltaMovement(getDeltaMovement().multiply(1,(double).6f,1));if(level().isClientSide)particles.accept(this);}
    @Override protected void customServerAiStep(){
        if(attackCooldown>0)--attackCooldown;
        var target=getTarget();boolean hadTarget=target!=null;
        if(target!=null&&!target.isAlive()){setTarget(null);target=null;}
        if(target!=null&&getSensing().hasLineOfSight(target))attackLegacy(target);
        if(!hadTarget&&!getIsSummoned()){
            var player=level().getNearestPlayer(this,12);
            if(player!=null){setTarget(player);target=player;}
        }
        double dx,dy,dz;
        if(target==null){
            if(getIsSummoned())hurt(damageSources().starve(),5);
            if(flightTarget!=null){var pos=new BlockPos(flightTarget.x(),flightTarget.y(),flightTarget.z());if(!level().isEmptyBlock(pos)||pos.getY()<1||pos.getY()>level().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,pos.getX(),pos.getZ())+8||!taintedBiome.test(level(),pos))flightTarget=null;}
            var rng=new java.util.random.RandomGenerator(){public long nextLong(){return random.nextLong();}public int nextInt(int bound){return random.nextInt(bound);}};
            if(TaintSwarmFlight.needsTarget(flightTarget,getX(),getY(),getZ(),rng))flightTarget=TaintSwarmFlight.choose(getX(),getY(),getZ(),rng);
            dx=flightTarget.x()+.5-getX();dy=flightTarget.y()+.1-getY();dz=flightTarget.z()+.5-getZ();
        }else{dx=target.getX()-getX();dy=target.getY()+target.getEyeHeight()-getY();dz=target.getZ()-getZ();}
        var v=getDeltaMovement();var next=TaintSwarmFlight.steer(v.x,v.y,v.z,dx,dy,dz,target!=null);setDeltaMovement(next.x(),next.y(),next.z());setYRot(getYRot()+net.minecraft.util.Mth.wrapDegrees(next.yaw()-getYRot()));zza=.1f;
        if(target instanceof net.minecraft.world.entity.player.Player player&&player.getAbilities().invulnerable){setTarget(null);return;}
    }
    private void attackLegacy(LivingEntity target){
        if(TaintSwarmFlight.canAttack(attackCooldown,distanceTo(target),target.getBoundingBox().maxY,target.getBoundingBox().minY,getBoundingBox().maxY,getBoundingBox().minY)){
            if(getIsSummoned())recentlyHit.accept(target);
            attackCooldown=10+random.nextInt(5);var velocity=target.getDeltaMovement();
            if(doHurtTarget(target))target.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON,100,0));
            target.hurtMarked=false;target.setDeltaMovement(velocity);playSound(attackSound.get(),.3f,.9f+random.nextFloat()*.2f);
        }
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return null;}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return attackSound.get();}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return attackSound.get();}
    @Override protected float getSoundVolume(){return .1f;}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putByte("Flags",entityData.get(FLAGS));tag.putByte("damBonus",(byte)damBonus);}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);entityData.set(FLAGS,tag.getByte("Flags"));damBonus=tag.getByte("damBonus");}
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean recent){if(random.nextBoolean())spawnAtLocation(new net.minecraft.world.item.ItemStack(goo.get()),getBbHeight()/2);}
}
