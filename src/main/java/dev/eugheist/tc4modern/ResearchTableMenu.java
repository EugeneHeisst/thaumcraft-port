package dev.eugheist.tc4modern;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Slot positions and restrictions follow original ContainerResearchTable. */
public class ResearchTableMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    public final Container table;
    public final thaumcraft.api.aspects.AspectList bonus;
    public ResearchTableMenu(int id, Inventory inventory) { this(id, inventory, ContainerLevelAccess.NULL, new SimpleContainer(2)); }
    public ResearchTableMenu(int id, Inventory inventory, ContainerLevelAccess access, Container table) {
        this(id, inventory, access, table, new thaumcraft.api.aspects.AspectList());
    }
    public ResearchTableMenu(int id, Inventory inventory, ContainerLevelAccess access, Container table, thaumcraft.api.aspects.AspectList bonus) {
        super(TC4Modern.RESEARCH_MENU.get(), id);
        this.access = access;
        this.table = table;
        this.bonus = bonus;
        for (var aspect : thaumcraft.api.aspects.Aspect.aspects.values().stream().sorted(java.util.Comparator.comparing(thaumcraft.api.aspects.Aspect::getTag)).toList()) {
            addDataSlot(new net.minecraft.world.inventory.DataSlot() {
                @Override public int get() { return bonus.getAmount(aspect); }
                @Override public void set(int value) { if (value > 0) bonus.aspects.put(aspect, value); else bonus.remove(aspect); }
            });
        }
        addSlot(new Slot(table, 0, 14, 10) { @Override public boolean mayPlace(ItemStack stack) { return ScribingToolsItem.isScribe(stack); } });
        addSlot(new Slot(table, 1, 70, 10) { @Override public boolean mayPlace(ItemStack stack) { return stack.is(TC4Modern.RESEARCH_NOTES); } });
        for (int row=0;row<3;row++) for (int col=0;col<9;col++) addSlot(new Slot(inventory, col+row*9+9, 48+col*18,175+row*18));
        for (int col=0;col<9;col++) addSlot(new Slot(inventory, col,48+col*18,233));
    }
    @Override public boolean clickMenuButton(Player player,int button){
        if(button!=5||player.level().isClientSide||!stillValid(player)||!ResearchProgress.completed(player.getPersistentData()).contains("RESEARCHDUPE"))return false;
        var stack=table.getItem(1);var note=NoteData.read(stack);if(note==null||!note.complete||stack.getCount()>=64)return false;
        var research=thaumcraft.api.research.ResearchCategories.getResearch(note.key);if(research==null)return false;
        int paper=-1,ink=-1;for(int i=0;i<player.getInventory().getContainerSize();i++){var item=player.getInventory().getItem(i);if(item.is(net.minecraft.world.item.Items.PAPER))paper=i;if(item.is(net.minecraft.world.item.Items.INK_SAC))ink=i;}
        if(paper<0||ink<0||!ResearchKnowledge.payCopy(player.getPersistentData(),research.tags,note.copies))return false;
        player.getInventory().getItem(paper).shrink(1);player.getInventory().getItem(ink).shrink(1);note.copies++;NoteData.write(stack,note);stack.grow(1);table.setChanged();broadcastChanges();
        if(player instanceof net.minecraft.server.level.ServerPlayer server)ThaumometerItem.sync(server,"");return true;
    }
    @Override public boolean stillValid(Player player) { return stillValid(access, player, TC4Modern.RESEARCH_TABLE.get()); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack=slot.getItem(), original=stack.copy();
        if (!(index < 2 ? moveItemStackTo(stack, 2, slots.size(), true) : moveItemStackTo(stack, 0, 2, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }
}
