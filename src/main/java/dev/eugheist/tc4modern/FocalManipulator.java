package dev.eugheist.tc4modern;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * blockStoneDevice metadata 13: the focal manipulator (TileFocalManipulator, ContainerFocalManipulator). A focus placed on
 * it is upgraded to its next rank for 8 experience levels per rank and 2 vis of each of the upgrade's aspects (reduced to
 * primals) doubling every rank, drained from the vis network one centivis-hundred per aspect every five ticks.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FocalManipulator {
    private FocalManipulator() {}
    public static final DeferredHolder<Block, Block> BLOCK = DeferredHolder.create(Registries.BLOCK, AlchemyContent.id("focal_manipulator"));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TableEntity>> ENTITY = DeferredHolder.create(Registries.BLOCK_ENTITY_TYPE, AlchemyContent.id("focal_manipulator"));
    public static final DeferredHolder<MenuType<?>, MenuType<Menu>> MENU = DeferredHolder.create(Registries.MENU, AlchemyContent.id("focal_manipulator"));
    public static final int XP_MULT = 8, VIS_MULT = 200;

    @SubscribeEvent static void register(RegisterEvent event) {
        // BlockStoneDevice: stone, hardness 3, resistance 25.
        event.register(Registries.BLOCK, h -> h.register(BLOCK.getId(), new TableBlock(BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(3, TC4SoundTypes.res(25))
            .sound(SoundType.STONE).requiresCorrectToolForDrops().noOcclusion())));
        event.register(Registries.BLOCK_ENTITY_TYPE, h -> h.register(ENTITY.getId(), BlockEntityType.Builder.of(TableEntity::new, BLOCK.get()).build(null)));
        event.register(Registries.ITEM, h -> h.register(BLOCK.getId(), new BlockItem(BLOCK.get(), new Item.Properties())));
        event.register(Registries.MENU, h -> h.register(MENU.getId(), net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id, inv, buf) ->
            new Menu(id, inv, inv.player.level().getBlockEntity(buf.readBlockPos()) instanceof TableEntity t ? t : null))));
    }

    /** The cost of one upgrade rank: VIS_MULT doubled per rank above the first, for every aspect of the type, reduced to primals. */
    static AspectList cost(int upgrade, int rank) {
        int amt = VIS_MULT;
        for (int a = 1; a < rank; ++a) amt *= 2;
        var tal = new AspectList();
        var type = FociContent.upgradeType(upgrade);
        if (type != null) for (Aspect as : type.aspects().getAspects()) tal.add(as, amt);
        return thaumcraft.common.lib.research.ResearchManager.reduceToPrimals(tal);
    }
    static void sound(Level level, BlockPos pos, String name, float volume) {
        level.playSound(null, pos, FociContent.sound(name), SoundSource.BLOCKS, volume, 1f);
    }

    public static final class TableBlock extends BaseEntityBlock {
        TableBlock(Properties properties) { super(properties); }
        @Override protected MapCodec<? extends BaseEntityBlock> codec() { return simpleCodec(TableBlock::new); }
        @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
        @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new TableEntity(pos, state); }
        @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
            return createTickerHelper(type, ENTITY.get(), level.isClientSide ? TableEntity::clientTick : TableEntity::serverTick);
        }
        /** onBlockActivated: opens only once FOCALMANIPULATION is known; sneaking falls through. */
        @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof TableEntity table)) return InteractionResult.PASS;
            if (player instanceof ServerPlayer server) {
                if (ResearchProgress.completed(server.getPersistentData()).contains("FOCALMANIPULATION"))
                    server.openMenu(new SimpleMenuProvider((id, inv, p) -> new Menu(id, inv, table), Component.translatable("tile.blockStoneDevice.13.name")), buf -> buf.writeBlockPos(pos));
                else server.sendSystemMessage(Component.translatable("tc.researchmissing").withStyle(ChatFormatting.RED));
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
            if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof TableEntity table) net.minecraft.world.Containers.dropContents(level, pos, table);
            super.onRemove(state, level, pos, next, moved);
        }
    }

    public static final class TableEntity extends BlockEntity implements Container {
        private final NonNullList<ItemStack> items = NonNullList.withSize(1, ItemStack.EMPTY);
        public AspectList aspects = new AspectList();
        public int size, upgrade = -1, rank = -1;
        private int ticks;
        /** Client: set when the focus changed so the screen recomputes its upgrade list. */
        public boolean reset;
        public TableEntity(BlockPos pos, BlockState state) { super(ENTITY.get(), pos, state); }

        static void serverTick(Level level, BlockPos pos, BlockState state, TableEntity t) {
            boolean complete = false;
            if (t.rank < 0) t.rank = 0;
            if (++t.ticks % 5 != 0) return;
            if (t.size > 0 && (t.aspects.visSize() <= 0 || t.getItem(0).isEmpty())) {
                complete = true;
                sound(level, pos, "craftfail", .33f);
            }
            if (t.size > 0) {
                for (Aspect aspect : t.aspects.getAspectsSortedAmount()) {
                    int drain = VisNetwork.drain((ServerLevel) level, pos, aspect, Math.min(100, t.aspects.getAmount(aspect)), (node, color) -> {});
                    if (drain <= 0) continue;
                    t.aspects.reduce(aspect, drain);
                    t.sync();
                }
                if (t.aspects.visSize() <= 0 && t.getItem(0).getItem() instanceof FociContent.Focus focus) {
                    complete = true;
                    focus.applyUpgrade(t.getItem(0), t.upgrade, t.rank);
                    sound(level, pos, "wand", 1f);
                }
            }
            if (complete) {
                t.size = 0;
                t.rank = -1;
                t.aspects = new AspectList();
                t.sync();
            }
        }
        /** While working: drifting pale sparkles (FXGeneric cells 112-120) above the table. */
        static void clientTick(Level level, BlockPos pos, BlockState state, TableEntity t) {
            if (t.size <= 0) return;
            var r = level.random;
            FociFx.generic(pos.getX() + .5 + (r.nextFloat() - r.nextFloat()) * .3f, pos.getY() + 1.25 + (r.nextFloat() - r.nextFloat()) * .3f, pos.getZ() + .5 + (r.nextFloat() - r.nextFloat()) * .3f,
                0, 0, 0, .5f + r.nextFloat() * .4f, 1 - r.nextFloat() * .4f, 1 - r.nextFloat() * .4f, .8f, false, 112, 9, 1, 6 + r.nextInt(5), 0, .7f + r.nextFloat() * .4f);
        }

        /** startCraft: the next free rank, enough levels, an offered and applicable upgrade; levels are paid up front. */
        public boolean startCraft(int id, Player p) {
            var stack = getItem(0);
            if (size > 0 || !(stack.getItem() instanceof FociContent.Focus focus)) return false;
            short[] s = focus.getAppliedUpgrades(stack);
            rank = 1;
            while (rank <= 5 && s[rank - 1] != -1) ++rank;
            int xp = rank * XP_MULT;
            if (p.experienceLevel < xp) return false;
            int[] ut = focus.getPossibleUpgradesByRank(stack, rank);
            if (ut == null) return false;
            boolean offered = false;
            for (int u : ut) if (u == id) { offered = true; break; }
            if (!offered || FociContent.upgradeType(id) == null || !focus.canApplyUpgrade(stack, p, id, rank)) return false;
            aspects = cost(id, rank);
            size = aspects.visSize();
            upgrade = id;
            if (!p.getAbilities().instabuild) p.giveExperienceLevels(-xp);
            sync();
            sound(level, worldPosition, "craftstart", .25f);
            return true;
        }

        void sync() { setChanged(); if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

        @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
            super.saveAdditional(tag, lookup);
            ContainerHelper.saveAllItems(tag, items, lookup);
            aspects.writeToNBT(tag);
            tag.putInt("size", size);
            tag.putInt("upgrade", upgrade);
            tag.putInt("rank", rank);
        }
        @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
            super.loadAdditional(tag, lookup);
            var before = items.get(0).copy();
            items.set(0, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(tag, items, lookup);
            aspects = new AspectList();
            aspects.readFromNBT(tag);
            size = tag.getInt("size");
            upgrade = tag.getInt("upgrade");
            rank = tag.getInt("rank");
            if (level != null && level.isClientSide && !ItemStack.matches(before, items.get(0))) reset = true;
        }
        @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) { return saveWithoutMetadata(lookup); }
        @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }

        @Override public int getContainerSize() { return 1; }
        @Override public boolean isEmpty() { return items.get(0).isEmpty(); }
        @Override public ItemStack getItem(int slot) { return items.get(slot); }
        @Override public ItemStack removeItem(int slot, int n) { var r = ContainerHelper.removeItem(items, slot, n); if (!r.isEmpty()) changedSlot(); return r; }
        @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
        /** setInventorySlotContents: a new focus clears any pending cost. */
        @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); changedSlot(); }
        private void changedSlot() { if (level != null && level.isClientSide) reset = true; else aspects = new AspectList(); sync(); }
        @Override public void clearContent() { items.clear(); }
        @Override public int getMaxStackSize() { return 1; }
        @Override public boolean canPlaceItem(int slot, ItemStack stack) { return stack.getItem() instanceof FociContent.Focus; }
        @Override public boolean stillValid(Player p) { return level != null && level.getBlockEntity(worldPosition) == this && p.distanceToSqr(worldPosition.getCenter()) <= 64; }
    }

    public static final class Menu extends AbstractContainerMenu {
        public final TableEntity table;
        private final Container input;
        public Menu(int id, Inventory inv, TableEntity table) {
            super(MENU.get(), id);
            this.table = table;
            input = table != null ? table : new net.minecraft.world.SimpleContainer(1);
            // SlotLimitedByClass(ItemFocusBasic), one focus.
            addSlot(new Slot(input, 0, 88, 60) {
                @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof FociContent.Focus; }
                @Override public int getMaxStackSize() { return 1; }
            });
            for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inv, j + i * 9 + 9, 16 + j * 18, 151 + i * 18));
            for (int i = 0; i < 9; ++i) addSlot(new Slot(inv, i, 16 + i * 18, 209));
        }
        /** enchantItem: the button id is the chosen upgrade; a refusal plays craftfail at the table. */
        @Override public boolean clickMenuButton(Player p, int button) {
            if (table == null || button < 0) return false;
            if (!table.startCraft(button, p)) sound(table.getLevel(), table.getBlockPos(), "craftfail", .33f);
            return true;
        }
        @Override public boolean stillValid(Player p) { return table != null && table.stillValid(p); }
        @Override public ItemStack quickMoveStack(Player p, int index) {
            var slot = slots.get(index);
            if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
            var stack = slot.getItem();
            var copy = stack.copy();
            if (index == 0) { if (!moveItemStackTo(stack, 1, 37, false)) return ItemStack.EMPTY; }
            else if (stack.getItem() instanceof FociContent.Focus) { if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY; }
            else if (index < 28) { if (!moveItemStackTo(stack, 28, 37, false)) return ItemStack.EMPTY; }
            else if (!moveItemStackTo(stack, 1, 28, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
            slot.onTake(p, stack);
            return copy;
        }
    }
}
