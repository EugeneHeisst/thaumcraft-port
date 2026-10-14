package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Survival block behaviour against the original (flag blockBehaviourAuditOnly via runFocalAudit): real client digging with
 * the given tool, heard hit/break sounds, digging particle sprites, drops, and step sounds while actually walking.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
public final class BlockBehaviourAudit {
    record Case(String block, String tool, String sound, String sprite, boolean drops, boolean particles, java.util.function.Function<BlockState, BlockState> variant) {
        Case(String block, String tool, String sound, String sprite, boolean drops) { this(block, tool, sound, sprite, drops, true, s -> s); }
    }
    static final List<Case> CASES = List.of(
        new Case("warded_jar", "", "tc4modern:jar", "jar_side", true, false, s -> s),
        new Case("brain_jar", "", "tc4modern:jar", "jar_side", true, false, s -> s),
        new Case("air_crystal_cluster", "", "tc4modern:crystal", "crystal", true),
        new Case("mirror", "", "tc4modern:jar", "mirrorframe", true),
        new Case("taint", "", "tc4modern:gore", "taint_crust", false), // original BlockTaint metadata 0 drops nothing
        new Case("taint", "", "tc4modern:gore", "fleshblock", true, true, s -> s.setValue(TaintBlock.VARIANT, 2)),
        new Case("crucible", "iron_pickaxe", "minecraft:block.metal", "metalbase", true),
        new Case("crucible", "", "minecraft:block.metal", "metalbase", false),
        new Case("essentia_tube", "iron_pickaxe", "minecraft:block.metal", "pipe_1", true),
        new Case("arcane_lamp", "iron_pickaxe", "minecraft:block.metal", "lamp_side", true),
        new Case("vis_relay", "iron_pickaxe", "minecraft:block.metal", "metalbase", true),
        new Case("node_stabilizer", "iron_pickaxe", "minecraft:block.stone", "pedestal_top", true),
        new Case("research_table", "iron_axe", "minecraft:block.wood", "woodplain", true),
        new Case("deconstructor", "iron_axe", "minecraft:block.wood", "woodplain", true),
        new Case("bellows", "iron_axe", "minecraft:block.wood", "woodplain", true),
        new Case("thaumium_block", "iron_pickaxe", "minecraft:block.stone", "thaumiumblock", true),
        new Case("arcane_stone", "iron_pickaxe", "minecraft:block.stone", "pedestal_top", true),
        new Case("amber_block", "iron_pickaxe", "minecraft:block.stone", "amberblock", true),
        new Case("amber_block", "", "minecraft:block.stone", "amberblock", false),
        new Case("cinnabar_ore", "iron_pickaxe", "minecraft:block.stone", "cinnibar", true),
        new Case("cinnabar_ore", "stone_pickaxe", "minecraft:block.stone", "cinnibar", false),
        new Case("air_infused_stone", "iron_pickaxe", "minecraft:block.stone", "infusedorestone", true),
        new Case("arcane_spa", "iron_pickaxe", "minecraft:block.stone", "spa_side", true),
        new Case("tallow_block", "iron_pickaxe", "minecraft:block.stone", "tallowblock", true));
    static final String[] WALK = {"warded_jar", "taint", "thaumium_block"};
    static final BlockPos BASE = new BlockPos(-40, 120, -40), TARGET = BASE.offset(0, 0, 2);
    static int ticks, index = -1, phase, phaseTick, digTicks, walk = -1, failures, sparkles0;
    static final List<String> heard = Collections.synchronizedList(new ArrayList<>());
    static final Set<String> sprites = new TreeSet<>();
    static final Set<Object> old = Collections.newSetFromMap(new IdentityHashMap<>());
    static int breakTick = -1, burstNew;
    static volatile int drops = -1;
    static boolean listening;
    static final List<String> report = new ArrayList<>();

    static void log(String s) { System.out.println("TC4_BLOCKS " + s); report.add(s); }
    static void check(boolean ok, String what) { if (!ok) failures++; log((ok ? "PASS " : "FAIL ") + what); }
    static BlockState state(Case c) { return c.variant.apply(BuiltInRegistries.BLOCK.get(AlchemyContent.id(c.block)).defaultBlockState()); }

