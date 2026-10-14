package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * TileEldritchAltar. Spawn type 0: after 80 ticks, every 40 ticks place four ritualist clerics at the corners, then keep
 * up to eight cultists around while any cleric remains. Spawn type 1 summons Eldritch Guardians after the third eye is inserted.
 */
public final class CrimsonAltarEntity extends BlockEntity {
    private boolean spawner, spawnedClerics, open, layoutChecked;
    private byte spawnType, eyes;
    private int counter;

    public CrimsonAltarEntity(BlockPos pos, BlockState state) { super(CrimsonContent.ALTAR_ENTITY.get(), pos, state); }

    public void setSpawner(boolean spawner) { this.spawner = spawner; setChanged(); }
    public void setSpawnType(byte type) { spawnType = type; setChanged(); }
    public byte getEyes() { return eyes; }
    public boolean isOpen(){return open;}
    public void openPortal(){open=true;setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}

    public void insertEye(){if(eyes>=4)return;if(eyes>=2){spawner=true;spawnType=1;}eyes++;if(level instanceof ServerLevel server)OuterActivation.ensureMaze(server,worldPosition);setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}

    static void tick(Level world, BlockPos pos, BlockState state, CrimsonAltarEntity altar) {
        if (!(world instanceof ServerLevel level)) return;
        if (!altar.layoutChecked) { altar.layoutChecked = true; altar.upgradeOldLayout(level); }
        if (!altar.spawner || altar.counter++ < 80 || altar.counter % 40 != 0) return;
        if (altar.spawnType == 0) {
            if (!altar.spawnedClerics) altar.spawnClerics(level);
            else altar.spawnGuards(level);
        }else if(altar.spawnType==1)altar.spawnGuardian(level);
    }

    /** 0.17.11-0.17.12 rings put a capstone at the obelisk base and four obelisk tiles above it. */
    private void upgradeOldLayout(ServerLevel level) {
        // 0.17.11-0.17.16 rings were founded on arcane stone bricks instead of obsidian tile.
        var brick = TC4Modern.ARCANE_STONE_BRICKS.get();
        for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            if (Math.abs(x) == 3 && Math.abs(z) == 3) continue;
            for (int y = -5; y <= -1; y++) {
                var at = worldPosition.offset(x, y, z);
                if (level.getBlockState(at).is(brick)) level.setBlock(at, CrimsonContent.OBSIDIAN_TILE.get().defaultBlockState(), 2);
            }
        }
        var base = worldPosition.above(2);
        if (!level.getBlockState(base).is(CrimsonContent.CAP.get())) return;
        level.setBlock(base, CrimsonContent.OBELISK.get().defaultBlockState(), 2);
        for (int y = 1; y <= 4; y++)
            if (level.getBlockState(base.above(y)).is(CrimsonContent.OBELISK.get()))
                level.setBlock(base.above(y), CrimsonContent.OBELISK_BODY.get().defaultBlockState(), 2);
    }

    private AABB around() { return new AABB(worldPosition).inflate(24, 16, 24); }

    private void spawnGuards(ServerLevel level) {
        if (level.getEntitiesOfClass(CultistEntity.Cleric.class, around()).isEmpty()) { setSpawner(false); return; }
        if (level.getEntitiesOfClass(CultistEntity.class, around()).size() >= 8) return;
        var random = level.random;
        int x = worldPosition.getX() + Mth.nextInt(random, 4, 10) * Mth.nextInt(random, -1, 1);
        int y = worldPosition.getY() + Mth.nextInt(random, 0, 3) * Mth.nextInt(random, -1, 1);
        int z = worldPosition.getZ() + Mth.nextInt(random, 4, 10) * Mth.nextInt(random, -1, 1);
        var at = new BlockPos(x, y, z);
        if (!solidBelow(level, at)) return;
        var knight = CrimsonContent.KNIGHT.get().create(level);
        if (knight == null) return;
        // setPosition(i1, j1, k1): block corner, as in the original.
        knight.moveTo(x, y, z, 0, 0);
        if (!fits(level, knight)) return;
        knight.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
        knight.spawnAnim();
        knight.restrictTo(worldPosition, 16);
        level.addFreshEntity(knight);
    }

    private void spawnGuardian(ServerLevel level){
        var random=level.random;var at=worldPosition.offset(Mth.nextInt(random,4,10)*Mth.nextInt(random,-1,1),Mth.nextInt(random,0,3)*Mth.nextInt(random,-1,1),Mth.nextInt(random,4,10)*Mth.nextInt(random,-1,1));
        if(!solidBelow(level,at)||!level.getEntitiesOfClass(EldritchGuardian.class,around().inflate(8,0,8)).isEmpty())return;var guardian=WarpEntities.ELDRITCH_GUARDIAN.get().create(level);if(guardian==null)return;
        guardian.moveTo(at.getX(),at.getY(),at.getZ(),0,0);if(!fits(level,guardian)||!guardian.checkSpawnRules(level,MobSpawnType.STRUCTURE))return;
        guardian.finalizeSpawn(level,level.getCurrentDifficultyAt(at),MobSpawnType.STRUCTURE,null);guardian.spawnAnim();guardian.restrictTo(worldPosition,16);level.addFreshEntity(guardian);
    }

    private void spawnClerics(ServerLevel level) {
        int success = 0;
        for (int[] corner : new int[][]{{-2, -2}, {-2, 2}, {2, -2}, {2, 2}}) {
            var at = worldPosition.offset(corner[0], 0, corner[1]);
            if (!solidBelow(level, at)) continue;
            var cleric = CrimsonContent.CLERIC.get().create(level);
            if (cleric == null) continue;
            cleric.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
            if (!fits(level, cleric)) continue;
            cleric.restrictTo(worldPosition, 8);
            cleric.finalizeSpawn(level, level.getCurrentDifficultyAt(at), MobSpawnType.STRUCTURE, null);
            cleric.spawnAnim();
            if (!level.addFreshEntity(cleric)) continue;
            success++;
            cleric.setRitualist(true);
        }
        if (success > 2) { spawnedClerics = true; setChanged(); }
    }

    /** World.doesBlockHaveSolidTopSurface. */
    private static boolean solidBelow(ServerLevel level, BlockPos at) {
        return level.hasChunkAt(at) && level.getBlockState(at.below()).isFaceSturdy(level, at.below(), Direction.UP);
    }
    /** checkNoEntityCollision, no block collision and no liquid. */
    private static boolean fits(ServerLevel level, Mob mob) {
        return level.isUnobstructed(mob) && level.noCollision(mob) && !level.containsAnyLiquid(mob.getBoundingBox());
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("spawnedClerics", spawnedClerics);
        tag.putBoolean("spawner", spawner);
        tag.putByte("spawntype", spawnType);
        tag.putByte("eyes", eyes);
        tag.putBoolean("open", open);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        spawnedClerics = tag.getBoolean("spawnedClerics");
        spawner = tag.getBoolean("spawner");
        spawnType = tag.getByte("spawntype");
        eyes = tag.getByte("eyes");
        open = tag.getBoolean("open");
    }
    /** readCustomNBT/writeCustomNBT: only eyes and open reach the client. */
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.putByte("eyes", eyes);
        tag.putBoolean("open", open);
        return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
