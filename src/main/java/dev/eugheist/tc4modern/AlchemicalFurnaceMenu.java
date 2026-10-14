package dev.eugheist.tc4modern;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** ContainerAlchemyFurnace: aspect-limited input at 80,8, fuel at 80,48, original progress fields. */
public final class AlchemicalFurnaceMenu extends AbstractContainerMenu {
    private final Container furnace;
    public final ContainerData data;
    public AlchemicalFurnaceMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(2), new SimpleContainerData(5)); }
    public AlchemicalFurnaceMenu(int id, Inventory inventory, Container furnace, ContainerData data) {
        super(EssentiaDevices.FURNACE_MENU.get(), id);
        this.furnace = furnace; this.data = data;
        // SlotLimitedHasAspects
        addSlot(new Slot(furnace, 0, 80, 8) { @Override public boolean mayPlace(ItemStack s) { return AlchemicalFurnaceEntity.tags(s, inventory.player.level()) != null; } });
        addSlot(new Slot(furnace, 1, 80, 48));
        for (int i = 0; i < 3; ++i) for (int j = 0; j < 9; ++j) addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; ++i) addSlot(new Slot(inventory, i, 8 + i * 18, 142));
        addDataSlots(data);
    }
    public int cookTime() { return data.get(0); }
    public int burnTime() { return data.get(1); }
    public int itemBurnTime() { int v = data.get(2); return v == 0 ? 200 : v; }
    public int vis() { return data.get(3); }
    public int smeltTime() { int v = data.get(4); return v <= 0 ? 1 : v; }
    @Override public boolean stillValid(Player player) { return furnace.stillValid(player); }
    /** transferStackInSlot: aspect items to slot 0, fuels to slot 1, else between inventory and hotbar. */
    @Override public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index != 1 && index != 0) {
                if (AlchemicalFurnaceEntity.tags(stack, player.level()) != null) { if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY; }
                else if (stack.getBurnTime(net.minecraft.world.item.crafting.RecipeType.SMELTING) > 0) { if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY; }
                else if (index >= 2 && index < 29) { if (!moveItemStackTo(stack, 29, 38, false)) return ItemStack.EMPTY; }
                else if (index >= 29 && index < 38 && !moveItemStackTo(stack, 2, 29, false)) return ItemStack.EMPTY;
            } else if (!moveItemStackTo(stack, 2, 38, false)) return ItemStack.EMPTY;
            if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
            if (stack.getCount() == result.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, stack);
        }
        return result;
    }
}
