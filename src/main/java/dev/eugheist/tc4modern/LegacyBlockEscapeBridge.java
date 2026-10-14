package dev.eugheist.tc4modern;
/** Modern collision queries for the source entity's legacy push-out motion. */
public final class LegacyBlockEscapeBridge {
    public static void escape(net.minecraft.world.entity.LivingEntity entity){
        var level=entity.level();var random=entity.getRandom();var velocity=entity.getDeltaMovement();
        var rng=new java.util.random.RandomGenerator(){public long nextLong(){return random.nextLong();}public float nextFloat(){return random.nextFloat();}};
        var result=thaumcraft.common.entities.LegacyBlockEscape.escape((x,y,z)->{var p=new net.minecraft.core.BlockPos(x,y,z);var shape=level.getBlockState(p).getCollisionShape(level,p);if(shape.isEmpty())return false;var bounds=shape.bounds();return (bounds.getXsize()+bounds.getYsize()+bounds.getZsize())/3>=1;},level.getBlockCollisions(entity,entity.getBoundingBox()).iterator().hasNext(),entity.getX(),entity.getY(),entity.getZ(),velocity.x,velocity.y,velocity.z,rng);
        entity.setDeltaMovement(result.x(),result.y(),result.z());
    }
    private LegacyBlockEscapeBridge(){}
}
