package dev.eugheist.tc4modern;

import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.*;
import thaumcraft.api.aspects.Aspect;

/** Port of TC4 4.2.3.5 EntityPech with AIPechTradePlayer/AIPechItemEntityGoto: pack, bribery, trading, pack anger and mumbling. */
public final class PechEntity extends Monster implements RangedAttackMob {
    private static final EntityDataAccessor<Integer> TYPE = SynchedEntityData.defineId(PechEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> ANGER = SynchedEntityData.defineId(PechEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> TAMED = SynchedEntityData.defineId(PechEntity.class, EntityDataSerializers.BOOLEAN);
    /** Config.golemDelay default: the item search only runs on these ticks. */
    private static final int GOLEM_DELAY = 5;

    public final SimpleContainer loot = new SimpleContainer(9);
    public boolean trading;
    /** Client jaw animation driven by entity events 16/17/19, decays by 0.75 per tick. */
    public float mumble;
    private LivingEntity angryAt;
    private int chargecount;

    public PechEntity(EntityType<? extends PechEntity> type, Level level) {
        super(type, level);
        xpReward = 5;
        setDropChance(EquipmentSlot.MAINHAND, .2f);
    }

    public static AttributeSupplier.Builder attributes() {
        // Original getTotalArmorValue adds 2 (capped at 20).
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30).add(Attributes.ATTACK_DAMAGE, 6).add(Attributes.MOVEMENT_SPEED, .5).add(Attributes.ARMOR, 2);
    }

    @Override protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
        builder.define(ANGER, 0);
        builder.define(TAMED, false);
    }

    public int variant() { return entityData.get(TYPE); }
    public void variant(int type) { entityData.set(TYPE, Math.max(0, Math.min(2, type))); }
    public boolean tamed() { return entityData.get(TAMED); }
    public void tamed(boolean value) { entityData.set(TAMED, value); }
    public int anger() { return entityData.get(ANGER); }
    private void anger(int value) { entityData.set(ANGER, Math.max(0, Math.min(Short.MAX_VALUE, value))); }

    @Override protected Component getTypeName() {
        return variant() == 0 ? super.getTypeName() : Component.translatable("entity.tc4modern.pech." + variant());
    }

