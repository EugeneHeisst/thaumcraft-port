package dev.eugheist.tc4modern;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.IWarpingGear;

/** Original main-hand, armor and four Baubles slots; accessory adapter supplies its slots. */
public final class WarpGear {
    private WarpGear() {}
    public static int contribution(ItemStack stack, Player player) {
        return !stack.isEmpty() && stack.getItem() instanceof IWarpingGear gear
            ? gear.getWarp(stack, player) : 0;
    }
    public static int total(Player player, Iterable<ItemStack> accessorySlots) {
        int total = contribution(player.getMainHandItem(), player);
        for (ItemStack stack : player.getArmorSlots()) total += contribution(stack, player);
        for (ItemStack stack : accessorySlots) total += contribution(stack, player);
        return total;
    }
}
