package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * blockStoneDevice metadata 5 (TileWandPedestal, wand recharge pedestal) and 8 (aspect splitter on top of it), the
 * primal charm and mundane bauble blanks of BASICARTIFACE, and the greater vis amulet of VISAMULET.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class WandPedestal {
    private WandPedestal() {}
    public static final DeferredHolder<Block, Block> PEDESTAL = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("wand_pedestal"));
    public static final DeferredHolder<Block, Block> FOCUS = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("wand_pedestal_focus"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PedestalEntity>> PEDESTAL_ENTITY =
        DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("wand_pedestal"));
    public static final DeferredHolder<Item, Item> PRIMAL_CHARM = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("primal_charm"));
    public static final DeferredHolder<Item, Item> MUNDANE_AMULET = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("mundane_amulet"));
    public static final DeferredHolder<Item, Item> MUNDANE_RING = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("mundane_ring"));
    public static final DeferredHolder<Item, Item> MUNDANE_BELT = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("mundane_belt"));
    public static final DeferredHolder<Item, Item> VIS_AMULET = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("vis_amulet"));

    @SubscribeEvent static void register(RegisterEvent event) {
        // BlockStoneDevice: stone, hardness 3, resistance 25.
        BlockBehaviour.Properties stone = BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3, TC4SoundTypes.res(25)).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion();
        event.register(Registries.BLOCK, h -> {
            h.register(PEDESTAL.getId(), new PedestalBlock(stone));
            h.register(FOCUS.getId(), new FocusBlock(stone));
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, h -> h.register(PEDESTAL_ENTITY.getId(), BlockEntityType.Builder.of(PedestalEntity::new, PEDESTAL.get()).build(null)));
        event.register(Registries.ITEM, h -> {
            h.register(PEDESTAL.getId(), new BlockItem(PEDESTAL.get(), new Item.Properties()));
            h.register(FOCUS.getId(), new BlockItem(FOCUS.get(), new Item.Properties()));
            h.register(PRIMAL_CHARM.getId(), new Item(new Item.Properties().rarity(Rarity.UNCOMMON)));
            h.register(MUNDANE_AMULET.getId(), new MundaneBauble(baubles.api.BaubleType.AMULET));
            h.register(MUNDANE_RING.getId(), new MundaneBauble(baubles.api.BaubleType.RING));
            h.register(MUNDANE_BELT.getId(), new MundaneBauble(baubles.api.BaubleType.BELT));
            h.register(VIS_AMULET.getId(), new PechRewards.AccessoryItem(false, 25000, Rarity.RARE, "VISAMULET"));
        });
    }

    /** ItemBaubleBlanks 0-2: wearable, otherwise inert. */
    public static final class MundaneBauble extends Item implements baubles.api.IBauble {
        private final baubles.api.BaubleType type;
        MundaneBauble(baubles.api.BaubleType type) { super(new Item.Properties().stacksTo(1)); this.type = type; }
        @Override public baubles.api.BaubleType getBaubleType(ItemStack stack) { return type; }
    }

    static boolean accepts(ItemStack stack) {
        return WandData.isWand(stack) || stack.getItem() instanceof PechRewards.AccessoryItem amulet && !amulet.ring;
    }

    public static final class PedestalBlock extends BaseEntityBlock {
        /** addCollisionBoxesToList: base, step and column; the outline is the central column. */
        private static final VoxelShape COLLISION = Shapes.or(box(0, 0, 0, 16, 4, 16), box(2, 4, 2, 14, 8, 14), box(4, 8, 4, 12, 16, 12));
        private static final VoxelShape OUTLINE = box(4, 0, 4, 12, 16, 12);
        PedestalBlock(Properties properties) { super(properties); }
        @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(PedestalBlock::new); }
        @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return OUTLINE; }
        @Override protected VoxelShape getCollisionShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return COLLISION; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PedestalEntity(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? null : createTickerHelper(type, PEDESTAL_ENTITY.get(), PedestalEntity::tick);
        }
        @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return PedestalEntity.interact(level, pos, player, held);
        }
        @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
            if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PedestalEntity pedestal) Block.popResource(level, pos, pedestal.stack);
            super.onRemove(state, level, pos, newState, moved);
        }
        /** onNeighborBlockChange: anything but air or the splitter on top throws the item off. */
        @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
            var above = level.getBlockState(pos.above());
            if (!level.isClientSide && !above.isAir() && !above.is(FOCUS.get()) && level.getBlockEntity(pos) instanceof PedestalEntity pedestal && !pedestal.stack.isEmpty()) {
                Block.popResource(level, pos.above(), pedestal.stack);
                pedestal.setItem(ItemStack.EMPTY);
            }
            super.neighborChanged(state, level, pos, block, from, moving);
        }
        @Override protected boolean hasAnalogOutputSignal(BlockState state) { return true; }
        @Override protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
            if (!(level.getBlockEntity(pos) instanceof PedestalEntity pedestal) || pedestal.stack.isEmpty()) return 0;
            return net.minecraft.util.Mth.floor(pedestal.fill() * 14) + 1;
        }
    }

    /** blockStoneDevice metadata 8: a flat cross with four claws; clicks pass through to the pedestal below. */
    public static final class FocusBlock extends Block {
        private static final VoxelShape SHAPE = box(1, 0, 1, 15, 7, 15);
        FocusBlock(Properties properties) { super(properties); }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
        @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            return level.getBlockEntity(pos.below()) instanceof PedestalEntity ? PedestalEntity.interact(level, pos.below(), player, held) : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
    }

    /** TileWandPedestal: every five ticks pull one vis from a node within eight blocks into the held wand or amulet. */
    public static final class PedestalEntity extends BlockEntity {
        ItemStack stack = ItemStack.EMPTY;
        public boolean draining;
        public BlockPos drain = BlockPos.ZERO;
        public int drainColor;
        private int counter;
        private boolean changed;
        private List<BlockPos> nodes;
        public PedestalEntity(BlockPos pos, BlockState state) { super(PEDESTAL_ENTITY.get(), pos, state); }
        public ItemStack item() { return stack; }

        static ItemInteractionResult interact(Level level, BlockPos pos, Player player, ItemStack held) {
            if (!(level.getBlockEntity(pos) instanceof PedestalEntity pedestal)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (held.is(FOCUS.get().asItem())) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            float pitch = ((level.random.nextFloat() - level.random.nextFloat()) * .7f + 1) * 1.5f;
            if (!pedestal.stack.isEmpty()) {
                if (!player.getInventory().add(pedestal.stack)) player.drop(pedestal.stack, false);
                pedestal.setItem(ItemStack.EMPTY);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .2f, pitch);
                return ItemInteractionResult.SUCCESS;
            }
            if (accepts(held)) {
                pedestal.setItem(held.split(1));
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .2f, pitch * 1.07f);
                return ItemInteractionResult.SUCCESS;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        void setItem(ItemStack item) {
            stack = item;
            draining = false;
            sync();
        }
        private void sync() {
            setChanged();
            if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }

        /** Fullest fraction of the stored aspects, for the comparator. */
        float fill() {
            int max = capacity(stack);
            if (max <= 0) return 0;
            int most = 0;
            for (var a : Aspect.getPrimalAspects()) most = Math.max(most, vis(stack, a));
            return most / (float) max;
        }
        static int capacity(ItemStack item) {
            if (WandData.isWand(item)) return WandData.capacity(item);
            return item.getItem() instanceof PechRewards.AccessoryItem amulet ? amulet.capacity : 0;
        }
        static int vis(ItemStack item, Aspect aspect) { return thaumcraft.common.lib.vis.VisStorage.getVis(NoteData.tag(item), aspect); }
        /** getAspectsWithRoom: primals not yet at capacity. */
        static List<Aspect> withRoom(ItemStack item) {
            var out = new ArrayList<Aspect>();
            int max = capacity(item);
            for (var a : Aspect.getPrimalAspects()) if (vis(item, a) < max) out.add(a);
            return out;
        }
        static void addVis(ItemStack item, Aspect aspect, int centivis) {
            // ItemWandCasting.addVis takes whole vis; the amulet stores hundredths directly.
            if (WandData.isWand(item)) { WandData.add(item, aspect, centivis / 100, true); return; }
            var tag = NoteData.tag(item).copy();
            int max = capacity(item);
            tag.putInt(aspect.getTag(), Math.min(max, tag.getInt(aspect.getTag()) + centivis));
            item.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        }

        static void tick(Level level, BlockPos pos, BlockState state, PedestalEntity ped) {
            if (ped.nodes == null) ped.findNodes();
            ped.counter++;
            boolean recalc = false;
            if (ped.counter % 20 == 0 && ped.changed && !ped.nodes.isEmpty() && !ped.stack.isEmpty()) {
                level.updateNeighbourForOutputSignal(pos, state.getBlock());
                ped.changed = false;
            }
            if (ped.counter % 5 == 0 && !ped.nodes.isEmpty() && !ped.stack.isEmpty() && accepts(ped.stack)) {
                boolean splitter = level.getBlockState(pos.above()).is(FOCUS.get());
                // Iron-capped or wooden wands may drain a node to its last point; others leave one.
                int min = 1;
                if (WandData.isWand(ped.stack) && (WandParts.cap(ped.stack).tag().equals("iron") || WandParts.rod(ped.stack).tag().equals("wood"))) min = 0;
                var room = withRoom(ped.stack);
                boolean was = ped.draining;
                BlockPos oldDrain = ped.drain;
                ped.draining = false;
                if (!room.isEmpty()) {
                    search:
                    for (var at : ped.nodes) {
                        if (!(level.getBlockEntity(at) instanceof AuraNodeEntity node)) continue;
                        var aspects = node.reservoir.getAspects();
                        for (var aspect : room) {
                            if (aspects.getAmount(aspect) <= min) continue;
                            addVis(ped.stack, aspect, 100);
                            node.reservoir.takeFromContainer(aspect, 1);
                            node.changed();
                            ped.mark(at, aspect.getColor());
                            break search;
                        }
                        if (!splitter) continue;
                        for (var aspect : aspects.getAspects()) {
                            if (aspect == null || aspect.isPrimal()) continue;
                            var primals = thaumcraft.common.lib.research.ResearchManager.reduceToPrimals(new AspectList().add(aspect, 1));
                            for (var primal : room) {
                                if (primals.getAmount(primal) <= 0 || aspects.getAmount(aspect) <= min) continue;
                                addVis(ped.stack, primal, 100);
                                node.reservoir.takeFromContainer(aspect, 1);
                                node.changed();
                                ped.mark(at, aspect.getColor());
                                break search;
                            }
                        }
                    }
                    if (!ped.draining) recalc = true;
                }
                if (ped.draining != was || !ped.drain.equals(oldDrain) || ped.draining) ped.sync();
            }
            if (ped.counter % 100 == 0 && (recalc || ped.nodes.isEmpty())) ped.findNodes();
        }
        private void mark(BlockPos at, int color) { changed = true; draining = true; drain = at; drainColor = color; }

        private void findNodes() {
            nodes = new ArrayList<>();
            for (var at : BlockPos.betweenClosed(worldPosition.offset(-8, -8, -8), worldPosition.offset(8, 8, 8)))
                if (level.getBlockEntity(at) instanceof AuraNodeEntity) nodes.add(at.immutable());
        }

        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            if (!stack.isEmpty()) tag.put("Item", stack.save(registries));
            tag.putBoolean("draining", draining);
            tag.putLong("drain", drain.asLong());
            tag.putInt("drainColor", drainColor);
        }
        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            stack = tag.contains("Item") ? ItemStack.parseOptional(registries, tag.getCompound("Item")) : ItemStack.EMPTY;
            draining = tag.getBoolean("draining");
            drain = BlockPos.of(tag.getLong("drain"));
            drainColor = tag.getInt("drainColor");
        }
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
            return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
        }
    }
}
