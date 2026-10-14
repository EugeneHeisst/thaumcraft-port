package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.common.tiles.NodeConverterState;
/** Native world host for original converter state and aspect transfer. */
public final class NodeConverterEntity extends BlockEntity {
    private boolean updating;
    public final NodeConverterState converter=new NodeConverterState();
    public NodeConverterEntity(BlockPos pos,BlockState state){super(TC4Modern.NODE_CONVERTER_ENTITY.get(),pos,state);}
    private NodeConverterState.Host host(){return new NodeConverterState.Host(){
        public boolean powered(){return level.hasNeighborSignal(worldPosition);}
        public boolean hasStabilizer(){var p=worldPosition.below(2);return level.getBlockEntity(p) instanceof NodeStabilizerEntity&&!level.hasNeighborSignal(p);}
        public boolean ordinaryNode(){return level.getBlockEntity(worldPosition.below()) instanceof AuraNodeEntity;}
        public boolean energizedNode(){return level.getBlockEntity(worldPosition.below()) instanceof EnergizedNodeEntity;}
        public void energize(){
            var p=worldPosition.below();if(!(level.getBlockEntity(p) instanceof AuraNodeEntity old))return;
            var base=old.reservoir.aspectsBase.copy();var type=old.nodeType;var modifier=old.reservoir.modifier;
            level.setBlock(p,TC4Modern.ENERGIZED_NODE.get().defaultBlockState(),3);
            if(level.getBlockEntity(p) instanceof EnergizedNodeEntity next){
                next.auraBase.add(base);next.nodeType=type;next.modifier=modifier;
                var random=new java.util.Random(){@Override public int nextInt(int bound){return level.random.nextInt(bound);}};
                next.energy.setup(base,type,modifier,random);next.changed();
            }
        }
        public void revertAndEmpty(){
            var p=worldPosition.below();if(!(level.getBlockEntity(p) instanceof EnergizedNodeEntity old))return;
            var base=old.auraBase.copy();var type=old.nodeType;var modifier=old.modifier;
            level.setBlock(p,TC4Modern.AURA_NODE.get().defaultBlockState(),3);
            if(level.getBlockEntity(p) instanceof AuraNodeEntity next){next.reservoir.aspectsBase.add(base);next.reservoir.aspects.add(base);for(var aspect:next.reservoir.aspects.getAspects())next.reservoir.aspects.reduce(aspect,next.reservoir.aspects.getAmount(aspect));next.nodeType=type;next.reservoir.modifier=modifier;next.changed();}
        }
        public void drainRandomAspect(int count){
            if(level.getBlockEntity(worldPosition.below()) instanceof AuraNodeEntity node){
                var aspects=node.reservoir.aspects.getAspects();if(aspects.length==0)return;
                node.reservoir.aspects.reduce(aspects[level.random.nextInt(aspects.length)],1);node.setChanged();
                if(count%5==0||node.reservoir.aspects.visSize()==0)node.changed();
            }
        }
        public void explodeNode(){NodeExplosion.explode(level,worldPosition.below());}
        public void changed(){NodeConverterEntity.this.changed();}
        public void conversionEvent(){level.blockEvent(worldPosition,getBlockState().getBlock(),10,10);}
    };}
    public void refresh(){
        if(updating||level==null||level.isClientSide)return;
        updating=true;try{converter.checkStatus(host());}finally{updating=false;}
    }
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level level,BlockPos pos,BlockState state,NodeConverterEntity tile){
        if(level.isClientSide){
            int previousCount=tile.converter.count;
            tile.converter.count=NodeDeviceVisuals.converterCount(tile.converter.count,tile.converter.status,level.hasNeighborSignal(pos));
            if(tile.converter.count>50&&tile.converter.count!=previousCount){
                NodeClientEffects.converter(level,pos,tile.host().hasStabilizer());
            }
            return;
        }
        if(tile.updating)return;
        tile.updating=true;try{tile.converter.tick(tile.host());tile.setChanged();}finally{tile.updating=false;}
    }
    @Override public boolean triggerEvent(int id,int parameter){
        if(id==10&&parameter==10){
            if(level!=null&&level.isClientSide){
                NodeClientEffects.burst(level,worldPosition.getX()+.5,worldPosition.getY()-.5,worldPosition.getZ()+.5);
                level.playLocalSound(worldPosition.getX()+.5,worldPosition.getY()-.5,worldPosition.getZ()+.5,
                    net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(TC4Modern.MODID,"craftfail")),
                    net.minecraft.sounds.SoundSource.BLOCKS,.5f,1f,false);
            }
            return true;
        }
        return super.triggerEvent(id,parameter);
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider provider){super.saveAdditional(tag,provider);converter.write(tag);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider provider){super.loadAdditional(tag,provider);converter.read(tag);}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider provider){return saveWithoutMetadata(provider);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
