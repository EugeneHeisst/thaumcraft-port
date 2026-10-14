package dev.eugheist.tc4modern;

import baubles.api.BaubleType;
import baubles.api.IBauble;
import java.util.Collections;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Baubles InventoryBaubles and PlayerHandler: four slots (amulet, ring, ring, belt) per player, saved as the original
 * "Baubles.Inventory" list in the player's persistent data, ticked every player tick and dropped on death.
 */
@EventBusSubscriber(modid = TC4Modern.MODID)
public final class BaublesInventory implements Container {
    static final String TAG = "Baubles.Inventory";
    /** Storage used before the Baubles port (amulet, ring, ring, belt order is the same). */
    private static final String LEGACY_TAG = "TC4Accessories";
    // Entity.equals compares numeric entity IDs, which match on the client and integrated server.
    // A map keyed by Player therefore shares predicted client mutations with authoritative server slots.
    // A transient attachment belongs to the actual player instance; legacy persistent NBT stays unchanged.
    static final net.neoforged.neoforge.registries.DeferredHolder<net.neoforged.neoforge.attachment.AttachmentType<?>,
        net.neoforged.neoforge.attachment.AttachmentType<BaublesInventory>> STORAGE =
        net.neoforged.neoforge.registries.DeferredHolder.create(
            net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ATTACHMENT_TYPES, AlchemyContent.id("baubles_inventory"));

