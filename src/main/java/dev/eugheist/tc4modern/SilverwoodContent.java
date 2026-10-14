package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.common.lib.vis.BiomeAura;
import thaumcraft.common.lib.vis.NodeGenerator;
import thaumcraft.common.lib.world.SilverwoodTreeGenerator;

/**
 * Silverwood family from BlockMagicalLog/BlockMagicalLeaves/BlockCustomPlant metadata 1 (and 2 for the
 * knot/shimmerleaf), with the original WorldGenSilverwoodTrees adapted externally.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class SilverwoodContent {
    private SilverwoodContent() {}
    private static DeferredHolder<Block, Block> block(String name) { return DeferredHolder.create(Registries.BLOCK, AlchemyContent.id(name)); }
    public static final DeferredHolder<Block, Block> LOG = block("silverwood_log");
    public static final DeferredHolder<Block, Block> KNOT = block("silverwood_knot");
    public static final DeferredHolder<Block, Block> LEAVES = block("silverwood_leaves");
    public static final DeferredHolder<Block, Block> SAPLING = block("silverwood_sapling");
    public static final DeferredHolder<Block, Block> SHIMMERLEAF = block("shimmerleaf");
    public static final DeferredHolder<Feature<?>, SilverwoodFeature> FEATURE = DeferredHolder.create(Registries.FEATURE, AlchemyContent.id("silverwood"));

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> {
            var wood = BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD);
            helper.register(LOG.getId(), new LogBlock(wood));
            // getLightValue: (meta & 2) == 2 -> 7 for the knot.
            helper.register(KNOT.getId(), new KnotBlock(wood.lightLevel(s -> 7)));
            helper.register(LEAVES.getId(), new LeavesBlock(BlockBehaviour.Properties.of().strength(.2f).sound(SoundType.GRASS).noOcclusion().randomTicks().lightLevel(s -> 7).isValidSpawn((s, l, p, t) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
            var plant = BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.GRASS).lightLevel(s -> 8);
            helper.register(SAPLING.getId(), new SaplingBlock(plant.randomTicks()));
            helper.register(SHIMMERLEAF.getId(), new PlantBlock(BlockBehaviour.Properties.of().noCollission().instabreak().sound(SoundType.GRASS).lightLevel(s -> 8)));
        });
        event.register(Registries.ITEM, helper -> {
            for (var b : java.util.List.of(LOG, KNOT, LEAVES, SAPLING, SHIMMERLEAF))
                helper.register(b.getId(), new BlockItem(b.get(), new Item.Properties()));
        });
        event.register(Registries.FEATURE, helper -> helper.register(FEATURE.getId(), new SilverwoodFeature()));
    }

    @SubscribeEvent
    static void nodeHost(BlockEntityTypeAddBlocksEvent event) { event.modify(TC4Modern.AURA_NODE_ENTITY.get(), KNOT.get()); }


    public static final class LogBlock extends RotatedPillarBlock {
        public LogBlock(Properties p) { super(p); }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction f) { return 5; }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction f) { return 5; }
        @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
            if (!state.is(replacement.getBlock())) LeavesBlock.beginDecay(level, pos, 4);
            super.onRemove(state, level, pos, replacement, moving);
        }
    }

    /** BlockMagicalLog metadata 2: a silverwood log hosting a TileNode. */
    public static final class KnotBlock extends AuraNodeBlock {
        public static final MapCodec<KnotBlock> CODEC = simpleCodec(KnotBlock::new);
        public KnotBlock(Properties p) { super(p); }
        @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        // A solid log, unlike the invisible blockAiry host; node layers still come from the AuraNodeEntity renderer.
        @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction f) { return 5; }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction f) { return 5; }
        @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
            if (!state.is(replacement.getBlock())) LeavesBlock.beginDecay(level, pos, 4);
            super.onRemove(state, level, pos, replacement, moving);
        }
        /** addDestroyEffects: burst and craftfail sound. */
        @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.player.Player player) {
            if (level.isClientSide) {
                NodeClientEffects.burst(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5);
                level.playLocalSound(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("craftfail")), net.minecraft.sounds.SoundSource.BLOCKS, 1, 1, false);
            }
            return super.playerWillDestroy(level, pos, state, player);
        }
    }

    /** BlockMagicalLeaves metadata 1: original four-block support search, 1/250 sapling, constant 0x8899AA tint. */
    public static final class LeavesBlock extends net.minecraft.world.level.block.LeavesBlock {
        public static final BooleanProperty CHECK_DECAY = BooleanProperty.create("check_decay");
        public LeavesBlock(Properties p) { super(p); registerDefaultState(defaultBlockState().setValue(CHECK_DECAY, false)); }
        @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { super.createBlockStateDefinition(b); b.add(CHECK_DECAY); }
        @Override protected BlockState updateShape(BlockState s, Direction d, BlockState n, LevelAccessor l, BlockPos p, BlockPos np) { return s; }
        @Override protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {}
        @Override public boolean isRandomlyTicking(BlockState s) { return s.getValue(CHECK_DECAY) && !s.getValue(PERSISTENT); }
        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!state.getValue(CHECK_DECAY) || state.getValue(PERSISTENT) || !level.hasChunksAt(pos.offset(-5, -5, -5), pos.offset(5, 5, 5))) return;
            boolean supported = thaumcraft.common.lib.world.GreatwoodLeafDecay.supported(new thaumcraft.common.lib.world.GreatwoodLeafDecay.View() {
                public boolean sustains(int x, int y, int z) { return level.getBlockState(pos.offset(x, y, z)).is(BlockTags.LOGS); }
                public boolean leaves(int x, int y, int z) { return level.getBlockState(pos.offset(x, y, z)).is(BlockTags.LEAVES); }
            });
            if (supported) level.setBlock(pos, state.setValue(CHECK_DECAY, false), 3);
            else { if (random.nextInt(250) == 0) popResource(level, pos, new ItemStack(SAPLING.get())); level.removeBlock(pos, false); }
        }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction f) { return 60; }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction f) { return 30; }
        static void beginDecay(Level level, BlockPos pos, int radius) {
            if (level.isClientSide || !level.hasChunksAt(pos.offset(-radius - 1, -radius - 1, -radius - 1), pos.offset(radius + 1, radius + 1, radius + 1))) return;
            for (var p : BlockPos.betweenClosed(pos.offset(-radius, -radius, -radius), pos.offset(radius, radius, radius))) {
                var s = level.getBlockState(p);
                if (s.getBlock() instanceof LeavesBlock) level.setBlock(p, s.setValue(CHECK_DECAY, true), 4);
            }
        }
        @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
            if (!state.is(replacement.getBlock())) beginDecay(level, pos, 1);
            super.onRemove(state, level, pos, replacement, moving);
        }
    }

    /** BlockCustomPlant: flower bounds (0.3 wide, 0.8 tall), grass/dirt support. */
    public static class PlantBlock extends BushBlock {
        public static final MapCodec<PlantBlock> CODEC = simpleCodec(PlantBlock::new);
        private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = Block.box(3.2, 0, 3.2, 12.8, 12.8, 12.8);
        public PlantBlock(Properties p) { super(p); }
        @Override protected MapCodec<? extends BushBlock> codec() { return CODEC; }
        @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) { return SHAPE; }
        @Override public int getFlammability(BlockState s, BlockGetter l, BlockPos p, Direction f) { return 100; }
        @Override public int getFireSpreadSpeed(BlockState s, BlockGetter l, BlockPos p, Direction f) { return 60; }
    }

    /** BlockCustomPlant metadata 1: light >= 9 and 1/50 random tick grows WorldGenSilverwoodTrees(true, 7, 5). */
    public static final class SaplingBlock extends PlantBlock {
        public SaplingBlock(Properties p) { super(p); }
        @Override protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (!state.canSurvive(level, pos)) { dropResources(state, level, pos); level.removeBlock(pos, false); return; }
            if (level.getMaxLocalRawBrightness(pos.above()) < 9 || random.nextInt(50) != 0) return;
            level.removeBlock(pos, false);
            if (!FEATURE.get().grow(level, random, pos, false)) level.setBlock(pos, state, 2);
        }
    }

    /** Modern binding of the original silverwood generator plus createRandomNodeAt(silverwood) for knots. */
    public static final class SilverwoodFeature extends Feature<NoneFeatureConfiguration> {
        public SilverwoodFeature() { super(NoneFeatureConfiguration.CODEC); }
        /** ThaumcraftWorldGenerator.generateVegetation: 1/60 per chunk, then generateSilverwood biome test. */
        @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            if(!WorldgenConfig.trees())return false;
            var level = context.level(); var random = context.random();
            if (random.nextInt(60) != 3) return false;
            int x = context.origin().getX() + random.nextInt(16), z = context.origin().getZ() + random.nextInt(16);
            int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
            var pos = new BlockPos(x, y, z);
            if (!level.getBiome(pos).is(net.minecraft.tags.TagKey.create(Registries.BIOME, AlchemyContent.id("silverwood")))) return false;
            return grow(level, random, pos, true);
        }
        public boolean grow(WorldGenLevel level, RandomSource source, BlockPos pos, boolean natural) {return grow(level,source,pos,natural,7,natural?4:5);}
        public boolean grow(WorldGenLevel level, RandomSource source, BlockPos pos, boolean natural,int minimumHeight,int randomHeight) {
            var random = new java.util.Random() {
                @Override public int nextInt(int bound) { return source.nextInt(bound); }
                @Override public boolean nextBoolean() { return source.nextBoolean(); }
                @Override public long nextLong() { return source.nextLong(); }
            };
            int flags = natural ? 2 : 3;
            var world = new SilverwoodTreeGenerator.World() {
                public boolean passable(int x, int y, int z) { var s = level.getBlockState(new BlockPos(x, y, z)); return s.isAir() || s.is(BlockTags.LEAVES) || s.canBeReplaced(); }
                public boolean soil(int x, int y, int z) { return level.getBlockState(new BlockPos(x, y, z)).is(BlockTags.DIRT); }
                public boolean canBeReplacedByLeaves(int x, int y, int z) { var s = level.getBlockState(new BlockPos(x, y, z)); return s.isAir() || s.is(BlockTags.LEAVES) || s.canBeReplaced(); }
                public void log(int x, int y, int z, int meta) {
                    var axis = (meta & 12) == 4 ? Direction.Axis.X : (meta & 12) == 8 ? Direction.Axis.Z : Direction.Axis.Y;
                    var state = (meta & 3) == 2 ? KNOT.get().defaultBlockState() : LOG.get().defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
                    level.setBlock(new BlockPos(x, y, z), state, flags);
                }
                public void leaves(int x, int y, int z) { level.setBlock(new BlockPos(x, y, z), LEAVES.get().defaultBlockState(), flags); }
                public void knotNode(int x, int y, int z, java.util.Random rng) { createNode(level, new BlockPos(x, y, z), rng); }
                public void shimmerleaf(int x, int y, int z, java.util.Random rng) {
                    var p = new BlockPos(x, y, z);
                    var below = level.getBlockState(p.below());
                    if (level.isEmptyBlock(p) && (below.is(Blocks.GRASS_BLOCK) || below.is(Blocks.SAND))) level.setBlock(p, SHIMMERLEAF.get().defaultBlockState(), 3);
                }
            };
            return new SilverwoodTreeGenerator(!natural, minimumHeight, randomHeight).generate(world, random, pos.getX(), pos.getY(), pos.getZ(), level.getMinBuildHeight(), level.getMaxBuildHeight());
        }
        static void createNode(WorldGenLevel level, BlockPos pos, java.util.Random random) {
            var types = BiomeAspectBridge.types(level.getBiome(pos));
            int water = 0, lava = 0, stone = 0, foliage = 0;
            for (int x = -5; x <= 5; x++) for (int y = -5; y <= 5; y++) for (int z = -5; z <= 5; z++) {
                var s = level.getBlockState(pos.offset(x, y, z));
                if (s.getFluidState().is(FluidTags.WATER)) water++;
                else if (s.getFluidState().is(FluidTags.LAVA)) lava++;
                else if (s.is(Blocks.STONE)) stone++;
                if (s.is(BlockTags.LEAVES)) foliage++;
            }
            var generated = NodeGenerator.generate(random, BiomeAura.aura(types), () -> BiomeAura.tag(types, random), water, lava, stone, foliage, true, false, false);
            if (level.getBlockEntity(pos) instanceof AuraNodeEntity node) {
                node.nodeType = generated.type(); node.reservoir.modifier = generated.modifier(); node.reservoir.setAspects(generated.aspects()); node.setChanged();
            }
        }
    }
}
