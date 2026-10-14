package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

/** Bridge for the original fixed 3 by 3 biome grass color lookup. */
public final class TaintColors {
    private TaintColors() {}

    public static int grass(BlockAndTintGetter world, BlockPos pos) {
        var level = net.minecraft.client.Minecraft.getInstance().level;
        if (world == null || pos == null || level == null) return 7160201;
        int red = 0, green = 0, blue = 0;
        for (int z = -1; z <= 1; z++) {
            for (int x = -1; x <= 1; x++) {
                var sample = pos.offset(x, 0, z);
                int color = level.getBiome(sample).value().getGrassColor(sample.getX(), sample.getZ());
                red += (color >> 16) & 255;
                green += (color >> 8) & 255;
                blue += color & 255;
            }
        }
        return (red / 9 << 16) | (green / 9 << 8) | blue / 9;
    }
}
