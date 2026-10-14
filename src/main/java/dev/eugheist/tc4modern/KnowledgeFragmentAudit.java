package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Knowledge fragment use and the aspect-gain effect (flag fragmentAuditOnly via runFocalAudit). */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class KnowledgeFragmentAudit {
    static int ticks, failures; static int[] before = new int[6], after = new int[6]; static volatile boolean read;
    static void check(boolean ok, String what) { if (!ok) failures++; System.out.println("TC4_FRAGMENT " + (ok ? "PASS " : "FAIL ") + what); }
    static void pools(Minecraft mc, int[] out) {
        var srv = mc.getSingleplayerServer(); var id = mc.player.getUUID(); read = false;
        srv.execute(() -> { var data = srv.getPlayerList().getPlayer(id).getPersistentData(); int i = 0; for (Aspect a : Aspect.getPrimalAspects()) out[i++] = ResearchKnowledge.points(data, a.getTag()); read = true; });
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tc4modern.fragmentAuditOnly")) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        var srv = mc.getSingleplayerServer(); var id = mc.player.getUUID();
        if (ticks == 40) srv.execute(() -> { var p = srv.getPlayerList().getPlayer(id); p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent(); p.getInventory().setItem(0, new ItemStack(BasicsContent.KNOWLEDGE_FRAGMENT.get(), 3)); p.getInventory().selected = 0; p.connection.teleport(p.getX(), p.getY(), p.getZ(), 0, -10); });
        if (ticks == 50) pools(mc, before);
        if (ticks == 60) { AspectGain.flying.clear(); mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND); }
        if (ticks == 72) { LiquidDeathAudit.shot(mc, "fragment-effect-early.png"); check(AspectGain.flying.size() >= 6 && AspectGain.flying.size() <= 12, "one flying aspect per gained point (" + AspectGain.flying.size() + ")"); }
        if (ticks == 85) LiquidDeathAudit.shot(mc, "fragment-effect-mid.png");
        if (ticks == 90) pools(mc, after);
        if (ticks == 100) {
            check(read, "server pools read");
            boolean ok = true; StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 6; i++) { int d = after[i] - before[i]; sb.append(d).append(' '); if (d < 1 || d > 2) ok = false; }
            check(ok, "every primal gained 1-2 research points: " + sb);
            check(mc.player.getInventory().getItem(0).getCount() == 2, "survival use consumed one fragment");
            var list = ClientKnowledge.notifications.update(System.nanoTime() / 1_000_000L);
            check(list.size() >= 6 && list.stream().filter(e -> e.image() != null).count() >= 6, "six 'Gained N research point(s)' notifications with aspect icons (" + list.size() + ")");
            for (var e : list) System.out.println("TC4_FRAGMENT note " + e.text());
            srv.execute(() -> srv.getPlayerList().getPlayer(id).setGameMode(GameType.CREATIVE));
        }
        if (ticks == 115) mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        if (ticks == 130) { check(mc.player.getInventory().getItem(0).getCount() == 2, "creative use keeps the fragment"); LiquidDeathAudit.shot(mc, "fragment-notifications.png"); }
        if (ticks == 140) { System.out.println("TC4_FRAGMENT " + (failures == 0 ? "ALL PASS" : "FAILURES " + failures)); mc.stop(); }
    }
}
