package dev.eugheist.tc4modern;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
/** Original climate and colours; TerraBlender supplies modern climate placement. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public final class MagicalForestContent {
 public static final ResourceKey<Biome> BIOME=ResourceKey.create(Registries.BIOME,AlchemyContent.id("magical_forest"));
 @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent e){e.enqueueWork(()->{if(WorldgenConfig.forestWeight()>0)terrablender.api.Regions.register(new ForestRegion());});}
 private static final class ForestRegion extends terrablender.api.Region {
  ForestRegion(){super(AlchemyContent.id("magical_forest"),terrablender.api.RegionType.OVERWORLD,WorldgenConfig.forestWeight());}
  @Override public void addBiomes(net.minecraft.core.Registry<Biome> registry,java.util.function.Consumer<com.mojang.datafixers.util.Pair<Climate.ParameterPoint,ResourceKey<Biome>>> mapper){addModifiedVanillaOverworldBiomes(mapper,builder->builder.replaceBiome(net.minecraft.world.level.biome.Biomes.FOREST,BIOME));}
 }
 @SubscribeEvent public static void data(net.neoforged.neoforge.data.event.GatherDataEvent e){var builder=new net.minecraft.core.RegistrySetBuilder().add(Registries.BIOME,MagicalForestContent::bootstrap);e.getGenerator().addProvider(e.includeServer(),new net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider(e.getGenerator().getPackOutput(),e.getLookupProvider(),builder,java.util.Set.of(TC4Modern.MODID)));}
 private static void bootstrap(net.minecraft.data.worldgen.BootstrapContext<Biome> context){var base=net.minecraft.data.worldgen.biome.OverworldBiomes.forest(context.lookup(Registries.PLACED_FEATURE),context.lookup(Registries.CONFIGURED_CARVER),false,false,false);var effects=new BiomeSpecialEffects.Builder().fogColor(12638463).skyColor(base.getSpecialEffects().getSkyColor()).waterColor(30702).waterFogColor(329011).grassColorOverride(5635969).foliageColorOverride(6750149).ambientMoodSound(AmbientMoodSettings.LEGACY_CAVE_SETTINGS).build();var mobs=new MobSpawnSettings.Builder();net.minecraft.data.worldgen.BiomeDefaultFeatures.farmAnimals(mobs);net.minecraft.data.worldgen.BiomeDefaultFeatures.commonSpawns(mobs);mobs.addSpawn(net.minecraft.world.entity.MobCategory.CREATURE,new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityType.WOLF,2,1,3));mobs.addSpawn(net.minecraft.world.entity.MobCategory.CREATURE,new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityType.HORSE,2,1,3));mobs.addSpawn(net.minecraft.world.entity.MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityType.WITCH,3,1,1));mobs.addSpawn(net.minecraft.world.entity.MobCategory.MONSTER,new MobSpawnSettings.SpawnerData(net.minecraft.world.entity.EntityType.ENDERMAN,3,1,1));context.register(BIOME,new Biome.BiomeBuilder().hasPrecipitation(true).temperature(.7f).downfall(.6f).specialEffects(effects).mobSpawnSettings(mobs.build()).generationSettings(base.getGenerationSettings()).build());}
}
