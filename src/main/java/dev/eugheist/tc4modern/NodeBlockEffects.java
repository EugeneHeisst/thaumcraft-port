package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.phys.shapes.*;
import thaumcraft.api.aspects.Aspect;

/** Shared BlockAiry metadata 0 (node) / 5 (energized node) block behaviour. */
final class NodeBlockEffects {
    private NodeBlockEffects() {}
    /** Hardness 2, explosion resistance 200, light 8, silent "cloth" sound (volume 0). */
    static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().strength(2, 200).lightLevel(s -> 8).noCollission().noOcclusion().noLootTable()
            .sound(new SoundType(0, 1, net.minecraft.sounds.SoundEvents.WOOL_BREAK, net.minecraft.sounds.SoundEvents.WOOL_STEP,
                net.minecraft.sounds.SoundEvents.WOOL_PLACE, net.minecraft.sounds.SoundEvents.WOOL_HIT, net.minecraft.sounds.SoundEvents.WOOL_FALL));
    }
    /** setBlockBoundsBasedOnState: 0.3..0.7 selection box. */
    static final VoxelShape SHAPE = Shapes.box(.3, .3, .3, .7, .7, .7);
    /** addDestroyEffects: burst and craftfail sound. */
    static void destroyed(Level level, BlockPos pos) {
        if (!level.isClientSide) return;
        NodeClientEffects.burst(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
        level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5,
            net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("craftfail")), net.minecraft.sounds.SoundSource.BLOCKS, 1, 1, false);
    }
    /** harvestBlock (metadata 0 only): one wisp essence of 2 per full ten of every aspect holding at least 5. */
    static void dropWispEssence(Level level, BlockPos pos, thaumcraft.api.aspects.AspectList aspects) {
        if (level.isClientSide || aspects == null) return;
        for (Aspect aspect : aspects.getAspects()) {
            int amount = aspects.getAmount(aspect);
            if (amount < 5) continue;
            for (int i = 0; i <= amount / 10; i++)
                net.minecraft.world.level.block.Block.popResource(level, pos, WispEssenceItem.of(aspect, 2));
        }
    }
}
