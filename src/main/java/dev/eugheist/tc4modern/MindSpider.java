package dev.eugheist.tc4modern;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;

/** Original EntityMindSpider: tiny real spiders and viewer-bound harmless hallucinations. */
public final class MindSpider extends Spider {
    private static final EntityDataAccessor<Byte> HARMLESS = SynchedEntityData.defineId(MindSpider.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<String> VIEWER = SynchedEntityData.defineId(MindSpider.class, EntityDataSerializers.STRING);
    private int lifeSpan = Integer.MAX_VALUE;
    public MindSpider(EntityType<? extends MindSpider> type, Level level) {
        super(type, level);
        xpReward = 1;
    }
    public static AttributeSupplier.Builder attributes() {
        return Spider.createAttributes().add(Attributes.MAX_HEALTH, 1).add(Attributes.ATTACK_DAMAGE, 1)
            .add(Attributes.FOLLOW_RANGE, 12);
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(1, new FloatGoal(this));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, .4f));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1, false));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, .8));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // Original override searches a vulnerable player within 12, without a darkness gate.
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, null));
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(HARMLESS, (byte) 0);
        builder.define(VIEWER, "");
    }
    public boolean harmless() { return entityData.get(HARMLESS) != 0; }
    public String viewer() { return entityData.get(VIEWER); }
    public void setViewer(String name) { entityData.set(VIEWER, name); }
    public void setHarmless(boolean value) {
        if (value) lifeSpan = 1200;
        entityData.set(HARMLESS, (byte) (value ? 1 : 0));
    }
    @Override public void tick() {
        super.tick();
        if (!level().isClientSide && tickCount > lifeSpan) discard();
    }
    @Override public boolean doHurtTarget(Entity target) { return !harmless() && super.doHurtTarget(target); }
    @Override protected int getBaseExperienceReward() { return harmless() ? 0 : super.getBaseExperienceReward(); }
    @Override protected float getSoundVolume() { return harmless() ? 0 : .1f; }
    @Override public boolean isIgnoringBlockTriggers() { return true; }
    @Override protected MovementEmission getMovementEmission() { return MovementEmission.NONE; }
    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource damage, boolean recentlyHit) {}
    @Override protected void dropFromLootTable(DamageSource source, boolean recentlyHit) {}
    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("harmless", entityData.get(HARMLESS));
        tag.putString("viewer", viewer());
    }
    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        // Original load restores watcher values directly, not setHarmless/lifespan.
        entityData.set(HARMLESS, tag.getByte("harmless"));
        entityData.set(VIEWER, tag.getString("viewer"));
    }
}
