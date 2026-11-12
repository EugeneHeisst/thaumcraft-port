package thaumcraft.common.items.armor;
import java.util.ArrayList;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
/** Original one-robe/multiple-dye recipe, adapted to CraftingInput and original sheep colors. */
public final class RecipesRobeArmorDyes extends CustomRecipe {
    public RecipesRobeArmorDyes(CraftingBookCategory category){super(category);}
    @Override public boolean matches(CraftingInput input,Level level){return !assemble(input,null).isEmpty();}
    @Override public ItemStack assemble(CraftingInput input,HolderLookup.Provider registries){
        ItemStack robe=ItemStack.EMPTY;var dyes=new ArrayList<Integer>();
        for(int i=0;i<input.size();i++){
            var stack=input.getItem(i);if(stack.isEmpty())continue;
            if(stack.getItem() instanceof ItemRobeArmor||dev.eugheist.tc4modern.VoidRobe.piece(stack)){/* RecipesVoidRobeArmorDyes shares the rule */if(!robe.isEmpty())return ItemStack.EMPTY;robe=stack.copyWithCount(1);}
            else if(stack.getItem() instanceof DyeItem dye)dyes.add(dye.getDyeColor().getId());
            else return ItemStack.EMPTY;
        }
        if(robe.isEmpty()||dyes.isEmpty())return ItemStack.EMPTY;
        ItemRobeArmor.setColor(robe,RobeDyeColors.blend(ItemRobeArmor.color(robe),dyes.stream().mapToInt(Integer::intValue).toArray()));
        return robe;
    }
    @Override public boolean canCraftInDimensions(int width,int height){return width*height>=2;}
    @Override public RecipeSerializer<?> getSerializer(){return dev.eugheist.tc4modern.TC4Modern.ROBE_DYE_SERIALIZER.get();}
}
