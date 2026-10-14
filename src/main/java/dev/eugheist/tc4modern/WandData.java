package dev.eugheist.tc4modern;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.lib.vis.VisStorage;
/** Component adapter for the original wood rod capacity of 25 vis per primal. */
public final class WandData {
    public static final int CAPACITY=2500;
    private WandData(){}
    public static int get(ItemStack stack,Aspect aspect){return VisStorage.getVis(NoteData.tag(stack),aspect);}
    public static int add(ItemStack stack,Aspect aspect,int amount,boolean apply){
        var tag=NoteData.tag(stack);int left=VisStorage.addVis(tag,aspect,amount,apply,capacity(stack));
        if(apply)stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return left;
    }
    public static boolean isWand(ItemStack stack){return stack.getItem() instanceof BasicWandItem;}
    /** WandRod capacity in centivis; legacy fixed wands map to wood or greatwood. */
    public static int capacity(ItemStack stack){return isWand(stack)?WandParts.rod(stack).capacity()*(WandParts.isSceptre(stack)?150:100):0;}
    public static float craftingModifier(ItemStack stack,net.minecraft.world.entity.player.Player player,Aspect aspect){
        var effect=player==null?null:player.getEffect(TC4Modern.VIS_EXHAUST);
        var infectious=player==null?null:player.getEffect(WarpEffects.INFECTIOUS_VIS_EXHAUST);
        float discount=thaumcraft.common.lib.vis.VisGearDiscount.withExhaustion(
                thaumcraft.common.lib.vis.VisGearDiscount.armor(player,aspect),effect==null?-1:effect.getAmplifier(),infectious==null?-1:infectious.getAmplifier());
        return VisStorage.consumptionModifier(isWand(stack)?WandParts.cap(stack).modifier(aspect):1,discount,0,false,WandParts.isSceptre(stack),true);
    }
    /** Preview or server-side deduction using original iron-cap cost modifier. */
    public static boolean consumeCrafting(ItemStack stack,thaumcraft.api.aspects.AspectList cost,boolean apply,net.minecraft.world.entity.player.Player player){
        if(!isWand(stack))return false;
        var tag=NoteData.tag(stack);
        boolean success=VisStorage.consumeAllVisCrafting(tag,cost,apply,aspect->craftingModifier(stack,player,aspect));
        if(success && apply)stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));
        return success;
    }
}
