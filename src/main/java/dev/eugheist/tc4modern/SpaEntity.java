package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.*;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** TileSpa: one salt slot,5000mB tank,40tick filling scan, redstone disables operation. */
public final class SpaEntity extends BlockEntity implements WorldlyContainer,MenuProvider {
 private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
 boolean mix=true;int counter;
 public final FluidTank tank=new FluidTank(5000){@Override protected void onContentsChanged(){changed();}};
 public final ContainerData data=new ContainerData(){public int get(int i){return switch(i){case 0->mix?1:0;case 1->tank.getFluidAmount();default->net.minecraft.core.registries.BuiltInRegistries.FLUID.getId(tank.getFluid().getFluid());};}public void set(int i,int v){}public int getCount(){return 3;}};
 public SpaEntity(BlockPos pos,BlockState s){super(Hygiene.SPA_ENTITY.get(),pos,s);}
 public void toggleMix(){mix=!mix;changed();}public boolean getMix(){return mix;}
 void changed(){setChanged();if(level!=null&&!level.isClientSide)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
 boolean hasIngredients(){return mix?tank.getFluid().is(Fluids.WATER)&&tank.getFluidAmount()>=1000&&getItem(0).is(Hygiene.SALTS.get()):tank.getFluidAmount()>=1000&&!tank.getFluid().getFluid().defaultFluidState().createLegacyBlock().isAir();}
 boolean validLocation(BlockPos pos,boolean adjacent,BlockState target){if(target.is(Blocks.WATER)&&level.dimensionType().ultraWarm())return false;var s=level.getBlockState(pos);if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP)||!s.canBeReplaced()||(s.is(target.getBlock())&&s.getFluidState().isSource()))return false;if(!adjacent)return true;for(var d:Direction.values()){var near=level.getBlockState(pos.relative(d));if(near.is(target.getBlock())&&near.getFluidState().isSource())return true;}return false;}
 static void serverTick(Level level,BlockPos pos,BlockState state,SpaEntity tile){if(tile.counter++%40!=0||level.hasNeighborSignal(pos)||!tile.hasIngredients())return;var target=tile.mix?PureWater.BLOCK.get().defaultBlockState():tile.tank.getFluid().getFluid().defaultFluidState().createLegacyBlock();var above=pos.above();var current=level.getBlockState(above);if(current.is(target.getBlock())&&current.getFluidState().isSource()){for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++){var to=above.offset(x,0,z);if(tile.validLocation(to,true,target)){tile.place(to,target);return;}}}else if(tile.validLocation(above,false,target))tile.place(above,target);}
 private void place(BlockPos p,BlockState target){if(mix)removeItem(0,1);tank.drain(1000,FluidAction.EXECUTE);level.setBlock(p,target,3);changed();}
 @Override protected void saveAdditional(CompoundTag t,HolderLookup.Provider r){super.saveAdditional(t,r);t.putBoolean("mix",mix);t.put("tank",tank.writeToNBT(r,new CompoundTag()));ContainerHelper.saveAllItems(t,items,r);}
 @Override protected void loadAdditional(CompoundTag t,HolderLookup.Provider r){super.loadAdditional(t,r);mix=t.getBoolean("mix");tank.readFromNBT(r,t.getCompound("tank"));items.clear();ContainerHelper.loadAllItems(t,items,r);}
 @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}@Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
 @Override public Component getDisplayName(){return Component.translatable("thaumcraft.spa");}@Override public AbstractContainerMenu createMenu(int id,Inventory inv,Player player){return new SpaMenu(id,inv,this,data);}
 @Override public int getContainerSize(){return 1;}@Override public boolean isEmpty(){return items.getFirst().isEmpty();}@Override public ItemStack getItem(int i){return items.get(i);}
 @Override public ItemStack removeItem(int i,int n){var out=ContainerHelper.removeItem(items,i,n);if(!out.isEmpty())changed();return out;}@Override public ItemStack removeItemNoUpdate(int i){return ContainerHelper.takeItem(items,i);}
 @Override public void setItem(int i,ItemStack s){items.set(i,s);s.limitSize(64);changed();}@Override public void clearContent(){items.clear();changed();}
 @Override public boolean stillValid(Player p){return level.getBlockEntity(worldPosition)==this&&p.distanceToSqr(worldPosition.getCenter())<=64;}
 @Override public boolean canPlaceItem(int slot,ItemStack s){return s.is(Hygiene.SALTS.get());}
 @Override public int[] getSlotsForFace(Direction d){return d==Direction.UP?new int[0]:new int[]{0};}
 @Override public boolean canPlaceItemThroughFace(int slot,ItemStack s,Direction d){return d!=Direction.UP&&canPlaceItem(slot,s);}@Override public boolean canTakeItemThroughFace(int slot,ItemStack s,Direction d){return d!=Direction.UP;}
}
