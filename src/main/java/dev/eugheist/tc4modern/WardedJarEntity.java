package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.essentia.JarState;

/** Modern host for the original TileJarFillable. */
public final class WardedJarEntity extends BlockEntity implements thaumcraft.api.aspects.IEssentiaTransport {
    public final JarState state;
    public WardedJarEntity(BlockPos pos, BlockState blockState) { super(EssentiaDevices.JAR_ENTITY.get(), pos, blockState); state = blockState.is(AlchemyMachines.VOID_JAR.get()) ? new thaumcraft.common.tiles.essentia.VoidJarState() : new JarState(); }
    public void changed() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, WardedJarEntity tile) {
        if (tile.state.tick(EssentiaTransport.connectable(level, pos, Direction.UP))) tile.changed();
    }
    // IEssentiaTransport: the top face only.
    public boolean isConnectable(Direction face) { return face == Direction.UP; }
    public boolean canInputFrom(Direction face) { return face == Direction.UP; }
    public boolean canOutputTo(Direction face) { return face == Direction.UP; }
    public void setSuction(Aspect aspect, int amount) {}
    public Aspect getSuctionType(Direction face) { return state.getSuctionType(); }
    public int getSuctionAmount(Direction face) { return state.getSuctionAmount(); }
    public Aspect getEssentiaType(Direction face) { return state.aspect; }
    public int getEssentiaAmount(Direction face) { return state.amount; }
    public int takeEssentia(Aspect aspect, int amount, Direction face) { if (canOutputTo(face) && state.takeFromContainer(aspect, amount)) { changed(); return amount; } return 0; }
    public int addEssentia(Aspect aspect, int amount, Direction face) { if (!canInputFrom(face)) return 0; int r = amount - state.addToContainer(aspect, amount); if (r > 0) changed(); return r; }
    public int getMinimumSuction() { return state.getMinimumSuction(); }
    public boolean renderExtendedTube() { return true; }
    void write(CompoundTag tag) {
        if (state.aspect != null) tag.putString("Aspect", state.aspect.getTag());
        if (state.aspectFilter != null) tag.putString("AspectFilter", state.aspectFilter.getTag());
        tag.putShort("Amount", (short) state.amount);
        tag.putByte("facing", (byte) state.facing);
    }
    void read(CompoundTag tag) {
        state.aspect = Aspect.getAspect(tag.getString("Aspect"));
        state.aspectFilter = Aspect.getAspect(tag.getString("AspectFilter"));
        state.amount = tag.getShort("Amount");
        state.facing = tag.contains("facing") ? tag.getByte("facing") : 2;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.saveAdditional(tag, registries); write(tag); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) { super.loadAdditional(tag, registries); read(tag); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
}
