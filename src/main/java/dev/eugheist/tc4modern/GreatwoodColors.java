package dev.eugheist.tc4modern;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;

/** Original BlockMagicalLeaves averages foliage RGB across a fixed three by three biome square. */
public final class GreatwoodColors {
    private GreatwoodColors(){}
    public static int foliage(BlockAndTintGetter world,BlockPos pos){
        var level=net.minecraft.client.Minecraft.getInstance().level;
        if(world==null||pos==null||level==null)return net.minecraft.world.level.FoliageColor.getDefaultColor();
        int red=0,green=0,blue=0;
        for(int z=-1;z<=1;z++)for(int x=-1;x<=1;x++){
            int color=level.getBiome(pos.offset(x,0,z)).value().getFoliageColor();
            red+=(color>>16)&255;green+=(color>>8)&255;blue+=color&255;
        }
        return (red/9<<16)|(green/9<<8)|blue/9;
    }
}
