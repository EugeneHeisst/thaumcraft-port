package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * The architect focus upgrade (IArchitect, WandManager area NBT and toggleMisc on the G key), ItemFocusWarding,
 * and BlockWarded/TileWarded: a block frozen in place that keeps the look and shape of what it replaced.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class Warding {
    private Warding() {}
    public static final DeferredHolder<net.minecraft.world.item.Item, net.minecraft.world.item.Item> FOCUS = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("focus_warding"));
    public static final DeferredHolder<Block, Block> WARDED = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("warded"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WardedEntity>> WARDED_ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("warded"));
    static final int ARCHITECT = FociContent.ARCHITECT, ENLARGE = FociContent.ENLARGE, FRUGAL = FociContent.FRUGAL;

    @SubscribeEvent static void register(RegisterEvent event) {
        // BlockWarded: stone material and sound, unbreakable, resistance 999, light from the stored block.
        event.register(Registries.BLOCK, h -> h.register(WARDED.getId(), new WardedBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(-1, 999 * .6f)
            .sound(SoundType.STONE).noLootTable().noOcclusion().dynamicShape().pushReaction(PushReaction.BLOCK).isValidSpawn((s, l, p, t) -> false))));
        event.register(Registries.BLOCK_ENTITY_TYPE, h -> h.register(WARDED_ENTITY.getId(), BlockEntityType.Builder.of(WardedEntity::new, WARDED.get()).build(null)));
        event.register(Registries.ITEM, h -> h.register(FOCUS.getId(), new WardingFocus()));
    }
    @SubscribeEvent static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("tc4-warding-1").playToServer(MiscToggle.TYPE, MiscToggle.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if(ctx.player() instanceof ServerPlayer player){var held=player.getMainHandItem();if(held.getItem() instanceof ElementalShovel)ElementalShovel.toggle(held);else if(WandData.isWand(held))toggleMisc(held,player);}
        }));
    }

    // ---------------------------------------------------------------- IArchitect and WandManager area

    public enum Axis { X, Y, Z }
    public interface Architect {
        List<BlockPos> architectBlocks(ItemStack wand, Level level, BlockPos pos, int side, Player player);
        boolean showAxis(ItemStack wand, Level level, Player player, int side, Axis axis);
    }
    /** PacketItemKeyToServer key 1 from the "Misc Wand Toggle" key. */
    public record MiscToggle() implements CustomPacketPayload {
        public static final Type<MiscToggle> TYPE = new Type<>(AlchemyContent.id("misc_toggle"));
        public static final StreamCodec<FriendlyByteBuf, MiscToggle> CODEC = StreamCodec.unit(new MiscToggle());
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    private static CompoundTag tag(ItemStack wand) { return wand.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    private static int max(ItemStack wand) { var f = FociContent.getFocus(wand); return f == null ? 1 : f.getMaxAreaSize(FociContent.getFocusItem(wand)); }
    static int areaDim(ItemStack wand) { return tag(wand).getInt("aread"); }
    /** getAreaX/Y/Z: the stored size capped at the focus maximum, or the maximum when unset. */
    static int area(ItemStack wand, String key) { var t = tag(wand); int m = max(wand); return t.contains(key) ? Math.min(t.getInt(key), m) : m; }
    static int areaX(ItemStack wand) { return area(wand, "areax"); }
    static int areaY(ItemStack wand) { return area(wand, "areay"); }
    static int areaZ(ItemStack wand) { return area(wand, "areaz"); }
    /** toggleMisc: sneaking cycles the edited dimension (all, then each axis), otherwise grows it and wraps to zero past the maximum. */
    static void toggleMisc(ItemStack wand, Player player) {
        var focus = FociContent.getFocus(wand);
        if (!(focus instanceof Architect) || !focus.isUpgradedWith(FociContent.getFocusItem(wand), ARCHITECT)) return;
        int dim = areaDim(wand);
        if (player.isShiftKeyDown()) {
            if (++dim > (focus instanceof FociWorld.TradeFocus ? 2 : 3)) dim = 0;
            int d = dim;
            CustomData.update(DataComponents.CUSTOM_DATA, wand, t -> t.putInt("aread", d));
            return;
        }
        int x = areaX(wand), y = areaY(wand), z = areaZ(wand), m = max(wand);
        if (dim == 0) { ++x; ++z; ++y; } else if (dim == 1) ++x; else if (dim == 2) ++z; else if (dim == 3) ++y;
        int fx = x > m ? 0 : x, fy = y > m ? 0 : y, fz = z > m ? 0 : z;
        CustomData.update(DataComponents.CUSTOM_DATA, wand, t -> { t.putInt("areax", fx); t.putInt("areay", fy); t.putInt("areaz", fz); });
    }

    // ---------------------------------------------------------------- ItemFocusWarding

    public static final class WardingFocus extends FociContent.Focus implements Architect {
        private static final AspectList COST = new AspectList().add(Aspect.EARTH, 25).add(Aspect.ORDER, 25).add(Aspect.WATER, 10);
        private static final HashMap<String, Long> DELAY = new HashMap<>();
        @Override public AspectList getVisCost(ItemStack f) { return COST.copy(); }
        @Override public String getSortingHelperBase() { return "BWA"; }
        @Override public int getFocusColor(ItemStack f) { return 16771535; }
        @Override public int getMaxAreaSize(ItemStack f) { return 3 + getUpgradeLevel(f, ENLARGE); }
        @Override public int[] getPossibleUpgradesByRank(ItemStack f, int rank) {
            return FociContent.byRank(rank, new int[]{FRUGAL}, new int[]{FRUGAL, ARCHITECT}, new int[]{FRUGAL, ENLARGE}, new int[]{FRUGAL, ENLARGE}, new int[]{FRUGAL, ENLARGE});
        }
        @Override public boolean canApplyUpgrade(ItemStack f, Player player, int type, int rank) { return type != ENLARGE || isUpgradedWith(f, ARCHITECT); }

        /** Ward every connected solid block in the area (one cost each), or unward your own warded blocks. */
        @Override public boolean onFocusRightClick(ItemStack wand, Level level, Player player) {
            player.swing(InteractionHand.MAIN_HAND);
            var hit = FociWorld.target(player, player.blockInteractionRange());
            if (level.isClientSide || hit.getType() != HitResult.Type.BLOCK) return true;
            var pos = hit.getBlockPos();
            String key = pos.getX() + ":" + pos.getY() + ":" + pos.getZ() + ":" + level.dimension().location();
            if (DELAY.containsKey(key) && DELAY.get(key) > System.currentTimeMillis()) return true;
            DELAY.put(key, System.currentTimeMillis() + 500L);
            var tile = level.getBlockEntity(pos);
            int side = hit.getDirection().get3DDataValue();
            int owner = player.getName().getString().hashCode();
            if (tile == null && solid(level, pos)) {
                for (var c : architectBlocks(wand, level, pos, side, player)) {
                    if (!FociContent.consumeAllVis(wand, player, getVisCost(wand), true)) break;
                    if (level.getBlockEntity(c) != null || !solid(level, c)) continue;
                    var state = level.getBlockState(c);
                    int light = state.getLightEmission(level, c);
                    level.setBlock(c, WARDED.get().defaultBlockState(), 3);
                    if (level.getBlockEntity(c) instanceof WardedEntity w) { w.state = state; w.light = (byte) light; w.owner = owner; w.sync(); }
                    FociWorld.sparkle((ServerLevel) level, c, 16556032, 1);
                }
                level.playSound(null, pos, FociContent.sound("zap"), SoundSource.PLAYERS, .25f, 1f);
            } else if (tile instanceof WardedEntity w && w.owner == owner) {
                for (var c : architectBlocks(wand, level, pos, side, player)) {
                    if (!(level.getBlockEntity(c) instanceof WardedEntity w2) || w2.owner != owner) continue;
                    w2.restoring = true;
                    level.setBlock(c, w2.state, 3);
                    FociWorld.sparkle((ServerLevel) level, c, 16556032, 1);
                }
                level.playSound(null, pos, FociContent.sound("zap"), SoundSource.PLAYERS, .25f, 1f);
            }
            return true;
        }
        /** World.isBlockNormalCubeDefault(..., true): only normal solid cubes, not slabs or stairs. */
        static boolean solid(Level level, BlockPos pos) {
            var state = level.getBlockState(pos);
            return !state.isAir() && state.isSolidRender(level, pos);
        }
        @Override public List<BlockPos> architectBlocks(ItemStack wand, Level level, BlockPos pos, int side, Player player) {
            var out = new ArrayList<BlockPos>();
            var tt = level.getBlockEntity(pos);
            boolean tiles = tt instanceof WardedEntity;
            int sx = 0, sy = 0, sz = 0;
            if (isUpgradedWith(FociContent.getFocusItem(wand), ARCHITECT)) { sx = areaX(wand); sy = areaY(wand); sz = areaZ(wand); }
            var checked = new HashSet<BlockPos>();
            if (side == 2 || side == 3) check(level, pos, pos, side, sz, sy, sx, out, player, tiles, checked);
            else check(level, pos, pos, side, sx, sy, sz, out, player, tiles, checked);
            return out;
        }
        private void check(Level level, BlockPos o, BlockPos pos, int side, int sizeX, int sizeY, int sizeZ, List<BlockPos> list, Player player, boolean tiles, Set<BlockPos> checked) {
            if (!checked.add(pos)) return;
            int dx = Math.abs(pos.getX() - o.getX()), dy = Math.abs(pos.getY() - o.getY()), dz = Math.abs(pos.getZ() - o.getZ());
            switch (side) {
                case 0, 1 -> { if (dx > sizeX || dz > sizeZ || dy > sizeY) return; }
                case 2, 3 -> { if (dx > sizeX || dy > sizeZ || dz > sizeY) return; }
                default -> { if (dy > sizeX || dz > sizeZ || dx > sizeY) return; }
            }
            var tt = level.getBlockEntity(pos);
            if (tiles && !(tt instanceof WardedEntity)) return;
            if (!tiles && !(tt == null && solid(level, pos))) return;
            if (tiles && ((WardedEntity) tt).owner != player.getName().getString().hashCode()) return;
            if (level.isEmptyBlock(pos)) return;
            list.add(pos);
            for (var d : Direction.values()) check(level, o, pos.relative(d), side, sizeX, sizeY, sizeZ, list, player, tiles, checked);
        }
        @Override public boolean showAxis(ItemStack wand, Level level, Player player, int side, Axis axis) {
            int dim = areaDim(wand);
            if (dim == 0) return true;
            return switch (side) {
                case 0, 1 -> axis == Axis.X && dim == 1 || axis == Axis.Z && dim == 2 || axis == Axis.Y && dim == 3;
                case 2, 3 -> axis == Axis.Y && dim == 1 || axis == Axis.X && dim == 2 || axis == Axis.Z && dim == 3;
                default -> axis == Axis.Y && dim == 1 || axis == Axis.Z && dim == 2 || axis == Axis.X && dim == 3;
            };
        }
    }

    // ---------------------------------------------------------------- BlockWarded / TileWarded

    public static final class WardedBlock extends BaseEntityBlock {
        WardedBlock(Properties p) { super(p); }
        @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(WardedBlock::new); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WardedEntity(pos, state); }
        /** The stored block is drawn by the block entity renderer. */
        @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
        static BlockState stored(BlockGetter level, BlockPos pos) {
            return level.getBlockEntity(pos) instanceof WardedEntity w && !w.state.is(WARDED.get()) ? w.state : Blocks.STONE.defaultBlockState();
        }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) { return stored(level, pos).getShape(level, pos, c); }
        @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter level, BlockPos pos, CollisionContext c) { return stored(level, pos).getCollisionShape(level, pos, c); }
        @Override protected VoxelShape getOcclusionShape(BlockState s, BlockGetter level, BlockPos pos) { return Shapes.empty(); }
        @Override public int getLightEmission(BlockState s, BlockGetter level, BlockPos pos) { return level.getBlockEntity(pos) instanceof WardedEntity w ? w.light : 0; }
        @Override protected int getLightBlock(BlockState s, BlockGetter level, BlockPos pos) { return stored(level, pos).getLightBlock(level, pos); }
        @Override protected VoxelShape getBlockSupportShape(BlockState s, BlockGetter level, BlockPos pos) { return stored(level,pos).getBlockSupportShape(level,pos); }
        @Override public boolean isLadder(BlockState s, LevelReader level, BlockPos pos, net.minecraft.world.entity.LivingEntity entity) { return stored(level,pos).isLadder(level,pos,entity); }
        @Override public net.neoforged.neoforge.common.util.TriState canSustainPlant(BlockState s, BlockGetter level, BlockPos pos, Direction facing, BlockState plant) { return stored(level,pos).canSustainPlant(level,pos,facing,plant); }
        @Override public boolean isFertile(BlockState s, BlockGetter level, BlockPos pos) { return stored(level,pos).isFertile(level,pos); }
        @Override public float getEnchantPowerBonus(BlockState s, LevelReader level, BlockPos pos) { return stored(level,pos).getEnchantPowerBonus(level,pos); }
        @Override public void animateTick(BlockState s, Level level, BlockPos pos, net.minecraft.util.RandomSource random) { var original=stored(level,pos); original.getBlock().animateTick(original,level,pos,random); }
        @Override public void stepOn(Level level, BlockPos pos, BlockState s, Entity entity) { var original=stored(level,pos); original.getBlock().stepOn(level,pos,original,entity); }
        @Override public void fallOn(Level level, BlockState s, BlockPos pos, Entity entity, float distance) { var original=stored(level,pos); original.getBlock().fallOn(level,original,pos,entity,distance); }
        @Override protected void entityInside(BlockState s, Level level, BlockPos pos, Entity entity) { stored(level,pos).entityInside(level,pos,entity); }
        @Override public ItemStack getCloneItemStack(BlockState s, HitResult target, LevelReader level, BlockPos pos, Player player) { return stored(level, pos).getCloneItemStack(target, level, pos, player); }
        @Override public boolean canEntityDestroy(BlockState s, BlockGetter level, BlockPos pos, Entity entity) { return false; }
        @Override public void onBlockExploded(BlockState s, Level level, BlockPos pos, Explosion explosion) {}
        @Override public boolean canDropFromExplosion(BlockState s, BlockGetter level, BlockPos pos, Explosion explosion) { return false; }
        @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
            super.onRemove(state, level, pos, next, moved);
            level.getLightEngine().checkBlock(pos);
        }
    }

    public static final class WardedEntity extends BlockEntity {
        public BlockState state = Blocks.STONE.defaultBlockState();
        public byte light;
        public int owner;
        boolean restoring;
        public WardedEntity(BlockPos pos, BlockState block) { super(WARDED_ENTITY.get(), pos, block); }
        void sync() {
            setChanged();
            if (level != null) { level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); level.getLightEngine().checkBlock(worldPosition); }
        }
        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
            super.saveAdditional(tag, lookup);
            tag.put("bs", NbtUtils.writeBlockState(state));
            tag.putByte("ll", light);
            tag.putInt("oi", owner);
        }
        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
            super.loadAdditional(tag, lookup);
            state = NbtUtils.readBlockState(lookup.lookupOrThrow(Registries.BLOCK), tag.getCompound("bs"));
            if (state.isAir()) state = Blocks.STONE.defaultBlockState();
            light = tag.getByte("ll");
            owner = tag.getInt("oi");
            if (level != null && level.isClientSide) level.getLightEngine().checkBlock(worldPosition);
        }
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    }
}
