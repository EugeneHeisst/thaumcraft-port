package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetCustomDataFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Original Config.initLoot additions share the vanilla weighted pool and its existing roll count. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class BasicsLoot {
 static void add(List<LootPoolEntryContainer> entries,ItemStack s,int min,int max,int weight){var builder=LootItem.lootTableItem(s.getItem()).setWeight(weight).apply(SetItemCountFunction.setCount(UniformGenerator.between(min,max)));var data=s.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);if(data!=null)builder.apply(SetCustomDataFunction.setCustomData(data.copyTag()));entries.add(builder.build());}
 @SubscribeEvent public static void load(net.neoforged.neoforge.event.LootTableLoadEvent event){var id=event.getName();if(!id.getNamespace().equals("minecraft"))return;
 boolean nearby=switch(id.getPath()){case "chests/simple_dungeon","chests/jungle_temple","chests/desert_pyramid"->true;default->false;};
 boolean other=switch(id.getPath()){case "chests/abandoned_mineshaft","chests/stronghold_corridor","chests/stronghold_crossing","chests/stronghold_library"->true;default->false;};
 boolean blacksmith=id.getPath().equals("chests/village/village_weaponsmith");if(!nearby&&!other&&!blacksmith)return;
 for(var pool:event.getTable().pools){if(pool.entries.isEmpty())continue;var entries=new ArrayList<>(pool.entries);
 if(blacksmith)add(entries,VillageTrades.mod("thaumium_ingot",1),1,3,10);
 else{
  for(String name:List.of("loot_bag_common","thaumium_ingot","amber"))add(entries,VillageTrades.mod(name,1),1,3,nearby?5:4);
  for(String name:List.of("loot_bag_uncommon","mundane_amulet","mundane_ring","mundane_belt","knowledge_fragment"))add(entries,VillageTrades.mod(name,1),1,2,nearby?4:3);
  if(id.getPath().equals("chests/stronghold_library"))add(entries,VillageTrades.mod("knowledge_fragment",1),3,6,20);
  for(String name:List.of("loot_bag_rare","thaumonomicon","thaumium_sword","thaumium_pickaxe","thaumium_axe","thaumium_hoe","runic_ring_lesser"))add(entries,VillageTrades.mod(name,1),1,1,1);
  for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())add(entries,VillageTrades.mod("primal_ring_"+a.getTag(),1),1,1,1);
  add(entries,LootBags.chestAmulet(),1,1,1);
 }
 pool.entries=List.copyOf(entries);break;}
 }
}
