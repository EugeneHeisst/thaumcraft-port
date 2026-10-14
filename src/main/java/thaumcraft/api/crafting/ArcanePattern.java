package thaumcraft.api.crafting;
import java.util.List;
import java.util.function.Predicate;
/** Original ShapedArcaneRecipe matching loops with injected item/tag comparison. Null is an empty slot. */
public final class ArcanePattern<T> {
    public final int width,height;
    public final String research;
    private final List<Predicate<T>> input;
    private boolean mirrored=true,shapeless;
    public ArcanePattern(String research,int width,int height,List<Predicate<T>> input){
        this.research=research;this.width=width;this.height=height;this.input=new java.util.ArrayList<>(input);
        if(width<1 || width>3 || height<1 || height>3 || input.size()!=width*height)throw new IllegalArgumentException("Invalid arcane shape");
    }
    public ArcanePattern<T> setMirrored(boolean mirror){mirrored=mirror;return this;}
    public ArcanePattern<T> setShapeless(boolean value){shapeless=value;return this;}
    public boolean matches(List<T> grid,Predicate<String> completed){
        if(grid.size()!=9)return false;
        if(research.length()>0 && !completed.test(research))return false;
        if(shapeless){
            var remaining=new java.util.ArrayList<Predicate<T>>();for(var ingredient:input)if(ingredient!=null)remaining.add(ingredient);
            for(T slot:grid){if(slot==null)continue;int index=-1;for(int i=0;i<remaining.size();i++)if(remaining.get(i).test(slot)){index=i;break;}if(index<0)return false;remaining.remove(index);}
            return remaining.isEmpty();
        }
        for(int x=0;x<=3-width;x++)for(int y=0;y<=3-height;y++){
            if(checkMatch(grid,x,y,false))return true;
            if(mirrored && checkMatch(grid,x,y,true))return true;
        }
        return false;
    }
    private boolean checkMatch(List<T> grid,int startX,int startY,boolean mirror){
        for(int x=0;x<3;x++)for(int y=0;y<3;y++){
            int subX=x-startX,subY=y-startY;
            Predicate<T> target=null;
            if(subX>=0 && subY>=0 && subX<width && subY<height)target=input.get((mirror?width-subX-1:subX)+subY*width);
            T slot=grid.get(x+y*3);
            if(target!=null){if(slot==null || !target.test(slot))return false;}
            else if(slot!=null)return false;
        }
        return true;
    }
}