    private boolean holding(Item item) { return getMainHandItem().is(item); }
    private boolean holdingWand() { return getMainHandItem().getItem() instanceof BasicWandItem; }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new TradeGoal());
        // setCombatTask: exactly one attack AI by held item, re-evaluated whenever equipment changes.
        goalSelector.addGoal(2, new RangedAttackGoal(this, .6, 20, 50, 15) {
            @Override public boolean canUse() { return holding(Items.BOW) && super.canUse(); }
            @Override public boolean canContinueToUse() { return holding(Items.BOW) && super.canContinueToUse(); }
        });
        goalSelector.addGoal(2, new RangedAttackGoal(this, .6, 20, 30, 15) {
            @Override public boolean canUse() { return holdingWand() && super.canUse(); }
            @Override public boolean canContinueToUse() { return holdingWand() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(2, new MeleeAttackGoal(this, .6, false) {
            @Override public boolean canUse() { return !holding(Items.BOW) && !holdingWand() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !holding(Items.BOW) && !holdingWand() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(3, new ItemGotoGoal());
        goalSelector.addGoal(4, new AvoidEntityGoal<>(this, Player.class, 8, .5, .6) {
            @Override public boolean canUse() { return !tamed() && super.canUse(); }
            @Override public boolean canContinueToUse() { return !tamed() && super.canContinueToUse(); }
        });
        goalSelector.addGoal(6, new MoveTowardsRestrictionGoal(this, .5));
        goalSelector.addGoal(9, new WaterAvoidingRandomStrollGoal(this, .6));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 3, 1));
        goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8));
        goalSelector.addGoal(11, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    /** AIPechTradePlayer: hold still while a trade GUI is open; losing the conditions ends the trade state. */
    private final class TradeGoal extends Goal {
        TradeGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.JUMP)); }
        @Override public boolean canUse() { return isAlive() && !isInWater() && tamed() && onGround() && !hurtMarked && trading; }
        @Override public void start() { getNavigation().stop(); }
        @Override public void stop() { trading = false; }
    }

    /** AIPechItemEntityGoto: walk to the nearest wanted item within 16 blocks and take it at 1.5 blocks. */
    private final class ItemGotoGoal extends Goal {
        private static final float RANGE = 16;
        private ItemEntity target;
        private int count, failedPathFindingPenalty;
        ItemGotoGoal() { setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK)); }

        @Override public boolean canUse() {
            if (tickCount % GOLEM_DELAY > 0 || --count > 0) return false;
            target = null;
            double best = Double.MAX_VALUE;
            for (var item : level().getEntitiesOfClass(ItemEntity.class, getBoundingBox().inflate(RANGE))) {
                if (!canPickup(item.getItem()) || item.getPersistentData().getBoolean("PechDrop")) continue;
                double distance = item.distanceToSqr(PechEntity.this);
                if (distance < best && distance <= RANGE * RANGE) { best = distance; target = item; }
            }
            return target != null;
        }
        @Override public boolean canContinueToUse() { return target != null && target.isAlive() && !getNavigation().isDone() && target.distanceToSqr(PechEntity.this) < RANGE * RANGE; }
        @Override public void start() { getNavigation().moveTo(target, 1.5); count = 0; }
        @Override public void stop() { target = null; }
        @Override public void tick() {
            getLookControl().setLookAt(target, 30, 30);
            if (getSensing().hasLineOfSight(target) && --count <= 0) {
                count = failedPathFindingPenalty + 4 + random.nextInt(4);
                getNavigation().moveTo(target, 1.5);
                var path = getNavigation().getPath();
                var end = path == null ? null : path.getEndNode();
                failedPathFindingPenalty = end != null && target.distanceToSqr(end.x, end.y, end.z) < 1 ? 0 : failedPathFindingPenalty + 10;
            }
            if (distanceToSqr(target.getX(), target.getBoundingBox().minY, target.getZ()) <= 1.5) {
                count = 0;
                int before = target.getItem().getCount();
                var rest = pickupItem(target.getItem().copy());
                if (rest.isEmpty()) target.discard(); else target.setItem(rest);
                if (rest.getCount() != before)
                    target.playSound(SoundEvents.ITEM_PICKUP, .2f, ((random.nextFloat() - random.nextFloat()) * .7f + 1) * 2);
            }
        }
    }

    @Override public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, MobSpawnType reason, SpawnGroupData data) {
        data = super.finalizeSpawn(level, difficulty, reason, data);
        populateDefaultEquipmentSlots(random, difficulty);
        var held = getMainHandItem();
        if (held.getItem() instanceof BasicWandItem) {
            variant(1);
            setDropChance(EquipmentSlot.MAINHAND, .1f);
        } else if (!held.isEmpty()) {
            if (held.is(Items.BOW)) variant(2);
            populateDefaultEquipmentEnchantments(level, random, difficulty);
        }
        setCanPickUpLoot(random.nextFloat() < .75f * difficulty.getSpecialMultiplier());
        return data;
    }

    @Override protected void populateDefaultEquipmentSlots(net.minecraft.util.RandomSource random, DifficultyInstance difficulty) {
        super.populateDefaultEquipmentSlots(random, difficulty);
        Item held = switch (random.nextInt(20)) {
            case 0, 12 -> null;
            case 1 -> Items.STONE_SWORD;
            case 3 -> Items.STONE_AXE;
            case 5 -> Items.IRON_SWORD;
            case 6 -> Items.IRON_AXE;
            case 7 -> Items.FISHING_ROD;
            case 8 -> Items.STONE_PICKAXE;
            case 9 -> Items.IRON_PICKAXE;
            case 2, 4, 10, 11, 13 -> Items.BOW;
            default -> Items.AIR;
        };
        if (held == null) {
            var wand = WandParts.make(WandParts.CAPS.get("iron"), WandParts.RODDEFS.get("wood"));
            FociContent.setFocus(wand, new ItemStack(PechContent.FOCUS.get()));
            WandData.add(wand, Aspect.EARTH, 2 + random.nextInt(6), true);
            WandData.add(wand, Aspect.ENTROPY, 2 + random.nextInt(6), true);
            WandData.add(wand, Aspect.WATER, 2 + random.nextInt(6), true);
            WandData.add(wand, Aspect.AIR, random.nextInt(4), true);
            WandData.add(wand, Aspect.FIRE, random.nextInt(4), true);
            WandData.add(wand, Aspect.ORDER, random.nextInt(4), true);
            setItemSlot(EquipmentSlot.MAINHAND, wand);
        } else if (held != Items.AIR) setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(held));
    }

    @Override public void performRangedAttack(LivingEntity target, float power) {
        if (variant() == 2) {
            var weapon = getMainHandItem();
            var arrow = ProjectileUtil.getMobArrow(this, new ItemStack(Items.ARROW), power, weapon);
            double dx = target.getX() - getX(), dy = target.getEyeY() - .7 - arrow.getY(), dz = target.getZ() - getZ();
            arrow.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * .2, dz, 1.6f, 14 - level().getDifficulty().getId() * 4);
            playSound(SoundEvents.ARROW_SHOOT, 1, 1 / (random.nextFloat() * .4f + .8f));
            level().addFreshEntity(arrow);
        } else if (variant() == 1) {
            var blast = new PechBlast(level(), this, 1, 0, random.nextFloat() < .1f);
            var motion = target.getDeltaMovement();
            double dx = target.getX() + motion.x - getX(), dy = target.getY() + target.getEyeHeight() - 1.5 - getY(), dz = target.getZ() + motion.z - getZ();
            blast.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * .1, dz, 1.5f, 4);
            playSound(sound("ice"), .4f, 1 + random.nextFloat() * .1f);
            level().addFreshEntity(blast);
        }
        swing(InteractionHand.MAIN_HAND);
    }

    static SoundEvent sound(String name) { return SoundEvent.createVariableRangeEvent(AlchemyContent.id(name)); }
    @Override protected float getSoundVolume() { return .4f; }
    @Override public int getAmbientSoundInterval() { return 120; }
    @Override protected SoundEvent getAmbientSound() { return sound("pech_idle"); }
    @Override protected SoundEvent getHurtSound(DamageSource source) { return sound("pech_hit"); }
    @Override protected SoundEvent getDeathSound() { return sound("pech_death"); }

    /** Original playLivingSound: a third of the time chatter with a nearby Pech instead of idling. */
    @Override public void playAmbientSound() {
        if (!level().isClientSide) {
            if (random.nextInt(3) == 0 && !level().getEntitiesOfClass(PechEntity.class, getBoundingBox().inflate(4, 2, 4), other -> other != this).isEmpty()) {
                level().broadcastEntityEvent(this, (byte) 17);
                playSound(sound("pech_trade"), getSoundVolume(), getVoicePitch());
                return;
            }
            level().broadcastEntityEvent(this, (byte) 16);
        }
        super.playAmbientSound();
    }

    @Override public void handleEntityEvent(byte id) {
        switch (id) {
            case 16 -> mumble = (float) Math.PI;
            case 17 -> mumble = (float) Math.PI * 2;
            case 18 -> villagerParticles(ParticleTypes.HAPPY_VILLAGER, 5);
            case 19 -> { villagerParticles(ParticleTypes.ANGRY_VILLAGER, 5); mumble = (float) Math.PI * 2; }
            default -> super.handleEntityEvent(id);
        }
    }

    private void villagerParticles(net.minecraft.core.particles.SimpleParticleType type, int count) {
        float w = getBbWidth(), h = getBbHeight();
        for (int i = 0; i < count; i++)
            level().addParticle(type, getX() + random.nextFloat() * w * 2 - w, getY() + .5 + random.nextFloat() * h, getZ() + random.nextFloat() * w * 2 - w,
                    random.nextGaussian() * .02, random.nextGaussian() * .02, random.nextGaussian() * .02);
    }

    public static boolean canSpawn(EntityType<PechEntity> type, ServerLevelAccessor level, MobSpawnType reason, BlockPos pos, net.minecraft.util.RandomSource random) {
        return ManaPodBlock.magical(level, pos) && Monster.checkMonsterSpawnRules(type, level, reason, pos, random)
                && level.getLevel().getEntitiesOfClass(PechEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(16)).size() < 4;
    }

    @Override public void tick() {
        if (mumble > 0) mumble *= .75f;
        if (!level().isClientSide) {
            if (anger() > 0) anger(anger() - 1);
            if (anger() > 0 && getTarget() == null && angryAt != null && angryAt.isAlive()) {
                setTarget(angryAt);
                if (chargecount > 0) chargecount--;
                if (chargecount == 0) { chargecount = 100; playSound(sound("pech_charge"), getSoundVolume(), getVoicePitch()); }
                level().broadcastEntityEvent(this, (byte) 17);
            }
        } else {
            if (random.nextInt(15) == 0 && anger() > 0) villagerParticles(ParticleTypes.ANGRY_VILLAGER, 1);
            if (random.nextInt(25) == 0 && tamed()) villagerParticles(ParticleTypes.HAPPY_VILLAGER, 1);
        }
        super.tick();
    }

    @Override protected void customServerAiStep() {
        super.customServerAiStep();
        if (tickCount % 40 == 0) heal(1);
    }

    public boolean canPickup(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (!tamed() && PechTrades.value(stack) > 0) return true;
        for (int i = 0; i < loot.getContainerSize(); i++) {
            var held = loot.getItem(i);
            if (held.isEmpty()) return true;
            if (ItemStack.isSameItemSameComponents(stack, held) && stack.getCount() + held.getCount() <= held.getMaxStackSize()) return true;
        }
        return false;
    }

    /** Takes the stack (bribe or pack storage) and returns what remains on the ground. */
    public ItemStack pickupItem(ItemStack stack) {
        if (stack.isEmpty()) return stack;
        if (!tamed() && PechTrades.value(stack) > 0) {
            if (random.nextInt(10) < PechTrades.value(stack)) {
                tamed(true);
                level().broadcastEntityEvent(this, (byte) 18);
            }
            stack.shrink(1);
            return stack;
        }
        for (int i = 0; i < loot.getContainerSize() && !stack.isEmpty(); i++) {
            var held = loot.getItem(i);
            if (!held.isEmpty() && held.getCount() < held.getMaxStackSize() && ItemStack.isSameItemSameComponents(stack, held)) {
                int moved = Math.min(stack.getCount(), held.getMaxStackSize() - held.getCount());
                held.grow(moved);
                stack.shrink(moved);
            }
        }
        for (int i = 0; i < loot.getContainerSize() && !stack.isEmpty(); i++)
            if (loot.getItem(i).isEmpty()) { loot.setItem(i, stack.copy()); stack.setCount(0); }
        loot.setChanged();
        return stack;
    }

    @Override protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (player.isShiftKeyDown() || player.getItemInHand(hand).is(Items.NAME_TAG)) return super.mobInteract(player, hand);
        if (tamed()) {
            if (player instanceof ServerPlayer serverPlayer)
                serverPlayer.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PechMenu(id, inventory, this), getDisplayName()), buf -> buf.writeInt(getId()));
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    private void becomeAngryAt(LivingEntity attacker) {
        angryAt = attacker;
        if (anger() <= 0) {
            level().broadcastEntityEvent(this, (byte) 19);
            playSound(sound("pech_charge"), getSoundVolume(), getVoicePitch());
        }
        setTarget(attacker);
        anger(400 + random.nextInt(400));
        tamed(false);
    }

    @Override public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) return false;
        if (!level().isClientSide && source.getEntity() instanceof Player player) {
            for (var other : level().getEntitiesOfClass(PechEntity.class, getBoundingBox().inflate(32, 16, 32), other -> other != this))
                other.becomeAngryAt(player);
            becomeAngryAt(player);
        }
        return super.hurt(source, amount);
    }

    @Override public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putByte("PechType", (byte) variant());
        tag.putShort("Anger", (short) anger());
        tag.putBoolean("Tamed", tamed());
        var inventory = new net.minecraft.nbt.ListTag();
        for (int i = 0; i < loot.getContainerSize(); i++) {
            var entry = new CompoundTag();
            entry.putInt("Slot", i);
            if (!loot.getItem(i).isEmpty()) entry.put("Item", loot.getItem(i).save(registryAccess()));
            inventory.add(entry);
        }
        tag.put("Loot", inventory);
    }

    @Override public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("PechType")) variant(tag.getInt("PechType"));
        anger(tag.getInt("Anger"));
        tamed(tag.getBoolean("Tamed"));
        loot.clearContent();
        var inventory = tag.getList("Loot", 10);
        for (int i = 0; i < inventory.size(); i++) {
            var entry = inventory.getCompound(i);
            int slot = entry.getInt("Slot");
            if (slot >= 0 && slot < loot.getContainerSize() && entry.contains("Item"))
                loot.setItem(slot, ItemStack.parseOptional(registryAccess(), entry.getCompound("Item")));
        }
    }

    /** Original canDespawn: only Pechs carrying fewer than five stacks wander off. */
    @Override public boolean removeWhenFarAway(double distance) {
        int stacks = 0;
        for (int i = 0; i < loot.getContainerSize(); i++) if (!loot.getItem(i).isEmpty()) stacks++;
        return stacks < 5;
    }

    @Override protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
        int looting = source.getEntity() instanceof LivingEntity killer
                ? EnchantmentHelper.getEnchantmentLevel(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOOTING), killer) : 0;
        for (int i = 0; i < loot.getContainerSize(); i++)
            if (!loot.getItem(i).isEmpty() && level.random.nextFloat() < .88f) spawnAtLocation(loot.getItem(i).copy(), 1.5f);
        var primals = Aspect.getPrimalAspects();
        for (int i = 0; i < 1 + looting; i++)
            if (random.nextBoolean()) spawnAtLocation(ManaBeanItem.of(primals.get(random.nextInt(primals.size())), 1), 1.5f);
        if (level.random.nextInt(10) < 1 + looting) spawnAtLocation(PechTrades.mod("coin"), 1.5f);
        // EntityLiving rare drop (rand(200) - looting < 5) gives dropRareDrop: a knowledge fragment.
        if (recentlyHit && random.nextInt(200) - looting < 5) spawnAtLocation(new ItemStack(BasicsContent.KNOWLEDGE_FRAGMENT.get()), 1.5f);
        super.dropCustomDeathLoot(level, source, recentlyHit);
    }
}
