package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import thaumcraft.api.aspects.*;
import thaumcraft.common.tiles.essentia.AlchemyMachineState;

public final class AlchemyMachineEntity extends BlockEntity implements IEssentiaTransport {
    public final AlchemyMachineState state;
    public int venting;
    public AlchemyMachineEntity(BlockPos p,BlockState s){super(AlchemyMachines.ENTITY.get(),p,s);state=new AlchemyMachineState(s.is(AlchemyMachines.CRYSTALIZER.get()));}
    public void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    public static void tick(Level l,BlockPos p,BlockState s,AlchemyMachineEntity t){
        boolean power=l.hasNeighborSignal(p);
        if(l.isClientSide){
            int before=(int)t.state.rotation;t.state.animate(power);
            if(!t.state.crystalizer&&t.state.rotation%180<=20&&before%180>=160&&t.state.speed>0)
                l.playLocalSound(p.getX()+.5,p.getY()+.5,p.getZ()+.5,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("pump")),net.minecraft.sounds.SoundSource.BLOCKS,1,1,false);
            if(t.venting>0){t.venting--;vent(l,p,t.state.facing.getOpposite(),2.1);}
        }else{
            var source=EssentiaTransport.connectable(l,p,t.state.crystalizer?t.state.facing:Direction.DOWN);
            if(t.state.tick(power,source,requested->VisNetwork.drain((ServerLevel)l,p,Aspect.EARTH,requested,(node,color)->{}),t::eject,l.random))t.changed();
            /* Progress is intentionally transient in original save data; still dirty the host for inventory operations. */
        }
    }
    static void vent(Level l,BlockPos p,Direction d,double divisor){
        var r=l.random;
        EssentiaTubeEntity.clientVent.accept(new EssentiaTubeEntity.Vent(l,p.getX()+.5+.1-r.nextFloat()*.2+d.getStepX()/divisor,p.getY()+.5+.1-r.nextFloat()*.2+d.getStepY()/divisor,p.getZ()+.5+.1-r.nextFloat()*.2+d.getStepZ()/divisor,d.getStepX()/4.0+.1-r.nextFloat()*.2,d.getStepY()/4.0+.1-r.nextFloat()*.2,d.getStepZ()/4.0+.1-r.nextFloat()*.2,0xFFFFFF));
    }
    void eject(Aspect aspect){
        var stack=LiquidDeath.crystal(aspect,1);var out=state.facing.getOpposite();
        var handler=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,worldPosition.relative(out),state.facing);
        if(handler!=null)stack=net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(handler,stack,false);
        if(!stack.isEmpty()){
            var item=new ItemEntity(level,worldPosition.getX()+.5+out.getStepX()*.65,worldPosition.getY()+.5+out.getStepY()*.65,worldPosition.getZ()+.5+out.getStepZ()*.65,stack);
            item.setDeltaMovement(out.getStepX()*.04,out.getStepY()*.04,out.getStepZ()*.04);level.addFreshEntity(item);level.blockEvent(worldPosition,getBlockState().getBlock(),0,0);
        }
        level.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.FIRE_EXTINGUISH,net.minecraft.sounds.SoundSource.BLOCKS,.25f,2.6f+(level.random.nextFloat()-level.random.nextFloat())*.8f);
    }
    @Override public boolean triggerEvent(int i,int j){if(i>=0){if(level!=null&&level.isClientSide)venting=7;return true;}return super.triggerEvent(i,j);}
    public boolean isConnectable(Direction d){return state.crystalizer?d==state.facing:d==Direction.UP||d==Direction.DOWN;}
    public boolean canInputFrom(Direction d){return d==(state.crystalizer?state.facing:Direction.DOWN);}
    public boolean canOutputTo(Direction d){return !state.crystalizer&&d==Direction.UP;}
    public void setSuction(Aspect a,int n){}
    public Aspect getSuctionType(Direction d){return null;}
    public int getSuctionAmount(Direction d){return state.suction(d,level!=null&&level.hasNeighborSignal(worldPosition));}
    public Aspect getEssentiaType(Direction d){return state.crystalizer?state.input:state.output;}
    public int getEssentiaAmount(Direction d){return getEssentiaType(d)==null?0:1;}
    public int takeEssentia(Aspect a,int n,Direction d){if(canOutputTo(d)&&state.output!=null&&a==state.output){state.output=null;changed();return n;}return 0;}
    public int addEssentia(Aspect a,int n,Direction d){int r=state.add(a,n,d);if(r!=0)changed();return r;}
    public int getMinimumSuction(){return 0;}
    public boolean renderExtendedTube(){return false;}
    @Override protected void saveAdditional(CompoundTag n,HolderLookup.Provider r){super.saveAdditional(n,r);if(state.input!=null)n.putString("aspectIn",state.input.getTag());if(state.output!=null)n.putString("aspectOut",state.output.getTag());n.putByte("face",(byte)state.facing.get3DDataValue());}
    @Override protected void loadAdditional(CompoundTag n,HolderLookup.Provider r){super.loadAdditional(n,r);state.input=Aspect.getAspect(n.getString("aspectIn"));state.output=Aspect.getAspect(n.getString("aspectOut"));state.facing=Direction.from3DDataValue(n.getByte("face"));}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider r){return saveWithoutMetadata(r);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
