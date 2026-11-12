package thaumcraft.common.tiles.essentia;
import java.util.*;
import net.minecraft.world.item.ItemStack;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.CrucibleRecipe;
/** TileThaumatorium update loop with modern recipe IDs and injected world operations. */
public final class ThaumatoriumState {
    public AspectList essentia=new AspectList();
    public final List<String> selected=new ArrayList<>(),players=new ArrayList<>();
    public int maxRecipes=1,currentCraft=-1,counter;
    public Aspect suction;
    public boolean heated;
    public CrucibleRecipe currentRecipe;
    public interface Host {
        boolean heated();boolean power();int upgrades();ItemStack input();
        CrucibleRecipe recipe(String key);boolean outputFits(ItemStack output);
        void pull(Aspect aspect);void complete(ItemStack output,String player);void changed();
    }
    public void tick(Host h){
        if(counter%40==0){heated=h.heated();int max=h.upgrades();if(max!=maxRecipes){maxRecipes=max;while(selected.size()>max){selected.remove(selected.size()-1);players.remove(players.size()-1);}currentCraft=-1;h.changed();}}
        ++counter;
        if(!heated||h.power()||counter%5!=0||selected.isEmpty())return;
        if(h.input().isEmpty()){suction=null;return;}
        if(currentCraft<0||currentCraft>=selected.size()||currentRecipe==null||!currentRecipe.catalystMatches(h.input())){
            /* Preserve the original stale-craft behavior if no new catalyst matches. */
            for(int i=0;i<selected.size();i++){var r=h.recipe(selected.get(i));if(r!=null&&r.catalystMatches(h.input())){currentCraft=i;currentRecipe=r;break;}}
        }
        if(currentCraft<0||!h.outputFits(currentRecipe.getRecipeOutput()))return;
        suction=null;
        for(var a:currentRecipe.aspects.getAspectsSorted())if(essentia.getAmount(a)<currentRecipe.aspects.getAmount(a)){suction=a;break;}
        if(suction==null){if(currentRecipe.matches(essentia,h.input())){essentia=new AspectList();h.complete(currentRecipe.getRecipeOutput().copy(),players.get(currentCraft));currentCraft=-1;h.changed();}}
        else h.pull(suction);
    }
    public int add(Aspect a,int n){
        /* Original dereferences currentRecipe before its null guard. DEVIATION: guard first to avoid network-triggered NPE. */
        if(currentRecipe==null)return n;
        int missing=currentRecipe.aspects.getAmount(a)-essentia.getAmount(a);
        if(missing<=0)return n;int accepted=Math.min(missing,n);essentia.add(a,accepted);return n-accepted;
    }
}
