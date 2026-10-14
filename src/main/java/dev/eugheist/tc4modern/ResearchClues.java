package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchItem;

/** Original ResearchManager.createClue aspect candidate rules. The input is awarded aspects. */
public final class ResearchClues {
    private ResearchClues() {}
    public static void revealClue(net.minecraft.server.level.ServerPlayer player, String entityClue, String itemClue, AspectList awarded) {
        var data = player.getPersistentData();
        var completed = ResearchProgress.completed(data);
        var keys = candidates(completed, entityClue, itemClue, awarded);
        if (keys.isEmpty()) return;
        String key = keys.get(player.getRandom().nextInt(keys.size()));
        completed.add("@" + key);
        var list = new net.minecraft.nbt.ListTag();
        completed.stream().sorted().forEach(completedKey -> list.add(net.minecraft.nbt.StringTag.valueOf(completedKey)));
        var knowledge = data.getCompound(ResearchKnowledge.KEY);
        knowledge.put("Research", list);
        data.put(ResearchKnowledge.KEY, knowledge);
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new ResearchCluePayload(key));
    }
    static boolean matchesItemTrigger(String research, String item) {
        if (item == null) return false;
        return switch (research) {
            case "PRIMPEARL" -> item.equals("tc4modern:primordial_pearl");
            /* blockEldritch 5 and 10: glyphed and runed stone of the Outer Lands */
            case "OUTERREV" -> item.equals("tc4modern:glyphed_stone") || item.equals("tc4modern:runed_stone");
            case "BANNERS" -> item.equals("tc4modern:crimson_banner");
            case "ROD_primal_staff" -> item.equals("tc4modern:focus_primal");
            case "JARBRAIN" -> item.equals("tc4modern:quicksilver");
            case "MIRROR" -> Set.of("minecraft:ender_pearl", "minecraft:nether_portal", "minecraft:end_portal", "minecraft:end_portal_frame").contains(item);
            case "BONEBOW" -> item.equals("minecraft:bow") || item.equals("minecraft:bone");
            case "TINYHAT", "TINYGLASSES", "TINYBOWTIE", "TINYFEZ" -> item.startsWith("minecraft:") && Set.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black").stream().anyMatch(color -> item.equals("minecraft:" + color + "_wool"));
            default -> false;
        };
    }
    public static List<String> aspectCandidates(Set<String> completed, AspectList awarded) {
        return candidates(completed, null, null, awarded);
    }
    public static List<String> candidates(Set<String> completed, String entityClue, String itemClue, AspectList awarded) {
        var keys = new ArrayList<String>();
        for (var category : ResearchCategories.researchCategories.values()) {
            for (ResearchItem research : category.research.values()) {
                if (research.tags == null || research.tags.size() == 0
                        || !(research.isLost() || research.isHidden())
                        || completed.contains(research.key) || completed.contains("@" + research.key)) continue;
                if (matchesItemTrigger(research.key, itemClue)) { keys.add(research.key); continue; }
                boolean entityMatch = false;
                if (entityClue != null && research.getEntityTriggers() != null)
                    for (String trigger : research.getEntityTriggers())
                        if (entityClue.equals(trigger)) { entityMatch = true; break; }
                if (entityMatch) { keys.add(research.key); continue; }
                if (awarded == null || research.getAspectTriggers() == null) continue;
                for (var aspect : research.getAspectTriggers()) {
                    if (awarded.getAmount(aspect) <= 0) continue;
                    keys.add(research.key);
                    break;
                }
            }
        }
        return keys;
    }
}
