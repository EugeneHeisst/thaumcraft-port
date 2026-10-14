package dev.eugheist.tc4modern;

import java.util.*;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.recipe.*;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.*;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import thaumcraft.api.aspects.*;
import thaumcraft.api.crafting.CrucibleRecipe;

/** Optional client bridge. Nothing outside this JEI entry point links the JEI API. */
@JeiPlugin
public final class TC4JeiPlugin implements IModPlugin {
    public static final RecipeType<ArcaneRecipes.Entry> ARCANE = RecipeType.create(TC4Modern.MODID,"arcane",ArcaneRecipes.Entry.class);
    public static final RecipeType<CrucibleRecipe> CRUCIBLE = RecipeType.create(TC4Modern.MODID,"crucible",CrucibleRecipe.class);
    public static final RecipeType<InfusionDisplay> INFUSION = RecipeType.create(TC4Modern.MODID,"infusion",InfusionDisplay.class);
    public record InfusionDisplay(String id,String research,ItemStack input,List<List<ItemStack>> components,
                                  ItemStack output,AspectList aspects,int instability,int xp,Component note) {
        InfusionDisplay(String id,String research,ItemStack input,List<List<ItemStack>> components,ItemStack output,AspectList aspects,int instability){this(id,research,input,components,output,aspects,instability,0,Component.empty());}
    }
    @Override public ResourceLocation getPluginUid(){return AlchemyContent.id("jei");}
    @Override public void registerCategories(IRecipeCategoryRegistration r){var h=r.getJeiHelpers().getGuiHelper();r.addRecipeCategories(new ArcaneCategory(h),new CrucibleCategory(h),new InfusionCategory(h));}
    @Override public void registerRecipes(IRecipeRegistration r){
        r.addRecipes(ARCANE,arcaneRecipes());r.addRecipes(CRUCIBLE,CrucibleRecipes.entries());r.addRecipes(INFUSION,infusionRecipes());
    }
    /** Ask the production composite-wand recipe for every valid pair, including staff and sceptre forms. */
    static List<ArcaneRecipes.Entry> arcaneRecipes(){
        var result=new ArrayList<>(ArcaneRecipes.entries());var unlocked=new HashSet<String>();
        WandParts.CAPS.values().forEach(c->unlocked.add(c.research()));WandParts.RODDEFS.values().forEach(r->unlocked.add(r.research()));unlocked.add("SCEPTRE");
        for(var cap:WandParts.CAPS.values())for(var rod:WandParts.RODDEFS.values()){
            var grid=new ArrayList<ItemStack>(Collections.nCopies(9,ItemStack.EMPTY));
            grid.set(2,new ItemStack(cap.item().get()));grid.set(6,new ItemStack(cap.item().get()));grid.set(4,new ItemStack(rod.item().get()));
            var recipe=WandParts.recipe(grid,unlocked);if(recipe!=null)result.add(recipe);
            result.add(WandParts.sceptreEntry(cap,rod));
        }
        return List.copyOf(result);
    }
    static List<InfusionDisplay> infusionRecipes(){
        var result=new ArrayList<InfusionDisplay>();
        for(var recipe:InfusionItemRecipes.all()){
            var input=new ItemStack(recipe.central().get());var components=new ArrayList<List<ItemStack>>();
            for(var part:recipe.componentStacks()){
                // Production matcher accepts all four original raw-fish equivalents.
                components.add(NoteData.tag(part).getBoolean("TC4AnyRawFish")?List.of(new ItemStack(Items.COD),new ItemStack(Items.SALMON),new ItemStack(Items.PUFFERFISH),new ItemStack(Items.TROPICAL_FISH)):List.of(part));
            }
            result.add(new InfusionDisplay(recipe.key(),recipe.research(),input,components,recipe.outputStack(input),recipe.aspects(),recipe.instability()));
        }
        result.add(new InfusionDisplay("JarBrain","JARBRAIN",BrainJarRecipe.input(),BrainJarRecipe.ingredients().stream().map(List::of).toList(),BrainJarRecipe.output(),BrainJarRecipe.cost(),BrainJarRecipe.INSTABILITY));
        for(var item:BuiltInRegistries.ITEM)if(item instanceof thaumcraft.api.IRunicArmor){
            var input=new ItemStack(item);
            for(int upgrade=0;upgrade<5;upgrade++){
                result.add(new InfusionDisplay("RunicAugmentation","RUNICAUGMENTATION",input,RunicAugmentation.components(input).stream().map(List::of).toList(),RunicAugmentation.output(input),RunicAugmentation.cost(input),RunicAugmentation.instability(input)));
                input=RunicAugmentation.output(input);
            }
        }
        var level=Minecraft.getInstance().level;
        if(level!=null)for(var recipe:thaumcraft.common.tiles.infusion.InfusionEnchantments.all(()->InfusionContent.SALIS.get())){
            var enchantment=thaumcraft.common.tiles.infusion.InfusionEnchantments.holder(level.registryAccess(),recipe.enchantment());
            for(var item:BuiltInRegistries.ITEM){
                var input=new ItemStack(item);if(!recipe.acceptsCentral(input,enchantment))continue;
                for(int rank=1;rank<=enchantment.value().getMaxLevel();rank++){
                    var cost=recipe.aspects().copy();float modifier=recipe.getEssentiaMod(input,enchantment);
                    for(var aspect:cost.getAspects())cost.add(aspect,(int)(cost.getAmount(aspect)*modifier));
                    var output=input.copy();output.enchant(enchantment,rank);
                    result.add(new InfusionDisplay(recipe.key(),"INFUSIONENCHANTMENT",input,recipe.components().stream().map(c->List.of(new ItemStack(c.get()))).toList(),output,cost,recipe.calcInstability(input),recipe.calcXP(input,enchantment),net.minecraft.world.item.enchantment.Enchantment.getFullname(enchantment,rank)));
                    input=output;
                }
            }
        }
        return List.copyOf(result);
    }
    @Override public void registerExtraIngredients(IExtraIngredientRegistration r){
        var extras=new ArrayList<ItemStack>();arcaneRecipes().forEach(e->extras.add(e.output()));
        InfusionItemRecipes.all().forEach(e->extras.add(e.outputStack(new ItemStack(e.central().get()))));
        for(var aspect:Aspect.aspects.values())extras.add(EssentiaItems.phialOf(aspect,8));
        r.addExtraItemStacks(extras);
    }
    @Override public void registerRecipeCatalysts(IRecipeCatalystRegistration r){
        r.addRecipeCatalysts(ARCANE,TC4Modern.ARCANE_WORKBENCH_ITEM.get(),TC4Modern.TABLE_ITEM.get(),TC4Modern.BASIC_WAND.get());r.addRecipeCatalysts(CRUCIBLE,item("crucible"),item("alchemical_construct"),Thaumatorium.BRAIN.get());r.addRecipeCatalysts(INFUSION,InfusionContent.MATRIX.get(),InfusionContent.PEDESTAL.get());
    }
    static Item item(String path){return BuiltInRegistries.ITEM.get(AlchemyContent.id(path));}
    @Override public void registerRecipeTransferHandlers(IRecipeTransferRegistration r){var h=r.getTransferHelper();r.addRecipeTransferHandler(new ArcaneGridTransfer.Workbench<>(ARCANE,h),ARCANE);r.addRecipeTransferHandler(new ArcaneGridTransfer.Workbench<>(mezz.jei.api.constants.RecipeTypes.CRAFTING,h),mezz.jei.api.constants.RecipeTypes.CRAFTING);}
    @Override public void registerGuiHandlers(IGuiHandlerRegistration r){r.addRecipeClickArea(ArcaneWorkbenchScreen.class,132,64,20,16,ARCANE);}
    @Override public void registerItemSubtypes(ISubtypeRegistration r){
        var items=new HashSet<Item>();items.add(WandParts.WAND.get());
        ArcaneRecipes.entries().stream().map(e->e.output()).filter(s->!NoteData.tag(s).isEmpty()).forEach(s->items.add(s.getItem()));
        InfusionItemRecipes.all().stream().filter(e->e.augment()).forEach(e->items.add(e.central().get()));
        items.add(EssentiaDevices.ESSENTIA_PHIAL.get());
        for(var item:BuiltInRegistries.ITEM)if(item instanceof thaumcraft.api.IRunicArmor)items.add(item);
        for(var item:items)r.registerSubtypeInterpreter(item,new ISubtypeInterpreter<ItemStack>(){
            @Override public Object getSubtypeData(ItemStack stack,UidContext context){return subtype(stack);}
            @Override public String getLegacyStringSubtypeInfo(ItemStack stack,UidContext context){return subtype(stack);}
        });
    }
    static String subtype(ItemStack stack){
        var data=NoteData.tag(stack);var stable=new net.minecraft.nbt.CompoundTag();
        for(String key:List.of("cap","rod","sceptre","color","advanced","goggles","mask","Aspects","RS.HARDEN"))if(data.contains(key))stable.put(key,data.get(key).copy());
        return stable.isEmpty()?"":stable.toString();
    }
    @Override public void onRuntimeAvailable(IJeiRuntime runtime){if(Boolean.getBoolean("tc4modern.jeiAuditOnly"))JeiAudit.start(runtime);}

