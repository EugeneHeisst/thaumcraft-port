package dev.eugheist.tc4modern;

import net.minecraft.nbt.CompoundTag;

public final class ClientKnowledge {
    public static CompoundTag knowledge = new CompoundTag();
    public static String status = "";
    public static long messageUntil;
    public static final NotificationQueue notifications = new NotificationQueue(WarpClientConfig::notificationDelay);
    private ClientKnowledge() {}
    public static int researchDifficulty(){return knowledge.getCompound("Rules").getInt("researchDifficulty");}
    public static void receiveClue(ResearchCluePayload payload) {
        var player = net.minecraft.client.Minecraft.getInstance().player;
        if (player == null) return;
        notifications.add("\u00a7a" + net.minecraft.network.chat.Component.translatable("tc.addclue").getString(), System.nanoTime() / 1_000_000L);
        player.playSound(TC4Modern.LEARN.get(), 0.2f, 1.0f + player.level().random.nextFloat() * 0.1f);
    }
    public static void receive(ScanPayload payload) {
        knowledge = payload.knowledge().copy();
        status = payload.status();
        messageUntil = System.nanoTime() + 2_500_000_000L;
    }
}
