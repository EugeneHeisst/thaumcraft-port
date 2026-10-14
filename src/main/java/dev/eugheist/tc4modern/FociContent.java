package dev.eugheist.tc4modern;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * Wand foci (ItemFocusBasic / ItemWandCasting focus handling / WandManager.changeFocus and cooldowns) and the
 * fire, frost and shock foci with their EntityEmber and EntityFrostShard projectiles.
 * Upgrades live in the original "upgrade" NBT list and are applied by the focal manipulator (FocalManipulator).
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class FociContent {
    private FociContent() {}
    private static DeferredHolder<Item, Item> item(String n) { return DeferredHolder.create(Registries.ITEM, AlchemyContent.id(n)); }
    public static final DeferredHolder<Item, Item> FIRE = item("focus_fire");
    public static final DeferredHolder<Item, Item> FROST = item("focus_frost");
    public static final DeferredHolder<Item, Item> SHOCK = item("focus_shock");
    public static final DeferredHolder<EntityType<?>, EntityType<FociEntities.Ember>> EMBER = DeferredHolder.create(Registries.ENTITY_TYPE, AlchemyContent.id("ember"));
    public static final DeferredHolder<EntityType<?>, EntityType<FociEntities.FrostShard>> FROST_SHARD = DeferredHolder.create(Registries.ENTITY_TYPE, AlchemyContent.id("frost_shard"));

    @SubscribeEvent
    static void register(RegisterEvent event) {
        event.register(Registries.ITEM, h -> {
            h.register(FIRE.getId(), new FireFocus());
            h.register(FROST.getId(), new FrostFocus());
            h.register(SHOCK.getId(), new ShockFocus());
        });
        event.register(Registries.ENTITY_TYPE, h -> {
            h.register(EMBER.getId(), EntityType.Builder.<FociEntities.Ember>of(FociEntities.Ember::new, MobCategory.MISC).sized(.25f, .25f).clientTrackingRange(4).updateInterval(10).build("ember"));
            h.register(FROST_SHARD.getId(), EntityType.Builder.<FociEntities.FrostShard>of(FociEntities.FrostShard::new, MobCategory.MISC).sized(.25f, .25f).clientTrackingRange(4).updateInterval(10).build("frost_shard"));
        });
    }

    // ---------------- ItemFocusBasic ----------------
    public abstract static class Focus extends Item {
        protected Focus() { super(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)); }
        public abstract AspectList getVisCost(ItemStack focus);
        public boolean isVisCostPerTick(ItemStack focus) { return false; }
        /** ItemFocusBasic.getAnimation: WAVE by default. */
        public Animation getAnimation(ItemStack focus) { return Animation.WAVE; }
        public int getActivationCooldown(ItemStack focus) { return 0; }
        public abstract String getSortingHelperBase();
        public int getFocusColor(ItemStack focus) { return 0; }
        public String getDepthTexture(ItemStack focus) { return null; }
        public String getSortingHelper(ItemStack focus) { StringBuilder s = new StringBuilder(getSortingHelperBase()); for (short id : getAppliedUpgrades(focus)) s.append(id); return s.toString(); }
        /** @return true if handled (onFocusRightClick returned a stack). */
        public abstract boolean onFocusRightClick(ItemStack wand, Level level, Player player);
        public void onUsingFocusTick(ItemStack wand, Player player, int count) {}
        public void onPlayerStoppedUsingFocus(ItemStack wand, Level level, Player player, int count) {}
        public short[] getAppliedUpgrades(ItemStack focus) {
            short[] l = {-1, -1, -1, -1, -1};
            var data = focus.get(DataComponents.CUSTOM_DATA);
            if (data == null) return l;
            var list = data.copyTag().getList("upgrade", 10);
            for (int j = 0; j < list.size() && j < 5; ++j) l[j] = list.getCompound(j).getShort("id");
            return l;
        }
        public int getUpgradeLevel(ItemStack focus, int typeId) { int n = 0; for (short id : getAppliedUpgrades(focus)) if (id == typeId) n++; return n; }
        public boolean isUpgradedWith(ItemStack focus, int typeId) { return getUpgradeLevel(focus, typeId) > 0; }
        /** getMaxAreaSize: the largest architect area radius on any axis. */
        public int getMaxAreaSize(ItemStack focus) { return 1; }
        /** getPossibleUpgradesByRank: upgrade ids offered by the focal manipulator at rank 1-5, null when none. */
        public int[] getPossibleUpgradesByRank(ItemStack focus, int rank) { return null; }
        public boolean canApplyUpgrade(ItemStack focus, Player player, int typeId, int rank) { return true; }
        /** applyUpgrade: fills the rank's slot; the whole five-entry list (with -1 gaps) is rewritten as in the original. */
        public boolean applyUpgrade(ItemStack focus, int typeId, int rank) {
            short[] upgrades = getAppliedUpgrades(focus);
            if (rank < 1 || rank > 5 || upgrades[rank - 1] != -1) return false;
            upgrades[rank - 1] = (short) typeId;
            CustomData.update(DataComponents.CUSTOM_DATA, focus, tag -> {
                var list = new net.minecraft.nbt.ListTag();
                for (short id : upgrades) { var f = new CompoundTag(); f.putShort("id", id); list.add(f); }
                tag.put("upgrade", list);
            });
            return true;
        }
        @Override public void appendHoverText(ItemStack stack, TooltipContext ctx, List<Component> list, TooltipFlag flag) {
            AspectList al = getVisCost(stack);
            if (al != null && al.size() > 0) {
                list.add(Component.translatable(isVisCostPerTick(stack) ? "item.Focus.cost2" : "item.Focus.cost1"));
                var fmt = new java.text.DecimalFormat("#####.##");
                for (Aspect a : al.getAspectsSorted()) list.add(Component.literal(" §" + a.getChatcolor() + a.getName() + "§r x " + fmt.format(al.getAmount(a) / 100f)));
            }
            // addFocusInformation: each applied upgrade once, with its level when taken more than once.
            var levels = new java.util.LinkedHashMap<Short, Integer>();
            for (short id : getAppliedUpgrades(stack)) if (id >= 0) levels.merge(id, 1, Integer::sum);
            for (var e : levels.entrySet()) {
                var type = upgradeType(e.getKey());
                if (type == null) continue;
                var line = Component.translatable(type.nameKey());
                if (e.getValue() > 1) line.append(" ").append(Component.translatable("enchantment.level." + e.getValue()));
                list.add(line.withStyle(net.minecraft.ChatFormatting.DARK_PURPLE));
            }
        }
    }

    // ---------------- FocusUpgradeType ----------------
    /** FocusUpgradeType: id, icon textures/foci/<key>.png, focus.upgrade.<key>.name/.text and the aspects one rank costs. */
    public record UpgradeType(int id, String key, AspectList aspects) {
        public net.minecraft.resources.ResourceLocation icon() { return AlchemyContent.id("textures/foci/" + key + ".png"); }
        public String nameKey() { return "focus.upgrade." + key + ".name"; }
        public String textKey() { return "focus.upgrade." + key + ".text"; }
    }
    private static final UpgradeType[] UPGRADES = new UpgradeType[21];
    private static void upgrade(int id, String key, AspectList aspects) { UPGRADES[id] = new UpgradeType(id, key, aspects); }
    static {
        upgrade(0, "potency", new AspectList().add(Aspect.WEAPON, 1));
        upgrade(1, "frugal", new AspectList().add(Aspect.HUNGER, 1));
        upgrade(2, "treasure", new AspectList().add(Aspect.GREED, 1));
        upgrade(3, "enlarge", new AspectList().add(Aspect.TRAVEL, 1));
        upgrade(4, "alchemistsfire", new AspectList().add(Aspect.ENERGY, 1).add(Aspect.SLIME, 1));
        upgrade(5, "alchemistsfrost", new AspectList().add(Aspect.COLD, 1).add(Aspect.TRAP, 1));
        upgrade(6, "architect", new AspectList().add(Aspect.CRAFT, 1));
        upgrade(7, "extend", new AspectList().add(Aspect.EXCHANGE, 1));
        upgrade(8, "silktouch", new AspectList().add(Aspect.GREED, 1));
        upgrade(9, "fireball", new AspectList().add(Aspect.DARKNESS, 1));
        upgrade(10, "firebeam", new AspectList().add(Aspect.ENERGY, 1).add(Aspect.AIR, 1));
        upgrade(11, "scattershot", new AspectList().add(Aspect.COLD, 1).add(Aspect.WEAPON, 1));
        upgrade(12, "iceboulder", new AspectList().add(Aspect.COLD, 1).add(Aspect.CRYSTAL, 1));
        upgrade(13, "batbombs", new AspectList().add(Aspect.ENERGY, 1).add(Aspect.TRAP, 1));
        upgrade(14, "devilbats", new AspectList().add(Aspect.ARMOR, 1));
        upgrade(15, "nightshade", new AspectList().add(Aspect.LIFE, 1).add(Aspect.POISON, 1).add(Aspect.MAGIC, 1));
        upgrade(16, "seeker", new AspectList().add(Aspect.SENSES, 1).add(Aspect.MIND, 1));
        upgrade(17, "chainlightning", new AspectList().add(Aspect.WEATHER, 1));
        upgrade(18, "earthshock", new AspectList().add(Aspect.WEATHER, 1));
        upgrade(19, "vampirebats", new AspectList().add(Aspect.HUNGER, 1).add(Aspect.LIFE, 1));
        upgrade(20, "dowsing", new AspectList().add(Aspect.MINE, 1));
    }
    public static UpgradeType upgradeType(int id) { return id >= 0 && id < UPGRADES.length ? UPGRADES[id] : null; }
    /** Shorthand for the rank tables: ranks[rank - 1] lists the ids offered at that rank. */
    static int[] byRank(int rank, int[]... ranks) { return rank >= 1 && rank <= ranks.length ? ranks[rank - 1] : null; }
    public enum Animation { WAVE, CHARGE }
    /** FocusUpgradeType ids used by the ported foci. */
    static final int POTENCY = 0, FRUGAL = 1, ENLARGE = 3, ARCHITECT = 6, ALCHEMISTSFIRE = 4, ALCHEMISTSFROST = 5, FIREBALL = 9, FIREBEAM = 10,
        SCATTERSHOT = 11, ICEBOULDER = 12, CHAINLIGHTNING = 17, EARTHSHOCK = 18;

    // ---------------- ItemWandCasting focus plumbing ----------------
    public static ItemStack getFocusItem(ItemStack wand) {
        var data = wand.get(DataComponents.CUSTOM_DATA);
        if (data == null) return ItemStack.EMPTY;
        var tag = data.copyTag();
        if (!tag.contains("focus")) return ItemStack.EMPTY;
        return ItemStack.parseOptional(registries(), tag.getCompound("focus"));
    }
    static net.minecraft.core.HolderLookup.Provider registries() {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.registryAccess();
        return ClientRegistries.get();
    }
    static final class ClientRegistries { static net.minecraft.core.HolderLookup.Provider get() { var l = net.minecraft.client.Minecraft.getInstance().level; return l == null ? net.minecraft.core.RegistryAccess.EMPTY : l.registryAccess(); } }
    public static Focus getFocus(ItemStack wand) { return getFocusItem(wand).getItem() instanceof Focus f ? f : null; }
    public static void setFocus(ItemStack wand, ItemStack focus) {
        CustomData.update(DataComponents.CUSTOM_DATA, wand, tag -> {
            if (focus == null || focus.isEmpty()) tag.remove("focus");
            else tag.put("focus", focus.save(registries()));
        });
    }
    public static int getFocusPotency(ItemStack wand) { var f = getFocus(wand); return f == null ? 0 : f.getUpgradeLevel(getFocusItem(wand), POTENCY) + (WandParts.rod(wand).runes()?1:0); }
    /** ItemWandCasting.consumeAllVis(crafting=false): centi-vis amounts times the cap/discount/frugal modifier (min 0.1). */
    public static boolean consumeAllVis(ItemStack wand, Player player, AspectList cost, boolean doit) {
        if (!WandData.isWand(wand) || cost == null) return false;
        var tag = NoteData.tag(wand);
        var f = getFocus(wand);
        int frugal = f == null ? 0 : f.getUpgradeLevel(getFocusItem(wand), FRUGAL);
        Map<Aspect, Integer> need = new HashMap<>();
        for (Aspect a : cost.getAspects()) {
            float mod = Math.max(WandData.craftingModifier(wand, player, a) - frugal / 10f, .1f);
            int amount = (int) (cost.getAmount(a) * mod);
            if (thaumcraft.common.lib.vis.VisStorage.getVis(tag, a) < amount) return false;
            need.put(a, amount);
        }
        if (doit) {
            for (var e : need.entrySet()) thaumcraft.common.lib.vis.VisStorage.storeVis(tag, e.getKey(), thaumcraft.common.lib.vis.VisStorage.getVis(tag, e.getKey()) - e.getValue());
            wand.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
        return true;
    }
    private static final Map<Integer, Long> COOLDOWN_CLIENT = new HashMap<>(), COOLDOWN_SERVER = new HashMap<>();
    public static boolean isOnCooldown(LivingEntity e) { var m = e.level().isClientSide ? COOLDOWN_CLIENT : COOLDOWN_SERVER; Long t = m.get(e.getId()); return t != null && t > System.currentTimeMillis(); }
    /** WandManager.getCooldown: remaining client cooldown in seconds. */
    public static float cooldownSeconds(LivingEntity e) { Long t = COOLDOWN_CLIENT.get(e.getId()); return t == null ? 0 : Math.max(0, (t - System.currentTimeMillis()) / 1000f); }
    public static void setCooldown(LivingEntity e, int cd) {
        if (cd == 0) { COOLDOWN_CLIENT.remove(e.getId()); COOLDOWN_SERVER.remove(e.getId()); }
        else (e.level().isClientSide ? COOLDOWN_CLIENT : COOLDOWN_SERVER).put(e.getId(), System.currentTimeMillis() + cd);
    }
    private static final java.util.Set<Integer> FOCUS_USERS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** ItemWandCasting.onItemRightClick focus branch; null when the focus did not handle the click. */
    public static InteractionResultHolder<ItemStack> wandUse(Level level, Player player, InteractionHand hand) {
        ItemStack wand = player.getItemInHand(hand);
        Focus focus = getFocus(wand);
        if (focus == null || isOnCooldown(player)) return null;
        setCooldown(player, focus.getActivationCooldown(getFocusItem(wand)));
        if (!focus.onFocusRightClick(wand, level, player)) return null;
        return InteractionResultHolder.sidedSuccess(wand, level.isClientSide);
    }
    static void startUsing(Player player, InteractionHand hand) { FOCUS_USERS.add(player.getId() * 2 + (player.level().isClientSide ? 1 : 0)); player.startUsingItem(hand); setCooldown(player, -1); }
    public static boolean usingFocus(LivingEntity e) { return FOCUS_USERS.contains(e.getId() * 2 + (e.level().isClientSide ? 1 : 0)); }
    public static void useTick(ItemStack wand, Player player, int count) {
        Focus focus = getFocus(wand);
        if (focus != null && !isOnCooldown(player)) { setCooldown(player, focus.getActivationCooldown(getFocusItem(wand))); focus.onUsingFocusTick(wand, player, count); }
    }
    public static void release(ItemStack wand, Level level, LivingEntity entity, int count) {
        FOCUS_USERS.remove(entity.getId() * 2 + (level.isClientSide ? 1 : 0));
        if (entity instanceof Player p && getFocus(wand) != null) getFocus(wand).onPlayerStoppedUsingFocus(wand, level, p, count);
    }
    static SoundEvent sound(String n) { return SoundEvent.createVariableRangeEvent(AlchemyContent.id(n)); }

    // ---------------- WandManager.changeFocus (inventory foci and18-slot focus pouches) ----------------
    public record FocusChange(String key) implements CustomPacketPayload {
        public static final Type<FocusChange> TYPE = new Type<>(AlchemyContent.id("focus_change"));
        public static final StreamCodec<FriendlyByteBuf, FocusChange> CODEC = StreamCodec.composite(ByteBufCodecs.STRING_UTF8, FocusChange::key, FocusChange::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    @SubscribeEvent
    static void payloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("claude-foci-1");
        registrar.playToServer(FocusChange.TYPE, FocusChange.CODEC, (p, ctx) -> ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player && WandData.isWand(player.getMainHandItem()) && !WandParts.isSceptre(player.getMainHandItem())) changeFocus(player.getMainHandItem(), player, p.key());
        }));
        registrar.playToClient(Zap.TYPE, Zap.CODEC, (p, ctx) -> ctx.enqueueWork(() -> FociFx.zap(p.source(), p.target())));
    }
    private record FocusSlot(int inventorySlot,int pouchSlot,ItemStack focus) {}
    static void changeFocus(ItemStack wand,Player player,String key) {
        var inventory=player.getInventory();var available=new TreeMap<String,FocusSlot>();
        for(int slot=0;slot<inventory.getContainerSize();slot++) {
            var stack=inventory.getItem(slot);
            if(stack.getItem() instanceof Focus focus)available.put(focus.getSortingHelper(stack),new FocusSlot(slot,-1,stack));
            if(stack.getItem() instanceof FocusPouchItem) {
                var contents=FocusPouchItem.contents(stack);
                for(int inside=0;inside<contents.size();inside++)if(contents.get(inside).getItem() instanceof Focus focus)
                    available.put(focus.getSortingHelper(contents.get(inside)),new FocusSlot(slot,inside,contents.get(inside)));
            }
        }
        // WandManager.changeFocus also searched pouches worn as baubles (stored as slot -1 - bauble index).
        var baubles=BaublesInventory.of(player);
        for(int b=0;b<baubles.getContainerSize();b++) {
            var stack=baubles.getItem(b);
            if(!(stack.getItem() instanceof FocusPouchItem))continue;
            var contents=FocusPouchItem.contents(stack);
            for(int inside=0;inside<contents.size();inside++)if(contents.get(inside).getItem() instanceof Focus focus)
                available.put(focus.getSortingHelper(contents.get(inside)),new FocusSlot(-1-b,inside,contents.get(inside)));
        }
        var old=getFocusItem(wand).copy();
        if(key.equals("REMOVE")||available.isEmpty()) {
            if(!old.isEmpty()&&inventory.add(old)){setFocus(wand,null);player.level().playSound(null,player.blockPosition(),sound("cameraticks"),SoundSource.PLAYERS,.3f,.9f);}return;
        }
        String chosen=available.containsKey(key)?key:available.higherKey(key);
        if(chosen==null)chosen=available.firstKey();var slot=available.get(chosen);
        if(slot.pouchSlot<0)inventory.setItem(slot.inventorySlot,old);
        else {var pouch=slot.inventorySlot<0?baubles.getItem(-1-slot.inventorySlot):inventory.getItem(slot.inventorySlot);var contents=FocusPouchItem.contents(pouch);contents.set(slot.pouchSlot,old);FocusPouchItem.store(pouch,contents);if(slot.inventorySlot<0)baubles.setChanged();}
        setFocus(wand,slot.focus.copy());inventory.setChanged();
        player.level().playSound(null,player.blockPosition(),sound("cameraticks"),SoundSource.PLAYERS,.3f,1);
    }
    /** KeyHandler used F; in 1.21 F also swaps hands, so a held wand keeps its hand while F changes the focus. */
    @EventBusSubscriber(modid = TC4Modern.MODID)
    static final class SwapGuard {
        @SubscribeEvent static void swap(net.neoforged.neoforge.event.entity.living.LivingSwapItemsEvent.Hands event) {
            if (WandData.isWand(event.getEntity().getMainHandItem())) event.setCanceled(true);
        }
    }
    /** Client: the key after the current focus' sorting helper, i.e. the next focus in the radial order. */
    static String nextKey(ItemStack wand) { var f = getFocus(wand); return f == null ? "" : f.getSortingHelper(getFocusItem(wand)) + "￿"; }

    // ---------------- foci ----------------
    // The original foci pass the wand stack, not the focus, to getVisCost when charging, so the casting cost is always the
    // base cost (a wand carries no "upgrade" list); only tooltips show the upgraded cost. Kept as in the original.

    /** ItemFocusFire: 0.1 Ignis per tick, 2+potency embers per tick with 15 scatter, fireloop sound every 500 ms; fireball and fire beam upgrades. */
    public static final class FireFocus extends Focus {
        private static final AspectList COST = new AspectList().add(Aspect.FIRE, 10);
        private static final AspectList COST_BEAM = new AspectList().add(Aspect.FIRE, 10).add(Aspect.ORDER, 3);
        private static final AspectList COST_BALL = new AspectList().add(Aspect.FIRE, 66).add(Aspect.ENTROPY, 33);
        private long soundDelay;
        @Override public AspectList getVisCost(ItemStack f) { return isUpgradedWith(f, FIREBEAM) ? COST_BEAM : isUpgradedWith(f, FIREBALL) ? COST_BALL : COST; }
        @Override public int getActivationCooldown(ItemStack f) { return isUpgradedWith(f, FIREBALL) ? 1000 : 0; }
        @Override public boolean isVisCostPerTick(ItemStack f) { return true; }
        @Override public String getSortingHelperBase() { return "AF"; }
        @Override public Animation getAnimation(ItemStack f) { return isUpgradedWith(f, FIREBALL) ? Animation.WAVE : Animation.CHARGE; }
        @Override public int getFocusColor(ItemStack f) { return 15028484; }
        @Override public int[] getPossibleUpgradesByRank(ItemStack f, int rank) {
            return byRank(rank, new int[]{FRUGAL, POTENCY}, new int[]{FRUGAL, POTENCY, ALCHEMISTSFIRE}, new int[]{FRUGAL, POTENCY, FIREBALL, FIREBEAM},
                new int[]{FRUGAL, POTENCY, ALCHEMISTSFIRE}, new int[]{FRUGAL, POTENCY});
        }
        @Override public boolean canApplyUpgrade(ItemStack f, Player player, int type, int rank) {
            return type != ALCHEMISTSFIRE || !isUpgradedWith(f, FIREBALL) || !isUpgradedWith(f, ALCHEMISTSFIRE);
        }
        @Override public boolean onFocusRightClick(ItemStack wand, Level level, Player p) {
            var focus = getFocusItem(wand);
            if (isUpgradedWith(focus, FIREBALL)) {
                if (consumeAllVis(wand, p, getVisCost(wand), !level.isClientSide)) {
                    if (!level.isClientSide) {
                        var orb = new FocusProjectiles.ExplosiveOrb(level, p);
                        orb.strength += getFocusPotency(wand) * .4f;
                        orb.onFire = isUpgradedWith(focus, ALCHEMISTSFIRE);
                        level.addFreshEntity(orb);
                        // playAuxSFX 1009: mob.ghast.fireball at volume 2.
                        level.playSound(null, p.blockPosition(), net.minecraft.sounds.SoundEvents.GHAST_SHOOT, SoundSource.HOSTILE, 2f, (level.random.nextFloat() - level.random.nextFloat()) * .2f + 1f);
                    }
                    p.swing(InteractionHand.MAIN_HAND);
                }
                return true;
            }
            startUsing(p, p.getUsedItemHand() == null ? InteractionHand.MAIN_HAND : p.getUsedItemHand());
            return true;
        }
        @Override public void onUsingFocusTick(ItemStack wand, Player p, int count) {
            if (!consumeAllVis(wand, p, getVisCost(wand), false)) { p.releaseUsingItem(); return; }
            var level = p.level();
            var focus = getFocusItem(wand);
            if (!level.isClientSide && soundDelay < System.currentTimeMillis()) { level.playSound(null, p.blockPosition(), sound("fireloop"), SoundSource.PLAYERS, .33f, 2f); soundDelay = System.currentTimeMillis() + 500L; }
            if (!level.isClientSide && consumeAllVis(wand, p, getVisCost(wand), true)) {
                boolean beam = isUpgradedWith(focus, FIREBEAM);
                for (int a = 0; a < 2 + getFocusPotency(wand); ++a) {
                    var orb = new FociEntities.Ember(level, p, beam ? .25f : 15f);
                    orb.damage = 2 + getFocusPotency(wand);
                    if (beam) { orb.damage += .5f; orb.damage *= 1.5f; orb.duration = 30; }
                    orb.firey = getUpgradeLevel(focus, ALCHEMISTSFIRE);
                    orb.setPos(orb.getX() + orb.getDeltaMovement().x, orb.getY() + orb.getDeltaMovement().y, orb.getZ() + orb.getDeltaMovement().z);
                    level.addFreshEntity(orb);
                }
            }
        }
    }
    /** ItemFocusFrost: one shard (3 + 1.5 potency damage), 200 ms cooldown; scattershot and ice boulder upgrades. */
    public static final class FrostFocus extends Focus {
        private static final AspectList COST = new AspectList().add(Aspect.WATER, 5).add(Aspect.FIRE, 2).add(Aspect.ENTROPY, 2);
        private static final AspectList COST_SCATTER = new AspectList().add(Aspect.WATER, 20).add(Aspect.FIRE, 2).add(Aspect.ENTROPY, 2).add(Aspect.AIR, 5);
        private static final AspectList COST_BOULDER = new AspectList().add(Aspect.WATER, 20).add(Aspect.FIRE, 2).add(Aspect.ENTROPY, 2).add(Aspect.EARTH, 5);
        @Override public AspectList getVisCost(ItemStack f) { return isUpgradedWith(f, SCATTERSHOT) ? COST_SCATTER : isUpgradedWith(f, ICEBOULDER) ? COST_BOULDER : COST; }
        @Override public int getActivationCooldown(ItemStack f) { return isUpgradedWith(f, SCATTERSHOT) || isUpgradedWith(f, ICEBOULDER) ? 500 : 200; }
        @Override public String getSortingHelperBase() { return "BF"; }
        @Override public int getFocusColor(ItemStack f) { return 5204428; }
        @Override public int[] getPossibleUpgradesByRank(ItemStack f, int rank) {
            return byRank(rank, new int[]{FRUGAL, POTENCY, ALCHEMISTSFROST}, new int[]{FRUGAL, POTENCY}, new int[]{FRUGAL, POTENCY, SCATTERSHOT, ICEBOULDER, ALCHEMISTSFROST},
                new int[]{FRUGAL, POTENCY}, new int[]{FRUGAL, POTENCY, ALCHEMISTSFROST});
        }
        @Override public boolean onFocusRightClick(ItemStack wand, Level level, Player p) {
            if (!level.isClientSide && consumeAllVis(wand, p, getVisCost(wand), true)) {
                var focus = getFocusItem(wand);
                int frosty = getUpgradeLevel(focus, ALCHEMISTSFROST);
                FociEntities.FrostShard shard = null;
                if (isUpgradedWith(focus, SCATTERSHOT)) {
                    for (int a = 0; a < 5 + getFocusPotency(wand) * 2; ++a) {
                        shard = new FociEntities.FrostShard(level, p, 8f);
                        shard.setDamage(1f);
                        shard.fragile = true;
                        shard.setFrosty(frosty);
                        level.addFreshEntity(shard);
                    }
                } else if (isUpgradedWith(focus, ICEBOULDER)) {
                    shard = new FociEntities.FrostShard(level, p, 1f);
                    shard.setDamage(4 + getFocusPotency(wand) * 2);
                    shard.bounce = .8;
                    shard.bounceLimit = 6;
                    shard.setFrosty(frosty);
                    level.addFreshEntity(shard);
                } else {
                    shard = new FociEntities.FrostShard(level, p, 1f);
                    shard.setDamage((float) (3.0 + getFocusPotency(wand) * 1.5));
                    shard.setFrosty(frosty);
                    level.addFreshEntity(shard);
                }
                level.playSound(null, shard.blockPosition(), sound("ice"), SoundSource.PLAYERS, .4f, 1f + level.random.nextFloat() * .1f);
            }
            p.swing(InteractionHand.MAIN_HAND);
            return true;
        }
    }
    /** ItemFocusShock: 0.25 Aer per pulse every 250 ms, 4+potency damage to the pointed entity within 20 blocks; chain lightning and earth shock upgrades. */
    public static final class ShockFocus extends Focus {
        private static final AspectList COST = new AspectList().add(Aspect.AIR, 25);
        private static final AspectList COST_CHAIN = new AspectList().add(Aspect.AIR, 40).add(Aspect.WATER, 10);
        private static final AspectList COST_GROUND = new AspectList().add(Aspect.AIR, 75).add(Aspect.EARTH, 25);
        @Override public AspectList getVisCost(ItemStack f) { return isUpgradedWith(f, CHAINLIGHTNING) ? COST_CHAIN : isUpgradedWith(f, EARTHSHOCK) ? COST_GROUND : COST; }
        @Override public int getActivationCooldown(ItemStack f) { return isUpgradedWith(f, CHAINLIGHTNING) ? 500 : isUpgradedWith(f, EARTHSHOCK) ? 1000 : 250; }
        @Override public String getSortingHelperBase() { return "BL"; }
        @Override public Animation getAnimation(ItemStack f) { return isUpgradedWith(f, EARTHSHOCK) ? Animation.WAVE : Animation.CHARGE; }
        @Override public int getFocusColor(ItemStack f) { return 10466239; }
        @Override public int[] getPossibleUpgradesByRank(ItemStack f, int rank) {
            return byRank(rank, new int[]{FRUGAL, POTENCY}, new int[]{FRUGAL, POTENCY}, new int[]{FRUGAL, POTENCY, CHAINLIGHTNING, EARTHSHOCK},
                new int[]{FRUGAL, POTENCY, ENLARGE}, new int[]{FRUGAL, POTENCY, ENLARGE});
        }
        @Override public boolean canApplyUpgrade(ItemStack f, Player player, int type, int rank) {
            return type != ENLARGE || isUpgradedWith(f, CHAINLIGHTNING) || isUpgradedWith(f, EARTHSHOCK);
        }
        @Override public boolean onFocusRightClick(ItemStack wand, Level level, Player p) {
            var focus = getFocusItem(wand);
            if (isUpgradedWith(focus, EARTHSHOCK)) {
                if (consumeAllVis(wand, p, getVisCost(wand), !level.isClientSide)) {
                    if (!level.isClientSide) {
                        var orb = new FocusProjectiles.ShockOrb(level, p);
                        orb.area += getUpgradeLevel(focus, ENLARGE) * 2;
                        orb.damage = (int) (orb.damage + getFocusPotency(wand) * 1.33);
                        level.addFreshEntity(orb);
                        level.playSound(null, orb.blockPosition(), sound("zap"), SoundSource.PLAYERS, 1f, 1f + (level.random.nextFloat() - level.random.nextFloat()) * .2f);
                    }
                    p.swing(InteractionHand.MAIN_HAND);
                }
                return true;
            }
            startUsing(p, InteractionHand.MAIN_HAND);
            return true;
        }
        @Override public void onUsingFocusTick(ItemStack wand, Player p, int count) {
            var level = p.level();
            if (!consumeAllVis(wand, p, getVisCost(wand), !level.isClientSide)) { p.releaseUsingItem(); return; }
            int potency = getFocusPotency(wand);
            Entity pointed = pointedEntity(p, 20);
            if (level.isClientSide) {
                var r = level.random;
                // BlockUtils.getTargetBlock(world, player, false): ten blocks, no liquids; otherwise ten blocks along the look from the feet.
                var look = p.getViewVector(2f);
                double px = p.getX() + look.x * 10, py = p.getY() + look.y * 10, pz = p.getZ() + look.z * 10;
                var mop = FociWorld.target(p, 10);
                if (mop.getType() != HitResult.Type.MISS) {
                    px = mop.getLocation().x; py = mop.getLocation().y; pz = mop.getLocation().z;
                    for (int a = 0; a < 5; ++a) FociFx.sparkle(px + (r.nextFloat() - r.nextFloat()) * .3f, py + (r.nextFloat() - r.nextFloat()) * .3f, pz + (r.nextFloat() - r.nextFloat()) * .3f, 2 + r.nextFloat(), 2, .05f + r.nextFloat() * .05f);
                }
                if (pointed != null) {
                    px = pointed.getX(); py = pointed.getBoundingBox().minY + pointed.getBbHeight() / 2; pz = pointed.getZ();
                    for (int a = 0; a < 5; ++a) FociFx.sparkle(px + (r.nextFloat() - r.nextFloat()) * .6f, py + (r.nextFloat() - r.nextFloat()) * .6f, pz + (r.nextFloat() - r.nextFloat()) * .6f, 2 + r.nextFloat(), 2, .05f + r.nextFloat() * .05f);
                }
                FociFx.shootLightning(p, px, py, pz);
            } else {
                level.playSound(null, p.getX(), p.getY(), p.getZ(), sound("shock"), SoundSource.PLAYERS, .25f, 1f);
                boolean pvp = level.getServer().isPvpAllowed();
                if (pointed instanceof LivingEntity && (!(pointed instanceof Player) || pvp)) {
                    var focus = getFocusItem(wand);
                    int cl = getUpgradeLevel(focus, CHAINLIGHTNING) * 2;
                    pointed.hurt(level.damageSources().playerAttack(p), (cl > 0 ? 6 : 4) + potency);
                    if (cl > 0) {
                        cl += getUpgradeLevel(focus, ENLARGE) * 2;
                        Entity center = pointed;
                        var targets = new java.util.ArrayList<Integer>();
                        targets.add(pointed.getId());
                        while (cl > 0) {
                            --cl;
                            // EntityUtils.getEntitiesInRange: living entities in an 8-block box, excluding the caster.
                            var list = level.getEntitiesOfClass(LivingEntity.class, new AABB(center.getX(), center.getY(), center.getZ(), center.getX(), center.getY(), center.getZ()).inflate(8), e -> e != p);
                            double d = Double.MAX_VALUE;
                            Entity closest = null;
                            for (var e : list) {
                                double dd;
                                if (targets.contains(e.getId()) || e instanceof Player && !pvp || !((dd = e.distanceToSqr(center)) < d)) continue;
                                closest = e;
                                d = dd;
                            }
                            if (closest == null) continue;
                            net.neoforged.neoforge.network.PacketDistributor.sendToPlayersNear((net.minecraft.server.level.ServerLevel) level, null, center.getX(), center.getY(), center.getZ(), 64, new Zap(center.getId(), closest.getId()));
                            targets.add(closest.getId());
                            closest.hurt(level.damageSources().playerAttack(p), 4 + potency);
                            center = closest;
                        }
                    }
                }
            }
        }
    }
    /** PacketFXZap: a shock-coloured bolt between two entities' centres. */
    public record Zap(int source, int target) implements CustomPacketPayload {
        public static final Type<Zap> TYPE = new Type<>(AlchemyContent.id("fx_zap"));
        public static final StreamCodec<FriendlyByteBuf, Zap> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, Zap::source, ByteBufCodecs.VAR_INT, Zap::target, Zap::new);
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    /** EntityUtils.getPointedEntity(world, player, 0, range, 1.1): nearest collidable entity along the look ray. */
    static Entity pointedEntity(Player p, double range) {
        Vec3 eye = p.getEyePosition(), look = p.getViewVector(1f), end = eye.add(look.scale(range));
        var block = p.level().clip(new net.minecraft.world.level.ClipContext(eye, end, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, p));
        double max = block.getLocation().distanceToSqr(eye);
        EntityHitResult hit = net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(p, eye, eye.add(look.scale(Math.sqrt(max))), p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.1), e -> !e.isSpectator() && e.isPickable(), max);
        return hit == null ? null : hit.getEntity();
    }
}
