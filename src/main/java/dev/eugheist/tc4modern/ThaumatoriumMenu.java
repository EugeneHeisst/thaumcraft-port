package dev.eugheist.tc4modern;
import java.util.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class ThaumatoriumMenu extends AbstractContainerMenu {
    public ThaumatoriumEntity tile;
    public final Player player;
    public final Set<String> known;
    public ThaumatoriumMenu(int id,Inventory inv,ThaumatoriumEntity t){this(id,inv,t,ResearchProgress.completed(inv.player.getPersistentData()));}
    public ThaumatoriumMenu(int id,Inventory inv,ThaumatoriumEntity t,Set<String> completed){
        super(Thaumatorium.MENU.get(),id);tile=t;player=inv.player;known=Set.copyOf(completed);
        addSlot(new Slot(inv.player.level().isClientSide?new net.minecraft.world.SimpleContainer(1):t,0,48,16));
        for(int i=0;i<3;i++)for(int j=0;j<9;j++)addSlot(new Slot(inv,j+i*9+9,8+j*18,84+i*18));
        for(int i=0;i<9;i++)addSlot(new Slot(inv,i,8+i*18,142));
    }
    static ThaumatoriumMenu fromNetwork(int id,Inventory inv,net.minecraft.network.RegistryFriendlyByteBuf buf){
        var pos=buf.readBlockPos();var completed=new HashSet<>(buf.readList(net.minecraft.network.FriendlyByteBuf::readUtf));var snapshot=buf.readNbt();
        var t=new ThaumatoriumEntity(pos,Thaumatorium.BODY.get().defaultBlockState());t.setLevel(inv.player.level());
        if(snapshot!=null)t.loadWithComponents(snapshot,inv.player.registryAccess());
        return new ThaumatoriumMenu(id,inv,t,completed);
    }
    public List<String> recipes(){
        if(player.level().isClientSide&&player.level().getBlockEntity(tile.getBlockPos()) instanceof ThaumatoriumEntity loaded)tile=loaded;
        var completed=player.level().isClientSide?known:ResearchProgress.completed(player.getPersistentData());
        return CrucibleRecipes.byKey().entrySet().stream().filter(e->tile.state.selected.contains(e.getKey())||completed.contains(e.getValue().key)&&e.getValue().catalystMatches(getSlot(0).getItem())).map(Map.Entry::getKey).sorted().toList();
    }
    @Override public boolean clickMenuButton(Player p,int button){var all=recipes();return button>=0&&button<all.size()&&tile.toggle(all.get(button),p);}
    @Override public boolean stillValid(Player p){return tile.stillValid(p);}
    @Override public ItemStack quickMoveStack(Player p,int i){
        var slot=slots.get(i);if(!slot.hasItem())return ItemStack.EMPTY;var s=slot.getItem();var old=s.copy();
        if(i==0){if(!moveItemStackTo(s,1,37,true))return ItemStack.EMPTY;}else if(!moveItemStackTo(s,0,1,false))return ItemStack.EMPTY;
        if(s.isEmpty())slot.setByPlayer(ItemStack.EMPTY);else slot.setChanged();slot.onTake(p,s);return old;
    }
}
