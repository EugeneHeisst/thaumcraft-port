package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.neoforged.neoforge.registries.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class TaintedContent {
 public static final java.util.Map<String,DeferredHolder<EntityType<?>,EntityType<TaintedMob>>> TYPES=new java.util.LinkedHashMap<>();
 public static final DeferredHolder<EntityType<?>,EntityType<AspectOrb>> ORB=DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("aspect_orb"));
 static {for(String name:java.util.List.of("chicken","cow","creeper","pig","sheep","villager"))TYPES.put(name,DeferredHolder.create(Registries.ENTITY_TYPE,AlchemyContent.id("taint_"+name)));}
 @SubscribeEvent static void register(RegisterEvent e){
  e.register(Registries.ENTITY_TYPE,h->{for(var entry:TYPES.entrySet()){float[] size=switch(entry.getKey()){case "chicken"->new float[]{.5f,.8f};case "cow","sheep"->new float[]{.9f,1.3f};case "pig"->new float[]{.9f,.9f};default->new float[]{.6f,1.8f};};h.register(entry.getValue().getId(),EntityType.Builder.of(TaintedMob::new,MobCategory.MONSTER).sized(size[0],size[1]).build(entry.getValue().getId().toString()));}h.register(ORB.getId(),EntityType.Builder.<AspectOrb>of(AspectOrb::new,MobCategory.MISC).sized(.125f,.125f).clientTrackingRange(6).updateInterval(3).build(ORB.getId().toString()));});
  e.register(Registries.ITEM,h->{for(var t:TYPES.values())h.register(AlchemyContent.id(t.getId().getPath()+"_spawn_egg"),new net.neoforged.neoforge.common.DeferredSpawnEggItem(t,0xFFC0FF,switch(t.getId().getPath()){case "taint_chicken"->0xC0C0C0;case "taint_cow"->8272443;case "taint_creeper"->65280;case "taint_pig"->0xEF99EF;case "taint_sheep"->0x808080;default->65535;},new Item.Properties()));});
 }
 @SubscribeEvent static void attributes(net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent e){for(var entry:TYPES.entrySet())e.put(entry.getValue().get(),TaintedMob.attributes(entry.getKey()).build());}
 @EventBusSubscriber(modid=TC4Modern.MODID) public static final class Death {
  @SubscribeEvent static void death(net.neoforged.neoforge.event.entity.living.LivingDeathEvent e){
   var original=e.getEntity();if(original.level().isClientSide)return;
   if(!(original instanceof thaumcraft.api.entities.ITaintedMob)&&original.hasEffect(TC4Modern.FLUX_TAINT)){
    String species=original instanceof net.minecraft.world.entity.monster.Creeper?"creeper":original instanceof net.minecraft.world.entity.animal.Sheep?"sheep":original instanceof net.minecraft.world.entity.animal.Cow?"cow":original instanceof net.minecraft.world.entity.animal.Pig?"pig":original instanceof net.minecraft.world.entity.animal.Chicken?"chicken":original instanceof net.minecraft.world.entity.npc.Villager?"villager":null;
    Mob replacement;if(species!=null)replacement=TYPES.get(species).get().create(original.level());else {var slime=new thaumcraft.common.entities.monster.EntityThaumicSlime(TC4Modern.THAUMIC_SLIME.get(),original.level());slime.setSlimeSize((int)(1+Math.min(original.getMaxHealth()/10,6)));replacement=slime;}
    replacement.moveTo(original.getX(),original.getY(),original.getZ(),original.getYRot(),0);original.level().addFreshEntity(replacement);original.discard();e.setCanceled(true);return;
   }
   if(original.lastHurtByPlayerTime>0){var scanned=new thaumcraft.api.aspects.AspectList();ScanTarget.entity(original).aspects().forEach((tag,amount)->scanned.add(thaumcraft.api.aspects.Aspect.getAspect(tag),amount));var aspects=thaumcraft.common.lib.research.ResearchManager.reduceToPrimals(scanned);for(var a:aspects.getAspects())if(original.getRandom().nextBoolean())original.level().addFreshEntity(new AspectOrb(original.level(),original.getX(),original.getY(),original.getZ(),a,1+original.getRandom().nextInt(aspects.getAmount(a))));}
  }
 }
}
