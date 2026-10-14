package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.tiles.essentia.TubeVariants;

/** Essentia tube variants on real jars (flag tubeAuditOnly via runFocalAudit). */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class TubeAudit {
    static final BlockPos BASE = new BlockPos(-100, 120, -100);
    static int ticks, failures;
    static void check(boolean ok, String what) { if (!ok) failures++; System.out.println("TC4_TUBES " + (ok ? "PASS " : "FAIL ") + what); }
    static void set(ServerLevel l, BlockPos p, net.minecraft.world.level.block.Block b) { l.setBlock(p, b.defaultBlockState(), 3); }
    static WardedJarEntity jar(ServerLevel l, BlockPos p, Aspect aspect, int amount, Aspect label) {
        set(l, p, EssentiaDevices.JAR.get());
        var j = (WardedJarEntity) l.getBlockEntity(p); j.state.aspect = aspect; j.state.amount = amount; j.state.aspectFilter = label; j.changed(); return j;
    }
    static int amount(ServerLevel l, BlockPos p) { return ((WardedJarEntity) l.getBlockEntity(p)).state.amount; }
    static EssentiaTubeEntity tube(ServerLevel l, BlockPos p) { return (EssentiaTubeEntity) l.getBlockEntity(p); }
    // Valve line: source jar (0,0,0) -> tubes up/along -> valve (2,1,0) -> labeled Ignis jar (4,0,0).
    static BlockPos at(int x, int y, int z) { return BASE.offset(x, y, z); }
    static volatile int stage, lastDest;
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tc4modern.tubeAuditOnly")) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        var srv = mc.getSingleplayerServer(); var id = mc.player.getUUID();
        if (ticks == 40) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayer(id); var l = p.serverLevel(); l.setDayTime(6000);
            for (int x = -2; x <= 16; x++) for (int z = -3; z <= 10; z++) for (int y = -1; y < 4; y++) l.setBlock(at(x, y, z), y < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            check(ArcaneRecipes.entries().stream().anyMatch(e -> e.output().is(EssentiaTubes.VALVE.get().asItem())) && ArcaneRecipes.entries().stream().anyMatch(e -> e.output().is(EssentiaTubes.BUFFER.get().asItem())), "arcane recipes registered");
            var grid = new java.util.ArrayList<ItemStack>(java.util.Collections.nCopies(9, ItemStack.EMPTY));
            grid.set(4, new ItemStack(EssentiaTubes.TUBE.get())); grid.set(0, new ItemStack(net.minecraft.world.item.Items.LEVER));
            var r = ArcaneRecipes.find(grid, java.util.Set.of("TUBES"));
            check(r != null && r.output().is(EssentiaTubes.VALVE.get().asItem()), "shapeless tube + lever anywhere -> essentia valve");
            check(ArcaneRecipes.find(grid, java.util.Set.of()) == null, "valve recipe gated by TUBES");
            jar(l, at(0, 0, 0), Aspect.FIRE, 64, null);
            set(l, at(0, 1, 0), EssentiaTubes.TUBE.get()); set(l, at(1, 1, 0), EssentiaTubes.TUBE.get()); set(l, at(2, 1, 0), EssentiaTubes.VALVE.get()); set(l, at(3, 1, 0), EssentiaTubes.TUBE.get()); set(l, at(4, 1, 0), EssentiaTubes.TUBE.get());
            ((EssentiaTubeEntity) l.getBlockEntity(at(2, 1, 0))).state.facing = Direction.UP;
            jar(l, at(4, 0, 0), null, 0, Aspect.FIRE);
            // Suction checks: labeled Ignis jar (64) below each tested tube at z=4.., all in the open.
            jar(l, at(6, 0, 4), null, 0, Aspect.FIRE); set(l, at(6, 1, 4), EssentiaTubes.TUBE.get());
            jar(l, at(8, 0, 4), null, 0, Aspect.FIRE); set(l, at(8, 1, 4), EssentiaTubes.RESTRICT.get());
            jar(l, at(10, 0, 4), null, 0, null); set(l, at(10, 1, 4), EssentiaTubes.FILTER.get());
            ((TubeVariants.Filter) tube(l, at(10, 1, 4)).state).aspectFilter = Aspect.FIRE;
            jar(l, at(12, 0, 4), null, 0, Aspect.WATER); set(l, at(12, 1, 4), EssentiaTubes.FILTER.get());
            ((TubeVariants.Filter) tube(l, at(12, 1, 4)).state).aspectFilter = Aspect.FIRE;
            // Directional: labeled jar under (14,1,4), oneway beside it at (14,1,5) with a plain tube after it.
            jar(l, at(14, 0, 4), null, 0, Aspect.FIRE); set(l, at(14, 1, 4), EssentiaTubes.TUBE.get()); set(l, at(14, 1, 5), EssentiaTubes.ONEWAY.get()); set(l, at(14, 1, 6), EssentiaTubes.TUBE.get());
            ((EssentiaTubeEntity) l.getBlockEntity(at(14, 1, 5))).state.facing = Direction.SOUTH;
            // Buffer fed by a tube holding one essentia (suction 0).
            set(l, at(0, 1, 8), EssentiaTubes.TUBE.get()); set(l, at(1, 1, 8), EssentiaTubes.BUFFER.get());
            p.setGameMode(net.minecraft.world.level.GameType.CREATIVE); p.getAbilities().flying = true; p.onUpdateAbilities(); p.connection.teleport(BASE.getX() + 7.5, BASE.getY() + 3.5, BASE.getZ() - 2, 0, 45);
            stage = 1;
        });
        if (ticks == 140) srv.execute(() -> { var l = srv.overworld();
            check(amount(l, at(4, 0, 0)) > 0, "open valve: essentia flows into the labeled jar (" + amount(l, at(4, 0, 0)) + ")");
            check(tube(l, at(6, 1, 4)).state.suction == 63, "plain tube above a labeled jar: suction 63 (" + tube(l, at(6, 1, 4)).state.suction + ")");
            check(tube(l, at(8, 1, 4)).state.suction == 32, "restricted tube halves it: 32 (" + tube(l, at(8, 1, 4)).state.suction + ")");
            var f = tube(l, at(10, 1, 4)).state;
            check(f.suction == 31 && f.suctionType == Aspect.FIRE, "filter over an empty unlabeled jar asks for its own aspect (" + f.suction + " " + f.suctionType + ")");
            check(tube(l, at(12, 1, 4)).state.suction == 0, "filter refuses suction for another aspect (" + tube(l, at(12, 1, 4)).state.suction + ")");
            int oneway = tube(l, at(14, 1, 5)).state.suction;
            System.out.println("TC4_TUBES oneway facing SOUTH suction " + oneway + ", tail tube " + tube(l, at(14, 1, 6)).state.suction);
            ((EssentiaTubeEntity) l.getBlockEntity(at(14, 1, 5))).state.facing = Direction.NORTH;
            stage = oneway;
            // close the valve with redstone
            l.setBlock(at(2, 1, -1), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            var buffTube = tube(l, at(0, 1, 8)); buffTube.state.essentiaType = Aspect.AIR; buffTube.state.essentiaAmount = 1;
        });
        if (ticks == 150) LiquidDeathAudit.shot(mc, "tubes-variants.png");
        if (ticks == 152) srv.execute(() -> srv.getPlayerList().getPlayer(id).connection.teleport(BASE.getX() + 2.5, BASE.getY() + 1.6, BASE.getZ() - 1.4, 0, 20));
        if (ticks == 175) LiquidDeathAudit.shot(mc, "tubes-valve-closed.png");
        if (ticks == 320) LiquidDeathAudit.shot(mc, "tubes-valve-open.png");
        if (ticks == 170) srv.execute(() -> { var l = srv.overworld(); lastDest = amount(l, at(4, 0, 0));
            var v = (TubeVariants.Valve) tube(l, at(2, 1, 0)).state;
            check(!v.allowFlow && v.suction == 0, "redstone closes the valve and drops its suction");
            var buffer = (EssentiaBufferEntity) l.getBlockEntity(at(1, 1, 8));
            check(buffer.state.aspects.getAmount(Aspect.AIR) == 1, "buffer pulled the essentia from the tube (" + buffer.state.aspects.visSize() + ")");
            buffer.state.chokedSides[Direction.WEST.get3DDataValue()] = 2;
            check(buffer.getSuctionAmount(Direction.WEST) == 0 && buffer.getSuctionAmount(Direction.EAST) == 1, "buffer choke 2: no suction on that side, 1 elsewhere without bellows");
        });
        if (ticks == 260) srv.execute(() -> { var l = srv.overworld();
            check(amount(l, at(4, 0, 0)) <= lastDest + 1, "closed valve stops the flow (" + lastDest + " -> " + amount(l, at(4, 0, 0)) + ")");
            int oneway = tube(l, at(14, 1, 5)).state.suction;
            System.out.println("TC4_TUBES oneway facing NORTH suction after decay " + oneway);
            check(stage > 0 && oneway == 0, "directional tube passes suction one way only (" + stage + " vs " + oneway + ")");
            l.setBlock(at(2, 1, -1), Blocks.AIR.defaultBlockState(), 3);
        });
        if (ticks == 300) srv.execute(() -> { var l = srv.overworld(); var v = (TubeVariants.Valve) tube(l, at(2, 1, 0)).state; check(v.allowFlow, "power edge off reopens the valve"); lastDest = amount(l, at(4, 0, 0)); });
        if (ticks == 380) srv.execute(() -> { var l = srv.overworld(); check(amount(l, at(4, 0, 0)) > lastDest, "flow resumes after reopening (" + lastDest + " -> " + amount(l, at(4, 0, 0)) + ")"); });
        if (ticks == 400) { System.out.println("TC4_TUBES " + (failures == 0 ? "ALL PASS" : "FAILURES " + failures)); mc.stop(); }
    }
}
