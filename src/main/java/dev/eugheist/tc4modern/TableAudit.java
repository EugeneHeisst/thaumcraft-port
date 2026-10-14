package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Tables (flag tableAuditOnly via runFocalAudit): yaw-based orientation, two-table research structure, breaking back into tables. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class TableAudit {
    static final BlockPos BASE = new BlockPos(-60, 120, -60);
    static int ticks, failures, digTicks; static volatile String server = "";
    static void check(boolean ok, String what) { if (!ok) failures++; System.out.println("TC4_TABLE " + (ok ? "PASS " : "FAIL ") + what); }
    static void place(Minecraft mc, BlockPos on, float yaw) {
        mc.player.setYRot(yaw); mc.player.setXRot(60); mc.player.yRotO = yaw;
        mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.Rot(yaw, 60, true));
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(on).add(0, .5, 0), Direction.UP, on, false));
    }
    static void dig(Minecraft mc, BlockPos pos) {
        mc.gameMode.startDestroyBlock(pos, Direction.UP);
        for (int i = 0; i < 1; i++) mc.gameMode.continueDestroyBlock(pos, Direction.UP);
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tc4modern.tableAuditOnly")) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        var srv = mc.getSingleplayerServer(); var id = mc.player.getUUID();
        BlockPos a = BASE.offset(0, 0, 2), b = BASE.offset(1, 0, 2), c = BASE.offset(-2, 0, 2), d = BASE.offset(-3, 0, 2);
        if (ticks == 40) {
            check(!BuiltInRegistries.ITEM.containsKey(AlchemyContent.id("research_table")), "no separate research table item");
            srv.execute(() -> {
                var p = srv.getPlayerList().getPlayer(id); var level = p.serverLevel(); level.setDayTime(6000);
                for (int x = -6; x <= 6; x++) for (int z = -3; z <= 6; z++) for (int y = -1; y < 4; y++) level.setBlock(BASE.offset(x, y, z), y < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
                for (var e : level.getEntitiesOfClass(ItemEntity.class, new AABB(BASE).inflate(12))) e.discard();
                p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
                p.getInventory().setItem(0, new ItemStack(TC4Modern.TABLE_ITEM.get(), 8)); p.getInventory().selected = 0;
                p.connection.teleport(BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 60);
            });
        }
        if (ticks == 60) place(mc, a.below(), 0);      // facing south
        if (ticks == 66) place(mc, b.below(), 0);
        if (ticks == 72) place(mc, c.below(), -90);    // facing east
        if (ticks == 78) place(mc, d.below(), 90);     // facing west
        if (ticks == 95) {
            check(mc.level.getBlockState(a).is(TC4Modern.TABLE.get()) && mc.level.getBlockState(a).getValue(TableBlock.AXIS) == Direction.Axis.Z, "table placed facing south keeps metadata 0 orientation");
            check(mc.level.getBlockState(c).is(TC4Modern.TABLE.get()) && mc.level.getBlockState(c).getValue(TableBlock.AXIS) == Direction.Axis.X, "table placed facing east turns 90 degrees (metadata 1)");
            check(mc.level.getBlockState(d).getValue(TableBlock.AXIS) == Direction.Axis.X, "table placed facing west turns 90 degrees (metadata 1)");
            srv.execute(() -> srv.getPlayerList().getPlayer(id).connection.teleport(BASE.getX() - .8, BASE.getY() + 1.2, BASE.getZ() - 1.6, 0, 35));
        }
        if (ticks == 105) LiquidDeathAudit.shot(mc, "tables-orientation.png");
        if (ticks == 110) srv.execute(() -> { var p = srv.getPlayerList().getPlayer(id); p.getInventory().setItem(0, new ItemStack(BuiltInRegistries.ITEM.get(AlchemyContent.id("scribing_tools")))); p.connection.teleport(BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 60); });
        if (ticks == 125) place(mc, a, 0);
        if (ticks == 140) {
            var sa = mc.level.getBlockState(a); var sb = mc.level.getBlockState(b);
            check(sa.is(TC4Modern.RESEARCH_TABLE.get()) && sb.is(TC4Modern.RESEARCH_TABLE.get()) && !sa.getValue(ResearchTableBlock.SECOND) && sb.getValue(ResearchTableBlock.SECOND), "scribing tools join two tables into the research table");
            srv.execute(() -> { var p = srv.getPlayerList().getPlayer(id); p.getInventory().setItem(0, new ItemStack(Items.IRON_AXE)); p.connection.teleport(BASE.getX() - .8, BASE.getY() + 1.2, BASE.getZ() - 1.6, 0, 35); });
        }
        if (ticks == 150) { LiquidDeathAudit.shot(mc, "tables-research.png"); srv.execute(() -> { var p = srv.getPlayerList().getPlayer(id); var t = srv.overworld().getBlockEntity(a); System.out.println("TC4_TABLE before break inv " + (t instanceof ResearchTableEntity r ? r.inventory.getItem(0) + "/" + r.inventory.getItem(1) : "none") + " hand " + p.getInventory().getItem(0)); }); }
        if (ticks == 155) { srv.execute(() -> srv.getPlayerList().getPlayer(id).connection.teleport(BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 60)); }
        if (ticks == 160) { mc.gameMode.startDestroyBlock(b, Direction.UP); digTicks = 0; }
        if (ticks > 160 && ticks < 230 && !mc.level.getBlockState(b).isAir() && mc.level.getBlockState(b).is(TC4Modern.RESEARCH_TABLE.get())) mc.gameMode.continueDestroyBlock(b, Direction.UP);
        if (ticks == 240) srv.execute(() -> {
            var level = srv.overworld(); var p = srv.getPlayerList().getPlayer(id);
            int tables = 0, tools = 0;
            for (var e : level.getEntitiesOfClass(ItemEntity.class, new AABB(a).inflate(4))) { if (e.getItem().is(TC4Modern.TABLE_ITEM.get())) tables += e.getItem().getCount(); if (e.getItem().is(BuiltInRegistries.ITEM.get(AlchemyContent.id("scribing_tools")))) tools++; }
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) { var st = p.getInventory().getItem(i); if (st.is(TC4Modern.TABLE_ITEM.get())) tables += st.getCount() - (i == 0 ? 0 : 0); if (st.is(BuiltInRegistries.ITEM.get(AlchemyContent.id("scribing_tools")))) tools++; }
            var detail = new StringBuilder();
            for (var e : level.getEntitiesOfClass(ItemEntity.class, new AABB(a).inflate(4))) detail.append(" entity:").append(e.getItem());
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) if (!p.getInventory().getItem(i).isEmpty()) detail.append(" inv").append(i).append(':').append(p.getInventory().getItem(i));
            System.out.println("TC4_TABLE detail" + detail);
            server = "brokenHalf=" + level.getBlockState(b) + " survivor=" + level.getBlockState(a) + " droppedTables=" + tables + " tools=" + tools;
        });
        if (ticks == 250) {
            System.out.println("TC4_TABLE " + server);
            check(server.contains("brokenHalf=Block{minecraft:air}") && server.contains("survivor=Block{tc4modern:table}"), "breaking one half leaves the other as a plain table");
            check(server.contains("droppedTables=1") && server.contains("tools=1"), "broken half drops a table and the scribing tools come back");
            LiquidDeathAudit.shot(mc, "tables-after-break.png");
            System.out.println("TC4_TABLE " + (failures == 0 ? "ALL PASS" : "FAILURES " + failures));
            mc.stop();
        }
    }
}
