package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.essentia.AlchemyFurnaceState;
import thaumcraft.common.tiles.essentia.AlembicState;

/** Modern host for the original TileAlembic. */
public final class AlembicEntity extends BlockEntity implements thaumcraft.api.aspects.IEssentiaTransport {
    public final AlembicState state = new AlembicState();
    public AlembicEntity(BlockPos pos, BlockState blockState) { super(EssentiaDevices.ALEMBIC_ENTITY.get(), pos, blockState); }
    /** Alembic view for the furnace; every add marks and synchronises the tile as the original did. */
    AlchemyFurnaceState.Alembic bound() {
        return new AlchemyFurnaceState.Alembic() {
            public Aspect aspect() { return state.aspect; }
            public int amount() { return state.amount; }
            public int maxAmount() { return state.maxAmount; }
            public Aspect filter() { return state.aspectFilter; }
            public int addToContainer(Aspect a, int n) { int r = state.addToContainer(a, n); changed(); return r; }
        };
    }
    public void changed() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    /** getAppearance */
    public boolean aboveFurnace() { return level != null && level.getBlockState(worldPosition.below()).is(EssentiaDevices.FURNACE.get()); }
    public boolean aboveAlembic() { return level != null && level.getBlockState(worldPosition.below()).is(EssentiaDevices.ALEMBIC.get()); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (state.aspect != null) tag.putString("aspect", state.aspect.getTag());
        if (state.aspectFilter != null) tag.putString("AspectFilter", state.aspectFilter.getTag());
        tag.putShort("amount", (short) state.amount);
        tag.putByte("facing", (byte) state.facing);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state.facing = tag.getByte("facing");
        state.aspectFilter = Aspect.getAspect(tag.getString("AspectFilter"));
        state.aspect = tag.contains("aspect") ? Aspect.getAspect(tag.getString("aspect")) : null;
        state.amount = tag.getShort("amount");
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    // IEssentiaTransport: outputs on every side but its facing and the bottom; never accepts input.
    Direction facing() { return Direction.from3DDataValue(state.facing); }
    public boolean isConnectable(Direction face) { return face != facing() && face != Direction.DOWN; }
    public boolean canInputFrom(Direction face) { return false; }
    public boolean canOutputTo(Direction face) { return face != facing() && face != Direction.DOWN; }
    public void setSuction(Aspect aspect, int amount) {}
    public Aspect getSuctionType(Direction face) { return null; }
    public int getSuctionAmount(Direction face) { return 0; }
    public Aspect getEssentiaType(Direction face) { return state.aspect; }
    public int getEssentiaAmount(Direction face) { return state.amount; }
    public int takeEssentia(Aspect aspect, int amount, Direction face) { if (canOutputTo(face) && state.takeFromContainer(aspect, amount)) { changed(); return amount; } return 0; }
    public int addEssentia(Aspect aspect, int amount, Direction face) { return 0; }
    public int getMinimumSuction() { return 0; }
    public boolean renderExtendedTube() { return true; }
    public net.minecraft.world.phys.AABB getRenderBoundingBox() { return net.minecraft.world.phys.AABB.encapsulatingFullBlocks(worldPosition.offset(-1, 0, -1), worldPosition.offset(1, 0, 1)); }
}
