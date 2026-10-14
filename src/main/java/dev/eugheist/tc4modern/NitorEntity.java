package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Minecraft tile bridge; original wisp particle integration follows before release. */
public final class NitorEntity extends BlockEntity {
    public NitorEntity(BlockPos pos,BlockState state){super(TC4Modern.NITOR_ENTITY.get(),pos,state);}
    public final java.util.List<thaumcraft.common.tiles.NitorWisp> wisps=new java.util.ArrayList<>();
    public static java.util.function.IntSupplier particleSetting=()->2;
    public static java.util.function.Predicate<BlockPos> visible=pos->false;
    public static void tick(Level level,BlockPos pos,BlockState state,NitorEntity nitor){
        if(!level.isClientSide)return;
        nitor.wisps.removeIf(wisp->!wisp.tick(w->{
            double x=pos.getX()+w.x,y=pos.getY()+w.y,z=pos.getZ()+w.z;
            var bounds=new net.minecraft.world.phys.AABB(x-.05,y,z-.05,x+.05,y+.1,z+.05);
            var requested=new net.minecraft.world.phys.Vec3(w.dx,w.dy,w.dz);
            var motion=net.minecraft.world.entity.Entity.collideBoundingBox(null,requested,bounds,level,java.util.List.of());
            w.x+=motion.x;w.y+=motion.y;w.z+=motion.z;
            if(motion.x!=requested.x)w.dx=0;
            if(motion.y!=requested.y)w.dy=0;
            if(motion.z!=requested.z)w.dz=0;
            return motion.y!=requested.y && requested.y<0;
        }));
        int setting=particleSetting.getAsInt();
        int factor=setting==2?0:setting==1?1:2;
        if(level.getGameTime()%100==0 && Boolean.getBoolean("tc4modern.debugNitor"))
            org.slf4j.LoggerFactory.getLogger("tc4modern.Nitor").info("tick {} visible={} setting={} wisps={}",pos,visible.test(pos),setting,nitor.wisps.size());
        if(!visible.test(pos))return;
        java.util.random.RandomGenerator random=new java.util.random.RandomGenerator(){
            @Override public long nextLong(){return level.random.nextLong();}
            @Override public float nextFloat(){return level.random.nextFloat();}
        };
        if(level.random.nextInt(9-2*factor)==0)nitor.wisps.add(new thaumcraft.common.tiles.NitorWisp(random,
                .3f+level.random.nextFloat()*.4f,.3f+level.random.nextFloat()*.4f,.5f,4,-.025f));
        if(level.random.nextInt(15-4*factor)==0)nitor.wisps.add(new thaumcraft.common.tiles.NitorWisp(random,
                .4f+level.random.nextFloat()*.2f,.4f+level.random.nextFloat()*.2f,.25f,1,-.02f));
    }
}
