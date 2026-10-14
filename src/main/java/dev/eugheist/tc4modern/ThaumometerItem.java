package dev.eugheist.tc4modern;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.network.PacketDistributor;

public class ThaumometerItem extends Item {
    public static final int SCAN_TICKS = 20;
    private static final String START = "tc4modern_scan_start";
    public ThaumometerItem(Properties properties) { super(properties); }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72000; }
    @Override public InteractionResult useOn(UseOnContext context) {
        if (context.getPlayer() == null) return InteractionResult.PASS;
        begin(context.getLevel(), context.getPlayer(), context.getHand());
        return InteractionResult.CONSUME;
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        begin(level, player, hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }
    @Override public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        begin(player.level(), player, hand);
        return InteractionResult.CONSUME;
    }
    private void begin(Level level, Player player, InteractionHand hand) {
        var target = ScanTarget.look(player);
        if (target == null) return;
        player.getPersistentData().putString(START, target.identity());
        player.startUsingItem(hand);
    }
    @Override public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player)) return;
        var target = ScanTarget.look(player);
        if (target == null || !target.identity().equals(player.getPersistentData().getString(START))) {
            player.stopUsingItem();
            return;
        }
        if (level.isClientSide) ScanRunes.scanTick(player, target, remaining);
        if (getUseDuration(stack, entity) - remaining < SCAN_TICKS) return;
        if (player instanceof ServerPlayer serverPlayer) {
            String status = ResearchKnowledge.scan(player.getPersistentData(), target.knowledgeKey(), target.aspects(), awarded -> { AspectGain.send(serverPlayer, awarded); ResearchClues.revealClue(serverPlayer, target.entityClue(), target.knowledgeKey().startsWith("entity:") || target.knowledgeKey().startsWith("node:") ? null : target.knowledgeKey(), awarded); });
            sync(serverPlayer, status);
        }
        player.stopUsingItem();
    }
    public static void sync(ServerPlayer player, String status) {
        ResearchKnowledge.initialize(player.getPersistentData());
        PacketDistributor.sendToPlayer(player, new ScanPayload(status, ResearchConfig.snapshot(player.getPersistentData().getCompound(ResearchKnowledge.KEY))));
    }
}
