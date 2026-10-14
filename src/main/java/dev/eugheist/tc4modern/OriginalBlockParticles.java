package dev.eugheist.tc4modern;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Break, hit, running and landing particles use the sprite the original block returned from getIcon(side, meta)
 * (EntityDiggingFX), not whatever the modern display model happens to carry. Also ports BlockJar's empty
 * addDestroyEffects and the BlockCustomOre / BlockAiry infusedStoneSparkle hit effects.
 */
@EventBusSubscriber(modid = TC4Modern.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class OriginalBlockParticles {
    private OriginalBlockParticles() {}
    static final Map<String, Function<BlockState, String>> SPRITES = new HashMap<>();
    static void fixed(String texture, String... blocks) { for (var b : blocks) SPRITES.put(b, s -> texture); }
    static {
        // BlockTable: every table metadata returns woodplain.
        fixed("woodplain", "table", "research_table", "arcane_workbench", "deconstructor");
        // BlockMetalDevice: metadata 0, 2, 5, 14 -> metalbase (grate's world icon is not used by digging particles).
        fixed("metalbase", "crucible", "vis_charge_relay", "grate", "vis_relay");
        fixed("pipe_1", "centrifuge", "essentia_crystalizer");
        fixed("alchemyblock", "alchemical_construct", "thaumatorium");
        fixed("brainbox", "mnemonic_matrix");
        fixed("lamp_grow_side", "lamp_growth");
        fixed("lamp_fert_side", "lamp_fertility");
        // BlockStoneDevice: unlisted metadata fall back to pedestal_top.
        fixed("pedestal_top", "runic_matrix", "infusion_pillar", "infusion_pillar_top", "node_stabilizer", "node_stabilizer_advanced", "node_converter", "focal_manipulator", "flux_scrubber");
        fixed("al_furnace_front_off", "alchemical_furnace");
        // BlockAiry and BlockWarded: the transparent blank icon.
        fixed("blank", "aura_node", "energized_node", "warded");
        fixed("mirrorframe", "mirror");
        fixed("mirrorframe2", "mirror_essentia");
        fixed("obsidiantile", "obsidian_totem", "charged_obsidian_totem");
        fixed("warded_glass_1", "warded_glass");
        fixed("jar_side", "node_jar");
        // Original quirk: BlockWoodenDevice metadata 4, 5 and 8 fall through to the arcane ear side icon.
        fixed("arcaneearsideon", "arcane_bore_base", "arcane_bore", "crimson_banner");
        for (var a : new String[]{"air", "fire", "water", "earth", "order", "entropy", "mixed"}) fixed("crystal", a + "_crystal_cluster");
        SPRITES.put("taint", s -> switch (s.getValue(TaintBlock.VARIANT)) { case 1 -> "taint_soil"; case 2 -> "fleshblock"; default -> "taint_crust"; });
        SPRITES.put("taint_fibres", s -> switch (s.getValue(TaintFibresBlock.VARIANT)) { case 1 -> "taintgrass1"; case 2 -> "taintgrass2"; case 3 -> "taint_spore_stalk_1"; case 4 -> "taint_spore_stalk_2"; default -> "taint_fibres"; });
        SPRITES.put("mana_pod", s -> switch (s.getValue(ManaPodBlock.AGE)) { case 0 -> "manapod_stem_0"; case 1 -> "manapod_stem_1"; default -> "manapod_stem_2"; });
    }
    static int wrapped;

    @SubscribeEvent static void bake(ModelEvent.ModifyBakingResult event) {
        var models = event.getModels();
        for (var entry : SPRITES.entrySet()) {
            var block = BuiltInRegistries.BLOCK.get(AlchemyContent.id(entry.getKey()));
            for (var state : block.getStateDefinition().getPossibleStates()) {
                var location = BlockModelShaper.stateToModelLocation(state);
                var model = models.get(location);
                if (model == null) continue;
                var sprite = event.getTextureGetter().apply(new Material(TextureAtlas.LOCATION_BLOCKS, AlchemyContent.id("blocks/" + entry.getValue().apply(state))));
                models.put(location, new Particle(model, sprite));
                wrapped++;
            }
        }
    }
    static final class Particle extends BakedModelWrapper<BakedModel> {
        final TextureAtlasSprite sprite;
        Particle(BakedModel model, TextureAtlasSprite sprite) { super(model); this.sprite = sprite; }
        @Override public TextureAtlasSprite getParticleIcon() { return sprite; }
        @Override public TextureAtlasSprite getParticleIcon(ModelData data) { return sprite; }
    }

    @SubscribeEvent static void extensions(RegisterClientExtensionsEvent event) {
        // BlockJar.addDestroyEffects returns true (meta != 15): jars break without digging particles.
        event.registerBlock(new IClientBlockExtensions() {
            @Override public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) { return true; }
        }, AlchemyMachines.VOID_JAR.get(), EssentiaDevices.JAR.get(), BrainJarContent.BLOCK.get(), BasicsContent.NODE_JAR.get());
        // BlockCustomOre.addHitEffects: metadata 1..5 sparkle (entropy, metadata 6, is excluded by the original md < 6 check).
        int[] colours = {1, 4, 2, 3, 6};
        for (int i = 0; i < 5; i++) {
            int colour = colours[i];
            event.registerBlock(new IClientBlockExtensions() {
                @Override public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
                    if (target instanceof BlockHitResult hit) sparkle(level, hit.getBlockPos(), colour);
                    return false;
                }
            }, TC4Modern.SHARD_ORES.get(i).get());
        }
        // BlockAiry.addHitEffects: aura nodes sparkle (colour 0) on half of the hits.
        event.registerBlock(new IClientBlockExtensions() {
            @Override public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
                if (target instanceof BlockHitResult hit && level.random.nextBoolean()) sparkle(level, hit.getBlockPos(), 0);
                return false;
            }
        }, TC4Modern.AURA_NODE.get(), TC4Modern.ENERGIZED_NODE.get());
    }
    /** UtilsFX.infusedStoneSparkle: particleCount(3) FXSparkle(size 1.75, multiplier 3..5, gravity 0.1) inside the block. */
    static void sparkle(Level level, BlockPos pos, int colour) {
        if (!(level instanceof ClientLevel client)) return;
        var r = level.random;
        for (int a = 0; a < InfusionFx.particleCount(3); a++)
            Minecraft.getInstance().particleEngine.add(new FociFx.Sparkle(client, pos.getX() + r.nextFloat(), pos.getY() + r.nextFloat(), pos.getZ() + r.nextFloat(), 1.75f, colour, 3 + r.nextInt(3)).gravity(.1f));
    }
}