    private abstract static class Category<T> implements IRecipeCategory<T>{
        private final RecipeType<T> type;private final Component title;private final IDrawable icon;
        Category(RecipeType<T> type,String title,Item icon,IGuiHelper h){this.type=type;this.title=Component.translatable(title);this.icon=h.createDrawableIngredient(VanillaTypes.ITEM_STACK,new ItemStack(icon));}
        @Override public RecipeType<T> getRecipeType(){return type;}
        @Override public Component getTitle(){return title;}
        @Override public IDrawable getIcon(){return icon;}
        @Override public int getWidth(){return 160;}
        @Override public int getHeight(){return 246;}
        abstract AspectList aspects(T recipe);abstract List<String> research(T recipe);
        int aspectColumns(){return 6;} int aspectY(){return 174;}
        @Override public void draw(T recipe,IRecipeSlotsView slots,GuiGraphics g,double mx,double my){
            var list=aspects(recipe).getAspectsSorted();
            for(int i=0;i<list.length;i++){int cols=aspectColumns(),row=i/cols,n=Math.min(cols,list.length-row*cols);RecipeBookBridge.drawTag(g,list[i],aspects(recipe).getAmount(list[i]),(160-n*23)/2+i%cols*23,aspectY()+row*20);}
            var keys=research(recipe).stream().filter(s->!s.isEmpty()).toList();
            if(!keys.isEmpty()){
                boolean known=ClientKnowledge.knowledge.getList("Research",8).stream().map(t->t.getAsString()).collect(java.util.stream.Collectors.toSet()).containsAll(keys);
                centered(g,Component.translatable(known?"jei.tc4modern.research_known":"jei.tc4modern.research_required"),230,known?0x48623A:0x85403A);
            }
        }
        @Override public void getTooltip(ITooltipBuilder tooltip,T recipe,IRecipeSlotsView slots,double mx,double my){
            var list=aspects(recipe).getAspectsSorted();for(int i=0;i<list.length;i++){int cols=aspectColumns(),row=i/cols,n=Math.min(cols,list.length-row*cols),x=(160-n*23)/2+i%cols*23,y=aspectY()+row*20;if(mx>=x&&mx<x+18&&my>=y&&my<y+18){tooltip.add(Component.literal(list[i].getName()+": "+aspects(recipe).getAmount(list[i])));tooltip.add(Component.translatable(getRecipeType()==ARCANE?"jei.tc4modern.vis_base":"jei.tc4modern.essentia"));}}
            if(my>=226)for(var key:research(recipe))if(!key.isEmpty())tooltip.add(ResearchProgress.name(key));
        }
        static void centered(GuiGraphics g,Component text,int y,int color){var f=Minecraft.getInstance().font;float scale=Math.min(1,152f/Math.max(1,f.width(text)));g.pose().pushPose();g.pose().translate(80,y,0);g.pose().scale(scale,scale,1);g.drawString(f,text,-f.width(text)/2,0,color,false);g.pose().popPose();}
    }
    private static final class ArcaneCategory extends Category<ArcaneRecipes.Entry>{
        ArcaneCategory(IGuiHelper h){super(ARCANE,"recipe.type.arcane",TC4Modern.TABLE_ITEM.get(),h);}
        @Override AspectList aspects(ArcaneRecipes.Entry r){return r.cost();}
        @Override List<String> research(ArcaneRecipes.Entry r){if(r.output().is(WandParts.WAND.get())){var list=new ArrayList<>(List.of(WandParts.cap(r.output()).research(),WandParts.rod(r.output()).research()));if(WandParts.isSceptre(r.output()))list.add("SCEPTRE");return list;}return List.of(r.research());}
        @Override public void setRecipe(IRecipeLayoutBuilder b,ArcaneRecipes.Entry r,IFocusGroup focus){
            b.addOutputSlot(72,32).addItemStack(r.output());for(int i=0;i<r.ingredients().size();i++)if(!r.ingredients().get(i).isEmpty())b.addInputSlot(40+i%r.width()*32,76+i/r.width()*32).addIngredients(r.ingredients().get(i));
        }
        @Override public void draw(ArcaneRecipes.Entry r,IRecipeSlotsView s,GuiGraphics g,double x,double y){RecipeBookBridge.background(g,24,0,2,32,112,15,52,52);RecipeBookBridge.background(g,24,0,20,12,20,3,16,16);super.draw(r,s,g,x,y);}
    }
    private static final class CrucibleCategory extends Category<CrucibleRecipe>{
        CrucibleCategory(IGuiHelper h){super(CRUCIBLE,"recipe.type.crucible",item("crucible"),h);}
        @Override int aspectColumns(){return 3;}
        @Override int aspectY(){return 124;}
        @Override AspectList aspects(CrucibleRecipe r){return r.aspects;}
        @Override List<String> research(CrucibleRecipe r){return List.of(r.key);}
        @Override public void setRecipe(IRecipeLayoutBuilder b,CrucibleRecipe r,IFocusGroup focus){b.addInputSlot(40,64).addIngredients(r.catalyst);b.addOutputSlot(72,32).addItemStack(r.getRecipeOutput());}
        @Override public void draw(CrucibleRecipe r,IRecipeSlotsView s,GuiGraphics g,double x,double y){RecipeBookBridge.background(g,24,24,0,0,0,3,56,17);RecipeBookBridge.background(g,24,24,0,32,0,20,56,48);RecipeBookBridge.background(g,24,24,21,24,100,84,11,13);super.draw(r,s,g,x,y);}
    }
    private static final class InfusionCategory extends Category<InfusionDisplay>{
        InfusionCategory(IGuiHelper h){super(INFUSION,"recipe.type.infusion",InfusionContent.MATRIX.get().asItem(),h);}
        @Override AspectList aspects(InfusionDisplay r){return r.aspects();}
        @Override List<String> research(InfusionDisplay r){return List.of(r.research());}
        @Override public void setRecipe(IRecipeLayoutBuilder b,InfusionDisplay r,IFocusGroup focus){
            b.addInputSlot(72,94).addItemStack(r.input());b.addOutputSlot(72,28).addItemStack(r.output());
            for(int i=0;i<r.components().size();i++){double angle=-Math.PI/2+i*Math.PI*2/r.components().size();b.addInputSlot(72+(int)(Math.cos(angle)*40),94+(int)(Math.sin(angle)*40)).addItemStacks(r.components().get(i));}
        }
        @Override public void draw(InfusionDisplay r,IRecipeSlotsView s,GuiGraphics g,double x,double y){RecipeBookBridge.background(g,24,20,0,0,0,3,56,17);RecipeBookBridge.background(g,24,20,0,19,200,77,60,44);super.draw(r,s,g,x,y);if(!r.note().getString().isEmpty())centered(g,r.note().copy().withStyle(net.minecraft.ChatFormatting.DARK_PURPLE),8,0x685078);if(r.xp()>0)centered(g,Component.translatable("jei.tc4modern.xp",r.xp()),154,0x48623A);centered(g,Component.translatable("tc.inst").append(" ").append(Component.translatable("tc.inst."+Math.min(5,r.instability()/2))),214,0x685078);}
    }
}
