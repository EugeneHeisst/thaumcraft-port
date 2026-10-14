package dev.eugheist.tc4modern;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
/** ContainerSpa, original slot positions and salt-only shift transfer. */
public final class SpaMenu extends AbstractContainerMenu {
 final Container spa;final ContainerData data;
 public SpaMenu(int id,Inventory inv){this(id,inv,new SimpleContainer(1),new SimpleContainerData(3));}
 SpaMenu(int id,Inventory inv,Container spa,ContainerData data){super(Hygiene.SPA_MENU.get(),id);this.spa=spa;this.data=data;addSlot(new Slot(spa,0,65,31){@Override public boolean mayPlace(ItemStack s){return s.is(Hygiene.SALTS.get());}});for(int i=0;i<3;i++)for(int j=0;j<9;j++)addSlot(new Slot(inv,j+i*9+9,8+j*18,84+i*18));for(int i=0;i<9;i++)addSlot(new Slot(inv,i,8+i*18,142));addDataSlots(data);}
 public boolean mix(){return data.get(0)!=0;}public int amount(){return data.get(1);}public net.minecraft.world.level.material.Fluid fluid(){return net.minecraft.core.registries.BuiltInRegistries.FLUID.byId(data.get(2));}
 @Override public boolean clickMenuButton(Player player,int button){if(button==1&&spa instanceof SpaEntity entity)entity.toggleMix();return false;}
 @Override public boolean stillValid(Player p){return spa.stillValid(p);}
 @Override public ItemStack quickMoveStack(Player p,int index){var slot=slots.get(index);if(!slot.hasItem())return ItemStack.EMPTY;var stack=slot.getItem();var copy=stack.copy();if(!stack.is(Hygiene.SALTS.get())||!moveItemStackTo(stack,index==0?1:0,index==0?slots.size():1,index==0))return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();return copy;}
}
