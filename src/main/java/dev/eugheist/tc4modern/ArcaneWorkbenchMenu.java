package dev.eugheist.tc4modern;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
/** Original slot coordinates, with modern server recipe and inventory synchronization. */
public final class ArcaneWorkbenchMenu extends AbstractContainerMenu {
    public final SimpleContainer table;
    public final ArcaneCraftingGrid grid;
    private final ResultContainer result=new ResultContainer();
    private final Player owner;
    private final ContainerLevelAccess access;
    private final net.minecraft.world.ContainerListener listener;
    public ArcaneWorkbenchMenu(int id,Inventory inventory){this(id,inventory,ContainerLevelAccess.NULL,new SimpleContainer(10));}
    public ArcaneWorkbenchMenu(int id,Inventory inventory,ContainerLevelAccess access,SimpleContainer table){
        super(TC4Modern.ARCANE_MENU.get(),id);this.table=table;this.access=access;owner=inventory.player;grid=new ArcaneCraftingGrid(table);
        addSlot(new ResultSlot(owner,grid,result,0,160,64){
            @Override public boolean mayPickup(Player player){return player.level().isClientSide || (stillValid(player) && validResult());}
            @Override public void onTake(Player player,ItemStack stack){
                if(player.level().isClientSide)return;
                var arcane=matchingArcane();
                if(arcane==null){super.onTake(player,stack);return;}
                if(!WandData.consumeCrafting(table.getItem(9),arcane.cost(),true,player))throw new IllegalStateException("Arcane vis changed between validation and take");
                checkTakeAchievements(stack);
                for(int i=0;i<9;i++){
                    var input=table.getItem(i);if(input.isEmpty())continue;
                    var remainder=input.getCraftingRemainingItem();table.removeItem(i,1);
                    if(!remainder.isEmpty()){
                        var current=table.getItem(i);
                        if(current.isEmpty())table.setItem(i,remainder);
                        else if(ItemStack.isSameItemSameComponents(current,remainder)){remainder.grow(current.getCount());table.setItem(i,remainder);}
                        else if(!player.getInventory().add(remainder))player.drop(remainder,false);
                    }
                }
                table.setChanged();
            }
        });
        addSlot(new Slot(table,9,160,24){@Override public boolean mayPlace(ItemStack stack){return WandData.isWand(stack)&&!WandParts.isStaff(stack);}@Override public int getMaxStackSize(){return 1;}});
        for(int row=0;row<3;row++)for(int col=0;col<3;col++)addSlot(new Slot(table,col+row*3,40+col*24,40+row*24));
        for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(inventory,col+row*9+9,16+col*18,151+row*18));
        for(int col=0;col<9;col++)addSlot(new Slot(inventory,col,16+col*18,209));
        listener=c->recompute();table.addListener(listener);recompute();
    }
    private ArcaneRecipes.Entry matchingArcane(){
        if(owner.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid.asCraftInput(),owner.level()).isPresent())return null;
        return ArcaneRecipes.find(grid.getItems(),ResearchProgress.completed(owner.getPersistentData()));
    }
    private boolean validResult(){
        var found=owner.level().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid.asCraftInput(),owner.level());
        if(found.isPresent())return ItemStack.isSameItemSameComponents(found.get().value().assemble(grid.asCraftInput(),owner.level().registryAccess()),result.getItem(0));
        var arcane=matchingArcane();return arcane!=null && WandData.consumeCrafting(table.getItem(9),arcane.cost(),false,owner) && ItemStack.isSameItemSameComponents(arcane.output(),result.getItem(0));
    }
    private void recompute(){
        if(owner.level().isClientSide)return;
        var level=owner.level();var input=grid.asCraftInput();var found=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,input,level);
        ItemStack output=ItemStack.EMPTY;
        if(found.isPresent() && result.setRecipeUsed(level,(net.minecraft.server.level.ServerPlayer)owner,found.get())){
            var assembled=found.get().value().assemble(input,level.registryAccess());if(assembled.isItemEnabled(level.enabledFeatures()))output=assembled;
        }
        if(found.isEmpty()){
            result.setRecipeUsed(null);
            var arcane=matchingArcane();if(arcane!=null && WandData.consumeCrafting(table.getItem(9),arcane.cost(),false,owner))output=arcane.output().copy();
        }
        result.setItem(0,output);broadcastChanges();
    }
    @Override public boolean stillValid(Player player){return stillValid(access,player,TC4Modern.ARCANE_WORKBENCH.get());}
    @Override public void removed(Player player){table.removeListener(listener);super.removed(player);}
    @Override public ItemStack quickMoveStack(Player player,int index){
        if(index<0 || index>=slots.size())return ItemStack.EMPTY;
        var slot=slots.get(index);if(!slot.hasItem() || !slot.mayPickup(player))return ItemStack.EMPTY;
        var stack=slot.getItem();var original=stack.copy();
        if(index==0){if(!moveItemStackTo(stack,11,47,true))return ItemStack.EMPTY;slot.onQuickCraft(stack,original);}
        else if(index<11){if(!moveItemStackTo(stack,11,47,false))return ItemStack.EMPTY;}
        else if(WandData.isWand(stack)&&!WandParts.isStaff(stack)){if(!moveItemStackTo(stack,1,2,false))return ItemStack.EMPTY;}
        else if(!moveItemStackTo(stack,2,11,false)){
            if(index<38){if(!moveItemStackTo(stack,38,47,false))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,11,38,false))return ItemStack.EMPTY;
        }
        if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();
        if(stack.getCount()==original.getCount())return ItemStack.EMPTY;
        slot.onTake(player,stack);if(index==0 && !stack.isEmpty())player.drop(stack,false);
        return original;
    }
}
