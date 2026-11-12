package thaumcraft.common.lib.crafting;

import java.util.List;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;

/** Original ThaumcraftCraftingManager arithmetic; modern adapters resolve ingredient tags. */
public final class RecipeAspectMath {
    public record IngredientTags(AspectList tags, AspectList containerTags, boolean returnsItself) {}
    private RecipeAspectMath() {}
    public static AspectList fromIngredients(List<IngredientTags> ingredients, int outputCount) {
        if(outputCount<=0)throw new IllegalArgumentException("Invalid recipe output count");
        AspectList out=new AspectList(),mid=new AspectList();
        for(IngredientTags ingredient:ingredients){
            if(ingredient.returnsItself())continue;
            if(ingredient.containerTags()!=null)for(Aspect as:ingredient.containerTags().getAspects())out.reduce(as,ingredient.containerTags().getAmount(as));
            AspectList obj=ingredient.tags();if(obj==null)continue;
            for(Aspect as:obj.getAspects())if(as!=null)mid.add(as,obj.getAmount(as));
        }
        for(Aspect as:mid.getAspects())if(as!=null)out.add(as,(int)((float)mid.getAmount(as)*0.75f/(float)outputCount));
        return positive(out);
    }
    public static AspectList addCost(AspectList tags,AspectList cost,int outputCount){
        if(outputCount<=0)throw new IllegalArgumentException("Invalid recipe output count");
        AspectList out=tags.copy();if(cost!=null)for(Aspect a:cost.getAspects())out.add(a,(int)(Math.sqrt(cost.getAmount(a))/(double)outputCount));
        return positive(out);
    }
    /** Original crucible path copies catalyst tags whole; only cost is divided by output count. */
    public static AspectList fromCrucible(AspectList catalyst,AspectList cost,int outputCount) {
        return addCost(catalyst == null ? new AspectList() : catalyst.copy(),cost,outputCount);
    }
    public static AspectList positive(AspectList tags){
        for(Aspect as:tags.copy().getAspects())if(tags.getAmount(as)<=0)tags.remove(as);
        return tags;
    }
    public static AspectList capAspects(AspectList tags,int amount){
        if(tags==null)return null;
        AspectList out=new AspectList();for(Aspect aspect:tags.getAspects())out.merge(aspect,Math.min(amount,tags.getAmount(aspect)));return out;
    }
}