    @SuppressWarnings("unchecked")
    static void sampleParticles(Minecraft mc) {
        try {
            var f = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles"); f.setAccessible(true);
            var spriteField = net.minecraft.client.particle.TextureSheetParticle.class.getDeclaredField("sprite"); spriteField.setAccessible(true);
            for (var q : ((Map<Object, Queue<net.minecraft.client.particle.Particle>>) f.get(mc.particleEngine)).values())
                for (var p : q) if (p instanceof net.minecraft.client.particle.TerrainParticle t && !old.contains(t) && t.getBoundingBox().getCenter().distanceTo(TARGET.getCenter()) < 2) {
                    if (breakTick >= 0 && seen.add(t)) burstNew++;
                    var sprite = (net.minecraft.client.renderer.texture.TextureAtlasSprite) spriteField.get(t);
                    sprites.add(sprite.contents().name().getPath().replace("blocks/", ""));
                }
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }

    @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("tc4modern.blockBehaviourAuditOnly")) return;
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ticks++;
        var server = mc.getSingleplayerServer(); var id = mc.player.getUUID();
        if (!listening) {
            listening = true;
            mc.getSoundManager().addListener((sound, events, range) -> {
                if (Math.abs(sound.getX() - (TARGET.getX() + .5)) < 3 && Math.abs(sound.getZ() - (TARGET.getZ() + .5)) < 3 || walk >= 0)
                    heard.add(sound.getLocation() + "@" + String.format(Locale.ROOT, "%.2f/%.2f", sound.getVolume(), sound.getPitch()));
            });
        }
        if (ticks == 40) server.execute(() -> {
            var p = server.getPlayerList().getPlayer(id); var level = p.serverLevel();
            level.getServer().setDifficulty(net.minecraft.world.Difficulty.PEACEFUL, true); level.setDayTime(6000);
            for (int x = -4; x <= 4; x++) for (int z = -4; z <= 12; z++) for (int y = -1; y < 4; y++)
                level.setBlock(BASE.offset(x, y, z), y < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2);
            p.setGameMode(GameType.SURVIVAL); p.getAbilities().flying = false; p.onUpdateAbilities(); p.getInventory().clearContent(); p.removeAllEffects();
            p.connection.teleport(BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 50);
        });
        if (ticks < 70) return;
        if (index < CASES.size() && walk < 0) {
            if (index < 0 || phase == 3) {
                index++; phase = 0; phaseTick = ticks;
                if (index >= CASES.size()) { walk = 0; phaseTick = ticks; return; }
                var c = CASES.get(index);
                heard.clear(); sprites.clear(); drops = -1; breakTick = -1; burstNew = 0; seen.clear(); snapshot(mc);
                server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id); var level = p.serverLevel();
                    for (var e : level.getEntitiesOfClass(ItemEntity.class, new AABB(TARGET).inflate(4))) e.discard();
                    level.setBlock(TARGET.below(), Blocks.STONE.defaultBlockState(), 3);
                    level.setBlock(TARGET, state(c), 3);
                    p.getInventory().clearContent();
                    p.getInventory().setItem(0, c.tool.isEmpty() ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.withDefaultNamespace(c.tool))));
                    p.getInventory().selected = 0; p.inventoryMenu.broadcastChanges();
                    p.connection.teleport(BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 50);
                });
                return;
            }
            var c = CASES.get(index);
            if (phase == 0 && ticks - phaseTick == 6) {
                heard.clear();
                mc.gameMode.startDestroyBlock(TARGET, Direction.UP); phase = 1; digTicks = 0;
                if (c.block.equals("air_infused_stone")) sparkles0 = sparkleCount(mc);
            } else if (phase == 1) {
                digTicks++;
                sampleParticles(mc);
                int before = terrainCount(mc);
                if (!mc.level.getBlockState(TARGET).isAir() && digTicks <= 400) { mc.gameMode.continueDestroyBlock(TARGET, Direction.UP); mc.particleEngine.addBlockHitEffects(TARGET, new net.minecraft.world.phys.BlockHitResult(TARGET.getCenter(), Direction.UP, TARGET, false)); mc.player.swing(net.minecraft.world.InteractionHand.MAIN_HAND); }
                if (c.block.equals("air_infused_stone") && sparkleCount(mc) > sparkles0) sparkleSeen = true;
                if (mc.level.getBlockState(TARGET).isAir() || digTicks > 400) { snapshotSeenNow(mc); breakTick = ticks; phase = 2; phaseTick = ticks; }
            } else if (phase == 2) {
                sampleParticles(mc);
                if (ticks - phaseTick == 4 && (c.block.contains("jar") || c.block.equals("crucible") && index == 6)) LiquidDeathAudit.shot(mc, "blocks-broken-" + c.block + ".png");
                if (ticks - phaseTick == 3) burst = burstNew;
                if (ticks - phaseTick == 12) server.execute(() -> { var level = server.overworld(); var pl = server.getPlayerList().getPlayer(id); int inv = 0; for (int i = 0; i < pl.getInventory().getContainerSize(); i++) { var st = pl.getInventory().getItem(i); if (!st.isEmpty() && !(st.getItem() instanceof net.minecraft.world.item.TieredItem)) inv += st.getCount(); } drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(TARGET).inflate(3)).size() + inv; });
                if (ticks - phaseTick == 16) {
                    boolean broken = mc.level.getBlockState(TARGET).isAir();
                    var soundSet = new TreeSet<String>(); for (var h : heard) soundSet.add(h);
                    String tag = c.block + (c.tool.isEmpty() ? " by hand" : " with " + c.tool);
                    log(tag + ": dig " + digTicks + " ticks, sounds " + soundSet + ", sprites " + sprites + ", drops " + drops);
                    check(broken, tag + " broke");
                    check(heard.stream().anyMatch(h -> h.startsWith(c.sound)), tag + " plays " + c.sound);
                    check(heard.stream().noneMatch(h -> h.startsWith("minecraft:block.glass")) || c.sound.contains("glass"), tag + " no stray glass sound");
                    check(sprites.contains(c.sprite) && sprites.size() == 1, tag + " digging particles use original " + c.sprite);
                    if (c.particles) check(burst >= 8, tag + " destroy burst (" + burst + " particles)");
                    else check(burst < 4, tag + " BlockJar.addDestroyEffects: no destroy burst (" + burst + ")");
                    check((drops > 0) == c.drops, tag + (c.drops ? " drops" : " drops nothing (needs proper tool)"));
                    if (c.block.equals("air_infused_stone")) check(sparkleSeen, "air infused stone hit sparkle");
                    phase = 3;
                }
            }
            return;
        }
        if (walk >= 0) {
            int t = ticks - phaseTick;
            if (walk < WALK.length) {
                String name = WALK[walk];
                if (t == 1) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id); var level = p.serverLevel();
                    var block = BuiltInRegistries.BLOCK.get(AlchemyContent.id(name)).defaultBlockState();
                    for (int z = 0; z <= 10; z++) { level.setBlock(BASE.offset(0, 0, z), block, 3); for (int y = 1; y < 4; y++) level.setBlock(BASE.offset(0, y, z), Blocks.AIR.defaultBlockState(), 3); }
                    p.getInventory().clearContent();
                    p.connection.teleport(BASE.getX() + .5, BASE.getY() + 1, BASE.getZ() + .5, 0, 30);
                });
                if (t == 10) heard.clear();
                if (t >= 10 && t < 40) mc.player.setDeltaMovement(0, mc.player.getDeltaMovement().y, .22);
                if (t == 22) LiquidDeathAudit.shot(mc, "blocks-walk-" + name + ".png");
                if (t == 40) {
                    var steps = heard.stream().filter(h -> !h.contains("ambient")).toList();
                    String expected = name.equals("warded_jar") ? "tc4modern:jar" : name.equals("taint") ? "tc4modern:gore" : "minecraft:block.stone.step";
                    log("walk on " + name + ": " + new TreeSet<>(steps));
                    check(steps.stream().filter(h -> h.startsWith(expected)).count() >= 2, "walking on " + name + " plays " + expected + " steps");
                    walk++; phaseTick = ticks;
                }
                return;
            }
            if (t == 5) {
                System.out.println("TC4_BLOCKS " + (failures == 0 ? "ALL PASS" : "FAILURES " + failures));
                try { java.nio.file.Files.write(java.nio.file.Path.of("block-behaviour.txt"), report); } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
                mc.stop();
            }
        }
    }
    static boolean sparkleSeen; static int burst;
    static final Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
    @SuppressWarnings("unchecked")
    static void snapshot(Minecraft mc) {
        old.clear();
        try { var f = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles"); f.setAccessible(true);
            for (var q : ((Map<Object, Queue<Object>>) f.get(mc.particleEngine)).values()) old.addAll(q);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    /** Particles already alive at the break tick (hit particles) are not part of the destroy burst. */
    @SuppressWarnings("unchecked")
    static void snapshotSeenNow(Minecraft mc) {
        try { var f = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles"); f.setAccessible(true);
            for (var q : ((Map<Object, Queue<Object>>) f.get(mc.particleEngine)).values()) seen.addAll(q);
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    @SuppressWarnings("unchecked")
    static int terrainCount(Minecraft mc) {
        try {
            var f = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles"); f.setAccessible(true);
            int n = 0; for (var q : ((Map<Object, Queue<Object>>) f.get(mc.particleEngine)).values()) for (var p : q) if (p instanceof net.minecraft.client.particle.TerrainParticle) n++;
            return n;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
    @SuppressWarnings("unchecked")
    static int sparkleCount(Minecraft mc) {
        try {
            var f = net.minecraft.client.particle.ParticleEngine.class.getDeclaredField("particles"); f.setAccessible(true);
            int n = 0; for (var q : ((Map<Object, Queue<Object>>) f.get(mc.particleEngine)).values()) for (var p : q) if (p instanceof FociFx.Sparkle) n++;
            return n;
        } catch (ReflectiveOperationException e) { throw new IllegalStateException(e); }
    }
}
