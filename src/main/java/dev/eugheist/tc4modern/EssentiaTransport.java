package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import thaumcraft.api.aspects.IEssentiaTransport;

/** ThaumcraftApiHelper.getConnectableTile for modern block entities implementing the original interface. */
public final class EssentiaTransport {
    private EssentiaTransport() {}
    public static IEssentiaTransport connectable(BlockGetter level, BlockPos pos, Direction face) {
        var te = level.getBlockEntity(pos.relative(face));
        return te instanceof IEssentiaTransport t && t.isConnectable(face.getOpposite()) ? t : null;
    }
    /** Legacy side id (0..5) to Direction; identical ordering to ForgeDirection. */
    public static Direction side(int legacy) { return Direction.from3DDataValue(legacy); }
}
