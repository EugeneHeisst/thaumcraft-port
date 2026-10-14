package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/** Opt-in hidden client check: an eldritch ring with banners, ritualist clerics, then cultists fighting the player. */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class CrimsonClientAudit {
    static boolean enabled() { return Boolean.getBoolean("tc4modern.crimsonAudit"); }
    static final BlockPos BASE = new BlockPos(0, 100, 0);

    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!enabled() || !(event.getEntity() instanceof ServerPlayer player)) return;
        var level = player.serverLevel();
        level.getServer().setDifficulty(Difficulty.NORMAL, true);
        player.setGameMode(GameType.CREATIVE);
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, new net.minecraft.world.phys.AABB(BASE).inflate(40), e -> !(e instanceof ServerPlayer))) e.discard();
        // Retry seeds until the altar became a cleric spawner with banners (40% of rings).
        boolean banners = false;
        for (long seed = 0; seed < 50 && !banners; seed++) {
            clear(level);
            boolean placed = CrimsonContent.RING.get().place(NoneFeatureConfiguration.INSTANCE, level, level.getChunkSource().getGenerator(),
                net.minecraft.util.RandomSource.create(seed), BASE.above());
            banners = placed && level.getBlockState(BASE.offset(0, 1, -3)).is(CrimsonContent.BANNER.get());
            System.out.println("TC4_CRIMSON_AUDIT seed=" + seed + " placed=" + placed + " banners=" + banners);
        }
        System.out.println("TC4_CRIMSON_AUDIT altar=" + level.getBlockState(BASE.above()) + " node=" + level.getBlockState(BASE.above(2))
            + " obelisk=" + level.getBlockState(BASE.above(3)) + " body=" + level.getBlockState(BASE.above(7)) + " cap=" + level.getBlockState(BASE.offset(3, 1, 1)));
        player.connection.teleport(BASE.getX() + .5, BASE.getY() + 1, BASE.getZ() - 8.5, 0, -12);
    }

    private static void clear(net.minecraft.server.level.ServerLevel level) {
            for (int x = -14; x <= 14; x++) for (int z = -16; z <= 14; z++) {
            for (int y = -6; y < 0; y++) level.setBlock(BASE.offset(x, y, z), Blocks.STONE.defaultBlockState(), 3);
            level.setBlock(BASE.offset(x, 0, z), Blocks.GRASS_BLOCK.defaultBlockState(), 3);
            for (int y = 1; y < 12; y++) level.setBlock(BASE.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
    static final class Client {
        private static int ticks;
        @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            if (!enabled()) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            ticks++;
            var out = java.nio.file.Path.of("crimson-audit");
            var server = mc.getSingleplayerServer();
            var id = mc.player.getUUID();
            try {
                java.nio.file.Files.createDirectories(out);
                if (ticks == 260) shot(mc, out, "ritual.png");
                if (ticks == 262) server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(id);
                    var level = player.serverLevel();
                    player.setGameMode(GameType.SURVIVAL);
                    player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 2000, 10));
                    player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 2000, 10));
                    var cleric = CrimsonContent.CLERIC.get().create(level);
                    cleric.moveTo(BASE.getX() + 6.5, BASE.getY() + 1, BASE.getZ() - 3.5, 180, 0);
                    cleric.finalizeSpawn(level, level.getCurrentDifficultyAt(cleric.blockPosition()), net.minecraft.world.entity.MobSpawnType.COMMAND, null);
                    level.addFreshEntity(cleric);
                    int clerics = level.getEntitiesOfClass(CultistEntity.Cleric.class, new net.minecraft.world.phys.AABB(BASE).inflate(30)).size();
                    long ritualists = level.getEntitiesOfClass(CultistEntity.Cleric.class, new net.minecraft.world.phys.AABB(BASE).inflate(30), CultistEntity.Cleric::isRitualist).size();
                    System.out.println("TC4_CRIMSON_AUDIT clerics=" + clerics + " ritualists=" + ritualists);
                });
                if (ticks >= 300 && ticks <= 600 && ticks % 4 == 0 && !orbShot) {
                    var orbs = mc.level.getEntitiesOfClass(GolemOrb.class, mc.player.getBoundingBox().inflate(30));
                    if (!orbs.isEmpty()) { orbShot = true; shot(mc, out, "orb.png"); }
                }
                if (ticks == 620) {
                    shot(mc, out, "combat.png");
                    server.execute(() -> {
                        var level = server.overworld();
                        var all = level.getEntitiesOfClass(CultistEntity.class, new net.minecraft.world.phys.AABB(BASE).inflate(30));
                        System.out.println("TC4_CRIMSON_AUDIT cultists=" + all.size() + " knights=" + all.stream().filter(e -> e instanceof CultistEntity.Knight).count()
                            + " targeting=" + all.stream().filter(e -> e.getTarget() != null).count() + " health=" + server.getPlayerList().getPlayer(id).getHealth());
                    });
                }
                if (ticks == 640) mc.stop();
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        private static boolean orbShot;
        private static void shot(net.minecraft.client.Minecraft mc, java.nio.file.Path out, String name) throws java.io.IOException {
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(out.resolve(name)); }
            System.out.println("TC4_CRIMSON_AUDIT " + name);
        }
    }
}
