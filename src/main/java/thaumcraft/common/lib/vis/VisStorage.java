package thaumcraft.common.lib.vis;
import net.minecraft.nbt.CompoundTag;
import thaumcraft.api.aspects.Aspect;
/** Original ItemWandCasting vis methods; stack NBT and rod capacity supplied by platform bridge. */
public final class VisStorage {
    public static int getVis(CompoundTag is, Aspect aspect) {
        int out = 0;
        if (is != null && aspect != null && is.contains(aspect.getTag())) {
            out = is.getInt(aspect.getTag());
        }
        return out;
    }
    public static void storeVis(CompoundTag is, Aspect aspect, int amount) {
        is.putInt(aspect.getTag(), amount);
    }
    public static int addVis(CompoundTag is, Aspect aspect, int amount, boolean doit, int capacity) {
        if (!aspect.isPrimal()) {
            return 0;
        }
        int storeAmount = getVis(is, aspect) + amount * 100;
        int leftover = Math.max(storeAmount - capacity, 0);
        if (doit) {
            storeVis(is, aspect, Math.min(storeAmount, capacity));
        }
        return leftover / 100;
    }
    public static int addRealVis(CompoundTag is, Aspect aspect, int amount, boolean doit, int capacity) {
        if (!aspect.isPrimal()) {
            return 0;
        }
        int storeAmount = getVis(is, aspect) + amount;
        int leftover = Math.max(storeAmount - capacity, 0);
        if (doit) {
            storeVis(is, aspect, Math.min(storeAmount, capacity));
        }
        return leftover;
    }
    public static float consumptionModifier(float capModifier,float discount,int frugal,boolean hasFocus,boolean sceptre,boolean crafting) {
        float modifier=capModifier-discount;
        if(hasFocus && !crafting)modifier-=(float)frugal/10.0f;
        if(sceptre)modifier-=0.1f;
        return Math.max(modifier,0.1f);
    }
    public static boolean consumeVis(CompoundTag is,Aspect aspect,int amount,float modifier) {
        amount=(int)((float)amount*modifier);
        if(getVis(is,aspect)>=amount){storeVis(is,aspect,getVis(is,aspect)-amount);return true;}
        return false;
    }
    public static boolean consumeAllVisCrafting(CompoundTag is,thaumcraft.api.aspects.AspectList aspects,boolean doit,java.util.function.Function<Aspect,Float> modifiers) {
        if(aspects==null || aspects.size()==0)return false;
        var costs=new thaumcraft.api.aspects.AspectList();
        for(Aspect aspect:aspects.getAspects())costs.add(aspect,aspects.getAmount(aspect)*100);
        return consumeAllVis(is,costs,doit,modifiers);
    }
    public static boolean consumeAllVis(CompoundTag is,thaumcraft.api.aspects.AspectList aspects,boolean doit,java.util.function.Function<Aspect,Float> modifiers) {
        if(aspects==null || aspects.size()==0)return false;
        var costs=new thaumcraft.api.aspects.AspectList();
        for(Aspect aspect:aspects.getAspects())costs.add(aspect,(int)((float)aspects.getAmount(aspect)*modifiers.apply(aspect)));
        for(Aspect aspect:costs.getAspects())if(getVis(is,aspect)<costs.getAmount(aspect))return false;
        if(doit)for(Aspect aspect:costs.getAspects())storeVis(is,aspect,getVis(is,aspect)-costs.getAmount(aspect));
        return true;
    }
}
