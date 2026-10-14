package dev.eugheist.tc4modern;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
/** A live view of the nine ingredients; the wand remains outside recipe input. */
public final class ArcaneCraftingGrid implements CraftingContainer {
    private final SimpleContainer inventory;
    public ArcaneCraftingGrid(SimpleContainer inventory){this.inventory=inventory;}
    @Override public int getWidth(){return 3;}
    @Override public int getHeight(){return 3;}
    @Override public int getContainerSize(){return 9;}
    @Override public java.util.List<ItemStack> getItems(){return java.util.List.copyOf(inventory.getItems().subList(0,9));}
    @Override public ItemStack getItem(int slot){return inventory.getItem(slot);}
    @Override public ItemStack removeItem(int slot,int amount){return inventory.removeItem(slot,amount);}
    @Override public ItemStack removeItemNoUpdate(int slot){return inventory.removeItemNoUpdate(slot);}
    @Override public void setItem(int slot,ItemStack stack){inventory.setItem(slot,stack);}
    @Override public void setChanged(){inventory.setChanged();}
    @Override public boolean stillValid(net.minecraft.world.entity.player.Player player){return inventory.stillValid(player);}
    @Override public boolean isEmpty(){return getItems().stream().allMatch(ItemStack::isEmpty);}
    @Override public void clearContent(){for(int i=0;i<9;i++)inventory.setItem(i,ItemStack.EMPTY);}
    @Override public void fillStackedContents(net.minecraft.world.entity.player.StackedContents contents){getItems().forEach(contents::accountSimpleStack);}
}
