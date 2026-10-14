package dev.eugheist.tc4modern;
import net.minecraft.world.level.block.*;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.core.*;import net.minecraft.world.level.*;
/** Original always-burning, non-colliding single candle and infusion stabilizer. */
public final class ThaumicCandleBlock extends Block {
 public final int color;public static final int[] COLORS={0xF0F0F0,15435844,12801229,6719955,14602026,4312372,14188952,0x434343,0xA0A0A0,2651799,8073150,2437522,5320730,3887386,11743532,0x1E1B1B};
 public ThaumicCandleBlock(int color){super(Properties.of().strength(.1f).noCollission().noOcclusion().sound(SoundType.WOOL).lightLevel(s->14));this.color=COLORS[color];}
 @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return box(6,0,6,10,8,10);}
 @Override protected boolean canSurvive(BlockState state,LevelReader level,BlockPos pos){return level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),Direction.UP);}
 @Override protected BlockState updateShape(BlockState state,Direction direction,BlockState neighbor,LevelAccessor level,BlockPos pos,BlockPos neighborPos){if(!canSurvive(state,level,pos)){if(level instanceof Level world)dropResources(state,world,pos);return Blocks.AIR.defaultBlockState();}return state;}
 @Override public void animateTick(BlockState state,Level level,BlockPos pos,net.minecraft.util.RandomSource random){level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME,pos.getX()+.5,pos.getY()+.65,pos.getZ()+.5,0,0,0);level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,pos.getX()+.5,pos.getY()+.65,pos.getZ()+.5,0,0,0);}
}
