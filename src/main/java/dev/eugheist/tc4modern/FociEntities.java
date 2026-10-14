package dev.eugheist.tc4modern;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

/** EntityEmber and EntityFrostShard ports. 1.7 setThrowableHeading uses 0.0075 inaccuracy; modern 0.0172275. */
public final class FociEntities {
    private FociEntities() {}
    static final float INACCURACY = 0.0075f / 0.0172275f;
    static void launch(ThrowableProjectile p, LivingEntity thrower, float speed, float scatter) {
        p.setPos(thrower.getX() - Math.cos(thrower.getYRot() / 180f * Math.PI) * .16f, thrower.getEyeY() - .1f, thrower.getZ() - Math.sin(thrower.getYRot() / 180f * Math.PI) * .16f);
        p.shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0, speed, scatter * INACCURACY);
    }

    public static final class Ember extends ThrowableProjectile {
        public int duration = 20, firey;
        public float damage = 1f;
        private static final EntityDataAccessor<Integer> DURATION = SynchedEntityData.defineId(Ember.class, EntityDataSerializers.INT);
        public Ember(EntityType<? extends Ember> type, Level level) { super(type, level); }
        public Ember(Level level, LivingEntity thrower, float scatter) {
            super(FociContent.EMBER.get(), level);
            setOwner(thrower);
            launch(this, thrower, 1.0f, scatter);
        }
        @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(DURATION, 20); }
        @Override protected double getDefaultGravity() { return 0; }
        @Override public void tick() {
            if (!level().isClientSide) entityData.set(DURATION, duration); else duration = entityData.get(DURATION);
            if (tickCount > duration) discard();
            double f = duration <= 20 ? .95 : .975;
            setDeltaMovement(getDeltaMovement().scale(f));
            if (onGround()) setDeltaMovement(getDeltaMovement().scale(.66));
            super.tick();
        }
        @Override protected void onHit(HitResult mop) {
            if (!level().isClientSide) {
                if (mop instanceof EntityHitResult eh) {
                    var e = eh.getEntity();
                    if (!e.fireImmune() && e.hurt(new net.minecraft.world.damagesource.DamageSource(level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.FIREBALL), this, getOwner()), damage)) e.igniteForSeconds(3 + firey);
                } else if (mop instanceof BlockHitResult bh && random.nextFloat() < .025f * firey) {
                    var p = bh.getBlockPos().relative(bh.getDirection());
                    if (level().isEmptyBlock(p)) level().setBlockAndUpdate(p, Blocks.FIRE.defaultBlockState());
                }
            }
            discard();
        }
        @Override public boolean isPickable() { return false; }
        @Override public boolean hurt(net.minecraft.world.damagesource.DamageSource s, float a) { return false; }
        @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putFloat("damage", damage); t.putInt("firey", firey); t.putInt("duration", duration); }
        @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); damage = t.getFloat("damage"); firey = t.getInt("firey"); duration = t.getInt("duration"); }
    }

    public static final class FrostShard extends ThrowableProjectile {
        public double bounce = .5;
        public int bounceLimit = 3;
        public boolean fragile;
        private static final EntityDataAccessor<Float> DAMAGE = SynchedEntityData.defineId(FrostShard.class, EntityDataSerializers.FLOAT);
        private static final EntityDataAccessor<Integer> FROSTY = SynchedEntityData.defineId(FrostShard.class, EntityDataSerializers.INT);
        public FrostShard(EntityType<? extends FrostShard> type, Level level) { super(type, level); }
        public FrostShard(Level level, LivingEntity thrower, float scatter) {
            super(FociContent.FROST_SHARD.get(), level);
            setOwner(thrower);
            launch(this, thrower, 1.5f, scatter);
        }
        @Override protected void defineSynchedData(SynchedEntityData.Builder b) { b.define(DAMAGE, 0f); b.define(FROSTY, 0); }
        public float getDamage() { return entityData.get(DAMAGE); }
        public void setDamage(float d) { entityData.set(DAMAGE, d); }
        public int getFrosty() { return entityData.get(FROSTY); }
        public void setFrosty(int f) { entityData.set(FROSTY, f); }
        @Override protected double getDefaultGravity() { return fragile ? .015 : .05; }
        private void crack(net.minecraft.world.level.block.state.BlockState state, int count) {
            for (int a = 0; a < count; ++a) level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), getX(), getY(), getZ(), 4.0 * (random.nextFloat() - .5), .5, (random.nextFloat() - .5) * 4.0);
        }
        @Override protected void onHit(HitResult mop) {
            var m = getDeltaMovement();
            double mx = m.x, my = m.y, mz = m.z;
            if (mop instanceof EntityHitResult eh) {
                var e = eh.getEntity();
                int ox = (int) Math.floor(getX()) - (int) Math.floor(e.getX()), oy = (int) Math.floor(getY()) - (int) Math.floor(e.getY()), oz = (int) Math.floor(getZ()) - (int) Math.floor(e.getZ());
                if (oz != 0) mz *= -1; if (ox != 0) mx *= -1; if (oy != 0) my *= -.9;
                mx *= .66; my *= .66; mz *= .66;
                crack(Blocks.ICE.defaultBlockState(), (int) getDamage());
            } else if (mop instanceof BlockHitResult bh) {
                var d = bh.getDirection();
                if (d.getStepZ() != 0) mz *= -1; if (d.getStepX() != 0) mx *= -1; if (d.getStepY() != 0) my *= -.9;
                var state = level().getBlockState(bh.getBlockPos());
                playSound(state.getSoundType().getHitSound(), .3f, 1.2f / (random.nextFloat() * .2f + .9f));
                crack(state, (int) getDamage());
            }
            mx *= bounce; my *= bounce; mz *= bounce;
            setDeltaMovement(mx, my, mz);
            double len = Math.sqrt(mx * mx + my * my + mz * mz);
            if (len > 0) setPos(getX() - mx / len * .05, getY() - my / len * .05, getZ() - mz / len * .05);
            if (!level().isClientSide && mop instanceof EntityHitResult eh) {
                var e = eh.getEntity();
                var before = e.getDeltaMovement();
                e.hurt(damageSources().thrown(this, getOwner()), getDamage());
                if (e instanceof LivingEntity le && getFrosty() > 0) le.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, getFrosty() - 1));
                if (fragile) {
                    e.invulnerableTime = 0;
                    discard();
                    playSound(Blocks.ICE.defaultBlockState().getSoundType().getBreakSound(), .3f, 1.2f / (random.nextFloat() * .2f + .9f));
                    var now = e.getDeltaMovement();
                    e.setDeltaMovement(before.add(now.subtract(before).scale(.1)));
                }
            }
            if (bounceLimit-- <= 0) {
                discard();
                playSound(Blocks.ICE.defaultBlockState().getSoundType().getBreakSound(), .3f, 1.2f / (random.nextFloat() * .2f + .9f));
                crack(Blocks.ICE.defaultBlockState(), (int) (8 * getDamage()));
            }
        }
        @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putFloat("damage", getDamage()); t.putBoolean("fragile", fragile); t.putInt("frost", getFrosty()); }
        @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); setDamage(t.getFloat("damage")); fragile = t.getBoolean("fragile"); setFrosty(t.getInt("frost")); }
    }
}
