package dev.eugheist.tc4modern;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import thaumcraft.common.tiles.InfernalFurnaceInventory;

/** Modern stack/recipe adapter for the original furnace's 32-slot queue. */
public final class InfernalFurnaceContents {
    public final NonNullList<ItemStack> items=NonNullList.withSize(32,ItemStack.EMPTY);
    public void save(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries){
        net.minecraft.world.ContainerHelper.saveAllItems(tag,items,registries);
    }
    public void load(net.minecraft.nbt.CompoundTag tag,net.minecraft.core.HolderLookup.Provider registries){
        items.clear();
        net.minecraft.world.ContainerHelper.loadAllItems(tag,items,registries);
    }
    public java.util.Optional<RecipeHolder<SmeltingRecipe>> recipe(Level level,int slot){
        if(items.get(slot).isEmpty())return java.util.Optional.empty();
        return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING,new SingleRecipeInput(items.get(slot)),level);
    }
    public boolean hasSmeltableInput(Level level){
        for(int slot=0;slot<items.size();slot++)if(recipe(level,slot).isPresent())return true;
        return false;
    }
    public boolean insert(Level level,ItemStack incoming,Runnable destroyEffects,Runnable changed){
        if(incoming.isEmpty())return false;
        return InfernalFurnaceInventory.insert(new InfernalFurnaceInventory.Host(){
            public boolean empty(int slot){return items.get(slot).isEmpty();}
            // Legacy isItemEqual compared item/metadata, ignoring stack NBT.
            public boolean sameItem(int slot){return ItemStack.isSameItem(items.get(slot),incoming);}
            public int count(int slot){return items.get(slot).getCount();}
            public int incomingCount(){return incoming.getCount();}
            public int incomingMaxStackSize(){return incoming.getMaxStackSize();}
            public void merge(int slot){items.get(slot).grow(incoming.getCount());}
            public void assign(int slot,int limit){items.set(slot,incoming.copyWithCount(Math.min(limit,incoming.getCount())));}
            public boolean canSmelt(int slot){return recipe(level,slot).isPresent();}
            public void destroy(int slot){items.set(slot,ItemStack.EMPTY);destroyEffects.run();}
            public void changed(){changed.run();}
        });
    }
    public interface Ejection {void eject(ItemStack result,ItemStack original,float experience);}
    public void smeltFirst(Level level,Ejection ejection){
        for(int slot=0;slot<items.size();slot++){
            var found=recipe(level,slot);if(found.isEmpty())continue;
            var input=items.get(slot);var recipe=found.get().value();
            var result=recipe.assemble(new SingleRecipeInput(input),level.registryAccess());
            ejection.eject(result,input,recipe.getExperience());
            input.shrink(1);if(input.isEmpty())items.set(slot,ItemStack.EMPTY);
            return;
        }
    }
}
