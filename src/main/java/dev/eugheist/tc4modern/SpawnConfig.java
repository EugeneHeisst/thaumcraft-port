package dev.eugheist.tc4modern;

import java.util.LinkedHashMap;
import java.util.Map;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Original monster switches; biome spawn-list changes take effect after restarting the world. */
final class SpawnConfig {
    private static final Map<String,ModConfigSpec.BooleanValue> VALUES=new LinkedHashMap<>();
    static void define(ModConfigSpec.Builder builder){
        builder.push("Monster_Spawning");
        for(String key:new String[]{"spawn_angry_zombies","spawn_fire_bats","spawn_wisps","spawn_taintacles","spawn_pechs","spawn_eldercreatures"})
            VALUES.put(key,builder.comment("Enable original natural spawning. Existing entities, spawn eggs and scripted encounters are unaffected; restart to rebuild biome spawn lists.").worldRestart().define(key,true));
        VALUES.put("spawn_taint_spores",builder.comment("Allow mature taint fibres and crust to create new spores/swarmers; existing spores are unaffected.").define("spawn_taint_spores",true));
        VALUES.put("champion_mobs",builder.comment("Enable ordinary champion chances. Disabling retains reduced chances in dangerous places and always permits bosses, as in original Thaumcraft.").define("champion_mobs",true));
        builder.pop();
    }
    static boolean enabled(String key){var value=VALUES.get(key);if(value==null)throw new IllegalArgumentException("Unknown Thaumcraft spawn setting: "+key);return !WarpConfig.SPEC.isLoaded()||value.get();}
    static void set(String key,boolean value){VALUES.get(key).set(value);}
    static boolean champions(){return enabled("champion_mobs");}
    static boolean spores(){return enabled("spawn_taint_spores");}
}
