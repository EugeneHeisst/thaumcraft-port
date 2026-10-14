package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class BrainyContent {
 public static final DeferredHolder<EntityType<?>,EntityType<BrainyZombie>> NORMAL=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("brainy_zombie")),GIANT=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("giant_brainy_zombie"));
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ENTITY_TYPE,h->{for(var t:java.util.List.of(NORMAL,GIANT))h.register(t.getId(),EntityType.Builder.of(BrainyZombie::new,MobCategory.MONSTER).sized(.6f,1.8f).build(t.getId().toString()));});e.register(Registries.ITEM,h->{for(var t:java.util.List.of(NORMAL,GIANT))h.register(AlchemyContent.id(t.getId().getPath()+"_spawn_egg"),new net.neoforged.neoforge.common.DeferredSpawnEggItem(t,0xFFC0FF,t==GIANT?16384:32768,new Item.Properties()));});}
 @SubscribeEvent static void spawns(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent e){for(var t:java.util.List.of(NORMAL,GIANT))e.register(t.get(),SpawnPlacementTypes.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,BrainyZombie::canSpawn,net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);e.register(CrabContent.ZOMBIE.get(),SpawnPlacementTypes.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,BrainyZombie::canSpawn,net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);e.register(WarpEntities.ELDRITCH_GUARDIAN.get(),SpawnPlacementTypes.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,BrainyZombie::canSpawn,net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);}
 @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e){e.put(NORMAL.get(),BrainyZombie.attributes(false).build());e.put(GIANT.get(),BrainyZombie.attributes(true).build());}
}
