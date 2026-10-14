package dev.eugheist.tc4modern;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/** Bridges the old biome dictionary probabilities to explicit, extensible biome tags. */
public final class NaturalGreatwoodFeature extends Feature<NoneFeatureConfiguration> {
    private static final String[] TYPES={"coniferous","forest","savanna","plains","swamp","lush","magical"};
    private static final float[] CHANCES={.2f,1f,.2f,.2f,.2f,.5f,1f};
    public NaturalGreatwoodFeature(){super(NoneFeatureConfiguration.CODEC);}
    @Override public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context){
        if(!WorldgenConfig.trees())return false;
        var biome=context.level().getBiome(context.origin());
        float chance=0;
        for(int i=0;i<TYPES.length;i++){
            if(biome.is(TagKey.create(Registries.BIOME,ResourceLocation.fromNamespaceAndPath("tc4modern","greatwood/"+TYPES[i])))){
                chance=CHANCES[i];break;
            }
        }
        return chance>context.random().nextFloat()&&TC4Modern.GREATWOOD_FEATURE.get().grow(context,true);
    }
}
