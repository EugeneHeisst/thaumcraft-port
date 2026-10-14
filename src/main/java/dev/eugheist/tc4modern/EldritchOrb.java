package dev.eugheist.tc4modern;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** EntityEldritchOrb combat, lifetime and original thirty-wisp impact burst. */
public final class EldritchOrb extends ThrowableProjectile {
    public EldritchOrb(EntityType<? extends EldritchOrb> type, Level level) { super(type, level); }
    public EldritchOrb(EntityType<? extends EldritchOrb> type, LivingEntity owner, Level level) {
        super(type, owner, level);
    }
    public EldritchOrb(Level level, LivingEntity owner) { this(WarpEntities.ELDRITCH_ORB.get(), owner, level); }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {}
    @Override protected double getDefaultGravity() { return 0; }
    @Override public void tick() {
        super.tick();
        if (tickCount > 100) discard();
    }
    @Override public void handleEntityEvent(byte event){if(event==16){if(level().isClientSide)GuardianClient.orbBurst(this);}else super.handleEntityEvent(event);}
    @Override protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (level().isClientSide || !(getOwner() instanceof LivingEntity owner)) return;
        // Original uses the orb's current AABB, not a sphere around the hit point.
        for (var target : level().getEntities(owner, getBoundingBox().inflate(2))) {
            if (!(target instanceof LivingEntity living) || living.isSleeping()) continue;
            living.hurt(damageSources().indirectMagic(this, owner),
                (float) owner.getAttributeValue(Attributes.ATTACK_DAMAGE) * .666f);
            living.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 160, 0));
        }
        playSound(SoundEvents.LAVA_EXTINGUISH, .5f, 2.6f + (random.nextFloat() - random.nextFloat()) * .8f);
        // Original allows this tick's movement to finish before expiring on the next tick.
        tickCount = 100;
        level().broadcastEntityEvent(this, (byte) 16);
    }
}
