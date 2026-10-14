package dev.eugheist.tc4modern;

import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.aspects.Aspect;

/** Regression coverage for the original awarded-aspect input to research clues. */
public final class ResearchClueScanTest {
    static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    public static void run() {
        var data = new CompoundTag();
        int[] calls = {0};
        require(ResearchKnowledge.scan(data, "clue:first", Map.of("terra", 4), awarded -> {
            calls[0]++;
            require(awarded.getAmount(Aspect.EARTH) == 4, "Known primal awards actual amount");
            require(ResearchKnowledge.points(data, "terra") == 4, "Callback runs after persisted award");
        }).equals("scanned"), "Successful scan status");
        require(ResearchKnowledge.scan(data, "clue:first", Map.of("terra", 4), a -> calls[0]++).equals("already"), "Repeat status");
        require(ResearchKnowledge.scan(new CompoundTag(), "clue:locked", Map.of("metallum", 4), a -> calls[0]++).startsWith("missing:"), "Compound prerequisites");
        require(ResearchKnowledge.scan(data, "clue:unknown", Map.of(), a -> calls[0]++).equals("unknown"), "Empty scan status");
        require(calls[0] == 1, "Failed and repeated scans never dispatch clues");
        var capped = ResearchKnowledge.load(data);
        capped.addAspectPool("current", Aspect.EARTH, (short)96);
        ResearchKnowledge.save(data, capped);
        require(ResearchKnowledge.scan(data, "clue:cap", Map.of("terra", 16), awarded -> {
            calls[0]++;
            require(awarded.getAmount(Aspect.EARTH) == 4, "Original cap applies sqrt before clue dispatch");
        }).equals("scanned"), "Capped scan remains valid");
        require(ResearchKnowledge.points(data, "terra") == 104, "Original cap is diminishing returns, not hard limit");
        thaumcraft.common.config.ConfigResearchData.init();
        var lost = thaumcraft.api.research.ResearchCategories.getResearch("OUTERREV");
        require(!ResearchProgress.visible(java.util.Set.of(), lost), "Undiscovered lost research hidden");
        require(ResearchProgress.visible(java.util.Set.of("@OUTERREV"), lost), "Original lost research clue shows icon");
        require(ResearchProgress.visible(java.util.Set.of("OUTERREV"), lost), "Learned lost research stays readable");
        require(!ResearchProgress.requisites(java.util.Set.of("@OUTERREV"), lost), "Clue does not bypass unlock prerequisites");
        var metal = new thaumcraft.api.aspects.AspectList().add(Aspect.METAL, 1);
        require(ResearchClues.aspectCandidates(java.util.Set.of(), metal).contains("THAUMIUM"), "Thaumium clue does not require Crucible completion");
        require(!ResearchClues.aspectCandidates(java.util.Set.of("@THAUMIUM"), metal).contains("THAUMIUM"), "Revealed clue excluded");
        require(!ResearchClues.aspectCandidates(java.util.Set.of("THAUMIUM"), metal).contains("THAUMIUM"), "Learned research excluded");
        var empty = new thaumcraft.api.aspects.AspectList();
        require(ResearchClues.candidates(java.util.Set.of(), "Enderman", null, empty).contains("MIRROR"), "Original entity clue independent of aspect awards");
        require(ResearchClues.candidates(java.util.Set.of(), null, "minecraft:ender_pearl", empty).contains("MIRROR"), "Original pearl clue");
        require(!ResearchClues.candidates(java.util.Set.of(), null, "minecraft:stone", empty).contains("MIRROR"), "Unrelated item cannot reveal mirror");
        require(ResearchClues.candidates(java.util.Set.of(), null, "tc4modern:quicksilver", empty).contains("JARBRAIN"), "Original resource metadata3 is quicksilver; no Infusion parent required for clue");
        require(!ResearchClues.candidates(java.util.Set.of("@JARBRAIN"), null, "tc4modern:quicksilver", empty).contains("JARBRAIN"), "Quicksilver cannot repeat a revealed clue");
        require(!ResearchClues.candidates(java.util.Set.of("JARBRAIN"), null, "tc4modern:quicksilver", empty).contains("JARBRAIN"), "Quicksilver cannot reveal learned Jar Brain");
        require(!ResearchClues.matchesItemTrigger("JARBRAIN", "tc4modern:quicksilver_drop"), "Resource metadata3 does not match quicksilver nugget");
        require(!ResearchClues.matchesItemTrigger("JARBRAIN", "tc4modern:zombie_brain"), "Zombie brain is recipe ingredient, not original item trigger");
        var jarBrain = thaumcraft.api.research.ResearchCategories.getResearch("JARBRAIN");
        require(!ResearchProgress.requisites(java.util.Set.of("@JARBRAIN"), jarBrain), "Clue still requires Infusion to unlock");
        var both = ResearchClues.candidates(java.util.Set.of(), "Enderman", "minecraft:ender_pearl", empty);
        require(both.stream().filter("MIRROR"::equals).count() == 1, "Multiple matching conditions produce one candidate");
        for (String color : java.util.List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black"))
            require(ResearchClues.matchesItemTrigger("TINYHAT", "minecraft:" + color + "_wool"), "Original wool metadata wildcard: " + color);
        require(!ResearchClues.matchesItemTrigger("TINYHAT", "minecraft:white_carpet"), "Carpet is not original wool trigger");
        require("Enderman".equals(new ScanTarget("test", "entity:minecraft:enderman", Map.of()).entityClue()), "Modern Enderman maps original entity name");
        System.out.println("Research clue scan tests passed: awards, persistence ordering, duplicates, prerequisites, cap scaling.");
    }
}
