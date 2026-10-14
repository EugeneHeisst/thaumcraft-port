package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Projectiles of the rank-three focus upgrades: EntityExplosiveOrb (fire focus fireball) and EntityShockOrb (shock focus
 * earth shock), plus the electrified ground the shock orb leaves behind (BlockAiry metadata 10).
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FocusProjectiles {
    private FocusProjectiles() {}
    public static final DeferredHolder<EntityType<?>, EntityType<ExplosiveOrb>> EXPLOSIVE_ORB = DeferredHolder.create(Registries.ENTITY_TYPE, AlchemyContent.id("explosive_orb"));
    public static final DeferredHolder<EntityType<?>, EntityType<ShockOrb>> SHOCK_ORB = DeferredHolder.create(Registries.ENTITY_TYPE, AlchemyContent.id("shock_orb"));
    public static final DeferredHolder<Block, Block> SPARK = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("electric_spark"));

    @SubscribeEvent static void register(RegisterEvent event) {
        // EntityRegistry: both tracked at 64 blocks, every tick, with velocity updates.
        event.register(Registries.ENTITY_TYPE, h -> {
            h.register(EXPLOSIVE_ORB.getId(), EntityType.Builder.<ExplosiveOrb>of(ExplosiveOrb::new, MobCategory.MISC).sized(.25f, .25f).clientTrackingRange(4).updateInterval(1).build("explosive_orb"));
            h.register(SHOCK_ORB.getId(), EntityType.Builder.<ShockOrb>of(ShockOrb::new, MobCategory.MISC).sized(.25f, .25f).clientTrackingRange(4).updateInterval(1).build("shock_orb"));
        });
        // BlockAiry metadata 10: cloth sound, hardness 100, resistance 50, light 8, random ticks.
        event.register(Registries.BLOCK, h -> h.register(SPARK.getId(), new SparkBlock(BlockBehaviour.Properties.of().replaceable().noCollission().noOcclusion()
            .noLootTable().strength(100, 50).lightLevel(s -> 8).randomTicks().sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY).air())));
    }

    /** EntityThrowable(world, thrower) launch and the shared "hit by something with a look vector" deflection. */
    static boolean knockBack(ThrowableProjectile orb, DamageSource source) {
        if (orb.isInvulnerableTo(source)) return false;
        orb.hurtMarked = true;
        var attacker = source.getEntity();
        if (attacker == null) return false;
        orb.setDeltaMovement(attacker.getLookAngle().scale(.9));
        return true;
    }

    /** EntityUtils.canEntityBeSeen: no collidable block between the two points (liquids ignored). */
    static boolean clear(Entity from, Vec3 to) {
        return from.level().clip(new ClipContext(from.position(), to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from)).getType() == HitResult.Type.MISS;
    }

    // ---------------------------------------------------------------- EntityExplosiveOrb

    /** A slow-falling fireball that explodes without breaking blocks; the alchemist's fire upgrade makes the blast set fires. */
    public static final class ExplosiveOrb extends ThrowableProjectile {
        public float strength = 1;
        public boolean onFire;
        public ExplosiveOrb(EntityType<? extends ExplosiveOrb> type, Level level) { super(type, level); }
        public ExplosiveOrb(Level level, LivingEntity thrower) {
            super(EXPLOSIVE_ORB.get(), level);
            setOwner(thrower);
            FociEntities.launch(this, thrower, 1.5f, 1f);
        }
        @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {}
        @Override protected double getDefaultGravity() { return .01; }
        @Override protected void onHit(HitResult mop) {
            if (!level().isClientSide) {
                if (mop instanceof EntityHitResult eh) eh.getEntity().hurt(fireball(), strength * 1.5f);
                level().explode(null, getX(), getY(), getZ(), strength, onFire, Level.ExplosionInteraction.NONE);
                discard();
            }
            discard();
        }
        /** causeFireballDamage: "onFire" from the orb itself without a thrower, else "fireball"; both fire and projectile damage. */
        private DamageSource fireball() {
            var types = level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            return getOwner() == null ? new DamageSource(types.getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.UNATTRIBUTED_FIREBALL), this, this)
                : new DamageSource(types.getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.FIREBALL), this, getOwner());
        }
        @Override public void tick() {
            super.tick();
            if (level().isClientSide) {
                var r = random;
                FociFx.generic(xo + (r.nextFloat() - r.nextFloat()) * .3f, yo + (r.nextFloat() - r.nextFloat()) * .3f, zo + (r.nextFloat() - r.nextFloat()) * .3f,
                    0, 0, 0, 1, 1, 1, .8f, false, 151, 9, 1, 7 + r.nextInt(5), 0, 2 + r.nextFloat());
            }
            if (tickCount > 500) discard();
        }
        @Override public boolean hurt(DamageSource source, float amount) { return knockBack(this, source); }
        @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putFloat("strength", strength); t.putBoolean("onFire", onFire); }
        @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); strength = t.getFloat("strength"); onFire = t.getBoolean("onFire"); }
    }

    // ---------------------------------------------------------------- EntityShockOrb

    /** A lobbed ball of lightning: indirect magic damage to everything in sight within its area, then electrified ground. */
    public static final class ShockOrb extends ThrowableProjectile {
        public int area = 4, damage = 5;
        public ShockOrb(EntityType<? extends ShockOrb> type, Level level) { super(type, level); }
        public ShockOrb(Level level, LivingEntity thrower) {
            super(SHOCK_ORB.get(), level);
            setOwner(thrower);
            FociEntities.launch(this, thrower, 1.5f, 1f);
        }
        @Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder b) {}
        @Override protected double getDefaultGravity() { return .05; }
        @Override protected void onHit(HitResult mop) {
            var level = level();
            if (!level.isClientSide) {
                for (var e : level.getEntities(this, new AABB(getX(), getY(), getZ(), getX(), getY(), getZ()).inflate(area))) {
                    if (!clear(this, e.position())) continue;
                    e.hurt(damageSources().indirectMagic(this, getOwner()), damage);
                }
                var r = random;
                for (int a = 0; a < 20; ++a) {
                    int xx = Mth.floor(getX()) + r.nextInt(area) - r.nextInt(area);
                    int zz = Mth.floor(getZ()) + r.nextInt(area) - r.nextInt(area);
                    int yy = Mth.floor(getY()) + area;
                    while (level.isEmptyBlock(new BlockPos(xx, yy, zz)) && yy > Mth.floor(getY()) - area) --yy;
                    var above = new BlockPos(xx, yy + 1, zz);
                    if (!level.isEmptyBlock(above) || level.isEmptyBlock(new BlockPos(xx, yy, zz)) || level.getBlockState(above).is(SPARK.get())
                        || !clear(this, new Vec3(xx + .5, yy + 1.5, zz + .5))) continue;
                    level.setBlock(above, SPARK.get().defaultBlockState(), 3);
                }
            } else {
                NodeClientEffects.burst(level, getX(), getY(), getZ(), 3);
            }
            level.playSound(null, getX(), getY(), getZ(), FociContent.sound("shock"), SoundSource.NEUTRAL, 1f, 1f + (random.nextFloat() - random.nextFloat()) * .2f);
            discard();
        }
        @Override public void tick() { super.tick(); if (tickCount > 500) discard(); }
        @Override public boolean hurt(DamageSource source, float amount) {
            boolean deflected = knockBack(this, source);
            if (deflected) level().playSound(null, getX(), getY(), getZ(), FociContent.sound("zap"), SoundSource.NEUTRAL, 1f, 1f + (random.nextFloat() - random.nextFloat()) * .2f);
            return deflected;
        }
        @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putInt("area", area); t.putInt("damage", damage); }
        @Override public void readAdditionalSaveData(CompoundTag t) { super.readAdditionalSaveData(t); area = t.getInt("area"); damage = t.getInt("damage"); }
    }

    // ---------------------------------------------------------------- BlockAiry metadata 10

    /** Invisible crackling sparks: hurt and slow whatever stands in them, fizzle on any random tick or one touch in a hundred. */
    public static final class SparkBlock extends Block {
        SparkBlock(Properties properties) { super(properties); }
        @Override protected MapCodec<? extends Block> codec() { return simpleCodec(SparkBlock::new); }
        @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Shapes.empty(); }
        @Override protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
            entity.hurt(level.damageSources().magic(), 1 + level.random.nextInt(2));
            var m = entity.getDeltaMovement();
            entity.setDeltaMovement(m.x * .8, m.y, m.z * .8);
            if (!level.isClientSide && level.random.nextInt(100) == 0) level.removeBlock(pos, false);
        }
        @Override protected void randomTick(BlockState state, net.minecraft.server.level.ServerLevel level, BlockPos pos, RandomSource random) { level.removeBlock(pos, false); }
        @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource r) {
            float h = r.nextFloat() * .33f;
            FociFx.spark(pos.getX() + level.random.nextFloat(), pos.getY() + .1515f + h / 2, pos.getZ() + level.random.nextFloat(), .33f + h,
                .65f + level.random.nextFloat() * .1f, 1, 1, .8f);
            if (r.nextInt(50) == 0) level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), FociContent.sound("jacobs"), SoundSource.BLOCKS, .5f, 1 + (r.nextFloat() - r.nextFloat()) * .2f, false);
        }
    }
}
