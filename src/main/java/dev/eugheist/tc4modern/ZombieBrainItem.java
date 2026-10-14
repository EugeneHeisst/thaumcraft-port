package dev.eugheist.tc4modern;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerPlayer;

/** Original ItemZombieBrain applies warp before the normal food completion. */
public final class ZombieBrainItem extends Item {
    public ZombieBrainItem(Properties properties) { super(properties); }
    @Override public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof ServerPlayer player) {
            if (level.random.nextFloat() < .1f) PlayerWarp.add(player, WarpState.Kind.STICKY, 1);
            else PlayerWarp.add(player, WarpState.Kind.TEMPORARY, 1 + level.random.nextInt(3));
        }
        return super.finishUsingItem(stack, level, entity);
    }
}
