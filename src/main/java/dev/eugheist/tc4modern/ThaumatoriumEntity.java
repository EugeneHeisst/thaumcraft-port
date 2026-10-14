package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.*;
import thaumcraft.api.aspects.*;
import thaumcraft.common.tiles.essentia.ThaumatoriumState;

public final class ThaumatoriumEntity extends BlockEntity implements Container,MenuProvider,IEssentiaTransport {
    public final ThaumatoriumState state=new ThaumatoriumState();
    private final NonNullList<ItemStack> items=NonNullList.withSize(1,ItemStack.EMPTY);
    public int venting;
    public ThaumatoriumEntity(BlockPos p,BlockState s){super(Thaumatorium.ENTITY.get(),p,s);}
    ThaumatoriumEntity base(){if(!getBlockState().getValue(Thaumatorium.TOP))return this;return level!=null&&level.getBlockEntity(worldPosition.below()) instanceof ThaumatoriumEntity t?t:null;}
    Direction facing(){return getBlockState().getValue(Thaumatorium.FACING);}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level l,BlockPos p,BlockState s,ThaumatoriumEntity t){
        if(s.getValue(Thaumatorium.TOP))return;
        if(l.isClientSide){if(t.venting>0){t.venting--;AlchemyMachineEntity.vent(l,p,t.facing(),2);}return;}
        t.state.tick(new ThaumatoriumState.Host(){
            public boolean heated(){var b=l.getBlockState(p.below(2));return b.is(TC4Modern.NITOR_BLOCK.get())||b.is(Blocks.LAVA)||b.is(net.minecraft.tags.BlockTags.FIRE);}
            public boolean power(){return l.hasNeighborSignal(p)||l.hasNeighborSignal(p.above())||l.hasNeighborSignal(p.below());}
            public int upgrades(){return t.upgradeCount();}
            public ItemStack input(){return t.items.getFirst();}
            public thaumcraft.api.crafting.CrucibleRecipe recipe(String key){return CrucibleRecipes.byKey().get(key);}
            public boolean outputFits(ItemStack output){var h=l.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,p.relative(t.facing()),t.facing().getOpposite());return h==null||net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(h,output.copy(),true).isEmpty();}
            public void pull(Aspect a){for(int y=0;y<=1;y++)for(var d:Direction.values()){
                if(d==t.facing()||d==Direction.DOWN||y==0&&d==Direction.UP)continue;
                var source=EssentiaTransport.connectable(l,p.above(y),d);var other=d.getOpposite();
                if(source==null||source.getEssentiaAmount(other)<=0||source.getSuctionAmount(other)>=128||source.getMinimumSuction()>128)continue;
                int n=source.takeEssentia(a,1,other);if(n>0){t.state.add(a,n);t.changed();return;}
            }}
            public void complete(ItemStack output,String name){
                var catalyst=t.removeItem(0,1);var owner=l.getServer().getPlayerList().getPlayerByName(name);
                if(owner!=null)net.neoforged.neoforge.event.EventHooks.firePlayerCraftingEvent(owner,output,new SimpleContainer(catalyst));
                var d=t.facing();var h=l.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,p.relative(d),d.getOpposite());
                if(h!=null)output=net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(h,output,false);
                if(!output.isEmpty()){
                    var item=new ItemEntity(l,p.getX()+.5+d.getStepX()*.66,p.getY()+.33+d.getOpposite().getStepY(),p.getZ()+.5+d.getStepZ()*.66,output);
                    item.setDeltaMovement(.075*d.getStepX(),.025,.075*d.getStepZ());l.addFreshEntity(item);l.blockEvent(p,s.getBlock(),0,0);
                }
                l.playSound(null,p,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.25f,2.6f+(l.random.nextFloat()-l.random.nextFloat())*.8f);
            }
            public void changed(){t.changed();}
        });
    }
    int upgradeCount(){int n=1;for(int y=0;y<=1;y++)for(var d:Direction.values()){if(d==Direction.DOWN||d==facing())continue;var b=level.getBlockState(worldPosition.above(y).relative(d));if(b.is(Thaumatorium.BRAIN.get())&&b.getValue(Thaumatorium.FACING)==d.getOpposite())n+=2;}return n;}
    void refreshUpgrades(){int max=upgradeCount();if(max!=state.maxRecipes){state.maxRecipes=max;while(state.selected.size()>max){state.selected.remove(state.selected.size()-1);state.players.remove(state.players.size()-1);}state.currentCraft=-1;changed();}}
    public boolean toggle(String key,Player player){
        var recipe=CrucibleRecipes.byKey().get(key);if(recipe==null)return false;
        int i=state.selected.indexOf(key);
        if(i>=0){state.selected.remove(i);state.players.remove(i);}
        else{
            if(state.selected.size()>=state.maxRecipes||!ResearchProgress.completed(player.getPersistentData()).contains(recipe.key)||!recipe.catalystMatches(getItem(0)))return false;
            state.selected.add(key);state.players.add(player.getGameProfile().getName());
        }
        state.currentCraft=-1;state.currentRecipe=null;changed();return true;
    }
    @Override public int getContainerSize(){return 1;}
    @Override public boolean isEmpty(){return getItem(0).isEmpty();}
    @Override public ItemStack getItem(int i){var b=base();return b==null?ItemStack.EMPTY:b.items.get(0);}
    @Override public ItemStack removeItem(int i,int n){var b=base();if(b==null)return ItemStack.EMPTY;var stack=ContainerHelper.removeItem(b.items,0,n);b.changed();return stack;}
    @Override public ItemStack removeItemNoUpdate(int i){var b=base();return b==null?ItemStack.EMPTY:ContainerHelper.takeItem(b.items,0);}
    @Override public void setItem(int i,ItemStack s){var b=base();if(b!=null){b.items.set(0,s);b.changed();}}
    @Override public void clearContent(){setItem(0,ItemStack.EMPTY);}
    @Override public boolean stillValid(Player p){return Container.stillValidBlockEntity(this,p);}
    @Override public Component getDisplayName(){return Component.translatable("tc.research_name.THAUMATORIUM");}
    @Override public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id,Inventory inv,Player p){return new ThaumatoriumMenu(id,inv,this);}
    public boolean isConnectable(Direction d){var b=base();return b!=null&&d!=b.facing();}
    public boolean canInputFrom(Direction d){return isConnectable(d);}
    public boolean canOutputTo(Direction d){return false;}
    public void setSuction(Aspect a,int n){var b=base();if(b!=null)b.state.suction=a;}
    public Aspect getSuctionType(Direction d){var b=base();return b==null?null:b.state.suction;}
    public int getSuctionAmount(Direction d){return getSuctionType(d)!=null?128:0;}
    public Aspect getEssentiaType(Direction d){return null;}
    public int getEssentiaAmount(Direction d){return 0;}
    public int takeEssentia(Aspect a,int n,Direction d){return 0;}
    public int addEssentia(Aspect a,int n,Direction d){var b=base();if(b==null||!canInputFrom(d))return 0;int accepted=n-b.state.add(a,n);if(accepted>0)b.changed();return accepted;}
    public int getMinimumSuction(){return 0;}
    public boolean renderExtendedTube(){return false;}
    @Override public boolean triggerEvent(int i,int j){if(i>=0){if(level!=null&&level.isClientSide)venting=7;return true;}return super.triggerEvent(i,j);}
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider r){super.saveAdditional(n,r);ContainerHelper.saveAllItems(n,items,r);state.essentia.writeToNBT(n);n.putInt("maxrec",state.maxRecipes);var list=new ListTag();for(int i=0;i<state.selected.size();i++){var entry=new CompoundTag();entry.putString("recipe",state.selected.get(i));entry.putString("player",state.players.get(i));list.add(entry);}n.put("recipes",list);}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider r){super.loadAdditional(n,r);items.clear();ContainerHelper.loadAllItems(n,items,r);state.essentia=new AspectList();state.essentia.readFromNBT(n);state.maxRecipes=Math.max(1,n.getInt("maxrec"));state.selected.clear();state.players.clear();var list=n.getList("recipes",10);for(int i=0;i<list.size();i++){var entry=list.getCompound(i);if(CrucibleRecipes.byKey().containsKey(entry.getString("recipe"))){state.selected.add(entry.getString("recipe"));state.players.add(entry.getString("player"));}}state.currentCraft=-1;state.currentRecipe=null;}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
