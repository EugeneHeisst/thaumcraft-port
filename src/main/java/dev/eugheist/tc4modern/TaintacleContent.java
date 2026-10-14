package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.*;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class TaintacleContent {
 public static final DeferredHolder<EntityType<?>,EntityType<Taintacle>> NORMAL=ref("taintacle"),SMALL=ref("taintacle_small"),GIANT=ref("taintacle_giant");
 public static final DeferredHolder<net.minecraft.sounds.SoundEvent,net.minecraft.sounds.SoundEvent> TENTACLE=DeferredHolder.create(Registries.SOUND_EVENT,AlchemyContent.id("tentacle"));
 public static final DeferredHolder<Item,Item> PEARL=DeferredHolder.create(Registries.ITEM,AlchemyContent.id("primordial_pearl"));
 private static DeferredHolder<EntityType<?>,EntityType<Taintacle>> ref(String id){return DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id(id));}
 @SubscribeEvent static void register(RegisterEvent e){
  e.register(Registries.SOUND_EVENT,h->h.register(TENTACLE.getId(),net.minecraft.sounds.SoundEvent.createVariableRangeEvent(TENTACLE.getId())));
  e.register(Registries.ENTITY_TYPE,h->{h.register(NORMAL.getId(),EntityType.Builder.of(Taintacle::new,MobCategory.MONSTER).sized(.66f,3).build(NORMAL.getId().toString()));h.register(SMALL.getId(),EntityType.Builder.of(Taintacle::new,MobCategory.MONSTER).sized(.22f,1).build(SMALL.getId().toString()));h.register(GIANT.getId(),EntityType.Builder.of(Taintacle::new,MobCategory.MONSTER).sized(1.1f,6).build(GIANT.getId().toString()));});
  e.register(Registries.ITEM,h->{h.register(PEARL.getId(),new PrimordialPearl());for(var t:java.util.List.of(NORMAL,SMALL,GIANT))h.register(AlchemyContent.id(t.getId().getPath()+"_spawn_egg"),new net.neoforged.neoforge.common.DeferredSpawnEggItem(t,0xFFC0FF,t==SMALL?0x800090:t==GIANT?0x808080:0x800080,new Item.Properties()));});
 }
 @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e){e.put(NORMAL.get(),Taintacle.attributes(50,7).build());e.put(SMALL.get(),Taintacle.attributes(8,2).build());e.put(GIANT.get(),Taintacle.attributes(125,9).build());}
 @SubscribeEvent static void spawns(net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent e){e.register(NORMAL.get(),SpawnPlacementTypes.ON_GROUND,net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,(t,l,r,p,random)->{
  var s=l.getBlockState(p);boolean support=s.is(TC4Modern.TAINT_FIBRES.get())&&s.getValue(TaintFibresBlock.VARIANT)==0||s.is(TC4Modern.TAINT.get())&&s.getValue(TaintBlock.VARIANT)==1;
  return support&&l.getBiome(p).is(TaintBiomes.TAINTED_LAND)&&l.getLevel().getEntitiesOfClass(Taintacle.class,new net.minecraft.world.phys.AABB(p).inflate(24,8,24)).isEmpty()&&net.minecraft.world.entity.monster.Monster.checkMonsterSpawnRules(t,l,r,p,random);
 },net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent.Operation.REPLACE);}
}
