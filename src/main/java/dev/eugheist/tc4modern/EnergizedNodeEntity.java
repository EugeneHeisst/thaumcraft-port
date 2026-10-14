package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.*;
import thaumcraft.api.nodes.*;
import thaumcraft.api.visnet.VisNearbyRules;
import thaumcraft.common.tiles.EnergizedNodeState;
/** Modern world lifecycle/persistence host for original energized-node supply. */
public final class EnergizedNodeEntity extends BlockEntity implements thaumcraft.api.visnet.VisParentRules.Node {
    public final EnergizedNodeState energy=new EnergizedNodeState();
    public final AspectList auraBase=new AspectList();
    public NodeType nodeType=NodeType.NORMAL;
    public NodeModifier modifier;
    public String nodeId="blank";
    public EnergizedNodeEntity(BlockPos pos,BlockState state){super(TC4Modern.ENERGIZED_NODE_ENTITY.get(),pos,state);}
    @Override public void onLoad(){super.onLoad();if(level instanceof ServerLevel server)VisNetwork.register(server,this);}
    @Override public void setRemoved(){if(level instanceof ServerLevel server)VisNetwork.unregister(server,this);super.setRemoved();}
    @Override public void onChunkUnloaded(){if(level instanceof ServerLevel server)VisNetwork.unregister(server,this);super.onChunkUnloaded();}
    public int x(){return worldPosition.getX();}public int y(){return worldPosition.getY();}public int z(){return worldPosition.getZ();}
    public int range(){return energy.getRange();}
    public boolean loaded(){return level!=null&&level.hasChunkAt(worldPosition);}
    public boolean valid(){return !isRemoved()&&loaded()&&level.getBlockEntity(worldPosition)==this;}
    public int attunement(){return -1;}
    public Iterable<VisNearbyRules.Node> children(){return level instanceof ServerLevel server?VisNetwork.children(server,this):java.util.List.of();}
    public int consumeVis(Aspect aspect,int amount){return valid()?energy.consumeVis(aspect,amount):0;}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level level,BlockPos pos,BlockState state,EnergizedNodeEntity node){
        if(!(level instanceof ServerLevel))return;
        var random=new java.util.Random(){@Override public int nextInt(int bound){return level.random.nextInt(bound);}};
        node.energy.tick(node.auraBase,node.nodeType,node.modifier,random,node::changed);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);tag.putString("nodeId",nodeId);tag.putByte("type",(byte)nodeType.ordinal());
        tag.putByte("modifier",modifier==null?(byte)-1:(byte)modifier.ordinal());energy.writeVisBase(tag);auraBase.writeToNBT(tag);
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);nodeId=tag.getString("nodeId");int type=tag.getByte("type"),mod=tag.getByte("modifier");
        nodeType=type>=0&&type<NodeType.values().length?NodeType.values()[type]:NodeType.NORMAL;
        modifier=mod>=0&&mod<NodeModifier.values().length?NodeModifier.values()[mod]:null;
        energy.readVisBase(tag);auraBase.readFromNBT(tag);VisNetwork.invalidate();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
