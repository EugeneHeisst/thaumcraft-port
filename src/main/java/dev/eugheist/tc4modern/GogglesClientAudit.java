package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.*;

/** Opt-in hidden client check of goggles aspect popups over a node, an energized node and a jar-free empty case. */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class GogglesClientAudit {
    static boolean enabled() { return Boolean.getBoolean("tc4modern.gogglesAudit"); }

    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!enabled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        var level = player.serverLevel();
        player.setGameMode(GameType.CREATIVE);
        level.setDayTime(6000);
        level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        var base = new BlockPos(0, 200, 0);
        for (int x = -4; x <= 4; x++) for (int z = -2; z <= 6; z++) {
            level.setBlock(base.offset(x, -1, z), Blocks.STONE.defaultBlockState(), 3);
            for (int y = 0; y < 4; y++) level.setBlock(base.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        var nodePos = base.offset(0, 1, 3);
        level.setBlock(nodePos, TC4Modern.AURA_NODE.get().defaultBlockState(), 3);
        var node = (AuraNodeEntity) level.getBlockEntity(nodePos);
        var aspects = new AspectList();
        for (var primal : Aspect.getPrimalAspects()) aspects.add(primal, 20 + primal.getTag().length());
        aspects.add(Aspect.MAGIC, 12).add(Aspect.PLANT, 7);
        node.reservoir.aspects = aspects;
        node.reservoir.aspectsBase = aspects.copy();
        node.changed();
        // Deconstruction table at work and see-through check for infused ore beside it.
        var tablePos = base.offset(-1, 0, 2);
        level.setBlock(tablePos, BasicsContent.DECONSTRUCTOR.get().defaultBlockState(), 3);
        if (level.getBlockEntity(tablePos) instanceof DeconstructorEntity table) {
            table.setItem(0, new ItemStack(net.minecraft.world.item.Items.OAK_LOG, 64));
            table.state.aspect = Aspect.TREE;
            table.changed();
        }
        var registry = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
        level.setBlock(base.offset(1, 0, 2), registry.get(AlchemyContent.id("air_infused_stone")).defaultBlockState(), 3);
        level.setBlock(base.offset(2, 0, 2), registry.get(AlchemyContent.id("fire_infused_stone")).defaultBlockState(), 3);
        level.setBlock(base.offset(2, 0, 3), registry.get(AlchemyContent.id("order_infused_stone")).defaultBlockState(), 3);
        player.getInventory().setItem(0, new ItemStack(BasicsContent.DECONSTRUCTOR.get()));
        player.getInventory().setItem(1, new ItemStack(EssentiaDevices.ALEMBIC.get()));
        player.getInventory().selected = 0;
        var baubles = BaublesInventory.of(player);
        baubles.setItem(0, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("vis_amulet_lesser"))));
        baubles.setItem(1, new ItemStack(MinersRingItem.RING.get()));
        player.getInventory().setItem(2, new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(AlchemyContent.id("focus_pouch"))));
        player.getInventory().setItem(3, new ItemStack(MinersRingItem.RING.get()));
        var known = new java.util.HashMap<String, Integer>();
        for (var primal : Aspect.getPrimalAspects()) known.put(primal.getTag(), 1);
        ResearchKnowledge.discover(player.getPersistentData(), "goggles_audit", known);
        ThaumometerItem.sync(player, "");
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(TC4Modern.GOGGLES.get()));
        double eye = base.getY() + player.getEyeHeight();
        float pitch = (float) Math.toDegrees(Math.atan2(eye - (nodePos.getY() + .5), 2.5));
        player.connection.teleport(base.getX() + .5, base.getY(), base.getZ() + .5, 0, pitch);
        System.out.println("TC4_GOGGLES_AUDIT ready node=" + nodePos);
    }

    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
    static final class Client {
        private static int ticks;
        @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            if (!enabled()) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            ticks++;
            var out = java.nio.file.Path.of("goggles-audit");
            try {
                java.nio.file.Files.createDirectories(out);
                if (ticks == 200 || ticks == 260) {
                    String name = ticks == 200 ? "goggles-on.png" : "goggles-off.png";
                    var hit = mc.hitResult;
                    String target = hit instanceof net.minecraft.world.phys.BlockHitResult b ? mc.level.getBlockState(b.getBlockPos()).toString() : String.valueOf(hit);
                    try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(out.resolve(name)); }
                    java.nio.file.Files.writeString(out.resolve("result.txt"), name + " tagscale=" + GogglesAspectTags.tagscale + " target=" + target + "\n",
                        java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
                    System.out.println("TC4_GOGGLES_AUDIT " + name + " tagscale=" + GogglesAspectTags.tagscale + " target=" + target);
                    if (ticks == 200) { var server = mc.getSingleplayerServer(); var id = mc.player.getUUID(); server.execute(() -> server.getPlayerList().getPlayer(id).setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY)); }
                }
                if (ticks == 280) net.neoforged.neoforge.network.PacketDistributor.sendToServer(BaublesNetwork.Open.INSTANCE);
                if (ticks == 320 || ticks == 360) {
                    String name = ticks == 320 ? "baubles-screen.png" : "inventory-screen.png";
                    System.out.println("TC4_BAUBLES_AUDIT " + name + " screen=" + mc.screen + " haste=" + mc.player.hasEffect(net.minecraft.world.effect.MobEffects.DIG_SPEED));
                    try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(out.resolve(name)); }
                }
                if (ticks == 322) { var server = mc.getSingleplayerServer(); var id = mc.player.getUUID(); server.execute(() -> server.getPlayerList().getPlayer(id).setGameMode(GameType.SURVIVAL)); }
                if (ticks == 340) BaublesScreen.Client.backToInventory();
                if (ticks == 380) mc.stop();
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
    }
}
