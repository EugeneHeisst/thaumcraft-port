package dev.eugheist.tc4modern;

import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;

/** EntityCultist: Crimson Cult base mob; subclasses EntityCultistKnight and EntityCultistCleric. */
public abstract class CultistEntity extends Monster {
    protected CultistEntity(EntityType<? extends CultistEntity> type, Level level) {
        super(type, level);
        xpReward = 10;
        if (getNavigation() instanceof GroundPathNavigation ground) { ground.setCanOpenDoors(true); ground.setCanPassDoors(true); }
    }

    public static AttributeSupplier.Builder attributes(double health) {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.FOLLOW_RANGE, 32)
            .add(Attributes.MOVEMENT_SPEED, .3).add(Attributes.ATTACK_DAMAGE, 4);
    }

    /** Goals shared by both cultists, at the original priorities. */
    protected void commonGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(3, new MeleeAttackGoal(this, 1, false));
        goalSelector.addGoal(5, new OpenDoorGoal(this, true));
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, .8));
        goalSelector.addGoal(7, new RandomStrollGoal(this, .8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByCultistTarget(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, true, false, null));
    }

    /** dropFewItems: 10% knowledge fragment, 10% void seed, otherwise (3 + looting)/10 a coin. */
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        int looting = source.getEntity() instanceof LivingEntity killer
            ? net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantmentLevel(registryAccess()
                .lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.LOOTING), killer) : 0;
        int r = random.nextInt(10);
        if (r == 0) spawnAtLocation(new ItemStack(BasicsContent.KNOWLEDGE_FRAGMENT.get()), 1.5f);
        else if (r <= 1) spawnAtLocation(PechTrades.mod("void_seed"), 1.5f);
        else if (r <= 3 + looting) spawnAtLocation(PechTrades.mod("coin"), 1.5f);
        // EntityLivingBase.onDeath: rare drop when rand(200) - looting < 5 after a player hit.
        if (recentlyHit && random.nextInt(200) - looting < 5) spawnAtLocation(new ItemStack(BasicsContent.CRIMSON_RITES.get()), 1);
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        var result = super.finalizeSpawn(level, difficulty, reason, data);
        // Monster.finalizeSpawn already rolled vanilla gear; cultists only wear what addRandomArmor gives them.
        for (var slot : EquipmentSlot.values()) setItemSlot(slot, ItemStack.EMPTY);
        equip(level.getLevel().getDifficulty());
        enchant(difficulty.getSpecialMultiplier());
        return result;
    }
    @Override protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {}
    protected abstract void equip(Difficulty difficulty);
    protected void enchant(float tension) {}

    /** canDespawn: true, except ritualist clerics. */
    @Override public boolean removeWhenFarAway(double distance) { return true; }

    /** isOnSameTeam: cultists (and the Praetor) are allies. */
    @Override public boolean isAlliedTo(Entity entity) { return entity instanceof CultistEntity ||entity instanceof CultistLeader|| super.isAlliedTo(entity); }
    @Override public boolean canAttackType(EntityType<?> type) {
        return type != CrimsonContent.KNIGHT.get() && type != CrimsonContent.CLERIC.get() &&type!=OuterBossContent.LEADER.get()&& super.canAttackType(type);
    }

    /** HomeX/Y/Z/D, saved only when a home is set. */
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (hasRestriction() && getRestrictRadius() > 0) {
            tag.putInt("HomeD", (int) getRestrictRadius());
            tag.putInt("HomeX", getRestrictCenter().getX());
            tag.putInt("HomeY", getRestrictCenter().getY());
            tag.putInt("HomeZ", getRestrictCenter().getZ());
        }
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("HomeD")) restrictTo(new BlockPos(tag.getInt("HomeX"), tag.getInt("HomeY"), tag.getInt("HomeZ")), tag.getInt("HomeD"));
    }

    protected static SoundEvent sound(String name) { return SoundEvent.createVariableRangeEvent(AlchemyContent.id(name)); }

    /** EntityCultistKnight: 36 health, plate armour, iron sword, rarely a thaumium or void sword. */
    public static final class Knight extends CultistEntity {
        public Knight(EntityType<? extends Knight> type, Level level) { super(type, level); }
        @Override protected void registerGoals() { commonGoals(); }
        @Override protected void equip(Difficulty difficulty) {
            setItemSlot(EquipmentSlot.HEAD, PechTrades.mod("cultist_plate_helmet"));
            setItemSlot(EquipmentSlot.CHEST, PechTrades.mod("cultist_plate_chestplate"));
            setItemSlot(EquipmentSlot.LEGS, PechTrades.mod("cultist_plate_leggings"));
            setItemSlot(EquipmentSlot.FEET, PechTrades.mod("cultist_boots"));
            if (random.nextFloat() < (difficulty == Difficulty.HARD ? .05f : .01f)) {
                if (random.nextInt(5) == 0) {
                    // The void sword is not ported yet; the thaumium sword stands in for it.
                    var voidSword = AlchemyContent.id("void_sword");
                    setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(voidSword)
                        ? new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(voidSword)) : PechTrades.mod("thaumium_sword"));
                    setItemSlot(EquipmentSlot.HEAD, PechTrades.mod("cultist_robe_helmet"));
                } else {
                    setItemSlot(EquipmentSlot.MAINHAND, PechTrades.mod("thaumium_sword"));
                    if (random.nextBoolean()) setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
                }
            } else setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        }
        /** enchantEquipment: 25% * tension chance for level 5 + tension * rand(18). */
        @Override protected void enchant(float tension) {
            var held = getMainHandItem();
            if (held.isEmpty() || random.nextFloat() >= .25f * tension) return;
            setItemSlot(EquipmentSlot.MAINHAND, net.minecraft.world.item.enchantment.EnchantmentHelper.enchantItem(random, held,
                (int) (5 + tension * random.nextInt(18)), registryAccess(), Optional.empty()));
        }
    }

    /** EntityCultistCleric: 30 health, robes, fireball volleys or a red shock orb, and the altar ritual. */
    public static final class Cleric extends CultistEntity implements RangedAttackMob, IEntityWithComplexSpawn {
        private static final EntityDataAccessor<Byte> FLAGS = SynchedEntityData.defineId(Cleric.class, EntityDataSerializers.BYTE);
        public Cleric(EntityType<? extends Cleric> type, Level level) { super(type, level); }
        @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
            super.defineSynchedData(builder);
            builder.define(FLAGS, (byte) 0);
        }
        public boolean isRitualist() { return (entityData.get(FLAGS) & 1) != 0; }
        public void setRitualist(boolean ritualist) {
            byte flags = entityData.get(FLAGS);
            entityData.set(FLAGS, (byte) (ritualist ? flags | 1 : flags & ~1));
        }

        @Override protected void registerGoals() {
            commonGoals();
            goalSelector.addGoal(1, new AltarFocus(this));
            goalSelector.addGoal(2, new LongRangeAttack(this, 2, 1, 20, 40, 24));
        }
        @Override protected void equip(Difficulty difficulty) {
            setItemSlot(EquipmentSlot.HEAD, PechTrades.mod("cultist_robe_helmet"));
            setItemSlot(EquipmentSlot.CHEST, PechTrades.mod("cultist_robe_chestplate"));
            setItemSlot(EquipmentSlot.LEGS, PechTrades.mod("cultist_robe_leggings"));
            if (random.nextFloat() < (difficulty == Difficulty.HARD ? .3f : .1f)) setItemSlot(EquipmentSlot.FEET, PechTrades.mod("cultist_boots"));
        }

        /** attackEntityWithRangedAttack: a third of the time a red golem orb, otherwise three small fireballs. */
        @Override public void performRangedAttack(LivingEntity target, float distanceFactor) {
            double dx = target.getX() - getX();
            double dy = target.getBoundingBox().minY + target.getBbHeight() / 2 - (getY() + getBbHeight() / 2);
            double dz = target.getZ() - getZ();
            swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            if (random.nextFloat() > .66f) {
                var orb = new GolemOrb(level(), this, target, true);
                var motion = orb.getDeltaMovement();
                orb.setPos(orb.getX() + motion.x / 2, orb.getY(), orb.getZ() + motion.z / 2);
                orb.shoot(dx, dy + 2, dz, .66f, 3);
                playSound(sound("egattack"), 1, 1 + random.nextFloat() * .1f);
                level().addFreshEntity(orb);
            } else {
                float spread = Mth.sqrt(distanceFactor) * .5f;
                level().levelEvent(null, net.minecraft.world.level.block.LevelEvent.SOUND_BLAZE_FIREBALL, blockPosition(), 0);
                for (int i = 0; i < 3; i++) {
                    var fireball = new SmallFireball(level(), this, new Vec3(dx + random.nextGaussian() * spread, dy, dz + random.nextGaussian() * spread).normalize());
                    fireball.setPos(fireball.getX(), getY() + getBbHeight() / 2 + .5, fireball.getZ());
                    level().addFreshEntity(fireball);
                }
            }
        }

        @Override public boolean removeWhenFarAway(double distance) { return !isRitualist(); }

        @Override public boolean hurt(DamageSource source, float amount) {
            if (isInvulnerableTo(source)) return false;
            setRitualist(false);
            return super.hurt(source, amount);
        }

        @Override public void addAdditionalSaveData(CompoundTag tag) { super.addAdditionalSaveData(tag); tag.putByte("Flags", entityData.get(FLAGS)); }
        @Override public void readAdditionalSaveData(CompoundTag tag) { super.readAdditionalSaveData(tag); entityData.set(FLAGS, tag.getByte("Flags")); }

        /** writeSpawnData: the altar position, so the client can aim the ritual and its floating line. */
        @Override public void writeSpawnData(RegistryFriendlyByteBuf buffer) { buffer.writeBlockPos(getRestrictCenter()); }
        @Override public void readSpawnData(RegistryFriendlyByteBuf buffer) { restrictTo(buffer.readBlockPos(), 8); }

        /** onUpdate (client): a ritualist keeps turning its head toward the altar top. */
        @Override public void tick() {
            super.tick();
            if (!level().isClientSide || !isRitualist()) return;
            var home = getRestrictCenter();
            double d0 = home.getX() + .5 - getX(), d1 = home.getY() + 1.5 - (getY() + getEyeHeight()), d2 = home.getZ() + .5 - getZ();
            double d3 = Math.sqrt(d0 * d0 + d2 * d2);
            float yaw = (float) (Math.atan2(d2, d0) * 180 / Math.PI) - 90, pitch = (float) -(Math.atan2(d1, d3) * 180 / Math.PI);
            setXRot(turn(getXRot(), pitch, 10));
            yHeadRot = turn(yHeadRot, yaw, getMaxHeadYRot());
        }
        private static float turn(float from, float to, float max) { return from + Mth.clamp(Mth.wrapDegrees(to - from), -max, max); }

        @Override protected SoundEvent getAmbientSound() { return sound("chant"); }
        @Override public int getAmbientSoundInterval() { return 500; }
    }

    /** AIAltarFocus: holds a ritualist still; drops the ritual if it strays over 4 blocks or the altar is gone. */
    static final class AltarFocus extends Goal {
        private final Cleric cleric;
        AltarFocus(Cleric cleric) { this.cleric = cleric; setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP)); }
        @Override public boolean canUse() { return cleric.isRitualist() && cleric.hasRestriction(); }
        @Override public boolean canContinueToUse() { return canUse(); }
        @Override public boolean requiresUpdateEveryTick() { return true; }
        @Override public void tick() {
            if (cleric.tickCount % 40 != 0) return;
            var home = cleric.getRestrictCenter();
            if (home.distSqr(cleric.blockPosition()) > 16 || !(cleric.level().getBlockState(home).getBlock() instanceof EldritchBlock))
                cleric.setRitualist(false);
        }
    }

    /** AILongRangeAttack: ranged attack that refuses targets closer than the minimum distance. */
    static final class LongRangeAttack extends RangedAttackGoal {
        private final Mob wielder;
        private final double min;
        LongRangeAttack(Cleric cleric, double min, double speed, int minInterval, int maxInterval, float range) {
            super(cleric, speed, minInterval, maxInterval, range);
            wielder = cleric;
            this.min = min;
        }
        @Override public boolean canUse() {
            if (!super.canUse()) return false;
            var target = wielder.getTarget();
            if (target == null) return false;
            if (!target.isAlive()) { wielder.setTarget(null); return false; }
            return wielder.distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) >= min * min;
        }
    }

    /** AICultistHurtByTarget: retaliate and rally idle cultists nearby; a ritualist cleric breaks off only one time in three. */
    static final class HurtByCultistTarget extends TargetGoal {
        private int lastHurtTimestamp;
        HurtByCultistTarget(CultistEntity mob) { super(mob, false); setFlags(EnumSet.of(Flag.TARGET)); }
        @Override public boolean canUse() {
            return mob.getLastHurtByMobTimestamp() != lastHurtTimestamp && canAttack(mob.getLastHurtByMob(), TargetingConditions.DEFAULT);
        }
        @Override public void start() {
            var attacker = mob.getLastHurtByMob();
            mob.setTarget(attacker);
            lastHurtTimestamp = mob.getLastHurtByMobTimestamp();
            double range = getFollowDistance();
            var box = new net.minecraft.world.phys.AABB(mob.getX(), mob.getY(), mob.getZ(), mob.getX() + 1, mob.getY() + 1, mob.getZ() + 1).inflate(range, 10, range);
            for (var other : mob.level().getEntitiesOfClass(CultistEntity.class, box)) {
                if (other == mob || other.getTarget() != null || other.isAlliedTo(attacker)) continue;
                if (other instanceof Cleric cleric && cleric.isRitualist()) {
                    if (mob.level().random.nextInt(3) != 0) continue;
                    cleric.setRitualist(false);
                }
                other.setTarget(attacker);
            }
            super.start();
        }
    }
}
