package dev.eugheist.tc4modern;

import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import thaumcraft.common.config.ConfigOreAspects;

/** Maps original OreDictionary categories to current Minecraft/NeoForge item tags. */
final class OreTagBridge {
    private OreTagBridge() {}
    static ResourceLocation modern(String legacy){
        String fixed=switch(legacy){
            case "stone" -> "c:stones";
            case "cobblestone" -> "c:cobblestones";
            case "logWood" -> "minecraft:logs";
            case "plankWood" -> "minecraft:planks";
            case "slabWood" -> "minecraft:wooden_slabs";
            case "stairWood" -> "minecraft:wooden_stairs";
            case "stickWood" -> "c:rods/wooden";
            case "treeSapling" -> "minecraft:saplings";
            case "treeLeaves" -> "minecraft:leaves";
            case "glowstone" -> "c:storage_blocks/glowstone";
            default -> null;
        };
        if(fixed!=null)return ResourceLocation.parse(fixed);
        for(var prefix:Map.of("ore","ores","gem","gems","nugget","nuggets","ingot","ingots","dust","dusts","dye","dyes").entrySet()){
            if(legacy.startsWith(prefix.getKey())){
                String name=legacy.substring(prefix.getKey().length()).replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase(java.util.Locale.ROOT);
                return ResourceLocation.parse("c:"+prefix.getValue()+"/"+name);
            }
        }
        throw new IllegalArgumentException("Unknown original ore category: "+legacy);
    }
    private static TagKey<Item> tag(ResourceLocation id){return TagKey.create(Registries.ITEM,id);}
    /** The original golem entropy upgrade uses OreDictionary equivalence, not broad material families. */
    static boolean equivalent(ItemStack a,ItemStack b){
        if(a.isEmpty()||b.isEmpty())return false;
        for(var record:ConfigOreAspects.ENTRIES){var category=tag(modern(record.legacyTag()));if(a.is(category)&&b.is(category))return true;}
        return false;
    }
    static Map<String,Integer> get(ItemStack stack){
        Map<String,Integer> result=Map.of();
        for(var record:ConfigOreAspects.ENTRIES){
            if(!record.requiredIngot().isEmpty() && !BuiltInRegistries.ITEM.getTag(tag(ResourceLocation.parse("c:ingots/"+record.requiredIngot()))).map(set->set.size()>0).orElse(false))continue;
            if(!stack.is(tag(modern(record.legacyTag()))))continue;
            result=new LinkedHashMap<>();for(var aspect:record.aspects().getAspects())result.put(aspect.getTag(),record.aspects().getAmount(aspect));
        }
        return result;
    }
}
