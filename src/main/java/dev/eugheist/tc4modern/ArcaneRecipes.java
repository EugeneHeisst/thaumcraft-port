package dev.eugheist.tc4modern;
import com.google.gson.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.ArcanePattern;
import java.util.*;
/** Modern ingredient bridge for local original arcane recipe records. */
public final class ArcaneRecipes {
    public record Entry(String key,String research,int width,int height,List<Ingredient> ingredients,ItemStack output,AspectList cost,ArcanePattern<ItemStack> shape){
        public boolean matches(List<ItemStack> grid,Set<String> completed){if(key.equals("WandGoldGreatwood")&&!completed.contains("CAP_gold"))return false;return shape.matches(grid.stream().map(s->s.isEmpty()?null:s).collect(java.util.stream.Collectors.toList()),completed::contains);}
    }
    private static List<Entry> entries;
    private static List<Entry> base(){if(entries==null)entries=List.of(load("golem_upgrade_fire"),load("golem_tiny_hat"),load("golem_tiny_fez"),load("golem_tiny_bowtie"),load("golem_tiny_glasses"),load("golem_tiny_dart"),load("golem_tiny_visor"),load("golem_tiny_armor"),load("golem_tiny_hammer"),load("golem_upgrade_order"),load("golem_upgrade_entropy"),load("golem_upgrade_air"),load("golem_upgrade_earth"),load("golem_upgrade_water"),load("hungry_chest"),load("golem_fetter"),load("golem_bell"),load("golem_core_blank"),load("arcane_door"),load("warded_glass"),load("iron_key"),load("gold_key"),load("arcane_pressure_plate"),load("arcane_bore_base"),load("bone_bow"),load("primal_arrow_0"),load("primal_arrow_1"),load("primal_arrow_2"),load("primal_arrow_3"),load("primal_arrow_4"),load("primal_arrow_5"),load("banner_0"),load("banner_1"),load("banner_2"),load("banner_3"),load("banner_4"),load("banner_5"),load("banner_6"),load("banner_7"),load("banner_8"),load("banner_9"),load("banner_10"),load("banner_11"),load("banner_12"),load("banner_13"),load("banner_14"),load("banner_15"),load("arcane_lamp"),load("flux_scrubber"),load("levitator"),load("mirror_glass"),load("arcane_ear"),load("paving_stone_travel"),load("paving_stone_warding"),load("arcane_spa"),load("deconstructor"),load("advanced_deconstructor"),load("enchanted_fabric"),load("arcane_stone"),load("goggles"),load("robe_chest"),load("robe_legs"),load("robe_boots"),load("bellows"),load("gold_cap"),load("greatwood_rod"),load("node_stabilizer"),load("node_converter"),load("vis_relay"),load("vis_charge_relay"),load("vis_filter"),load("alchemical_furnace"),load("alembic"),load("warded_jar"),load("essentia_tube"),load("essentia_valve"),load("essentia_tube_filter"),load("essentia_tube_restricted"),load("essentia_tube_oneway"),load("essentia_buffer"),load("alchemical_construct"),load("advanced_alchemical_construct"),load("centrifuge"),load("essentia_crystalizer"),load("void_jar"),load("mnemonic_matrix"),load("runic_matrix"),load("arcane_pedestal"),load("focus_pouch"),load("focus_fire"),load("focus_frost"),load("focus_shock"),load("focus_excavation"),load("focus_trade"),load("focus_primal"),load("greatwood_staff_rod"),load("obsidian_staff_rod"),load("ice_staff_rod"),load("quartz_staff_rod"),load("reed_staff_rod"),load("blaze_staff_rod"),load("bone_staff_rod"),load("silverwood_staff_rod"),load("primal_charm"),load("copper_cap"),load("thaumium_cap_inert"),load("void_cap_inert"),load("focal_manipulator"));return entries;}
    /** Original recipes followed by addon recipes registered through TC4Addons. */
    public static List<Entry> entries(){var addon=dev.eugheist.tc4modern.api.TC4Addons.arcaneRecipes();if(addon.isEmpty())return base();var all=new java.util.ArrayList<>(base());all.addAll(addon);return List.copyOf(all);}
    public static Entry find(List<ItemStack> grid,Set<String> completed){var e=entries().stream().filter(x->x.matches(grid,completed)).findFirst().orElse(null);return e!=null?e:WandParts.recipe(grid,completed);}
    private static Entry load(String id){
        try(var stream=ArcaneRecipes.class.getResourceAsStream("/data/tc4modern/arcane_recipe/"+id+".json")){
            if(stream==null)throw new IllegalStateException("Missing original arcane recipe: "+id);
            var data=JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            var pattern=data.getAsJsonArray("pattern");int height=pattern.size(),width=pattern.get(0).getAsString().length();
            List<Ingredient> ingredients=new ArrayList<>();List<java.util.function.Predicate<ItemStack>> predicates=new ArrayList<>();
            var keys=data.getAsJsonObject("key_items");
            for(var row:pattern){if(row.getAsString().length()!=width)throw new IllegalStateException("Unequal arcane rows");for(char c:row.getAsString().toCharArray()){
                Ingredient ingredient=Ingredient.EMPTY;
                if(c!=' '){var key=keys.getAsJsonObject(String.valueOf(c));
                    if(key.has("item")){var name=ResourceLocation.parse(key.get("item").getAsString());if(!BuiltInRegistries.ITEM.containsKey(name))throw new IllegalStateException("Missing ingredient: "+name);ingredient=Ingredient.of(BuiltInRegistries.ITEM.get(name));}
                    else ingredient=Ingredient.of(TagKey.create(Registries.ITEM,ResourceLocation.parse(key.get("tag").getAsString())));
                }
                ingredients.add(ingredient);predicates.add(c==' '?null:ingredient::test);
            }}
            var result=data.getAsJsonObject("result");var name=ResourceLocation.parse(result.get("id").getAsString());if(!BuiltInRegistries.ITEM.containsKey(name))throw new IllegalStateException("Missing arcane result: "+name);
            var output=new ItemStack(BuiltInRegistries.ITEM.get(name),result.get("count").getAsInt());if(result.has("color")){var tag=new net.minecraft.nbt.CompoundTag();tag.putByte("color",result.get("color").getAsByte());output.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));}var cost=new AspectList();
            for(var pair:data.getAsJsonObject("aspects").entrySet()){var aspect=Aspect.getAspect(pair.getKey());if(aspect==null || !aspect.isPrimal())throw new IllegalStateException("Invalid vis aspect");cost.add(aspect,pair.getValue().getAsInt());}
            String research=data.get("research").getAsString();return new Entry(data.get("key").getAsString(),research,width,height,List.copyOf(ingredients),output,cost,new ArcanePattern<>(research,width,height,predicates).setMirrored(!data.has("mirrored")||data.get("mirrored").getAsBoolean()).setShapeless(data.has("shapeless")&&data.get("shapeless").getAsBoolean()));
        }catch(java.io.IOException error){throw new IllegalStateException(error);}
    }
}
