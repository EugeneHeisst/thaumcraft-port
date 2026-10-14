package baubles.api;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;

/** Baubles 1.0.1.10 API entry point: the player's four-slot bauble inventory. */
public final class BaublesApi {
    private BaublesApi() {}
    public static Container getBaubles(Player player) {
        return dev.eugheist.tc4modern.BaublesInventory.of(player);
    }
}
