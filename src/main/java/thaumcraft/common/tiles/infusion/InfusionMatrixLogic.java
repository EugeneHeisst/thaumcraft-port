package thaumcraft.common.tiles.infusion;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Server half of the original TileInfusionMatrix (TC4 4.2.3.5): activation, craftingStart, craftCycle with
 * the 21 instability outcomes, ingredient consumption, craftingFinish and getSurroundings symmetry.
 * The platform supplies pedestal storage, essentia draining, research checks, FX and block events.
 */
public final class InfusionMatrixLogic {
    public final List<BlockPos> pedestals = new ArrayList<>();
    public boolean active, crafting, checkSurroundings = true;
    public int symmetry, instability, count, countDelay = 10, itemCount;
    public AspectList recipeEssentia = new AspectList();
    /** Legacy potion identity was metadata; modern potion contents must survive the consumption check too. */
    public static boolean sameIngredient(ItemStack actual,ItemStack expected){return (dev.eugheist.tc4modern.NoteData.tag(expected).getBoolean("TC4AnyRawFish")?java.util.Set.of(net.minecraft.world.item.Items.COD,net.minecraft.world.item.Items.SALMON,net.minecraft.world.item.Items.TROPICAL_FISH,net.minecraft.world.item.Items.PUFFERFISH).contains(actual.getItem()):ItemStack.isSameItem(actual,expected))&&(!expected.has(net.minecraft.core.component.DataComponents.POTION_CONTENTS)||java.util.Objects.equals(actual.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS),expected.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS)));}
    public List<ItemStack> recipeIngredients;
    public ItemStack recipeOutput;
    public Holder<Enchantment> recipeEnchantment;
    public ItemStack recipeInput;
    public String recipePlayer;
    public int recipeInstability, recipeXP, recipeType;

    public interface Host {
        Level level();
        BlockPos pos();
        boolean isPedestal(BlockPos p);
        boolean isPillar(BlockPos p);
        ItemStack pedestalItem(BlockPos p);
        void setPedestalItem(BlockPos p, ItemStack stack);
        /** Vanilla skull/IInfusionStabiliser check. */
        boolean stabilises(BlockPos p);
        boolean drainEssentia(Aspect aspect);
        boolean researchComplete(Player player, String key);
        InfusionRecipeMatch findRecipe(List<ItemStack> components, ItemStack central, Player player);
        void sync();
        void sound(String event, float volume, float pitch);
        void fxSource(BlockPos from, int entityId);
        void fxZap(double x0, double y0, double z0, double x1, double y1, double z1);
        /** TilePedestal block event 11: particleCount(5) x blockSparkle(0xC000C0). */
        void fxSparkle(BlockPos pedestal);
        /** TilePedestal block event 12: particleCount(10) x blockSparkle(random colours), plus the port's completion cue. */
        void fxFinish(BlockPos pedestal);
        void fluxGoo(BlockPos p);
        void fluxGas(BlockPos p);
        void taint(LivingEntity e);
        void visExhaust(LivingEntity e);
        void warp(Player p, int amount, boolean sticky);
        void playerCrafted(Player p, ItemStack result);
    }
    /** One matched recipe from either the infusion or the enchantment list. */
    public record InfusionRecipeMatch(int type, List<ItemStack> components, ItemStack output, Holder<Enchantment> enchantment, int instability, AspectList essentia, int xp) {}

    public void serverTick(Host host) {
        ++count;
        if (checkSurroundings) { checkSurroundings = false; getSurroundings(host); }
        if (count % (crafting ? 20 : 100) == 0 && !validLocation(host)) { active = false; host.sync(); return; }
        if (active && crafting && count % countDelay == 0) { craftCycle(host); host.sync(); }
    }
    public boolean validLocation(Host host) {
        BlockPos p = host.pos();
        return host.isPedestal(p.offset(0, -2, 0)) && host.isPillar(p.offset(1, -2, 1)) && host.isPillar(p.offset(1, -2, -1)) && host.isPillar(p.offset(-1, -2, -1)) && host.isPillar(p.offset(-1, -2, 1));
    }
    /** onWandRightClick: first activate a valid altar, then start crafting. */
    public boolean wand(Host host, Player player) {
        if (active && !crafting) { craftingStart(host, player); return true; }
        if (!active && validLocation(host)) { active = true; host.sync(); return true; }
        return false;
    }
    public void craftingStart(Host host, Player player) {
        if (!validLocation(host)) { active = false; host.sync(); return; }
        getSurroundings(host);
        recipeInput = null;
        BlockPos center = host.pos().offset(0, -2, 0);
        if (!host.pedestalItem(center).isEmpty()) recipeInput = host.pedestalItem(center).copy();
        if (recipeInput == null) return;
        List<ItemStack> components = new ArrayList<>();
        for (BlockPos cc : pedestals) if (host.isPedestal(cc) && !host.pedestalItem(cc).isEmpty()) components.add(host.pedestalItem(cc).copy());
        if (components.isEmpty()) return;
        InfusionRecipeMatch recipe = host.findRecipe(components, recipeInput, player);
        if (recipe == null) return;
        recipeType = recipe.type();
        recipeIngredients = new ArrayList<>();
        for (ItemStack ing : recipe.components()) recipeIngredients.add(ing.copy());
        recipeOutput = recipe.output() == null ? null : recipe.output().copy();
        recipeEnchantment = recipe.enchantment();
        recipeInstability = recipe.instability();
        recipeEssentia = recipe.essentia().copy();
        recipeXP = recipe.xp();
        recipePlayer = player.getGameProfile().getName();
        instability = symmetry + recipeInstability;
        crafting = true;
        host.sound("craftstart", .5f, 1f);
        host.sync();
    }
    public void craftCycle(Host host) {
        Level world = host.level();
        var rand = world.random;
        BlockPos pos = host.pos();
        boolean valid = false;
        BlockPos center = pos.offset(0, -2, 0);
        ItemStack i2 = host.pedestalItem(center);
        if (host.isPedestal(center) && !i2.isEmpty() && ItemStack.isSameItemSameComponents(i2, recipeInput)) valid = true;
        if (!valid || instability > 0 && rand.nextInt(500) <= instability) {
            switch (rand.nextInt(21)) {
                case 0, 2, 10, 13 -> inEvEjectItem(host, 0);
                case 6, 17 -> inEvEjectItem(host, 1);
                case 1, 11 -> inEvEjectItem(host, 2);
                case 3, 8, 14 -> inEvZap(host, false);
                case 5, 16 -> inEvHarm(host, false);
                case 12 -> inEvZap(host, true);
                case 19 -> inEvEjectItem(host, 3);
                case 7 -> inEvEjectItem(host, 4);
                case 4, 15 -> inEvEjectItem(host, 5);
                case 18 -> inEvHarm(host, true);
                case 9 -> world.explode(null, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 1.5f + rand.nextFloat(), Level.ExplosionInteraction.NONE);
                case 20 -> inEvWarp(host);
                default -> {}
            }
            if (valid) return;
        }
        if (!valid) {
            instability = 0; crafting = false; recipeEssentia = new AspectList();
            host.sync();
            host.sound("craftfail", 1f, .6f);
            return;
        }
        if (recipeType == 1 && recipeXP > 0) {
            List<Player> targets = world.getEntitiesOfClass(Player.class, new AABB(pos).inflate(10));
            if (!targets.isEmpty()) {
                for (Player target : targets) {
                    if (target.experienceLevel <= 0) continue;
                    target.giveExperienceLevels(-1);
                    --recipeXP;
                    target.hurt(world.damageSources().magic(), rand.nextInt(2));
                    host.fxSource(pos, target.getId());
                    world.playSound(null, target.blockPosition(), net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.PLAYERS, 1f, 2f + rand.nextFloat() * .4f);
                    countDelay = 20;
                    return;
                }
                Aspect[] ingEss = recipeEssentia.getAspects();
                if (ingEss != null && ingEss.length > 0 && rand.nextInt(3) == 0) {
                    Aspect as = ingEss[rand.nextInt(ingEss.length)];
                    recipeEssentia.add(as, 1);
                    if (rand.nextInt(50 - recipeInstability * 2) == 0) ++instability;
                    if (instability > 25) instability = 25;
                    host.sync();
                }
            }
            return;
        }
        if (recipeType == 1 && recipeXP == 0) countDelay = 10;
        if (recipeEssentia.visSize() > 0) {
            for (Aspect aspect : recipeEssentia.getAspects()) {
                if (recipeEssentia.getAmount(aspect) <= 0) continue;
                if (host.drainEssentia(aspect)) { recipeEssentia.reduce(aspect, 1); host.sync(); return; }
                if (rand.nextInt(100 - recipeInstability * 3) == 0) ++instability;
                if (instability > 25) instability = 25;
                host.sync();
            }
            checkSurroundings = true;
            return;
        }
        if (!recipeIngredients.isEmpty()) {
            for (int a = 0; a < recipeIngredients.size(); ++a) {
                for (BlockPos cc : pedestals) {
                    ItemStack stack = host.pedestalItem(cc);
                    if (!host.isPedestal(cc) || stack.isEmpty() || !sameIngredient(stack, recipeIngredients.get(a))) continue;
                    if (itemCount == 0) {
                        itemCount = 5;
                        host.fxSource(cc, 0);
                    } else if (itemCount-- <= 1) {
                        ItemStack is = stack.getCraftingRemainingItem();
                        host.setPedestalItem(cc, is.isEmpty() ? ItemStack.EMPTY : is.copy());
                        recipeIngredients.remove(a);
                    }
                    return;
                }
                Aspect[] ingEss = recipeEssentia.getAspects();
                if (ingEss == null || ingEss.length <= 0 || rand.nextInt(1 + a) != 0) continue;
                Aspect as = ingEss[rand.nextInt(ingEss.length)];
                recipeEssentia.add(as, 1);
                if (rand.nextInt(50 - recipeInstability * 2) == 0) ++instability;
                if (instability > 25) instability = 25;
                host.sync();
            }
            return;
        }
        instability = 0;
        crafting = false;
        craftingFinish(host);
        recipeOutput = null;
        host.sync();
    }
    void inEvZap(Host host, boolean all) {
        Level world = host.level(); BlockPos pos = host.pos();
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(10))) {
            host.fxZap(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ());
            target.hurt(world.damageSources().magic(), 4 + world.random.nextInt(4));
            if (!all) break;
        }
    }
    void inEvHarm(Host host, boolean all) {
        Level world = host.level();
        for (LivingEntity target : world.getEntitiesOfClass(LivingEntity.class, new AABB(host.pos()).inflate(10))) {
            if (world.random.nextBoolean()) host.taint(target); else host.visExhaust(target);
            if (!all) break;
        }
    }
    void inEvWarp(Host host) {
        Level world = host.level();
        List<Player> targets = world.getEntitiesOfClass(Player.class, new AABB(host.pos()).inflate(10));
        if (targets.isEmpty()) return;
        Player target = targets.get(world.random.nextInt(targets.size()));
        if (world.random.nextFloat() < .25f) host.warp(target, 1, true);
        else host.warp(target, 1 + world.random.nextInt(5), false);
    }
    void inEvEjectItem(Host host, int type) {
        Level world = host.level(); BlockPos pos = host.pos();
        for (int q = 0; q < 50 && !pedestals.isEmpty(); ++q) {
            BlockPos cc = pedestals.get(world.random.nextInt(pedestals.size()));
            if (!host.isPedestal(cc) || host.pedestalItem(cc).isEmpty()) continue;
            if (type < 3 || type == 5) {
                ItemStack drop = host.pedestalItem(cc).copy();
                host.setPedestalItem(cc, ItemStack.EMPTY);
                net.minecraft.world.Containers.dropItemStack(world, cc.getX(), cc.getY() + 1, cc.getZ(), drop);
            } else host.setPedestalItem(cc, ItemStack.EMPTY);
            if (type == 1 || type == 3) { host.fluxGoo(cc.above()); world.playSound(null, cc, net.minecraft.sounds.SoundEvents.GENERIC_SWIM, net.minecraft.sounds.SoundSource.BLOCKS, .3f, 1f); }
            else if (type == 2 || type == 4) { host.fluxGas(cc.above()); world.playSound(null, cc, net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH, net.minecraft.sounds.SoundSource.BLOCKS, .3f, 1f); }
            else if (type == 5) world.explode(null, cc.getX() + .5, cc.getY() + .5, cc.getZ() + .5, 1f, Level.ExplosionInteraction.NONE);
            host.fxSparkle(cc);
            host.fxZap(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, cc.getX() + .5, cc.getY() + 1.5, cc.getZ() + .5);
            return;
        }
    }
    void craftingFinish(Host host) {
        BlockPos center = host.pos().offset(0, -2, 0);
        if (!host.isPedestal(center)) return;
        if (recipeType == 0 && recipeOutput != null) host.setPedestalItem(center, recipeOutput.copy());
        else if (recipeType == 1 && recipeEnchantment != null) {
            ItemStack temp = host.pedestalItem(center).copy();
            int level = EnchantmentHelper.getItemEnchantmentLevel(recipeEnchantment, temp);
            temp.enchant(recipeEnchantment, level + 1);
            host.setPedestalItem(center, temp);
        }
        if (recipePlayer != null) {
            Player p = host.level().getServer() == null ? null : host.level().getServer().getPlayerList().getPlayerByName(recipePlayer);
            if (p != null) host.playerCrafted(p, host.pedestalItem(center));
        }
        recipeEssentia = new AspectList();
        host.sync();
        host.fxFinish(center);
    }
    /** getSurroundings: pedestals within 8 (first per column, 1..10 below) and stabilisers within 12. */
    public void getSurroundings(Host host) {
        List<BlockPos> stuff = new ArrayList<>();
        pedestals.clear();
        BlockPos pos = host.pos();
        for (int xx = -12; xx <= 12; ++xx) {
            for (int zz = -12; zz <= 12; ++zz) {
                boolean skip = false;
                for (int yy = -5; yy <= 10; ++yy) {
                    if (xx == 0 && zz == 0) continue;
                    BlockPos p = new BlockPos(pos.getX() + xx, pos.getY() - yy, pos.getZ() + zz);
                    if (!skip && yy > 0 && Math.abs(xx) <= 8 && Math.abs(zz) <= 8 && host.isPedestal(p)) { pedestals.add(p); skip = true; continue; }
                    if (host.stabilises(p)) stuff.add(p);
                }
            }
        }
        symmetry = 0;
        for (BlockPos cc : pedestals) {
            boolean items = false;
            int x2 = pos.getX() - cc.getX(), z = pos.getZ() - cc.getZ();
            if (host.isPedestal(cc)) { symmetry += 2; if (!host.pedestalItem(cc).isEmpty()) { ++symmetry; items = true; } }
            BlockPos opposite = new BlockPos(pos.getX() + x2, cc.getY(), pos.getZ() + z);
            if (!host.isPedestal(opposite)) continue;
            symmetry -= 2;
            if (host.pedestalItem(opposite).isEmpty() || !items) continue;
            --symmetry;
        }
        float sym = 0f;
        for (BlockPos cc : stuff) {
            int x = pos.getX() - cc.getX(), z = pos.getZ() - cc.getZ();
            if (host.stabilises(cc)) sym += .1f;
            if (!host.stabilises(new BlockPos(pos.getX() + x, cc.getY(), pos.getZ() + z))) continue;
            sym -= .2f;
        }
        symmetry = (int) (symmetry + sym);
    }
    public void save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putBoolean("active", active);
        tag.putBoolean("crafting", crafting);
        tag.putShort("instability", (short) instability);
        recipeEssentia.writeToNBT(tag);
        if (recipeIngredients != null && !recipeIngredients.isEmpty()) {
            ListTag list = new ListTag();
            for (ItemStack s : recipeIngredients) if (!s.isEmpty()) list.add(s.save(registries));
            tag.put("recipein", list);
        }
        if (recipeOutput != null && !recipeOutput.isEmpty()) tag.put("recipeout", recipeOutput.save(registries));
        if (recipeEnchantment != null) tag.putString("recipeench", recipeEnchantment.getRegisteredName());
        if (recipeInput != null && !recipeInput.isEmpty()) tag.put("recipeinput", recipeInput.save(registries));
        tag.putInt("recipeinst", recipeInstability);
        tag.putInt("recipetype", recipeType);
        tag.putInt("recipexp", recipeXP);
        tag.putString("recipeplayer", recipePlayer == null ? "" : recipePlayer);
    }
    public void load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        active = tag.getBoolean("active");
        crafting = tag.getBoolean("crafting");
        instability = tag.getShort("instability");
        recipeEssentia = new AspectList(); recipeEssentia.readFromNBT(tag);
        recipeIngredients = new ArrayList<>();
        for (Tag t : tag.getList("recipein", Tag.TAG_COMPOUND)) ItemStack.parse(registries, t).ifPresent(recipeIngredients::add);
        recipeOutput = tag.contains("recipeout") ? ItemStack.parse(registries, tag.get("recipeout")).orElse(null) : null;
        recipeEnchantment = null;
        if (tag.contains("recipeench")) {
            var key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, net.minecraft.resources.ResourceLocation.parse(tag.getString("recipeench")));
            recipeEnchantment = registries.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(key).orElse(null);
        }
        recipeInput = tag.contains("recipeinput") ? ItemStack.parse(registries, tag.get("recipeinput")).orElse(null) : null;
        recipeInstability = tag.getInt("recipeinst");
        recipeType = tag.getInt("recipetype");
        recipeXP = tag.getInt("recipexp");
        recipePlayer = tag.getString("recipeplayer");
        if (recipePlayer.isEmpty()) recipePlayer = null;
    }
}
