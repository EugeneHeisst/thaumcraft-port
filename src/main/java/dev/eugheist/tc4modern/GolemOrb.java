package dev.eugheist.tc4modern;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/**
 * EntityGolemOrb: weightless homing shock orb (red for cultist clerics). Steers toward its target by
 * 0.2 / distance^2 per tick with each velocity component clamped to 0.25, shocks on impact and expires
 * after 240 (red) or 160 ticks.
 */
public final class GolemOrb extends ThrowableProjectile implements IEntityWithComplexSpawn {
    private LivingEntity target;
    public boolean red;

    public GolemOrb(EntityType<? extends GolemOrb> type, Level level) { super(type, level); }
    public GolemOrb(Level level, LivingEntity owner, LivingEntity target, boolean red) {
        super(CrimsonContent.GOLEM_ORB.get(), owner, level);
        this.target = target;
        this.red = red;
        // EntityThrowable(World, EntityLivingBase): eye height minus 0.1, shifted 0.16 sideways, launched along the look at 1.5.
        float yaw = owner.getYRot() * Mth.DEG_TO_RAD;
        setPos(getX() - Mth.cos(yaw) * .16, getY(), getZ() - Mth.sin(yaw) * .16);
        shootFromRotation(owner, owner.getXRot(), owner.getYRot(), 0, 1.5f, 1);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected double getDefaultGravity() { return 0; }

    @Override public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        buffer.writeInt(target == null ? -1 : target.getId());
        buffer.writeBoolean(red);
    }
    @Override public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        int id = buffer.readInt();
        if (id >= 0 && level().getEntity(id) instanceof LivingEntity living) target = living;
        red = buffer.readBoolean();
    }

    @Override public void tick() {
        super.tick();
        if (tickCount > (red ? 240 : 160)) discard();
        if (target != null) {
            // Original divides by the squared distance.
            double d = distanceToSqr(target);
            double dx = (target.getX() - getX()) / d, dy = (target.getY() + target.getBbHeight() * .6 - getY()) / d, dz = (target.getZ() - getZ()) / d;
            var motion = getDeltaMovement().add(dx * .2, dy * .2, dz * .2);
            setDeltaMovement(Mth.clamp(motion.x, -.25, .25), Mth.clamp(motion.y, -.25, .25), Mth.clamp(motion.z, -.25, .25));
        }
    }

    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!level().isClientSide && getOwner() instanceof LivingEntity owner && hit instanceof EntityHitResult entityHit)
            entityHit.getEntity().hurt(damageSources().thrown(this, owner), (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * (red ? 1 : .6f));
        if (level().isClientSide) return;
        level().playSound(null, getX(), getY(), getZ(), sound("shock"), SoundSource.HOSTILE, 1, 1 + (random.nextFloat() - random.nextFloat()) * .2f);
        level().broadcastEntityEvent(this, (byte) 3);
        discard();
    }

    @Override public void handleEntityEvent(byte id) {
        if (id == 3) NodeClientEffects.burst(level(), getX(), getY(), getZ());
        else super.handleEntityEvent(id);
    }

    /** attackEntityFrom: a blow from an entity knocks the orb along the attacker's look. */
    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        markHurt();
        if (source.getEntity() == null) return false;
        setDeltaMovement(source.getEntity().getLookAngle().scale(.9));
        level().playSound(null, this, sound("zap"), SoundSource.HOSTILE, 1, 1 + (random.nextFloat() - random.nextFloat()) * .2f);
        return true;
    }

    private static SoundEvent sound(String name) { return SoundEvent.createVariableRangeEvent(AlchemyContent.id(name)); }
}
