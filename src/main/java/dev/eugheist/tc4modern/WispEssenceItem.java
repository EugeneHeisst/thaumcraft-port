package dev.eugheist.tc4modern;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;
import thaumcraft.api.aspects.*;

/** ItemWispEssence: one aspect stored like a phial; tinted by its first aspect, cycling when blank. */
public final class WispEssenceItem extends Item {
    public WispEssenceItem() { super(new Properties().stacksTo(64)); }
    public static ItemStack of(Aspect aspect, int amount) {
        var stack = new ItemStack(BasicsContent.WISP_ESSENCE.get());
        EssentiaItems.setAspects(stack, new AspectList().add(aspect, amount));
        return stack;
    }
    public static int color(ItemStack stack) {
        var aspects = EssentiaItems.getAspects(stack);
        if (aspects != null && aspects.size() > 0) return 0xFF000000 | aspects.getAspects()[0].getColor();
        var all = Aspect.aspects.values().toArray(Aspect[]::new);
        return 0xFF000000 | all[(int) (System.currentTimeMillis() / 500 % all.length)].getColor();
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        EssentiaItems.tooltip(stack, lines);
    }
}