    @EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD)
    static final class Registration {
        @SubscribeEvent static void register(net.neoforged.neoforge.registries.RegisterEvent event) {
            event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.ATTACHMENT_TYPES,
                helper -> helper.register(STORAGE.getId(), net.neoforged.neoforge.attachment.AttachmentType
                    .builder(holder -> new BaublesInventory((Player) holder)).build()));
        }
    }

    private final Player player;
    private final NonNullList<ItemStack> stacks = NonNullList.withSize(4, ItemStack.EMPTY);
    private final NonNullList<ItemStack> lastSent = NonNullList.withSize(4, ItemStack.EMPTY);
    private boolean sentInitialState;

    private BaublesInventory(Player player) {
        this.player = player;
        load();
    }

    public static BaublesInventory of(Player player) {
        return player.getData(STORAGE);
    }

    private void load() {
        var data = player.getPersistentData();
        if (!data.contains(TAG) && data.contains(LEGACY_TAG)) {
            var legacy = data.getList(LEGACY_TAG, 10);
            for (int i = 0; i < Math.min(4, legacy.size()); i++)
                stacks.set(i, ItemStack.parseOptional(player.registryAccess(), legacy.getCompound(i)));
            data.remove(LEGACY_TAG);
            save();
            return;
        }
        var list = data.getList(TAG, 10);
        for (int i = 0; i < list.size(); i++) {
            var slotTag = list.getCompound(i);
            int slot = slotTag.getByte("Slot") & 255;
            if (slot < stacks.size()) stacks.set(slot, ItemStack.parseOptional(player.registryAccess(), slotTag));
        }
    }

    private void save() {
        var list = new ListTag();
        for (int i = 0; i < stacks.size(); i++) {
            if (stacks.get(i).isEmpty()) continue;
            var slotTag = (CompoundTag) stacks.get(i).save(player.registryAccess(), new CompoundTag());
            slotTag.putByte("Slot", (byte) i);
            list.add(slotTag);
        }
        player.getPersistentData().put(TAG, list);
    }

    /** InventoryBaubles.isItemValidForSlot. */
    public static boolean fits(int slot, ItemStack stack, Player player) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IBauble bauble) || !bauble.canEquip(stack, player)) return false;
        var type = bauble.getBaubleType(stack);
        return type == BaubleType.TRINKET ? slot >= 0 && slot < 4 : slot == 0 ? type == BaubleType.AMULET : slot == 1 || slot == 2 ? type == BaubleType.RING : slot == 3 && type == BaubleType.BELT;
    }

    /** ItemRing.onItemRightClick: wear the stack in the first free fitting slot. */
    public static boolean equipFromHand(Player player, ItemStack held) {
        var baubles = of(player);
        for (int i = 0; i < baubles.getContainerSize(); i++) {
            if (!baubles.getItem(i).isEmpty() || !fits(i, held, player)) continue;
            baubles.setItem(i, held.copy());
            if (!player.getAbilities().instabuild) held.setCount(0);
            return true;
        }
        return false;
    }

    @Override public int getContainerSize() { return stacks.size(); }
    @Override public boolean isEmpty() { return stacks.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return slot < stacks.size() ? stacks.get(slot) : ItemStack.EMPTY; }
    @Override public ItemStack removeItem(int slot, int amount) {
        var current = getItem(slot);
        if (current.isEmpty()) return ItemStack.EMPTY;
        var taken = current.split(amount);
        if (taken.getItem() instanceof IBauble bauble && !player.level().isClientSide) bauble.onUnequipped(taken, player);
        setChanged();
        return taken;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        var current = getItem(slot);
        stacks.set(slot, ItemStack.EMPTY);
        save();
        return current;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        var old = stacks.get(slot);
        boolean server = !player.level().isClientSide;
        // Menu resyncs put back the very same stack; only a real swap counts as unequip/equip.
        boolean swapped = !ItemStack.isSameItemSameComponents(old, stack);
        if (server && swapped && old.getItem() instanceof IBauble bauble) bauble.onUnequipped(old, player);
        stacks.set(slot, stack);
        if (server && swapped && stack.getItem() instanceof IBauble bauble) bauble.onEquipped(stack, player);
        setChanged();
    }
    @Override public int getMaxStackSize() { return 1; }
    @Override public void setChanged() { save(); }
    @Override public boolean stillValid(Player player) { return true; }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return fits(slot, stack, player); }
    @Override public void clearContent() { Collections.fill(stacks, ItemStack.EMPTY); save(); }

    /** EventHandlerEntity.playerTick: onWornTick for every worn bauble. */
    @SubscribeEvent static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event) {
        var player = event.getEntity();
        if (player.level().isClientSide) return;
        var baubles = of(player);
        for (int i = 0; i < baubles.getContainerSize(); i++) {
            var stack = baubles.getItem(i);
            if (stack.getItem() instanceof IBauble bauble) bauble.onWornTick(stack, player);
        }
        // The menu already synchronizes these slots. Outside it, send a snapshot only when changed.
        if (player instanceof net.minecraft.server.level.ServerPlayer server && !(server.containerMenu instanceof BaublesMenu)) {
            boolean changed = !baubles.sentInitialState;
            for (int i = 0; i < 4; i++) changed |= !ItemStack.matches(baubles.stacks.get(i), baubles.lastSent.get(i));
            if (changed) {
                // Worn items that change themselves (e.g. a wireless terminal's power or grid) must persist too.
                if (baubles.sentInitialState) baubles.save();
                for (int i = 0; i < 4; i++) baubles.lastSent.set(i, baubles.stacks.get(i).copy());
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(server, new BaublesSyncPayload(
                    baubles.lastSent.get(0).copy(), baubles.lastSent.get(1).copy(),
                    baubles.lastSent.get(2).copy(), baubles.lastSent.get(3).copy()));
                baubles.sentInitialState = true;
            }
        }
    }

    /** EventHandlerEntity.playerDeath / InventoryBaubles.dropItemsAt, unless keepInventory. */
    @SubscribeEvent static void drops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide
            || player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY)) return;
        var baubles = of(player);
        var random = player.getRandom();
        for (int i = 0; i < baubles.getContainerSize(); i++) {
            var stack = baubles.getItem(i);
            if (stack.isEmpty()) continue;
            var item = new ItemEntity(player.level(), player.getX(), player.getEyeY(), player.getZ(), stack.copy());
            item.setPickUpDelay(40);
            float speed = random.nextFloat() * .5f, angle = random.nextFloat() * (float) Math.PI * 2;
            item.setDeltaMovement(-Mth.sin(angle) * speed, .2f, Mth.cos(angle) * speed);
            event.getDrops().add(item);
            baubles.stacks.set(i, ItemStack.EMPTY);
        }
        baubles.save();
    }

    /** The client rebuilds its player on dimension change, respawn and login: send the worn slots again. */
    @SubscribeEvent static void resyncDimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent event) { of(event.getEntity()).sentInitialState = false; }
    @SubscribeEvent static void resyncRespawn(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event) { of(event.getEntity()).sentInitialState = false; }
    @SubscribeEvent static void resyncLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) { of(event.getEntity()).sentInitialState = false; }

    /** The original kept a separate .baub file per player, so the slots followed the player through respawn. */
    @SubscribeEvent static void clone(net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event) {
        var original = event.getOriginal();
        of(original).save();
        var list = original.getPersistentData().getList(TAG, 10).copy();
        event.getEntity().getPersistentData().put(TAG, list);
        event.getEntity().removeData(STORAGE);
    }
}
