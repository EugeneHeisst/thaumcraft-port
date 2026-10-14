package dev.eugheist.tc4modern;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.aspects.Aspect;

/** ItemAmuletVis.onWornTick: every five ticks feed the held wand from the amulet and refill the amulet from nearby vis relays. */
public final class Accessories {
    private Accessories() {}
    static void visAmuletTick(ItemStack amulet, Player player) {
        if (player.level().isClientSide || player.tickCount % 5 != 0) return;
        var data = NoteData.tag(amulet).copy();
        var wand = player.getMainHandItem();
        boolean changed = false;
        for (var aspect : Aspect.getPrimalAspects()) {
            int amount = data.getInt(aspect.getTag());
            if (WandData.isWand(wand)) {
                int transfer = Math.min(amount, Math.min(5, WandData.capacity(wand) - WandData.get(wand, aspect)));
                if (transfer > 0) {
                    var wandTag = NoteData.tag(wand).copy();
                    wandTag.putInt(aspect.getTag(), WandData.get(wand, aspect) + transfer);
                    wand.set(DataComponents.CUSTOM_DATA, CustomData.of(wandTag));
                    amount -= transfer;
                    changed = true;
                }
            }
            int room = (amulet.getItem() instanceof PechRewards.AccessoryItem item ? item.capacity : 2500) - amount;
            if (room > 0) {
                outer:
                for (int x = -5; x <= 5; x++) for (int y = -5; y <= 5; y++) for (int z = -5; z <= 5; z++) {
                    var at = player.blockPosition().offset(x, y, z);
                    if (at.getCenter().distanceToSqr(player.position()) >= 26 || !player.level().hasChunkAt(at)) continue;
                    if (player.level().getBlockEntity(at) instanceof VisRelayEntity relay) {
                        int drawn = relay.consumeVis(aspect, Math.min(5, room));
                        if (drawn > 0) { amount += drawn; changed = true; break outer; }
                    }
                }
            }
            data.putInt(aspect.getTag(), amount);
        }
        if (changed) {
            amulet.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            BaublesInventory.of(player).setChanged();
        }
    }
}
