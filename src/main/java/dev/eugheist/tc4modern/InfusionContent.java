package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.infusion.InfusionEnchantments;
import thaumcraft.common.tiles.infusion.InfusionMatrixLogic;

/** Infusion altar: arcane pedestal, runic matrix, infusion pillars, salis mundus and the enchantment recipes. */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class InfusionContent {
    private InfusionContent() {}
    private static DeferredHolder<Block, Block> block(String n) { return DeferredHolder.create(Registries.BLOCK, AlchemyContent.id(n)); }
    public static final DeferredHolder<Block, Block> PEDESTAL = block("arcane_pedestal");
    public static final DeferredHolder<Block, Block> MATRIX = block("runic_matrix");
    public static final DeferredHolder<Block, Block> PILLAR = block("infusion_pillar");
    public static final DeferredHolder<Block, Block> PILLAR_TOP = block("infusion_pillar_top");
    public static final DeferredHolder<Item, Item> SALIS = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("salis_mundus"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PedestalEntity>> PEDESTAL_ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("arcane_pedestal"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MatrixEntity>> MATRIX_ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("runic_matrix"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PillarEntity>> PILLAR_ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("infusion_pillar"));

    @SubscribeEvent
    static void register(RegisterEvent event) {
        var stone = BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, TC4SoundTypes.res(25)).sound(SoundType.STONE).requiresCorrectToolForDrops();
        event.register(Registries.BLOCK, h -> {
            h.register(PEDESTAL.getId(), new PedestalBlock(stone.noOcclusion()));
            h.register(MATRIX.getId(), new MatrixBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3f, TC4SoundTypes.res(25)).sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion().lightLevel(s -> 10)));
            h.register(PILLAR.getId(), new PillarBlock(stone.noOcclusion()));
            h.register(PILLAR_TOP.getId(), new PillarTopBlock(stone.noOcclusion()));
        });
        event.register(Registries.BLOCK_ENTITY_TYPE, h -> {
            h.register(PEDESTAL_ENTITY.getId(), BlockEntityType.Builder.of(PedestalEntity::new, PEDESTAL.get()).build(null));
            h.register(MATRIX_ENTITY.getId(), BlockEntityType.Builder.of(MatrixEntity::new, MATRIX.get()).build(null));
            h.register(PILLAR_ENTITY.getId(), BlockEntityType.Builder.of(PillarEntity::new, PILLAR.get()).build(null));
        });
        event.register(Registries.ITEM, h -> {
            h.register(PEDESTAL.getId(), new BlockItem(PEDESTAL.get(), new Item.Properties()));
            h.register(MATRIX.getId(), new BlockItem(MATRIX.get(), new Item.Properties()));
            h.register(SALIS.getId(), new Item(new Item.Properties()));
        });
    }

    // ---- client FX payload (PacketFXInfusionSource / PacketFXEssentiaSource / PacketFXBlockZap / sparkle) ----
    public record FxPayload(int kind, double x0, double y0, double z0, double x1, double y1, double z1, int data) implements CustomPacketPayload {
        public static final Type<FxPayload> TYPE = new Type<>(AlchemyContent.id("infusion_fx"));
        public static final StreamCodec<FriendlyByteBuf, FxPayload> CODEC = StreamCodec.of((b, p) -> {
            b.writeVarInt(p.kind); b.writeDouble(p.x0); b.writeDouble(p.y0); b.writeDouble(p.z0); b.writeDouble(p.x1); b.writeDouble(p.y1); b.writeDouble(p.z1); b.writeInt(p.data);
        }, b -> new FxPayload(b.readVarInt(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readDouble(), b.readInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    @SubscribeEvent
    static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("claude-infusion-1").playToClient(FxPayload.TYPE, FxPayload.CODEC, (payload, context) -> context.enqueueWork(() -> InfusionFx.handle(payload)));
    }
    static void sendFx(Level level, BlockPos near, FxPayload payload) {
        if (level instanceof ServerLevel server) PacketDistributor.sendToPlayersNear(server, null, near.getX(), near.getY(), near.getZ(), 32, payload);
    }

    // ---- pedestal ----
    public static final class PedestalEntity extends BlockEntity {
        private ItemStack item = ItemStack.EMPTY;
        public PedestalEntity(BlockPos pos, BlockState state) { super(PEDESTAL_ENTITY.get(), pos, state); }
        public ItemStack item() { return item; }
        public void setItem(ItemStack stack) { item = stack; setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) { super.saveAdditional(tag, r); if (!item.isEmpty()) tag.put("Item", item.save(r)); }
        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) { super.loadAdditional(tag, r); item = tag.contains("Item") ? ItemStack.parse(r, tag.get("Item")).orElse(ItemStack.EMPTY) : ItemStack.EMPTY; }
        // A non-empty update tag even when the pedestal is empty: empty tags are ignored by onDataPacket.
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { var tag = saveWithoutMetadata(r); tag.putBoolean("synced", true); return tag; }
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    }
    /** BlockStoneDevice metadata 1: base, stem and top slabs; one item, right click to place or take. */
    public static final class PedestalBlock extends BaseEntityBlock {
        public static final MapCodec<PedestalBlock> CODEC = simpleCodec(PedestalBlock::new);
        private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(4, 4, 4, 12, 12, 12), Block.box(2, 12, 2, 14, 16, 14));
        public PedestalBlock(Properties p) { super(p); }
        @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.MODEL; }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PedestalEntity(pos, state); }
        @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!(level.getBlockEntity(pos) instanceof PedestalEntity ped)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!ped.item().isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (!level.isClientSide) {
                ped.setItem(held.copyWithCount(1));
                held.shrink(1);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .2f, ((level.random.nextFloat() - level.random.nextFloat()) * .7f + 1f) * 1.6f);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (!(level.getBlockEntity(pos) instanceof PedestalEntity ped) || ped.item().isEmpty()) return InteractionResult.PASS;
            if (!level.isClientSide) {
                ItemStack drop = ped.item();
                ped.setItem(ItemStack.EMPTY);
                if (!player.getInventory().add(drop)) player.drop(drop, false);
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, .2f, ((level.random.nextFloat() - level.random.nextFloat()) * .7f + 1f) * 1.5f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
            if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof PedestalEntity ped && !ped.item().isEmpty())
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), ped.item());
            super.onRemove(state, level, pos, replacement, moving);
        }
    }

    // ---- pillars ----
    public static final class PillarEntity extends BlockEntity {
        public byte orientation;
        public PillarEntity(BlockPos pos, BlockState state) { super(PILLAR_ENTITY.get(), pos, state); }
        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) { super.saveAdditional(tag, r); tag.putByte("orientation", orientation); }
        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) { super.loadAdditional(tag, r); orientation = tag.getByte("orientation"); }
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { return saveWithoutMetadata(r); }
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    }
    /** BlockStoneDevice metadata 3: lower pillar half, collision 0..0.5, drops arcane stone bricks; needs its top. */
    public static final class PillarBlock extends BaseEntityBlock {
        public static final MapCodec<PillarBlock> CODEC = simpleCodec(PillarBlock::new);
        public PillarBlock(Properties p) { super(p); }
        @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.ENTITYBLOCK_ANIMATED; }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(0, 0, 0, 16, 8, 16); }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PillarEntity(pos, state); }
        @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
            if (!level.isClientSide && !level.getBlockState(pos.above()).is(PILLAR_TOP.get())) { dropResources(state, level, pos); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); }
        }
    }
    /** BlockStoneDevice metadata 4: upper pillar half (collision -1..-0.5), drops arcane stone. */
    public static final class PillarTopBlock extends Block {
        public PillarTopBlock(Properties p) { super(p); }
        @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
        @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return Block.box(0, 0, 0, 16, 16, 16); }
        @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moving) {
            if (!level.isClientSide && !level.getBlockState(pos.below()).is(PILLAR.get())) { dropResources(state, level, pos); level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3); }
        }
    }

    // ---- matrix ----
    public static final class MatrixEntity extends BlockEntity implements InfusionMatrixLogic.Host {
        public final InfusionMatrixLogic logic = new InfusionMatrixLogic();
        public float startUp;
        public int craftCount, count;
        public MatrixEntity(BlockPos pos, BlockState state) { super(MATRIX_ENTITY.get(), pos, state); }
        public static void serverTick(Level level, BlockPos pos, BlockState state, MatrixEntity tile) { tile.logic.serverTick(tile); }
        /** TileInfusionMatrix.doEffects: sounds, rune particles, start-up spin and instability bolts. */
        public static void clientTick(Level level, BlockPos pos, BlockState state, MatrixEntity tile) {
            tile.count++;
            var rand = level.random;
            if (tile.logic.crafting) {
                if (tile.craftCount == 0) level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), sound("infuserstart"), SoundSource.BLOCKS, .5f, 1f, false);
                else if (tile.craftCount % 65 == 0) level.playLocalSound(pos.getX(), pos.getY(), pos.getZ(), sound("infuser"), SoundSource.BLOCKS, .5f, 1f, false);
                ++tile.craftCount;
                ScanRunes.blockRunes((net.minecraft.client.multiplayer.ClientLevel) level, pos.getX(), pos.getY() - 2, pos.getZ(), .5f + rand.nextFloat() * .2f, .1f, .7f + rand.nextFloat() * .3f, 25, -.03f);
            } else if (tile.craftCount > 0) {
                tile.craftCount -= 2;
                if (tile.craftCount < 0) tile.craftCount = 0;
                if (tile.craftCount > 50) tile.craftCount = 50;
            }
            if (tile.logic.active && tile.startUp != 1f) {
                if (tile.startUp < 1f) tile.startUp += Math.max(tile.startUp / 10f, .001f);
                if (tile.startUp > .999) tile.startUp = 1f;
            }
            if (!tile.logic.active && tile.startUp > 0f) {
                tile.startUp -= tile.startUp / 10f;
                if (tile.startUp < .001) tile.startUp = 0f;
            }
            if (tile.logic.crafting && tile.logic.instability > 0 && rand.nextInt(200) <= tile.logic.instability)
                InfusionFx.bolt(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, pos.getX() + .5 + (rand.nextFloat() - rand.nextFloat()) * 2, pos.getY() + .5 + (rand.nextFloat() - rand.nextFloat()) * 2, pos.getZ() + .5 + (rand.nextFloat() - rand.nextFloat()) * 2);
        }
        static SoundEvent sound(String name) { return SoundEvent.createVariableRangeEvent(AlchemyContent.id(name)); }
        // Host
        public Level level() { return level; }
        public BlockPos pos() { return worldPosition; }
        public boolean isPedestal(BlockPos p) { return level.getBlockEntity(p) instanceof PedestalEntity; }
        public boolean isPillar(BlockPos p) { return level.getBlockEntity(p) instanceof PillarEntity; }
        public ItemStack pedestalItem(BlockPos p) { return level.getBlockEntity(p) instanceof PedestalEntity ped ? ped.item() : ItemStack.EMPTY; }
        public void setPedestalItem(BlockPos p, ItemStack s) { if (level.getBlockEntity(p) instanceof PedestalEntity ped) ped.setItem(s); }
        /** Blocks.skull (all vanilla skulls/heads), original candles and all original crystal variants. */
        public boolean stabilises(BlockPos p) { var s = level.getBlockState(p); return s.getBlock() instanceof net.minecraft.world.level.block.AbstractSkullBlock || s.getBlock() instanceof CrystalClusterBlock || s.getBlock() instanceof OuterCrystal.CrystalBlock || s.getBlock() instanceof ThaumicCandleBlock; }
        public boolean drainEssentia(Aspect aspect) { return EssentiaSources.drain(this, aspect, 12); }
        public boolean researchComplete(Player player, String key) { return ResearchProgress.completed(player.getPersistentData()).contains(key); }
        public InfusionMatrixLogic.InfusionRecipeMatch findRecipe(List<ItemStack> components, ItemStack central, Player player) {
            if (researchComplete(player, "JARBRAIN") && central.is(EssentiaDevices.JAR.get().asItem())) {
                List<ItemStack> remaining = new ArrayList<>(components);
                List<ItemStack> ingredients = BrainJarRecipe.ingredients();
                boolean matches = true;
                for (ItemStack ingredient : ingredients) {
                    int index = -1;
                    for (int i = 0; i < remaining.size(); i++)
                        if (remaining.get(i).is(ingredient.getItem())) { index = i; break; }
                    if (index < 0) { matches = false; break; }
                    remaining.remove(index);
                }
                if (matches && remaining.isEmpty())
                    return new InfusionMatrixLogic.InfusionRecipeMatch(0, ingredients,
                        BrainJarRecipe.output(), null, BrainJarRecipe.INSTABILITY, BrainJarRecipe.cost(), 0);
            }
            if(RunicAugmentation.matches(components,central,ResearchProgress.completed(player.getPersistentData())))return new InfusionMatrixLogic.InfusionRecipeMatch(0,RunicAugmentation.components(central),RunicAugmentation.output(central),null,RunicAugmentation.instability(central),RunicAugmentation.cost(central),0);
            var item = InfusionItemRecipes.match(components, central, ResearchProgress.completed(player.getPersistentData()));
            if (item != null) {
                List<ItemStack> ing = new ArrayList<>();
                for (var comp : item.componentStacks()) ing.add(comp.copy());
                return new InfusionMatrixLogic.InfusionRecipeMatch(0, ing, item.outputStack(central), null, item.instability(), item.aspects().copy(), 0);
            }
            if (!researchComplete(player, "INFUSIONENCHANTMENT")) return null;
            for (var r : InfusionEnchantments.all(() -> SALIS.get())) {
                var holder = InfusionEnchantments.holder(level.registryAccess(), r.enchantment());
                if (!r.acceptsCentral(central, holder)) continue;
                List<ItemStack> remaining = new ArrayList<>(components);
                boolean ok = true;
                for (var comp : r.components()) {
                    int idx = -1;
                    for (int a = 0; a < remaining.size(); a++) if (remaining.get(a).is(comp.get())) { idx = a; break; }
                    if (idx < 0) { ok = false; break; }
                    remaining.remove(idx);
                }
                if (!ok || !remaining.isEmpty()) continue;
                AspectList cost = r.aspects().copy();
                float mod = r.getEssentiaMod(central, holder);
                for (Aspect as : cost.getAspects()) cost.add(as, (int) (cost.getAmount(as) * mod));
                List<ItemStack> ing = new ArrayList<>();
                for (var comp : r.components()) ing.add(new ItemStack(comp.get()));
                return new InfusionMatrixLogic.InfusionRecipeMatch(1, ing, null, holder, r.calcInstability(central), cost, r.calcXP(central, holder));
            }
            return null;
        }
        public void sync() { setChanged(); level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }
        public void sound(String event, float volume, float pitch) { level.playSound(null, worldPosition, sound(event), SoundSource.BLOCKS, volume, pitch); }
        public void fxSource(BlockPos from, int entityId) {
            sendFx(level, worldPosition, new FxPayload(entityId != 0 ? 4 : 1, from.getX(), from.getY(), from.getZ(), worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), entityId));
        }
        public void fxZap(double x0, double y0, double z0, double x1, double y1, double z1) { sendFx(level, worldPosition, new FxPayload(2, x0, y0, z0, x1, y1, z1, 0)); }
        public void fxSparkle(BlockPos p) { sendFx(level, worldPosition, new FxPayload(3, p.getX(), p.getY() + 1, p.getZ(), 5, 0, 0, 0xC000C0)); }
        public void fxFinish(BlockPos p) {
            sendFx(level, worldPosition, new FxPayload(3, p.getX(), p.getY() + 1, p.getZ(), 10, 0, 0, -9999));
            // DEVIATION: TC4 4.2.3.5 has no completion sound (craftingFinish only fires pedestal event 12 sparkles);
            // the user asked for one, so reuse the original "wand" SFX that also plays when the altar is formed.
            level.playSound(null, p.above(), sound("wand"), SoundSource.BLOCKS, .6f, 1.2f);
        }
        public void fluxGoo(BlockPos p) { level.setBlock(p, TC4Modern.FLUX_GOO.get().defaultBlockState().setValue(FluxBlock.AMOUNT, 8), 3); }
        public void fluxGas(BlockPos p) { level.setBlock(p, TC4Modern.FLUX_GAS.get().defaultBlockState().setValue(FluxBlock.AMOUNT, 8), 3); }
        public void taint(LivingEntity e) { e.addEffect(new MobEffectInstance(TC4Modern.FLUX_TAINT, 120, 0, false, true)); }
        public void visExhaust(LivingEntity e) { e.addEffect(new MobEffectInstance(TC4Modern.VIS_EXHAUST, 2400, 0, true, true)); }
        public void warp(Player p, int amount, boolean sticky) {
            PlayerWarp.add(p, sticky ? WarpState.Kind.STICKY : WarpState.Kind.TEMPORARY, amount);
        }
        public void playerCrafted(Player p, ItemStack result) { p.awardStat(net.minecraft.stats.Stats.ITEM_CRAFTED.get(result.getItem()), 1); }
        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider r) { super.saveAdditional(tag, r); logic.save(tag, r); }
        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider r) { super.loadAdditional(tag, r); logic.load(tag, r); }
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider r) { var tag = new CompoundTag(); tag.putBoolean("active", logic.active); tag.putBoolean("crafting", logic.crafting); tag.putShort("instability", (short) logic.instability); logic.recipeEssentia.writeToNBT(tag); return tag; }
        @Override public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider r) {
            logic.active = tag.getBoolean("active"); logic.crafting = tag.getBoolean("crafting"); logic.instability = tag.getShort("instability");
            logic.recipeEssentia = new AspectList(); logic.recipeEssentia.readFromNBT(tag);
        }
        @Override public void onDataPacket(net.minecraft.network.Connection net, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider r) { handleUpdateTag(pkt.getTag(), r); }
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }
    }
    /** BlockStoneDevice metadata 2: wand forms the altar, activates it and starts crafting. */
    public static final class MatrixBlock extends BaseEntityBlock {
        public static final MapCodec<MatrixBlock> CODEC = simpleCodec(MatrixBlock::new);
        public MatrixBlock(Properties p) { super(p); }
        @Override protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }
        @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.ENTITYBLOCK_ANIMATED; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new MatrixEntity(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return level.isClientSide ? createTickerHelper(type, MATRIX_ENTITY.get(), MatrixEntity::clientTick) : createTickerHelper(type, MATRIX_ENTITY.get(), MatrixEntity::serverTick);
        }
        @Override protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
            if (!WandData.isWand(held) || !(level.getBlockEntity(pos) instanceof MatrixEntity tile)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            // WandManager.createInfusionAltar first, then TileInfusionMatrix.onWandRightClick.
            if (!tile.logic.validLocation(tile) && createAltar(held, player, level, pos)) return ItemInteractionResult.SUCCESS;
            tile.logic.wand(tile, player);
            return ItemInteractionResult.SUCCESS;
        }
        @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState replacement, boolean moving) {
            if (!state.is(replacement.getBlock()) && level.getBlockEntity(pos) instanceof MatrixEntity tile && tile.logic.crafting)
                level.explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 2f, Level.ExplosionInteraction.BLOCK);
            super.onRemove(state, level, pos, replacement, moving);
        }
    }

    /** WandManager.createInfusionAltar/fitInfusionAltar/replaceInfusionAltar with 25 of each primal vis. */
    static boolean createAltar(ItemStack wand, Player player, Level world, BlockPos matrix) {
        if (!ResearchProgress.completed(player.getPersistentData()).contains("INFUSION")) return false;
        int x = matrix.getX(), y = matrix.getY(), z = matrix.getZ();
        var cost = new AspectList().add(Aspect.FIRE, 25).add(Aspect.EARTH, 25).add(Aspect.ORDER, 25).add(Aspect.AIR, 25).add(Aspect.ENTROPY, 25).add(Aspect.WATER, 25);
        for (int xx = x - 2; xx <= x; ++xx) for (int yy = y - 2; yy <= y; ++yy) for (int zz = z - 2; zz <= z; ++zz) {
            if (!fitAltar(world, xx, yy, zz) || !WandData.consumeCrafting(wand, cost, true, player)) continue;
            replaceAltar(world, xx, yy, zz);
            return true;
        }
        return false;
    }
    static boolean fitAltar(Level world, int x, int y, int z) {
        // blueprint[yy][xx][zz]: 0 top matrix, 1 arcane stone corners, 2 arcane stone brick corners and pedestal centre.
        for (int yy = 0; yy < 3; ++yy) for (int xx = 0; xx < 3; ++xx) for (int zz = 0; zz < 3; ++zz) {
            BlockPos p = new BlockPos(x + xx, y - yy + 2, z + zz);
            var s = world.getBlockState(p);
            boolean corner = xx != 1 && zz != 1;
            if (yy == 0 && xx == 1 && zz == 1) { if (!s.is(MATRIX.get())) return false; continue; }
            if (yy == 1 && corner) { if (!s.is(TC4Modern.ARCANE_STONE.get())) return false; continue; }
            if (yy == 2 && corner) { if (!s.is(TC4Modern.ARCANE_STONE_BRICKS.get())) return false; continue; }
            if (yy == 2 && xx == 1 && zz == 1) { if (!(world.getBlockEntity(p) instanceof PedestalEntity)) return false; continue; }
            if (!s.isAir()) return false;
        }
        return true;
    }
    static void replaceAltar(Level world, int x, int y, int z) {
        int[][][] blueprint = {{{0, 0, 0}, {0, 9, 0}, {0, 0, 0}}, {{1, 0, 1}, {0, 0, 0}, {1, 0, 1}}, {{2, 0, 3}, {0, 0, 0}, {4, 0, 5}}};
        for (int yy = 0; yy < 3; ++yy) for (int xx = 0; xx < 3; ++xx) for (int zz = 0; zz < 3; ++zz) {
            int b = blueprint[yy][xx][zz];
            BlockPos p = new BlockPos(x + xx, y - yy + 2, z + zz);
            if (b == 1) { world.setBlock(p, PILLAR_TOP.get().defaultBlockState(), 2); sendFx(world, p, new FxPayload(3, p.getX(), p.getY(), p.getZ(), 0, 0, 0, 0xC000C0)); }
            if (b > 1 && b < 9) {
                world.setBlock(p, PILLAR.get().defaultBlockState(), 2);
                if (world.getBlockEntity(p) instanceof PillarEntity tip) { tip.orientation = (byte) b; tip.setChanged(); world.sendBlockUpdated(p, tip.getBlockState(), tip.getBlockState(), 3); }
                sendFx(world, p, new FxPayload(3, p.getX(), p.getY(), p.getZ(), 0, 0, 0, 0xC000C0));
            }
            if (b == 9 && world.getBlockEntity(p) instanceof MatrixEntity tis) { tis.logic.active = true; tis.sync(); }
        }
        world.playSound(null, new BlockPos(x, y, z), SoundEvent.createVariableRangeEvent(AlchemyContent.id("wand")), SoundSource.BLOCKS, 1f, 1f);
    }
}
