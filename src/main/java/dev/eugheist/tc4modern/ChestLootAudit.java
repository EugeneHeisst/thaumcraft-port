package dev.eugheist.tc4modern;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.LootParams;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Real loaded vanilla pools and generated loot, including shared original amulet NBT. */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class ChestLootAudit {
 static void check(boolean good,String text){if(!good)throw new IllegalStateException("TC4_CHEST_LOOT FAIL "+text);System.out.println("TC4_CHEST_LOOT PASS "+text);}
 @SubscribeEvent static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e){if(!Boolean.getBoolean("tc4modern.chestLootAudit"))return;var server=e.getServer();try{
 var l=server.overworld();var params=new LootParams.Builder(l).withParameter(LootContextParams.ORIGIN,new net.minecraft.world.phys.Vec3(0,180,0)).create(LootContextParamSets.CHEST);
 for(String name:java.util.List.of("simple_dungeon","jungle_temple","desert_pyramid","abandoned_mineshaft","stronghold_corridor","stronghold_crossing","stronghold_library","village/village_weaponsmith")){
 var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE,ResourceLocation.withDefaultNamespace("chests/"+name));var table=server.reloadableRegistries().getLootTable(key);var pool=table.pools.stream().filter(p->!p.entries.isEmpty()).findFirst().orElseThrow();var counts=new java.util.HashSet<String>();int modEntries=0,modWeight=0;
 for(var entry:pool.entries){var json=net.minecraft.world.level.storage.loot.entries.LootPoolEntries.CODEC.encodeStart(server.registryAccess().createSerializationContext(com.mojang.serialization.JsonOps.INSTANCE),entry).getOrThrow().toString();if(json.contains("tc4modern:")){modEntries++;var obj=com.google.gson.JsonParser.parseString(json).getAsJsonObject();modWeight+=obj.has("weight")?obj.get("weight").getAsInt():1;}}
 check(modEntries==(name.contains("weaponsmith")?1:name.equals("stronghold_library")?23:22),name+" exact original added entry count "+modEntries);
 int expectedWeight=name.contains("weaponsmith")?10:name.equals("stronghold_library")?61:java.util.Set.of("simple_dungeon","jungle_temple","desert_pyramid").contains(name)?49:41;check(modWeight==expectedWeight,name+" original total added weight "+modWeight);
 boolean amulet=false,libraryStack=false;for(int i=0;i<8000;i++)for(var s:table.getRandomItems(params)){var id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem());if(id.getNamespace().equals("tc4modern")){counts.add(id.getPath());if(id.getPath().equals("vis_amulet_lesser")){amulet=true;var tag=NoteData.tag(s);for(var a:thaumcraft.api.aspects.Aspect.getPrimalAspects())checkCharge(tag.getInt(a.getTag()));}if(id.getPath().equals("knowledge_fragment")&&s.getCount()>=3)libraryStack=true;}}
 if(name.contains("weaponsmith"))check(counts.contains("thaumium_ingot"),"actual weaponsmith loot includes original thaumium");else{check(counts.containsAll(java.util.Set.of("loot_bag_common","loot_bag_uncommon","loot_bag_rare","thaumium_ingot","amber","knowledge_fragment","thaumonomicon","runic_ring_lesser"))&&amulet,name+" actual generated common/uncommon/rare items and charged amulet");if(name.equals("stronghold_library"))check(libraryStack,"actual library generates original extra three-to-six fragments");}
 }
 System.out.println("TC4_CHEST_LOOT ALL PASS eight real vanilla tables, original entry counts, actual generated items and charged amulet NBT");
 }finally{server.halt(false);}}
 static void checkCharge(int n){if(n<0||n>400||n%100!=0)throw new IllegalStateException("TC4_CHEST_LOOT FAIL original amulet centivis "+n);}
}
