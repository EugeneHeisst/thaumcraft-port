package dev.eugheist.tc4modern;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Lists every registered item, block and entity type the thaumometer would read no aspects for. */
@EventBusSubscriber(modid=TC4Modern.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
final class AspectCoverageQuickAudit {
 static int ticks;
 // Scanned from their live contents (contained stack, essentia, node reservoir, player tags) rather than a static table.
 static final java.util.Set<String> DYNAMIC=java.util.Set.of("minecraft:item","minecraft:player","tc4modern:special_item","tc4modern:following_item","tc4modern:permanent_item","tc4modern:aspect_orb","tc4modern:essentia_phial","tc4modern:crystal_essence","tc4modern:aura_node");
 @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post e){if(!Boolean.getBoolean("tc4modern.aspectCoverageAuditOnly"))return;var mc=net.minecraft.client.Minecraft.getInstance();if(mc.player==null||mc.level==null)return;if(++ticks!=40)return;
  var server=mc.getSingleplayerServer();server.execute(()->{var l=server.overworld();
   var items=new java.util.ArrayList<String>();int itemTotal=0;
   for(var item:BuiltInRegistries.ITEM){var id=BuiltInRegistries.ITEM.getKey(item);if(item==net.minecraft.world.item.Items.AIR)continue;itemTotal++;
    var m=RecipeAspects.get(new net.minecraft.world.item.ItemStack(item),l);if((m==null||m.isEmpty())&&!DYNAMIC.contains(id.toString()))items.add(id.toString());}
   var blocks=new java.util.ArrayList<String>();int blockTotal=0;
   for(var block:BuiltInRegistries.BLOCK){var id=BuiltInRegistries.BLOCK.getKey(block);if(block.defaultBlockState().isAir())continue;blockTotal++;
    var m=BlockAspectBridge.get(block.defaultBlockState(),l);if((m==null||m.isEmpty())&&!DYNAMIC.contains(id.toString()))blocks.add(id.toString());}
   var entities=new java.util.ArrayList<String>();int entityTotal=0;
   for(var type:BuiltInRegistries.ENTITY_TYPE){var id=BuiltInRegistries.ENTITY_TYPE.getKey(type).toString();entityTotal++;
    var a=ModernAspectCatalog.entity(id);if((a==null||a.size()==0)&&!DYNAMIC.contains(id))entities.add(id);}
   System.out.println("TC4_COVER items without aspects "+items.size()+"/"+itemTotal+": "+items);
   System.out.println("TC4_COVER blocks without aspects "+blocks.size()+"/"+blockTotal+": "+blocks);
   System.out.println("TC4_COVER entities without aspects "+entities.size()+"/"+entityTotal+": "+entities);
   mc.execute(mc::stop);});}
}
