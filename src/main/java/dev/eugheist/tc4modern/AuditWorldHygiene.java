package dev.eugheist.tc4modern;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * The audit world is persistent: golems, drops and bobbers of earlier (or interrupted) audit runs are saved with their chunks
 * and load back after a fixture's cleanup, stealing items or merging with test stacks. While any audit flag is set,
 * entities restored from disk (other than players) are refused so every run starts from its own fixtures only.
 */
@EventBusSubscriber(modid=TC4Modern.MODID)
final class AuditWorldHygiene {
    static Boolean active;static int refused;
    static boolean active(){if(active==null)active=System.getProperties().stringPropertyNames().stream().anyMatch(k->k.startsWith("tc4modern.")&&(k.endsWith("AuditOnly")||k.endsWith("ShowOnly"))&&Boolean.getBoolean(k));return active;}
    @SubscribeEvent static void join(EntityJoinLevelEvent e){if(e.getLevel().isClientSide()||!e.loadedFromDisk()||e.getEntity() instanceof Player||!active())return;e.setCanceled(true);if(refused++<20)System.out.println("TC4_AUDIT_HYGIENE refused stale "+e.getEntity().getType()+" at "+e.getEntity().blockPosition());}
}
