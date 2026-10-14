package dev.eugheist.tc4modern;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class ZombieBrainContent {
    public static final DeferredHolder<Item, Item> ITEM = DeferredHolder.create(Registries.ITEM, AlchemyContent.id("zombie_brain"));
    @SubscribeEvent public static void register(RegisterEvent event) {
        event.register(Registries.ITEM, helper -> helper.register(AlchemyContent.id("zombie_brain"),
            new ZombieBrainItem(new Item.Properties().food(new FoodProperties.Builder().nutrition(4).saturationModifier(.2f)
                .effect(() -> new MobEffectInstance(MobEffects.HUNGER, 600, 0), .8f).build()))));
    }
}
