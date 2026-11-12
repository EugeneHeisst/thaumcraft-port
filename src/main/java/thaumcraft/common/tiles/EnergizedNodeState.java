package thaumcraft.common.tiles;

import java.util.Random;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.nodes.NodeModifier;
import thaumcraft.api.nodes.NodeType;
import thaumcraft.common.lib.research.ResearchManager;

/** Server tick, setup and consumption extracted from original TileNodeEnergized. */
public final class EnergizedNodeState {
    public AspectList visBase = new AspectList();
    public AspectList vis = new AspectList();
    public void tick(AspectList auraBase,NodeType type,NodeModifier modifier,Random random,Runnable changed) {
        if(type==NodeType.UNSTABLE && random.nextInt(500)==1)visBase=new AspectList();
        if(visBase.size()==0 && auraBase.size()>0){
            setup(auraBase,type,modifier,random);
            changed.run();
        }
        vis=visBase.copy();
    }
    public void setup(AspectList auraBase,NodeType type,NodeModifier modifier,Random random){
        visBase=new AspectList();
        AspectList temp=ResearchManager.reduceToPrimals(auraBase,true);
        for(Aspect aspect:temp.getAspects()){
            int amt=temp.getAmount(aspect);
            if(modifier==NodeModifier.BRIGHT)amt=(int)((float)amt*1.2f);
            if(modifier==NodeModifier.PALE)amt=(int)((float)amt*.8f);
            if(modifier==NodeModifier.FADING)amt=(int)((float)amt*.5f);
            amt=(int)Math.floor(Math.sqrt(amt));
            if(type==NodeType.UNSTABLE)amt+=random.nextInt(5)-2;
            if(amt<1)continue;
            visBase.merge(aspect,amt);
        }
    }
    public void readVisBase(net.minecraft.nbt.CompoundTag tag){
        visBase.aspects.clear();
        var entries=tag.getList("AEB",10);
        for(int j=0;j<entries.size();j++){
            var entry=entries.getCompound(j);
            if(!entry.contains("key"))continue;
            visBase.add(Aspect.getAspect(entry.getString("key")),entry.getInt("amount"));
        }
    }
    public void writeVisBase(net.minecraft.nbt.CompoundTag tag){
        var entries=new net.minecraft.nbt.ListTag();
        tag.put("AEB",entries);
        for(Aspect aspect:visBase.getAspects()){
            if(aspect==null)continue;
            var entry=new net.minecraft.nbt.CompoundTag();
            entry.putString("key",aspect.getTag());entry.putInt("amount",visBase.getAmount(aspect));
            entries.add(entry);
        }
    }
    public int consumeVis(Aspect aspect,int amount){
        int drain=Math.min(vis.getAmount(aspect),amount);
        if(drain>0)vis.reduce(aspect,drain);
        return drain;
    }
    public int getRange(){return 8;}
    public boolean isSource(){return true;}
}
