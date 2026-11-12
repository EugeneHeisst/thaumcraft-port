package thaumcraft.common.lib.research;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original TC4 scanning algorithms; packets and notifications handled by the modern bridge. */
public final class ScanManager {
    public static boolean validScan(AspectList aspects, PlayerKnowledge rp, String username) {
        if (aspects != null && aspects.size() > 0) {
            for (Aspect aspect : aspects.getAspects()) {
                if (aspect == null || aspect.isPrimal() || rp.hasDiscoveredParentAspects(username, aspect)) continue;
                return false;
            }
        } else {
            return false;
        }
        return true;
    }
    public static int checkAndSyncAspectKnowledge(PlayerKnowledge rp, String username, Aspect aspect, int amount, int aspectTotalCap) {
        int save = 0;
        if (!rp.hasDiscoveredAspect(username, aspect)) {
            save = amount += 2;
        }
        if (rp.getAspectPoolFor(username, aspect) >= aspectTotalCap) {
            amount = (int)Math.sqrt(amount);
        }
        if (amount > 1 && (float)rp.getAspectPoolFor(username, aspect) >= (float)aspectTotalCap * 1.25f) {
            amount = 1;
        }
        if (rp.addAspectPool(username, aspect, (short)amount)) {
            save = amount;
        }
        return save;
    }
}
