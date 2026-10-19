package dev.eugheist.tc4modern;

import com.google.common.collect.ImmutableSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * Wizard and banker are modern villager professions (issue #4): the Crucible and the Hungry Chest are their job sites,
 * so ordinary unemployed villagers take the jobs, level up and unlock trades like vanilla professions.
 * The legacy wizard/banker entity types stay registered only so old saves convert (see ThaumcraftVillager).
 */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class VillageContent {
    public static final DeferredHolder<EntityType<?>,EntityType<ThaumcraftVillager>> WIZARD=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("wizard")),BANKER=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("banker"));
    public static final DeferredHolder<VillagerProfession,VillagerProfession> WIZARD_PROFESSION=DeferredHolder.create(Registries.VILLAGER_PROFESSION,AlchemyContent.id("wizard")),BANKER_PROFESSION=DeferredHolder.create(Registries.VILLAGER_PROFESSION,AlchemyContent.id("banker"));
    public static final ResourceKey<PoiType> WIZARD_POI=ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,AlchemyContent.id("wizard")),BANKER_POI=ResourceKey.create(Registries.POINT_OF_INTEREST_TYPE,AlchemyContent.id("banker"));
    static Block jobSite(boolean wizard){return wizard?TC4Modern.CRUCIBLE.get():HungryChest.BLOCK.get();}
    @SubscribeEvent static void register(RegisterEvent e){
        e.register(Registries.POINT_OF_INTEREST_TYPE,h->{for(boolean wizard:new boolean[]{true,false})h.register((wizard?WIZARD_POI:BANKER_POI).location(),new PoiType(ImmutableSet.copyOf(jobSite(wizard).getStateDefinition().getPossibleStates()),1,1));});
        e.register(Registries.VILLAGER_PROFESSION,h->{for(boolean wizard:new boolean[]{true,false}){var poi=wizard?WIZARD_POI:BANKER_POI;
            h.register(AlchemyContent.id(wizard?"wizard":"banker"),new VillagerProfession(wizard?"wizard":"banker",p->p.is(poi),p->p.is(poi),ImmutableSet.of(),ImmutableSet.of(),wizard?net.minecraft.sounds.SoundEvents.VILLAGER_WORK_CLERIC:net.minecraft.sounds.SoundEvents.VILLAGER_WORK_CARTOGRAPHER));}});
        e.register(Registries.ENTITY_TYPE,h->{for(var type:java.util.List.of(WIZARD,BANKER))h.register(type.getId(),EntityType.Builder.of(ThaumcraftVillager::new,MobCategory.MISC).sized(.6f,1.95f).eyeHeight(1.62f).clientTrackingRange(10).build(type.getId().toString()));});
    }
    @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e){e.put(WIZARD.get(),Villager.createAttributes().build());e.put(BANKER.get(),Villager.createAttributes().build());}
    /** An ordinary vanilla villager already holding a Thaumcraft profession. */
    static Villager villager(net.minecraft.world.level.Level level,boolean wizard){
        var villager=EntityType.VILLAGER.create(level);if(villager!=null)villager.setVillagerData(villager.getVillagerData().setProfession((wizard?WIZARD_PROFESSION:BANKER_PROFESSION).get()));
        return villager;
    }
}
