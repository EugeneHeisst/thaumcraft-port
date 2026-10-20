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
    /** Original recipe definitions in original order, by data file name (also the tc4modern:arcane/<name> recipe id). */
    private static final List<String> NAMES=List.of("golem_upgrade_fire","golem_tiny_hat","golem_tiny_fez","golem_tiny_bowtie","golem_tiny_glasses","golem_tiny_dart","golem_tiny_visor","golem_tiny_armor","golem_tiny_hammer","golem_upgrade_order","golem_upgrade_entropy","golem_upgrade_air","golem_upgrade_earth","golem_upgrade_water","hungry_chest","golem_fetter","golem_bell","golem_core_blank","arcane_door","warded_glass","iron_key","gold_key","arcane_pressure_plate","arcane_bore_base","bone_bow","primal_arrow_0","primal_arrow_1","primal_arrow_2","primal_arrow_3","primal_arrow_4","primal_arrow_5","banner_0","banner_1","banner_2","banner_3","banner_4","banner_5","banner_6","banner_7","banner_8","banner_9","banner_10","banner_11","banner_12","banner_13","banner_14","banner_15","arcane_lamp","flux_scrubber","levitator","mirror_glass","arcane_ear","paving_stone_travel","paving_stone_warding","arcane_spa","deconstructor","enchanted_fabric","arcane_stone","goggles","robe_chest","robe_legs","robe_boots","bellows","gold_cap","greatwood_rod","node_stabilizer","node_converter","vis_relay","vis_charge_relay","vis_filter","alchemical_furnace","alembic","warded_jar","essentia_tube","essentia_valve","resonator","essentia_tube_filter","essentia_tube_restricted","essentia_tube_oneway","essentia_buffer","alchemical_construct","advanced_alchemical_construct","centrifuge","essentia_crystalizer","void_jar","mnemonic_matrix","runic_matrix","arcane_pedestal","focus_pouch","focus_fire","focus_frost","focus_shock","focus_excavation","focus_trade","focus_primal","greatwood_staff_rod","obsidian_staff_rod","ice_staff_rod","quartz_staff_rod","reed_staff_rod","blaze_staff_rod","bone_staff_rod","silverwood_staff_rod","primal_charm","copper_cap","thaumium_cap_inert","void_cap_inert","focal_manipulator");
    private static java.util.Map<String,Entry> byName;
    private static synchronized java.util.Map<String,Entry> loaded(){if(byName==null){var map=new java.util.LinkedHashMap<String,Entry>();for(var name:NAMES)map.put(name,load(name));byName=java.util.Collections.unmodifiableMap(map);}return byName;}
    private static List<Entry> base(){return List.copyOf(loaded().values());}
    public static List<String> names(){return NAMES;}
    public static Entry byName(String name){return NAMES.contains(name)?loaded().get(name):null;}
    /** Original recipes whose tc4modern:arcane/<name> recipe is present (issue #11), followed by addon recipes registered through TC4Addons. */
    public static List<Entry> entries(){
        var enabled=ArcaneRecipeIds.enabled();List<Entry> base;
        if(enabled==null)base=base();else{var list=new java.util.ArrayList<Entry>();for(var e:loaded().entrySet())if(enabled.contains(e.getKey()))list.add(e.getValue());base=list;}
        var addon=dev.eugheist.tc4modern.api.TC4Addons.arcaneRecipes();if(addon.isEmpty())return List.copyOf(base);var all=new java.util.ArrayList<>(base);all.addAll(addon);return List.copyOf(all);
    }
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
