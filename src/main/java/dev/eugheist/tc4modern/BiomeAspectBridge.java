package dev.eugheist.tc4modern;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
/** Modern common biome tags replacing the removed Forge BiomeDictionary. */
public final class BiomeAspectBridge {
    private static final Map<String,String> TYPES=Map.ofEntries(
        Map.entry("MAGICAL","is_magical"),Map.entry("OCEAN","is_ocean"),Map.entry("RIVER","is_river"),Map.entry("WET","is_wet"),Map.entry("HOT","is_hot"),Map.entry("DESERT","is_desert"),Map.entry("NETHER","is_nether"),Map.entry("MESA","is_badlands"),Map.entry("DENSE","is_dense_vegetation"),Map.entry("SNOWY","is_snowy"),Map.entry("COLD","is_cold"),Map.entry("MUSHROOM","is_mushroom"),Map.entry("CONIFEROUS","is_tree/coniferous"),Map.entry("FOREST","is_forest"),Map.entry("SANDY","is_sandy"),Map.entry("BEACH","is_beach"),Map.entry("SAVANNA","is_savanna"),Map.entry("MOUNTAIN","is_mountain"),Map.entry("HILLS","is_hill"),Map.entry("PLAINS","is_plains"),Map.entry("DRY","is_dry"),Map.entry("SPARSE","is_sparse_vegetation"),Map.entry("SWAMP","is_swamp"),Map.entry("WASTELAND","is_wasteland"),Map.entry("JUNGLE","is_jungle"),Map.entry("LUSH","is_lush"),Map.entry("END","is_end"),Map.entry("SPOOKY","is_spooky"),Map.entry("DEAD","is_dead"));
    public static List<String> types(Holder<Biome> biome){
        List<String> result=new ArrayList<>();
        TYPES.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{if(biome.is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("c",entry.getValue()))))result.add(entry.getKey());});
        if(result.contains("OCEAN") || result.contains("RIVER"))result.add("WATER");
        if(biome.value().getBaseTemperature()<0)result.add("FROZEN");
        return result;
    }
}
