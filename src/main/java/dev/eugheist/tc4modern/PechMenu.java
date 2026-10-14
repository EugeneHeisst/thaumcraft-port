package dev.eugheist.tc4modern;
import net.minecraft.world.*;import net.minecraft.world.entity.player.*;import net.minecraft.world.inventory.*;import net.minecraft.world.item.*;
/** Original one payment/four output slots and server-authoritative dice button. */
public final class PechMenu extends AbstractContainerMenu {
 public final PechEntity pech;public final SimpleContainer inventory=new SimpleContainer(5);
 public PechMenu(int id,Inventory player,PechEntity pech){super(PechContent.MENU.get(),id);this.pech=pech;if(pech!=null)pech.trading=true;addSlot(new Slot(inventory,0,36,29));for(int i=0;i<4;i++)addSlot(new Slot(inventory,i+1,106+i%2*18,20+i/2*18){@Override public boolean mayPlace(ItemStack stack){return false;}});for(int row=0;row<3;row++)for(int col=0;col<9;col++)addSlot(new Slot(player,9+row*9+col,8+col*18,84+row*18));for(int col=0;col<9;col++)addSlot(new Slot(player,col,8+col*18,142));}
 public boolean canTrade(){if(PechTrades.value(inventory.getItem(0))<=0)return false;for(int i=1;i<5;i++)if(!inventory.getItem(i).isEmpty())return false;return true;}
 @Override public boolean clickMenuButton(Player player,int button){if(button!=0||player.level().isClientSide||!stillValid(player)||!canTrade())return false;PechTrades.trade(pech,inventory,player);broadcastChanges();return true;}
 @Override public boolean stillValid(Player player){return pech!=null&&pech.isAlive()&&pech.tamed()&&player.distanceToSqr(pech)<64;}
 /** Original transferStackInSlot: outputs and payment go back to the player, anything else tries the payment slot. */
 @Override public ItemStack quickMoveStack(Player player,int index){if(index<0||index>=slots.size())return ItemStack.EMPTY;var slot=slots.get(index);var stack=slot.getItem();if(stack.isEmpty())return ItemStack.EMPTY;var original=stack.copy();if(index<5){if(!moveItemStackTo(stack,5,41,true))return ItemStack.EMPTY;}else if(!moveItemStackTo(stack,0,1,true))return ItemStack.EMPTY;if(stack.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();if(stack.getCount()==original.getCount())return ItemStack.EMPTY;slot.onTake(player,stack);return original;}
 @Override public void removed(Player player){super.removed(player);if(pech!=null)pech.trading=false;if(!player.level().isClientSide)for(int i=0;i<5;i++){var stack=inventory.removeItemNoUpdate(i);if(!stack.isEmpty()){var dropped=player.drop(stack,false);if(dropped!=null&&pech!=null)dropped.getPersistentData().putBoolean("PechDrop",true);}}}
}
