package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** ItemFocusHellbat and EntityFireBat: Nether fire bats, and the focus that summons them at whatever the caster points at. */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Hellbat {
    private Hellbat() {}
    public static final DeferredHolder<EntityType<?>, EntityType<FireBat>> FIREBAT = DeferredHolder.create(Registries.ENTITY_TYPE, AlchemyContent.id("firebat"));
    public static final DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.Item> FOCUS = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("focus_hellbat"));
    static final int BATBOMBS = 13, DEVILBATS = 14, VAMPIREBATS = 19;

    @SubscribeEvent static void register(RegisterEvent event) {
        // EntityRegistry: tracked at 64 blocks every 3 ticks with velocity; 0.5 x 0.9, fire immune.
        event.register(Registries.ENTITY_TYPE, h -> h.register(FIREBAT.getId(), EntityType.Builder.of(FireBat::new, MobCategory.MONSTER)
            .sized(.5f, .9f).fireImmune().clientTrackingRange(4).updateInterval(3).build("firebat")));
        event.register(Registries.ITEM, h -> {
            h.register(FOCUS.getId(), new Focus());
            h.register(AlchemyContent.id("firebat_spawn_egg"), new net.neoforged.neoforge.common.DeferredSpawnEggItem(FIREBAT, 0x600000, 0xFF8800, new net.minecraft.world.item.Item.Properties()));
        });
    }
    @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event) {
        event.put(FIREBAT.get(), Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 5).add(Attributes.ATTACK_DAMAGE, 1).add(Attributes.MOVEMENT_SPEED, .7).build());
    }
    /** ConfigEntities: weight 10, groups of 1-2 in the Nether (biome modifier); getCanSpawnHere refuses block light above rand(7). */
    @SubscribeEvent static void spawns(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event) {
        event.register(FIREBAT.get(), SpawnPlacementTypes.NO_RESTRICTIONS, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, FireBat::canSpawn,
            net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    // ---------------------------------------------------------------- ItemFocusHellbat

    public static final class Focus extends FociContent.Focus {
        private static final AspectList COST = new AspectList().add(Aspect.FIRE, 200).add(Aspect.ENTROPY, 100).add(Aspect.AIR, 100);
        private static final AspectList COST_BOMB = new AspectList().add(Aspect.FIRE, 100).add(Aspect.ENTROPY, 200).add(Aspect.AIR, 100);
        private static final AspectList COST_DEVIL = new AspectList().add(Aspect.FIRE, 100).add(Aspect.ENTROPY, 100).add(Aspect.AIR, 100).add(Aspect.EARTH, 100);
        @Override public AspectList getVisCost(ItemStack f) { return isUpgradedWith(f, BATBOMBS) ? COST_BOMB : isUpgradedWith(f, DEVILBATS) ? COST_DEVIL : COST; }
        @Override public int getActivationCooldown(ItemStack f) { return 1000; }
        @Override public String getSortingHelperBase() { return "HH"; }
        @Override public int getFocusColor(ItemStack f) { return 14431746; }
        @Override public int[] getPossibleUpgradesByRank(ItemStack f, int rank) {
            return FociContent.byRank(rank, new int[]{1, 0}, new int[]{1, 0}, new int[]{1, 0, BATBOMBS, DEVILBATS}, new int[]{1, 0}, new int[]{1, 0, VAMPIREBATS});
        }
        @Override public boolean canApplyUpgrade(ItemStack f, Player player, int type, int rank) {
            if (type != VAMPIREBATS) return true;
            return player instanceof net.minecraft.server.level.ServerPlayer server ? ResearchProgress.completed(server.getPersistentData()).contains("VAMPBAT") : ClientKnowledgeAccess.has("VAMPBAT");
        }
        /** A fire bat appears just in front of the caster's chest and goes for the pointed living entity within 32 blocks. */
        @Override public boolean onFocusRightClick(ItemStack wand, Level level, Player player) {
            Entity pointed = pointed(player, 32);
            double px = player.getX() - Mth.cos(player.getYRot() / 180f * 3.141593f) * .16f;
            double py = player.getBoundingBox().minY + player.getBbHeight() / 2 + .25 - .05000000014901161;
            double pz = player.getZ() - Mth.sin(player.getYRot() / 180f * 3.141593f) * .16f;
            var look = player.getViewVector(1f);
            px += look.x * .5; py += look.y * .5; pz += look.z * .5;
            if (!(pointed instanceof LivingEntity target)) return true;
            if (!level.isClientSide) {
                if (pointed instanceof Player && !level.getServer().isPvpAllowed()) return true;
                var focus = FociContent.getFocusItem(wand);
                var bat = new FireBat(FIREBAT.get(), level);
                bat.moveTo(px, py + bat.getBbHeight(), pz, player.getYRot(), 0);
                bat.setTarget(target);
                bat.damBonus = FociContent.getFocusPotency(wand);
                bat.setSummoned(true);
                bat.setHanging(false);
                if (isUpgradedWith(focus, DEVILBATS)) bat.setDevil(true);
                if (isUpgradedWith(focus, BATBOMBS)) bat.setExplosive(true);
                if (isUpgradedWith(focus, VAMPIREBATS)) { bat.owner = player; bat.setVampire(true); }
                if (FociContent.consumeAllVis(wand, player, getVisCost(wand), true) && level.addFreshEntity(bat)) {
                    level.levelEvent(2004, BlockPos.containing(px, py, pz), 0);
                    level.playSound(null, bat.getX(), bat.getY(), bat.getZ(), FociContent.sound("ice"), SoundSource.PLAYERS, .2f, .95f + level.random.nextFloat() * .1f);
                } else level.playSound(null, player.getX(), player.getY(), player.getZ(), FociContent.sound("wandfail"), SoundSource.PLAYERS, .1f, .8f + level.random.nextFloat() * .1f);
            }
            player.swing(InteractionHand.MAIN_HAND);
            return true;
        }
        /** EntityUtils.getPointedEntity(world, player, range, EntityFireBat.class): ignores fire bats along the look ray. */
        static Entity pointed(Player p, double range) {
            var eye = p.getEyePosition();
            var look = p.getViewVector(1f);
            var end = eye.add(look.scale(range));
            var block = p.level().clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
            double max = block.getLocation().distanceToSqr(eye);
            var hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p, eye, eye.add(look.scale(Math.sqrt(max))), p.getBoundingBox().expandTowards(look.scale(range)).inflate(1),
                e -> !e.isSpectator() && e.isPickable() && !(e instanceof FireBat), max);
            return hit == null ? null : hit.getEntity();
        }
    }
    /** Client-side research lookup for the focal manipulator screen's canApplyUpgrade. */
    static final class ClientKnowledgeAccess {
        static boolean has(String key) {
            var data = ClientKnowledge.knowledge;
            if (data == null) return false;
            var list = data.getList("Research", 8);
            for (int i = 0; i < list.size(); i++) if (key.equals(list.getString(i))) return true;
            return false;
        }
    }

    // ---------------------------------------------------------------- EntityFireBat

    public static class FireBat extends Monster {
        private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(FireBat.class, EntityDataSerializers.BYTE);
        private BlockPos flightTarget;
        public Player owner;
        public int damBonus;
        private int attackTime;
        public FireBat(EntityType<? extends FireBat> type, Level level) { super(type, level); setHanging(true); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder b) { super.defineSynchedData(b); b.define(FLAGS, (byte) 0); }
        private boolean bit(int n) { return (entityData.get(FLAGS) >> n & 1) != 0; }
        private void bit(int n, boolean on) { byte v = entityData.get(FLAGS); entityData.set(FLAGS, (byte) (on ? v | 1 << n : v & ~(1 << n))); }
        public boolean isHanging() { return bit(0); }
        public void setHanging(boolean b) { bit(0, b); }
        public boolean isSummoned() { return bit(1); }
        public void setSummoned(boolean b) { bit(1, b); damage(); }
        public boolean isExplosive() { return bit(2); }
        public void setExplosive(boolean b) { bit(2, b); }
        public boolean isDevil() { return bit(3); }
        /** setIsDevil: 15 health instead of 5 (applyEntityAttributes runs again when the flag is read back), more bite. */
        public void setDevil(boolean b) { bit(3, b); if (b) { damage(); getAttribute(Attributes.MAX_HEALTH).setBaseValue(15); setHealth(15); } }
        public boolean isVampire() { return bit(4); }
        public void setVampire(boolean b) { bit(4, b); }
        private void damage() { getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(isSummoned() ? (isDevil() ? 3 : 2) + damBonus : 1); }

        static boolean canSpawn(EntityType<FireBat> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, RandomSource random) {
            if (level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos) > random.nextInt(7)) return false;
            return Monster.checkMonsterSpawnRules(type, level, reason, pos, random);
        }

        @Override public float getLightLevelDependentMagicValue() { return 1; }
        @Override protected float getSoundVolume() { return .1f; }
        @Override public float getVoicePitch() { return super.getVoicePitch() * .95f; }
        @Override protected SoundEvent getAmbientSound() { return isHanging() && random.nextInt(4) != 0 ? null : SoundEvents.BAT_AMBIENT; }
        @Override protected SoundEvent getHurtSound(DamageSource s) { return SoundEvents.BAT_HURT; }
        @Override protected SoundEvent getDeathSound() { return SoundEvents.BAT_DEATH; }
        @Override public boolean isPushable() { return false; }
        @Override protected void doPush(Entity e) {}
        @Override protected void pushEntities() {}
        @Override public boolean causeFallDamage(float d, float m, DamageSource s) { return false; }
        @Override protected void checkFallDamage(double y, boolean ground, net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {}
        @Override public boolean isIgnoringBlockTriggers() { return true; }
        @Override protected Entity.MovementEmission getMovementEmission() { return Entity.MovementEmission.NONE; }
        @Override public boolean removeWhenFarAway(double d) { return !isSummoned() && super.removeWhenFarAway(d); }

        @Override public void aiStep() {
            // isWet: water, rain or bubbles hurt it.
            if (isInWaterRainOrBubble()) hurt(damageSources().drown(), 1);
            super.aiStep();
        }
        @Override public void tick() {
            super.tick();
            var level = level();
            if (level.isClientSide && isExplosive())
                FociFx.generic(xo + (random.nextFloat() - random.nextFloat()) * .1f, yo + getBbHeight() / 2 + (random.nextFloat() - random.nextFloat()) * .1f, zo + (random.nextFloat() - random.nextFloat()) * .1f,
                    0, 0, 0, 1, 1, 1, .8f, false, 151, 9, 1, 7 + random.nextInt(5), 0, 1 + random.nextFloat() * .5f);
            if (isHanging()) {
                setDeltaMovement(0, 0, 0);
                setPos(getX(), Mth.floor(getY()) + 1.0 - getBbHeight(), getZ());
            } else setDeltaMovement(getDeltaMovement().multiply(1, .6, 1));
            if (level.isClientSide && !isVampire()) {
                var r = level.random;
                level.addParticle(ParticleTypes.SMOKE, xo + (r.nextFloat() - r.nextFloat()) * .2f, yo + getBbHeight() / 2 + (r.nextFloat() - r.nextFloat()) * .2f, zo + (r.nextFloat() - r.nextFloat()) * .2f, 0, 0, 0);
                level.addParticle(ParticleTypes.FLAME, xo + (r.nextFloat() - r.nextFloat()) * .2f, yo + getBbHeight() / 2 + (r.nextFloat() - r.nextFloat()) * .2f, zo + (r.nextFloat() - r.nextFloat()) * .2f, 0, 0, 0);
            }
        }

        /** EntityCreature.updateEntityActionState then EntityFireBat's own flight, every server tick. */
        @Override protected void customServerAiStep() {
            super.customServerAiStep();
            if (attackTime > 0) --attackTime;
            var target = getTarget();
            if (target == null) {
                if (!isSummoned()) { var p = level().getNearestPlayer(this, 12); if (p != null && !p.getAbilities().invulnerable && !p.isSpectator()) setTarget(target = p); }
            } else if (!target.isAlive()) setTarget(target = null);
            else if (hasLineOfSight(target)) attack(target, distanceTo(target));
            var level = level();
            var above = BlockPos.containing(getX(), (int) getY() + 1, getZ());
            if (isHanging()) {
                if (!level.getBlockState(above).isRedstoneConductor(level, above)) {
                    setHanging(false);
                    level.levelEvent(null, 1025, blockPosition(), 0);
                } else {
                    if (random.nextInt(200) == 0) yHeadRot = random.nextInt(360);
                    if (level.getNearestPlayer(this, 4) != null) { setHanging(false); level.levelEvent(null, 1025, blockPosition(), 0); }
                }
                return;
            }
            double dx, dy, dz;
            if (target == null) {
                if (isSummoned()) hurt(damageSources().generic(), 2);
                if (flightTarget != null && (!level.isEmptyBlock(flightTarget) || flightTarget.getY() < 1)) flightTarget = null;
                if (flightTarget == null || random.nextInt(30) == 0 || flightTarget.distToCenterSqr((int) getX(), (int) getY(), (int) getZ()) < 4)
                    flightTarget = new BlockPos((int) getX() + random.nextInt(7) - random.nextInt(7), (int) getY() + random.nextInt(6) - 2, (int) getZ() + random.nextInt(7) - random.nextInt(7));
                dx = flightTarget.getX() + .5 - getX(); dy = flightTarget.getY() + .1 - getY(); dz = flightTarget.getZ() + .5 - getZ();
            } else {
                dx = target.getX() - getX(); dy = target.getY() + target.getEyeHeight() * .66f - getY(); dz = target.getZ() - getZ();
            }
            var m = getDeltaMovement();
            double mx = m.x + (Math.signum(dx) * .5 - m.x) * .1f, my = m.y + (Math.signum(dy) * .7f - m.y) * .1f, mz = m.z + (Math.signum(dz) * .5 - m.z) * .1f;
            setDeltaMovement(mx, my, mz);
            float yaw = (float) (Math.atan2(mz, mx) * 180 / Math.PI) - 90;
            zza = .5f;
            setYRot(getYRot() + Mth.wrapDegrees(yaw - getYRot()));
            if (target == null && random.nextInt(100) == 0 && level.getBlockState(above).isRedstoneConductor(level, above)) setHanging(true);
            if (target instanceof Player p && p.getAbilities().invulnerable) setTarget(null);
        }

        /** attackEntity: in reach and vertically overlapping, once a second; explode, bite (keeping the victim's motion) or set alight. */
        private void attack(LivingEntity e, float dist) {
            if (attackTime > 0 || dist >= Math.max(2.5f, e.getBbWidth() * 1.1f) || e.getBoundingBox().maxY <= getBoundingBox().minY || e.getBoundingBox().minY >= getBoundingBox().maxY) return;
            if (isSummoned()) e.setLastHurtByMob(this);
            if (isVampire()) {
                if (owner != null && !owner.hasEffect(MobEffects.REGENERATION)) owner.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 26, 1));
                heal(1);
            }
            attackTime = 20;
            var level = level();
            if ((isExplosive() || level.random.nextInt(10) == 0) && !isDevil()) {
                e.invulnerableTime = 0;
                level.explode(this, getX(), getY(), getZ(), 1.5f + (isExplosive() ? damBonus * .33f : 0), false, Level.ExplosionInteraction.NONE);
                discard();
            } else if (isVampire() || level.random.nextBoolean()) {
                var keep = e.getDeltaMovement();
                doHurtTarget(e);
                e.hasImpulse = false;
                e.setDeltaMovement(keep);
                e.hurtMarked = false;
            } else e.igniteForSeconds(isSummoned() ? 4 : 2);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.BAT_HURT, SoundSource.HOSTILE, .5f, .9f + level.random.nextFloat() * .2f);
        }

        @Override public boolean hurt(DamageSource source, float amount) {
            if (isInvulnerableTo(source) || source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.IS_EXPLOSION)) return false;
            if (!level().isClientSide && isHanging()) setHanging(false);
            return super.hurt(source, amount);
        }
        @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recent) {
            super.dropCustomDeathLoot(level, source, recent);
            // getDropItem: gunpowder (0-2, looting adds) unless summoned.
            if (!isSummoned()) { int n = random.nextInt(3); if (n > 0) spawnAtLocation(new ItemStack(net.minecraft.world.item.Items.GUNPOWDER, n)); }
        }
        @Override public void addAdditionalSaveData(CompoundTag t) { super.addAdditionalSaveData(t); t.putByte("BatFlags", entityData.get(FLAGS)); t.putByte("damBonus", (byte) damBonus); }
        @Override public void readAdditionalSaveData(CompoundTag t) {
            super.readAdditionalSaveData(t);
            entityData.set(FLAGS, t.getByte("BatFlags"));
            damBonus = t.getByte("damBonus");
            damage();
            if (isDevil()) getAttribute(Attributes.MAX_HEALTH).setBaseValue(15);
        }
    }
}
