package dev.eugheist.tc4modern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import thaumcraft.api.aspects.Aspect;

/** Server persistence bridge for original event awards, independent of browser prerequisites. */
public final class WarpResearchApplication {
    private WarpResearchApplication() {}
    public static void grant(ServerPlayer player, int times) {
        WarpResearch.grant(times, player.serverLevel().random::nextInt, index -> {
            var data = player.getPersistentData();
            var knowledge = ResearchKnowledge.load(data);
            knowledge.addAspectPool("current", Aspect.getPrimalAspects().get(index), (short) 1);
            ResearchKnowledge.save(data, knowledge);
            ThaumometerItem.sync(player, "");
            AspectGain.send(player, Aspect.getPrimalAspects().get(index), 1);
        });
    }
    /** Original automatic unlocks bypass research-table prerequisites, including @ discovery keys. */
    public static boolean persistUnlock(net.minecraft.nbt.CompoundTag data, String key) {
        var completed = ResearchProgress.completed(data);
        if (!completed.add(key)) return false;
        ListTag list = new ListTag();
        completed.stream().sorted().forEach(value -> list.add(StringTag.valueOf(value)));
        var knowledge = data.getCompound(ResearchKnowledge.KEY);
        knowledge.put("Research", list);
        data.put(ResearchKnowledge.KEY, knowledge);
        return true;
    }
}
