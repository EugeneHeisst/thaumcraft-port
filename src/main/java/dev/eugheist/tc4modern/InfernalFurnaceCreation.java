package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.InfernalFurnaceStructure;

/** Native host for original WandManager creation scan and replacement metadata. */
public final class InfernalFurnaceCreation {
    private InfernalFurnaceCreation(){}
    private static InfernalFurnaceStructure.Material material(Level level,BlockPos pos){
        var state=level.getBlockState(pos);
        if(state.isAir())return InfernalFurnaceStructure.Material.AIR;
        if(state.is(Blocks.NETHER_BRICKS))return InfernalFurnaceStructure.Material.NETHER_BRICK;
        if(state.is(Blocks.OBSIDIAN))return InfernalFurnaceStructure.Material.OBSIDIAN;
        if(state.is(Blocks.LAVA))return InfernalFurnaceStructure.Material.LAVA;
        if(state.is(Blocks.IRON_BARS))return InfernalFurnaceStructure.Material.IRON_BARS;
        return InfernalFurnaceStructure.Material.OTHER;
    }
    public static boolean create(Level level,BlockPos clicked,Player player,ItemStack wand){
        if(!ResearchProgress.completed(player.getPersistentData()).contains("INFERNALFURNACE"))return false;
        var cost=new AspectList().add(Aspect.FIRE,50).add(Aspect.EARTH,50);
        InfernalFurnaceStructure.View view=(x,y,z)->material(level,new BlockPos(x,y,z));
        for(int x=clicked.getX()-2;x<=clicked.getX();x++)for(int y=clicked.getY()-2;y<=clicked.getY();y++)for(int z=clicked.getZ()-2;z<=clicked.getZ();z++){
            if(!InfernalFurnaceStructure.fits(view,x,y,z)||!WandData.consumeCrafting(wand,cost,false,player))continue;
            if(level.isClientSide)return true;
            if(!WandData.consumeCrafting(wand,cost,true,player))return false;
            var outlet=net.minecraft.core.Direction.SOUTH;
            for(var direction:net.minecraft.core.Direction.Plane.HORIZONTAL)
                if(level.getBlockState(new BlockPos(x+1,y+1,z+1).relative(direction)).is(Blocks.IRON_BARS))outlet=direction;
            for(int yy=0;yy<3;yy++)for(int zz=0;zz<3;zz++)for(int xx=0;xx<3;xx++){
                var pos=new BlockPos(x+xx,y+yy,z+zz);int part=InfernalFurnaceStructure.convertedMetadata(material(level,pos),xx,zz);
                if(part<0)continue;
                level.setBlock(pos,TC4Modern.INFERNAL_FURNACE.get().defaultBlockState().setValue(InfernalFurnaceBlock.PART,part).setValue(InfernalFurnaceBlock.LAYER,yy).setValue(InfernalFurnaceBlock.OUTLET,outlet),2);
                level.blockEvent(pos,TC4Modern.INFERNAL_FURNACE.get(),1,4);
            }
            for(int yy=0;yy<3;yy++)for(int zz=0;zz<3;zz++)for(int xx=0;xx<3;xx++){
                var pos=new BlockPos(x+xx,y+yy,z+zz);level.updateNeighborsAt(pos,level.getBlockState(pos).getBlock());
            }
            level.playSound(null,clicked,net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("tc4modern","wand")),net.minecraft.sounds.SoundSource.BLOCKS,1,1);
            return true;
        }
        return false;
    }
}
