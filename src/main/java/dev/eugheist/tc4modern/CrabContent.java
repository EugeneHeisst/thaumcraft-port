package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class CrabContent {
 public static final DeferredHolder<EntityType<?>,EntityType<EldritchCrab>> CRAB=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("eldritch_crab"));
 public static final DeferredHolder<EntityType<?>,EntityType<InhabitedZombie>> ZOMBIE=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("inhabited_zombie"));
 public static final DeferredHolder<SoundEvent,SoundEvent> TALK=sound("crabtalk"),CLAW=sound("crabclaw"),DEATH=sound("crabdeath");
 private static DeferredHolder<SoundEvent,SoundEvent> sound(String name){return DeferredHolder.create(Registries.SOUND_EVENT,AlchemyContent.id(name));}
 @SubscribeEvent static void register(RegisterEvent e){e.register(Registries.ENTITY_TYPE,h->{h.register(CRAB.getId(),EntityType.Builder.of(EldritchCrab::new,MobCategory.MONSTER).sized(.8f,.6f).build(CRAB.getId().toString()));h.register(ZOMBIE.getId(),EntityType.Builder.of(InhabitedZombie::new,MobCategory.MONSTER).sized(.6f,1.95f).build(ZOMBIE.getId().toString()));});e.register(Registries.SOUND_EVENT,h->{for(var s:java.util.List.of(TALK,CLAW,DEATH))h.register(s.getId(),SoundEvent.createVariableRangeEvent(s.getId()));});e.register(Registries.ITEM,h->{for(var t:java.util.List.of(CRAB,ZOMBIE))h.register(AlchemyContent.id(t.getId().getPath()+"_spawn_egg"),new net.neoforged.neoforge.common.DeferredSpawnEggItem(t,t==CRAB?0x555555:0x557755,0x550000,new Item.Properties()));});}
 @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e){e.put(CRAB.get(),EldritchCrab.attributes().build());e.put(ZOMBIE.get(),InhabitedZombie.attributes().build());}
}
