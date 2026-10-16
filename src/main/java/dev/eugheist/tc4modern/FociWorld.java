package dev.eugheist.tc4modern;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** ItemFocusExcavation, ItemFocusTrade (with ServerTickEventsFML swappers) and ItemFocusPortableHole (BlockHole/TileHole). */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class FociWorld {
    private FociWorld() {}
    public static final DeferredHolder<Item, Item> EXCAVATION = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("focus_excavation"));
    public static final DeferredHolder<Item, Item> TRADE = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("focus_trade"));
    public static final DeferredHolder<Item, Item> PORTABLE_HOLE = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("focus_portable_hole"));
    public static final DeferredHolder<Block, Block> HOLE = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("hole"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HoleEntity>> HOLE_ENTITY =
        DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("hole"));
    /** FocusUpgradeType ids beyond potency/frugal. */
    static final int TREASURE = 2, ENLARGE = 3, EXTEND = 7, SILKTOUCH = 8, DOWSING = 20;

    @EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
    static final class Registration {
        @SubscribeEvent static void register(RegisterEvent event) {
            event.register(Registries.ITEM, h -> {
                h.register(EXCAVATION.getId(), new ExcavationFocus());
                h.register(TRADE.getId(), new TradeFocus());
                h.register(PORTABLE_HOLE.getId(), new PortableHoleFocus());
            });
            // BlockHole: unbreakable, resistance 6000000, light 0.7, no collision or outline, invisible.
            event.register(Registries.BLOCK, h -> h.register(HOLE.getId(), new HoleBlock(Block.Properties.of().strength(-1, TC4SoundTypes.res(6000000)).sound(net.minecraft.world.level.block.SoundType.GLASS).lightLevel(s -> 10)
                .noCollission().noOcclusion().noLootTable().sound(net.minecraft.world.level.block.SoundType.WOOL).isValidSpawn((s, l, p, t) -> false))));
            event.register(Registries.BLOCK_ENTITY_TYPE, h -> h.register(HOLE_ENTITY.getId(), BlockEntityType.Builder.of(HoleEntity::new, HOLE.get()).build(null)));
        }
        @SubscribeEvent static void payloads(RegisterPayloadHandlersEvent event) {
            event.registrar("tc4-foci-world-1").playToClient(Sparkle.TYPE, Sparkle.CODEC, (p, c) -> c.enqueueWork(() -> FociFx.blockSparkle(p.pos(), p.color(), p.count())));
        }
    }

    /** PacketFXBlockSparkle. */
    public record Sparkle(BlockPos pos, int color, int count) implements CustomPacketPayload {
        public static final Type<Sparkle> TYPE = new Type<>(AlchemyContent.id("block_sparkle"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Sparkle> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, Sparkle::pos, ByteBufCodecs.INT, Sparkle::color, ByteBufCodecs.VAR_INT, Sparkle::count, Sparkle::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    static void sparkle(ServerLevel level, BlockPos pos, int color, int count) {
        PacketDistributor.sendToPlayersNear(level, null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 32, new Sparkle(pos, color, count));
    }

    /** BlockUtils.isBlockExposed: any neighbour that is not an opaque cube. */
    static boolean exposed(Level level, BlockPos pos) {
        for (var d : Direction.values()) if (!level.getBlockState(pos.relative(d)).isSolidRender(level, pos.relative(d))) return true;
        return false;
    }

    /** BlockUtils.getTargetBlock(world, player, false): blocks only, ten blocks from the eye. */
    static BlockHitResult target(Player p, double range) {
        var eye = p.getEyePosition();
        return p.level().clip(new ClipContext(eye, eye.add(p.getViewVector(1).scale(range)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
    }

    static int upgrade(ItemStack wand, int id) {
        var focus = FociContent.getFocus(wand);
        return focus == null ? 0 : focus.getUpgradeLevel(FociContent.getFocusItem(wand), id);
    }

    // ---------------------------------------------------------------- Excavation

    /** ItemFocusExcavation: 0.15 Terra per tick beam that mines whatever it holds on, much faster on stone and earth. */
    public static final class ExcavationFocus extends FociContent.Focus {
        private static final AspectList COST = new AspectList().add(Aspect.EARTH, 15);
        private static final AspectList COST_SILK = new AspectList().add(Aspect.AIR, 1).add(Aspect.FIRE, 1).add(Aspect.EARTH, 16).add(Aspect.WATER, 1).add(Aspect.ORDER, 1).add(Aspect.ENTROPY, 1);
        private static final AspectList COST_DOWSING = new AspectList().add(Aspect.FIRE, 2).add(Aspect.ORDER, 2).add(Aspect.EARTH, 15);
        private final Map<String, long[]> sound = new HashMap<>();
        private final Map<String, Float> breakCount = new HashMap<>();
        private final Map<String, BlockPos> last = new HashMap<>();
        @Override public AspectList getVisCost(ItemStack f) {
            if (getUpgradeLevel(f, SILKTOUCH) > 0) return COST_SILK;
            if (getUpgradeLevel(f, DOWSING) > 0) return COST_DOWSING;
            return COST;
        }
        @Override public boolean isVisCostPerTick(ItemStack f) { return true; }
        @Override public String getSortingHelperBase() { return "BE"; }
        @Override public int[] getPossibleUpgradesByRank(ItemStack f, int rank) {
            return FociContent.byRank(rank, new int[]{1, 0, TREASURE}, new int[]{1, 0, ENLARGE}, new int[]{1, 0, TREASURE, DOWSING}, new int[]{1, 0, ENLARGE}, new int[]{1, 0, TREASURE, SILKTOUCH});
        }
        @Override public FociContent.Animation getAnimation(ItemStack f) { return FociContent.Animation.CHARGE; }
        @Override public int getFocusColor(ItemStack f) { return 409606; }
        @Override public boolean onFocusRightClick(ItemStack wand, Level level, Player p) { FociContent.startUsing(p, InteractionHand.MAIN_HAND); return true; }

        @Override public void onUsingFocusTick(ItemStack wand, Player p, int count) {
            var level = p.level();
            var focusStack = FociContent.getFocusItem(wand);
            if (!FociContent.consumeAllVis(wand, p, getVisCost(wand), false)) { p.releaseUsingItem(); return; }
            String key = (level.isClientSide ? "R" : "S") + p.getUUID();
            var hit = target(p, 10);
            boolean onBlock = hit.getType() == HitResult.Type.BLOCK;
            Vec3 to = onBlock ? hit.getLocation() : p.getEyePosition().add(p.getViewVector(1).scale(10));
            long[] delay = sound.computeIfAbsent(key, k -> new long[1]);
            if (onBlock) {
                if (!level.isClientSide && delay[0] < System.currentTimeMillis()) {
                    level.playSound(null, to.x, to.y, to.z, FociContent.sound("rumble"), SoundSource.PLAYERS, .3f, 1);
                    delay[0] = System.currentTimeMillis() + 1200;
                }
            } else delay[0] = 0;
            if (level.isClientSide) FociFx.beam(p, to.x, to.y, to.z, 2, 65382, onBlock ? 2 : 0, onBlock ? 5 : 0);
            var pos = onBlock ? hit.getBlockPos() : null;
            if (pos == null || !level.mayInteract(p, pos)) { reset(key); return; }
            var state = level.getBlockState(pos);
            float hardness = state.getDestroySpeed(level, pos);
            if (hardness < 0) return;
            int potency = FociContent.getFocusPotency(wand);
            float speed = .05f + potency * .1f;
            if (state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE) || state.is(net.minecraft.tags.BlockTags.MINEABLE_WITH_SHOVEL)) speed = .25f + potency * .25f;
            if (state.is(Blocks.OBSIDIAN)) speed *= 3;
            if (!pos.equals(last.get(key))) { last.put(key, pos); breakCount.put(key, 0f); return; }
            float done = breakCount.getOrDefault(key, 0f);
            if (level.isClientSide) {
                if (done > 0 && !state.isAir()) FociFx.excavate(p, pos, (int) (done / hardness * 9));
                breakCount.put(key, done >= hardness ? 0 : done + speed);
            } else if (done >= hardness && FociContent.consumeAllVis(wand, p, getVisCost(wand), true)) {
                if (excavate((ServerLevel) level, wand, (ServerPlayer) p, pos)) {
                    for (int a = 0; a < upgrade(wand, ENLARGE); a++)
                        if (FociContent.consumeAllVis(wand, p, getVisCost(wand), false) && breakNeighbour((ServerLevel) level, (ServerPlayer) p, pos, state, wand))
                            FociContent.consumeAllVis(wand, p, getVisCost(wand), true);
                }
                reset(key);
            } else breakCount.put(key, done + speed);
        }
        private void reset(String key) { last.put(key, null); breakCount.put(key, 0f); }

        /** excavate: a cancellable break event, then drops (silk touch or treasure as fortune) and the break effect. */
        private static boolean excavate(ServerLevel level, ItemStack wand, ServerPlayer player, BlockPos pos) {
            var state = level.getBlockState(pos);
            var event = net.neoforged.neoforge.common.CommonHooks.fireBlockBreak(level, player.gameMode.getGameModeForPlayer(), player, pos, state);
            if (event.isCanceled()) return false;
            var tool = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
            var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            if (upgrade(wand, SILKTOUCH) > 0) tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
            else if (upgrade(wand, TREASURE) > 0) tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), upgrade(wand, TREASURE));
            var be = level.getBlockEntity(pos);
            Block.dropResources(state, level, pos, be, player, tool);
            state.spawnAfterBreak(level, pos, tool, true);
            level.removeBlock(pos, false);
            level.levelEvent(2001, pos, Block.getId(state));
            return true;
        }
        private static boolean breakNeighbour(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState state, ItemStack wand) {
            var directions = new ArrayList<>(List.of(Direction.values()));
            Collections.shuffle(directions, new Random(level.random.nextLong()));
            for (var d : directions) {
                var at = pos.relative(d);
                if (level.getBlockState(at) == state && excavate(level, wand, player, at)) return true;
            }
            return false;
        }
        @Override public void onPlayerStoppedUsingFocus(ItemStack wand, Level level, Player p, int count) {
            reset((level.isClientSide ? "R" : "S") + p.getUUID());
            if (level.isClientSide) FociFx.stopBeam(p);
        }
    }

    // ---------------------------------------------------------------- Equal Trade

    /** ItemFocusTrade: sneak-click picks a block; right-click or swing swaps the looked-at block (spreading 3 + enlarge steps). */
    public static final class TradeFocus extends FociContent.Focus implements Warding.Architect {
        private static final AspectList COST = new AspectList().add(Aspect.ENTROPY, 5).add(Aspect.EARTH, 5).add(Aspect.ORDER, 5);
        private static final AspectList COST_SILK = new AspectList().add(Aspect.AIR, 1).add(Aspect.FIRE, 1).add(Aspect.EARTH, 6).add(Aspect.WATER, 1).add(Aspect.ORDER, 6).add(Aspect.ENTROPY, 6);
        @Override public AspectList getVisCost(ItemStack f) { return getUpgradeLevel(f, SILKTOUCH) > 0 ? COST_SILK : COST; }
        @Override public String getSortingHelperBase() { return "BT"; }
        @Override public int[] getPossibleUpgradesByRank(ItemStack f, int rank) {
            return FociContent.byRank(rank, new int[]{1, ENLARGE}, new int[]{1, ENLARGE}, new int[]{1, ENLARGE, TREASURE, FociContent.ARCHITECT}, new int[]{1, ENLARGE}, new int[]{1, ENLARGE, SILKTOUCH});
        }
        @Override public int getFocusColor(ItemStack f) { return 8747923; }

        @Override public int getMaxAreaSize(ItemStack focus) { return 3 + getUpgradeLevel(focus, ENLARGE) * 2; }
        @Override public java.util.List<BlockPos> architectBlocks(ItemStack wand, Level level, BlockPos origin, int side, Player player) {
            var out = new ArrayList<BlockPos>();
            int sx = Warding.areaX(wand), sz = Warding.areaZ(wand);
            if (side == 2 || side == 3) { int t = sx; sx = sz; sz = t; }
            checkNeighbours(level, origin, origin, level.getBlockState(origin), side, sx, sz, player, new java.util.HashSet<>(), out);
            return out;
        }
        private void checkNeighbours(Level level, BlockPos origin, BlockPos pos, BlockState source, int side, int sx, int sz,
                                     Player player, java.util.Set<BlockPos> checked, java.util.List<BlockPos> out) {
            if (!checked.add(pos)) return;
            int dx = Math.abs(pos.getX()-origin.getX()), dy = Math.abs(pos.getY()-origin.getY()), dz = Math.abs(pos.getZ()-origin.getZ());
            if (side < 2 ? dx > sx || dz > sz : side < 4 ? dx > sx || dy > sz : dy > sx || dz > sz) return;
            var state = level.getBlockState(pos);
            if (state != source || !exposed(level, pos) || state.isAir() || state.getDestroySpeed(level, pos) < 0 || !level.mayInteract(player, pos)) return;
            out.add(pos);
            for (var d : Direction.values()) if (d.get3DDataValue() != side && d.getOpposite().get3DDataValue() != side)
                checkNeighbours(level, origin, pos.relative(d), source, side, sx, sz, player, checked, out);
        }
        @Override public boolean showAxis(ItemStack wand, Level level, Player player, int side, Warding.Axis axis) {
            int dim = Warding.areaDim(wand);
            return switch (side) {
                case 0, 1 -> axis == Warding.Axis.X && (dim == 0 || dim == 1) || axis == Warding.Axis.Z && (dim == 0 || dim == 2);
                case 2, 3 -> axis == Warding.Axis.Y && (dim == 0 || dim == 1) || axis == Warding.Axis.X && (dim == 0 || dim == 2);
                default -> axis == Warding.Axis.Y && (dim == 0 || dim == 1) || axis == Warding.Axis.Z && (dim == 0 || dim == 2);
            };
        }

        /** getMovingObjectPositionFromPlayer: reach-limited block ray. */
        private static BlockHitResult reach(Player p) {
            double reach = p.blockInteractionRange();
            var eye = p.getEyePosition();
            return p.level().clip(new ClipContext(eye, eye.add(p.getViewVector(1).scale(reach)), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, p));
        }
        @Override public boolean onFocusRightClick(ItemStack wand, Level level, Player p) {
            var hit = reach(p);
            if (hit.getType() != HitResult.Type.BLOCK) return true;
            var pos = hit.getBlockPos();
            if (p.isShiftKeyDown()) {
                if (!level.isClientSide && level.getBlockEntity(pos) == null) {
                    var state = level.getBlockState(pos);
                    var picked = state.getCloneItemStack(hit, level, pos, p);
                    storePicked(wand, picked.isEmpty() ? new ItemStack(state.getBlock()) : picked.copyWithCount(1));
                } else p.swing(InteractionHand.MAIN_HAND);
                return true;
            }
            var picked = picked(wand);
            if (!picked.isEmpty() && level.isClientSide) p.swing(InteractionHand.MAIN_HAND);
            else if (!picked.isEmpty() && level.getBlockEntity(pos) == null && !isTaint(level.getBlockState(pos))) {
                if (isUpgradedWith(FociContent.getFocusItem(wand), FociContent.ARCHITECT)) {
                    for (var at : architectBlocks(wand, level, pos, hit.getDirection().get3DDataValue(), p))
                        addSwapper((ServerLevel) level, at, level.getBlockState(at), picked, 0, p, p.getInventory().selected);
                } else addSwapper((ServerLevel) level, pos, level.getBlockState(pos), picked, 3 + upgrade(wand, ENLARGE), p, p.getInventory().selected);
            }
            return true;
        }
        /** onEntitySwing: a single swap without spreading. */
        static void swing(Player p, ItemStack wand) {
            if (p.level().isClientSide || !(FociContent.getFocus(wand) instanceof TradeFocus)) return;
            var picked = picked(wand);
            var hit = reach(p);
            if (picked.isEmpty() || hit.getType() != HitResult.Type.BLOCK) return;
            var pos = hit.getBlockPos();
            if (p.level().getBlockEntity(pos) == null && !isTaint(p.level().getBlockState(pos)))
                addSwapper((ServerLevel) p.level(), pos, p.level().getBlockState(pos), picked, 0, p, p.getInventory().selected);
        }
        private static boolean isTaint(BlockState state) {
            var id = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock());
            return id.getNamespace().equals(TC4Modern.MODID) && id.getPath().contains("taint");
        }
        static ItemStack picked(ItemStack wand) {
            var focus = FociContent.getFocusItem(wand);
            var data = focus.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            if (data == null || !data.copyTag().contains("picked")) return ItemStack.EMPTY;
            return ItemStack.parseOptional(FociContent.registries(), data.copyTag().getCompound("picked"));
        }
        private static void storePicked(ItemStack wand, ItemStack picked) {
            var focus = FociContent.getFocusItem(wand);
            net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, focus,
                tag -> tag.put("picked", picked.save(FociContent.registries())));
            FociContent.setFocus(wand, focus);
        }
    }

    private record Swapper(BlockPos pos, BlockState source, ItemStack target, int life, Player player, int slot) {}
    private static final Map<net.minecraft.resources.ResourceKey<Level>, LinkedBlockingQueue<Swapper>> SWAPS = new HashMap<>();

    /** ServerTickEventsFML.addSwapper. */
    static void addSwapper(ServerLevel level, BlockPos pos, BlockState source, ItemStack target, int life, Player player, int slot) {
        if (source.isAir() || source.getDestroySpeed(level, pos) < 0 || sameAsTarget(source, target)) return;
        SWAPS.computeIfAbsent(level.dimension(), k -> new LinkedBlockingQueue<>()).offer(new Swapper(pos, source, target, life, player, slot));
        level.playSound(null, player, FociContent.sound("wand"), SoundSource.PLAYERS, .25f, 1);
    }
    private static boolean sameAsTarget(BlockState state, ItemStack target) {
        return target.getItem() instanceof BlockItem item && item.getBlock() == state.getBlock();
    }

    /** tickBlockSwap: one successful swap per world tick; failed entries are dropped. */
    @SubscribeEvent static void tickSwaps(net.neoforged.neoforge.event.tick.LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        var queue = SWAPS.get(level.dimension());
        if (queue == null) return;
        for (Swapper vs; (vs = queue.poll()) != null; ) {
            var player = vs.player;
            var state = level.getBlockState(vs.pos);
            var wand = player.getInventory().getItem(vs.slot);
            var focus = WandData.isWand(wand) ? FociContent.getFocus(wand) : null;
            if (!player.isAlive() || !level.mayInteract(player, vs.pos) || sameAsTarget(state, vs.target) || focus == null
                || !FociContent.consumeAllVis(wand, player, focus.getVisCost(FociContent.getFocusItem(wand)), false)) continue;
            int carried = carrying(player, vs.target);
            if (player.getAbilities().instabuild) carried = 1;
            if (state != vs.source || carried < 0 || !(vs.target.getItem() instanceof BlockItem blockItem)) continue;
            if (!player.getAbilities().instabuild) {
                player.getInventory().removeItem(carried, 1);
                var tool = new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);
                var enchantments = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                if (upgrade(wand, SILKTOUCH) > 0) tool.enchant(enchantments.getOrThrow(Enchantments.SILK_TOUCH), 1);
                else if (upgrade(wand, TREASURE) > 0) tool.enchant(enchantments.getOrThrow(Enchantments.FORTUNE), upgrade(wand, TREASURE));
                for (var drop : Block.getDrops(state, level, vs.pos, null, player, tool))
                    if (!player.getInventory().add(drop)) level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(level, vs.pos.getX() + .5, vs.pos.getY() + .5, vs.pos.getZ() + .5, drop));
                FociContent.consumeAllVis(wand, player, focus.getVisCost(FociContent.getFocusItem(wand)), true);
            }
            level.setBlock(vs.pos, blockItem.getBlock().defaultBlockState(), Block.UPDATE_ALL);
            blockItem.getBlock().setPlacedBy(level, vs.pos, level.getBlockState(vs.pos), player, vs.target);
            sparkle(level, vs.pos, 0xC0C0FF, 1);
            level.levelEvent(2001, vs.pos, Block.getId(vs.source));
            if (vs.life > 0)
                for (int xx = -1; xx <= 1; xx++) for (int yy = -1; yy <= 1; yy++) for (int zz = -1; zz <= 1; zz++) {
                    if (xx == 0 && yy == 0 && zz == 0) continue;
                    var at = vs.pos.offset(xx, yy, zz);
                    if (level.getBlockState(at) == vs.source && exposed(level, at))
                        queue.offer(new Swapper(at, vs.source, vs.target, vs.life - 1, player, vs.slot));
                }
            break;
        }
    }
    /** InventoryUtils.isPlayerCarrying: slot of a matching stack, or -1. */
    private static int carrying(Player player, ItemStack target) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.items.size(); i++) if (ItemStack.isSameItem(inventory.items.get(i), target)) return i;
        return -1;
    }

    @SubscribeEvent static void swing(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
        if (event.getAction() == net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock.Action.START && WandData.isWand(event.getItemStack())
            && FociContent.getFocus(event.getItemStack()) instanceof TradeFocus) {
            TradeFocus.swing(event.getEntity(), event.getItemStack());
            event.setCanceled(true);
        }
    }

    // ---------------------------------------------------------------- Portable Hole

    /** ItemFocusPortableHole: tunnels up to 33 + 8*enlarge blocks deep for 120 + 60*extend ticks, costing per block. */
    public static final class PortableHoleFocus extends FociContent.Focus {
        private static final AspectList COST = new AspectList().add(Aspect.ENTROPY, 10).add(Aspect.AIR, 10);
        @Override public AspectList getVisCost(ItemStack f) { return COST.copy(); }
        @Override public String getSortingHelperBase() { return "BPH"; }
        @Override public int[] getPossibleUpgradesByRank(ItemStack f, int rank) { return rank >= 1 && rank <= 5 ? new int[]{1, ENLARGE, EXTEND} : null; }
        @Override public int getFocusColor(ItemStack f) { return 594985; }
        @Override public boolean onFocusRightClick(ItemStack wand, Level level, Player p) {
            var hit = target(p, p.blockInteractionRange());
            if (hit.getType() != HitResult.Type.BLOCK) return true;
            var start = hit.getBlockPos();
            if (level.dimension() == OuterDimension.KEY) {
                if (!level.isClientSide)
                    level.playSound(null, start.getX() + .5, start.getY() + .5, start.getZ() + .5, FociContent.sound("wandfail"), SoundSource.PLAYERS, 1, 1);
                p.swing(InteractionHand.MAIN_HAND);
                return true;
            }
            var into = hit.getDirection().getOpposite();
            int max = 33 + upgrade(wand, ENLARGE) * 8, distance = 0;
            for (var at = start; distance < max && holeable(level, at, false); at = at.relative(into)) distance++;
            var cost = getVisCost(FociContent.getFocusItem(wand));
            for (var a : cost.getAspects()) cost.merge(a, cost.getAmount(a) * distance);
            if (!level.isClientSide && FociContent.consumeAllVis(wand, p, cost, true)) {
                short duration = (short) (120 + 60 * upgrade(wand, EXTEND));
                createHole((ServerLevel) level, start, hit.getDirection().get3DDataValue(), (byte) (distance + 1), duration);
            }
            p.swing(InteractionHand.MAIN_HAND);
            if (!level.isClientSide)
                level.playSound(null, start.getX() + .5, start.getY() + .5, start.getZ() + .5, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1, 1);
            return true;
        }
    }

    /** Blocks a hole may replace (createHole) or tunnel through (the distance loop). */
    static boolean holeable(Level level, BlockPos pos, boolean placing) {
        var state = level.getBlockState(pos);
        if (PortableHoleConfig.blocked(state) || state.is(Blocks.BEDROCK) || state.is(HOLE.get()) || state.isAir() || state.getDestroySpeed(level, pos) == -1) return false;
        return !placing || level.getBlockEntity(pos) == null && !state.canBeReplaced();
    }

    /** createHole: remember the block, swap in a hole carrying the remaining depth, sparkle. */
    static boolean createHole(ServerLevel level, BlockPos pos, int side, byte count, short max) {
        if (!holeable(level, pos, true)) return false;
        var old = level.getBlockState(pos);
        level.setBlock(pos, HOLE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof HoleEntity hole) {
            hole.old = old;
            hole.countdownMax = max;
            hole.count = count;
            hole.direction = (byte) side;
            hole.setChanged();
        }
        sparkle(level, pos, 0x400040, 1);
        return true;
    }

    public static final class HoleBlock extends BaseEntityBlock {
        HoleBlock(Properties properties) { super(properties); }
        @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(HoleBlock::new); }
        @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }
        @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return Shapes.empty(); }
        @Override protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) { return true; }
        @Override public ItemStack getCloneItemStack(net.minecraft.world.level.LevelReader level, BlockPos pos, BlockState state) { return ItemStack.EMPTY; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HoleEntity(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return createTickerHelper(type, HOLE_ENTITY.get(), HoleEntity::tick);
        }
    }

    /** TileHole (TileMemory): spreads a 3x3 ring then one step deeper, and restores the remembered block when its time is up. */
    public static final class HoleEntity extends BlockEntity {
        BlockState old = Blocks.AIR.defaultBlockState();
        short countdown, countdownMax = 120;
        byte count, direction;
        public HoleEntity(BlockPos pos, BlockState state) { super(HOLE_ENTITY.get(), pos, state); }

        static void tick(Level level, BlockPos pos, BlockState state, HoleEntity hole) {
            if (level.isClientSide) hole.sparkles();
            else if (hole.countdown == 0 && hole.count > 1 && hole.direction != -1) {
                var server = (ServerLevel) level;
                var axis = Direction.from3DDataValue(hole.direction).getAxis();
                for (int a = 0; a < 9; a++) {
                    if (a / 3 == 1 && a % 3 == 1) continue;
                    int i = a / 3 - 1, j = a % 3 - 1;
                    var ring = switch (axis) { case Y -> pos.offset(i, 0, j); case Z -> pos.offset(i, j, 0); case X -> pos.offset(0, i, j); };
                    createHole(server, ring, -1, (byte) 1, hole.countdownMax);
                }
                var next = pos.relative(Direction.from3DDataValue(hole.direction).getOpposite());
                if (!createHole(server, next, hole.direction, (byte) (hole.count - 1), hole.countdownMax)) hole.count = 0;
            }
            hole.countdown++;
            if (hole.countdown >= hole.countdownMax) {
                if (level.isClientSide) FociFx.blockSparkle(pos, 0x400040, 1);
                else {
                    level.setBlock(pos, hole.old, Block.UPDATE_CLIENTS);
                    level.scheduleTick(pos, hole.old.getBlock(), 2);
                }
            }
        }

        /** surroundwithsparkles: twinkles along the hole's edges that border solid blocks. */
        private void sparkles() {
            var l = level;
            var r = l.random;
            int x = worldPosition.getX(), y = worldPosition.getY(), z = worldPosition.getZ();
            boolean yp = solid(0, 1, 0), xp = solid(1, 0, 0), zp = solid(0, 0, 1), yn = solid(0, -1, 0), xn = solid(-1, 0, 0), zn = solid(0, 0, -1);
            boolean b1 = notHole(0, 1, 0), b2 = notHole(0, -1, 0), b3 = notHole(0, 0, -1), b4 = notHole(0, 0, 1), b5 = notHole(-1, 0, 0), b6 = notHole(1, 0, 0);
            if (!xp && yp && b6) FociFx.sparkle(x + 1, y + 1, z + r.nextFloat(), 2);
            if (!xn && yp && b5) FociFx.sparkle(x, y + 1, z + r.nextFloat(), 2);
            if (!zp && yp && b4) FociFx.sparkle(x + r.nextFloat(), y + 1, z + 1, 2);
            if (!zn && yp && b3) FociFx.sparkle(x + r.nextFloat(), y + 1, z, 2);
            if (!xp && yn && b6) FociFx.sparkle(x + 1, y, z + r.nextFloat(), 2);
            if (!xn && yn && b5) FociFx.sparkle(x, y, z + r.nextFloat(), 2);
            if (!zp && yn && b4) FociFx.sparkle(x + r.nextFloat(), y, z + 1, 2);
            if (!zn && yn && b3) FociFx.sparkle(x + r.nextFloat(), y, z, 2);
            if (!yp && xp && b1) FociFx.sparkle(x + 1, y + 1, z + r.nextFloat(), 2);
            if (!yn && xp && b2) FociFx.sparkle(x + 1, y, z + r.nextFloat(), 2);
            if (!zp && xp && b4) FociFx.sparkle(x + 1, y + r.nextFloat(), z + 1, 2);
            if (!zn && xp && b3) FociFx.sparkle(x + 1, y + r.nextFloat(), z, 2);
            if (!yp && xn && b1) FociFx.sparkle(x, y + 1, z + r.nextFloat(), 2);
            if (!yn && xn && b2) FociFx.sparkle(x, y, z + r.nextFloat(), 2);
            if (!zp && xn && b4) FociFx.sparkle(x, y + r.nextFloat(), z + 1, 2);
            if (!zn && xn && b3) FociFx.sparkle(x, y + r.nextFloat(), z, 2);
            if (!xp && zp && b6) FociFx.sparkle(x + 1, y + r.nextFloat(), z + 1, 2);
            if (!xn && zp && b5) FociFx.sparkle(x, y + r.nextFloat(), z + 1, 2);
            if (!yp && zp && b1) FociFx.sparkle(x + r.nextFloat(), y + 1, z + 1, 2);
            if (!yn && zp && b2) FociFx.sparkle(x + r.nextFloat(), y, z + 1, 2);
            if (!xp && zn && b6) FociFx.sparkle(x + 1, y + r.nextFloat(), z, 2);
            if (!xn && zn && b5) FociFx.sparkle(x, y + r.nextFloat(), z, 2);
            if (!yp && zn && b1) FociFx.sparkle(x + r.nextFloat(), y + 1, z, 2);
            if (!yn && zn && b2) FociFx.sparkle(x + r.nextFloat(), y, z, 2);
        }
        private boolean solid(int dx, int dy, int dz) { var at = worldPosition.offset(dx, dy, dz); return level.getBlockState(at).isSolidRender(level, at); }
        private boolean notHole(int dx, int dy, int dz) { return !level.getBlockState(worldPosition.offset(dx, dy, dz)).is(HOLE.get()); }

        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.saveAdditional(tag, registries);
            tag.put("oldblock", NbtUtils.writeBlockState(old));
            tag.putShort("countdown", countdown);
            tag.putShort("countdownmax", countdownMax);
            tag.putByte("count", count);
            tag.putByte("direction", direction);
        }
        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
            super.loadAdditional(tag, registries);
            old = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), tag.getCompound("oldblock"));
            countdown = tag.getShort("countdown");
            countdownMax = tag.getShort("countdownmax");
            count = tag.getByte("count");
            direction = tag.getByte("direction");
        }
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
            return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
        }
    }

    /**
     * EventHandlerWorld.harvestEvent for a wand holding a dowsing excavation focus: each drop may become a native cluster,
     * chance (0.2 + 0.075 * treasure) times the result's own factor (Utils.addSpecialMiningResult in Config).
     * 1.21 ores drop raw metal, so raw iron/gold/copper map like their ores; copper (oreCopper in the original) gives one cluster per block.
     */
    @EventBusSubscriber(modid = TC4Modern.MODID)
    static final class Dowsing {
        record Special(String cluster, float chance, boolean perBlock) {}
        private static final Map<Item, Special> SPECIAL = new HashMap<>();
        static Map<Item, Special> special() {
            if (SPECIAL.isEmpty()) {
                var iron = new Special("iron_cluster", 1f, false);
                var gold = new Special("gold_cluster", .9f, false);
                var copper = new Special("copper_cluster", 1f, true);
                for (var i : List.of(net.minecraft.world.item.Items.IRON_ORE, net.minecraft.world.item.Items.DEEPSLATE_IRON_ORE, net.minecraft.world.item.Items.RAW_IRON)) SPECIAL.put(i, iron);
                for (var i : List.of(net.minecraft.world.item.Items.GOLD_ORE, net.minecraft.world.item.Items.DEEPSLATE_GOLD_ORE, net.minecraft.world.item.Items.NETHER_GOLD_ORE, net.minecraft.world.item.Items.RAW_GOLD)) SPECIAL.put(i, gold);
                for (var i : List.of(net.minecraft.world.item.Items.COPPER_ORE, net.minecraft.world.item.Items.DEEPSLATE_COPPER_ORE, net.minecraft.world.item.Items.RAW_COPPER)) SPECIAL.put(i, copper);
                SPECIAL.put(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("cinnabar_ore")), new Special("cinnabar_cluster", .9f, false));
                SPECIAL.put(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("deepslate_cinnabar_ore")), new Special("cinnabar_cluster", .9f, false));
            }
            return SPECIAL;
        }
        static Special resolve(ItemStack stack){var base=special().get(stack.getItem());if(base!=null)return base;var addon=dev.eugheist.tc4modern.api.TC4Addons.nativeCluster(stack);return addon==null?null:new Special(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(addon.output().get()).toString(),addon.factor(),addon.perBlock());}
        static Item clusterItem(Special result){var key=result.cluster().contains(":")?net.minecraft.resources.ResourceLocation.parse(result.cluster()):AlchemyContent.id(result.cluster());return net.minecraft.core.registries.BuiltInRegistries.ITEM.get(key);}
        @SubscribeEvent static void drops(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
            if (!(event.getBreaker() instanceof Player player) || event.getDrops().isEmpty()) return;
            var held = player.getMainHandItem();
            boolean pick=held.getItem() instanceof ElementalTools.Pick||held.getItem() instanceof PrimalCrusher.Crusher;
            if (!pick && (!WandData.isWand(held) || upgrade(held, DOWSING) <= 0)) return;
            int fortune=pick?BasicsEnchantments.level(player,held,net.minecraft.world.item.enchantment.Enchantments.FORTUNE):upgrade(held,TREASURE);
            float chance = .2f + fortune * .075f;
            var level = event.getLevel();
            for (var drop : event.getDrops()) {
                var is = drop.getItem();
                var result = resolve(is);
                if (result == null || level.random.nextFloat() > chance * result.chance) continue;
                var cluster = clusterItem(result);
                drop.setItem(new ItemStack(cluster, result.perBlock ? 1 : is.getCount()));
                level.playSound(null, event.getPos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, .2f, .7f + level.random.nextFloat() * .2f);
            }
        }
    }
}
