package dev.eugheist.tc4modern;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
public final class BellowsEntity extends BlockEntity {
    public final thaumcraft.common.tiles.BellowsState bellows=new thaumcraft.common.tiles.BellowsState();
    public BellowsEntity(BlockPos pos,BlockState state){super(TC4Modern.BELLOWS_ENTITY.get(),pos,state);}
    public void placed(net.minecraft.core.Direction clicked){
        bellows.orientation=(byte)clicked.getOpposite().get3DDataValue();
        var attached=level.getBlockState(worldPosition.relative(clicked.getOpposite()));
        bellows.onVanillaFurnace=attached.is(net.minecraft.world.level.block.Blocks.FURNACE);
        setChanged();level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);
    }
    public static void tick(Level level,BlockPos pos,BlockState state,BellowsEntity tile){
        boolean powered=level.hasNeighborSignal(pos);
        if(level.isClientSide){tile.bellows.clientTick(powered,new java.util.random.RandomGenerator(){public long nextLong(){return level.random.nextLong();}public float nextFloat(){return level.random.nextFloat();}},pitch->level.playLocalSound(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,net.minecraft.sounds.SoundEvents.GHAST_SHOOT,net.minecraft.sounds.SoundSource.BLOCKS,.01f,(float)pitch,false));return;}
        var direction=Direction.from3DDataValue(tile.bellows.orientation);
        // Original furnace tick intentionally ignores the orientation's vertical offset.
        var target=level.getBlockEntity(pos.offset(direction.getStepX(),0,direction.getStepZ()));
        int progress=target instanceof net.minecraft.world.level.block.entity.FurnaceBlockEntity furnace?furnace.cookingProgress:0;
        int next=tile.bellows.furnaceTick(powered,progress);
        if(next!=progress&&target instanceof net.minecraft.world.level.block.entity.FurnaceBlockEntity furnace){furnace.cookingProgress=next;furnace.setChanged();}
    }
    @Override protected void saveAdditional(CompoundTag tag,HolderLookup.Provider registries){super.saveAdditional(tag,registries);tag.putByte("orientation",bellows.orientation);tag.putBoolean("onVanillaFurnace",bellows.onVanillaFurnace);}
    @Override protected void loadAdditional(CompoundTag tag,HolderLookup.Provider registries){super.loadAdditional(tag,registries);bellows.orientation=tag.getByte("orientation");bellows.onVanillaFurnace=tag.getBoolean("onVanillaFurnace");}
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries){return saveWithoutMetadata(registries);}
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket(){return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);}
}
