package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
/**
 * Original ThaumcraftWorldGenerator.generateOres cinnabar and amber loops: 18 single cinnabar blocks
 * below the legacy height/5 (256/5) and 20 single amber blocks up to 24 below the surface, each only
 * replacing stone.
 */
public final class CinnabarAmberFeature extends Feature<NoneFeatureConfiguration> {
    private static final int LEGACY_HEIGHT = 256;
    public CinnabarAmberFeature() { super(NoneFeatureConfiguration.CODEC); }
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        var world = context.level(); var random = context.random(); var chunk = new ChunkPos(context.origin());
        boolean placed = false;
        if(WorldgenConfig.cinnabar())for (int i = 0; i < 18; i++) {
            int x = chunk.getMinBlockX() + random.nextInt(16), y = random.nextInt(LEGACY_HEIGHT / 5), z = chunk.getMinBlockZ() + random.nextInt(16);
            var pos = new BlockPos(x, y, z);
            if (world.getBlockState(pos).is(Blocks.STONE)) placed |= world.setBlock(pos, AlchemyContent.CINNABAR_ORE.get().defaultBlockState(), 2);
        }
        if(WorldgenConfig.amber())for (int i = 0; i < 20; i++) {
            int x = chunk.getMinBlockX() + random.nextInt(16), z = chunk.getMinBlockZ() + random.nextInt(16);
            int y = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) - random.nextInt(25);
            var pos = new BlockPos(x, y, z);
            if (world.getBlockState(pos).is(Blocks.STONE)) placed |= world.setBlock(pos, AlchemyContent.AMBER_ORE.get().defaultBlockState(), 2);
        }
        return placed;
    }
}
