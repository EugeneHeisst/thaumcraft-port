package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
/** Nine ingredient slots and wand slot; computed output is never persisted. */
public final class ArcaneWorkbenchEntity extends BlockEntity {
    public final SimpleContainer inventory=new SimpleContainer(10);
    public ArcaneWorkbenchEntity(BlockPos pos,BlockState state){super(TC4Modern.ARCANE_ENTITY.get(),pos,state);inventory.addListener(c->{setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);});}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);net.minecraft.world.ContainerHelper.saveAllItems(tag,inventory.getItems(),registries);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);inventory.clearContent();net.minecraft.world.ContainerHelper.loadAllItems(tag,inventory.getItems(),registries);}
}
