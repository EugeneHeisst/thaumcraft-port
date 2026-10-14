package dev.eugheist.tc4modern;

import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Six visible ghost slots and the original fire-upgrade page arrows (66/67). */
public final class GolemMenu extends AbstractContainerMenu {
    public final StrawGolem golem;
    private final net.minecraft.world.Container filters;
    private final net.minecraft.world.SimpleContainer visible=new net.minecraft.world.SimpleContainer(6);
    private final boolean client;
    int page,filterCount;
    /** Colours of the six visible filter slots, synchronised for the order-upgrade colour strips. */
    final int[] visibleColors={-1,-1,-1,-1,-1,-1};
    /** ItemGolemCore.hasInventory: cores whose filter slots carry marker colours. */
    static boolean hasInventory(int core){return core==0||core==1||core==2||core==5||core==8;}
    boolean colorsEnabled(){return golem!=null&&hasInventory(golem.core())&&golem.upgrades(4)>0;}
    int pages(){return (filterCount+5)/6;}
    GolemMenu(int id,Inventory inv,StrawGolem g){
        super(Golemancy.MENU.get(),id);golem=g;client=inv.player.level().isClientSide;filters=g==null?new net.minecraft.world.SimpleContainer(18):g.filters;filterCount=g==null?6:g.filterCount();if(!client&&g!=null){g.paused=true;g.getNavigation().stop();}
        addDataSlot(new DataSlot(){public int get(){return golem!=null&&golem.fillAny?1:0;}public void set(int value){if(golem!=null)golem.fillAny=value!=0;}});
        addDataSlot(new DataSlot(){public int get(){return filterCount;}public void set(int value){filterCount=Math.clamp(value,1,18);}});
        addDataSlot(new DataSlot(){public int get(){return golem==null?0:golem.getEntityData().get(StrawGolem.ATTACK_FLAGS);}public void set(int value){if(golem!=null)golem.getEntityData().set(StrawGolem.ATTACK_FLAGS,value);}});
        addDataSlot(new DataSlot(){public int get(){return page;}public void set(int value){page=Math.clamp(value,0,pages()-1);}});
        for(int a=0;a<6;a++){final int visible=a;addDataSlot(new DataSlot(){public int get(){int i=page*6+visible;return golem==null||i>=golem.colors.length?-1:golem.colors[i];}public void set(int value){visibleColors[visible]=value;}});}
        for(int a=0;a<6;a++){final int visibleIndex=a;addSlot(new Slot(visible,a,100+a/2*28,16+a%2*31){@Override public boolean isActive(){return visibleIndex+page*6<filterCount&&(golem==null||golem.core()!=4);}@Override public boolean mayPlace(ItemStack s){return false;}@Override public boolean mayPickup(Player p){return false;}@Override public int getMaxStackSize(){return golem!=null&&golem.core()==0?64:1;}});}
        for(int i=0;i<3;i++)for(int j=0;j<9;j++)addSlot(new Slot(inv,j+i*9+9,8+j*18,84+i*18));for(int j=0;j<9;j++)addSlot(new Slot(inv,j,8+j*18,142));
        refreshVisible();
    }
    private void refreshVisible(){if(client)return;if(golem!=null){filterCount=golem.filterCount();page=Math.min(page,pages()-1);}for(int i=0;i<6;i++)visible.setItem(i,filters.getItem(page*6+i).copy());}
    @Override public void broadcastChanges(){refreshVisible();super.broadcastChanges();}
    /** ContainerGolem buttons 0..slots-1 / slots..2*slots-1 step a slot colour down / up through -1..15, here 100+ / 110+ for the visible slot. */
    boolean stepColor(int visible,int step){int i=page*6+visible;if(!colorsEnabled()||visible<0||visible>=6||i>=filterCount)return false;int c=golem.colors[i]+step;if(c<-1)c=15;if(c>15)c=-1;golem.colors[i]=(byte)c;golem.level().playSound(null,golem.getX(),golem.getY(),golem.getZ(),net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),net.minecraft.sounds.SoundSource.NEUTRAL,.2f,.8f);broadcastChanges();return true;}
    @Override public boolean clickMenuButton(Player p,int id){if(id>=100&&id<106)return stepColor(id-100,-1);if(id>=110&&id<116)return stepColor(id-110,1);if(id>=50&&id<53&&golem!=null&&golem.core()==8){golem.getEntityData().set(StrawGolem.USE_FLAGS,golem.useFlags()^(1<<(id-50)));return true;}if(id>=80&&id<84&&golem!=null&&golem.core()==4&&(id==80||golem.upgrades(4)>0)){golem.getEntityData().set(StrawGolem.ATTACK_FLAGS,golem.getEntityData().get(StrawGolem.ATTACK_FLAGS)^(1<<(id-80)));return true;}if(id==0&&golem!=null&&golem.core()==0){golem.fillAny=!golem.fillAny;return true;}if(id==66&&page>0){page--;broadcastChanges();return true;}if(id==67&&page+1<pages()){page++;broadcastChanges();return true;}return false;}
    @Override public void clicked(int slot,int button,ClickType type,Player p){
        if(slot>=0&&slot<6){if(slot+page*6>=filterCount||golem!=null&&golem.core()==4)return;if(type==ClickType.PICKUP){var sample=getCarried().isEmpty()?ItemStack.EMPTY:getCarried().copyWithCount(golem!=null&&golem.core()==0?getCarried().getCount():1);filters.setItem(page*6+slot,sample);visible.setItem(slot,sample.copy());broadcastChanges();}return;}
        super.clicked(slot,button,type,p);
    }
    @Override public ItemStack quickMoveStack(Player p,int slot){if(golem!=null&&golem.core()==4)return ItemStack.EMPTY;if(slot<6||slot>=slots.size())return ItemStack.EMPTY;var s=slots.get(slot).getItem();if(s.isEmpty())return ItemStack.EMPTY;for(int i=0;i<filterCount;i++)if(filters.getItem(i).isEmpty()){filters.setItem(i,s.copyWithCount(golem!=null&&golem.core()==0?s.getCount():1));broadcastChanges();break;}return ItemStack.EMPTY;}
    @Override public boolean stillValid(Player p){return golem!=null&&golem.isAlive()&&golem.core()>=0&&p.distanceToSqr(golem)<64;}
    @Override public void removed(Player p){super.removed(p);if(!client&&golem!=null)golem.paused=false;}
}
