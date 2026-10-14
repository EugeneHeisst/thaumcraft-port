package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.world.item.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import thaumcraft.api.IRunicArmor;
import thaumcraft.api.aspects.*;
/** Original InfusionRunicAugmentRecipe: cost grows with FINAL charge, including innate shielding. */
final class RunicAugmentation {
 static List<ItemStack> components(ItemStack central){var out=new ArrayList<ItemStack>();out.add(new ItemStack(Items.DIAMOND));for(int i=0;i<=Math.max(0,RunicAccessories.charge(central));i++)out.add(new ItemStack(InfusionContent.SALIS.get()));return out;}
 static AspectList cost(ItemStack central){int vis=(int)(32*Math.pow(2,RunicAccessories.charge(central)));var out=new AspectList();if(vis>0)out.add(Aspect.ARMOR,vis/2).add(Aspect.MAGIC,vis/2).add(Aspect.ENERGY,vis);return out;}
 static int instability(ItemStack central){return 5+RunicAccessories.charge(central)/2;}
 static ItemStack output(ItemStack central){var out=central.copy();var tag=NoteData.tag(out);tag.putByte("RS.HARDEN",(byte)(tag.getByte("RS.HARDEN")+1));out.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return out;}
 static boolean matches(List<ItemStack> input,ItemStack central,Set<String> research){if(!research.contains("RUNICAUGMENTATION")||!(central.getItem() instanceof IRunicArmor))return false;var remaining=new ArrayList<>(input);for(var part:components(central)){int index=-1;for(int i=0;i<remaining.size();i++)if(remaining.get(i).is(part.getItem())){index=i;break;}if(index<0)return false;remaining.remove(index);}return remaining.isEmpty();}
}
