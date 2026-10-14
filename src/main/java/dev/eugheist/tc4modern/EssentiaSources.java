package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import thaumcraft.api.aspects.Aspect;

/**
 * EssentiaHandler.drainEssentia/getSources: cached IAspectSource lists within a cube of the given range
 * around the drainer (direction UNKNOWN), refreshed on failure with the original 5000 ms delay.
 * Sources are warded jars, essentia reservoirs and mirrors; remote mirror drains exclude mirrors.
 */
public final class EssentiaSources {
    private EssentiaSources() {}
    private static final Map<GlobalPos, List<BlockPos>> SOURCES = new HashMap<>();
    private static final Map<GlobalPos, Long> DELAY = new HashMap<>();
    public static boolean drain(BlockEntity tile, Aspect aspect, int range) {
        return drain(tile,aspect,null,range,false);
    }
    public static boolean drain(BlockEntity tile,Aspect aspect,Direction direction,int range,boolean ignoreMirrors) {
        var level = tile.getLevel();
        var key = GlobalPos.of(level.dimension(), tile.getBlockPos());
        if (!SOURCES.containsKey(key)) {
            getSources(tile, key, direction, range);
            if (SOURCES.containsKey(key)) return drain(tile, aspect, direction,range,ignoreMirrors);
            return false;
        }
        for (BlockPos source : SOURCES.get(key)) {
            var entity=level.getBlockEntity(source);
            boolean success;
            if(entity instanceof WardedJarEntity jar){success=jar.state.takeFromContainer(aspect,1);if(success)jar.changed();}
            else if(entity instanceof EssentiaReservoir.ReservoirEntity reservoir){success=reservoir.takeFromContainer(aspect,1);}
            else if(entity instanceof MagicMirrors.MirrorEntity mirror&&mirror.essentia()){success=!ignoreMirrors&&mirror.take(aspect,1);}
            else if(entity instanceof dev.eugheist.tc4modern.api.AspectSource addon){success=addon.takeFromContainer(aspect,1);}
            else break;
            if(!success)continue;
            var p = tile.getBlockPos();
            InfusionContent.sendFx(level, p, new InfusionContent.FxPayload(0, source.getX(), source.getY(), source.getZ(), p.getX(), p.getY(), p.getZ(), aspect.getColor()));
            return true;
        }
        SOURCES.remove(key);
        DELAY.put(key, System.currentTimeMillis() + 5000L);
        return false;
    }
    private static void getSources(BlockEntity tile, GlobalPos key,Direction direction, int range) {
        Long d = DELAY.get(key);
        if (d != null) { if (d <= System.currentTimeMillis()) DELAY.remove(key); else return; }
        var level = tile.getLevel();
        var origin = tile.getBlockPos();
        List<BlockPos> list = new ArrayList<>();
        int start=direction==null?-range:0;
        if(direction==null)direction=Direction.UP;
        for (int aa = -range; aa <= range; ++aa) for (int bb = -range; bb <= range; ++bb) for (int cc = start; cc < range; ++cc) {
            if (aa == 0 && bb == 0 && cc == 0) continue;
            BlockPos p = direction.getAxis()==Direction.Axis.Y?origin.offset(aa,cc*direction.getStepY(),bb):direction.getAxis()==Direction.Axis.Z?origin.offset(aa,bb,cc*direction.getStepZ()):origin.offset(cc*direction.getStepX(),aa,bb);
            var source=level.getBlockEntity(p);
            if(source instanceof WardedJarEntity||source instanceof EssentiaReservoir.ReservoirEntity||source instanceof dev.eugheist.tc4modern.api.AspectSource)list.add(p);
            else if(source instanceof MagicMirrors.MirrorEntity mirror&&mirror.essentia()&&!(tile instanceof MagicMirrors.MirrorEntity&&mirror.link.equals(origin)&&mirror.linkDimension==level.dimension()))list.add(p);
        }
        if (!list.isEmpty()) SOURCES.put(key, list); else DELAY.put(key, System.currentTimeMillis() + 5000L);
    }
}
