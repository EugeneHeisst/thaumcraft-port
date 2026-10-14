package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;

/** WarpEvents.suddenlySpiders: up to fifty candidates per spider, without spawn finalization. */
public final class WarpSpiderSpawns {
    private WarpSpiderSpawns() {}
    public static int spawn(ServerPlayer player, int count, boolean real) {
        var level = player.serverLevel();
        var random = level.random;
        int x = Mth.floor(player.getX()), y = Mth.floor(player.getY()), z = Mth.floor(player.getZ());
        int spawned = 0;
        for (int n = 0; n < Math.min(50, count); n++) {
            var spider = new MindSpider(WarpEntities.MIND_SPIDER.get(), level);
            boolean success = false;
            for (int attempt = 0; attempt < 50; attempt++) {
                // The original multiplier is -1, 0 or +1, including zero, on every axis.
                int sx = x + Mth.nextInt(random, 7, 24) * Mth.nextInt(random, -1, 1);
                int sy = y + Mth.nextInt(random, 7, 24) * Mth.nextInt(random, -1, 1);
                int sz = z + Mth.nextInt(random, 7, 24) * Mth.nextInt(random, -1, 1);
                var floor = new BlockPos(sx, sy - 1, sz);
                if (!supportsSpider(level.getBlockState(floor), level, floor)) continue;
                spider.setPos(sx, sy, sz);
                if (!level.isUnobstructed(spider) || !level.noCollision(spider)
                    || level.containsAnyLiquid(spider.getBoundingBox())) continue;
                success = true;
                break;
            }
            if (!success) continue;
            spider.setTarget(player);
            if (!real) {
                spider.setViewer(player.getGameProfile().getName());
                spider.setHarmless(true);
            }
            if (level.addFreshEntity(spider)) spawned++;
        }
        return spawned;
    }
    /** Legacy World.doesBlockHaveSolidTopSurface, including its explicit hopper exception. */
    static boolean supportsSpider(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.isSolidRender(level, pos)) return true;
        if (state.getBlock() instanceof StairBlock) return state.getValue(StairBlock.HALF) == Half.TOP;
        if (state.getBlock() instanceof SlabBlock) return state.getValue(SlabBlock.TYPE) == SlabType.TOP;
        if (state.getBlock() instanceof HopperBlock) return true;
        return state.getBlock() instanceof SnowLayerBlock && state.getValue(SnowLayerBlock.LAYERS) == 8;
    }
}
