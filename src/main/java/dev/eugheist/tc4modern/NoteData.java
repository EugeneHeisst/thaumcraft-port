package dev.eugheist.tc4modern;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ResearchNoteData;

/** Item components replace 1.7.10's mutable stack.tag, preserving original note NBT schema. */
public final class NoteData {
    private NoteData() {}
    public static CompoundTag tag(ItemStack stack) { return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag(); }
    public static ResearchNoteData read(ItemStack stack) { return stack.is(TC4Modern.RESEARCH_NOTES) ? ResearchManager.getData(tag(stack)) : null; }
    public static void write(ItemStack stack, ResearchNoteData note) {
        CompoundTag tag=tag(stack);
        ResearchManager.updateData(tag,note);
        stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
    }
}
