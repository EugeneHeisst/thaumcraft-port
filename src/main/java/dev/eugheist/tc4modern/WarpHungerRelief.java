package dev.eugheist.tc4modern;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/** Original finishedUsingItem relief and food rejection messages. */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class WarpHungerRelief {
    private WarpHungerRelief() {}
    @SubscribeEvent
    public static void finished(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        MobEffectInstance effect = player.getEffect(WarpEffects.UNNATURAL_HUNGER);
        if (effect == null) return;
        var item = event.getItem();
        if (item.is(Items.ROTTEN_FLESH) || item.is(ZombieBrainContent.ITEM.get())) {
            int duration = effect.getDuration() - 600;
            int amplifier = effect.getAmplifier() - 1;
            player.removeEffect(WarpEffects.UNNATURAL_HUNGER);
            if (duration > 0 && amplifier >= 0) {
                var remaining = new MobEffectInstance(WarpEffects.UNNATURAL_HUNGER, duration, amplifier, true, true);
                remaining.getCures().clear();
                player.addEffect(remaining);
            }
            player.sendSystemMessage(Component.translatable("warp.text.hunger.2")
                .withStyle(ChatFormatting.DARK_GREEN, ChatFormatting.ITALIC));
        } else if (item.has(DataComponents.FOOD)) {
            player.sendSystemMessage(Component.translatable("warp.text.hunger.1")
                .withStyle(ChatFormatting.DARK_RED, ChatFormatting.ITALIC));
        }
    }
}
