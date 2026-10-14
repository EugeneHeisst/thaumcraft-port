package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.entities.EntityFallingTaint;

/** World operations used by the original falling taint entity. */
public final class TaintFallingBridge implements EntityFallingTaint.World, thaumcraft.common.blocks.TaintFallingRules.World {
    private final Level level;
    public TaintFallingBridge(Level level){this.level=level;}
    public boolean isSource(BlockPos pos){return level.getBlockState(pos).is(TC4Modern.TAINT.get());}
    public void remove(BlockPos pos){level.removeBlock(pos,false);}
    public boolean denseGoo(BlockPos pos){var s=level.getBlockState(pos);return s.is(TC4Modern.FLUX_GOO.get())&&s.getValue(FluxBlock.AMOUNT)>=5;}
    public boolean piston(BlockPos pos){var s=level.getBlockState(pos);return s.is(Blocks.PISTON)||s.is(Blocks.PISTON_HEAD)||s.is(Blocks.MOVING_PISTON);}
    public boolean canPlace(BlockPos pos){var s=level.getBlockState(pos);return s.is(TC4Modern.TAINT_FIBRES.get())||s.is(TC4Modern.FLUX_GOO.get())||s.canBeReplaced();}
    public boolean canFallBelow(BlockPos pos){return canFallBelow(level,pos);}
    public static boolean canFallBelow(Level level,BlockPos pos){
        for(var sample:BlockPos.betweenClosed(pos.offset(-1,-1,-1),pos.offset(1,1,1)))if(level.getBlockState(sample).is(BlockTags.LOGS))return false;
        var s=level.getBlockState(pos);
        if(s.isAir())return true;
        if(s.is(TC4Modern.FLUX_GOO.get())&&s.getValue(FluxBlock.AMOUNT)>=5)return false;
        return s.is(Blocks.FIRE)||s.is(TC4Modern.TAINT_FIBRES.get())||s.canBeReplaced()||!s.getFluidState().isEmpty();
    }
    public void place(BlockPos pos,int metadata){level.setBlock(pos,TC4Modern.TAINT.get().defaultBlockState().setValue(TaintBlock.VARIANT,metadata),3);}
    public void gore(Entity entity,float pitch){level.playSound(null,entity.blockPosition(),TC4Modern.GORE.get(),net.minecraft.sounds.SoundSource.BLOCKS,.5f,pitch);}
    public boolean air(int x,int y,int z){return level.getBlockState(new BlockPos(x,y,z)).isAir();}
    public boolean taint(int x,int y,int z){return isSource(new BlockPos(x,y,z));}
    public boolean canFallBelow(int x,int y,int z){return canFallBelow(new BlockPos(x,y,z));}
    public boolean loadedArea(int x,int y,int z,int radius){return level.hasChunksAt(new BlockPos(x-radius,y-radius,z-radius),new BlockPos(x+radius,y+radius,z+radius));}
    public void spawn(int x,int y,int z,int metadata,int ox,int oy,int oz){
        var entity=new EntityFallingTaint(TC4Modern.FALLING_TAINT.get(),level);
        entity.metadata=metadata;entity.origin=new BlockPos(ox,oy,oz);
        entity.setPos(x+.5,y-entity.getBbHeight()/2.0f,z+.5);level.addFreshEntity(entity);
    }
    public void remove(int x,int y,int z){remove(new BlockPos(x,y,z));}
    public void place(int x,int y,int z,int metadata){place(new BlockPos(x,y,z),metadata);}

}
