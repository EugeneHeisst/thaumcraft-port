package dev.eugheist.tc4modern;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Baubles ItemRing metadata 0, the Miner's Ring: permanent Haste I while worn; right-click puts it on. */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class MinersRingItem extends Item implements IBauble {
    public static final DeferredHolder<Item, Item> RING = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("miners_ring"));
    @SubscribeEvent static void register(RegisterEvent event) {
        event.register(Registries.ITEM, h -> h.register(RING.getId(), new MinersRingItem()));
    }

    public MinersRingItem() { super(new Properties().stacksTo(1).rarity(Rarity.RARE)); }

    @Override public boolean isFoil(ItemStack stack) { return true; }
    @Override public BaubleType getBaubleType(ItemStack stack) { return BaubleType.RING; }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var held = player.getItemInHand(hand);
        if (!level.isClientSide) BaublesInventory.equipFromHand(player, held);
        return InteractionResultHolder.sidedSuccess(held, level.isClientSide);
    }

    @Override public void onWornTick(ItemStack stack, LivingEntity player) {
        if (!player.hasEffect(MobEffects.DIG_SPEED)) player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 0, true, true));
    }

    @Override public void onEquipped(ItemStack stack, LivingEntity player) {
        if (!player.level().isClientSide)
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, .1f, 1.3f);
    }
}
