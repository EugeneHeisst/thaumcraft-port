package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Modern persistence adapter for the original table's two inventory slots. */
public class ResearchTableEntity extends BlockEntity {
    public final SimpleContainer inventory = new SimpleContainer(2);
    public final thaumcraft.common.tiles.ResearchTableBonus bonus = new thaumcraft.common.tiles.ResearchTableBonus();
    public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, ResearchTableEntity table) {
        if (!level.isClientSide && table.bonus.tick(new ResearchTableEnvironment(level, pos))) {
            table.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }
    public ResearchTableEntity(BlockPos pos, BlockState state) {
        super(TC4Modern.RESEARCH_TABLE_ENTITY.get(), pos, state);
        inventory.addListener(container -> {
            setChanged();
            if (level != null && !level.isClientSide)
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        });
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        net.minecraft.world.ContainerHelper.saveAllItems(tag,inventory.getItems(),registries);
        bonus.save(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.clearContent();
        net.minecraft.world.ContainerHelper.loadAllItems(tag,inventory.getItems(),registries);
        bonus.load(tag);
    }
}
