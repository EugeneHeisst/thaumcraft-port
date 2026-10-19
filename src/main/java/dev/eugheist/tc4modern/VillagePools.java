package dev.eugheist.tc4modern;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElementType;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.DeferredHolder;
import dev.eugheist.tc4modern.mixin.VillagePoolAccessor;

/** Additive vanilla house-pool integration before world generation workers start. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class VillagePools {
    static final DeferredHolder<StructurePoolElementType<?>,StructurePoolElementType<VillagePoolElement>> TYPE=DeferredHolder.create(Registries.STRUCTURE_POOL_ELEMENT,AlchemyContent.id("village_building"));
    @EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD) static final class Registration {
        @SubscribeEvent static void register(net.neoforged.neoforge.registries.RegisterEvent e){e.register(Registries.STRUCTURE_POOL_ELEMENT,h->h.register(TYPE.getId(),(StructurePoolElementType<VillagePoolElement>)()->VillagePoolElement.CODEC));}
    }
    /** Modern house of each biome whose cartography table becomes the banker's Hungry Chest. */
    static String bankerHouse(String biome){return "minecraft:village/"+biome+"/houses/"+biome+switch(biome){case "plains","savanna"->"_cartographer_1";default->"_cartographer_house_1";};}
    @SubscribeEvent static void beforeStart(net.neoforged.neoforge.event.server.ServerAboutToStartEvent e){
        var registry=e.getServer().registryAccess().registryOrThrow(Registries.TEMPLATE_POOL);
        for(String biome:new String[]{"plains","desert","savanna","snowy","taiga"}){
            var pool=registry.get(ResourceLocation.withDefaultNamespace("village/"+biome+"/houses"));if(pool==null)continue;
            var access=(VillagePoolAccessor)pool;if(access.tc4modern$raw().stream().anyMatch(p->p.getFirst() instanceof VillagePoolElement))continue;
            var list=new java.util.ArrayList<>(access.tc4modern$raw());
            for(boolean wizard:new boolean[]{true,false}){int weight=wizard?15:25;var element=new VillagePoolElement(wizard,wizard?java.util.Optional.empty():java.util.Optional.of(bankerHouse(biome)),StructureTemplatePool.Projection.RIGID);list.add(Pair.of(element,weight));for(int n=0;n<weight;n++)access.tc4modern$templates().add(element);}
            access.tc4modern$raw(list);access.tc4modern$maxSize(Integer.MIN_VALUE);
        }
    }
}
