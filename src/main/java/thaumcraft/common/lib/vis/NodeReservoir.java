package thaumcraft.common.lib.vis;
import java.util.ArrayList;
import thaumcraft.api.nodes.NodeModifier;
import java.util.Random;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
/** Original TileNode reservoir/filtered selection methods; world RNG injected. */
public final class NodeReservoir {
    public AspectList aspects=new AspectList(),aspectsBase=new AspectList();
    private final Random random;
    public int count,regeneration=-1,wait,lock;
    public long lastActive;
    public boolean catchUp,destroyed;
    public thaumcraft.api.nodes.NodeModifier modifier;
    public AspectList getAspects(){return aspects;}
    public int getNodeVisBase(Aspect aspect){return aspectsBase.getAmount(aspect);}
    public void setNodeVisBase(Aspect aspect,short amount){
        if(aspectsBase.getAmount(aspect)<amount)aspectsBase.merge(aspect,amount);
        else aspectsBase.reduce(aspect,aspectsBase.getAmount(aspect)-amount);
    }
    public thaumcraft.api.nodes.NodeModifier getNodeModifier(){return modifier;}
    public void setNodeModifier(thaumcraft.api.nodes.NodeModifier value){modifier=value;}
    public int getLock(){return lock;}
    public void nodeChange(){regeneration=-1;}

    public NodeReservoir(Random random){this.random=random;}
    public void setAspects(AspectList aspects) {
        this.aspects = aspects;
        this.aspectsBase = aspects.copy();
    }
    public int addToContainer(Aspect aspect, int amount) {
        int left = amount + this.aspects.getAmount(aspect) - this.aspectsBase.getAmount(aspect);
        left = left > 0 ? left : 0;
        this.aspects.add(aspect, amount - left);
        return left;
    }
    public boolean takeFromContainer(Aspect aspect, int amount) {
        return this.aspects.reduce(aspect, amount);
    }
    public Aspect chooseRandomFilteredFromSource(AspectList filter, boolean preserve) {
        int min = preserve ? 1 : 0;
        ArrayList<Aspect> validaspects = new ArrayList<Aspect>();
        for (Aspect prim : this.aspects.getAspects()) {
            if (filter.getAmount(prim) <= 0 || this.aspects.getAmount(prim) <= min) continue;
            validaspects.add(prim);
        }
        if (validaspects.size() == 0) {
            return null;
        }
        Aspect asp = (Aspect)validaspects.get(random.nextInt(validaspects.size()));
        if (asp != null && this.aspects.getAmount(asp) > min) {
            return asp;
        }
        return null;
    }
    /** Original TileNode charging body with platform-independent wand storage. */
    public Aspect charge(net.minecraft.nbt.CompoundTag wand,int capacity,int tap,boolean preserve) {
        AspectList room=new AspectList();
        for(Aspect aspect:Aspect.getPrimalAspects()) if(VisStorage.getVis(wand,aspect)<capacity)room.add(aspect,1);
        Aspect aspect=chooseRandomFilteredFromSource(room,preserve);
        if(aspect!=null){
            int amt=aspects.getAmount(aspect);
            if(tap>amt)tap=amt;
            if(preserve && tap==amt)--tap;
            if(tap>0){
                int rem=VisStorage.addVis(wand,aspect,tap,true,capacity);
                if(rem<tap){takeFromContainer(aspect,tap-rem);return aspect;}
            }
        }
        return null;
    }
    public boolean recharge(long now) {
        boolean change=false;
        if (this.regeneration < 0) {
            this.regeneration = 600;
            if (this.getNodeModifier() != null) {
                switch (this.getNodeModifier()) {
                    case BRIGHT: {
                        this.regeneration = 400;
                        break;
                    }
                    case PALE: {
                        this.regeneration = 900;
                        break;
                    }
                    case FADING: {
                        this.regeneration = 0;
                    }
                }
            }
            if (this.getLock() == 1) {
                this.regeneration *= 2;
            }
            if (this.getLock() == 2) {
                this.regeneration *= 20;
            }
        }
        if (this.catchUp) {
            int amt;
            this.catchUp = false;
            long ct = now;
            int inc = this.regeneration * 75;
            int n = amt = inc > 0 ? (int)((ct - this.lastActive) / (long)inc) : 0;
            if (amt > 0) {
                for (int a = 0; a < Math.min(amt, this.aspectsBase.visSize()); ++a) {
                    AspectList al = new AspectList();
                    for (Aspect aspect : this.getAspects().getAspects()) {
                        if (this.getAspects().getAmount(aspect) >= this.getNodeVisBase(aspect)) continue;
                        al.add(aspect, 1);
                    }
                    if (al.size() <= 0) continue;
                    this.addToContainer(al.getAspects()[random.nextInt(al.size())], 1);
                }
            }
        }
        if (this.count % 1200 == 0) {
            for (Aspect aspect : this.getAspects().getAspects()) {
                if (this.getAspects().getAmount(aspect) > 0) continue;
                this.setNodeVisBase(aspect, (short)(this.getNodeVisBase(aspect) - 1));
                if (random.nextInt(20) == 0 || this.getNodeVisBase(aspect) <= 0) {
                    this.getAspects().remove(aspect);
                    if (random.nextInt(5) == 0) {
                        if (this.getNodeModifier() == NodeModifier.BRIGHT) {
                            this.setNodeModifier(null);
                        } else if (this.getNodeModifier() == null) {
                            this.setNodeModifier(NodeModifier.PALE);
                        }
                        if (this.getNodeModifier() == NodeModifier.PALE && random.nextInt(5) == 0) {
                            this.setNodeModifier(NodeModifier.FADING);
                        }
                    }
                    this.nodeChange();
                    change=true;
                    break;
                }
                this.nodeChange();
                change=true;
            }
            if (this.getAspects().size() <= 0) {
                this.destroyed=true;
                change=true;
            }
        }
        if (this.wait > 0) {
            --this.wait;
        }
        if (this.regeneration > 0 && this.wait == 0 && this.count % this.regeneration == 0) {
            this.lastActive = now;
            AspectList al = new AspectList();
            for (Aspect aspect : this.getAspects().getAspects()) {
                if (this.getAspects().getAmount(aspect) >= this.getNodeVisBase(aspect)) continue;
                al.add(aspect, 1);
            }
            if (al.size() > 0) {
                this.addToContainer(al.getAspects()[random.nextInt(al.size())], 1);
                change = true;
            }
        }
        return change;
    }

    /** Original ScanManager.generateNodeAspects rule for a NORMAL node. */
    public AspectList scanAspects(){return scanAspects(thaumcraft.api.nodes.NodeType.NORMAL);}
    public AspectList scanAspects(thaumcraft.api.nodes.NodeType type){
        AspectList tags=new AspectList();
        for(Aspect aspect:aspects.getAspectsSorted())tags.merge(aspect,Math.max(4,aspects.getAmount(aspect)/10));
        switch(type){
            case UNSTABLE:tags.merge(Aspect.ENTROPY,4);break;
            case HUNGRY:tags.merge(Aspect.HUNGER,4);break;
            case TAINTED:tags.merge(Aspect.TAINT,4);break;
            case PURE:tags.merge(Aspect.HEAL,2);tags.add(Aspect.ORDER,2);break;
            case DARK:tags.merge(Aspect.DEATH,2);tags.add(Aspect.DARKNESS,2);break;
            default:break;
        }
        return tags;
    }
}
