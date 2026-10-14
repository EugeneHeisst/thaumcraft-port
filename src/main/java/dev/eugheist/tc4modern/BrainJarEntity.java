package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** World host for the original TileJarBrain experience collection. */
public final class BrainJarEntity extends BlockEntity {
    public float rotation, previousRotation;
    private float targetRotation;
    private long nextSigh = System.currentTimeMillis() + 1500L;
    public final BrainJarState storage = new BrainJarState();
    public BrainJarEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    public void changed() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }
    public static float wrapRotation(float angle) {
        while (angle >= 3.141593f) angle -= 6.283185f;
        while (angle < -3.141593f) angle += 6.283185f;
        return angle;
    }
    public static void clientTick(Level level, BlockPos pos, BlockState state, BrainJarEntity jar) {
        jar.previousRotation = jar.rotation;
        net.minecraft.world.entity.Entity target = null;
        Vec3 center = Vec3.atCenterOf(pos);
        if (jar.storage.experience() < 2000) {
            double closest = Double.MAX_VALUE;
            for (var orb : level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(6))) {
                double distance = center.distanceToSqr(orb.position());
                if (distance < closest) { closest = distance; target = orb; }
            }
        }
        if (target == null) {
            target = level.getNearestPlayer(center.x, center.y, center.z, 6, false);
            long now = System.currentTimeMillis();
            if (target != null && jar.nextSigh < now) {
                level.playLocalSound(center.x, center.y, center.z, BrainJarContent.SIGH.get(), SoundSource.BLOCKS,
                    .15f, .8f + level.random.nextFloat() * .4f, false);
                jar.nextSigh = now + 5000L + level.random.nextInt(25000);
            }
        }
        if (target != null) jar.targetRotation = (float)Math.atan2(target.getZ() - center.z, target.getX() - center.x);
        else jar.targetRotation += .01f;
        jar.rotation = wrapRotation(jar.rotation);
        jar.targetRotation = wrapRotation(jar.targetRotation);
        jar.rotation += wrapRotation(jar.targetRotation - jar.rotation) * .04f;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState state, BrainJarEntity jar) {
        int previous = jar.storage.experience();
        jar.storage.beginTick();
        if (jar.storage.canAttract()) {
            Vec3 center = Vec3.atCenterOf(pos);
            ExperienceOrb nearest = null;
            double closest = Double.MAX_VALUE;
            for (var orb : level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(6))) {
                double distance = center.distanceToSqr(orb.position());
                if (distance < closest) { nearest = orb; closest = distance; }
            }
            if (nearest != null)
                nearest.setDeltaMovement(nearest.getDeltaMovement().add(BrainJarState.attraction(center, nearest.position())));
        }
        if (jar.storage.collectThisTick()) {
            for (var orb : level.getEntitiesOfClass(ExperienceOrb.class, new AABB(pos).inflate(0.1))) {
                // Modern orbs merge identical values into Count; original TC4 saw separate entities.
                var orbData = orb.saveWithoutId(new CompoundTag());
                jar.storage.absorb(orb.getValue() * Math.max(orbData.getInt("Count"), 1));
                level.playSound(null, orb.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.BLOCKS,
                    0.1f, (level.random.nextFloat() - level.random.nextFloat()) * 0.2f + 1);
                orb.discard();
            }
        }
        if (previous != jar.storage.experience()) jar.changed();
    }
    public void release() {
        if (level == null || level.isClientSide) return;
        int released = storage.release(level.random);
        spawnExperience(released);
        if (released > 0) changed();
    }
    public void spawnExperience(int amount) {
        spawnExperience(amount, Vec3.atCenterOf(worldPosition));
    }
    public void spawnExperience(int amount, Vec3 position) {
        if (level == null || level.isClientSide) return;
        while (amount > 0) {
            int split = ExperienceOrb.getExperienceValue(amount);
            amount -= split;
            level.addFreshEntity(new ExperienceOrb(level, position.x, position.y, position.z, split));
        }
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries); storage.save(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries); storage.load(tag);
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag(); storage.save(tag); return tag;
    }
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
