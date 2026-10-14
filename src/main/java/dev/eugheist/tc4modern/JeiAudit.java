package dev.eugheist.tc4modern;

import java.util.List;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Opt-in native check, loaded only by the optional JEI plugin. */
final class JeiAudit {
    private static IJeiRuntime runtime;private static int ticks;
    static void start(IJeiRuntime value){runtime=value;net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(JeiAudit::tick);}
    static void check(boolean value,String text){if(!value)throw new IllegalStateException("TC4_JEI FAIL "+text);System.out.println("TC4_JEI PASS "+text);}
    static <T> List<T> find(mezz.jei.api.recipe.RecipeType<T> type,ItemStack stack,RecipeIngredientRole role){
        var focus=runtime.getJeiHelpers().getFocusFactory().createFocus(role,VanillaTypes.ITEM_STACK,stack);
        return runtime.getRecipeManager().createRecipeLookup(type).limitFocus(List.of(focus)).get().toList();
    }
    private static <T> void show(mezz.jei.api.recipe.RecipeType<T> type,ItemStack stack){var focus=runtime.getJeiHelpers().getFocusFactory().createFocus(RecipeIngredientRole.OUTPUT,VanillaTypes.ITEM_STACK,stack);runtime.getRecipesGui().showRecipes(runtime.getRecipeManager().getRecipeCategory(type),find(type,stack,RecipeIngredientRole.OUTPUT),List.of(focus));}
    static void tick(net.neoforged.neoforge.client.event.ClientTickEvent.Post event){
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;ticks++;
        if(ticks==30){
            var m=runtime.getRecipeManager();
            check(m.createRecipeLookup(TC4JeiPlugin.ARCANE).get().count()==TC4JeiPlugin.arcaneRecipes().size(),"all static and composite arcane recipes registered");
            check(m.createRecipeLookup(TC4JeiPlugin.CRUCIBLE).get().count()==CrucibleRecipes.entries().size(),"all crucible recipes registered");
            check(m.createRecipeLookup(TC4JeiPlugin.INFUSION).get().count()==TC4JeiPlugin.infusionRecipes().size(),"all item infusions, brain jar, runic and enchantment variants registered");
            check(!find(TC4JeiPlugin.CRUCIBLE,new ItemStack(TC4Modern.NITOR.get()),RecipeIngredientRole.OUTPUT).isEmpty(),"R lookup finds nitor");
            check(find(TC4JeiPlugin.CRUCIBLE,new ItemStack(Items.COAL),RecipeIngredientRole.INPUT).stream().anyMatch(r->r.getRecipeOutput().is(TC4Modern.ALUMENTUM.get())),"U lookup finds coal alumentum");
            check(find(TC4JeiPlugin.CRUCIBLE,EssentiaItems.phialOf(thaumcraft.api.aspects.Aspect.TAINT,8),RecipeIngredientRole.INPUT).stream().anyMatch(r->r.key.equals("BOTTLETAINT")),"exact vitium phial catalyst indexed");
            check(find(TC4JeiPlugin.CRUCIBLE,EssentiaItems.phialOf(thaumcraft.api.aspects.Aspect.AIR,8),RecipeIngredientRole.INPUT).stream().noneMatch(r->r.key.equals("BOTTLETAINT")),"air phial does not match vitium catalyst");
            var wand=WandParts.make(WandParts.CAPS.get("thaumium"),WandParts.RODDEFS.get("silverwood"));
            check(find(TC4JeiPlugin.ARCANE,wand,RecipeIngredientRole.OUTPUT).stream().allMatch(r->WandParts.cap(r.output()).tag().equals("thaumium")&&WandParts.rod(r.output()).tag().equals("silverwood"))&&!find(TC4JeiPlugin.ARCANE,wand,RecipeIngredientRole.OUTPUT).isEmpty(),"composite wand subtype lookup is exact");
            check(find(TC4JeiPlugin.INFUSION,new ItemStack(Items.SALMON),RecipeIngredientRole.INPUT).stream().anyMatch(r->r.id().equals("BootsTraveller")),"raw fish alternatives indexed");
            check(m.createRecipeCatalystLookup(TC4JeiPlugin.ARCANE).get().findAny().isPresent()&&m.createRecipeCatalystLookup(TC4JeiPlugin.CRUCIBLE).get().findAny().isPresent()&&m.createRecipeCatalystLookup(TC4JeiPlugin.INFUSION).get().findAny().isPresent(),"all three station catalysts registered");
            check(m.createRecipeLookup(TC4JeiPlugin.INFUSION).get().anyMatch(r->r.id().equals("InfEnchRepair")&&r.xp()>0),"Repair infusion enchantment has actual XP and output");
            check(m.createRecipeLookup(TC4JeiPlugin.INFUSION).get().anyMatch(r->r.id().equals("RunicAugmentation")&&NoteData.tag(r.output()).getByte("RS.HARDEN")==5),"runic augmentation levels registered");
            check(!mc.getResourceManager().getResource(AlchemyContent.id("textures/gui/gui_researchbook_overlay.png")).isEmpty(),"original recipe overlay texture loaded");
            System.out.println("TC4_JEI COUNTS arcane="+m.createRecipeLookup(TC4JeiPlugin.ARCANE).get().count()+" crucible="+m.createRecipeLookup(TC4JeiPlugin.CRUCIBLE).get().count()+" infusion="+m.createRecipeLookup(TC4JeiPlugin.INFUSION).get().count());
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
        }
        if(ticks==50)show(TC4JeiPlugin.ARCANE,new ItemStack(TC4Modern.GOGGLES.get()));
        if(ticks==90)LiquidDeathAudit.shot(mc,"jei-arcane.png");
        if(ticks==110)show(TC4JeiPlugin.CRUCIBLE,new ItemStack(TC4Modern.NITOR.get()));
        if(ticks==150)LiquidDeathAudit.shot(mc,"jei-crucible.png");
        if(ticks==170)show(TC4JeiPlugin.INFUSION,new ItemStack(WandParts.THAUMIUM_CAP.get()));
        if(ticks==210)LiquidDeathAudit.shot(mc,"jei-infusion.png");
        if(ticks==230)show(TC4JeiPlugin.ARCANE,WandParts.make(WandParts.CAPS.get("thaumium"),WandParts.RODDEFS.get("silverwood")));
        if(ticks==270)LiquidDeathAudit.shot(mc,"jei-wand.png");
        if(ticks==280){var recipe=runtime.getRecipeManager().createRecipeLookup(TC4JeiPlugin.INFUSION).get().filter(r->r.id().equals("InfEnchRepair")).findFirst().orElseThrow();runtime.getRecipesGui().showRecipes(runtime.getRecipeManager().getRecipeCategory(TC4JeiPlugin.INFUSION),List.of(recipe),List.of());}
        if(ticks==320)LiquidDeathAudit.shot(mc,"jei-enchantment.png");
        if(ticks==350){check(mc.screen!=null&&mc.screen.getClass().getName().contains("RecipesGui"),"actual JEI recipe GUI rendered");mc.setScreen(null);var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{var p=server.getPlayerList().getPlayer(id);p.getInventory().clearContent();p.openMenu(new net.minecraft.world.SimpleMenuProvider((i,inv,pl)->new ArcaneWorkbenchMenu(i,inv,net.minecraft.world.inventory.ContainerLevelAccess.NULL,new net.minecraft.world.SimpleContainer(10)),net.minecraft.network.chat.Component.empty()));});}
        if(ticks==370)transfer(mc,false);
        if(ticks==375)mc.getSingleplayerServer().execute(()->{var p=mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.GLASS_PANE,10));p.getInventory().add(new ItemStack(net.minecraft.world.item.Items.OAK_SLAB,2));});
        if(ticks==390)transfer(mc,true);
        if(ticks==410){var server=mc.getSingleplayerServer();var id=mc.player.getUUID();server.execute(()->{var p=server.getPlayerList().getPlayer(id);var m=(ArcaneWorkbenchMenu)p.containerMenu;var r=jar();var g=ArcaneGridFill.grid(r);boolean ok=true;for(int i=0;i<9;i++){var have=m.table.getItem(i);if(g.get(i).isEmpty()!=have.isEmpty()||!have.isEmpty()&&!g.get(i).test(have))ok=false;}check(ok,"JEI + fills the arcane workbench grid from the inventory "+m.table.getItems());p.closeContainer();});}
        if(ticks==420){System.out.println("TC4_JEI ALL PASS");mc.stop();}
    }
    static ArcaneRecipes.Entry jar(){return ArcaneRecipes.entries().stream().filter(e->e.key().equals("WardedJar")).findFirst().orElseThrow();}
    /** Real JEI transfer handler lookup for the open arcane workbench and the warded jar recipe. */
    static void transfer(net.minecraft.client.Minecraft mc,boolean expectOk){
        check(mc.player.containerMenu instanceof ArcaneWorkbenchMenu,"arcane workbench menu open on the client");
        var category=runtime.getRecipeManager().getRecipeCategory(TC4JeiPlugin.ARCANE);
        var handler=runtime.getRecipeTransferManager().getRecipeTransferHandler(mc.player.containerMenu,category);
        check(handler.isPresent(),"JEI has a transfer handler (+ button) for arcane recipes in the arcane workbench");
        var layout=runtime.getRecipeManager().createRecipeLayoutDrawable(category,jar(),runtime.getJeiHelpers().getFocusFactory().getEmptyFocusGroup()).orElseThrow();
        @SuppressWarnings({"unchecked","rawtypes"}) var error=((mezz.jei.api.recipe.transfer.IRecipeTransferHandler)handler.get()).transferRecipe(mc.player.containerMenu,jar(),layout.getRecipeSlotsView(),mc.player,false,expectOk);
        if(expectOk)check(error==null,"transfer accepted with the items in the inventory");else check(error!=null,"missing items are reported before transfer");
    }
}
