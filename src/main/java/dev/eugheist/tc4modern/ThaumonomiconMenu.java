package dev.eugheist.tc4modern;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** The server keeps a book session so research requests cannot be sent from unrelated screens. */
public class ThaumonomiconMenu extends AbstractContainerMenu {
    public ThaumonomiconMenu(int id,Inventory inventory,Player player) { this(id,inventory); }
    public ThaumonomiconMenu(int id,Inventory inventory) { super(TC4Modern.BOOK_MENU.get(),id); }
    @Override public boolean stillValid(Player player) { return player.getInventory().contains(new ItemStack(TC4Modern.THAUMONOMICON.get())) || player.getOffhandItem().is(TC4Modern.THAUMONOMICON); }
    @Override public ItemStack quickMoveStack(Player player,int slot) { return ItemStack.EMPTY; }
}
