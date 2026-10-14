package dev.eugheist.tc4modern;

import java.util.Map;

/** Modern entry point to the locally adapted original ConfigAspects registrations. */
public final class AspectTable {
    private AspectTable() {}
    public static Map<String,Integer> get(net.minecraft.world.item.ItemStack stack){
        var direct=get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
        return direct.isEmpty()?OreTagBridge.get(stack):direct;
    }
    public static Map<String, Integer> get(String objectId) {
        var addon=dev.eugheist.tc4modern.api.TC4Addons.objectTag(objectId);
        if(addon!=null){var map=new java.util.LinkedHashMap<String,Integer>();for(var aspect:addon.getAspects())map.put(aspect.getTag(),addon.getAmount(aspect));return map;}
        if(objectId.equals("tc4modern:taint"))return Map.of("arbor",1,"vitium",3);
        if(objectId.equals("tc4modern:taint_fibres"))return Map.of("victus",1,"vitium",2);
        objectId=switch(objectId){
            case "tc4modern:taint_soil" -> "tc4modern:taint#variant=1";
            case "tc4modern:taint_grass" -> "tc4modern:taint_fibres#variant=1";
            case "tc4modern:taint_plant" -> "tc4modern:taint_fibres#variant=2";
            case "tc4modern:taint_spore" -> "tc4modern:taint_fibres#variant=3";
            default -> objectId;
        };
        if(objectId.equals("tc4modern:obsidian_totem"))return Map.of("terra",4,"tenebrae",2,"alienis",2);
        if(objectId.equals("tc4modern:primal_ring_aer"))return Map.of("praecantatio",5);
        if(objectId.equals("tc4modern:advanced_alchemical_furnace"))return Map.of("praecantatio",8,"aqua",8,"fabrico",8);
        if(objectId.equals("tc4modern:coin"))return Map.of("metallum",1);
        if(objectId.equals("tc4modern:candle_0"))return Map.of("lux",2,"corpus",1,"praecantatio",1);
        if(objectId.equals("tc4modern:primordial_pearl"))return Map.of("aer",16,"terra",16,"ignis",16,"aqua",16,"ordo",16,"perditio",16);
        if(objectId.equals("tc4modern:eldritch_eye"))return Map.of("alienis",5,"auram",3,"praecantatio",3,"sensus",3,"spiritus",3);
        // Original ConfigAspects explicitly tags ItemZombieBrain (not itemResource metadata 3).
        if (objectId.equals("tc4modern:zombie_brain"))
            return Map.of("corpus", 2, "cognitio", 4, "exanimis", 2);
        return thaumcraft.common.config.ConfigAspects.get(objectId);
    }
}
