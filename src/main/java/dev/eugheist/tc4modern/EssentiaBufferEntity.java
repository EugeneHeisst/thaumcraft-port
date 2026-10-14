package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.essentia.TubeBufferState;

/** Modern host of TileTubeBuffer (BlockTube metadata 4). */
public final class EssentiaBufferEntity extends BlockEntity implements IEssentiaTransport {
    public final TubeBufferState state = new TubeBufferState();
    private final java.util.Random random = new java.util.Random();
    public EssentiaBufferEntity(BlockPos pos, BlockState blockState) { super(EssentiaTubes.BUFFER_ENTITY.get(), pos, blockState); }
    TubeBufferState.Host host() {
        return new TubeBufferState.Host() {
            public IEssentiaTransport connectable(Direction face) { return EssentiaTransport.connectable(level, worldPosition, face); }
            public java.util.Random random() { return random; }
            public int countBellows() {
                int count = 0;
                for (Direction dir : Direction.values()) {
                    var p = worldPosition.relative(dir);
                    if (level.getBlockEntity(p) instanceof BellowsEntity b && b.bellows.orientation == dir.getOpposite().get3DDataValue() && !level.hasNeighborSignal(p)) count++;
                }
                return count;
            }
            public void changed() { sync(); }
        };
    }
    void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    public static void tick(Level level, BlockPos pos, BlockState blockState, EssentiaBufferEntity tile) { tile.state.tick(tile.host(), !level.isClientSide); }
    /** onWandRightClick: sneaking cycles the side's choke (squeek), otherwise toggles the side like a tube (tool). */
    public void useWand(net.minecraft.world.item.context.UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || level == null) return;
        var start = player.getEyePosition();
        var end = start.add(player.getLookAngle().scale(player.blockInteractionRange()));
        int selected = -1; double nearest = Double.POSITIVE_INFINITY;
        for (int index = 0; index < 6; index++) {
            if (!(level.getBlockEntity(worldPosition.relative(Direction.from3DDataValue(index))) instanceof IEssentiaTransport)) continue;
            var hit = EssentiaTubeEntity.traceBox(index).move(worldPosition).clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) < nearest) { nearest = start.distanceToSqr(hit.get()); selected = index; }
        }
        if (selected < 0) return;
        player.swing(context.getHand());
        if (player.isShiftKeyDown()) {
            if (level.isClientSide) level.playLocalSound(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, SoundEvent.createVariableRangeEvent(AlchemyContent.id("squeek")), SoundSource.BLOCKS, .6f, 1.1f + level.random.nextFloat() * .2f, false);
            else { state.chokedSides[selected]++; if (state.chokedSides[selected] > 2) state.chokedSides[selected] = 0; sync(); }
            return;
        }
        if (level.isClientSide) { level.playLocalSound(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, SoundEvent.createVariableRangeEvent(AlchemyContent.id("tool")), SoundSource.BLOCKS, .5f, .9f + level.random.nextFloat() * .2f, false); return; }
        state.openSides[selected] = !state.openSides[selected];
        var dir = Direction.from3DDataValue(selected);
        var other = level.getBlockEntity(worldPosition.relative(dir));
        if (other instanceof EssentiaTubeEntity tube) { tube.state.openSides[dir.getOpposite().get3DDataValue()] = state.openSides[selected]; tube.sync(); }
        if (other instanceof EssentiaBufferEntity buffer) { buffer.state.openSides[dir.getOpposite().get3DDataValue()] = state.openSides[selected]; buffer.sync(); }
        sync();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        state.aspects.writeToNBT(tag);
        byte[] sides = new byte[6];
        for (int a = 0; a < 6; ++a) sides[a] = state.openSides[a] ? (byte) 1 : 0;
        tag.putByteArray("open", sides);
        tag.putByteArray("choke", state.chokedSides);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state.aspects.aspects.clear();
        state.aspects.readFromNBT(tag);
        byte[] sides = tag.getByteArray("open");
        if (sides.length == 6) for (int a = 0; a < 6; ++a) state.openSides[a] = sides[a] == 1;
        byte[] choke = tag.getByteArray("choke");
        state.chokedSides = choke.length < 6 ? new byte[6] : choke;
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    // IEssentiaTransport
    public boolean isConnectable(Direction f) { return state.isConnectable(f); }
    public boolean canInputFrom(Direction f) { return state.canInputFrom(f); }
    public boolean canOutputTo(Direction f) { return state.canOutputTo(f); }
    public void setSuction(Aspect a, int n) {}
    public Aspect getSuctionType(Direction f) { return null; }
    public int getSuctionAmount(Direction f) { return state.getSuctionAmount(f); }
    public int takeEssentia(Aspect a, int n, Direction f) { return state.takeEssentia(host(), a, n, f); }
    public int addEssentia(Aspect a, int n, Direction f) { return state.addEssentia(host(), a, n, f); }
    public Aspect getEssentiaType(Direction f) { var all = state.aspects.size() > 0 ? state.aspects.getAspects() : null; return all == null ? null : all[level == null ? random.nextInt(all.length) : level.random.nextInt(all.length)]; }
    public int getEssentiaAmount(Direction f) { return state.aspects.visSize(); }
    public int getMinimumSuction() { return 0; }
    public boolean renderExtendedTube() { return false; }
    /** IAspectContainer.getAspects for goggles; add/take keep the original container semantics. */
    public AspectList getAspects() { return state.aspects; }
}
