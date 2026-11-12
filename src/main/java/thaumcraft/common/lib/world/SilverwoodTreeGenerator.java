package thaumcraft.common.lib.world;
import java.util.Random;
/**
 * Original WorldGenSilverwoodTrees (TC4 4.2.3.5) with injected world bindings.
 * Metadata follows BlockMagicalLog: 1 silverwood (Y axis), 5 X axis, 9 Z axis, 2 knot with node;
 * leaves meta 1 = silverwood. The knot callback replaces createRandomNodeAt(..., silverwood=true).
 */
public class SilverwoodTreeGenerator {
    public enum Block { AIR, LEAVES, LOG, OTHER }
    public interface World {
        /** isAir || isLeaves || isReplaceable */
        boolean passable(int x, int y, int z);
        boolean soil(int x, int y, int z);
        boolean canBeReplacedByLeaves(int x, int y, int z);
        void log(int x, int y, int z, int metadata);
        void leaves(int x, int y, int z);
        void knotNode(int x, int y, int z, Random random);
        void shimmerleaf(int x, int y, int z, Random random);
    }
    private final int minTreeHeight, randomTreeHeight;
    private final boolean worldgen;
    public SilverwoodTreeGenerator(boolean doBlockNotify, int minTreeHeight, int randomTreeHeight) {
        this.worldgen = !doBlockNotify;
        this.minTreeHeight = minTreeHeight;
        this.randomTreeHeight = randomTreeHeight;
    }
    private static int clamp(int v, int lo, int hi) { return v < lo ? lo : (v > hi ? hi : v); }
    public boolean generate(World world, Random random, int x, int y, int z, int minY, int maxY) {
        int height = random.nextInt(this.randomTreeHeight) + this.minTreeHeight;
        boolean flag = true;
        if (y >= minY + 1 && y + height + 1 <= maxY) {
            for (int i1 = y; i1 <= y + 1 + height; ++i1) {
                int spread = 1;
                if (i1 == y) spread = 0;
                if (i1 >= y + 1 + height - 2) spread = 3;
                for (int j1 = x - spread; j1 <= x + spread && flag; ++j1) {
                    for (int k1 = z - spread; k1 <= z + spread && flag; ++k1) {
                        if (i1 >= minY && i1 < maxY) {
                            if (world.passable(j1, i1, k1) || i1 <= y) continue;
                            flag = false;
                            continue;
                        }
                        flag = false;
                    }
                }
            }
            if (!flag) return false;
            boolean isSoil = world.soil(x, y - 1, z);
            if (isSoil && y < maxY - height - 1) {
                int k2;
                int start = y + height - 5;
                int end = y + height + 3 + random.nextInt(3);
                for (k2 = start; k2 <= end; ++k2) {
                    int cty = clamp(k2, y + height - 3, y + height);
                    for (int xx = x - 5; xx <= x + 5; ++xx) {
                        for (int zz = z - 5; zz <= z + 5; ++zz) {
                            double d3 = xx - x, d4 = k2 - cty, d5 = zz - z;
                            double dist = d3 * d3 + d4 * d4 + d5 * d5;
                            if (!(dist < (double)(10 + random.nextInt(8))) || !world.canBeReplacedByLeaves(xx, k2, zz)) continue;
                            world.leaves(xx, k2, zz);
                        }
                    }
                }
                int chance = (int)((double)height * 1.5);
                boolean lastblock = false;
                for (k2 = 0; k2 < height; ++k2) {
                    if (!world.passable(x, y + k2, z)) continue;
                    if (k2 > 0 && !lastblock && random.nextInt(chance) == 0) {
                        world.log(x, y + k2, z, 2);
                        world.knotNode(x, y + k2, z, random);
                        chance += height;
                        lastblock = true;
                    } else {
                        world.log(x, y + k2, z, 1);
                        lastblock = false;
                    }
                    world.log(x - 1, y + k2, z, 1);
                    world.log(x + 1, y + k2, z, 1);
                    world.log(x, y + k2, z - 1, 1);
                    world.log(x, y + k2, z + 1, 1);
                }
                world.log(x, y + k2, z, 1);
                world.log(x - 1, y, z - 1, 1);
                world.log(x + 1, y, z + 1, 1);
                world.log(x - 1, y, z + 1, 1);
                world.log(x + 1, y, z - 1, 1);
                if (random.nextInt(3) != 0) world.log(x - 1, y + 1, z - 1, 1);
                if (random.nextInt(3) != 0) world.log(x + 1, y + 1, z + 1, 1);
                if (random.nextInt(3) != 0) world.log(x - 1, y + 1, z + 1, 1);
                if (random.nextInt(3) != 0) world.log(x + 1, y + 1, z - 1, 1);
                world.log(x - 2, y, z, 5);
                world.log(x + 2, y, z, 5);
                world.log(x, y, z - 2, 9);
                world.log(x, y, z + 2, 9);
                world.log(x - 2, y - 1, z, 1);
                world.log(x + 2, y - 1, z, 1);
                world.log(x, y - 1, z - 2, 1);
                world.log(x, y - 1, z + 2, 1);
                world.log(x - 1, y + (height - 4), z - 1, 1);
                world.log(x + 1, y + (height - 4), z + 1, 1);
                world.log(x - 1, y + (height - 4), z + 1, 1);
                world.log(x + 1, y + (height - 4), z - 1, 1);
                if (random.nextInt(3) == 0) world.log(x - 1, y + (height - 5), z - 1, 1);
                if (random.nextInt(3) == 0) world.log(x + 1, y + (height - 5), z + 1, 1);
                if (random.nextInt(3) == 0) world.log(x - 1, y + (height - 5), z + 1, 1);
                if (random.nextInt(3) == 0) world.log(x + 1, y + (height - 5), z - 1, 1);
                world.log(x - 2, y + (height - 4), z, 5);
                world.log(x + 2, y + (height - 4), z, 5);
                world.log(x, y + (height - 4), z - 2, 9);
                world.log(x, y + (height - 4), z + 2, 9);
                if (this.worldgen) {
                    // WorldGenCustomFlowers(blockCustomPlant, 2): 18 shimmerleaf attempts.
                    for (int var6 = 0; var6 < 18; ++var6) {
                        int var7 = x + random.nextInt(8) - random.nextInt(8);
                        int var8 = y + random.nextInt(4) - random.nextInt(4);
                        int var9 = z + random.nextInt(8) - random.nextInt(8);
                        world.shimmerleaf(var7, var8, var9, random);
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }
}
