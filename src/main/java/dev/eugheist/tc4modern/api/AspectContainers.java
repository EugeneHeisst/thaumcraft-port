package dev.eugheist.tc4modern.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * IAspectContainer for addons: the essentia a block holds, independent of the tube faces (IEssentiaTransport is top-face only on
 * jars). Built in: warded/void jars and the essentia reservoir (both ways), alembic, centrifuge, tube buffer and advanced
 * alchemical furnace nozzle (extract only) — the containers Thaumic Energistics' transport permissions named.
 */
public final class AspectContainers {
    private AspectContainers() {}

    public interface Container {
        /** getAspects: a copy of the stored essentia. */
        AspectList getAspects();
        /** Total capacity used for injection (the original transport permission value). */
        int capacity();
        boolean canExtract();
        boolean canInject();
        /** doesContainerAccept plus the jar rule: a filled jar only takes its current aspect. */
        boolean accepts(Aspect aspect);
        /** takeFromContainer(aspect, amount); never partial, returns false when not enough is stored. */
        boolean take(Aspect aspect, int amount);
        /** addToContainer: returns the amount that did not fit (void jars destroy the excess and return 0). */
        int add(Aspect aspect, int amount);
        /** TileJarFillableVoid: destroys whatever does not fit. */
        default boolean voids() { return false; }
        default int stored() { return getAspects().visSize(); }
    }

    @FunctionalInterface public interface Lookup { @Nullable Container find(BlockEntity tile); }

    private static final List<Lookup> LOOKUPS = new CopyOnWriteArrayList<>();
    public static void register(Lookup lookup) { LOOKUPS.add(lookup); }

    @Nullable public static Container find(Level level, BlockPos pos) {
        var tile = level.getBlockEntity(pos);
        return tile == null ? null : find(tile);
    }
    @Nullable public static Container find(BlockEntity tile) {
        for (var lookup : LOOKUPS) { var c = lookup.find(tile); if (c != null) return c; }
        return null;
    }
}
