package dev.eugheist.tc4modern;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.ingredient.IRecipeSlotView;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferError;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandler;
import mezz.jei.api.recipe.transfer.IRecipeTransferHandlerHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.jetbrains.annotations.Nullable;

/**
 * JEI "+" button for 3x3 arcane grids: marks recipe slots nothing available can fill, otherwise sends one template stack per
 * grid slot (preferring stacks the player actually has) for the server to fill. Addons subclass it for their own menus.
 */
public abstract class ArcaneGridTransfer<C extends AbstractContainerMenu, R> implements IRecipeTransferHandler<C, R> {
    final Class<C> menuClass;final RecipeType<R> type;final IRecipeTransferHandlerHelper helper;
    protected ArcaneGridTransfer(Class<C> menuClass, RecipeType<R> type, IRecipeTransferHandlerHelper helper) { this.menuClass = menuClass;this.type = type;this.helper = helper; }

    /** Copies of every stack the transfer may use (inventory, grid, network...). */
    protected abstract List<ItemStack> available(C menu, Player player);
    protected abstract void send(C menu, List<ItemStack> templates);

    @Override public Class<? extends C> getContainerClass() { return menuClass; }
    @Override public Optional<MenuType<C>> getMenuType() { return Optional.empty(); }
    @Override public RecipeType<R> getRecipeType() { return type; }

    @Override public @Nullable IRecipeTransferError transferRecipe(C menu, R recipe, IRecipeSlotsView slots, Player player, boolean maxTransfer, boolean doTransfer) {
        var pool = available(menu, player);
        var check = new ArrayList<ItemStack>();for (var s : pool) check.add(s.copy());
        var missing = new ArrayList<IRecipeSlotView>();
        for (var view : slots.getSlotViews(RecipeIngredientRole.INPUT)) {
            var shown = view.getItemStacks().toList();
            if (shown.isEmpty()) continue;
            boolean found = false;
            for (var want : shown) { for (var have : check) if (!have.isEmpty() && have.is(want.getItem())) { have.shrink(1);found = true;break; } if (found) break; }
            if (!found) missing.add(view);
        }
        if (!missing.isEmpty()) return helper.createUserErrorForMissingSlots(Component.translatable("jei.tooltip.error.recipe.transfer.missing"), missing);
        if (!doTransfer) return null;
        Object raw = recipe instanceof RecipeHolder<?> holder ? holder.value() : recipe;
        var grid = ArcaneGridFill.grid(raw);var templates = new ArrayList<ItemStack>(9);
        for (Ingredient ingredient : grid) {
            if (ingredient.isEmpty()) { templates.add(ItemStack.EMPTY);continue; }
            ItemStack pick = null;
            for (var have : pool) if (!have.isEmpty() && ingredient.test(have)) { pick = have.copyWithCount(1);break; }
            templates.add(pick != null ? pick : ingredient.getItems().length > 0 ? ingredient.getItems()[0].copyWithCount(1) : ItemStack.EMPTY);
        }
        send(menu, templates);
        return null;
    }

    /** Arcane workbench: the player's inventory plus what already lies in the grid. */
    static final class Workbench<R> extends ArcaneGridTransfer<ArcaneWorkbenchMenu, R> {
        Workbench(RecipeType<R> type, IRecipeTransferHandlerHelper helper) { super(ArcaneWorkbenchMenu.class, type, helper); }
        @Override protected List<ItemStack> available(ArcaneWorkbenchMenu menu, Player player) {
            var list = new ArrayList<ItemStack>();
            for (var s : player.getInventory().items) if (!s.isEmpty()) list.add(s.copy());
            for (int i = 0; i < 9; i++) if (!menu.table.getItem(i).isEmpty()) list.add(menu.table.getItem(i).copy());
            return list;
        }
        @Override protected void send(ArcaneWorkbenchMenu menu, List<ItemStack> templates) { net.neoforged.neoforge.network.PacketDistributor.sendToServer(new ArcaneGridFill.Fill(templates)); }
    }
}
