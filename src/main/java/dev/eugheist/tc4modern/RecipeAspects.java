package dev.eugheist.tc4modern;

import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import thaumcraft.api.aspects.*;
import thaumcraft.common.lib.crafting.RecipeAspectMath;

/** Modern recipe/ingredient/cache bridge for original generated object tags. */
@EventBusSubscriber(modid=TC4Modern.MODID)
public final class RecipeAspects {
    private static final Map<RecipeManager,Resolver> RESOLVERS=new WeakHashMap<>();
    private RecipeAspects() {}
    public static synchronized Map<String,Integer> get(ItemStack stack,Level level){
        if(stack.isEmpty())return Map.of();
        var base=RESOLVERS.computeIfAbsent(level.getRecipeManager(),manager->new Resolver(level)).generate(stack.getItem(),new ArrayList<>());
        var tags=StackAspectBridge.apply(stack,base);
        if(tags==null)return Map.of();
        Map<String,Integer> result=new LinkedHashMap<>();for(var aspect:tags.getAspects())if(tags.getAmount(aspect)>0)result.put(aspect.getTag(),tags.getAmount(aspect));return result;
    }
    public static synchronized Map<String,Integer> block(String id,ItemStack stack,Level level){
        var complex=thaumcraft.common.config.ConfigComplexAspects.get(id);
        if(stack.isEmpty() && complex!=null && !complex.inherited().isEmpty()){
            var inherited=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(complex.inherited()));
            var tags=RESOLVERS.computeIfAbsent(level.getRecipeManager(),manager->new Resolver(level)).generate(inherited,new ArrayList<>());
            var result=complex.additional().copy();if(tags!=null)for(var aspect:tags.getAspects())result.add(aspect,tags.getAmount(aspect));
            Map<String,Integer> map=new LinkedHashMap<>();for(var aspect:result.getAspects())map.put(aspect.getTag(),Math.min(64,result.getAmount(aspect)));return map;
        }
        return get(stack,level);
    }
    public static synchronized void clear(){RESOLVERS.clear();}
    @SubscribeEvent public static void tags(net.neoforged.neoforge.event.TagsUpdatedEvent event){clear();}
    @SubscribeEvent public static void synced(net.neoforged.neoforge.event.OnDatapackSyncEvent event){clear();}
    private record Candidate(List<Ingredient> ingredients,int count,AspectList cost) {}
    private static final class Resolver {
        private final Map<Item,List<Candidate>> ordinary=new HashMap<>(),arcane=new HashMap<>();
        private final Map<Item,Candidate> infusion=new HashMap<>();
        private final Map<Item,thaumcraft.api.crafting.CrucibleRecipe> crucible=new HashMap<>();
        private final Map<Item,AspectList> generated=new HashMap<>();
        private final Level level;
        Resolver(Level level){
            this.level=level;
            for(var holder:level.getRecipeManager().getAllRecipesFor(RecipeType.CRAFTING)){
                var recipe=holder.value();if(!(recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe))continue;
                var output=recipe.getResultItem(level.registryAccess());if(output.isEmpty())continue;
                ordinary.computeIfAbsent(output.getItem(),item->new ArrayList<>()).add(new Candidate(List.copyOf(recipe.getIngredients()),output.getCount(),null));
            }
            for(var recipe:ArcaneRecipes.entries())arcane.computeIfAbsent(recipe.output().getItem(),item->new ArrayList<>()).add(new Candidate(recipe.ingredients(),recipe.output().getCount(),recipe.cost()));
            for(var recipe:CrucibleRecipes.entries())crucible.putIfAbsent(recipe.getRecipeOutput().getItem(),recipe);
            List<Ingredient> brainIngredients = new ArrayList<>();
            brainIngredients.add(Ingredient.of(BrainJarRecipe.input()));
            for (var component : BrainJarRecipe.ingredients()) brainIngredients.add(Ingredient.of(component));
            infusion.put(BrainJarRecipe.output().getItem(), new Candidate(List.copyOf(brainIngredients), 1, BrainJarRecipe.cost()));
            for(var recipe:InfusionItemRecipes.all()) {
                if(recipe.augment())continue;
                var ingredients=new ArrayList<Ingredient>();
                ingredients.add(Ingredient.of(recipe.central().get()));
                for(var component:recipe.components())ingredients.add(Ingredient.of(component.get()));
                infusion.put(recipe.output().get(),new Candidate(List.copyOf(ingredients),1,recipe.aspects()));
            }
            // Original complex registrations run in order before scanning begins.
            for(var entry:thaumcraft.common.config.ConfigComplexAspects.ENTRIES){
                var id=net.minecraft.resources.ResourceLocation.parse(entry.id());
                if(BuiltInRegistries.ITEM.containsKey(id))generate(BuiltInRegistries.ITEM.get(id),new ArrayList<>());
            }
        }
        AspectList generate(Item item,List<Item> history){
            var direct=AspectTable.get(new ItemStack(item));
            if(!direct.isEmpty()){var result=new AspectList();direct.forEach((key,value)->result.add(Aspect.getAspect(key),value));return result;}
            if(generated.containsKey(item))return generated.get(item);
            var addon=dev.eugheist.tc4modern.api.TC4Addons.provided(new ItemStack(item),level);
            if(addon!=null){generated.put(item,addon);return addon;}
            if(history.contains(item))return null;
            history.add(item);if(history.size()>=100)return null;
            AspectList result=null;
            String id=BuiltInRegistries.ITEM.getKey(item).toString();
            // Original direct registrations whose source tags themselves are recipe-derived.
            // ConfigAspects registers the book with wildcard metadata, so the metadata-42 cheat sheet shares its tags.
            if(id.equals("tc4modern:thaumonomicon")||id.equals("tc4modern:thaumonomicon_cheat")) {
                var source=generate(net.minecraft.world.item.Items.BOOKSHELF,new ArrayList<>(history));
                result=source==null ? new AspectList() : source.copy();
                result.add(Aspect.MAGIC,2).merge(Aspect.MIND,2);
            } else if(id.equals("tc4modern:crucible")) {
                var source=generate(net.minecraft.world.item.Items.CAULDRON,new ArrayList<>(history));
                result=source==null ? new AspectList() : source.copy();
                result.add(Aspect.CRAFT,4).add(Aspect.MAGIC,4);
            } else if(id.equals("tc4modern:salis_mundus")) {
                var source=generate(TC4Modern.BALANCED_SHARD.get(),new ArrayList<>(history));
                result=source==null ? new AspectList() : source.copy();
                result.add(Aspect.MAGIC,2).remove(Aspect.CRYSTAL);
            }
            // Original priority: crucible, arcane, infusion, ordinary crafting.
            var alchemy=crucible.get(item);
            if(result==null && alchemy!=null){
                var catalysts=alchemy.catalyst.getItems();
                var catalystTags=catalysts.length==0?null:generate(catalysts[0].getItem(),new ArrayList<>(history));
                result=RecipeAspectMath.fromCrucible(catalystTags,alchemy.aspects,alchemy.getRecipeOutput().getCount());
            }
            // Original arcane traversal keeps the last matching recipe, including empty tags.
            if(result==null)for(var candidate:arcane.getOrDefault(item,List.of()))result=calculate(candidate,history);
            if(result==null && infusion.containsKey(item))result=calculate(infusion.get(item),history);
            if(result==null){
                int value=Integer.MAX_VALUE;
                for(var candidate:ordinary.getOrDefault(item,List.of())){
                    var possible=calculate(candidate,history);
                    if(possible.visSize()>0 && possible.visSize()<value){result=possible;value=possible.visSize();}
                }
            }
            var complex=thaumcraft.common.config.ConfigComplexAspects.get(BuiltInRegistries.ITEM.getKey(item).toString());
            if(complex!=null){
                var extra=complex.additional().copy();
                if(!complex.inherited().isEmpty()){
                    var inherited=BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.parse(complex.inherited()));
                    var inheritedTags=generate(inherited,new ArrayList<>(history));
                    if(inheritedTags!=null)for(var aspect:inheritedTags.getAspects())extra.add(aspect,inheritedTags.getAmount(aspect));
                }
                if(result!=null)for(var aspect:result.getAspects())extra.add(aspect,result.getAmount(aspect));
                result=extra;
            }
            // Original ConfigAspects complex registrations for metadata zero / wildcard items.
            // Other primal-arrow metadata has no explicit complex bonus in TC4.
            if(id.equals("tc4modern:air_arrow") || id.equals("tc4modern:goggles") || id.equals("tc4modern:arcane_ear")) {
                if(result==null)result=new AspectList();
                result.add(id.equals("tc4modern:air_arrow")?Aspect.WEAPON:Aspect.SENSES,id.equals("tc4modern:air_arrow")?1:4);
            }
            if (result==null || result.visSize()==0) {
                var fallback=ModernAspectCatalog.list("items",BuiltInRegistries.ITEM.getKey(item).toString());
                if (fallback.size()>0) result=fallback;
            }
            result=RecipeAspectMath.capAspects(result,64);generated.put(item,result);return result;
        }
        AspectList calculate(Candidate recipe,List<Item> history){
            List<RecipeAspectMath.IngredientTags> tags=new ArrayList<>();
            for(var ingredient:recipe.ingredients().subList(0,Math.min(9,recipe.ingredients().size()))){
                var choices=ingredient.getItems();if(choices.length==0)continue;
                ItemStack choice=choices[0];AspectList aspects=null;
                if(choices.length>1){
                    choice=ItemStack.EMPTY;
                    for(var option:choices){var possible=generate(option.getItem(),new ArrayList<>(history));if(possible!=null && possible.size()>0){choice=option;aspects=possible;break;}}
                    if(choice.isEmpty())continue;
                }else aspects=generate(choice.getItem(),new ArrayList<>(history));
                var remainder=choice.getCraftingRemainingItem();boolean returnsItself=!remainder.isEmpty() && remainder.is(choice.getItem());
                var container=remainder.isEmpty() || returnsItself?null:generate(remainder.getItem(),new ArrayList<>(history));
                tags.add(new RecipeAspectMath.IngredientTags(aspects,container,returnsItself));
            }
            return RecipeAspectMath.addCost(RecipeAspectMath.fromIngredients(tags,recipe.count()),recipe.cost(),recipe.count());
        }
    }
}
