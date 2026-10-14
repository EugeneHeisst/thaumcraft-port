package dev.eugheist.tc4modern;

import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Curated modern content supplements the original tables; never replaces an original registration. */
public final class ModernAspectCatalog {
    private static final Map<String, Map<String, Map<String,Integer>>> TABLES = load();
    private ModernAspectCatalog() {}
    private static Map<String, Map<String, Map<String,Integer>>> load() {
        try (var input = ModernAspectCatalog.class.getResourceAsStream("/assets/tc4modern/aspect_catalog.json")) {
            if (input == null) throw new IllegalStateException("Missing modern aspect catalog");
            var root = JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String, Map<String, Map<String,Integer>>> result = new LinkedHashMap<>();
            for (String kind : new String[]{"items", "blocks", "entities"}) {
                Map<String, Map<String,Integer>> entries = new LinkedHashMap<>();
                for (var entry : root.getAsJsonObject(kind).entrySet()) {
                    Map<String,Integer> aspects = new LinkedHashMap<>();
                    for (var value : entry.getValue().getAsJsonObject().entrySet()) {
                        int amount = value.getValue().getAsInt();
                        if (Aspect.getAspect(value.getKey()) == null || amount < 1 || amount > 64)
                            throw new IllegalStateException("Invalid aspect entry: "+entry.getKey()+" / "+value.getKey());
                        aspects.put(value.getKey(), amount);
                    }
                    entries.put(entry.getKey(), Map.copyOf(aspects));
                }
                result.put(kind, Map.copyOf(entries));
            }
            return Map.copyOf(result);
        } catch (java.io.IOException ex) { throw new IllegalStateException("Cannot read modern aspects", ex); }
    }
    public static Map<String,Integer> get(String kind, String id) { return TABLES.get(kind).getOrDefault(id, Map.of()); }
    public static int size(String kind) { return TABLES.get(kind).size(); }
    public static AspectList list(String kind, String id) {
        var list = new AspectList(); get(kind,id).forEach((key,value)->list.add(Aspect.getAspect(key),value)); return list;
    }
    public static AspectList entity(String id) {
        if(id.equals("tc4modern:wizard")||id.equals("tc4modern:banker"))id="minecraft:villager";
        if(id.equals("tc4modern:taintacle"))return new AspectList().add(Aspect.TAINT,3).add(Aspect.WATER,2);
        if(id.equals("tc4modern:taintacle_small"))return new AspectList().add(Aspect.TAINT,1).add(Aspect.WATER,1);
        var original = thaumcraft.common.config.ConfigEntityAspects.get(id);
        return original.size()>0 ? original : list("entities",id);
    }
}
