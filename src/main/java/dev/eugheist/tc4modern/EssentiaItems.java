package dev.eugheist.tc4modern;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/**
 * IEssentiaContainerItem bridge: the original "Aspects" NBT list lives in minecraft:custom_data,
 * plus ItemEssence (phials) and the ItemResource jar label.
 */
public final class EssentiaItems {
    private EssentiaItems() {}
    public static AspectList getAspects(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) return null;
        var list = new AspectList();
        list.readFromNBT(data.copyTag());
        return list.size() > 0 ? list : null;
    }
    public static void setAspects(ItemStack stack, AspectList aspects) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> aspects.writeToNBT(tag));
    }
    public static ItemStack phialOf(Aspect aspect, int amount) {
        var stack = new ItemStack(EssentiaDevices.ESSENTIA_PHIAL.get());
        setAspects(stack, new AspectList().add(aspect, amount));
        return stack;
    }
    static void giveOrDrop(Player player, Level level, net.minecraft.core.BlockPos pos, ItemStack stack) {
        if (!player.getInventory().add(stack))
            level.addFreshEntity(new ItemEntity(level, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, stack));
    }
    static void tooltip(ItemStack stack, java.util.List<Component> lines) {
        var aspects = getAspects(stack);
        if (aspects == null) return;
        var known = ClientKnowledgeView.discovered();
        for (Aspect tag : aspects.getAspectsSorted()) {
            if (known.contains(tag.getTag())) lines.add(Component.literal(tag.getName() + " x " + aspects.getAmount(tag)));
            else lines.add(Component.translatable("tc.aspect.unknown"));
        }
    }

    /** ItemEssence metadata 0 (empty glass phial) and 1 (phial of essentia, 8 of one aspect). */
    public static final class Phial extends Item {
        private final boolean filled;
        public Phial(Properties properties, boolean filled) { super(properties); this.filled = filled; }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) { tooltip(stack, lines); }
        @Override public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
            var level = context.getLevel(); var pos = context.getClickedPos(); var player = context.getPlayer();
            if (player == null) return InteractionResult.PASS;
            var tile = level.getBlockEntity(pos);
            if (!filled && tile instanceof AlembicEntity alembic && alembic.state.amount >= 8) {
                if (level.isClientSide) { player.swing(context.getHand()); return InteractionResult.SUCCESS; }
                var aspect = alembic.state.aspect;
                if (alembic.state.takeFromContainer(aspect, 8)) {
                    alembic.changed();
                    stack.shrink(1);
                    giveOrDrop(player, level, pos, phialOf(aspect, 8));
                    level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_SWIM, net.minecraft.sounds.SoundSource.PLAYERS, .25f, 1f);
                    player.inventoryMenu.broadcastChanges();
                    return InteractionResult.SUCCESS;
                }
            }
            if (!filled && tile instanceof WardedJarEntity jar && jar.state.amount >= 8) {
                if (level.isClientSide) { player.swing(context.getHand()); return InteractionResult.SUCCESS; }
                var aspect = jar.state.aspect;
                if (jar.state.takeFromContainer(aspect, 8)) {
                    jar.changed();
                    stack.shrink(1);
                    giveOrDrop(player, level, pos, phialOf(aspect, 8));
                    level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_SWIM, net.minecraft.sounds.SoundSource.PLAYERS, .25f, 1f);
                    player.inventoryMenu.broadcastChanges();
                    return InteractionResult.SUCCESS;
                }
            }
            var al = getAspects(stack);
            if (filled && al != null && al.size() == 1 && tile instanceof WardedJarEntity jar) {
                Aspect aspect = al.getAspects()[0];
                if (jar.state.amount <= jar.state.maxAmount - 8 && jar.state.doesContainerAccept(aspect)) {
                    if (level.isClientSide) { player.swing(context.getHand()); return InteractionResult.SUCCESS; }
                    if (jar.state.addToContainer(aspect, 8) == 0) {
                        jar.changed();
                        stack.shrink(1);
                        giveOrDrop(player, level, pos, new ItemStack(EssentiaDevices.PHIAL.get()));
                        level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_SWIM, net.minecraft.sounds.SoundSource.PLAYERS, .25f, 1f);
                        player.inventoryMenu.broadcastChanges();
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.PASS;
        }
    }

    /** ItemResource metadata 13: blank or aspect-marked jar label (overlay tinted with the aspect colour). */
    public static final class Label extends Item {
        public Label(Properties properties) { super(properties); }
        @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) { tooltip(stack, lines); }
    }

    /** Client-side discovered aspects, read from the synchronized knowledge copy. */
    static final class ClientKnowledgeView {
        static java.util.Set<String> discovered() {
            if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return java.util.Set.of();
            CompoundTag knowledge = ClientKnowledge.knowledge;
            var set = new java.util.HashSet<String>();
            // ResearchKnowledge stores each discovered aspect as a top-level tag -> points entry.
            for (String key : knowledge.getAllKeys()) if (Aspect.getAspect(key) != null) set.add(key);
            return set;
        }
    }
}
