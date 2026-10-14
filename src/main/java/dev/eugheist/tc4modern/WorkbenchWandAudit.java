package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Wand lying on the arcane worktable (flag workbenchWandAuditOnly, optional workbenchWandReload) via runFocalAudit. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class WorkbenchWandAudit {
    static final BlockPos BASE = new BlockPos(-80, 120, -80), TABLE = BASE.offset(0, 0, 2);
    static int ticks, failures;
    static void check(boolean ok, String what) { if (!ok) failures++; System.out.println("TC4_WORKBENCH " + (ok ? "PASS " : "FAIL ") + what); }
    static ItemStack clientWand(Minecraft mc) { return mc.level.getBlockEntity(TABLE) instanceof ArcaneWorkbenchEntity t ? t.inventory.getItem(9) : ItemStack.EMPTY; }
    static void use(Minecraft mc) { mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(TABLE).add(0, .5, 0), Direction.UP, TABLE, false)); }
    static void view(Minecraft mc) { var srv = mc.getSingleplayerServer(); var id = mc.player.getUUID(); srv.execute(() -> srv.getPlayerList().getPlayer(id).connection.teleport(TABLE.getX() + .5, TABLE.getY() + .4, TABLE.getZ() - 1.3, 0, 40)); }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tc4modern.workbenchWandAuditOnly")) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        var srv = mc.getSingleplayerServer(); var id = mc.player.getUUID();
        if (Boolean.getBoolean("tc4modern.workbenchWandReload")) {
            if (ticks == 40) view(mc);
            if (ticks == 70) { check(WandData.isWand(clientWand(mc)), "after full restart the client knows the wand on the table"); LiquidDeathAudit.shot(mc, "workbench-wand-reload.png"); }
            if (ticks == 80) { System.out.println("TC4_WORKBENCH RELOAD " + (failures == 0 ? "ALL PASS" : "FAILURES " + failures)); mc.stop(); }
            return;
        }
        if (ticks == 40) srv.execute(() -> {
            var p = srv.getPlayerList().getPlayer(id); var level = p.serverLevel(); level.setDayTime(6000);
            for (int x = -3; x <= 3; x++) for (int z = -2; z <= 5; z++) for (int y = -1; y < 3; y++) level.setBlock(BASE.offset(x, y, z), y < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            level.setBlock(TABLE, TC4Modern.TABLE.get().defaultBlockState(), 3);
            p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
            p.getInventory().setItem(0, FociClientAudit.wand(ItemStack.EMPTY)); p.getInventory().selected = 0;
            p.connection.teleport(BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 45);
        });
        if (ticks == 60) use(mc);
        if (ticks == 75) { check(mc.level.getBlockState(TABLE).is(TC4Modern.ARCANE_WORKBENCH.get()), "wand on a table converts it into the arcane worktable"); check(WandData.isWand(clientWand(mc)), "client block entity received the wand in slot 9"); view(mc); }
        if (ticks == 90) LiquidDeathAudit.shot(mc, "workbench-wand-converted.png");
        if (ticks == 95) srv.execute(() -> srv.getPlayerList().getPlayer(id).connection.teleport(BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 45));
        if (ticks == 105) use(mc);
        if (ticks == 115) { check(mc.player.containerMenu instanceof ArcaneWorkbenchMenu, "worktable GUI opened"); mc.gameMode.handleInventoryMouseClick(mc.player.containerMenu.containerId, 1, 0, ClickType.QUICK_MOVE, mc.player); }
        if (ticks == 125) { mc.player.closeContainer(); }
        if (ticks == 135) { check(clientWand(mc).isEmpty(), "taking the wand out empties the table on the client"); view(mc); }
        if (ticks == 150) LiquidDeathAudit.shot(mc, "workbench-wand-removed.png");
        if (ticks == 155) srv.execute(() -> srv.getPlayerList().getPlayer(id).connection.teleport(BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 45));
        if (ticks == 165) use(mc);
        if (ticks == 175) { int slot = -1; var m = mc.player.containerMenu; for (int i = 11; i < m.slots.size(); i++) if (WandData.isWand(m.getSlot(i).getItem())) slot = i; check(slot > 0, "wand in player inventory"); if (slot > 0) mc.gameMode.handleInventoryMouseClick(m.containerId, slot, 0, ClickType.QUICK_MOVE, mc.player); }
        if (ticks == 185) mc.player.closeContainer();
        if (ticks == 195) { check(WandData.isWand(clientWand(mc)), "shift-clicking the wand back shows it on the table"); view(mc); }
        if (ticks == 210) LiquidDeathAudit.shot(mc, "workbench-wand-returned.png");
        if (ticks == 220) { System.out.println("TC4_WORKBENCH " + (failures == 0 ? "ALL PASS" : "FAILURES " + failures)); mc.stop(); }
    }
}
