package dev.eugheist.tc4modern;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.biome.Biome;

/** Client side of the original general.blue_magical_forest option: grass 0x66AACC and foliage 0x77CCEE instead of the green defaults. */
public final class MagicalForestColors {
    public static final int BLUE_GRASS=0x66AACC,BLUE_FOLIAGE=0x77CCEE;
    private static volatile Object level;private static volatile Biome biome;
    private MagicalForestColors(){}
    public static boolean blue(Biome candidate){
        if(!WarpClientConfig.blueMagicalForest())return false;
        var current=Minecraft.getInstance().level;if(current==null)return false;
        if(current!=level){biome=current.registryAccess().registryOrThrow(Registries.BIOME).getOptional(MagicalForestContent.BIOME).orElse(null);level=current;}
        return candidate==biome;
    }
}
