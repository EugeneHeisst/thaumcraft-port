package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.common.blocks.InfernalFurnaceRestoration;

/** Native restoration host, guarded against modern synchronous neighbour recursion. */
public final class InfernalFurnaceLifecycle {
    private static final ThreadLocal<Boolean> RESTORING=ThreadLocal.withInitial(()->false);
    private InfernalFurnaceLifecycle(){}
    public static Block restored(int part){
        return switch(InfernalFurnaceRestoration.restored(part)){
            case AIR->Blocks.AIR;case IRON_BARS->Blocks.IRON_BARS;case OBSIDIAN->Blocks.OBSIDIAN;
            case NETHER_BRICK->Blocks.NETHER_BRICKS;default->throw new IllegalArgumentException("Unknown furnace part");
        };
    }
    public static void restore(Level level,BlockPos center){
        if(level.isClientSide||RESTORING.get())return;
        RESTORING.set(true);
        try{
            for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
                var pos=center.offset(x,y,z);var state=level.getBlockState(pos);
                if(state.is(TC4Modern.INFERNAL_FURNACE.get()))level.setBlock(pos,restored(state.getValue(InfernalFurnaceBlock.PART)).defaultBlockState(),3);
            }
        }finally{RESTORING.set(false);}
    }
    public static void validate(Level level,BlockPos center){
        if(level.isClientSide||RESTORING.get())return;
        if(!InfernalFurnaceRestoration.intact((x,y,z)->level.getBlockState(new BlockPos(x,y,z)).is(TC4Modern.INFERNAL_FURNACE.get()),center.getX(),center.getY(),center.getZ())){
            restore(level,center);level.removeBlock(center,false);
        }
    }
    public static void notifySurroundings(Level level,BlockPos pos){
        if(RESTORING.get())return;
        for(int y=-1;y<=1;y++)for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){
            var neighbor=pos.offset(x,y,z);var state=level.getBlockState(neighbor);
            if(state.is(TC4Modern.INFERNAL_FURNACE.get())&&state.getValue(InfernalFurnaceBlock.PART)==0)validate(level,neighbor);
        }
    }
}
