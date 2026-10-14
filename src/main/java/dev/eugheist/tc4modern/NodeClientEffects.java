package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Loaded only by client branches; common block/entity classes never construct client particles. */
final class NodeClientEffects {
    static int predationEffects;
    static void predation(BlockPos from,BlockPos to){
        var level=net.minecraft.client.Minecraft.getInstance().level;if(level==null)return;
        NodeBoltParticle.spawn(level,from.getX()+.5,from.getY()+.5,from.getZ()+.5,to.getX()+.5,to.getY()+.5,to.getZ()+.5);
        level.playLocalSound(from.getX()+.5,from.getY()+.5,from.getZ()+.5,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(AlchemyContent.id("zap")),net.minecraft.sounds.SoundSource.BLOCKS,.1f,1+level.random.nextFloat()*.2f,false);
        predationEffects++;
    }
    static void burst(Level level, double x, double y, double z) { burst(level, x, y, z, 1); }
    static void burst(Level level, double x, double y, double z, float size) {
        net.minecraft.client.Minecraft.getInstance().particleEngine.add(new NodeConversionBurst((net.minecraft.client.multiplayer.ClientLevel)level,x,y,z,size));
    }
    static void converter(Level level, BlockPos pos, boolean stabilizer) {
        var client=(net.minecraft.client.multiplayer.ClientLevel)level;
        if(level.random.nextBoolean())NodeBoltParticle.spawn(client,pos.getX()+.25f+level.random.nextFloat()*.5f,pos.getY()+.5f,pos.getZ()+.25f+level.random.nextFloat()*.5f,pos.getX()+.5f,pos.getY()-.5f,pos.getZ()+.5f);
        if(level.random.nextBoolean()&&stabilizer)NodeBoltParticle.spawn(client,pos.getX()+.25f+level.random.nextFloat()*.5f,pos.getY()-1.5f,pos.getZ()+.25f+level.random.nextFloat()*.5f,pos.getX()+.5f,pos.getY()-.5f,pos.getZ()+.5f);
    }
}
