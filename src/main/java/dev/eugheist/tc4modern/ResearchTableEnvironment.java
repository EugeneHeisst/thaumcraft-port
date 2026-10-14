package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.piston.*;
import thaumcraft.common.tiles.ResearchTableBonus;
import thaumcraft.common.tiles.ResearchTableBonus.Block;
import thaumcraft.common.tiles.ResearchTableBonus.Material;
import thaumcraft.common.tiles.ResearchTableBonus.Sample;

/** Maps the original materials and metadata to modern blocks without loading adjacent chunks. */
public record ResearchTableEnvironment(Level level, BlockPos pos) implements ResearchTableBonus.Environment {
    @Override public boolean daytime() { return level.isDay(); }
    @Override public int lightAbove() { return level.getMaxLocalRawBrightness(pos.above()); }
    @Override public boolean skyAbove() { return level.canSeeSky(pos.above()); }
    @Override public int y() { return pos.getY(); }
    @Override public int minY() { return level.getMinBuildHeight(); }
    @Override public int maxY() { return level.getMaxBuildHeight(); }
    @Override public int nextInt(int bound) { return level.random.nextInt(bound); }
    @Override public Sample sample(int x, int y, int z) {
        BlockPos at = pos.offset(x, y, z);
        if (!level.hasChunkAt(at)) return new Sample(Block.OTHER, 0, Material.OTHER);
        var state = level.getBlockState(at);
        var block = state.getBlock();
        if(block instanceof CrystalClusterBlock crystal)return new Sample(Block.CRYSTAL,crystal.variant,Material.OTHER);
        if (block instanceof ShardOreBlock ore) return new Sample(Block.ORE, ore.variant + 1, Material.OTHER);
        if (state.is(Blocks.BOOKSHELF)) return new Sample(Block.BOOKSHELF, 0, Material.OTHER);
        if (state.is(BrainJarContent.BLOCK.get())) return new Sample(Block.BRAIN_JAR, 1, Material.OTHER);
        Material material = Material.OTHER;
        if (block instanceof BaseFireBlock) material = Material.FIRE;
        else if (state.getFluidState().is(FluidTags.LAVA)) material = Material.LAVA;
        else if (state.is(BlockTags.DIRT) || state.is(Blocks.FARMLAND) || state.is(Blocks.DIRT_PATH)) material = Material.GROUND;
        else if (state.getFluidState().is(FluidTags.WATER)) material = Material.WATER;
        else if (block instanceof PistonBaseBlock || block instanceof PistonHeadBlock || block instanceof MovingPistonBlock) material = Material.PISTON;
        else if (block instanceof RedStoneWireBlock || block instanceof DiodeBlock || block instanceof BaseRailBlock
                || block instanceof TorchBlock || block instanceof LeverBlock || block instanceof ButtonBlock
                || block instanceof TripWireBlock || block instanceof TripWireHookBlock || block instanceof PressurePlateBlock
                || block instanceof WeightedPressurePlateBlock) material = Material.CIRCUITS;
        return new Sample(Block.OTHER, 0, material);
    }
}
