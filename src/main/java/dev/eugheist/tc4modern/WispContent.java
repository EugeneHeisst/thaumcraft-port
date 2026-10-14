package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class WispContent {
 public static final DeferredHolder<EntityType<?>,EntityType<Wisp>> WISP=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("wisp"));
 public static final DeferredHolder<SoundEvent,SoundEvent> LIVE=sound("wisplive"),DEAD=sound("wispdead");
 private static DeferredHolder<SoundEvent,SoundEvent> sound(String name){return DeferredHolder.create(Registries.SOUND_EVENT,AlchemyContent.id(name));}
 @SubscribeEvent static void register(RegisterEvent e){
  e.register(Registries.ENTITY_TYPE,h->h.register(WISP.getId(),EntityType.Builder.of(Wisp::new,MobCategory.MONSTER).sized(.9f,.9f).build(WISP.getId().toString())));
  e.register(Registries.SOUND_EVENT,h->{for(var s:java.util.List.of(LIVE,DEAD))h.register(s.getId(),SoundEvent.createVariableRangeEvent(s.getId()));});
  e.register(Registries.ITEM,h->h.register(AlchemyContent.id("wisp_spawn_egg"),new net.neoforged.neoforge.common.DeferredSpawnEggItem(WISP,0xFFC0FF,0xFFFFFF,new Item.Properties())));
 }
 @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e){e.put(WISP.get(),Wisp.attributes().build());}
 @SubscribeEvent static void spawns(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent e){e.register(WISP.get(),SpawnPlacementTypes.NO_RESTRICTIONS,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,r,p,random)->l.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL&&l.getBlockState(p).isAir()&&l.getBrightness(net.minecraft.world.level.LightLayer.SKY,p)<=random.nextInt(32)&&l.getMaxLocalRawBrightness(p)<=random.nextInt(8)&&l.getLevel().getEntitiesOfClass(Wisp.class,new net.minecraft.world.phys.AABB(p).inflate(16)).size()<8,net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);}
}
