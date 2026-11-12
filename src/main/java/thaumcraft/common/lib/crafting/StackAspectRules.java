package thaumcraft.common.lib.crafting;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
/** Original ThaumcraftCraftingManager stack-dependent tags, with modern effect/enchantment identifiers. */
public final class StackAspectRules {
    public static void wand(AspectList tags, int rodCost, int capCost) {
        tags.merge(Aspect.MAGIC,(rodCost+capCost)/2); tags.merge(Aspect.TOOL,(rodCost+capCost)/3);
    }
    public static void effect(AspectList tags, String id, int amplifier) {
        int n=amplifier+1; tags.merge(Aspect.MAGIC,n*2);
        switch(id) {
            case "minecraft:blindness" -> { tags.merge(Aspect.DARKNESS,n*3); }
            case "minecraft:nausea" -> { tags.merge(Aspect.ELDRITCH,n*3); }
            case "minecraft:strength" -> { tags.merge(Aspect.WEAPON,n*3); }
            case "minecraft:mining_fatigue" -> { tags.merge(Aspect.TRAP,n*3); }
            case "minecraft:haste" -> { tags.merge(Aspect.TOOL,n*3); }
            case "minecraft:fire_resistance" -> { tags.merge(Aspect.ARMOR,n*1); tags.merge(Aspect.FIRE,n*2); }
            case "minecraft:instant_damage" -> { tags.merge(Aspect.DEATH,n*3); }
            case "minecraft:instant_health" -> { tags.merge(Aspect.HEAL,n*3); }
            case "minecraft:hunger" -> { tags.merge(Aspect.DEATH,n*3); }
            case "minecraft:invisibility" -> { tags.merge(Aspect.SENSES,n*3); }
            case "minecraft:jump_boost" -> { tags.merge(Aspect.FLIGHT,n*3); }
            case "minecraft:slowness" -> { tags.merge(Aspect.TRAP,n*3); }
            case "minecraft:speed" -> { tags.merge(Aspect.MOTION,n*3); }
            case "minecraft:night_vision" -> { tags.merge(Aspect.SENSES,n*3); }
            case "minecraft:poison" -> { tags.merge(Aspect.POISON,n*3); }
            case "minecraft:regeneration" -> { tags.merge(Aspect.HEAL,n*3); }
            case "minecraft:resistance" -> { tags.merge(Aspect.ARMOR,n*3); }
            case "minecraft:water_breathing" -> { tags.merge(Aspect.AIR,n*3); }
            case "minecraft:weakness" -> { tags.merge(Aspect.DEATH,n*3); }
        }
    }
    public static void enchantment(AspectList tags,String id,int level) {
        switch(id) {
            case "minecraft:aqua_affinity" -> tags.merge(Aspect.WATER,level);
            case "minecraft:bane_of_arthropods" -> tags.merge(Aspect.BEAST,level);
            case "minecraft:blast_protection" -> tags.merge(Aspect.ARMOR,level);
            case "minecraft:efficiency" -> tags.merge(Aspect.TOOL,level);
            case "minecraft:feather_falling" -> tags.merge(Aspect.FLIGHT,level);
            case "minecraft:fire_aspect" -> tags.merge(Aspect.FIRE,level);
            case "minecraft:fire_protection" -> tags.merge(Aspect.ARMOR,level);
            case "minecraft:flame" -> tags.merge(Aspect.FIRE,level);
            case "minecraft:fortune" -> tags.merge(Aspect.GREED,level);
            case "minecraft:infinity" -> tags.merge(Aspect.CRAFT,level);
            case "minecraft:knockback" -> tags.merge(Aspect.AIR,level);
            case "minecraft:looting" -> tags.merge(Aspect.GREED,level);
            case "minecraft:power" -> tags.merge(Aspect.WEAPON,level);
            case "minecraft:projectile_protection" -> tags.merge(Aspect.ARMOR,level);
            case "minecraft:protection" -> tags.merge(Aspect.ARMOR,level);
            case "minecraft:punch" -> tags.merge(Aspect.AIR,level);
            case "minecraft:respiration" -> tags.merge(Aspect.AIR,level);
            case "minecraft:sharpness" -> tags.merge(Aspect.WEAPON,level);
            case "minecraft:silk_touch" -> tags.merge(Aspect.EXCHANGE,level);
            case "minecraft:thorns" -> tags.merge(Aspect.WEAPON,level);
            case "minecraft:smite" -> tags.merge(Aspect.ENTROPY,level);
            case "minecraft:unbreaking" -> tags.merge(Aspect.EARTH,level);
            case "minecraft:luck_of_the_sea" -> tags.merge(Aspect.GREED,level);
            case "minecraft:lure" -> tags.merge(Aspect.BEAST,level);
        }
    }
}
