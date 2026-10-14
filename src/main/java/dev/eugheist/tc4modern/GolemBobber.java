package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;

/** EntityGolemBobber: a float lobbed onto the water, bobbing and splashing until AIFish lands a catch; never saved. */
public final class GolemBobber extends Entity {
    static final EntityDataAccessor<Integer> FISHER=SynchedEntityData.defineId(GolemBobber.class,EntityDataSerializers.INT);
    GolemBobber(EntityType<? extends GolemBobber> type,Level l){super(type,l);noCulling=true;}
    /** Thrown from the golem towards the top of the water block, lifted by sqrt(distance)*0.08. */
    GolemBobber(StrawGolem fisher,BlockPos water){
        this(Golemancy.BOBBER.get(),fisher.level());entityData.set(FISHER,fisher.getId());
        double dx=water.getX()+.5-fisher.getX(),dy=water.getY()+1-fisher.getY(),dz=water.getZ()+.5-fisher.getZ(),d=Math.sqrt(dx*dx+dy*dy+dz*dz);
        setDeltaMovement(dx*.1,dy*.1+Math.sqrt(d)*.08,dz*.1);setPos(fisher.getX(),fisher.getY(),fisher.getZ());
    }
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){b.define(FISHER,-1);}
    StrawGolem fisher(){return level().getEntity(entityData.get(FISHER)) instanceof StrawGolem g?g:null;}
    @Override public boolean shouldRenderAtSqrDistance(double d){double size=getBoundingBox().getSize()*4*64;return d<size*size;}
    @Override public void tick(){
        super.tick();
        if(level() instanceof ServerLevel server){var f=fisher();if(f==null||!f.isAlive()){discard();return;}if(random.nextFloat()<.02f)server.sendParticles(ParticleTypes.SPLASH,getX()+random.nextFloat()-random.nextFloat(),getY()+random.nextFloat(),getZ()+random.nextFloat()-random.nextFloat(),2+random.nextInt(2),.1,0,.1,0);}
        if(tickCount>4000){discard();return;}
        var v=getDeltaMovement();var from=position();var hit=level().clip(new ClipContext(from,from.add(v),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,this));
        /* rayTraceBlocks ignores water, so any block hit is solid ground: the float is lost */
        if(hit.getType()!=HitResult.Type.MISS){if(!level().isClientSide)discard();return;}
        move(MoverType.SELF,v);
        float drag=onGround()||horizontalCollision?.5f:.92f;var box=getBoundingBox();double wet=0;
        for(int j=0;j<5;j++){double y0=box.minY+box.getYsize()*j/5,y1=box.minY+box.getYsize()*(j+1)/5;var slice=new AABB(box.minX,y0,box.minZ,box.maxX,y1,box.maxZ);if(inWater(slice))wet+=.2;}
        v=getDeltaMovement().add(0,.04*(wet*2-1),0);if(wet>0){drag*=.9f;v=new Vec3(v.x,v.y*.8,v.z);}
        setDeltaMovement(v.scale(drag));
    }
    boolean inWater(AABB box){for(var p:BlockPos.betweenClosed(BlockPos.containing(box.minX,box.minY,box.minZ),BlockPos.containing(box.maxX,box.maxY,box.maxZ))){var fluid=level().getFluidState(p);if(fluid.is(FluidTags.WATER)&&box.minY<p.getY()+fluid.getHeight(level(),p))return true;}return false;}
    @Override protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag t){}
    @Override protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag t){}
}
