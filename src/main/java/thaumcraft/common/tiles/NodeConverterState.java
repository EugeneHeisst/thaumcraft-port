package thaumcraft.common.tiles;

/** Original server tick/status order from TileNodeConverter; native host owns block replacement and FX. */
public final class NodeConverterState {
    public int count=-1,status;
    public interface Host {
        boolean powered();boolean hasStabilizer();boolean ordinaryNode();boolean energizedNode();
        void energize();void revertAndEmpty();void drainRandomAspect(int count);
        void explodeNode();void changed();void conversionEvent();
    }
    public void checkStatus(Host host){
        if(count==-1)count=0;
        if(status==2 && count>50 && !(host.hasStabilizer() && host.energizedNode())){
            host.explodeNode();status=0;count=50;host.changed();
        }else if(host.powered() && host.ordinaryNode() && host.hasStabilizer()){
            status=1;host.changed();
        }else if(host.energizedNode()){
            status=2;count=1000;host.changed();
        }else status=0;
    }
    public void tick(Host host){
        if(count==-1)checkStatus(host);
        if(status==1 && count>=1000 && host.ordinaryNode()){
            host.energize();checkStatus(host);host.conversionEvent();host.changed();
        }
        if(status==2 && count<=50 && host.energizedNode()){
            host.revertAndEmpty();host.conversionEvent();host.changed();status=0;
        }
        if(status==0 || !host.powered()){
            if(count>0)--count;
        }else if(count<1000){
            ++count;
            if(host.ordinaryNode())host.drainRandomAspect(count);
        }
        if(count>1000)count=1000;
    }
    public void read(net.minecraft.nbt.CompoundTag tag){status=tag.getInt("status");count=tag.getInt("count");}
    public void write(net.minecraft.nbt.CompoundTag tag){tag.putInt("status",status);tag.putInt("count",count);}
}
