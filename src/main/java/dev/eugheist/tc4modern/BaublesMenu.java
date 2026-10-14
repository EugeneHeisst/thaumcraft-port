package dev.eugheist.tc4modern;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Baubles ContainerPlayerExpanded: the 1.7 player inventory (2x2 crafting, armour, 36 slots) with the four bauble
 * slots in the column between the player model and the crafting grid. Slot indices follow the original:
 * 0 result, 1-4 grid, 5-8 armour, 9 amulet, 10-11 rings, 12 belt, 13-39 main inventory, 40-48 hotbar.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class BaublesMenu extends AbstractContainerMenu {
    public static final DeferredHolder<MenuType<?>, MenuType<BaublesMenu>> TYPE =
        DeferredHolder.create(Registries.MENU, AlchemyContent.id("baubles"));
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    @SubscribeEvent static void register(RegisterEvent event) {
        event.register(Registries.MENU, h -> h.register(TYPE.getId(), new MenuType<>(BaublesMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS)));
    }

    private final Player player;
    private final CraftingContainer craftSlots = new TransientCraftingContainer(this, 2, 2);
    private final ResultContainer resultSlots = new ResultContainer();

    public BaublesMenu(int id, Inventory inventory) {
        super(TYPE.get(), id);
        player = inventory.player;
        addSlot(new ResultSlot(player, craftSlots, resultSlots, 0, 144, 36));
        for (int i = 0; i < 2; i++) for (int j = 0; j < 2; j++) addSlot(new Slot(craftSlots, j + i * 2, 106 + j * 18, 26 + i * 18));
        for (int i = 0; i < 4; i++) {
            var equipment = ARMOR[i];
            addSlot(new Slot(inventory, 39 - i, 8, 8 + i * 18) {
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPlace(ItemStack stack) { return stack.canEquip(equipment, player); }
                @Override public boolean mayPickup(Player who) {
                    var stack = getItem();
                    return (stack.isEmpty() || who.isCreative() || !net.minecraft.world.item.enchantment.EnchantmentHelper.has(stack,
                        net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE)) && super.mayPickup(who);
                }
            });
        }
        Container baubles = BaublesInventory.of(player);
        addSlot(new BaubleSlot(baubles, 0, 80, 8));
        addSlot(new BaubleSlot(baubles, 1, 80, 26));
        addSlot(new BaubleSlot(baubles, 2, 80, 44));
        addSlot(new BaubleSlot(baubles, 3, 80, 62));
        for (int i = 0; i < 3; i++) for (int j = 0; j < 9; j++) addSlot(new Slot(inventory, j + (i + 1) * 9, 8 + j * 18, 84 + i * 18));
        for (int i = 0; i < 9; i++) addSlot(new Slot(inventory, i, 8 + i * 18, 142));
    }

    /** SlotBauble: one fitting bauble, removable only when canUnequip allows. */
    public final class BaubleSlot extends Slot {
        BaubleSlot(Container baubles, int index, int x, int y) { super(baubles, index, x, y); }
        @Override public boolean mayPlace(ItemStack stack) { return BaublesInventory.fits(getContainerSlot(), stack, player); }
        @Override public boolean mayPickup(Player who) {
            var stack = getItem();
            return !stack.isEmpty() && stack.getItem() instanceof IBauble bauble && bauble.canUnequip(stack, who);
        }
        @Override public int getMaxStackSize() { return 1; }
    }

    @Override public void slotsChanged(Container container) {
        if (!(player instanceof ServerPlayer server)) return;
        var input = craftSlots.asCraftInput();
        var result = ItemStack.EMPTY;
        var recipe = server.serverLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, server.serverLevel());
        if (recipe.isPresent() && resultSlots.setRecipeUsed(server.serverLevel(), server, recipe.get())) {
            var crafted = recipe.get().value().assemble(input, server.serverLevel().registryAccess());
            if (crafted.isItemEnabled(server.serverLevel().enabledFeatures())) result = crafted;
        }
        resultSlots.setItem(0, result);
        setRemoteSlot(0, result);
        server.connection.send(new ClientboundContainerSetSlotPacket(containerId, incrementStateId(), 0, result));
    }

    @Override public void removed(Player who) {
        super.removed(who);
        resultSlots.clearContent();
        if (!who.level().isClientSide) clearContainer(who, craftSlots);
    }

    @Override public boolean stillValid(Player who) { return true; }

    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != resultSlots && super.canTakeItemForPickAll(stack, slot);
    }

    /** ContainerPlayerExpanded.transferStackInSlot. */
    @Override public ItemStack quickMoveStack(Player who, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var copy = stack.copy();
        if (index == 0) {
            stack.getItem().onCraftedBy(stack, who.level(), who);
            if (!moveItemStackTo(stack, 13, 49, true)) return ItemStack.EMPTY;
            slot.onQuickCraft(stack, copy);
        } else if (index >= 1 && index < 13) {
            if (!moveItemStackTo(stack, 13, 49, false)) return ItemStack.EMPTY;
        } else if (armorTarget(copy) >= 0 && !slots.get(armorTarget(copy)).hasItem()) {
            int target = armorTarget(copy);
            if (!moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        } else if (baubleTarget(copy, index) >= 0) {
            int target = baubleTarget(copy, index);
            if (!moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        } else if (index >= 13 && index < 40) {
            if (!moveItemStackTo(stack, 40, 49, false)) return ItemStack.EMPTY;
        } else if (index >= 40 && index < 49) {
            if (!moveItemStackTo(stack, 13, 40, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 13, 49, false)) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY, copy);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(who, stack);
        if (index == 0) who.drop(stack, false);
        return copy;
    }

    private int armorTarget(ItemStack stack) {
        var equipment = player.getEquipmentSlotForItem(stack);
        for (int i = 0; i < ARMOR.length; i++) if (ARMOR[i] == equipment && stack.canEquip(equipment, player)) return 5 + i;
        return -1;
    }

    private int baubleTarget(ItemStack stack, int from) {
        if (!(stack.getItem() instanceof IBauble bauble) || !bauble.canEquip(stack, player)) return -1;
        var type = bauble.getBaubleType(stack);
        if (type == BaubleType.TRINKET) { for (int i = 9; i <= 12; i++) if (!slots.get(i).hasItem()) return i; return -1; }
        if (type == BaubleType.AMULET && !slots.get(9).hasItem()) return 9;
        if (from > 11 && type == BaubleType.RING && !slots.get(10).hasItem()) return 10;
        if (from > 11 && type == BaubleType.RING && !slots.get(11).hasItem()) return 11;
        if (type == BaubleType.BELT && !slots.get(12).hasItem()) return 12;
        return -1;
    }
}
