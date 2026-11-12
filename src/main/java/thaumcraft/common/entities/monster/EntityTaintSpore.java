package thaumcraft.common.entities.monster;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.*;
import net.minecraft.core.BlockPos;
/** Original EntityTaintSpore adapted to modern Monster; world dependencies injected by the mod bridge. */
public class EntityTaintSpore extends net.minecraft.world.entity.monster.Monster implements thaumcraft.api.entities.ITaintedMob,net.neoforged.neoforge.entity.IEntityWithComplexSpawn {
    private static final EntityDataAccessor<Byte> SIZE=SynchedEntityData.defineId(EntityTaintSpore.class,EntityDataSerializers.BYTE);
    public static java.util.function.BiPredicate<Level,BlockPos> supportingStalk,taintedBiome;
    public static java.util.function.BiConsumer<Level,BlockPos> resetStalk;
    public static java.util.function.Function<Level,? extends net.minecraft.world.entity.Mob> spiderling;
    public static java.util.function.Supplier<net.minecraft.sounds.SoundEvent> gore,swarmSound;
    public static java.util.function.Supplier<net.minecraft.world.item.Item> goo,tendril;
    public static java.util.function.Consumer<EntityTaintSpore> sploosh,swarmParticles;
    private int growth;
    public float displaySize;
    public EntityTaintSpore(EntityType<? extends EntityTaintSpore> type,Level level){super(type,level);setSporeSize(2);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(SIZE,(byte)1);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,1).add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,1);}
    public int getSporeSize(){return entityData.get(SIZE);}
    public void setSporeSize(int size){entityData.set(SIZE,(byte)size);xpReward=size;refreshDimensions();}
    @Override public EntityDimensions getDefaultDimensions(Pose pose){float size=Math.max(.15f*getSporeSize(),.5f);return EntityDimensions.scalable(size,size);}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(SIZE.equals(key))refreshDimensions();}
    @Override public void addAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("Size",getSporeSize()-1);}
    @Override public void readAdditionalSaveData(net.minecraft.nbt.CompoundTag tag){super.readAdditionalSaveData(tag);setSporeSize(tag.getInt("Size")+1);}
    @Override public void writeSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buffer){buffer.writeFloat(getSporeSize());}
    @Override public void readSpawnData(net.minecraft.network.RegistryFriendlyByteBuf buffer){try{displaySize=buffer.readFloat();}catch(Exception ignored){}}
    protected BlockPos support(){return BlockPos.containing(getX(),getBoundingBox().minY,getZ()).below();}
    @Override public void move(MoverType type,net.minecraft.world.phys.Vec3 movement){
        if(supportingStalk!=null&&supportingStalk.test(level(),support()))return;
        super.move(type,new net.minecraft.world.phys.Vec3(0,Math.min(movement.y,0),0));
    }
    @Override protected void customServerAiStep(){}
    @Override public boolean isPushable(){return false;}
    @Override public boolean isPickable(){return true;}
    @Override public boolean shouldRenderAtSqrDistance(double distance){return distance<4096;}
    @Override public void tick(){
        super.tick();
        if(!level().isClientSide&&tickCount%20==0&&!taintedBiome.test(level(),blockPosition()))hurt(damageSources().generic(),1);
        sporeOnUpdate();
    }
    protected void sporeOnUpdate(){
        if(getSporeSize()<10&&growth++==1200){setSporeSize(getSporeSize()+1);growth=0;}
        if(level().isClientSide){if(displaySize<getSporeSize())displaySize+=.02f;swarmParticles.accept(this);}
        if(!supportingStalk.test(level(),support())||deathTime>0)spiderBurst();
    }
    @Override public void playerTouch(net.minecraft.world.entity.player.Player player){spiderBurst();}
    private void spiderBurst(){
        if(level().isClientSide){sploosh.accept(this);return;}
        playSound(gore.get(),1,.9f+random.nextFloat()*.1f);
        int count=getSporeSize()/3+random.nextInt(getSporeSize()/2+1);
        for(int i=0;i<count;i++){
            var child=spiderling.apply(level());child.moveTo(getX()+random.nextFloat()-random.nextFloat(),getY()+random.nextFloat(),getZ()+random.nextFloat()-random.nextFloat(),random.nextFloat()*360,0);level().addFreshEntity(child);
        }
        if(supportingStalk.test(level(),support()))resetStalk.accept(level(),support());
        discard();
    }
    @Override protected float getSoundVolume(){return .1f;}
    @Override public int getAmbientSoundInterval(){return 200;}
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return swarmSound.get();}
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source){return gore.get();}
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound(){return gore.get();}
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean recent){spawnAtLocation(new net.minecraft.world.item.ItemStack(random.nextBoolean()?goo.get():tendril.get()),getBbHeight()/2);}
}
