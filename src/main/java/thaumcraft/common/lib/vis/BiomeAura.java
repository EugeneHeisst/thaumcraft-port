package thaumcraft.common.lib.vis;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;
import thaumcraft.api.aspects.Aspect;
/** Original Config biome registrations and BiomeHandler averaging/tag selection. */
public final class BiomeAura {
    public record Entry(int aura,Aspect aspect){}
    private static final Map<String,Entry> DATA=new LinkedHashMap<>();
    static {
        DATA.put("WATER",new Entry(100,Aspect.WATER));
        DATA.put("OCEAN",new Entry(120,Aspect.WATER));
        DATA.put("RIVER",new Entry(100,Aspect.WATER));
        DATA.put("WET",new Entry(80,Aspect.WATER));
        DATA.put("HOT",new Entry(100,Aspect.FIRE));
        DATA.put("DESERT",new Entry(100,Aspect.FIRE));
        DATA.put("NETHER",new Entry(120,Aspect.FIRE));
        DATA.put("MESA",new Entry(80,Aspect.FIRE));
        DATA.put("DENSE",new Entry(100,Aspect.ORDER));
        DATA.put("SNOWY",new Entry(80,Aspect.ORDER));
        DATA.put("COLD",new Entry(80,Aspect.ORDER));
        DATA.put("FROZEN",new Entry(100,Aspect.ORDER));
        DATA.put("MUSHROOM",new Entry(140,Aspect.ORDER));
        DATA.put("CONIFEROUS",new Entry(100,Aspect.EARTH));
        DATA.put("FOREST",new Entry(120,Aspect.EARTH));
        DATA.put("SANDY",new Entry(80,Aspect.EARTH));
        DATA.put("BEACH",new Entry(80,Aspect.EARTH));
        DATA.put("SAVANNA",new Entry(80,Aspect.AIR));
        DATA.put("MOUNTAIN",new Entry(100,Aspect.AIR));
        DATA.put("HILLS",new Entry(120,Aspect.AIR));
        DATA.put("PLAINS",new Entry(80,Aspect.AIR));
        DATA.put("DRY",new Entry(80,Aspect.ENTROPY));
        DATA.put("SPARSE",new Entry(80,Aspect.ENTROPY));
        DATA.put("SWAMP",new Entry(120,Aspect.ENTROPY));
        DATA.put("WASTELAND",new Entry(80,Aspect.ENTROPY));
        DATA.put("JUNGLE",new Entry(100,Aspect.PLANT));
        DATA.put("LUSH",new Entry(100,Aspect.PLANT));
        DATA.put("MAGICAL",new Entry(100,null));
        DATA.put("END",new Entry(80,Aspect.VOID));
        DATA.put("SPOOKY",new Entry(80,Aspect.SOUL));
        DATA.put("DEAD",new Entry(50,Aspect.DEATH));
    }
    public static int aura(List<String> types){
        try{int average=0,count=0;for(String type:types){average+=DATA.get(type).aura();count++;}return average/count;}catch(Exception e){return 100;}
    }
    public static Aspect tag(List<String> types,Random random){
        try{return DATA.get(types.get(random.nextInt(types.size()))).aspect();}catch(Exception e){return null;}
    }
}
