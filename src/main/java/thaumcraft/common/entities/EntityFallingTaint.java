package thaumcraft.common.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Original EntityFallingTaint update and persistence with modern movement/network hooks. */
public final class EntityFallingTaint extends Entity implements net.neoforged.neoforge.entity.IEntityWithComplexSpawn {
    public interface World {
        boolean isSource(BlockPos pos);
        void remove(BlockPos pos);
        boolean denseGoo(BlockPos pos);
        boolean piston(BlockPos pos);
        boolean canPlace(BlockPos pos);
        boolean canFallBelow(BlockPos pos);
        void place(BlockPos pos,int metadata);
        void gore(Entity entity,float pitch);
    }
    public static java.util.function.Function<Level,World> world;
    public static java.util.function.IntFunction<BlockState> state;
    public static java.util.function.Consumer<EntityFallingTaint> clientParticles=e->{};
    public int metadata,fallTime;
    public BlockPos origin=BlockPos.ZERO;
    public EntityFallingTaint(EntityType<? extends EntityFallingTaint> type,Level level){super(type,level);blocksBuilding=true;}
    public BlockState blockState(){return state.apply(metadata);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){}
    @Override public boolean isPickable(){return !isRemoved();}
    @Override public void tick(){
        xo=getX();yo=getY();zo=getZ();++fallTime;
        setDeltaMovement(getDeltaMovement().add(0,-(double).04f,0));
        move(MoverType.SELF,getDeltaMovement());
        setDeltaMovement(getDeltaMovement().scale((double).98f));
        if(!level().isClientSide){
            // Legacy Entity.posY denotes the center (yOffset = height / 2), modern Entity.y the feet.
            World access=world.apply(level());BlockPos pos=BlockPos.containing(getX(),getY()+getBbHeight()/2.0f,getZ());
            if(fallTime==1){if(!access.isSource(origin)){discard();return;}access.remove(origin);}
            if(onGround()||access.denseGoo(pos.below())){
                Vec3 velocity=getDeltaMovement();setDeltaMovement(velocity.x*.7f,velocity.y*-.5,velocity.z*.7f);
                if(access.piston(pos))return;
                access.gore(this,((random.nextFloat()-random.nextFloat())*.2f+1)*.8f);
                discard();
                if(access.canPlace(pos)&&!access.canFallBelow(pos.below()))access.place(pos,metadata);
            }else if((fallTime>100&&(pos.getY()<1||pos.getY()>256))||fallTime>600)discard();
        }else if(onGround()||fallTime==1){for(int i=0;i<10;i++)clientParticles.accept(this);}
    }
    @Override protected void addAdditionalSaveData(CompoundTag tag){
        tag.putByte("Data",(byte)metadata);tag.putByte("Time",(byte)fallTime);
        tag.putInt("OldX",origin.getX());tag.putInt("OldY",origin.getY());tag.putInt("OldZ",origin.getZ());
        tag.putFloat("FallHurtAmount",2);tag.putInt("FallHurtMax",40);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){metadata=tag.getByte("Data")&255;fallTime=tag.getByte("Time")&255;origin=new BlockPos(tag.getInt("OldX"),tag.getInt("OldY"),tag.getInt("OldZ"));}
    @Override public void writeSpawnData(RegistryFriendlyByteBuf buffer){buffer.writeByte(metadata);}
    @Override public void readSpawnData(RegistryFriendlyByteBuf buffer){metadata=buffer.readByte();}
}
