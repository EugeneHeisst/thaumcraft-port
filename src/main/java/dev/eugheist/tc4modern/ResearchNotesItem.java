package dev.eugheist.tc4modern;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import thaumcraft.api.research.ResearchCategories;

/** Modern item adapter for consuming original completed research notes. */
public class ResearchNotesItem extends Item {
    public ResearchNotesItem(Properties properties) { super(properties); }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand) {
        ItemStack stack=player.getItemInHand(hand);
        var note=NoteData.read(stack);
        if(player instanceof ServerPlayer server && note!=null && note.isComplete()) {
            if(ResearchProgress.completeAndNotify(server,note.key)) {
                stack.shrink(1);ThaumometerItem.sync(server,"research_complete");
            } else ThaumometerItem.sync(server,"research_locked");
        }
        return InteractionResultHolder.sidedSuccess(stack,level.isClientSide);
    }
    @Override public Component getName(ItemStack stack) {
        var note=NoteData.read(stack);return Component.translatable(note!=null && note.complete ? "item.discovery.name" : "item.researchnotes.name");
    }
    @Override public void appendHoverText(ItemStack stack,TooltipContext context,List<Component> tooltip,TooltipFlag flag) {
        var note=NoteData.read(stack);
        if(note!=null) {
            tooltip.add(ResearchProgress.name(note.key));
            tooltip.add(Component.translatable(note.complete ? "item.tc4modern.research_notes.complete" : "item.tc4modern.research_notes.solve"));
        }
    }
}
