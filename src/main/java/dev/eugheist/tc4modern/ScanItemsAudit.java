package dev.eugheist.tc4modern;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Prints the aspects the thaumometer would read for selected items (-Dtc4modern.scanItemsAuditOnly=id,id,...). */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class ScanItemsAudit {
 static int ticks;
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){String ids=System.getProperty("tc4modern.scanItemsAuditOnly");if(ids==null)return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;if(++ticks!=40)return;
  var server=mc.getSingleplayerServer();server.execute(()->{for(var id:ids.split(",")){var item=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(id));
   var stack=new net.minecraft.world.item.ItemStack(item);System.out.println("TC4_SCAN "+id+" table="+AspectTable.get(stack)+" recipe="+RecipeAspects.get(stack,server.overworld()));}
   mc.execute(mc::stop);});}
}
