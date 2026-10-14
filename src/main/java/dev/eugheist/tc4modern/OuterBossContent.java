package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class OuterBossContent {
 public static final DeferredHolder<EntityType<?>,EntityType<EldritchGolem>> GOLEM=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("eldritch_golem"));
 public static final DeferredHolder<EntityType<?>,EntityType<EldritchWarden>> WARDEN=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("eldritch_warden"));
 public static final DeferredHolder<EntityType<?>,EntityType<CultistPortal>> PORTAL=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("cultist_portal"));
 public static final DeferredHolder<EntityType<?>,EntityType<CultistLeader>> LEADER=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("cultist_leader"));
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ENTITY_TYPE,h->{h.register(PORTAL.getId(),EntityType.Builder.of(CultistPortal::new,MobCategory.MONSTER).sized(1.5f,3).fireImmune().clientTrackingRange(10).build(PORTAL.getId().toString()));h.register(LEADER.getId(),EntityType.Builder.of(CultistLeader::new,MobCategory.MONSTER).sized(.75f,2.25f).clientTrackingRange(10).build(LEADER.getId().toString()));h.register(WARDEN.getId(),EntityType.Builder.of(EldritchWarden::new,MobCategory.MONSTER).sized(1.5f,3.5f).eyeHeight(3.1f).clientTrackingRange(10).build(WARDEN.getId().toString()));h.register(GOLEM.getId(),EntityType.Builder.of(EldritchGolem::new,MobCategory.MONSTER).sized(1.75f,3.5f).eyeHeight(3).fireImmune().clientTrackingRange(10).build(GOLEM.getId().toString()));});}
 @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e){e.put(PORTAL.get(),CultistPortal.attributes().build());e.put(LEADER.get(),CultistLeader.attributes().build());e.put(WARDEN.get(),EldritchWarden.attributes().build());e.put(GOLEM.get(),EldritchGolem.attributes().build());}
}
