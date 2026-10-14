package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class PechContent {
 public static final DeferredHolder<EntityType<?>,EntityType<PechEntity>> PECH=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("pech"));
 public static final DeferredHolder<EntityType<?>,EntityType<PechBlast>> BLAST=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("pech_blast"));
 public static final DeferredHolder<MenuType<?>,MenuType<PechMenu>> MENU=DeferredHolder.create(Registries.MENU,AlchemyContent.id("pech"));
 public static final DeferredHolder<Item,Item> FOCUS=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("focus_pech"));
 @SubscribeEvent static void register(RegisterEvent event){event.register(Registries.ENTITY_TYPE,h->h.register(PECH.getId(),EntityType.Builder.of(PechEntity::new,MobCategory.MONSTER).sized(.6f,1.8f).eyeHeight(1.8f*.66f).build(PECH.getId().toString())));event.register(Registries.ENTITY_TYPE,h->h.register(BLAST.getId(),EntityType.Builder.<PechBlast>of(PechBlast::new,MobCategory.MISC).sized(.25f,.25f).clientTrackingRange(4).updateInterval(1).build(BLAST.getId().toString())));event.register(Registries.MENU,h->h.register(MENU.getId(),net.neoforged.neoforge.common.extensions.IMenuTypeExtension.create((id,inv,buf)->new PechMenu(id,inv,(PechEntity)inv.player.level().getEntity(buf.readInt())))));event.register(Registries.ITEM,h->{h.register(FOCUS.getId(),new PechFocus());h.register(AlchemyContent.id("pech_spawn_egg"),new net.neoforged.neoforge.common.DeferredSpawnEggItem(PECH,0x70683D,0xC9A783,new Item.Properties()));});}
 @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event){event.put(PECH.get(),PechEntity.attributes().build());}
 @SubscribeEvent static void spawns(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent event){event.register(PECH.get(),SpawnPlacementTypes.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,PechEntity::canSpawn,net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);}
}
