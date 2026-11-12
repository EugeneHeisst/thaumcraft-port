package thaumcraft.common.tiles;

/** Source addItemsToInventory ordering; stack operations and destruction effects supplied by host. */
public final class InfernalFurnaceInventory {
    public interface Host {
        boolean empty(int slot);
        boolean sameItem(int slot);
        int count(int slot);
        int incomingCount();
        int incomingMaxStackSize();
        void merge(int slot);
        void assign(int slot,int inventoryLimit);
        boolean canSmelt(int slot);
        void destroy(int slot);
        void changed();
    }
    public static boolean insert(Host host){
        for(int slot=0;slot<32;slot++){
            if(!host.empty(slot)&&host.sameItem(slot)&&host.count(slot)+host.incomingCount()<=host.incomingMaxStackSize()){
                host.merge(slot);
                if(!host.canSmelt(slot))host.destroy(slot);
                host.changed();return true;
            }
            if(!host.empty(slot))continue;
            host.assign(slot,64);
            if(!host.canSmelt(slot))host.destroy(slot);
            host.changed();return true;
        }
        return false;
    }
}
