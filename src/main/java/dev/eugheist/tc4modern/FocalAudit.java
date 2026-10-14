package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Opt-in visible client check (runFocalAudit): the focal manipulator GUI and a potency upgrade paid from an energized node,
 * then the shock focus bolt, chain lightning and earth shock against zombies, and the fire focus fireball.
 */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class FocalAudit {
    static boolean enabled() { return Boolean.getBoolean("tc4modern.focalAudit"); }
    static final BlockPos BASE = new BlockPos(0, 120, 0), TABLE = BASE.offset(0, 0, 2);

    @SubscribeEvent public static void joined(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (Boolean.getBoolean("tc4modern.containerVisualAuditOnly")||Boolean.getBoolean("tc4modern.bellInteractionAuditOnly")||Boolean.getBoolean("tc4modern.jeiAuditOnly")||!enabled() || Boolean.getBoolean("tc4modern.portalDepthAuditOnly")||Boolean.getBoolean("tc4modern.golemLinkAuditOnly")||Boolean.getBoolean("tc4modern.taintGlowAuditOnly")||Boolean.getBoolean("tc4modern.blueForestAuditOnly")||Boolean.getBoolean("tc4modern.golemChestAuditOnly")||Boolean.getBoolean("tc4modern.golemColorClientAuditOnly")||Boolean.getBoolean("tc4modern.cheatSheetClientAuditOnly")||Boolean.getBoolean("tc4modern.clientPreferencesAuditOnly")||Boolean.getBoolean("tc4modern.villageBuildingAuditOnly")||Boolean.getBoolean("tc4modern.villageAuditOnly")||Boolean.getBoolean("tc4modern.itemAspectAuditOnly")||Boolean.getBoolean("tc4modern.researchRulesAuditOnly")||Boolean.getBoolean("tc4modern.baublesClickAuditOnly")||Boolean.getBoolean("tc4modern.runicHudAuditOnly")||Boolean.getBoolean("tc4modern.infusionFxAuditOnly")||Boolean.getBoolean("tc4modern.bookIconAuditOnly")||Boolean.getBoolean("tc4modern.blockDumpOnly")||Boolean.getBoolean("tc4modern.blockBehaviourAuditOnly")||Boolean.getBoolean("tc4modern.tableAuditOnly")||Boolean.getBoolean("tc4modern.workbenchWandAuditOnly")||Boolean.getBoolean("tc4modern.fragmentAuditOnly")||Boolean.getBoolean("tc4modern.thaumatoriumAuditOnly")||Boolean.getBoolean("tc4modern.keysAuditOnly")||Boolean.getBoolean("tc4modern.alchemyMachinesAuditOnly")||Boolean.getBoolean("tc4modern.jarCreativeAuditOnly")||Boolean.getBoolean("tc4modern.mirrorSizeAuditOnly")||Boolean.getBoolean("tc4modern.golemAuditOnly")||Boolean.getBoolean("tc4modern.golemHarvestAuditOnly")||Boolean.getBoolean("tc4modern.golemUpgradeAuditOnly")||Boolean.getBoolean("tc4modern.golemCombatAuditOnly")||Boolean.getBoolean("tc4modern.golemLumberAuditOnly")||Boolean.getBoolean("tc4modern.golemLiquidAuditOnly")||Boolean.getBoolean("tc4modern.golemEssentiaAuditOnly")||Boolean.getBoolean("tc4modern.golemancyBookAuditOnly")||Boolean.getBoolean("tc4modern.golemAccessoryAuditOnly")||Boolean.getBoolean("tc4modern.golemBellShowOnly")||Boolean.getBoolean("tc4modern.followingPickupAuditOnly")||Boolean.getBoolean("tc4modern.golemFishAuditOnly")||Boolean.getBoolean("tc4modern.golemSortingAuditOnly")||Boolean.getBoolean("tc4modern.golemUseAuditOnly")||Boolean.getBoolean("tc4modern.trunkAuditOnly")||Boolean.getBoolean("tc4modern.championAuditOnly")||Boolean.getBoolean("tc4modern.grateShowOnly")||Boolean.getBoolean("tc4modern.eldritchGearAuditOnly")||Boolean.getBoolean("tc4modern.renderFixShowOnly")||Boolean.getBoolean("tc4modern.blockGalleryShowOnly")||Boolean.getBoolean("tc4modern.entityGalleryShowOnly")||Boolean.getBoolean("tc4modern.reservoirAuditOnly")||Boolean.getBoolean("tc4modern.advancedFurnaceAuditOnly")||Boolean.getBoolean("tc4modern.taintedLandShowOnly")||Boolean.getBoolean("tc4modern.decorAuditOnly")||Boolean.getBoolean("tc4modern.outerAuditOnly")||Boolean.getBoolean("tc4modern.outerJourneyAuditOnly")||Boolean.getBoolean("tc4modern.outerBossAuditOnly")||Boolean.getBoolean("tc4modern.nodeWorldAuditOnly")||Boolean.getBoolean("tc4modern.eldritchEyeAuditOnly")||Boolean.getBoolean("tc4modern.taintacleAuditOnly")||Boolean.getBoolean("tc4modern.taintedAuditOnly")||Boolean.getBoolean("tc4modern.wispAuditOnly")||Boolean.getBoolean("tc4modern.crabAuditOnly")||Boolean.getBoolean("tc4modern.inventoryHoverAuditOnly")||Boolean.getBoolean("tc4modern.deconstructorFixAuditOnly")||Boolean.getBoolean("tc4modern.addonBookAuditOnly")||Boolean.getBoolean("tc4modern.tubeAuditOnly") || !(event.getEntity() instanceof ServerPlayer player)) return;
        var level = player.serverLevel();
        player.setGameMode(GameType.SURVIVAL);
        level.setDayTime(6000);
        level.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, level.getServer());
        level.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, level.getServer());
        level.getServer().setDifficulty(net.minecraft.world.Difficulty.NORMAL, true);
        for (int x = -10; x <= 10; x++) for (int z = -4; z <= 16; z++) for (int y = -1; y < 8; y++)
            level.setBlock(BASE.offset(x, y, z), y < 0 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 3);
        thaumcraft.common.config.ConfigResearchData.init();
        DevCommands.grant(player.getPersistentData(), "FOCALMANIPULATION", new java.util.HashSet<>());
        player.setExperienceLevels(100);
        level.setBlock(TABLE, FocalManipulator.BLOCK.get().defaultBlockState(), 3);
        var nodePos = TABLE.offset(2, 2, 0);
        level.setBlock(nodePos, TC4Modern.ENERGIZED_NODE.get().defaultBlockState(), 3);
        if (level.getBlockEntity(nodePos) instanceof EnergizedNodeEntity node) {
            for (var a : Aspect.getPrimalAspects()) { node.auraBase.add(a, 400); node.energy.visBase.add(a, 400); }
            node.changed();
        }
        player.getInventory().clearContent();
        player.getInventory().setItem(0, FociClientAudit.wand(new ItemStack(FociContent.SHOCK.get())));
        player.getInventory().setItem(1, new ItemStack(FociContent.SHOCK.get()));
        player.getInventory().selected = 0;
        player.connection.teleport(BASE.getX() + .5, BASE.getY(), BASE.getZ() + .5, 0, 30);
        System.out.println("TC4_FOCAL_AUDIT ready");
    }

    static void require(boolean value,String message) { if(!value) throw new IllegalStateException("TC4_FOCAL_AUDIT FAIL " + message); }
    static int warded(net.minecraft.server.level.ServerLevel level) {
        int count=0;for(int x=-2;x<=2;x++)for(int y=1;y<=5;y++)if(level.getBlockState(BASE.offset(x,y,4)).is(Warding.WARDED.get()))count++;return count;
    }
    static void zombies(net.minecraft.server.level.ServerLevel level, int n) {
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, new net.minecraft.world.phys.AABB(BASE).inflate(30))) e.discard();
        for (int i = 0; i < n; i++) {
            var z = EntityType.HUSK.create(level);
            z.moveTo(BASE.getX() - 5.5 + i * 2, BASE.getY(), BASE.getZ() + 8.5, 180, 0);
            z.setNoAi(true);
            z.setPersistenceRequired();
            level.addFreshEntity(z);
        }
    }
    static float hp(net.minecraft.server.level.ServerLevel level) {
        float sum = 0;
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.monster.Husk.class, new net.minecraft.world.phys.AABB(BASE).inflate(30))) sum += e.getHealth();
        return sum;
    }

    @EventBusSubscriber(modid = TC4Modern.MODID, value = Dist.CLIENT)
    static final class Client {
        private static int ticks;
        private static ThaumonomiconScreen book;
        private static int alchemyWarmup;
        private static final java.util.List<Object[]> spreads = new java.util.ArrayList<>();
        private static void log(String s) { System.out.println("TC4_FOCAL_AUDIT " + s); }
        @SubscribeEvent static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event) {
            if (Boolean.getBoolean("tc4modern.containerVisualAuditOnly")||Boolean.getBoolean("tc4modern.bellInteractionAuditOnly")||Boolean.getBoolean("tc4modern.jeiAuditOnly")||!enabled()||Boolean.getBoolean("tc4modern.portalDepthAuditOnly")||Boolean.getBoolean("tc4modern.golemLinkAuditOnly")||Boolean.getBoolean("tc4modern.taintGlowAuditOnly")||Boolean.getBoolean("tc4modern.blueForestAuditOnly")||Boolean.getBoolean("tc4modern.golemChestAuditOnly")||Boolean.getBoolean("tc4modern.golemColorClientAuditOnly")||Boolean.getBoolean("tc4modern.cheatSheetClientAuditOnly")||Boolean.getBoolean("tc4modern.clientPreferencesAuditOnly")||Boolean.getBoolean("tc4modern.villageBuildingAuditOnly")||Boolean.getBoolean("tc4modern.villageAuditOnly")||Boolean.getBoolean("tc4modern.itemAspectAuditOnly")||Boolean.getBoolean("tc4modern.researchRulesAuditOnly")||Boolean.getBoolean("tc4modern.baublesClickAuditOnly")||Boolean.getBoolean("tc4modern.runicHudAuditOnly")||Boolean.getBoolean("tc4modern.infusionFxAuditOnly")||Boolean.getBoolean("tc4modern.bookIconAuditOnly")||Boolean.getBoolean("tc4modern.blockDumpOnly")||Boolean.getBoolean("tc4modern.blockBehaviourAuditOnly")||Boolean.getBoolean("tc4modern.tableAuditOnly")||Boolean.getBoolean("tc4modern.workbenchWandAuditOnly")||Boolean.getBoolean("tc4modern.fragmentAuditOnly")||Boolean.getBoolean("tc4modern.thaumatoriumAuditOnly")||Boolean.getBoolean("tc4modern.keysAuditOnly")||Boolean.getBoolean("tc4modern.alchemyMachinesAuditOnly")||Boolean.getBoolean("tc4modern.jarCreativeAuditOnly")||Boolean.getBoolean("tc4modern.mirrorSizeAuditOnly")||Boolean.getBoolean("tc4modern.golemAuditOnly")||Boolean.getBoolean("tc4modern.golemHarvestAuditOnly")||Boolean.getBoolean("tc4modern.golemUpgradeAuditOnly")||Boolean.getBoolean("tc4modern.golemCombatAuditOnly")||Boolean.getBoolean("tc4modern.golemLumberAuditOnly")||Boolean.getBoolean("tc4modern.golemLiquidAuditOnly")||Boolean.getBoolean("tc4modern.golemEssentiaAuditOnly")||Boolean.getBoolean("tc4modern.golemancyBookAuditOnly")||Boolean.getBoolean("tc4modern.golemAccessoryAuditOnly")||Boolean.getBoolean("tc4modern.golemBellShowOnly")||Boolean.getBoolean("tc4modern.followingPickupAuditOnly")||Boolean.getBoolean("tc4modern.golemFishAuditOnly")||Boolean.getBoolean("tc4modern.golemSortingAuditOnly")||Boolean.getBoolean("tc4modern.golemUseAuditOnly")||Boolean.getBoolean("tc4modern.trunkAuditOnly")||Boolean.getBoolean("tc4modern.championAuditOnly")||Boolean.getBoolean("tc4modern.grateShowOnly")||Boolean.getBoolean("tc4modern.eldritchGearAuditOnly")||Boolean.getBoolean("tc4modern.renderFixShowOnly")||Boolean.getBoolean("tc4modern.blockGalleryShowOnly")||Boolean.getBoolean("tc4modern.entityGalleryShowOnly")||Boolean.getBoolean("tc4modern.reservoirAuditOnly")||Boolean.getBoolean("tc4modern.advancedFurnaceAuditOnly")||Boolean.getBoolean("tc4modern.taintedLandShowOnly")||Boolean.getBoolean("tc4modern.decorAuditOnly")||Boolean.getBoolean("tc4modern.outerAuditOnly")||Boolean.getBoolean("tc4modern.outerJourneyAuditOnly")||Boolean.getBoolean("tc4modern.outerBossAuditOnly")||Boolean.getBoolean("tc4modern.nodeWorldAuditOnly")||Boolean.getBoolean("tc4modern.eldritchEyeAuditOnly")||Boolean.getBoolean("tc4modern.taintacleAuditOnly")||Boolean.getBoolean("tc4modern.taintedAuditOnly")||Boolean.getBoolean("tc4modern.wispAuditOnly")||Boolean.getBoolean("tc4modern.crabAuditOnly")||Boolean.getBoolean("tc4modern.inventoryHoverAuditOnly")||Boolean.getBoolean("tc4modern.deconstructorFixAuditOnly")||Boolean.getBoolean("tc4modern.addonBookAuditOnly")||Boolean.getBoolean("tc4modern.tubeAuditOnly")||Boolean.getBoolean("tc4modern.guardianAuditOnly")||Boolean.getBoolean("tc4modern.artificeBookAuditOnly")||Boolean.getBoolean("tc4modern.furnaceAuditOnly")||Boolean.getBoolean("tc4modern.fortressAuditOnly")||Boolean.getBoolean("tc4modern.arcanaAuditOnly")||Boolean.getBoolean("tc4modern.boreAuditOnly")||Boolean.getBoolean("tc4modern.earthAuditOnly")||Boolean.getBoolean("tc4modern.swordAuditOnly")||Boolean.getBoolean("tc4modern.elementalAuditOnly")||Boolean.getBoolean("tc4modern.bowAuditOnly")||Boolean.getBoolean("tc4modern.bannerAuditOnly")||Boolean.getBoolean("tc4modern.mirrorAuditOnly")||Boolean.getBoolean("tc4modern.hoverAuditOnly")||Boolean.getBoolean("tc4modern.bootsAuditOnly")||Boolean.getBoolean("tc4modern.runicAuditOnly")||Boolean.getBoolean("tc4modern.enchantAuditOnly")||Boolean.getBoolean("tc4modern.lampsAuditOnly")||Boolean.getBoolean("tc4modern.sinStoneAuditOnly")||Boolean.getBoolean("tc4modern.scrubberAuditOnly")||Boolean.getBoolean("tc4modern.lifterAuditOnly")||Boolean.getBoolean("tc4modern.earAuditOnly")||Boolean.getBoolean("tc4modern.artificeDeviceAuditOnly")||Boolean.getBoolean("tc4modern.infusionAuditOnly")||Boolean.getBoolean("tc4modern.hygieneAuditOnly")||Boolean.getBoolean("tc4modern.deathAuditOnly")||Boolean.getBoolean("tc4modern.bloomAuditOnly")) return;
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.player == null || mc.level == null) return;
            if(ticks==0&&(Boolean.getBoolean("tc4modern.alchemyStarterAuditOnly")||Boolean.getBoolean("tc4modern.alchemyBookAuditOnly"))&&alchemyWarmup++<80)return;
            if (ticks == 0 && Boolean.getBoolean("tc4modern.wardingAuditOnly")) ticks = 799;
            if(ticks==0&&(Boolean.getBoolean("tc4modern.thaumaturgyBookAuditOnly")||Boolean.getBoolean("tc4modern.alchemyStarterAuditOnly")||Boolean.getBoolean("tc4modern.alchemyBookAuditOnly")))ticks=1199;
            ticks++;
            var server = mc.getSingleplayerServer();
            var id = mc.player.getUUID();
            var out = java.nio.file.Path.of("focal-audit");
            try {
                java.nio.file.Files.createDirectories(out);
                if(Boolean.getBoolean("tc4modern.focalReopenAuditOnly")) {
                    if(ticks==40)server.execute(()->{
                        var p=server.getPlayerList().getPlayer(id);var l=p.serverLevel();l.removeBlock(TABLE.offset(2,2,0),false);
                        var table=(FocalManipulator.TableEntity)l.getBlockEntity(TABLE);table.setItem(0,new ItemStack(FociContent.SHOCK.get()));
                        require(table.startCraft(FociContent.POTENCY,p),"shock upgrade starts");log("PASS active shock upgrade prepared");
                    });
                    if(ticks==60||ticks==120)server.execute(()->{
                        var p=server.getPlayerList().getPlayer(id);var hit=new net.minecraft.world.phys.BlockHitResult(TABLE.getCenter(),net.minecraft.core.Direction.UP,TABLE,false);
                        p.level().getBlockState(TABLE).useWithoutItem(p.level(),p,hit);
                    });
                    if(ticks==80||ticks==140){
                        require(mc.screen instanceof FocalManipulatorClient.Screen,"active manipulator opens");
                        var table=((FocalManipulator.Menu)mc.player.containerMenu).table;require(table.size>0&&table.getItem(0).is(FociContent.SHOCK.get()),"opening retains active shock upgrade");
                        shot(mc,out,"shock-upgrade-reopen-"+ticks+".png");log("PASS active shock screen opened tick="+ticks);
                    }
                    if(ticks==100)mc.player.closeContainer();
                    if(ticks==160){mc.screen.resize(mc,mc.screen.width,mc.screen.height);require(mc.screen instanceof FocalManipulatorClient.Screen,"resize keeps screen");shot(mc,out,"shock-upgrade-resize.png");log("PASS active shock screen reinitialized");}
                    if(ticks==180)mc.stop();
                    return;
                }
                if (ticks == 80) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    var hit = new net.minecraft.world.phys.BlockHitResult(TABLE.getCenter(), net.minecraft.core.Direction.UP, TABLE, false);
                    p.level().getBlockState(TABLE).useWithoutItem(p.level(), p, hit);
                });
                if (ticks == 100) {
                    // Put the spare shock focus on the table through the menu slot (shift-click from slot 1+28 = hotbar 1).
                    var menu = mc.player.containerMenu;
                    log("menu=" + menu.getClass().getSimpleName());
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, 29, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE, mc.player);
                }
                if (ticks == 120 && mc.screen instanceof FocalManipulatorClient.Screen) {
                    // Select potency (second icon at rank 1: frugal, potency).
                    int gx = (mc.screen.width - 192) / 2, gy = (mc.screen.height - 233) / 2;
                    mc.screen.mouseClicked(gx + 48 + 16 + 8, gy + 104 + 8, 0);
                }
                if (ticks == 135) shot(mc, out, "gui-selected.png");
                if (ticks == 140 && mc.screen != null) {
                    int gx = (mc.screen.width - 192) / 2, gy = (mc.screen.height - 233) / 2;
                    mc.screen.mouseClicked(gx + 96, gy + 92, 0);
                }
                if (ticks == 150) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    if (p.level().getBlockEntity(TABLE) instanceof FocalManipulator.TableEntity t)
                        log("started size=" + t.size + " upgrade=" + t.upgrade + " rank=" + t.rank + " xp=" + p.experienceLevel);
                });
                if (ticks == 170) shot(mc, out, "gui-progress.png");
                if (ticks == 260) shot(mc, out, "gui-later.png");
                if (ticks == 400) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    if (p.level().getBlockEntity(TABLE) instanceof FocalManipulator.TableEntity t) {
                        var f = t.getItem(0);
                        log("after size=" + t.size + " remaining=" + t.aspects.visSize() + " upgrades=" + java.util.Arrays.toString(((FociContent.Focus) f.getItem()).getAppliedUpgrades(f)));
                    }
                });
                if (ticks == 420) mc.setScreen(null);
                if (ticks == 425) shot(mc, out, "table-world.png");
                // Shock focus: a rank-3 chain lightning focus with enlarge at rank 4; three zombies in a row.
                if (ticks == 440) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    var focus = new ItemStack(FociContent.SHOCK.get());
                    var fo = (FociContent.Focus) focus.getItem();
                    fo.applyUpgrade(focus, 0, 1); fo.applyUpgrade(focus, 0, 2); fo.applyUpgrade(focus, 17, 3); fo.applyUpgrade(focus, 3, 4);
                    p.getInventory().setItem(0, FociClientAudit.wand(focus));
                    zombies(p.serverLevel(), 3);
                    p.connection.teleport(BASE.getX() - 3.5, BASE.getY(), BASE.getZ() + .5, 0, 8);
                    log("zombies hp=" + hp(p.serverLevel()));
                });
                // Real input path: hold use with the wand so ItemWandCasting starts the focus and ticks it every tick.
                if (ticks == 470) mc.options.keyUse.setDown(true);
                if (ticks == 530) mc.options.keyUse.setDown(false);
                if (ticks >= 474 && ticks <= 492 && ticks % 3 == 0) shot(mc, out, "shock-bolt-" + ticks + ".png");
                if (ticks == 500) shot(mc, out, "shock-chain.png");
                if (ticks == 535) server.execute(() -> log("chain hp=" + hp(server.getPlayerList().getPlayer(id).serverLevel())));
                // Earth shock: a lobbed orb that leaves static fields.
                if (ticks == 540) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    var focus = new ItemStack(FociContent.SHOCK.get());
                    ((FociContent.Focus) focus.getItem()).applyUpgrade(focus, 18, 3);
                    p.getInventory().setItem(0, FociClientAudit.wand(focus));
                    zombies(p.serverLevel(), 3);
                    p.connection.teleport(BASE.getX() - 3.5, BASE.getY(), BASE.getZ() + .5, 0, -5);
                });
                if (ticks == 550) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    FociContent.setCooldown(p, 0);
                    FociContent.getFocus(p.getMainHandItem()).onFocusRightClick(p.getMainHandItem(), p.level(), p);
                });
                if (ticks == 562) shot(mc, out, "earthshock-flight.png");
                if (ticks == 600) {
                    shot(mc, out, "earthshock-field.png");
                    server.execute(() -> {
                        var p = server.getPlayerList().getPlayer(id);
                        int sparks = 0;
                        for (var pos : BlockPos.betweenClosed(BASE.offset(-8, -1, 0), BASE.offset(8, 6, 16))) if (p.level().getBlockState(pos).is(FocusProjectiles.SPARK.get())) sparks++;
                        log("earthshock hp=" + hp(p.serverLevel()) + " sparks=" + sparks);
                    });
                }
                // Fireball.
                if (ticks == 620) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    var focus = new ItemStack(FociContent.FIRE.get());
                    ((FociContent.Focus) focus.getItem()).applyUpgrade(focus, 9, 3);
                    p.getInventory().setItem(0, FociClientAudit.wand(focus));
                    zombies(p.serverLevel(), 3);
                    p.connection.teleport(BASE.getX() - 3.5, BASE.getY(), BASE.getZ() + .5, 0, -2);
                    FociContent.setCooldown(p, 0);
                    FociContent.getFocus(p.getMainHandItem()).onFocusRightClick(p.getMainHandItem(), p.level(), p);
                });
                if (ticks == 626) shot(mc, out, "fireball.png");
                if (ticks == 660) server.execute(() -> log("fireball hp=" + hp(server.getPlayerList().getPlayer(id).serverLevel())
                    + " stoneIntact=" + server.overworld().getBlockState(BASE.offset(0, -1, 8)).is(Blocks.STONE)));
                // Nine Hells: plain, bat bomb and vampire bats at three husks.
                if (ticks == 670) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    zombies(p.serverLevel(), 3);
                    p.setHealth(10);
                    int[][] ups = {{}, {0, 0, Hellbat.BATBOMBS}, {0, 0, 1, 1, Hellbat.VAMPIREBATS}};
                    for (int i = 0; i < 3; i++) {
                        var focus = new ItemStack(Hellbat.FOCUS.get());
                        for (int r = 0; r < ups[i].length; r++) ((FociContent.Focus) focus.getItem()).applyUpgrade(focus, ups[i][r], r + 1);
                        p.getInventory().setItem(0, FociClientAudit.wand(focus));
                        p.connection.teleport(BASE.getX() - 5.5 + i * 2, BASE.getY(), BASE.getZ() + 2.5, 0, 0);
                        FociContent.setCooldown(p, 0);
                        FociContent.getFocus(p.getMainHandItem()).onFocusRightClick(p.getMainHandItem(), p.level(), p);
                    }
                    p.connection.teleport(BASE.getX() - 3.5, BASE.getY() + 1, BASE.getZ() + 1.5, 0, 15);
                    log("bats=" + p.level().getEntitiesOfClass(Hellbat.FireBat.class, new net.minecraft.world.phys.AABB(BASE).inflate(30)).size());
                });
                if (ticks == 682) shot(mc, out, "hellbat-flight.png");
                if (ticks == 700) shot(mc, out, "hellbat-attack.png");
                if (ticks == 760) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    log("hellbat hp=" + hp(p.serverLevel()) + " bats=" + p.level().getEntitiesOfClass(Hellbat.FireBat.class, new net.minecraft.world.phys.AABB(BASE).inflate(30)).size()
                        + " playerHp=" + p.getHealth() + " regen=" + p.hasEffect(net.minecraft.world.effect.MobEffects.REGENERATION));
                });
                // Warding and architect: actual use and key packet paths, with server assertions.
                if (ticks == 800) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    for (var bat : p.serverLevel().getEntitiesOfClass(Hellbat.FireBat.class, new net.minecraft.world.phys.AABB(BASE).inflate(30))) bat.discard();
                    p.getAbilities().invulnerable = true; p.onUpdateAbilities();
                    p.level().setBlock(TABLE.offset(2,2,0),Blocks.AIR.defaultBlockState(),3);
                    p.level().setBlock(TABLE,Blocks.AIR.defaultBlockState(),3);
                    for (int x=-3;x<=3;x++) for(int y=0;y<=5;y++) for(int z=3;z<=6;z++)
                        p.level().setBlock(BASE.offset(x,y,z),Blocks.AIR.defaultBlockState(),3);
                    for (int x=-2;x<=2;x++) for(int y=1;y<=5;y++) p.level().setBlock(BASE.offset(x,y,4),Blocks.STONE_BRICKS.defaultBlockState(),3);
                    var focus = new ItemStack(Warding.FOCUS.get());
                    ((FociContent.Focus)focus.getItem()).applyUpgrade(focus,FociContent.ARCHITECT,2);
                    p.getInventory().setItem(0,FociClientAudit.wand(focus));
                    p.connection.teleport(.5,120,.5,0,-25);
                    log("warding wall ready; area=" + Warding.areaX(p.getMainHandItem()));
                });
                if(ticks==830) shot(mc,out,"warding-architect.png");
                if(ticks==835) net.minecraft.client.KeyMapping.click(WardingClient.KEY_G.getKey());
                if(ticks==845) server.execute(() -> {
                    var p=server.getPlayerList().getPlayer(id);
                    require(Warding.areaX(p.getMainHandItem())==0,"G wraps architect size 3 to 0");
                    log("G packet PASS area=0");
                });
                if(ticks==850) shot(mc,out,"warding-area-zero.png");
                if(ticks==855 || ticks==860) net.minecraft.client.KeyMapping.click(WardingClient.KEY_G.getKey());
                if(ticks==875) mc.options.keyUse.setDown(true);
                if(ticks==877) mc.options.keyUse.setDown(false);
                if(ticks==900) server.execute(() -> {
                    var p=server.getPlayerList().getPlayer(id);
                    int count=warded(p.serverLevel());require(count==25,"architect wards 5x5 wall; actual="+count);
                    log("warded wall PASS blocks="+count);
                });
                if(ticks==910) shot(mc,out,"warding-protected.png");
                if(ticks==920) server.execute(() -> {
                    var p=server.getPlayerList().getPlayer(id);
                    p.serverLevel().explode(null,.5,123,5.5,4,net.minecraft.world.level.Level.ExplosionInteraction.TNT);
                    require(warded(p.serverLevel())==25,"warded wall survives explosion");
                    log("explosion PASS warded=25");
                });
                if(ticks==940) shot(mc,out,"warding-after-explosion.png");
                if(ticks==945) server.execute(() -> {
                    var p=server.getPlayerList().getPlayer(id);var pos=BASE.offset(0,3,4);
                    var tile=(Warding.WardedEntity)p.level().getBlockEntity(pos);
                    var loaded=new Warding.WardedEntity(pos,tile.getBlockState());
                    loaded.loadAdditional(tile.getUpdateTag(p.registryAccess()),p.registryAccess());
                    require(loaded.state==tile.state && loaded.owner==tile.owner && loaded.light==tile.light,"ward NBT roundtrip");
                    int owner=tile.owner;tile.owner++;
                    FociContent.getFocus(p.getMainHandItem()).onFocusRightClick(p.getMainHandItem(),p.level(),p);
                    require(warded(p.serverLevel())==25,"foreign owner cannot unward");tile.owner=owner;
                    log("owner protection and NBT PASS");
                });
                if(ticks==950) mc.options.keyAttack.setDown(true);
                if(ticks==958) shot(mc,out,"warding-hit.png");
                if(ticks==960) mc.options.keyAttack.setDown(false);
                if(ticks==975) server.execute(() -> server.getPlayerList().getPlayer(id).connection.teleport(.5,120,.5,0,-25));
                if(ticks==980) mc.options.keyUse.setDown(true);
                if(ticks==982) mc.options.keyUse.setDown(false);
                if(ticks==1000) server.execute(() -> {
                    var p=server.getPlayerList().getPlayer(id);
                    require(warded(p.serverLevel())==0,"unward whole wall");
                    require(p.level().getBlockState(BASE.offset(0,3,4)).is(Blocks.STONE_BRICKS),"restore original block state");
                    log("unward PASS restored original bricks");
                    p.connection.teleport(.5,120,.5,0,-25);
                    // Trade architect: pick quartz by sneak-use, then swap exactly the connected plane.
                    var f=new ItemStack(FociWorld.TRADE.get());
                    ((FociContent.Focus)f.getItem()).applyUpgrade(f,FociContent.ARCHITECT,3);
                    p.getInventory().setItem(0,FociClientAudit.wand(f));
                    p.getInventory().setItem(2,new ItemStack(net.minecraft.world.item.Items.QUARTZ_BLOCK,64));
                    p.level().setBlock(BASE.offset(0,3,4),Blocks.QUARTZ_BLOCK.defaultBlockState(),3);
                    p.setShiftKeyDown(true);
                    ((FociContent.Focus)f.getItem()).onFocusRightClick(p.getMainHandItem(),p.level(),p);
                    p.setShiftKeyDown(false);
                    p.level().setBlock(BASE.offset(0,3,4),Blocks.STONE_BRICKS.defaultBlockState(),3);
                    require(((Warding.Architect)f.getItem()).architectBlocks(p.getMainHandItem(),p.level(),BASE.offset(0,3,4),2,p).size()==25,"trade architect connected plane");
                    ((FociContent.Focus)f.getItem()).onFocusRightClick(p.getMainHandItem(),p.level(),p);
                });
                if(ticks==1080) server.execute(() -> {
                    var p=server.getPlayerList().getPlayer(id);int n=0;
                    for(int x=-2;x<=2;x++)for(int y=1;y<=5;y++)if(p.level().getBlockState(BASE.offset(x,y,4)).is(Blocks.QUARTZ_BLOCK))n++;
                    require(n==25,"trade architect swaps 25 blocks; actual="+n);log("trade architect PASS quartz="+n);
                });
                if(ticks==1090) shot(mc,out,"trade-architect.png");
                if(ticks==1100 && Boolean.getBoolean("tc4modern.wardingAuditOnly")) mc.stop();
                // Thaumonomicon recipe spreads, in a world so the recipe pages resolve.
                if (ticks == 1200) server.execute(() -> {
                    var p = server.getPlayerList().getPlayer(id);
                    for (var cat : thaumcraft.api.research.ResearchCategories.researchCategories.values())
                        for (var r : cat.research.values()) DevCommands.grant(p.getPersistentData(), r.key, new java.util.HashSet<>());
                    ThaumometerItem.sync(p, "");
                    if(Boolean.getBoolean("tc4modern.alchemyStarterAuditOnly"))AlchemyStarterAudit.run(p);
                    if(Boolean.getBoolean("tc4modern.thaumaturgyBookAuditOnly")) {
                        var grid=java.util.List.of(new ItemStack(net.minecraft.world.item.Items.LEATHER),new ItemStack(net.minecraft.world.item.Items.GOLD_INGOT),new ItemStack(net.minecraft.world.item.Items.LEATHER),new ItemStack(net.minecraft.world.item.Items.LEATHER),new ItemStack(WandPedestal.MUNDANE_BELT.get()),new ItemStack(net.minecraft.world.item.Items.LEATHER),new ItemStack(net.minecraft.world.item.Items.LEATHER),new ItemStack(net.minecraft.world.item.Items.LEATHER),new ItemStack(net.minecraft.world.item.Items.LEATHER));
                        var recipe=ArcaneRecipes.find(grid,ResearchProgress.completed(p.getPersistentData()));
                        require(recipe!=null&&recipe.key().equals("FocusPouch")&&recipe.output().is(PechRewards.POUCH.get()),"pouch recipe matches workbench");
                        require(ArcaneRecipes.find(grid,java.util.Set.of())==null,"pouch recipe needs research");
                        log("PASS pouch workbench recipe and research gate");
                    }
                });
                if (ticks == 1220) {
                    if(Boolean.getBoolean("tc4modern.alchemyStarterAuditOnly"))require(AlchemyStarterAudit.passed,"alchemy server gameplay checks completed");
                    var inv = mc.player.getInventory();
                    if(Boolean.getBoolean("tc4modern.alchemyBookAuditOnly"))AlchemyBookRecipes.audit();
                    book = new ThaumonomiconScreen(new ThaumonomiconMenu(0, inv), inv, net.minecraft.network.chat.Component.empty());
                    mc.setScreen(book);
                    for (var cat : ((Boolean.getBoolean("tc4modern.alchemyStarterAuditOnly")||Boolean.getBoolean("tc4modern.alchemyBookAuditOnly"))?java.util.List.of("ALCHEMY"):Boolean.getBoolean("tc4modern.thaumaturgyBookAuditOnly")?java.util.List.of("THAUMATURGY"):java.util.List.of("BASICS", "THAUMATURGY", "ALCHEMY", "ARTIFICE")))
                        for (var r : thaumcraft.api.research.ResearchCategories.getResearchList(cat).research.values()) {
                            if(Boolean.getBoolean("tc4modern.alchemyStarterAuditOnly")&&!AlchemyStarterAudit.RESEARCH.contains(r.key))continue;
                            if(Boolean.getBoolean("tc4modern.thaumaturgyBookAuditOnly")||Boolean.getBoolean("tc4modern.alchemyStarterAuditOnly")||Boolean.getBoolean("tc4modern.alchemyBookAuditOnly")) {
                                var icon=BasicsBookRecipes.researchIcon(r.key);
                                require(icon!=null,"research icon "+r.key);
                                require(!icon.stack().isEmpty()||icon.texture()!=null||icon.model()!=null,"nonempty research icon "+r.key);
                                log("PASS research icon "+r.key);
                            }
                            book.auditOpen(r, 0);
                            var pages = book.readablePages();
                            for (int i = 0; i < pages.size(); i++) if (pages.get(i).recipeKey != null) { spreads.add(new Object[]{r, i - i % 2}); i = i - i % 2 + 1; }
                        }
                    log("book spreads=" + spreads.size());
                }
                if(Boolean.getBoolean("tc4modern.thaumaturgyBookAuditOnly")&&ticks>=1225&&ticks<1300&&(ticks-1225)%10==0) {
                    int n=(ticks-1225)/10;
                    if(n>0)shot(mc,out,"thaumaturgy-icons-"+(n-1)+".png");
                    if(n<6)book.auditBrowser(n%2==0?-6:1,-6+n/2*5);
                }
                if(Boolean.getBoolean("tc4modern.alchemyBookAuditOnly")&&ticks>=1225&&ticks<1300&&(ticks-1225)%10==0){int n=(ticks-1225)/10;if(n>0)shot(mc,out,"alchemy-icons-"+(n-1)+".png");if(n<6)book.auditBrowser("ALCHEMY",n%2==0?-3:8,-5+n/2*5);}
                int bookStart=(Boolean.getBoolean("tc4modern.thaumaturgyBookAuditOnly")||Boolean.getBoolean("tc4modern.alchemyBookAuditOnly"))?1300:1225;
                if (ticks > bookStart && (ticks - bookStart) % 5 == 0 && !spreads.isEmpty() && mc.screen == book) {
                    int n = (ticks - bookStart) / 5 - 1;
                    if (n > 0) shot(mc, out, String.format("book-%03d-%s-%d.png", n - 1, ((thaumcraft.api.research.ResearchItem) spreads.get(n - 1)[0]).key, (int) spreads.get(n - 1)[1]));
                    if (n < spreads.size()) book.auditOpen((thaumcraft.api.research.ResearchItem) spreads.get(n)[0], (int) spreads.get(n)[1]);
                    else mc.stop();
                }
            } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
        }
        private static void shot(net.minecraft.client.Minecraft mc, java.nio.file.Path out, String name) throws java.io.IOException {
            try (var image = net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())) { image.writeToFile(out.resolve(name)); }
            log(name);
        }
    }
}
