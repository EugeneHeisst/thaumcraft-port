package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.essentia.AlchemyFurnaceState;

/** Modern host for the original TileAlchemyFurnace: inventory, fuel, alembic stack and synchronisation. */
public final class AlchemicalFurnaceEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private static final int[] SLOTS_BOTTOM = {1}, SLOTS_TOP = {}, SLOTS_SIDES = {0};
    public final AlchemyFurnaceState state = new AlchemyFurnaceState();
    private final NonNullList<ItemStack> items = NonNullList.withSize(2, ItemStack.EMPTY);
    private final java.util.Random random = new java.util.Random();
    public final ContainerData data = new ContainerData() {
        public int get(int i) { return switch (i) { case 0 -> state.furnaceCookTime; case 1 -> state.furnaceBurnTime; case 2 -> state.currentItemBurnTime; case 3 -> state.vis; default -> state.smeltTime; }; }
        public void set(int i, int v) { switch (i) { case 0 -> state.furnaceCookTime = v; case 1 -> state.furnaceBurnTime = v; case 2 -> state.currentItemBurnTime = v; case 3 -> state.vis = v; default -> state.smeltTime = v; } }
        public int getCount() { return 5; }
    };
    public AlchemicalFurnaceEntity(BlockPos pos, BlockState blockState) { super(EssentiaDevices.FURNACE_ENTITY.get(), pos, blockState); }

    static AspectList tags(ItemStack stack, Level level) {
        if (stack.isEmpty()) return null;
        var map = RecipeAspects.get(stack, level);
        if (map == null || map.isEmpty()) return null;
        var list = new AspectList();
        map.forEach((tag, amount) -> { var a = thaumcraft.api.aspects.Aspect.getAspect(tag); if (a != null && amount > 0) list.add(a, amount); });
        return list.size() > 0 ? list : null;
    }
    int countBellows() {
        int count = 0;
        for (Direction dir : Direction.values()) {
            var p = worldPosition.relative(dir);
            if (level.getBlockEntity(p) instanceof BellowsEntity tile && tile.bellows.orientation == dir.getOpposite().get3DDataValue() && !level.hasNeighborSignal(p)) count++;
        }
        return count;
    }
    public static void serverTick(Level level, BlockPos pos, BlockState blockState, AlchemicalFurnaceEntity tile) {
        var host = new AlchemyFurnaceState.Host() {
            public AlchemyFurnaceState.Alembic alembic(int deep) { return level.getBlockEntity(pos.above(deep)) instanceof AlembicEntity a ? a.bound() : null; }
            public AspectList inputTags() { return tags(tile.items.get(0), level); }
            public int fuelBurnTime() { return tile.items.get(1).getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING); }
            public boolean fuelIsAlumentum() { return tile.items.get(1).is(TC4Modern.ALUMENTUM.get()); }
            public void consumeFuel() {
                var fuel = tile.items.get(1);
                if (fuel.isEmpty()) return;
                var remainder = fuel.getCraftingRemainingItem();
                fuel.shrink(1);
                if (fuel.isEmpty()) tile.items.set(1, remainder);
            }
            public void consumeInput() { tile.items.get(0).shrink(1); }
            public int countBellows() { return tile.countBellows(); }
            public java.util.Random random() { return tile.random; }
            public void markForUpdate() { tile.sync(); }
        };
        boolean changed = tile.state.serverTick(host);
        boolean lit = tile.state.isBurning(), filled = tile.state.vis > 0;
        if (blockState.getValue(AlchemicalFurnaceBlock.LIT) != lit || blockState.getValue(AlchemicalFurnaceBlock.FILLED) != filled)
            level.setBlock(pos, blockState.setValue(AlchemicalFurnaceBlock.LIT, lit).setValue(AlchemicalFurnaceBlock.FILLED, filled), 3);
        if (changed) tile.setChanged();
    }
    void sync() { setChanged(); if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3); }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("speedBoost", state.speedBoost);
        tag.putShort("CookTime", (short) state.furnaceCookTime);
        tag.putShort("BurnTime", (short) state.furnaceBurnTime);
        tag.putShort("Vis", (short) state.vis);
        ContainerHelper.saveAllItems(tag, items, registries);
        state.aspects.writeToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        state.speedBoost = tag.getBoolean("speedBoost");
        state.furnaceCookTime = tag.getShort("CookTime");
        state.furnaceBurnTime = tag.getShort("BurnTime");
        state.currentItemBurnTime = items.get(1).isEmpty() ? 0 : items.get(1).getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING);
        state.aspects = new AspectList();
        state.aspects.readFromNBT(tag);
        state.vis = state.aspects.visSize();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() { return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this); }

    @Override public Component getDisplayName() { return Component.translatable("container.tc4modern.alchemical_furnace"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new AlchemicalFurnaceMenu(id, inventory, this, data); }
    @Override public int getContainerSize() { return 2; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) { var r = ContainerHelper.removeItem(items, slot, amount); if (!r.isEmpty()) setChanged(); return r; }
    @Override public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    @Override public void setItem(int slot, ItemStack stack) { items.set(slot, stack); stack.limitSize(getMaxStackSize(stack)); setChanged(); }
    @Override public boolean stillValid(Player player) { return level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getX() + .5, worldPosition.getY() + .5, worldPosition.getZ() + .5) <= 64; }
    @Override public void clearContent() { items.clear(); }
    /** isItemValidForSlot: slot 0 needs aspects, slot 1 needs a furnace fuel. */
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == 0) return tags(stack, level) != null;
        return slot == 1 && stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING) > 0;
    }
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? SLOTS_BOTTOM : side == Direction.UP ? SLOTS_TOP : SLOTS_SIDES; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.UP && canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return side != Direction.DOWN || slot != 1 || stack.is(Items.BUCKET); }
}
