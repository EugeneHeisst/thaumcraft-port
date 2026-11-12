package thaumcraft.common.entities.monster;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

/** Original swarmer variant: stationary spore that releases flying taint swarms. */
public final class EntityTaintSporeSwarmer extends EntityTaintSpore {
    public int spawnCounter=500;
    public static java.util.function.Consumer<EntityTaintSporeSwarmer> pushOut;
    public static java.util.function.Function<Level,? extends Mob> swarm;
    public static java.util.function.BiConsumer<EntityTaintSporeSwarmer,Integer> fragments;
    public static java.util.function.Consumer<EntityTaintSporeSwarmer> particles,clearParticles;
    public static java.util.function.Supplier<net.minecraft.sounds.SoundEvent> roots;
    public EntityTaintSporeSwarmer(EntityType<? extends EntityTaintSporeSwarmer> type,Level level){super(type,level);setSporeSize(10);}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier.Builder attributes(){return net.minecraft.world.entity.monster.Monster.createMonsterAttributes().add(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH,75).add(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,1);}
    @Override public EntityDimensions getDefaultDimensions(Pose pose){return EntityDimensions.scalable(1,1);}
    @Override protected void sporeOnUpdate(){
        pushOut.accept(this);
        if(spawnCounter>0)--spawnCounter;
        if(spawnCounter<=0&&level().getNearestPlayer(this,16)!=null){spawnCounter=500;swarmBurst(1);}
        if(level().isClientSide)particles.accept(this);
        if(deathTime==1)swarmBurst(1);
    }
    @Override public void playerTouch(net.minecraft.world.entity.player.Player player){}
    @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource source,float damage){if(level().isClientSide)fragments.accept(this,10);return super.hurt(source,damage);}
    protected void swarmBurst(int count){
        if(!level().isClientSide){
            playSound(gore.get(),1,.9f+random.nextFloat()*.1f);
            for(int i=0;i<count;i++){var child=swarm.apply(level());child.moveTo(getX(),getY()+.5,getZ(),random.nextFloat()*360,0);level().addFreshEntity(child);}
            level().broadcastEntityEvent(this,(byte)6);
        }
    }
    @Override public void handleEntityEvent(byte event){
        if(event==6){spawnCounter=500;fragments.accept(this,25);clearParticles.accept(this);}
        else super.handleEntityEvent(event);
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound(){return roots.get();}
    @Override protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,boolean recent){for(int i=0;i<=1;i++)spawnAtLocation(new net.minecraft.world.item.ItemStack(random.nextBoolean()?goo.get():tendril.get()),getBbHeight()/2);}
}
