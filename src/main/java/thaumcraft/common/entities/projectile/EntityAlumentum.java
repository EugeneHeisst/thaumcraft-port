package thaumcraft.common.entities.projectile;

import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.projectile.ThrowableProjectile;

/** Original EntityAlumentum impact and EntityThrowable 1.7.10 launch, adapted to modern entity transport. */
public final class EntityAlumentum extends ThrowableProjectile {
    public static java.util.function.Consumer<EntityAlumentum> clientTrail=entity->{};
    @Override public void tick(){super.tick();if(level().isClientSide)clientTrail.accept(this);}
    public static final float SPEED=.75f,POWER=1.66f;
    public EntityAlumentum(EntityType<? extends EntityAlumentum> type,Level level){super(type,level);}
    public EntityAlumentum(EntityType<? extends EntityAlumentum> type,LivingEntity shooter,Level level){
        super(type,shooter,level);
        setYRot(shooter.getYRot());setXRot(shooter.getXRot());
        float yaw=getYRot()/180f*(float)Math.PI,pitch=getXRot()/180f*(float)Math.PI;
        setPos(getX()-net.minecraft.util.Mth.cos(yaw)*.16f,getY(),getZ()-net.minecraft.util.Mth.sin(yaw)*.16f);
        double dx=-net.minecraft.util.Mth.sin(yaw)*net.minecraft.util.Mth.cos(pitch)*.4f;
        double dz=net.minecraft.util.Mth.cos(yaw)*net.minecraft.util.Mth.cos(pitch)*.4f;
        double dy=-net.minecraft.util.Mth.sin(pitch)*.4f;
        float length=net.minecraft.util.Mth.sqrt((float)(dx*dx+dy*dy+dz*dz));
        setDeltaMovement((dx/length+random.nextGaussian()*.0075f)*SPEED,
                (dy/length+random.nextGaussian()*.0075f)*SPEED,(dz/length+random.nextGaussian()*.0075f)*SPEED);
    }
    @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder){}
    @Override protected void onHit(net.minecraft.world.phys.HitResult hit){
        if(!level().isClientSide){
            boolean grief=level().getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_MOBGRIEFING);
            level().explode(null,getX(),getY(),getZ(),POWER,grief?Level.ExplosionInteraction.BLOCK:Level.ExplosionInteraction.NONE);
            discard();
        }
    }
}
