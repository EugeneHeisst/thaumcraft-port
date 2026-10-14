package dev.eugheist.tc4modern;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.tags.BlockTags;
/** Modern world queries and mutations for original BlockTaintFibres rules. */
public final class TaintWorld implements thaumcraft.common.blocks.TaintFibresSpread.World {
    private final ServerLevel level;
    private final int biomeY;
    public TaintWorld(ServerLevel level,int biomeY){this.level=level;this.biomeY=biomeY;}
    private net.minecraft.world.level.block.state.BlockState state(int x,int y,int z){return level.getBlockState(new BlockPos(x,y,z));}
    private boolean taint(net.minecraft.world.level.block.state.BlockState state){return state.is(TC4Modern.TAINT.get())||state.is(TC4Modern.TAINT_FIBRES.get());}
    public boolean taintedBiome(int x,int z){return TaintBiomes.isTainted(level,new BlockPos(x,biomeY,z));}
    public void taintBiome(int x,int z){TaintBiomes.spread(level,new BlockPos(x,biomeY,z));}
    public int adjacentTaint(int x,int y,int z){int count=0;var pos=new BlockPos(x,y,z);for(var d:Direction.values())if(taint(level.getBlockState(pos.relative(d))))count++;return count;}
    public boolean onlyAdjacentToTaint(int x,int y,int z){var pos=new BlockPos(x,y,z);for(var d:Direction.values()){var state=level.getBlockState(pos.relative(d));if(!state.isAir()&&!taint(state))return false;}return true;}
    public boolean adjacentSolid(int x,int y,int z){var pos=new BlockPos(x,y,z);for(var d:Direction.values()){var neighbor=pos.relative(d);if(level.getBlockState(neighbor).isFaceSturdy(level,neighbor,d.getOpposite()))return true;}return false;}
    public boolean liquid(int x,int y,int z){return !state(x,y,z).getFluidState().isEmpty();}
    public boolean fibreReplaceable(int x,int y,int z){var state=state(x,y,z);return state.isAir()||state.canBeReplaced()||state.getBlock() instanceof net.minecraft.world.level.block.BushBlock||state.is(BlockTags.LEAVES);}
    public boolean air(int x,int y,int z){return state(x,y,z).isAir();}
    public boolean solidTop(int x,int y,int z){var pos=new BlockPos(x,y,z);return state(x,y,z).isFaceSturdy(level,pos,Direction.UP);}
    public boolean woodOrCactusOrGourd(int x,int y,int z){var s=state(x,y,z);return s.is(BlockTags.LOGS)||s.is(Blocks.CACTUS)||s.is(Blocks.PUMPKIN)||s.is(Blocks.CARVED_PUMPKIN)||s.is(Blocks.JACK_O_LANTERN)||s.is(Blocks.MELON);}
    public boolean groundOrSandOrClayOrPlant(int x,int y,int z){var s=state(x,y,z);return s.is(BlockTags.DIRT)||s.is(BlockTags.SAND)||s.is(Blocks.CLAY)||s.getBlock() instanceof net.minecraft.world.level.block.BushBlock;}
    public void fibres(int x,int y,int z,int variant){level.setBlock(new BlockPos(x,y,z),TC4Modern.TAINT_FIBRES.get().defaultBlockState().setValue(TaintFibresBlock.VARIANT,variant),3);}
    public void solidTaint(int x,int y,int z,int variant){level.setBlock(new BlockPos(x,y,z),TC4Modern.TAINT.get().defaultBlockState().setValue(TaintBlock.VARIANT,variant),3);}
    public void remove(int x,int y,int z){level.removeBlock(new BlockPos(x,y,z),false);}
    public void schedule(int x,int y,int z){var pos=new BlockPos(x,y,z);level.scheduleTick(pos,level.getBlockState(pos).getBlock(),1);}
    public void spore(int x,int y,int z){var spore=new thaumcraft.common.entities.monster.EntityTaintSpore(TC4Modern.TAINT_SPORE.get(),level);spore.moveTo(x+.5,y,z+.5,0,0);level.addFreshEntity(spore);}
    public boolean hasSpore(int x,int y,int z){return !level.getEntitiesOfClass(thaumcraft.common.entities.monster.EntityTaintSpore.class,new net.minecraft.world.phys.AABB(x,y,z,x+1,y+1,z+1)).isEmpty();}
}
