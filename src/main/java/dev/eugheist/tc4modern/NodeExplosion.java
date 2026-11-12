package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
/** Original BlockAiry.explodify, with finite flux metadata8 mapped to amount9. */
public final class NodeExplosion {
 public static void validateSupport(Level level,BlockPos pos){
  if(level.isClientSide)return;
  var below=pos.below();
  if(level.hasNeighborSignal(below)||!(level.getBlockEntity(below) instanceof NodeStabilizerEntity)
      ||!(level.getBlockEntity(pos.above()) instanceof NodeConverterEntity))explode(level,pos);
 }
 public static void explode(Level level,BlockPos pos){
  if(level.isClientSide)return;
  level.removeBlock(pos,false);
  level.explode(null,pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5,3f,Level.ExplosionInteraction.NONE);
  for(int a=0;a<50;a++){
   int x=pos.getX()+level.random.nextInt(8)-level.random.nextInt(8);
   int y=pos.getY()+level.random.nextInt(8)-level.random.nextInt(8);
   int z=pos.getZ()+level.random.nextInt(8)-level.random.nextInt(8);
   var p=new BlockPos(x,y,z);if(!level.isEmptyBlock(p))continue;
   level.setBlock(p,(y<pos.getY()?TC4Modern.FLUX_GOO:TC4Modern.FLUX_GAS).get().defaultBlockState().setValue(FluxBlock.AMOUNT,9),3);
  }
 }
}
