package dev.eugheist.tc4modern;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Entities created by the original warp events; no natural spawn entries. */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class WarpEntities {
    private WarpEntities() {}
    public static final DeferredHolder<EntityType<?>, EntityType<MindSpider>> MIND_SPIDER =
        DeferredHolder.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "mind_spider"));
    public static final DeferredHolder<EntityType<?>, EntityType<EldritchOrb>> ELDRITCH_ORB =
        DeferredHolder.create(Registries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID, "eldritch_orb"));

    public static final DeferredHolder<EntityType<?>,EntityType<EldritchGuardian>> ELDRITCH_GUARDIAN=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("eldritch_guardian"));
    public static final DeferredHolder<net.minecraft.world.item.Item,net.minecraft.world.item.Item> ELDRITCH_EYE=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("eldritch_eye"));
    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.ITEM,h->h.register(ELDRITCH_EYE.getId(),new net.minecraft.world.item.Item(new net.minecraft.world.item.Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)){
            @Override public void appendHoverText(net.minecraft.world.item.ItemStack stack,TooltipContext context,java.util.List<net.minecraft.network.chat.Component> lines,net.minecraft.world.item.TooltipFlag flag){lines.add(net.minecraft.network.chat.Component.translatable("item.ItemEldritchObject.text.1").withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));}
        }));
        event.register(Registries.ENTITY_TYPE, helper -> {
            helper.register(ELDRITCH_GUARDIAN.getId(),EntityType.Builder.of(EldritchGuardian::new,MobCategory.MONSTER).sized(.8f,2.25f).eyeHeight(2.1f).clientTrackingRange(8).build(ELDRITCH_GUARDIAN.getId().toString()));
            helper.register(MIND_SPIDER.getId(), EntityType.Builder.of(MindSpider::new, MobCategory.MONSTER)
                .sized(.3f, .3f).eyeHeight(.7f).build(MIND_SPIDER.getId().toString()));
            helper.register(ELDRITCH_ORB.getId(), EntityType.Builder.<EldritchOrb>of(EldritchOrb::new, MobCategory.MISC)
                .sized(.25f, .25f).clientTrackingRange(4).updateInterval(1)
                .build(ELDRITCH_ORB.getId().toString()));
        });
    }

    @SubscribeEvent
    static void attributes(EntityAttributeCreationEvent event) {
        event.put(ELDRITCH_GUARDIAN.get(),EldritchGuardian.attributes().build());
        event.put(MIND_SPIDER.get(), MindSpider.attributes().build());
    }
}
