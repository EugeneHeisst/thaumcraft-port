package dev.eugheist.tc4modern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;

/** NeoForge spawn-list adapter for original ConfigEntities.initEntitySpawns switches and Halloween. */
@EventBusSubscriber(modid=TC4Modern.MODID,bus=EventBusSubscriber.Bus.MOD)
public record ConfiguredSpawnModifier(HolderSet<Biome> biomes,MobSpawnSettings.SpawnerData spawners,String setting,boolean halloween) implements BiomeModifier {
    public static final MapCodec<ConfiguredSpawnModifier> CODEC=RecordCodecBuilder.mapCodec(instance->instance.group(
        Biome.LIST_CODEC.fieldOf("biomes").forGetter(ConfiguredSpawnModifier::biomes),
        MobSpawnSettings.SpawnerData.CODEC.fieldOf("spawners").forGetter(ConfiguredSpawnModifier::spawners),
        Codec.STRING.fieldOf("setting").forGetter(ConfiguredSpawnModifier::setting),
        Codec.BOOL.optionalFieldOf("halloween",false).forGetter(ConfiguredSpawnModifier::halloween)
    ).apply(instance,ConfiguredSpawnModifier::new));
    static boolean halloween(java.time.LocalDate date){return date.getMonthValue()==10&&date.getDayOfMonth()==31;}
    @Override public void modify(Holder<Biome> biome,Phase phase,ModifiableBiomeInfo.BiomeInfo.Builder builder){
        if(phase==Phase.ADD&&biomes.contains(biome)&&SpawnConfig.enabled(setting)&&(!halloween||halloween(java.time.LocalDate.now())))builder.getMobSpawnSettings().addSpawn(spawners.type.getCategory(),spawners);
    }
    @Override public MapCodec<? extends BiomeModifier> codec(){return CODEC;}
    @SubscribeEvent static void register(net.neoforged.neoforge.registries.RegisterEvent event){event.register(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,h->h.register(AlchemyContent.id("configured_spawns"),CODEC));}
}
