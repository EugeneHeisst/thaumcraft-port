package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/** Native client host for the original TileArcaneFurnace event and ClientProxy lava formula. */
public final class InfernalFurnaceClient {
    private InfernalFurnaceClient(){}
    public static void smelt(Level level,BlockPos pos,Direction outlet){
        var random=level.random;
        int fx=outlet.getStepX(),fz=outlet.getStepZ();
        for(int a=0;a<5;a++){
            double x=pos.getX()+.5f+(random.nextFloat()-random.nextFloat())*.3f+fx*1.0f;
            double y=pos.getY()+.3f;
            double z=pos.getZ()+.5f+(random.nextFloat()-random.nextFloat())*.3f+fz*1.0f;
            var particle=Minecraft.getInstance().particleEngine.createParticle(ParticleTypes.LAVA,x,y,z,0,0,0);
            float vy=.2f*random.nextFloat();
            float qx=fx==0?(random.nextFloat()-random.nextFloat())*.5f:fx*random.nextFloat();
            float qz=fz==0?(random.nextFloat()-random.nextFloat())*.5f:fz*random.nextFloat();
            if(particle!=null)particle.setParticleSpeed(.15f*qx,vy,.15f*qz);
            level.playLocalSound(pos.getX()+.5f,pos.getY()+.5f,pos.getZ()+.5f,SoundEvents.LAVA_POP,
                SoundSource.BLOCKS,.1f+random.nextFloat()*.1f,.9f+random.nextFloat()*.15f,false);
        }
    }
}
