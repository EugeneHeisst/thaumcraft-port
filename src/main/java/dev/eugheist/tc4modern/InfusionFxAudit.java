package dev.eugheist.tc4modern;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.common.tiles.infusion.InfusionMatrixLogic;

/**
 * Native infusion polish check (flag infusionFxAuditOnly, run through runFocalAudit): jar bubbles, item suction,
 * finish sparkles and cue on a real craft, matrix overlay old/new, particles around the pillars old/new,
 * and the actual consequences of every instability outcome plus a natural high-instability run.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class InfusionFxAudit {
    static final BlockPos POS = InfusionAudit.POS;
    static int ticks, finishTick = -1, stressTick = -1, phaseTick = -1, wandHeard, trailsAtFinish, bitsAtFinish, sparklesAtFinish;
    static volatile boolean serverFinished, instabilityDone, naturalDone;
    static volatile String naturalReport = "";
    static volatile boolean naturalAny;
    static int[] legacy, fixed;
    static boolean listening;

    static void check(boolean ok, String what) { if (!ok) throw new IllegalStateException("TC4_INFUSION_FX FAIL " + what); System.out.println("TC4_INFUSION_FX PASS " + what); }
    static void log(String s) { System.out.println("TC4_INFUSION_FX " + s); }
    static void look(ServerPlayer p, Vec3 eye, Vec3 at) {
        double dx = at.x - eye.x, dy = at.y - eye.y, dz = at.z - eye.z;
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        p.connection.teleport(eye.x, eye.y - p.getEyeHeight(), eye.z, yaw, pitch);
    }
    static Object call(String name, Class<?> type, Object arg, InfusionContent.MatrixEntity m) {
        try { Method method = InfusionMatrixLogic.class.getDeclaredMethod(name, InfusionMatrixLogic.Host.class, type); method.setAccessible(true); return method.invoke(m.logic, m, arg); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    static void warp(InfusionContent.MatrixEntity m) {
        try { Method method = InfusionMatrixLogic.class.getDeclaredMethod("inEvWarp", InfusionMatrixLogic.Host.class); method.setAccessible(true); method.invoke(m.logic, m); }
        catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    /** Every outcome of the 21-way switch invoked on the real matrix host, checking what actually changed in the world. */
    static void instability(ServerPlayer p) {
        var level = p.serverLevel();
        var m = (InfusionContent.MatrixEntity) level.getBlockEntity(POS);
        m.logic.getSurroundings(m);
        var husks = new ArrayList<Husk>();
        for (var off : List.of(new BlockPos(2, -2, -2), new BlockPos(-2, -2, 2))) {
            var h = EntityType.HUSK.create(level); h.moveTo(POS.getX() + off.getX() + .5, POS.getY() + off.getY(), POS.getZ() + off.getZ() + .5, 0, 0);
            h.setNoAi(true); h.setPersistenceRequired(); level.addFreshEntity(h); husks.add(h);
        }
        p.setGameMode(GameType.SURVIVAL);
        p.teleportTo(POS.getX() + .5, POS.getY() - 2, POS.getZ() - 14.5); // outside the 10-block AABB
        String[] names = {"eject drop", "eject drop + flux goo", "eject drop + flux gas", "delete + flux goo", "delete + flux gas", "eject drop + explosion"};
        for (int type = 0; type < 6; type++) {
            for (var c : InfusionAudit.components) { ((InfusionContent.PedestalEntity) level.getBlockEntity(c)).setItem(new ItemStack(InfusionContent.SALIS.get())); level.setBlock(c.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3); }
            for (var e : level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(12))) e.discard();
            call("inEvEjectItem", int.class, type, m);
            BlockPos hit = null; int emptied = 0;
            for (var c : InfusionAudit.components) if (((InfusionContent.PedestalEntity) level.getBlockEntity(c)).item().isEmpty()) { emptied++; hit = c; }
            check(emptied == 1, "instability " + names[type] + ": exactly one pedestal emptied");
            int drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(hit).inflate(2), e -> e.getItem().is(InfusionContent.SALIS.get())).size();
            boolean dropExpected = type < 3 || type == 5;
            check(drops == (dropExpected ? 1 : 0), "instability " + names[type] + ": item " + (dropExpected ? "dropped as entity" : "destroyed"));
            var above = level.getBlockState(hit.above());
            if (type == 1 || type == 3) check(above.is(TC4Modern.FLUX_GOO.get()), "instability " + names[type] + ": flux goo above pedestal");
            else if (type == 2 || type == 4) check(above.is(TC4Modern.FLUX_GAS.get()), "instability " + names[type] + ": flux gas above pedestal");
            else check(!above.is(TC4Modern.FLUX_GOO.get()) && !above.is(TC4Modern.FLUX_GAS.get()), "instability " + names[type] + ": no flux");
            check(level.getBlockEntity(hit) instanceof InfusionContent.PedestalEntity, "instability " + names[type] + ": pedestal block intact (no terrain damage)");
            level.setBlock(hit.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        }
        for (var e : level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(12))) e.discard();
        for (boolean all : new boolean[]{false, true}) {
            for (var h : husks) { h.setHealth(h.getMaxHealth()); h.invulnerableTime = 0; }
            call("inEvZap", boolean.class, all, m);
            long hurt = husks.stream().filter(h -> h.getHealth() < h.getMaxHealth()).count();
            float lost = husks.stream().map(h -> h.getMaxHealth() - h.getHealth()).filter(d -> d > 0).findFirst().orElse(0f);
            check(hurt == (all ? 2 : 1) && lost >= 4 && lost <= 7, "instability zap" + (all ? " all" : "") + ": " + hurt + " target(s) lost " + lost + " hp (original 4..7 magic)");
        }
        for (boolean all : new boolean[]{false, true}) {
            for (var h : husks) h.removeAllEffects();
            call("inEvHarm", boolean.class, all, m);
            long affected = husks.stream().filter(h -> h.hasEffect(TC4Modern.FLUX_TAINT) || h.hasEffect(TC4Modern.VIS_EXHAUST)).count();
            check(affected == (all ? 2 : 1), "instability harm" + (all ? " all" : "") + ": " + affected + " target(s) got flux taint / vis exhaust");
        }
        p.teleportTo(POS.getX() + 3.5, POS.getY() - 2, POS.getZ() + .5);
        int before = PlayerWarp.load(p).total();
        warp(m);
        var w = PlayerWarp.load(p);
        check(w.total() > before, "instability warp: player warp " + before + " -> " + w.total() + " (sticky " + w.get(WarpState.Kind.STICKY) + ", temporary " + w.get(WarpState.Kind.TEMPORARY) + ")");
        float hp = husks.get(0).getHealth();
        p.setGameMode(GameType.CREATIVE);
        for (var h : husks) { h.setHealth(h.getMaxHealth()); h.invulnerableTime = 0; }
        level.explode(null, POS.getX() + .5, POS.getY() + .5, POS.getZ() + .5, 2f, net.minecraft.world.level.Level.ExplosionInteraction.NONE);
        check(level.getBlockEntity(POS) instanceof InfusionContent.MatrixEntity && level.getBlockState(POS.offset(1, -1, 1)).is(InfusionContent.PILLAR_TOP.get()), "instability explosion (case 9, no block damage): altar intact");
        log("husk hp before explosion reset " + hp);
        instabilityDone = true;
    }

    /** A real craft forced to instability 500: every craft cycle rolls the original switch. */
    static void natural(ServerPlayer p) {
        var level = p.serverLevel();
        var m = (InfusionContent.MatrixEntity) level.getBlockEntity(POS);
        for (var c : InfusionAudit.components) ((InfusionContent.PedestalEntity) level.getBlockEntity(c)).setItem(new ItemStack(InfusionContent.SALIS.get()));
        ((InfusionContent.PedestalEntity) level.getBlockEntity(POS.below(2))).setItem(new ItemStack(WandParts.THAUMIUM_CAP_INERT.get()));
        for (var h : level.getEntitiesOfClass(Husk.class, new AABB(POS).inflate(12))) { h.setHealth(h.getMaxHealth()); h.removeAllEffects(); }
        p.setGameMode(GameType.CREATIVE);
        PlayerWarp.save(p, new WarpState());
        var wand = p.getMainHandItem();
        for (var a : thaumcraft.api.aspects.Aspect.getPrimalAspects()) WandData.add(wand, a, 50, true);
        InfusionAudit.click(p, wand);
        check(m.logic.crafting, "natural run: real wand click starts the craft");
        m.logic.instability = 500;
    }
    static String naturalSummary(ServerPlayer p) {
        var level = p.serverLevel();
        var m = (InfusionContent.MatrixEntity) level.getBlockEntity(POS);
        int drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(POS).inflate(12)).size(), goo = 0, gas = 0, empty = 0;
        for (var c : InfusionAudit.components) {
            if (level.getBlockState(c.above()).is(TC4Modern.FLUX_GOO.get())) goo++;
            if (level.getBlockState(c.above()).is(TC4Modern.FLUX_GAS.get())) gas++;
            if (((InfusionContent.PedestalEntity) level.getBlockEntity(c)).item().isEmpty()) empty++;
        }
        var husks = level.getEntitiesOfClass(Husk.class, new AABB(POS).inflate(12));
        long hurt = husks.stream().filter(h -> h.getHealth() < h.getMaxHealth()).count(), harmed = husks.stream().filter(h -> h.hasEffect(TC4Modern.FLUX_TAINT) || h.hasEffect(TC4Modern.VIS_EXHAUST)).count();
        naturalAny = empty > 0 || drops > 0 || goo + gas > 0 || hurt > 0 || harmed > 0 || PlayerWarp.load(p).total() > 0;
        return "crafting=" + m.logic.crafting + " emptiedPedestals=" + empty + " itemEntities=" + drops + " goo=" + goo + " gas=" + gas + " hurtHusks=" + hurt + "/" + husks.size() + " harmedHusks=" + harmed + " playerWarp=" + PlayerWarp.load(p).total()
                + " centre=" + ((InfusionContent.PedestalEntity) level.getBlockEntity(POS.below(2))).item();
    }

    /** Item bits launched from behind the four pillars: the previous vanilla ItemParticle approximation, or the BoreBits port. */
    static void stress(Minecraft mc, boolean legacy) {
        InfusionFx.boreStuck = InfusionFx.boreReached = InfusionFx.boreInsideAtDeath = InfusionFx.boreLingering = 0;
        TRACK.clear(); legacyStuck = legacyLingering = 0;
        var level = mc.level; var r = level.random;
        var target = new Vec3(POS.getX() + .5, POS.getY() - .5, POS.getZ() + .5);
        var stack = new ItemStack(InfusionContent.SALIS.get());
        for (int i = 0; i < 160; i++) {
            int sx = i % 2 == 0 ? 1 : -1, sz = i / 2 % 2 == 0 ? 1 : -1;
            var start = new Vec3(POS.getX() + .5 + sx * (2.2 + r.nextFloat() * .4), POS.getY() - 2 + .55 + r.nextFloat() * .9, POS.getZ() + .5 + sz * (2.2 + r.nextFloat() * .4));
            // 0.17.58 InfusionFx: vanilla item particle, velocity (target - start) / 12 (+0.1 up), vanilla gravity and move latch.
            if (legacy) level.addParticle(new net.minecraft.core.particles.ItemParticleOption(net.minecraft.core.particles.ParticleTypes.ITEM, stack), start.x, start.y, start.z, (target.x - start.x) / 12, (POS.getY() - start.y) / 12 + .1, (target.z - start.z) / 12);
            else mc.particleEngine.add(InfusionFx.BoreBits.item(level, start, target, stack));
        }
    }
    static final java.util.IdentityHashMap<net.minecraft.client.particle.Particle, double[]> TRACK = new java.util.IdentityHashMap<>();
    static int legacyStuck, legacyLingering;
    /** Samples the live vanilla item particles (audit only) for frozen bits and bits sitting in non-air blocks. */
    @SuppressWarnings("unchecked")
    static void sampleLegacy(Minecraft mc) {
        try {
            var f = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles"); f.setAccessible(true);
            var map = (java.util.Map<net.minecraft.client.particle.ParticleRenderType, java.util.Queue<net.minecraft.client.particle.Particle>>) f.get(mc.particleEngine);
            for (var q : map.values()) for (var p : q) {
                if (!(p instanceof net.minecraft.client.particle.BreakingItemParticle)) continue;
                var c = p.getBoundingBox().getCenter();
                var old = TRACK.get(p);
                if (old == null) { TRACK.put(p, new double[]{c.x, c.y, c.z, 0, 0}); continue; }
                if (Math.abs(c.x - old[0]) + Math.abs(c.y - old[1]) + Math.abs(c.z - old[2]) < 1e-4 && ++old[3] == 4) legacyStuck++;
                if (!mc.level.getBlockState(BlockPos.containing(c)).isAir() && ++old[4] == 6) legacyLingering++;
                old[0] = c.x; old[1] = c.y; old[2] = c.z;
            }
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tc4modern.infusionFxAuditOnly")) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        var server = mc.getSingleplayerServer(); var id = mc.player.getUUID();
        if (!listening) {
            listening = true;
            mc.getSoundManager().addListener((sound, events, range) -> { if (sound.getLocation().equals(AlchemyContent.id("wand"))) wandHeard++; });
        }
        if (ticks == 80) server.execute(() -> { var p = server.getPlayerList().getPlayer(id); InfusionAudit.prepare(p); look(p, new Vec3(POS.getX() + 4.2, POS.getY() - .3, POS.getZ() - 2.6), new Vec3(POS.getX() + .2, POS.getY() - 1, POS.getZ() - 1.8)); });
        if (ticks == 120) check(InfusionAudit.prepared, "real altar + three-pedestal cap recipe started by wand");
        if (firstTrail < 0 && InfusionFx.trailsSpawned > 0) firstTrail = ticks;
        if (firstTrail > 0 && (ticks == firstTrail + 8 || ticks == firstTrail + 20)) { log("live essentia trails " + live(mc, InfusionFx.EssentiaTrail.class) + " spawned " + InfusionFx.trailsSpawned); LiquidDeathAudit.shot(mc, "infusion-fx-essentia-" + (ticks - firstTrail) + ".png"); }
        if (Boolean.getBoolean("tc4modern.infusionFxEssentiaOnly") && firstTrail > 0 && ticks == firstTrail + 24) mc.stop();
        if (firstTrail > 0 && ticks == firstTrail + 25) server.execute(() -> look(server.getPlayerList().getPlayer(id), new Vec3(POS.getX() + 4.5, POS.getY() + .2, POS.getZ() - 3.5), new Vec3(POS.getX() + .5, POS.getY() - .8, POS.getZ() + .5)));
        if (ticks == 700) check(firstTrail > 0, "essentia drain started");
        if (ticks > 240 && ticks % 40 == 0 && finishTick < 0 && ticks < 1400) {
            if (ticks % 80 == 0) LiquidDeathAudit.shot(mc, "infusion-fx-crafting-" + ticks + ".png");
            server.execute(() -> { var level = server.overworld(); var m = (InfusionContent.MatrixEntity) level.getBlockEntity(POS); var centre = (InfusionContent.PedestalEntity) level.getBlockEntity(POS.below(2)); if (!m.logic.crafting && centre.item().is(WandParts.THAUMIUM_CAP.get())) serverFinished = true; });
        }
        if (finishTick < 0 && InfusionFx.finishSparkles > 0) {
            finishTick = ticks;
            trailsAtFinish = InfusionFx.trailsSpawned; bitsAtFinish = InfusionFx.itemBitsSpawned; sparklesAtFinish = InfusionFx.sparklesSpawned;
        }
        if (finishTick > 0 && ticks == finishTick + 3) LiquidDeathAudit.shot(mc, "infusion-fx-finish.png");
        if (finishTick > 0 && ticks == finishTick + 10) server.execute(() -> look(server.getPlayerList().getPlayer(id), new Vec3(POS.getX() + .5, POS.getY() + .6, POS.getZ() - 2.2), new Vec3(POS.getX() + .5, POS.getY() + .5, POS.getZ() + .5)));
        if (finishTick > 0 && ticks == finishTick + 16) InfusionRenderers.auditLegacyGlow = true;
        if (finishTick > 0 && ticks == finishTick + 20) LiquidDeathAudit.shot(mc, "infusion-fx-matrix-old-one-one.png");
        if (finishTick > 0 && ticks == finishTick + 22) InfusionRenderers.auditLegacyGlow = false;
        if (finishTick > 0 && ticks == finishTick + 26) LiquidDeathAudit.shot(mc, "infusion-fx-matrix-new-src-alpha.png");
        if (finishTick > 0 && ticks == finishTick + 45) {
            check(trailsAtFinish > 50, "FXEssentiaTrail bubbles from both jars during the craft (" + trailsAtFinish + ")");
            check(bitsAtFinish > 20, "FXBoreParticles item bits sucked from the pedestals (" + bitsAtFinish + ")");
            check(sparklesAtFinish > 5, "FXBoreSparkle purple pedestal sparkles (" + sparklesAtFinish + ")");
            check(InfusionFx.finishSparkles >= 10, "pedestal event 12: random-colour blockSparkle burst (" + InfusionFx.finishSparkles + ")");
            check(wandHeard > 0, "completion cue played on the client (" + wandHeard + ")");
            server.execute(() -> { var level = server.overworld(); var centre = (InfusionContent.PedestalEntity) level.getBlockEntity(POS.below(2)); check(centre.item().is(WandParts.THAUMIUM_CAP.get()), "real craft produced the thaumium cap"); look(server.getPlayerList().getPlayer(id), new Vec3(POS.getX() + 3.6, POS.getY() - .6, POS.getZ() - .9), new Vec3(POS.getX() + .5, POS.getY() - 1.2, POS.getZ() + .5)); });
            stressTick = ticks + 15;
        }
        if (stressTick > 0) {
            int t = ticks - stressTick;
            if (t == 0) stress(mc, true);
            if (t > 0 && t < 45) sampleLegacy(mc);
            if (t == 12) LiquidDeathAudit.shot(mc, "infusion-fx-pillars-old.png");
            if (t == 45) { legacy = new int[]{legacyStuck, legacyLingering}; stress(mc, false); }
            if (t == 52) LiquidDeathAudit.shot(mc, "infusion-fx-pillars-new.png");
            if (t == 90) {
                fixed = new int[]{InfusionFx.boreStuck, InfusionFx.boreLingering, InfusionFx.boreReached, InfusionFx.boreInsideAtDeath};
                log("pillar stress old vanilla bits frozen/lingering-in-blocks = " + legacy[0] + "/" + legacy[1] + "; new frozen/lingering/reached/diedInBlock = " + fixed[0] + "/" + fixed[1] + "/" + fixed[2] + "/" + fixed[3]);
                check(legacy[0] + legacy[1] > 20, "old vanilla item particles reproduce bits frozen on/in the pillars");
                check(fixed[0] <= 2 && fixed[1] <= 4, "new BoreBits: no frozen bits and none lingering inside blocks");
                check(fixed[2] >= 60, "new BoreBits: bits actually reach the matrix target");
                // XP drain (kind 4) and block-item branch (drawInfusionParticles2) on the client handler.
                int s0 = InfusionFx.sparklesSpawned;
                InfusionFx.handle(new InfusionContent.FxPayload(4, POS.getX(), POS.getY(), POS.getZ(), POS.getX(), POS.getY(), POS.getZ(), mc.player.getId()));
                server.execute(() -> { var level = server.overworld(); ((InfusionContent.PedestalEntity) level.getBlockEntity(InfusionAudit.components.get(0))).setItem(new ItemStack(Items.REDSTONE_BLOCK)); });
                phaseTick = ticks;
                s0Sparkles = s0;
            }
        }
        if (phaseTick > 0) {
            int t = ticks - phaseTick;
            if (t == 10) {
                check(InfusionFx.sparklesSpawned > s0Sparkles, "XP drain: green FXBoreSparkle from the player");
                bits0 = InfusionFx.itemBitsSpawned;
                var c = InfusionAudit.components.get(0);
                InfusionFx.handle(new InfusionContent.FxPayload(1, c.getX(), c.getY(), c.getZ(), POS.getX(), POS.getY(), POS.getZ(), 0));
            }
            if (t == 16) LiquidDeathAudit.shot(mc, "infusion-fx-block-bits.png");
            if (t == 30) {
                check(InfusionFx.itemBitsSpawned > bits0, "block ingredient: FXBoreParticles with the block sprite");
                server.execute(() -> instability(server.getPlayerList().getPlayer(id)));
            }
            if (t == 50) { check(instabilityDone, "all instability outcomes verified"); server.execute(() -> { var p = server.getPlayerList().getPlayer(id); natural(p); look(p, new Vec3(POS.getX() + 5.5, POS.getY() + 1, POS.getZ() - 4.5), new Vec3(POS.getX() + .5, POS.getY() - 1, POS.getZ() + .5)); }); }
            if (t == 120 || t == 220) LiquidDeathAudit.shot(mc, "infusion-fx-unstable-" + t + ".png");
            if (t == 400) server.execute(() -> { naturalReport = naturalSummary(server.getPlayerList().getPlayer(id)); naturalDone = true; });
            if (t == 410) {
                check(naturalDone, "natural summary collected");
                log("natural instability500 run: " + naturalReport);
                check(naturalAny, "natural run produced real instability consequences");
                System.out.println("TC4_INFUSION_FX ALL PASS");
                mc.stop();
            }
        }
    }
    static int s0Sparkles, bits0, firstTrail = -1;
    @SuppressWarnings("unchecked")
    static int live(Minecraft mc, Class<?> type) {
        try {
            var f = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles"); f.setAccessible(true);
            int n = 0; for (var q : ((java.util.Map<Object, java.util.Queue<Object>>) f.get(mc.particleEngine)).values()) for (var p : q) if (type.isInstance(p)) { if (n++ < 4) log("  " + type.getSimpleName() + " at " + ((net.minecraft.client.particle.Particle) p).getBoundingBox().getCenter() + " rel " + ((net.minecraft.client.particle.Particle) p).getBoundingBox().getCenter().subtract(POS.getX(), POS.getY(), POS.getZ())); }
            return n;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
