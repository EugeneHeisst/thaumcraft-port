package dev.eugheist.tc4modern;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** ItemResource metadata 9 onItemRightClick: consumed (unless creative) for 1-2 research points of every primal aspect. */
public final class KnowledgeFragmentItem extends Item {
    public KnowledgeFragmentItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild) stack.shrink(1);
        if (!level.isClientSide && player instanceof ServerPlayer server) {
            var knowledge = ResearchKnowledge.load(server.getPersistentData());
            var gained = new AspectList();
            for (Aspect a : Aspect.getPrimalAspects()) {
                short q = (short) (level.random.nextInt(2) + 1);
                knowledge.addAspectPool("current", a, q);
                gained.add(a, q);
            }
            ResearchKnowledge.save(server.getPersistentData(), knowledge);
            ThaumometerItem.sync(server, "");
            AspectGain.send(server, gained);
        }
        // The original returns the stack without a use animation.
        return InteractionResultHolder.consume(stack);
    }
}
