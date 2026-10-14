package dev.eugheist.tc4modern;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Original dissolve kills append aspect crystals without replacing normal loot. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class LiquidDeathDrops {
 @SubscribeEvent public static void drops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event){
  var entity=event.getEntity();if(entity.level().isClientSide||!event.getSource().is(LiquidDeath.DAMAGE))return;
  for(var entry:ScanTarget.entity(entity).aspects().entrySet()){
   if(entity.level().random.nextBoolean())continue;
   var aspect=thaumcraft.api.aspects.Aspect.getAspect(entry.getKey());if(aspect==null||entry.getValue()<1)continue;
   int amount=Math.max(1,(1+entity.level().random.nextInt(entry.getValue()))/2);
   event.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(entity.level(),entity.getX(),entity.getY()+entity.getEyeHeight(),entity.getZ(),LiquidDeath.crystal(aspect,amount)));
  }
 }
}
