package dev.eugheist.tc4modern;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Original EventHandlerEntity zombie brain drop: nextInt(10) - looting < 1. */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class ZombieBrainDrops {
    @SubscribeEvent public static void drops(LivingDropsEvent event) {
        var zombie = event.getEntity();
        if (zombie.level().isClientSide || !(zombie instanceof Zombie) || !event.isRecentlyHit()) return;
        if (zombie instanceof BrainyZombie) return;
        int looting = 0;
        if (event.getSource().getEntity() instanceof LivingEntity killer) {
            var enchantment = zombie.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.LOOTING);
            looting = EnchantmentHelper.getItemEnchantmentLevel(enchantment, killer.getMainHandItem());
        }
        if (zombie.level().random.nextInt(10) - looting < 1)
            event.getDrops().add(new ItemEntity(zombie.level(), zombie.getX(), zombie.getY() + zombie.getEyeHeight(), zombie.getZ(), new ItemStack(ZombieBrainContent.ITEM.get())));
    }
}
