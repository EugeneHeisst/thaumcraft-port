package thaumcraft.common.config;
import java.util.Map;
import java.util.LinkedHashMap;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
/** Original vanilla entity registrations with modern registry names. */
public final class ConfigEntityAspects {
    private static final Map<String,AspectList> TAGS=new LinkedHashMap<>();
    static {
        TAGS.put("minecraft:zombie",new AspectList().add(Aspect.UNDEAD,2).add(Aspect.MAN,1).add(Aspect.EARTH,1));
        TAGS.put("minecraft:giant",new AspectList().add(Aspect.UNDEAD,4).add(Aspect.MAN,3).add(Aspect.EARTH,3));
        TAGS.put("minecraft:skeleton",new AspectList().add(Aspect.UNDEAD,3).add(Aspect.MAN,1).add(Aspect.EARTH,1));
        TAGS.put("minecraft:wither_skeleton",new AspectList().add(Aspect.UNDEAD,4).add(Aspect.MAN,1).add(Aspect.FIRE,2));
        TAGS.put("minecraft:creeper",new AspectList().add(Aspect.PLANT,2).add(Aspect.FIRE,2));
        TAGS.put("minecraft:charged_creeper",new AspectList().add(Aspect.PLANT,3).add(Aspect.FIRE,3).add(Aspect.ENERGY,3));
        TAGS.put("minecraft:horse",new AspectList().add(Aspect.BEAST,4).add(Aspect.EARTH,1).add(Aspect.AIR,1));
        TAGS.put("minecraft:pig",new AspectList().add(Aspect.BEAST,2).add(Aspect.EARTH,2));
        TAGS.put("minecraft:experience_orb",new AspectList().add(Aspect.MIND,5));
        TAGS.put("minecraft:sheep",new AspectList().add(Aspect.BEAST,2).add(Aspect.EARTH,2));
        TAGS.put("minecraft:cow",new AspectList().add(Aspect.BEAST,3).add(Aspect.EARTH,3));
        TAGS.put("minecraft:mooshroom",new AspectList().add(Aspect.BEAST,3).add(Aspect.PLANT,1).add(Aspect.EARTH,2));
        TAGS.put("minecraft:snow_golem",new AspectList().add(Aspect.COLD,3).add(Aspect.WATER,1));
        TAGS.put("minecraft:ocelot",new AspectList().add(Aspect.BEAST,3).add(Aspect.ENTROPY,3));
        TAGS.put("minecraft:chicken",new AspectList().add(Aspect.BEAST,2).add(Aspect.FLIGHT,2).add(Aspect.AIR,1));
        TAGS.put("minecraft:squid",new AspectList().add(Aspect.BEAST,2).add(Aspect.WATER,2));
        TAGS.put("minecraft:wolf",new AspectList().add(Aspect.BEAST,3).add(Aspect.EARTH,3));
        TAGS.put("minecraft:bat",new AspectList().add(Aspect.BEAST,1).add(Aspect.FLIGHT,1).add(Aspect.AIR,1));
        TAGS.put("minecraft:boat",new AspectList().add(Aspect.MECHANISM,2).add(Aspect.WATER,2));
        TAGS.put("minecraft:spider",new AspectList().add(Aspect.BEAST,3).add(Aspect.ENTROPY,2));
        TAGS.put("minecraft:slime",new AspectList().add(Aspect.SLIME,2).add(Aspect.WATER,2));
        TAGS.put("minecraft:ghast",new AspectList().add(Aspect.UNDEAD,3).add(Aspect.FIRE,2));
        TAGS.put("minecraft:zombified_piglin",new AspectList().add(Aspect.UNDEAD,4).add(Aspect.FIRE,2));
        TAGS.put("minecraft:enderman",new AspectList().add(Aspect.ELDRITCH,4).add(Aspect.TRAVEL,2).add(Aspect.AIR,2));
        TAGS.put("minecraft:cave_spider",new AspectList().add(Aspect.BEAST,2).add(Aspect.POISON,2).add(Aspect.EARTH,1));
        TAGS.put("minecraft:silverfish",new AspectList().add(Aspect.BEAST,1).add(Aspect.EARTH,1));
        TAGS.put("minecraft:blaze",new AspectList().add(Aspect.ELDRITCH,4).add(Aspect.FIRE,1));
        TAGS.put("minecraft:magma_cube",new AspectList().add(Aspect.SLIME,3).add(Aspect.FIRE,2));
        TAGS.put("minecraft:ender_dragon",new AspectList().add(Aspect.ELDRITCH,20).add(Aspect.BEAST,20).add(Aspect.ENTROPY,20));
        TAGS.put("minecraft:wither",new AspectList().add(Aspect.UNDEAD,20).add(Aspect.ENTROPY,20).add(Aspect.FIRE,15));
        TAGS.put("minecraft:witch",new AspectList().add(Aspect.MAN,3).add(Aspect.MAGIC,2).add(Aspect.FIRE,1));
        TAGS.put("minecraft:villager",new AspectList().add(Aspect.MAN,3).add(Aspect.AIR,2));
        TAGS.put("minecraft:iron_golem",new AspectList().add(Aspect.METAL,4).add(Aspect.EARTH,3));
        TAGS.put("minecraft:minecart",new AspectList().add(Aspect.MECHANISM,3).add(Aspect.AIR,2));
        TAGS.put("minecraft:chest_minecart",new AspectList().add(Aspect.MECHANISM,3).add(Aspect.AIR,1).add(Aspect.VOID,1));
        TAGS.put("minecraft:furnace_minecart",new AspectList().add(Aspect.MECHANISM,3).add(Aspect.AIR,1).add(Aspect.FIRE,1));
        TAGS.put("minecraft:tnt_minecart",new AspectList().add(Aspect.MECHANISM,3).add(Aspect.AIR,1).add(Aspect.FIRE,1));
        TAGS.put("minecraft:hopper_minecart",new AspectList().add(Aspect.MECHANISM,3).add(Aspect.AIR,1).add(Aspect.EXCHANGE,1));
        TAGS.put("minecraft:spawner_minecart",new AspectList().add(Aspect.MECHANISM,3).add(Aspect.AIR,1).add(Aspect.MAGIC,1));
        TAGS.put("minecraft:end_crystal",new AspectList().add(Aspect.ELDRITCH,3).add(Aspect.MAGIC,3).add(Aspect.HEAL,3));
        TAGS.put("minecraft:item_frame",new AspectList().add(Aspect.SENSES,3).add(Aspect.CLOTH,1));
        TAGS.put("minecraft:painting",new AspectList().add(Aspect.SENSES,5).add(Aspect.CLOTH,3));
        TAGS.put("tc4modern:primal_orb",new AspectList().add(Aspect.AIR, 5).add(Aspect.ENTROPY, 10).add(Aspect.MAGIC, 10).add(Aspect.ENERGY, 10));
        TAGS.put("tc4modern:firebat",new AspectList().add(Aspect.BEAST, 2).add(Aspect.FLIGHT, 1).add(Aspect.FIRE, 2));
        TAGS.put("tc4modern:thaumic_slime",new AspectList().add(Aspect.SLIME, 2).add(Aspect.MAGIC, 1).add(Aspect.WATER, 1));
        TAGS.put("tc4modern:brainy_zombie",new AspectList().add(Aspect.UNDEAD, 3).add(Aspect.MAN, 1).add(Aspect.MIND, 1).add(Aspect.EARTH, 1));
        TAGS.put("tc4modern:giant_brainy_zombie",new AspectList().add(Aspect.UNDEAD, 4).add(Aspect.MAN, 2).add(Aspect.MIND, 1).add(Aspect.EARTH, 2));
        TAGS.put("tc4modern:taint_spider",new AspectList().add(Aspect.TAINT, 1).add(Aspect.EARTH, 1));
        TAGS.put("tc4modern:taint_spore",new AspectList().add(Aspect.TAINT, 2).add(Aspect.AIR, 2));
        TAGS.put("tc4modern:taint_swarm",new AspectList().add(Aspect.TAINT, 3).add(Aspect.AIR, 3));
        TAGS.put("tc4modern:mind_spider",new AspectList().add(Aspect.TAINT, 2).add(Aspect.FIRE, 2));
        TAGS.put("tc4modern:eldritch_orb",new AspectList().add(Aspect.ELDRITCH, 2).add(Aspect.DEATH, 2));
    }
    public static AspectList get(String id) { return TAGS.containsKey(id)?TAGS.get(id).copy():new AspectList(); }
    public static AspectList player(String name) {
        String id="player_"+name;
        AspectList tags=new AspectList().add(Aspect.MAN,4);
        if(name.equalsIgnoreCase("azanor"))tags.add(Aspect.ELDRITCH,20);
        else if(name.equalsIgnoreCase("direwolf20"))tags.add(Aspect.BEAST,20);
        else if(name.equalsIgnoreCase("pahimar"))tags.add(Aspect.EXCHANGE,20);
        else {
            java.util.Random random=new java.util.Random(id.hashCode());
            Aspect[] aspects=Aspect.aspects.values().toArray(new Aspect[0]);
            for(int i=0;i<3;i++)tags.add(aspects[random.nextInt(aspects.length)],4);
        }
        return tags;
    }
}
