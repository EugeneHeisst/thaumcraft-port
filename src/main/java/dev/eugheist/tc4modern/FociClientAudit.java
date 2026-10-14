package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Opt-in hidden client check of the excavation, equal trade and portable hole foci against a stone wall. */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class FociClientAudit {
    static boolean enabled() { return Boolean.getBoolean("tc4modern.fociAudit"); }
    static final BlockPos BASE = new BlockPos(0, 120, 0);

    static ItemStack wand(ItemStack focus) {
        var wand = WandParts.make(WandParts.CAPS.get("thaumium"), WandParts.RODDEFS.get("silverwood"));
        for (var a : Aspect.getPrimalAspects()) WandData.add(wand, a, WandData.capacity(wand), true);
        FociContent.setFocus(wand, focus);
        return wand;
    }

    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!enabled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        var level = player.serverLevel();
        player.setGameMode(GameType.SURVIVAL);
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        for (int x = -8; x <= 8; x++) for (int z = -4; z <= 12; z++) for (int y = -1; y < 8; y++)
            level.setBlock(BASE.offset(x, y, z), y < 0 || z >= 4 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        player.getInventory().clearContent();
        player.getInventory().setItem(0, wand(new ItemStack(FociWorld.EXCAVATION.get())));
        player.getInventory().setItem(1, new ItemStack(Blocks.GLASS, 64));
        player.getInventory().selected = 0;
        player.connection.teleport(BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 10);
        System.out.println("TC4_FOCI_AUDIT ready");
    }

    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
    static final class Client {
        private static int ticks;
        private static void log(String s) { System.out.println("TC4_FOCI_AUDIT " + s); }
        @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            if (!enabled()) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            ticks++;
            mc.player.getInventory().selected = 0;
            var server = mc.getSingleplayerServer();
            var id = mc.player.getUUID();
            var out = java.nio.file.Path.of("foci-audit");
            try {
                java.nio.file.Files.createDirectories(out);
                // Excavation: hold the beam on the wall for 4 seconds.
                if (ticks >= 100 && ticks < 180) {
                    var wand = mc.player.getInventory().getItem(0);
                    if (FociContent.getFocus(wand) != null) FociContent.getFocus(wand).onUsingFocusTick(wand, mc.player, ticks);
                    server.execute(() -> { var p = server.getPlayerList().getPlayer(id); var w = p.getMainHandItem(); FociContent.getFocus(w).onUsingFocusTick(w, p, 0); });
                }
                if (ticks == 140) shot(mc, out, "excavation.png");
                if (ticks == 182) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    log("excavation wallAir=" + p.level().getBlockState(BASE.offset(0, 1, 4)).isAir() + " cobble=" + p.getInventory().countItem(Blocks.COBBLESTONE.asItem())
                        + " terra=" + WandData.get(p.getMainHandItem(), Aspect.EARTH));
                    FociContent.getFocus(p.getMainHandItem()).onPlayerStoppedUsingFocus(p.getMainHandItem(), p.level(), p, 0);
                    // Equal trade: pick glass from a placed block, then swap stone.
                    p.level().setBlock(BASE.offset(2, 1, 2), Blocks.GLASS.defaultBlockState(), 3);
                    p.getInventory().setItem(0, wand(new ItemStack(FociWorld.TRADE.get())));
                });
                if (ticks == 184) FociFx.stopBeam(mc.player);
                if (ticks == 190) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    p.connection.teleport(BASE.getX() + 2.5, BASE.getY(), BASE.getZ() + .5, 0, 0);
                });
                if (ticks == 200) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    p.setShiftKeyDown(true);
                    FociContent.getFocus(p.getMainHandItem()).onFocusRightClick(p.getMainHandItem(), p.level(), p);
                    p.setShiftKeyDown(false);
                    log("trade picked=" + FociWorld.TradeFocus.picked(p.getMainHandItem()));
                    p.level().removeBlock(BASE.offset(2, 1, 2), false);
                    p.connection.teleport(BASE.getX() - 2.5, BASE.getY(), BASE.getZ() + 1.5, 0, 0);
                });
                if (ticks == 210) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    FociContent.getFocus(p.getMainHandItem()).onFocusRightClick(p.getMainHandItem(), p.level(), p);
                });
                if (ticks == 260) {
                    shot(mc, out, "trade.png");
                    server.execute(() -> {
                        var p = server.getPlayerList().getPlayer(id);
                        int glass = 0;
                        for (int x = -4; x <= 4; x++) for (int y = 0; y < 6; y++) for (int z = 4; z <= 12; z++) if (p.level().getBlockState(BASE.offset(x, y, z)).is(Blocks.GLASS)) glass++;
                        log("trade glassWall=" + glass + " glassLeft=" + p.getInventory().countItem(Blocks.GLASS.asItem()) + " stoneGot=" + p.getInventory().countItem(Blocks.COBBLESTONE.asItem()));
                        p.getInventory().setItem(0, wand(new ItemStack(FociWorld.PORTABLE_HOLE.get())));
                        p.connection.teleport(BASE.getX() + 5.5, BASE.getY(), BASE.getZ() + 1.5, 0, 0);
                    });
                }
                if (ticks == 280) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    FociContent.getFocus(p.getMainHandItem()).onFocusRightClick(p.getMainHandItem(), p.level(), p);
                });
                if (ticks == 300) {
                    shot(mc, out, "hole.png");
                    server.execute(() -> {
                        var p = server.getPlayerList().getPlayer(id);
                        int holes = 0;
                        for (int z = 4; z <= 12; z++) if (p.level().getBlockState(BASE.offset(5, 1, z)).is(FociWorld.HOLE.get())) holes++;
                        log("hole depth=" + holes + " ring=" + p.level().getBlockState(BASE.offset(4, 0, 5)).is(FociWorld.HOLE.get()));
                    });
                }
                if (ticks == 440) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    log("hole restored=" + p.level().getBlockState(BASE.offset(5, 1, 6)).is(Blocks.STONE));
                });
                if (ticks == 445) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    int slot = 2;
                    for (var item : java.util.List.of(FociContent.FIRE.get(), FociContent.FROST.get(), FociContent.SHOCK.get(), FociWorld.EXCAVATION.get(), FociWorld.TRADE.get()))
                        p.getInventory().setItem(slot++, new ItemStack(item));
                });
                if (ticks == 455) FociClient.KEY_F.setDown(true);
                if (ticks == 470) {
                    // Hover the first (top) focus: the radial ring radius is 16 + 2.5 per focus, in GUI pixels.
                    var window = mc.getWindow();
                    double scale = window.getGuiScale(), radius = 16 + 5 * 2.5;
                    try {
                        // Xvfb sends no cursor callback for glfwSetCursorPos, so place the tracked position directly.
                        var x = net.minecraft.client.MouseHandler.class.getDeclaredField("xpos"); x.setAccessible(true); x.setDouble(mc.mouseHandler, window.getScreenWidth() / 2.0);
                        var y = net.minecraft.client.MouseHandler.class.getDeclaredField("ypos"); y.setAccessible(true); y.setDouble(mc.mouseHandler, window.getScreenHeight() / 2.0 - radius * scale);
                    } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
                }
                if (ticks == 490) shot(mc, out, "radial.png");
                if (ticks == 495) FociClient.KEY_F.setDown(false);
                if (ticks == 510) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    log("radial focus=" + FociContent.getFocusItem(p.getMainHandItem()).getItem() + " holeBack=" + p.getInventory().countItem(FociWorld.PORTABLE_HOLE.get()));
                });
                if (ticks == 520) server.execute(() -> {
                    // Wand recharge pedestal with splitter, an aura node three blocks away, and an empty wand.
                    var p = server.getPlayerList().getPlayer(id);
                    var level = p.serverLevel();
                    var ped = BASE.offset(-3, 0, 2);
                    level.setBlock(ped, WandPedestal.PEDESTAL.get().defaultBlockState(), 3);
                    level.setBlock(ped.above(), WandPedestal.FOCUS.get().defaultBlockState(), 3);
                    var nodePos = BASE.offset(-6, 2, 2);
                    level.setBlock(nodePos, TC4Modern.AURA_NODE.get().defaultBlockState(), 3);
                    if (level.getBlockEntity(nodePos) instanceof AuraNodeEntity node) {
                        var aspects = new thaumcraft.api.aspects.AspectList().add(Aspect.FIRE, 40).add(Aspect.METAL, 40);
                        node.reservoir.aspects = aspects; node.reservoir.aspectsBase = aspects.copy(); node.changed();
                    }
                    var wand = WandParts.make(WandParts.CAPS.get("gold"), WandParts.RODDEFS.get("greatwood"));
                    var wandTag = NoteData.tag(wand).copy();
                    for (var a : Aspect.getPrimalAspects()) thaumcraft.common.lib.vis.VisStorage.storeVis(wandTag, a, 0);
                    wand.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(wandTag));
                    if (level.getBlockEntity(ped) instanceof WandPedestal.PedestalEntity pedestal) pedestal.setItem(wand);
                    p.connection.teleport(BASE.getX() - 2.5, BASE.getY(), BASE.getZ() - 1.5, 40, 10);
                });
                if (ticks == 900) {
                    shot(mc, out, "pedestal.png");
                    server.execute(() -> {
                        var level = server.overworld();
                        if (level.getBlockEntity(BASE.offset(-3, 0, 2)) instanceof WandPedestal.PedestalEntity pedestal) {
                            var w = pedestal.item();
                            log("pedestal ignis=" + WandData.get(w, Aspect.FIRE) + " ordo=" + WandData.get(w, Aspect.ORDER) + " terra=" + WandData.get(w, Aspect.EARTH) + " draining=" + pedestal.draining);
                        }
                    });
                }
                if (ticks == 1000) mc.stop();
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        private static void shot(net.minecraft.client.Minecraft mc, java.nio.file.Path out, String name) throws java.io.IOException {
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(out.resolve(name)); }
            log(name);
        }
    }
}
