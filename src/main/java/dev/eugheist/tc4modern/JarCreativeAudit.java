package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;

/** Real server player destruction path, contents round trip and survival regression. */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class JarCreativeAudit {
    static final BlockPos POS = new BlockPos(-120, 120, -120);
    static int ticks;
    static volatile boolean finished;
    static void check(boolean ok, String text) {
        if (!ok) throw new IllegalStateException("TC4_JAR_CREATIVE FAIL " + text);
        System.out.println("TC4_JAR_CREATIVE PASS " + text);
    }
    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tc4modern.jarCreativeAuditOnly")) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (++ticks == 40) {
            var server = mc.getSingleplayerServer(); var uuid = mc.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(uuid); var level = player.serverLevel();
                var area = new AABB(POS).inflate(8);
                for (var item : level.getEntitiesOfClass(ItemEntity.class, area)) item.discard();
                level.setDayTime(6000);
                for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) for (int y = -1; y < 3; y++)
                    level.setBlock(POS.offset(x,y,z), (y == -1 ? Blocks.STONE : Blocks.AIR).defaultBlockState(), 3);
                player.setGameMode(GameType.CREATIVE);
                level.setBlock(POS, EssentiaDevices.JAR.get().defaultBlockState(), 3);
                var jar = (WardedJarEntity) level.getBlockEntity(POS);
                jar.state.aspect = Aspect.FIRE; jar.state.amount = 37; jar.state.aspectFilter = Aspect.FIRE;
                jar.changed();
                check(player.gameMode.destroyBlock(POS), "creative player destroys filled jar");
                var drops = level.getEntitiesOfClass(ItemEntity.class, area);
                check(drops.size() == 1, "exactly one item drops");
                var stack = drops.getFirst().getItem().copy();
                check(stack.is(EssentiaDevices.JAR.get().asItem()) && stack.getCount() == 1, "dropped item is a single jar");
                check(EssentiaItems.getAspects(stack).getAmount(Aspect.FIRE) == 37, "37 Ignis retained");
                check(NoteData.tag(stack).getString("AspectFilter").equals(Aspect.FIRE.getTag()), "label retained");
                drops.forEach(ItemEntity::discard);
                level.setBlock(POS, EssentiaDevices.JAR.get().defaultBlockState(), 3);
                level.getBlockState(POS).getBlock().setPlacedBy(level, POS, level.getBlockState(POS), player, stack);
                jar = (WardedJarEntity) level.getBlockEntity(POS);
                check(jar.state.amount == 37 && jar.state.aspect == Aspect.FIRE && jar.state.aspectFilter == Aspect.FIRE,
                    "placing the dropped jar restores contents and label");
                player.setGameMode(GameType.SURVIVAL);
                check(player.gameMode.destroyBlock(POS), "survival player destroys the jar");
                drops = level.getEntitiesOfClass(ItemEntity.class, area);
                check(drops.size() == 1 && EssentiaItems.getAspects(drops.getFirst().getItem()).getAmount(Aspect.FIRE) == 37,
                    "survival still drops exactly one filled jar");
                drops.forEach(ItemEntity::discard);
                player.setGameMode(GameType.CREATIVE);
                level.setBlock(POS, EssentiaDevices.JAR.get().defaultBlockState(), 3);
                check(player.gameMode.destroyBlock(POS), "creative player destroys empty jar");
                check(level.getEntitiesOfClass(ItemEntity.class, area).isEmpty(), "empty creative jar does not drop");
                level.setBlock(POS, EssentiaDevices.JAR.get().defaultBlockState(), 3);
                jar = (WardedJarEntity) level.getBlockEntity(POS);
                jar.state.aspect = Aspect.FIRE; jar.state.amount = 37; jar.state.aspectFilter = Aspect.FIRE; jar.changed();
                player.connection.teleport(POS.getX()+.5, POS.getY(), POS.getZ()-2.4, 0, 12);
                System.out.println("TC4_JAR_CREATIVE ALL PASS"); finished = true;
            });
        }
        if (ticks == 80 && finished) LiquidDeathAudit.shot(mc, "jar-creative-preserved.png");
        if (ticks == 100 && finished) mc.stop();
        if (ticks > 300) throw new IllegalStateException("Jar creative audit timed out");
    }
}
