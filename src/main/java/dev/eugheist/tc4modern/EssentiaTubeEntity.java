package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.IEssentiaTransport;
import thaumcraft.common.tiles.essentia.AspectOrder;
import thaumcraft.common.tiles.essentia.TubeState;

/** Modern host of the original TileTube. */
public class EssentiaTubeEntity extends BlockEntity implements IEssentiaTransport {
    public final TubeState state;
    private final java.util.Random random = new java.util.Random();
    public int ventColor;
    private int clientVenting;
    public record Vent(Level level,double x,double y,double z,double dx,double dy,double dz,int color) {}
    public static java.util.function.Consumer<Vent> clientVent = vent -> {};
    public EssentiaTubeEntity(BlockPos pos, BlockState blockState) { this(EssentiaTubes.TUBE_ENTITY.get(), pos, blockState, new TubeState()); }
    /** BlockTube metadata 1, 3, 5 and 6: the same TileTube host with the original subclass behaviour. */
    public EssentiaTubeEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, BlockPos pos, BlockState blockState, TubeState state) { super(type, pos, blockState); this.state = state; }
    /** Original indexed ray cuboids remain selectable even when their side is closed. */
    public void useWand(net.minecraft.world.item.context.UseOnContext context) {
        var player = context.getPlayer();
        if (player == null || level == null || level.isClientSide) return;
        var start = player.getEyePosition();
        var end = start.add(player.getLookAngle().scale(player.blockInteractionRange()));
        int selected = -1;
        double nearest = Double.POSITIVE_INFINITY;
        for (int index = 0; index <= 6; index++) {
            if (index < 6 && !(level.getBlockEntity(worldPosition.relative(Direction.from3DDataValue(index))) instanceof IEssentiaTransport)) continue;
            var hit = traceBox(index).move(worldPosition).clip(start, end);
            if (hit.isPresent() && start.distanceToSqr(hit.get()) < nearest) {
                nearest = start.distanceToSqr(hit.get()); selected = index;
            }
        }
        if (selected < 0) return;
        level.playSound(null, worldPosition, SoundEvent.createVariableRangeEvent(AlchemyContent.id("tool")), SoundSource.BLOCKS, .5f, .9f + level.random.nextFloat() * .2f);
        if (selected < 6) {
            state.openSides[selected] = !state.openSides[selected];
            var direction = Direction.from3DDataValue(selected);
            if (level.getBlockEntity(worldPosition.relative(direction)) instanceof EssentiaTubeEntity other) {
                other.state.openSides[direction.getOpposite().get3DDataValue()] = state.openSides[selected];
                other.sync();
            }
        } else {
            if (state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Valve) {
                // TileTubeValve: the handle turns to the next side with no essentia transport attached.
                for (int a = state.facing.get3DDataValue() + 1; a < 20; a++) {
                    var candidate = Direction.from3DDataValue(a % 6);
                    if (level.getBlockEntity(worldPosition.relative(candidate)) instanceof IEssentiaTransport) continue;
                    state.facing = candidate; break;
                }
                sync(); return;
            }
            for (int a = state.facing.get3DDataValue() + 1; a < 20; a++) {
                var candidate = Direction.from3DDataValue(a % 6);
                var opposite = candidate.getOpposite();
                if (level.getBlockEntity(worldPosition.relative(opposite)) instanceof IEssentiaTransport && isConnectable(opposite)) {
                    state.facing = candidate; break;
                }
            }
        }
        sync();
    }
    static net.minecraft.world.phys.AABB traceBox(int index) {
        double min = .42f, max = .58f;
        return switch (index) {
            case 0 -> new net.minecraft.world.phys.AABB(min, 0, min, max, .5, max);
            case 1 -> new net.minecraft.world.phys.AABB(min, .5, min, max, 1, max);
            case 2 -> new net.minecraft.world.phys.AABB(min, min, 0, max, max, .5);
            case 3 -> new net.minecraft.world.phys.AABB(min, min, .5, max, max, 1);
            case 4 -> new net.minecraft.world.phys.AABB(0, min, min, .5, max, max);
            case 5 -> new net.minecraft.world.phys.AABB(.5, min, min, 1, max, max);
            default -> new net.minecraft.world.phys.AABB(.34375, .34375, .34375, .65625, .65625, .65625);
        };
    }
    void sync() { setChanged(); level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
    TubeState.Host host() {
        return new TubeState.Host() {
            public IEssentiaTransport connectable(Direction face) { return EssentiaTransport.connectable(level, worldPosition, face); }
            public java.util.Random random() { return random; }
            public void vent(int index) { level.blockEvent(worldPosition, getBlockState().getBlock(), 1, index); }
            public void creak() { level.blockEvent(worldPosition, getBlockState().getBlock(), 0, 0); }
            public void changed() { setChanged(); }
        };
    }
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, EssentiaTubeEntity tile) {
        if (tile.state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Valve valve) {
            var base = tile.host();
            valve.serverTick(new thaumcraft.common.tiles.essentia.TubeVariants.Valve.PowerHost() {
                public IEssentiaTransport connectable(Direction face) { return base.connectable(face); }
                public java.util.Random random() { return base.random(); }
                public void vent(int index) { base.vent(index); }
                public void creak() { base.creak(); }
                public void changed() { base.changed(); }
                public boolean powered() { return level.hasNeighborSignal(pos); }
                public void squeek() { EssentiaTubeEntity.squeek(level, pos); }
                public void sync() { tile.sync(); }
            });
        } else tile.state.serverTick(tile.host());
    }
    static void squeek(Level level, BlockPos pos) { level.playSound(null, pos, SoundEvent.createVariableRangeEvent(AlchemyContent.id("squeek")), SoundSource.BLOCKS, .7f, .9f + level.random.nextFloat() * .2f); }
    /** Client half of updateEntity: vent particles in a hash-seeded direction while venting. */
    public static void clientTick(Level level, BlockPos pos, BlockState blockState, EssentiaTubeEntity tile) {
        if (tile.state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Valve valve) valve.clientTick();
        if (tile.clientVenting <= 0) return;
        if (--tile.clientVenting <= 0) return;
        var r = new java.util.Random(tile.hashCode() * 4L);
        float rp = r.nextFloat() * 360f, ry = r.nextFloat() * 360f;
        double fx = -Math.sin(ry / 180f * Math.PI) * Math.cos(rp / 180f * Math.PI);
        double fy = -Math.sin(rp / 180f * Math.PI);
        // Original TileTube deliberately passes fx as BOTH x and z velocity.
        clientVent.accept(new Vent(level,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,fx/5,fy/5,fx/5,tile.ventColor));
    }
    @Override public boolean triggerEvent(int id, int param) {
        if (id == 0) {
            if (level.isClientSide) level.playLocalSound(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, SoundEvent.createVariableRangeEvent(AlchemyContent.id("creak")), SoundSource.BLOCKS, 1f, 1.3f + level.random.nextFloat() * .2f, false);
            return true;
        }
        if (id == 1) {
            if (level.isClientSide) {
                if (clientVenting <= 0) level.playLocalSound(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, .1f, 1f + level.random.nextFloat() * .1f, false);
                clientVenting = 50;
                var a = AspectOrder.at(param);
                ventColor = a == null ? 0xAAAAAA : a.getColor();
            }
            return true;
        }
        return super.triggerEvent(id, param);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (state.essentiaType != null) tag.putString("type", state.essentiaType.getTag());
        tag.putInt("amount", state.essentiaAmount);
        byte[] sides = new byte[6];
        for (int a = 0; a < 6; ++a) sides[a] = state.openSides[a] ? (byte) 1 : 0;
        tag.putInt("side", state.facing.get3DDataValue());
        tag.putByteArray("open", sides);
        if (state.suctionType != null) tag.putString("stype", state.suctionType.getTag());
        tag.putInt("samount", state.suction);
        if (state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Filter f && f.aspectFilter != null) tag.putString("AspectFilter", f.aspectFilter.getTag());
        if (state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Valve v) { tag.putBoolean("flow", v.allowFlow); tag.putBoolean("hadpower", v.wasPoweredLastTick); }
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        state.essentiaType = Aspect.getAspect(tag.getString("type"));
        state.essentiaAmount = tag.getInt("amount");
        state.facing = Direction.from3DDataValue(tag.getInt("side"));
        byte[] sides = tag.getByteArray("open");
        if (sides.length == 6) for (int a = 0; a < 6; ++a) state.openSides[a] = sides[a] == 1;
        state.suctionType = Aspect.getAspect(tag.getString("stype"));
        state.suction = tag.getInt("samount");
        if (state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Filter f) f.aspectFilter = Aspect.getAspect(tag.getString("AspectFilter"));
        if (state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Valve v) { v.allowFlow = !tag.contains("flow") || tag.getBoolean("flow"); v.wasPoweredLastTick = tag.getBoolean("hadpower"); }
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    public boolean isConnectable(Direction f) { return state.isConnectable(f); }
    public boolean canInputFrom(Direction f) { return state.canInputFrom(f); }
    public boolean canOutputTo(Direction f) { return state.canOutputTo(f); }
    public void setSuction(Aspect a, int n) { state.setSuction(a, n); }
    public Aspect getSuctionType(Direction f) { return state.getSuctionType(f); }
    public int getSuctionAmount(Direction f) { return state.getSuctionAmount(f); }
    public int takeEssentia(Aspect a, int n, Direction f) { int r = state.takeEssentia(a, n, f); if (r > 0) setChanged(); return r; }
    public int addEssentia(Aspect a, int n, Direction f) { int r = state.addEssentia(a, n, f); if (r > 0) setChanged(); return r; }
    public Aspect getEssentiaType(Direction f) { return state.getEssentiaType(f); }
    public int getEssentiaAmount(Direction f) { return state.getEssentiaAmount(f); }
    public int getMinimumSuction() { return state.getMinimumSuction(); }
    public boolean renderExtendedTube() { return state.renderExtendedTube(); }
}
