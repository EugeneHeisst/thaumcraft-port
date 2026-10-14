package thaumcraft.api;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Original Thaumcraft equipment API, with modern Minecraft argument types. */
public interface IWarpingGear {
    int getWarp(ItemStack itemstack, Player player);
}
