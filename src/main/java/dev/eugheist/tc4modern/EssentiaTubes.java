package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/** BlockTube metadata 0: the ordinary essentia tube. */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class EssentiaTubes {
    private EssentiaTubes() {}
    /** BlockTube metadata 0, 1, 3, 4, 5, 6. */
    public enum Kind { TUBE("essentia_tube"), VALVE("essentia_valve"), FILTER("essentia_tube_filter"), BUFFER("essentia_buffer"), RESTRICT("essentia_tube_restricted"), ONEWAY("essentia_tube_oneway");
        final String id; Kind(String id) { this.id = id; } }
    public static final DeferredHolder<Block, Block> TUBE = block(Kind.TUBE), VALVE = block(Kind.VALVE), FILTER = block(Kind.FILTER), BUFFER = block(Kind.BUFFER), RESTRICT = block(Kind.RESTRICT), ONEWAY = block(Kind.ONEWAY);
    static DeferredHolder<Block, Block> block(Kind kind) { return DeferredHolder.create(Registries.BLOCK, AlchemyContent.id(kind.id)); }
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssentiaTubeEntity>> TUBE_ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("essentia_tube"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssentiaTubeEntity>> VARIANT_ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("essentia_tube_variant"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssentiaBufferEntity>> BUFFER_ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("essentia_buffer"));
    static thaumcraft.common.tiles.essentia.TubeState state(Kind kind) {
        return switch (kind) {
            case VALVE -> new thaumcraft.common.tiles.essentia.TubeVariants.Valve();
            case FILTER -> new thaumcraft.common.tiles.essentia.TubeVariants.Filter();
            case RESTRICT -> new thaumcraft.common.tiles.essentia.TubeVariants.Restrict();
            case ONEWAY -> new thaumcraft.common.tiles.essentia.TubeVariants.Oneway();
            default -> new thaumcraft.common.tiles.essentia.TubeState();
        };
    }
    @SubscribeEvent
    static void register(RegisterEvent event) {
        // BlockTube: hardness 0.5, resistance 5, metal sound, Material.iron.
        event.register(Registries.BLOCK, h -> { for (var kind : Kind.values()) h.register(AlchemyContent.id(kind.id), new TubeBlock(BlockBehaviour.Properties.of().strength(.5f, TC4SoundTypes.res(5)).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion(), kind)); });
        event.register(Registries.BLOCK_ENTITY_TYPE, h -> {
            h.register(TUBE_ENTITY.getId(), BlockEntityType.Builder.of(EssentiaTubeEntity::new, TUBE.get()).build(null));
            h.register(VARIANT_ENTITY.getId(), BlockEntityType.Builder.<EssentiaTubeEntity>of((pos, st) -> new EssentiaTubeEntity(VARIANT_ENTITY.get(), pos, st, state(((TubeBlock) st.getBlock()).kind)), VALVE.get(), FILTER.get(), RESTRICT.get(), ONEWAY.get()).build(null));
            h.register(BUFFER_ENTITY.getId(), BlockEntityType.Builder.of(EssentiaBufferEntity::new, BUFFER.get()).build(null));
        });
        event.register(Registries.ITEM, h -> { for (var b : java.util.List.of(TUBE, VALVE, FILTER, BUFFER, RESTRICT, ONEWAY)) h.register(b.getId(), new BlockItem(b.get(), new Item.Properties())); });
    }

    public static final class TubeBlock extends BaseEntityBlock {
        public static final MapCodec<TubeBlock> CODEC = simpleCodec(p -> new TubeBlock(p, Kind.TUBE));
        static final float W6 = 6 / 16f, W7 = 7 / 16f, W9 = 9 / 16f, W10 = 10 / 16f;
        public final Kind kind;
        public TubeBlock(Properties p, Kind kind) { super(p); this.kind = kind; }
        @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return switch (kind) {
                case BUFFER -> new EssentiaBufferEntity(pos, state);
                case TUBE -> new EssentiaTubeEntity(pos, state);
                default -> new EssentiaTubeEntity(VARIANT_ENTITY.get(), pos, state, state(kind));
            };
        }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            if (kind == Kind.BUFFER) return createTickerHelper(type, BUFFER_ENTITY.get(), EssentiaBufferEntity::tick);
            var t = kind == Kind.TUBE ? TUBE_ENTITY.get() : VARIANT_ENTITY.get();
            return level.isClientSide ? createTickerHelper(type, t, EssentiaTubeEntity::clientTick) : createTickerHelper(type, t, EssentiaTubeEntity::serverTick);
        }
        /** Core (buffer: 4..12) plus one arm per connected open side. */
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
            VoxelShape shape = kind == Kind.BUFFER ? Block.box(4, 4, 4, 12, 12, 12) : Shapes.create(EssentiaTubeEntity.traceBox(6));
            for (Direction d : Direction.values()) {
                if (level.getBlockEntity(pos.relative(d)) instanceof thaumcraft.api.aspects.IEssentiaTransport)
                    shape = Shapes.or(shape, Shapes.create(EssentiaTubeEntity.traceBox(d.get3DDataValue())));
            }
            return shape;
        }
        @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
            if (!level.isClientSide) level.sendBlockUpdated(pos, state, state, 3);
            super.neighborChanged(state, level, pos, block, from, moving);
        }
        /** BlockTube.onBlockActivated, metadata 1 (valve) and 3 (filter). */
        @Override protected net.minecraft.world.ItemInteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.BlockHitResult hit) {
            if (kind == Kind.VALVE && (WandData.isWand(stack) || stack.is(asItem()) || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("resonator")))
                return net.minecraft.world.ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            if (kind == Kind.FILTER && level.getBlockEntity(pos) instanceof EssentiaTubeEntity tube && tube.state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Filter filter) {
                if (player.isShiftKeyDown() && filter.aspectFilter != null) {
                    filter.aspectFilter = null; tube.sync();
                    if (level.isClientSide) level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("page")), net.minecraft.sounds.SoundSource.BLOCKS, 1, 1, false);
                    else { var side = hit.getDirection(); level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, pos.getX() + .5 + side.getStepX() / 3f, pos.getY() + .5, pos.getZ() + .5 + side.getStepZ() / 3f, new net.minecraft.world.item.ItemStack(EssentiaDevices.LABEL.get()))); }
                    return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
                if (filter.aspectFilter == null && stack.is(EssentiaDevices.LABEL.get())) {
                    var aspects = EssentiaItems.getAspects(stack);
                    if (aspects != null && aspects.size() > 0) {
                        filter.aspectFilter = aspects.getAspects()[0];
                        stack.shrink(1); tube.sync();
                        if (level.isClientSide) level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("page")), net.minecraft.sounds.SoundSource.BLOCKS, 1, 1, false);
                    }
                    return net.minecraft.world.ItemInteractionResult.sidedSuccess(level.isClientSide);
                }
            }
            return net.minecraft.world.ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        @Override protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
            if (kind == Kind.VALVE && level.getBlockEntity(pos) instanceof EssentiaTubeEntity tube && tube.state instanceof thaumcraft.common.tiles.essentia.TubeVariants.Valve valve) {
                valve.allowFlow = !valve.allowFlow;
                tube.sync();
                if (!level.isClientSide) EssentiaTubeEntity.squeek(level, pos);
                return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
            }
            return super.useWithoutItem(state, level, pos, player, hit);
        }
    }
}
