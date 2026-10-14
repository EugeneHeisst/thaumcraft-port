package dev.eugheist.tc4modern.mixin;

import dev.eugheist.tc4modern.MagicalForestColors;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Original BiomeGenMagicalForest getBiomeGrassColor/getBiomeFoliageColor return the old blue colours when Config.blueBiome is set. */
@Mixin(Biome.class)
public abstract class MagicalForestColorMixin {
    @Inject(method="getGrassColor",at=@At("HEAD"),cancellable=true)
    private void tc4modern$blueGrass(double x,double z,CallbackInfoReturnable<Integer> result){if(MagicalForestColors.blue((Biome)(Object)this))result.setReturnValue(MagicalForestColors.BLUE_GRASS);}
    @Inject(method="getFoliageColor",at=@At("HEAD"),cancellable=true)
    private void tc4modern$blueFoliage(CallbackInfoReturnable<Integer> result){if(MagicalForestColors.blue((Biome)(Object)this))result.setReturnValue(MagicalForestColors.BLUE_FOLIAGE);}
}
