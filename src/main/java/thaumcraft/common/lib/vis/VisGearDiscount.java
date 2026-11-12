package thaumcraft.common.lib.vis;
import net.minecraft.world.entity.player.Player;
import thaumcraft.api.IVisDiscountGear;
import thaumcraft.api.aspects.Aspect;
/** Armor and Baubles loops from original WandManager.getTotalVisDiscount. */
public final class VisGearDiscount {
    private VisGearDiscount() {}
    /** Missing effects use -1; original penalty uses the strongest exhaustion amplifier. */
    public static float withExhaustion(float discount,int normal,int infectious){
        return normal>=0||infectious>=0?discount-(Math.max(normal,infectious)+1)*.1f:discount;
    }
    public static float armor(Player player,Aspect aspect){
        int total=0;if(player==null)return 0;
        for(var stack:player.getArmorSlots())if(stack.getItem() instanceof IVisDiscountGear gear)total+=gear.getVisDiscount(stack,player,aspect);
        var baubles=dev.eugheist.tc4modern.BaublesInventory.of(player);for(int i=0;i<baubles.getContainerSize();i++){var stack=baubles.getItem(i);if(stack.getItem() instanceof IVisDiscountGear gear)total+=gear.getVisDiscount(stack,player,aspect);}
        return (float)total/100.0f;
    }
}
