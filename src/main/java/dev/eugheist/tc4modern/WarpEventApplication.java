package dev.eugheist.tc4modern;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server consequence dispatch; entity events remain with their original spawn adapter. */
public final class WarpEventApplication {
    private WarpEventApplication() {}
    public static boolean apply(ServerPlayer player, WarpEventPlan.Plan plan) {
        switch (plan.kind()) {
            case NONE -> { return true; }
            case KNOWLEDGE -> WarpResearchApplication.grant(player, plan.count());
            case MESSAGE -> {}
            case REMOVE_STICKY -> {
                var state=PlayerWarp.load(player);int sticky=state.get(WarpState.Kind.STICKY);
                if(sticky>0){state.set(WarpState.Kind.STICKY,sticky-1);PlayerWarp.save(player,state);PacketDistributor.sendToPlayer(player,new WarpSyncPayload(sticky-1,WarpState.Kind.STICKY.ordinal()));PacketDistributor.sendToPlayer(player,new WarpMessagePayload(-1,WarpState.Kind.STICKY.ordinal()));}
            }
            case MIND_SPIDERS -> WarpSpiderSpawns.spawn(player, plan.count(), false);
            case REAL_MIND_SPIDERS -> WarpSpiderSpawns.spawn(player, plan.count(), true);
            case MIST -> WarpRuntime.mist(player,plan.count());
            default -> {
                if (!WarpEffectApplication.apply(player, plan)) return false;
            }
        }
        message(player, plan.message());
        return true;
    }
    public static void message(ServerPlayer player, String number) {
        if (number != null) player.sendSystemMessage(Component.translatable(number.startsWith("warp.text.")?number:"warp.text."+number)
            .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
    /** Run after a triggered event, including an otherwise quiet effect roll. */
    public static void unlockResearch(ServerPlayer player, int permanent, int sticky) {
        var data = player.getPersistentData();
        var completed = ResearchProgress.completed(data);
        for (var unlock : WarpResearch.unlocks(permanent, sticky, completed::contains)) {
            if (unlock.researchRollBound() > 0) WarpResearchApplication.grant(player, unlock.researchRollBound());
            if (!WarpResearchApplication.persistUnlock(data, unlock.key())) continue;
            ThaumometerItem.sync(player, "");
            if (unlock.key().startsWith("@"))
                PacketDistributor.sendToPlayer(player, new ResearchCluePayload(unlock.key().substring(1)));
            else PacketDistributor.sendToPlayer(player, new ResearchCompletedPayload(unlock.key()));
            if (unlock.message() > 0) message(player, Integer.toString(unlock.message()));
        }
    }
}
