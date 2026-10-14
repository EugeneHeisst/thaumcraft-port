package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * WorldGenEldritchRing plus its ThaumcraftWorldGenerator call: a 7x7 ring of obsidian tile and obsidian with capstones,
 * the altar under a floating obelisk and an eerie node between them. 40% of altars summon clerics and get four banners.
 */
public final class CrimsonRingFeature extends Feature<NoneFeatureConfiguration> {
    public CrimsonRingFeature() { super(NoneFeatureConfiguration.CODEC); }

    /** GetValidSpawnBlocks: stone, sand, packed ice, grass, gravel, dirt. */
    private static boolean validGround(BlockState state) {
        return state.is(Blocks.STONE) || state.is(BlockTags.SAND) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.GRASS_BLOCK)
            || state.is(Blocks.GRAVEL) || state.is(Blocks.DIRT) || state.is(Blocks.COARSE_DIRT) || state.is(Blocks.PODZOL);
    }

    /** LocationIsValidSpawn: climb at most two blocks to air, then the top must be valid ground (or snow/grass on it). */
    private static boolean validSpawn(WorldGenLevel level, int x, int y, int z) {
        int up = 0;
        while (!level.getBlockState(new BlockPos(x, y + up, z)).isAir()) if (++up > 2) return false;
        var top = new BlockPos(x, y + up - 1, z);
        if (!level.getBlockState(top.above()).isAir()) return false;
        var block = level.getBlockState(top);
        if (validGround(block)) return true;
        return (block.is(Blocks.SNOW) || block.is(Blocks.SHORT_GRASS) || block.is(Blocks.TALL_GRASS) || block.is(Blocks.FERN))
            && validGround(level.getBlockState(top.below()));
    }

    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        if(!WorldgenConfig.structures())return false;
        var level = context.level();
        var random = context.random();
        // randPosY = heightValue - 9 + 8: the topmost light-blocking block.
        var center = context.origin().below();
        int i = center.getX(), j = center.getY(), k = center.getZ();
        if (!(validSpawn(level, i - 3, j, k - 3) && validSpawn(level, i, j, k) && validSpawn(level, i + 3, j, k)
            && validSpawn(level, i + 3, j, k + 3) && validSpawn(level, i, j, k + 3))) return false;
        // blockCosmeticSolid metadata 1 (obsidian tile), with one in four plain obsidian.
        var brick = CrimsonContent.OBSIDIAN_TILE.get().defaultBlockState();
        for (int x = i - 3; x <= i + 3; x++) for (int z = k - 3; z <= k + 3; z++) {
            if ((x == i - 3 || x == i + 3) && (z == k - 3 || z == k + 3)) continue;
            for (int q = -4; q < 5; q++) {
                var at = new BlockPos(x, j + q, z);
                var state = level.getBlockState(at);
                boolean soft = state.canBeReplaced() || !state.blocksMotion() || state.is(BlockTags.LEAVES);
                if (q <= 0 || soft) set(level, at, random.nextInt(4) == 0 ? Blocks.OBSIDIAN.defaultBlockState() : brick);
                if (q > 0) set(level, at, Blocks.AIR.defaultBlockState());
            }
            if (x == i && z == k) {
                set(level, new BlockPos(x, j + 1, z), CrimsonContent.ALTAR.get().defaultBlockState());
                set(level, new BlockPos(x, j, z), brick);
                int r = random.nextInt(10);
                if (level.getBlockEntity(new BlockPos(x, j + 1, z)) instanceof CrimsonAltarEntity altar) {
                    if (r >= 1 && r <= 4) {
                        altar.setSpawner(true);
                        altar.setSpawnType((byte) 0);
                        // Banners on the four side midpoints, all facing the altar.
                        for (var dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                            var at = new BlockPos(x - dir.getStepX() * 3, j + 1, z + dir.getStepZ() * 3);
                            set(level, at, CrimsonContent.BANNER.get().defaultBlockState());
                            if (level.getBlockEntity(at) instanceof CrimsonBanner.Entity banner)
                                banner.setFacing((byte) switch (dir) { case NORTH -> 8; case SOUTH -> 0; case WEST -> 12; default -> 4; });
                        }
                    } else if (r == 6 || r == 7) {
                        altar.setSpawner(true);
                        altar.setSpawnType((byte) 1);
                    }
                }
                set(level, new BlockPos(x, j + 3, z), CrimsonContent.OBELISK.get().defaultBlockState());
                for (int y = 4; y <= 7; y++) set(level, new BlockPos(x, j + y, z), CrimsonContent.OBELISK_BODY.get().defaultBlockState());
                continue;
            }
            boolean side = (x == i - 3 || x == i + 3) && Math.abs((z - k) % 2) == 1 || (z == k - 3 || z == k + 3) && Math.abs((x - i) % 2) == 1;
            if (!side || Math.abs(x - i) == Math.abs(z - k)) continue;
            set(level, new BlockPos(x, j, z), brick);
            set(level, new BlockPos(x, j + 1, z), CrimsonContent.CAP.get().defaultBlockState());
        }
        AuraNodeFeature.createRandomNodeAt(level, new BlockPos(i, j + 2, k), random, false, true, false);
        return true;
    }

    private static void set(WorldGenLevel level, BlockPos at, BlockState state) { level.setBlock(at, state, Block.UPDATE_ALL); }
}
