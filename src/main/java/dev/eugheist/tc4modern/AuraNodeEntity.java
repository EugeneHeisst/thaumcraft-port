package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.lib.vis.NodeReservoir;
/** Modern persistence and synchronization for the original node reservoir. */
public class AuraNodeEntity extends BlockEntity {
    public final NodeReservoir reservoir=new NodeReservoir(new java.util.Random());
    public thaumcraft.api.nodes.NodeType nodeType=thaumcraft.api.nodes.NodeType.NORMAL;
    public int drainEntityId=-1,drainColor=0xFFFFFF,beamColor=0xFFFFFF;
    public long drainUntil;
    public void drain(net.minecraft.world.entity.player.Player player,int color){drainEntityId=player.getId();drainColor=color;drainUntil=level.getGameTime()+6;}
    public String nodeId=java.util.UUID.randomUUID().toString();
    public AuraNodeEntity(BlockPos pos,BlockState state){this(TC4Modern.AURA_NODE_ENTITY.get(),pos,state);}
    /** Addon node subclasses retain their registered block entity type when saved and reloaded. */
    protected AuraNodeEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,BlockPos pos,BlockState state){super(type,pos,state);}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.saveAdditional(tag,registries);reservoir.aspects.writeToNBT(tag);tag.putString("nodeId",nodeId);tag.putByte("type",(byte)nodeType.ordinal());
        tag.putLong("lastActive",reservoir.lastActive);tag.putByte("modifier",reservoir.modifier==null?(byte)-1:(byte)reservoir.modifier.ordinal());
        CompoundTag base=new CompoundTag();reservoir.aspectsBase.writeToNBT(base);tag.put("AspectsBase",base.getList("Aspects",10));
    }
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){
        super.loadAdditional(tag,registries);reservoir.aspects.readFromNBT(tag);if(tag.contains("nodeId"))nodeId=tag.getString("nodeId");
        CompoundTag base=new CompoundTag();base.put("Aspects",tag.getList("AspectsBase",10));reservoir.aspectsBase.readFromNBT(base);
        int type=tag.getByte("type");nodeType=type>=0 && type<thaumcraft.api.nodes.NodeType.values().length?thaumcraft.api.nodes.NodeType.values()[type]:thaumcraft.api.nodes.NodeType.NORMAL;
        reservoir.lastActive=tag.getLong("lastActive");int modifier=tag.contains("modifier")?tag.getByte("modifier"):-1;
        reservoir.modifier=modifier>=0 && modifier<thaumcraft.api.nodes.NodeModifier.values().length?thaumcraft.api.nodes.NodeModifier.values()[modifier]:null;
        drainEntityId=tag.contains("drainEntity")?tag.getInt("drainEntity"):-1;drainColor=tag.contains("drainColor")?tag.getInt("drainColor"):0xFFFFFF;
        reservoir.regeneration=-1;reservoir.catchUp=reservoir.lastActive>0;
    }
    public static void tick(net.minecraft.world.level.Level level,BlockPos pos,BlockState state,AuraNodeEntity node){
        if(level.isClientSide){NodeWorldClient.tick(node);if(++node.reservoir.count%50==0&&node.nodeType==thaumcraft.api.nodes.NodeType.DARK)SinisterStone.nodeSeen.accept(level,pos);node.beamColor=thaumcraft.common.lib.vis.NodeBeam.smoothColor(node.beamColor,node.drainColor);return;}
        if(node.drainEntityId>=0){var player=level.getEntity(node.drainEntityId);if(level.getGameTime()>node.drainUntil || !(player instanceof net.minecraft.world.entity.player.Player p) || !p.isUsingItem()){node.drainEntityId=-1;node.changed();}}
        node.reservoir.count++;
        if(node.reservoir.count<=1||node.reservoir.count%50==0){
            int lock=level.getBlockEntity(pos.below()) instanceof NodeStabilizerEntity stabilizer&&!level.hasNeighborSignal(pos.below())?stabilizer.lock():0;
            if(node.reservoir.lock!=lock){node.reservoir.lock=lock;node.reservoir.regeneration=-1;}
        }
        int before=node.reservoir.aspects.visSize();
        boolean changed=NodePredation.discharge(node,level.random);
        changed|=node.reservoir.recharge(System.currentTimeMillis());
        changed|=NodeWorldEffects.tick(node);
        if(node.reservoir.count%100==0)changed|=NodeStabilizerEntity.stabilize(node,level.random);
        changed |= before!=node.reservoir.aspects.visSize();
        if(node.reservoir.destroyed){level.removeBlock(pos,false);return;}
        if(changed)node.changed();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){var tag=saveWithoutMetadata(registries);tag.putInt("drainEntity",drainEntityId);tag.putInt("drainColor",drainColor);return tag;}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
