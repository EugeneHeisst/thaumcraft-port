package dev.eugheist.tc4modern;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class VillageContent {
    public static final DeferredHolder<EntityType<?>,EntityType<ThaumcraftVillager>> WIZARD=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("wizard")),BANKER=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("banker"));
    public static final DeferredHolder<VillagerProfession,VillagerProfession> WIZARD_PROFESSION=DeferredHolder.create(Registries.VILLAGER_PROFESSION,AlchemyContent.id("wizard")),BANKER_PROFESSION=DeferredHolder.create(Registries.VILLAGER_PROFESSION,AlchemyContent.id("banker"));
    @SubscribeEvent static void register(RegisterEvent e){
        e.register(Registries.VILLAGER_PROFESSION,h->{for(String name:new String[]{"wizard","banker"})h.register(AlchemyContent.id(name),new VillagerProfession(name,p->false,p->false,ImmutableSet.of(),ImmutableSet.of(),null));});
        e.register(Registries.ENTITY_TYPE,h->{for(var type:java.util.List.of(WIZARD,BANKER))h.register(type.getId(),EntityType.Builder.of(ThaumcraftVillager::new,MobCategory.MISC).sized(.6f,1.95f).eyeHeight(1.62f).clientTrackingRange(10).build(type.getId().toString()));});
    }
    @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e){e.put(WIZARD.get(),Villager.createAttributes().build());e.put(BANKER.get(),Villager.createAttributes().build());}
}
