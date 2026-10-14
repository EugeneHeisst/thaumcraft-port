#!/usr/bin/env python3
"""Prepare the local resource archive from the user's TC4 JAR for embedding at build time."""
import argparse
import json
import io
from PIL import Image
from pathlib import Path
import zipfile
from convert_tc4_model import convert

parser = argparse.ArgumentParser()
parser.add_argument('jar', type=Path)
parser.add_argument('game_directory', type=Path)
parser.add_argument('--reference', type=Path, default=Path(__file__).resolve().parents[2] / 'thaumcraft4-reference')
parser.add_argument('--baubles-jar', type=Path, default=Path(__file__).resolve().parents[1] / 'Baubles-1.7.10-1.0.1.10.jar', help='Original Baubles 1.7.10 JAR for the expanded inventory and Miner\'s Ring')
parser.add_argument('--minecraft-jar', type=Path, help='Original Minecraft 1.7.10 client JAR for the crucible water texture')
args = parser.parse_args()
args.game_directory.mkdir(parents=True, exist_ok=True)
out = args.game_directory / 'tc4-local-resources.zip'
staging = out.with_suffix('.zip.tmp')
with zipfile.ZipFile(args.jar) as source, zipfile.ZipFile(staging, 'w', zipfile.ZIP_DEFLATED) as pack:
    pack.writestr('pack.mcmeta', json.dumps({'pack': {'pack_format': 34, 'description': 'Local TC4 resources imported from your original JAR'}}))
    pack.writestr('assets/minecraft/atlases/blocks.json', json.dumps({'sources': [{'type': 'minecraft:directory', 'source': 'blocks', 'prefix': 'blocks/'}, {'type': 'minecraft:directory', 'source': 'models', 'prefix': 'models/'}, {'type': 'minecraft:directory', 'source': 'items', 'prefix': 'items/'}]}))
    baubles_lang = {}
    if args.baubles_jar.exists():
        with zipfile.ZipFile(args.baubles_jar) as baubles:
            pack.writestr('assets/tc4modern/textures/gui/baubles_expanded_inventory.png', baubles.read('assets/baubles/textures/gui/expanded_inventory.png'))
            pack.writestr('assets/tc4modern/textures/item/miners_ring.png', baubles.read('assets/baubles/textures/items/ring.png'))
            pack.writestr('assets/tc4modern/models/item/miners_ring.json', json.dumps({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'tc4modern:item/miners_ring'}}))
            for name in baubles.namelist():
                if name.startswith('assets/baubles/lang/') and name.endswith('.lang'):
                    entries = dict(line.split('=', 1) for line in baubles.read(name).decode('utf-8-sig').splitlines() if '=' in line and not line.startswith('#'))
                    modern = {'item.tc4modern.miners_ring': entries.get('item.Ring.0.name'), 'button.tc4modern.baubles': entries.get('button.baubles'), 'button.tc4modern.normal': entries.get('button.normal')}
                    baubles_lang[Path(name).stem.lower()] = {k: v.strip() for k, v in modern.items() if v}
    minecraft_jar = args.minecraft_jar or args.reference / 'minecraft-1.7.10-client.jar'
    if minecraft_jar.exists():
        with zipfile.ZipFile(minecraft_jar) as minecraft:
            # The research book uses the original 1.7.10 Unicode glyphs and metrics.
            pack.writestr('assets/tc4modern/font/glyph_sizes.bin', minecraft.read('assets/minecraft/font/glyph_sizes.bin'))
            for name in minecraft.namelist():
                if name.startswith('assets/minecraft/textures/font/unicode_page_') and name.endswith('.png'):
                    pack.writestr(name.replace('assets/minecraft/', 'assets/tc4modern/'), minecraft.read(name))
            pack.writestr('assets/tc4modern/textures/misc/legacy_spell_particles.png', minecraft.read('assets/minecraft/textures/particle/particles.png'))
            for suffix in ('.png', '.png.mcmeta'):
                pack.writestr('assets/tc4modern/textures/blocks/crucible_water' + suffix,
                    minecraft.read('assets/minecraft/textures/blocks/water_still' + suffix))
            for texture in ('lava_still','fire_layer_0'):
                for suffix in ('.png','.png.mcmeta'):
                    name='assets/minecraft/textures/blocks/'+texture+suffix
                    if name in minecraft.namelist():
                        pack.writestr('assets/tc4modern/textures/blocks/original_'+texture+suffix,minecraft.read(name))
            pack.writestr('assets/tc4modern/textures/misc/original_slimeball.png',minecraft.read('assets/minecraft/textures/items/slimeball.png'))
    sound_definitions = json.loads(source.read('assets/thaumcraft/sounds.json'))
    # Modern ResourceLocation rejects legacy camelCase event and file names.
    # Rename paths together; the original OGG bytes stay unchanged.
    sound_definitions = {name.lower(): event for name, event in sound_definitions.items()}
    for event in sound_definitions.values():
        event.pop('category', None)
        for index, sound in enumerate(event.get('sounds', [])):
            if isinstance(sound, str):
                event['sounds'][index] = 'tc4modern:' + sound.split(':')[-1].lower()
            else:
                sound['name'] = 'tc4modern:' + sound['name'].split(':')[-1].lower()
    pack.writestr('assets/tc4modern/sounds.json', json.dumps(sound_definitions))
    pack.writestr('assets/tc4modern/textures/models/hoverharness.obj',source.read('assets/thaumcraft/textures/models/hoverharness.obj'))
    pack.writestr('assets/tc4modern/textures/models/hemis.obj',source.read('assets/thaumcraft/textures/models/hemis.obj'))
    pack.writestr('assets/tc4modern/models/node_stabilizer.obj', source.read('assets/thaumcraft/textures/models/node_stabilizer.obj'))
    pack.writestr('assets/tc4modern/models/vis_relay.obj', source.read('assets/thaumcraft/textures/models/vis_relay.obj'))
    for name in source.namelist():
        if name.startswith('assets/thaumcraft/sounds/') and name.endswith('.ogg'):
            pack.writestr(name.replace('assets/thaumcraft/', 'assets/tc4modern/').lower(), source.read(name))
    # Original inventory GUI draws Potion.setIconIndex at y=198 + row*18.
    potion_sheet = Image.open(io.BytesIO(source.read('assets/thaumcraft/textures/misc/potions.png')))
    for effect, (column, row) in {
        'vis_exhaust': (5, 1),
        'flux_taint': (3, 1),
        'infectious_vis_exhaust': (6, 1),
        'unnatural_hunger': (7, 1),
        'warp_ward': (3, 2),
        'death_gaze': (4, 2),
        'blurred_vision': (5, 2),
        'sun_scorned': (6, 2),
        'thaumarhia': (7, 2),
    }.items():
        icon = io.BytesIO()
        potion_sheet.crop((column * 18, 198 + row * 18, (column + 1) * 18, 198 + (row + 1) * 18)).save(icon, format='PNG')
        pack.writestr(f'assets/tc4modern/textures/mob_effect/{effect}.png', icon.getvalue())
    for name in source.namelist():
        if name.startswith('assets/thaumcraft/textures/') and name.endswith(('.png', '.mcmeta')):
            pack.writestr(name.replace('assets/thaumcraft/', 'assets/tc4modern/').lower(), source.read(name))
    for name in source.namelist():
        if name.startswith('assets/thaumcraft/lang/') and name.endswith('.lang'):
            translations = {}
            for line in source.read(name).decode('utf-8-sig').splitlines():
                if line.strip() and not line.lstrip().startswith('#') and '=' in line:
                    key, value = line.split('=', 1)
                    translations[key.strip()] = value.strip()
            for modern, original in {
                'block.tc4modern.deconstructor': 'tile.blockTable.14.name',
                'block.tc4modern.cinderpearl': 'tile.blockCustomPlant.3.name',
                'item.tc4modern.mana_bean': 'item.ItemManaBean.name',
                'block.tc4modern.node_jar': 'tile.blockJar.2.name',
                'item.tc4modern.knowledge_fragment': 'item.ItemResource.9.name',
                'item.tc4modern.eldritch_eye': 'item.ItemEldritchObject.0.name',
                'item.tc4modern.runed_tablet': 'item.ItemEldritchObject.2.name',
                'item.tc4modern.obelisk_placer': 'item.ItemEldritchObject.4.name',
                'block.tc4modern.loot_urn_common': 'tile.blockLootUrn.name',
                'block.tc4modern.loot_urn_uncommon': 'tile.blockLootUrn.name',
                'block.tc4modern.loot_urn_rare': 'tile.blockLootUrn.name',
                'block.tc4modern.loot_crate_common': 'tile.blockLootCrate.name',
                'block.tc4modern.loot_crate_uncommon': 'tile.blockLootCrate.name',
                'block.tc4modern.loot_crate_rare': 'tile.blockLootCrate.name',
                'block.tc4modern.ancient_stone': 'tile.blockCosmeticSolid.11.name',
                'block.tc4modern.ancient_rock': 'tile.blockCosmeticSolid.12.name',
                'block.tc4modern.ancient_stone_no_spawn': 'tile.blockCosmeticSolid.13.name',
                'block.tc4modern.crusted_stone': 'tile.blockCosmeticSolid.14.name',
                'block.tc4modern.ancient_pedestal': 'tile.blockCosmeticSolid.15.name',
                'block.tc4modern.glowing_crusted_stone': 'tile.blockEldritch.4.name',
                'block.tc4modern.runed_stone': 'tile.blockEldritch.10.name',
                'block.tc4modern.crusted_opening': 'tile.blockEldritch.9.name',
                'block.tc4modern.glyphed_stone': 'tile.blockEldritch.5.name',
                'block.tc4modern.ancient_stone_stairs': 'tile.blockStairsEldritch.name',
                'entity.tc4modern.brainy_zombie': 'entity.Thaumcraft.BrainyZombie.name',
                'entity.tc4modern.giant_brainy_zombie': 'entity.Thaumcraft.GiantBrainyZombie.name',
                'entity.tc4modern.eldritch_guardian': 'entity.Thaumcraft.EldritchGuardian.name',
                'entity.tc4modern.eldritch_golem': 'entity.Thaumcraft.EldritchGolem.name',
                'entity.tc4modern.eldritch_warden': 'entity.Thaumcraft.EldritchWarden.name',
                'item.tc4modern.crimson_rites': 'item.ItemEldritchObject.1.name',
                **{'item.tc4modern.cultist_'+m+'_'+t: 'item.Item'+o+'Cultist'+n+'.name' for m,n in (('robe','Robe'),('plate','Plate')) for t,o in (('helmet','Helmet'),('chestplate','Chestplate'),('leggings','Leggings'))},
                'item.tc4modern.cultist_boots': 'item.ItemBootsCultist.name',
                **{'item.tc4modern.cultist_leader_'+t: 'item.Item'+o+'CultistLeaderPlate.name' for t,o in (('helmet','Helmet'),('chestplate','Chestplate'),('leggings','Leggings'))},
                'item.tc4modern.crimson_blade': 'item.ItemSwordCrimson.name',
                'entity.tc4modern.cultist_leader': 'entity.Thaumcraft.CultistLeader.name',
                'entity.tc4modern.cultist_portal': 'entity.Thaumcraft.CultistPortal.name',
                'entity.tc4modern.explosive_orb': 'entity.Thaumcraft.ExplosiveOrb.name',
                'entity.tc4modern.shock_orb': 'entity.Thaumcraft.ShockOrb.name',
                'entity.tc4modern.cultist_knight': 'entity.Thaumcraft.CultistKnight.name',
                'entity.tc4modern.cultist_cleric': 'entity.Thaumcraft.CultistCleric.name',
                'block.tc4modern.eldritch_altar': 'tile.blockEldritch.0.name',
                'block.tc4modern.eldritch_obelisk': 'tile.blockEldritch.1.name',
                'block.tc4modern.eldritch_obelisk_body': 'tile.blockEldritch.2.name',
                'block.tc4modern.eldritch_cap': 'tile.blockEldritch.3.name',
                'block.tc4modern.crimson_banner': 'tile.blockWoodenDevice.8.name',
                'block.tc4modern.obsidian_tile': 'tile.blockCosmeticSolid.1.name',
                'block.tc4modern.obsidian_totem': 'tile.blockCosmeticSolid.0.name',
                'block.tc4modern.charged_obsidian_totem': 'tile.blockCosmeticSolid.8.name',
                **{'block.tc4modern.'+name+'_crystal_cluster': 'tile.blockCrystal.'+str(i)+'.name' for i,name in enumerate(('air','fire','water','earth','order','entropy','mixed'))},
                'item.tc4modern.tainted_goo': 'item.ItemResource.11.name',
                'item.tc4modern.tainted_tendril': 'item.ItemResource.12.name',
                'entity.tc4modern.taint_spore': 'entity.Thaumcraft.TaintSpore.name',
                'entity.tc4modern.taint_swarmer': 'entity.Thaumcraft.TaintSwarmer.name',
                'entity.tc4modern.taint_swarm': 'entity.Thaumcraft.TaintSwarm.name',
                'entity.tc4modern.falling_taint': 'entity.Thaumcraft.FallingTaint.name',
                'entity.tc4modern.taint_spider': 'entity.Thaumcraft.TaintSpider.name',
                'entity.tc4modern.thaumic_slime': 'entity.Thaumcraft.ThaumSlime.name',
                'entity.tc4modern.alumentum': 'entity.Thaumcraft.Alumentum.name',
                'item.tc4modern.chicken_nugget': 'item.ItemNuggetChicken.name',
                'item.tc4modern.beef_nugget': 'item.ItemNuggetBeef.name',
                'item.tc4modern.pork_nugget': 'item.ItemNuggetPork.name',
                'item.tc4modern.fish_nugget': 'item.ItemNuggetFish.name',
                'item.tc4modern.triple_meat_treat': 'item.TripleMeatTreat.name',
                'item.tc4modern.alumentum': 'item.ItemResource.0.name',
                'block.tc4modern.nitor': 'tile.blockAiry.1.name',
                'block.tc4modern.aura_node': 'tile.blockAiry.0.name',
                'block.tc4modern.energized_node': 'tile.blockAiry.5.name',
                'block.tc4modern.flux_gas': 'tile.blockFluxGas.name',
                'block.tc4modern.flux_goo': 'tile.blockFluxGoo.name',
                'block.tc4modern.greatwood_leaves': 'tile.blockMagicalLeaves.greatwood.name',
                'block.tc4modern.greatwood_sapling': 'tile.blockCustomPlant.0.name',
                'block.tc4modern.infernal_furnace': 'tile.blockArcaneFurnace.name',
                'block.tc4modern.taint': 'tile.blockTaint.0.name',
                'block.tc4modern.taint_fibres': 'tile.blockTaintFibres.0.name',
                'item.tc4modern.tallow': 'item.ItemResource.4.name',
                'block.tc4modern.cinnabar_ore': 'tile.blockCustomOre.0.name',
                'block.tc4modern.amber_ore': 'tile.blockCustomOre.7.name',
                'block.tc4modern.greatwood_planks': 'tile.blockWoodenDevice.6.name',
                'block.tc4modern.greatwood_stairs': 'tile.blockStairsGreatwood.name',
                'block.tc4modern.silverwood_stairs': 'tile.blockStairsSilverwood.name',
                'block.tc4modern.greatwood_slab': 'tile.blockCosmeticSlabWood.greatwood.name',
                'block.tc4modern.silverwood_slab': 'tile.blockCosmeticSlabWood.silverwood.name',
                'block.tc4modern.silverwood_planks': 'tile.blockWoodenDevice.7.name',
                'block.tc4modern.silverwood_log': 'tile.blockMagicalLog.silverwood.name',
                'block.tc4modern.silverwood_leaves': 'tile.blockMagicalLeaves.silverwood.name',
                'block.tc4modern.silverwood_sapling': 'tile.blockCustomPlant.1.name',
                'item.tc4modern.quicksilver': 'item.ItemResource.3.name',
                'item.tc4modern.amber': 'item.ItemResource.6.name',
                'item.tc4modern.quicksilver_drop': 'item.ItemNugget.5.name',
                'item.tc4modern.vis_filter': 'item.ItemResource.8.name',
                'item.tc4modern.jar_label': 'item.ItemResource.13.name',
                'item.tc4modern.phial': 'item.ItemEssence.0.name',
                'item.tc4modern.essentia_phial': 'item.ItemEssence.1.name',
                'block.tc4modern.alchemical_furnace': 'tile.blockStoneDevice.0.name',
                'container.tc4modern.alchemical_furnace': 'container.alchemyfurnace',
                'block.tc4modern.alembic': 'tile.blockMetalDevice.1.name',
                'block.tc4modern.warded_jar': 'tile.blockJar.0.name',
                'block.tc4modern.brain_jar': 'tile.blockJar.1.name',
                'item.tc4modern.zombie_brain': 'item.ItemZombieBrain.name',
                'block.tc4modern.essentia_tube': 'tile.blockTube.0.name',
                'block.tc4modern.essentia_valve': 'tile.blockTube.1.name',
                'block.tc4modern.essentia_tube_filter': 'tile.blockTube.3.name',
                'block.tc4modern.essentia_buffer': 'tile.blockTube.4.name',
                'block.tc4modern.essentia_tube_restricted': 'tile.blockTube.5.name',
                'block.tc4modern.essentia_tube_oneway': 'tile.blockTube.6.name',
                'block.tc4modern.essentia_valve': 'tile.blockTube.1.name',
                'block.tc4modern.arcane_pedestal': 'tile.blockStoneDevice.1.name',
                'block.tc4modern.runic_matrix': 'tile.blockStoneDevice.2.name',
                'block.tc4modern.infusion_pillar': 'tile.blockStoneDevice.3.name',
                'block.tc4modern.infusion_pillar_top': 'tile.blockStoneDevice.4.name',
                'item.tc4modern.salis_mundus': 'item.ItemResource.14.name',
                'block.tc4modern.thaumium_block': 'tile.blockCosmeticSolid.4.name',
                'item.tc4modern.thaumium_nugget': 'item.ItemNugget.6.name',
                'item.tc4modern.thaumium_helmet': 'item.ItemHelmetThaumium.name',
                'item.tc4modern.thaumium_chestplate': 'item.ItemChestplateThaumium.name',
                'item.tc4modern.thaumium_leggings': 'item.ItemLeggingsThaumium.name',
                'item.tc4modern.thaumium_boots': 'item.ItemBootsThaumium.name',
                'item.tc4modern.thaumium_shovel': 'item.ItemShovelThaumium.name',
                'item.tc4modern.thaumium_axe': 'item.ItemAxeThaumium.name',
                'item.tc4modern.thaumium_sword': 'item.ItemSwordThaumium.name',
                'item.tc4modern.thaumium_pickaxe': 'item.ItemPickThaumium.name',
                'item.tc4modern.thaumium_hoe': 'item.ItemHoeThaumium.name',
                'item.tc4modern.focus_fire': 'item.FocusFire.name',
                'item.tc4modern.focus_primal': 'item.FocusPrimal.name',
                'item.tc4modern.bucket_death': 'item.ItemBucketDeath.name',
                'item.tc4modern.bottle_taint': 'item.ItemBottleTaint.name',
                'item.tc4modern.crystal_essence': 'item.ItemCrystalEssence.name',
                'item.tc4modern.focus_warding': 'item.FocusWarding.name',
                'item.tc4modern.focus_hellbat': 'item.FocusHellbat.name',
                'entity.tc4modern.firebat': 'entity.Thaumcraft.Firebat.name',
                'block.tc4modern.wand_pedestal': 'tile.blockStoneDevice.5.name',
                'block.tc4modern.focal_manipulator': 'tile.blockStoneDevice.13.name',
                'block.tc4modern.electric_spark': 'tile.blockAiry.10.name',
                'block.tc4modern.wand_pedestal_focus': 'tile.blockStoneDevice.8.name',
                'item.tc4modern.primal_charm': 'item.ItemResource.15.name',
                'item.tc4modern.mundane_amulet': 'item.ItemBaubleBlanks.0.name',
                'item.tc4modern.mundane_ring': 'item.ItemBaubleBlanks.1.name',
                'item.tc4modern.mundane_belt': 'item.ItemBaubleBlanks.2.name',
                'item.tc4modern.vis_amulet': 'item.ItemAmuletVis.1.name',
                'item.tc4modern.vis_amulet_lesser': 'item.ItemAmuletVis.0.name',
                'item.tc4modern.focus_excavation': 'item.FocusExcavation.name',
                'item.tc4modern.focus_trade': 'item.FocusTrade.name',
                'item.tc4modern.focus_portable_hole': 'item.FocusPortableHole.name',
                'item.tc4modern.copper_cap': 'item.WandCap.3.name',
                'item.tc4modern.thaumium_cap': 'item.WandCap.2.name',
                'item.tc4modern.void_cap': 'item.WandCap.7.name',
                'item.tc4modern.eldritch_eye': 'item.ItemEldritchObject.0.name',
                'item.tc4modern.runed_tablet': 'item.ItemEldritchObject.2.name',
                'item.tc4modern.obelisk_placer': 'item.ItemEldritchObject.4.name',
                'block.tc4modern.essentia_reservoir': 'tile.blockEssentiaReservoir.name',
                'item.tc4modern.primal_crusher': 'item.ItemPrimalCrusher.name',
                'item.tc4modern.void_robe_helm': 'item.ItemHelmetVoidRobe.name',
                'item.tc4modern.void_robe_chest': 'item.ItemChestplateVoidRobe.name',
                'item.tc4modern.void_robe_legs': 'item.ItemLeggingsVoidRobe.name',
                'item.tc4modern.void_cap_inert': 'item.WandCap.8.name',
                'item.tc4modern.void_nugget': 'item.ItemNugget.7.name',
                'item.tc4modern.thaumium_cap_inert': 'item.WandCap.6.name',
                'item.tc4modern.copper_nugget': 'item.ItemNugget.1.name',
                'item.tc4modern.obsidian_rod': 'item.WandRod.1.name',
                'item.tc4modern.silverwood_rod': 'item.WandRod.2.name',
                'item.tc4modern.ice_rod': 'item.WandRod.3.name',
                'item.tc4modern.quartz_rod': 'item.WandRod.4.name',
                'item.tc4modern.reed_rod': 'item.WandRod.5.name',
                'item.tc4modern.blaze_rod': 'item.WandRod.6.name',
                'item.tc4modern.bone_rod': 'item.WandRod.7.name',
                'item.tc4modern.wand': 'item.Wand.wand.obj',
                'item.tc4modern.focus_frost': 'item.FocusFrost.name',
                'item.tc4modern.focus_shock': 'item.FocusShock.name',
                'entity.tc4modern.ember': 'item.FocusFire.name',
                'entity.tc4modern.frost_shard': 'item.FocusFrost.name',
                'item.tc4modern.bath_salts':'item.ItemBathSalts.name',
                'item.tc4modern.sanity_soap':'item.ItemSanitySoap.name',
                'item.tc4modern.bucket_pure':'item.ItemBucketPure.name',
                'block.tc4modern.purifying_fluid':'tile.blockFluidPure.name',
                'fluid_type.tc4modern.purifying_fluid':'tile.blockFluidPure.name',
                'block.tc4modern.arcane_spa':'tile.blockStoneDevice.12.name',
                'block.tc4modern.arcane_lamp':'tile.blockMetalDevice.7.name',
                'block.tc4modern.lamp_growth':'tile.blockMetalDevice.8.name',
                'block.tc4modern.lamp_fertility':'tile.blockMetalDevice.13.name',
                'block.tc4modern.amber_block':'tile.blockCosmeticOpaque.0.name',
                'block.tc4modern.amber_bricks':'tile.blockCosmeticOpaque.1.name',
                'block.tc4modern.mirror':'tile.blockMirror.0.name',
                'block.tc4modern.mirror_essentia':'tile.blockMirror.6.name',
                'item.tc4modern.hand_mirror':'item.HandMirror.name',
                'item.tc4modern.elemental_shovel':'item.ItemShovelElemental.name',
                'item.tc4modern.elemental_hoe':'item.ItemHoeElemental.name',
                'item.tc4modern.elemental_sword':'item.ItemSwordElemental.name',
                'item.tc4modern.elemental_axe':'item.ItemAxeElemental.name',
                'item.tc4modern.elemental_pickaxe':'item.ItemPickaxeElemental.name',
                'item.tc4modern.bone_bow':'item.ItemBowBone.name',
                'item.tc4modern.air_arrow':'item.ItemPrimalArrow.0.name',
                'item.tc4modern.fire_arrow':'item.ItemPrimalArrow.1.name',
                'item.tc4modern.water_arrow':'item.ItemPrimalArrow.2.name',
                'item.tc4modern.earth_arrow':'item.ItemPrimalArrow.3.name',
                'item.tc4modern.order_arrow':'item.ItemPrimalArrow.4.name',
                'item.tc4modern.entropy_arrow':'item.ItemPrimalArrow.5.name',
                'item.tc4modern.hover_harness':'item.HoverHarness.name',
                'item.tc4modern.hover_girdle':'item.ItemGirdleHover.name',
                'item.tc4modern.fortress_helmet':'item.ItemHelmetFortress.name',
                'item.tc4modern.fortress_chestplate':'item.ItemChestplateFortress.name',
                'item.tc4modern.fortress_leggings':'item.ItemLeggingsFortress.name',
                'item.tc4modern.boots_traveller':'item.BootsTraveller.name',
                'item.tc4modern.runic_ring':'item.ItemRingRunic.1.name',
                'item.tc4modern.runic_ring_charged':'item.ItemRingRunic.2.name',
                'item.tc4modern.runic_ring_regen':'item.ItemRingRunic.3.name',
                'item.tc4modern.runic_amulet':'item.ItemAmuletRunic.0.name',
                'item.tc4modern.runic_amulet_emergency':'item.ItemAmuletRunic.1.name',
                'item.tc4modern.runic_girdle':'item.ItemGirdleRunic.0.name',
                'item.tc4modern.runic_girdle_kinetic':'item.ItemGirdleRunic.1.name',
                'item.tc4modern.sinister_stone':'item.ItemCompassStone.name',
                'item.tc4modern.mirror_glass':'item.ItemResource.10.name',
                'block.tc4modern.flux_scrubber':'tile.blockStoneDevice.14.name',
                'block.tc4modern.levitator':'tile.blockLifter.name',
                'block.tc4modern.arcane_door':'item.ItemArcaneDoor.name',
                'block.tc4modern.arcane_pressure_plate':'tile.blockWoodenDevice.2.name',
                'block.tc4modern.warded_glass':'tile.blockCosmeticOpaque.2.name',
                'item.tc4modern.iron_key':'item.ArcaneDoorKey.0.name',
                'item.tc4modern.gold_key':'item.ArcaneDoorKey.1.name',
                'block.tc4modern.arcane_bore':'tile.blockWoodenDevice.5.name',
                'block.tc4modern.arcane_bore_base':'tile.blockWoodenDevice.4.name',
                'block.tc4modern.arcane_ear':'tile.blockWoodenDevice.1.name',
                'block.tc4modern.grate':'tile.blockMetalDevice.5.name',
                'block.tc4modern.paving_stone_travel':'tile.blockCosmeticSolid.2.name',
                'block.tc4modern.paving_stone_warding':'tile.blockCosmeticSolid.3.name',
                'block.tc4modern.tallow_block':'tile.blockCosmeticSolid.5.name',
                'block.tc4modern.ethereal_bloom': 'tile.blockCustomPlant.4.name',
                'block.tc4modern.shimmerleaf': 'tile.blockCustomPlant.2.name',
                'block.tc4modern.silverwood_knot': 'tile.blockMagicalLog.silverwood.name',
                'item.tc4modern.thaumium_ingot': 'item.ItemResource.2.name',
                'item.tc4modern.magic_tallow': 'item.ItemResource.4.name',
                'item.tc4modern.balanced_shard': 'item.ItemShard.6.name',
                'block.tc4modern.crucible': 'tile.blockMetalDevice.0.name',
                'block.tc4modern.greatwood_log': 'tile.blockMagicalLog.greatwood.name',
                'item.tc4modern.greatwood_rod': 'item.WandRod.0.name',
                'item.tc4modern.greatwood_staff_rod': 'item.WandRod.50.name',
                'item.tc4modern.obsidian_staff_rod': 'item.WandRod.51.name',
                'item.tc4modern.silverwood_staff_rod': 'item.WandRod.52.name',
                'item.tc4modern.ice_staff_rod': 'item.WandRod.53.name',
                'item.tc4modern.quartz_staff_rod': 'item.WandRod.54.name',
                'item.tc4modern.reed_staff_rod': 'item.WandRod.55.name',
                'item.tc4modern.blaze_staff_rod': 'item.WandRod.56.name',
                'item.tc4modern.bone_staff_rod': 'item.WandRod.57.name',
                'item.tc4modern.primal_staff_rod': 'item.WandRod.100.name',

                'item.tc4modern.gold_cap': 'item.WandCap.1.name',
                'block.tc4modern.bellows': 'tile.blockWoodenDevice.0.name',
                'item.tc4modern.thaumometer': 'item.ItemThaumometer.name',
                'item.tc4modern.thaumonomicon': 'item.ItemThaumonomicon.name',
                'item.tc4modern.scribing_tools': 'item.ItemInkwell.name',
                'item.tc4modern.research_notes': 'item.researchnotes.name',
                'item.tc4modern.iron_cap': 'item.WandCap.0.name',
                'item.tc4modern.enchanted_fabric': 'item.ItemResource.7.name',
                'item.tc4modern.goggles': 'item.ItemGoggles.name',
                'item.tc4modern.robe_chest': 'item.ItemChestplateRobe.name',
                'item.tc4modern.robe_legs': 'item.ItemLeggingsRobe.name',
                'item.tc4modern.robe_boots': 'item.ItemBootsRobe.name',
                'block.tc4modern.research_table': 'tile.blockTable.research.name',
                'block.tc4modern.table': 'tile.blockTable.0.name',
                'block.tc4modern.arcane_workbench': 'tile.blockTable.15.name',
                'block.tc4modern.arcane_stone': 'tile.blockCosmeticSolid.6.name',
                'block.tc4modern.arcane_stone_bricks': 'tile.blockCosmeticSolid.7.name',
                'block.tc4modern.arcane_stone_stairs': 'tile.blockStairsArcaneStone.name',
                'block.tc4modern.arcane_stone_slab': 'tile.blockCosmeticSlabStone.arcane.name',
                'block.tc4modern.node_stabilizer': 'tile.blockStoneDevice.9.name',
                'block.tc4modern.node_stabilizer_advanced': 'tile.blockStoneDevice.10.name',
                'block.tc4modern.vis_charge_relay': 'tile.blockMetalDevice.2.name',
                'block.tc4modern.node_converter': 'tile.blockStoneDevice.11.name',
                'block.tc4modern.vis_relay': 'tile.blockMetalDevice.14.name',
                'item.tc4modern.air_shard': 'item.ItemShard.0.name',
                'block.tc4modern.air_infused_stone': 'tile.blockCustomOre.1.name',
                'item.tc4modern.fire_shard': 'item.ItemShard.1.name',
                'block.tc4modern.fire_infused_stone': 'tile.blockCustomOre.2.name',
                'item.tc4modern.water_shard': 'item.ItemShard.2.name',
                'block.tc4modern.water_infused_stone': 'tile.blockCustomOre.3.name',
                'item.tc4modern.earth_shard': 'item.ItemShard.3.name',
                'block.tc4modern.earth_infused_stone': 'tile.blockCustomOre.4.name',
                'item.tc4modern.order_shard': 'item.ItemShard.4.name',
                'block.tc4modern.order_infused_stone': 'tile.blockCustomOre.5.name',
                'item.tc4modern.entropy_shard': 'item.ItemShard.5.name',
                'block.tc4modern.entropy_infused_stone': 'tile.blockCustomOre.6.name',

            }.items():
                if original in translations:
                    translations[modern] = translations[original]
            if all(key in translations for key in ('item.Wand.name', 'item.Wand.gold.cap', 'item.Wand.greatwood.rod', 'item.Wand.wand.obj')):
                translations['item.tc4modern.gold_capped_greatwood_wand'] = translations['item.Wand.name'].replace('%CAP', translations['item.Wand.gold.cap']).replace('%ROD', translations['item.Wand.greatwood.rod']).replace('%OBJ', translations['item.Wand.wand.obj'])
            if all(key in translations for key in ('item.Wand.name', 'item.Wand.iron.cap', 'item.Wand.wood.rod', 'item.Wand.wand.obj')):
                translations['item.tc4modern.iron_capped_wooden_wand'] = translations['item.Wand.name'].replace('%CAP', translations['item.Wand.iron.cap']).replace('%ROD', translations['item.Wand.wood.rod']).replace('%OBJ', translations['item.Wand.wand.obj'])
            # Registered creative forms share the original names, including metadata variants.
            creative_manifest = Path(__file__).resolve().parents[1] / 'src/main/resources/creative-tab.json'
            for entry in json.loads(creative_manifest.read_text()):
                if entry['id'] != 'wand' and entry['original'] in translations:
                    translations[entry['translation']] = (translations.get('item.ItemGolemCore.name', '') + ': ' if entry['id'].startswith('golem_core_') else '') + translations[entry['original']]
            for color in range(16):
                key = f'tile.blockWoodenDevice.8.{color}.name'
                if key in translations:
                    translations[f'block.tc4modern.crimson_banner.color.{color}'] = translations[key]
            for entity, original in (('brainy_zombie', 'BrainyZombie'), ('giant_brainy_zombie', 'GiantBrainyZombie')):
                key = f'entity.Thaumcraft.{original}.name'
                if key in translations:
                    translations[f'item.tc4modern.{entity}_spawn_egg'] = ('Яйцо призыва: ' if name.lower().endswith('ru_ru.lang') else 'Spawn ') + translations[key]
            # KeyHandler registered "Change Wand Focus" without a language key.
            translations['key.tc4modern.hover_harness'] = 'Включить летающую упряжь' if name.lower().endswith('ru_ru.lang') else 'Activate Hover Harness'
            translations['key.tc4modern.misc_toggle'] = 'Дополнительное переключение' if name.lower().endswith('ru_ru.lang') else 'Misc Wand Toggle'
            translations['key.tc4modern.change_focus'] = 'Сменить фокус жезла' if name.lower().endswith('ru_ru.lang') else 'Change Wand Focus'
            if 'potion.visexhaust' in translations:
                translations['effect.tc4modern.vis_exhaust'] = translations['potion.visexhaust']
            if 'potion.fluxtaint' in translations:
                translations['effect.tc4modern.flux_taint'] = translations['potion.fluxtaint']
            translations.update(baubles_lang.get(Path(name).stem.lower(), {}))
            # Baubles KeyHandler registered the literal "Baubles Inventory".
            translations['key.tc4modern.baubles'] = 'Инвентарь Baubles' if name.lower().endswith('ru_ru.lang') else 'Baubles Inventory'
            pack.writestr('assets/tc4modern/lang/' + Path(name).stem.lower() + '.json', json.dumps(translations, ensure_ascii=False))
    pack.writestr('assets/tc4modern/textures/item/scanner.png', source.read('assets/thaumcraft/textures/models/scanner.png'))
    table_source = args.reference / 'decompiled/thaumcraft/client/renderers/models/ModelResearchTable.java'
    if table_source.exists():
        pack.writestr('assets/tc4modern/models/block/research_table.obj', convert(table_source.read_text(), 'tc4modern:models/restable'))
        pack.writestr('assets/tc4modern/models/block/research_table.mtl', 'newmtl table\nKd 1 1 1\nmap_Kd tc4modern:models/restable\n')
        pack.writestr('assets/tc4modern/models/block/research_table.json', json.dumps({'loader': 'neoforge:obj', 'model': 'tc4modern:models/block/research_table.obj', 'automatic_culling': False, 'flip_v': True, 'ambientocclusion': False, 'textures': {'particle': 'tc4modern:models/restable'}}))
    plain_table = args.reference / 'decompiled/thaumcraft/client/renderers/models/ModelTable.java'
    if plain_table.exists():
        pack.writestr('assets/tc4modern/models/block/table.obj', convert(plain_table.read_text(), 'tc4modern:models/table').replace('research_table.mtl', 'table.mtl'))
        pack.writestr('assets/tc4modern/models/block/table.mtl', 'newmtl table\nKd 1 1 1\nmap_Kd tc4modern:models/table\n')
        pack.writestr('assets/tc4modern/models/block/table.json', json.dumps({'loader': 'neoforge:obj', 'model': 'tc4modern:models/block/table.obj', 'automatic_culling': False, 'flip_v': True, 'ambientocclusion': False, 'textures': {'particle': 'tc4modern:models/table'}}))
    arcane = args.reference / 'decompiled/thaumcraft/client/renderers/models/ModelArcaneWorkbench.java'
    if arcane.exists():
        pack.writestr('assets/tc4modern/models/block/arcane_workbench.obj', convert(arcane.read_text(), 'tc4modern:models/worktable').replace('research_table.mtl', 'arcane_workbench.mtl'))
        pack.writestr('assets/tc4modern/models/block/arcane_workbench.mtl', 'newmtl table\nKd 1 1 1\nmap_Kd tc4modern:models/worktable\n')
        pack.writestr('assets/tc4modern/models/block/arcane_workbench.json', json.dumps({'loader': 'neoforge:obj', 'model': 'tc4modern:models/block/arcane_workbench.obj', 'automatic_culling': False, 'flip_v': True, 'ambientocclusion': False, 'textures': {'particle': 'tc4modern:models/worktable'}}))
        # TileFocalManipulatorRenderer draws the same ModelArcaneWorkbench with wandtable.png.
        pack.writestr('assets/tc4modern/models/block/focal_manipulator.obj', convert(arcane.read_text(), 'tc4modern:models/wandtable').replace('research_table.mtl', 'focal_manipulator.mtl'))
        pack.writestr('assets/tc4modern/models/block/focal_manipulator.mtl', 'newmtl table\nKd 1 1 1\nmap_Kd tc4modern:models/wandtable\n')
    wand_source = args.reference / 'decompiled/thaumcraft/client/renderers/models/gear/ModelWand.java'
    if wand_source.exists():
        mesh = convert(wand_source.read_text(), '', materials={'Rod': 'rod', 'Cap': 'cap', 'CapBottom': 'cap'}, exclude=('Focus',), origin_y=1.125, cap_scale=1.2).replace('research_table.mtl', 'wooden_wand.mtl')
        pack.writestr('assets/tc4modern/models/item/wooden_wand.obj', mesh)
        pack.writestr('assets/tc4modern/models/item/wooden_wand.mtl', 'newmtl rod\nKd 1 1 1\nmap_Kd tc4modern:models/wand_rod_wood\nnewmtl cap\nKd 1 1 1\nmap_Kd tc4modern:models/wand_cap_iron\n')
        # Rendered by WandRenderer (ItemWandRenderer port); identity transforms so it receives raw contexts.
        display = {}
        pack.writestr('assets/tc4modern/models/item/iron_capped_wooden_wand.json', json.dumps({'parent': 'minecraft:builtin/entity', 'gui_light': 'front', 'textures': {'particle': 'tc4modern:models/wand_rod_wood'}}))
        greatwood_mesh = mesh.replace('wooden_wand.mtl', 'gold_greatwood_wand.mtl')
        pack.writestr('assets/tc4modern/models/item/gold_greatwood_wand.obj', greatwood_mesh)
        pack.writestr('assets/tc4modern/models/item/gold_greatwood_wand.mtl', 'newmtl rod\nKd 1 1 1\nmap_Kd tc4modern:models/wand_rod_greatwood\nnewmtl cap\nKd 1 1 1\nmap_Kd tc4modern:models/wand_cap_gold\n')
        pack.writestr('assets/tc4modern/models/item/gold_capped_greatwood_wand.json', json.dumps({'parent': 'minecraft:builtin/entity', 'gui_light': 'front', 'textures': {'particle': 'tc4modern:models/wand_rod_greatwood'}}))
        pack.writestr('assets/tc4modern/models/item/iron_cap.json', json.dumps({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'tc4modern:items/wand_cap_iron'}}))
    pack.writestr('assets/tc4modern/models/item/greatwood_rod.json', json.dumps({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'tc4modern:items/wand_rod_greatwood'}}))
    for base in ['greatwood','obsidian','ice','quartz','reed','blaze','bone','silverwood','primal']:
        pack.writestr(f'assets/tc4modern/models/item/{base}_staff_rod.json', json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'tc4modern:items/staff_rod_{base}'}}))
    pack.writestr('assets/tc4modern/models/item/gold_cap.json', json.dumps({'parent': 'minecraft:item/generated', 'textures': {'layer0': 'tc4modern:items/wand_cap_gold'}}))
    for item, original in [('bucket_death','bucket_death'),('bottle_taint','bottle_taint'),('crystal_essence','crystalessence')]:
        pack.writestr(f'assets/tc4modern/models/item/{item}.json', json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'tc4modern:items/{original}'}}))
    for totem in ['obsidian_totem','charged_obsidian_totem']:
        pack.writestr(f'assets/tc4modern/blockstates/{totem}.json',json.dumps({'variants':{'':{'model':f'tc4modern:block/{totem}'}}}))
        pack.writestr(f'assets/tc4modern/models/block/{totem}.json',json.dumps({'parent':'minecraft:block/cube_all','textures':{'all':'tc4modern:blocks/obsidiantile'}}))
        pack.writestr(f'assets/tc4modern/models/item/{totem}.json',json.dumps({'parent':f'tc4modern:block/{totem}'}))
    for variant,texture in enumerate(['obsidiantotembase','obsidiantotembaseshaded','obsidiantotem1','obsidiantotem2','obsidiantotem3','obsidiantotem4']):
        pack.writestr(f'assets/tc4modern/models/block/obsidian_totem_face_{variant}.json',json.dumps({'parent':'minecraft:block/cube_all','textures':{'all':f'tc4modern:blocks/{texture}'}}))
    pack.writestr('assets/tc4modern/models/block/amber_block.json',json.dumps({'parent':'minecraft:block/cube_column','render_type':'minecraft:translucent','textures':{'side':'tc4modern:blocks/amberblock','end':'tc4modern:blocks/amberblock_top'}}))
    pack.writestr('assets/tc4modern/models/item/amber_block.json',json.dumps({'parent':'tc4modern:block/amber_block'}))
    pack.writestr('assets/tc4modern/blockstates/amber_bricks.json',json.dumps({'variants':{'':{'model':'tc4modern:block/amber_bricks'}}}))
    pack.writestr('assets/tc4modern/models/block/amber_bricks.json',json.dumps({'parent':'minecraft:block/cube_all','render_type':'minecraft:translucent','textures':{'all':'tc4modern:blocks/amberbrick'}}))
    pack.writestr('assets/tc4modern/models/item/amber_bricks.json',json.dumps({'parent':'tc4modern:block/amber_bricks'}))
    # Vanilla block/block display transforms; item models with only a GUI transform otherwise render at scale 1 in hand, on the ground and in frames.
    def block_display(gui):
        return {'gui':gui,'ground':{'rotation':[0,0,0],'translation':[0,3,0],'scale':[.25,.25,.25]},'fixed':{'rotation':[0,0,0],'translation':[0,0,0],'scale':[.5,.5,.5]},'thirdperson_righthand':{'rotation':[75,45,0],'translation':[0,2.5,0],'scale':[.375,.375,.375]},'firstperson_righthand':{'rotation':[0,45,0],'translation':[0,0,0],'scale':[.4,.4,.4]},'firstperson_lefthand':{'rotation':[0,225,0],'translation':[0,0,0],'scale':[.4,.4,.4]}}
    for name,original in [('arcane_lamp','lamp'),('lamp_growth','lamp_grow'),('lamp_fertility','lamp_fert')]:
        for active in (False,True):
            suffix='' if active or name=='arcane_lamp' else '_off'
            faces={f:{'texture':'#'+('top' if f in ('up','down') else 'side')} for f in ('up','down','north','south','east','west')}
            pack.writestr('assets/tc4modern/models/block/'+name+('' if active else '_off')+'.json',json.dumps({'render_type':'minecraft:cutout','textures':{'particle':'tc4modern:blocks/'+original+'_side'+suffix,'side':'tc4modern:blocks/'+original+'_side'+suffix,'top':'tc4modern:blocks/'+original+'_top'+suffix},'elements':[{'from':[4,2,4],'to':[12,14,12],'faces':faces}]}))
        pack.writestr('assets/tc4modern/models/item/'+name+'.json',json.dumps({'parent':'tc4modern:block/'+name,'display':block_display({'rotation':[30,225,0],'scale':[.625,.625,.625]})}))
    pack.writestr('assets/tc4modern/models/item/sinister_stone.json',json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'tc4modern:items/sinister_stone'},'overrides':[{'predicate':{'tc4modern:active':1},'model':'tc4modern:item/sinister_stone_active'}]}))
    pack.writestr('assets/tc4modern/models/item/sinister_stone_active.json',json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'tc4modern:items/sinister_stone_active'}}))
    pack.writestr('assets/tc4modern/models/item/flux_scrubber.json',json.dumps({'parent':'minecraft:builtin/entity','textures':{'particle':'tc4modern:models/fluxscrubber'},'display':{'gui':{'rotation':[30,225,0],'scale':[.625,.625,.625]}}}))
    pack.writestr('assets/tc4modern/models/block/flux_scrubber.json',json.dumps({'textures':{'particle':'tc4modern:models/fluxscrubber'}}))
    for powered in (False,True):
        faces={face:{'texture':'#'+('bottom' if face=='down' else 'top' if face=='up' else 'side'),'cullface':face} for face in ('up','down','north','south','east','west')}
        glow={'from':[.16,.16,.16],'to':[15.84,15.84,15.84],'shade':False,'neoforge_data':{'block_light':0 if powered else 11},'faces':{face:{'texture':'#glow','tintindex':0 if face=='up' else 1} for face in ('up','north','south','east','west')}}
        model={'parent':'minecraft:block/block','render_type':'minecraft:cutout','textures':{'particle':'tc4modern:blocks/lifterside','side':'tc4modern:blocks/lifterside','top':'tc4modern:blocks/liftertop','bottom':'tc4modern:blocks/arcaneearbottom','glow':'tc4modern:blocks/animatedglow'},'elements':[{'from':[0,0,0],'to':[16,16,16],'faces':faces},glow]}
        pack.writestr('assets/tc4modern/models/block/levitator'+('_off' if powered else '')+'.json',json.dumps(model))
    pack.writestr('assets/tc4modern/models/item/levitator.json',json.dumps({'parent':'tc4modern:block/levitator','display':block_display({'rotation':[30,225,0],'scale':[.625,.625,.625]})}))
    pack.writestr('assets/tc4modern/models/item/mirror_glass.json',json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'tc4modern:items/mirrorglass'}}))
    for active in (False,True):
        boxes=[([0,0,0],[16,3,16]),([4,3,4],[12,16,12]),([4,8,1],[12,16,3]),([5,8,3],[11,15,4]),([1,8,4],[3,16,12]),([3,8,5],[4,15,11]),([4,8,13],[12,16,15]),([5,8,12],[11,15,13]),([13,8,4],[15,16,12]),([12,8,5],[13,15,11])]
        elements=[]
        for i,(start,end) in enumerate(boxes):
            textures={face:'tc4modern:blocks/'+('arcaneearbelltop' if face in ('up','down') else 'arcaneearbellside') for face in ('up','down','north','south','east','west')} if i>=2 else {face:'tc4modern:blocks/'+('arcaneearbottom' if face=='down' else ('arcaneeartopon' if active else 'arcaneeartopoff') if face=='up' else 'arcaneearsideon') for face in ('up','down','north','south','east','west')}
            elements.append({'from':start,'to':end,'faces':{face:{'texture':'#'+texture.split('/')[-1]} for face,texture in textures.items()}})
        pack.writestr('assets/tc4modern/models/block/arcane_ear'+('_on' if active else '')+'.json',json.dumps({'render_type':'minecraft:cutout','textures':{'particle':'tc4modern:blocks/arcaneearsideon', **{name:'tc4modern:blocks/'+name for name in ('arcaneearbottom','arcaneeartopon','arcaneeartopoff','arcaneearsideon','arcaneearbelltop','arcaneearbellside')}},'elements':elements}))
    pack.writestr('assets/tc4modern/models/item/arcane_ear.json',json.dumps({'parent':'tc4modern:block/arcane_ear_on','display':block_display({'rotation':[30,225,0],'scale':[.625,.625,.625]})}))
    for name in ('paving_stone_travel','paving_stone_warding'):
        pack.writestr('assets/tc4modern/models/block/'+name+'.json',json.dumps({'parent':'minecraft:block/cube_all','textures':{'all':'tc4modern:blocks/'+name}}))
        pack.writestr('assets/tc4modern/models/item/'+name+'.json',json.dumps({'parent':'tc4modern:block/'+name}))
    # BlockMetalDeviceRenderer metadata 5/6: a 3px slab of the cut-out grate texture, the four bar walls inside its
    # openings on each axis (metalbase, renderFaceX/ZPos/Neg at W3/W9 offsets) and, when closed, the inset hatch box.
    def grate_elements(closed):
        elements=[{'from':[0,13,0],'to':[16,16,16],'faces':{face:{'texture':'#grate'} for face in ('up','down','north','south','east','west')}}]
        for x,face in ((3,'east'),(9,'east'),(7,'west'),(13,'west')):
            elements.append({'from':[x,13,0],'to':[x,16,16],'faces':{face:{'texture':'#wall'}}})
        for z,face in ((3,'south'),(9,'south'),(7,'north'),(13,'north')):
            elements.append({'from':[0,13,z],'to':[16,16,z],'faces':{face:{'texture':'#wall'}}})
        if closed:
            elements.append({'from':[1,14,1],'to':[15,15,15],'faces':{face:{'texture':'#hatch'} for face in ('up','down','north','south','east','west')}})
        return elements
    for name,closed in (('grate',False),('grate_closed',True)):
        pack.writestr('assets/tc4modern/models/block/'+name+'.json',json.dumps({'render_type':'minecraft:cutout','textures':{'particle':'tc4modern:blocks/grate','grate':'tc4modern:blocks/grate','wall':'tc4modern:blocks/metalbase','hatch':'tc4modern:blocks/grate_hatch'},'elements':grate_elements(closed)}))
    # inventory render draws the hatch over the grate: use the closed model, lowered like the original -0.3 translate
    pack.writestr('assets/tc4modern/models/item/grate.json',json.dumps({'parent':'tc4modern:block/grate_closed','display':block_display({'rotation':[30,225,0],'translation':[0,-4,0],'scale':[.625,.625,.625]})}))
    for name, texture in [('bath_salts','bath_salts'),('sanity_soap','soap'),('bucket_pure','bucket_pure')]:
        pack.writestr('assets/tc4modern/models/item/'+name+'.json',json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'tc4modern:items/'+texture}}))
    for name, side, top, bottom in [('arcane_spa','spa_side','spa_top','pedestal_top'),('tallow_block','tallowblock','tallowblock_top','tallowblock_top')]:
        pack.writestr('assets/tc4modern/models/block/'+name+'.json',json.dumps({'parent':'minecraft:block/cube','textures':{'particle':'tc4modern:blocks/'+side, **{face:'tc4modern:blocks/'+(top if face=='up' else bottom if face=='down' else side) for face in ('up','down','north','south','east','west')}}}))
        pack.writestr('assets/tc4modern/models/item/'+name+'.json',json.dumps({'parent':'tc4modern:block/'+name}))
    pack.writestr('assets/tc4modern/models/block/ethereal_bloom.json', json.dumps({'parent':'minecraft:block/air','textures':{'particle':'tc4modern:blocks/purifier_seed'}}))
    pack.writestr('assets/tc4modern/models/item/ethereal_bloom.json', json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'tc4modern:blocks/purifier_seed'}}))
    pack.writestr('assets/tc4modern/models/item/triple_meat_treat.json', json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'tc4modern:items/tripletreat'}}))
    for meat in ['chicken', 'beef', 'pork', 'fish']:
        pack.writestr(f'assets/tc4modern/models/item/{meat}_nugget.json', json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':f'tc4modern:items/nugget{meat}'}}))
    for name, base in [('research_notes', 'researchnotes'), ('research_notes_complete', 'discovery')]:
        model = {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'tc4modern:items/{base}', 'layer1': f'tc4modern:items/{base}overlay'}}
        if name == 'research_notes':
            model['overrides'] = [{'predicate': {'tc4modern:complete': 1}, 'model': 'tc4modern:item/research_notes_complete'}]
        pack.writestr(f'assets/tc4modern/models/item/{name}.json', json.dumps(model))
    for item, original in {'thaumonomicon': 'thaumonomicon', 'scribing_tools': 'inkwell'}.items():
        pack.writestr(f'assets/tc4modern/models/item/{item}.json', json.dumps({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'tc4modern:items/{original}'}}))
    # Rotate the horizontal old mesh into a vertical hand-held instrument,
    # preserving face topology and UVs. Minecraft block models use unit coordinates.
    lines = ['mtllib scanner.mtl', 'usemtl scanner']
    for line in source.read('assets/thaumcraft/textures/models/scanner.obj').decode().splitlines():
        parts = line.split()
        if not parts:
            continue
        if parts[0] == 'v':
            x, y, z = map(float, parts[1:4])
            line = f'v {0.5 + x * .25:.6f} {0.5 + z * .25:.6f} {0.525 - y * .25:.6f}'
        elif parts[0] == 'vn':
            x, y, z = map(float, parts[1:4])
            line = f'vn {x} {z} {-y}'
        elif parts[0] in ('mtllib', 'usemtl'):
            continue
        lines.append(line)
    pack.writestr('assets/tc4modern/models/item/scanner.obj', '\n'.join(lines))
    # Unchanged original mesh for the first-person ItemThaumometerRenderer port.
    pack.writestr('assets/tc4modern/models/scanner_original.obj', source.read('assets/thaumcraft/textures/models/scanner.obj'))
    for mesh in ("crystalizer", "thaumatorium", "reservoir", "adv_alch_furnace"):
        pack.writestr(f"assets/tc4modern/models/{mesh}.obj", source.read(f"assets/thaumcraft/textures/models/{mesh}.obj"))
    # Unchanged original alembic mesh for the TileAlembicRenderer port.
    pack.writestr('assets/tc4modern/models/alembic.obj', source.read('assets/thaumcraft/textures/models/alembic.obj'))
    # Unchanged original infusion pillar mesh (TileInfusionPillarRenderer).
    pack.writestr('assets/tc4modern/models/pillar.obj', source.read('assets/thaumcraft/textures/models/pillar.obj'))
    # Unchanged original orb mesh (RenderFrostShard).
    pack.writestr('assets/tc4modern/models/orb.obj', source.read('assets/thaumcraft/textures/models/orb.obj'))
    pack.writestr('assets/tc4modern/models/obelisk_cap.obj', source.read('assets/thaumcraft/textures/models/obelisk_cap.obj'))
    pack.writestr('assets/tc4modern/models/crabvent.obj', source.read('assets/thaumcraft/textures/models/crabvent.obj'))
    pack.writestr('assets/tc4modern/models/vcrystal.obj', source.read('assets/thaumcraft/textures/models/vcrystal.obj'))
    # ArmorMaterial.Layer textures: original thaumium_1/2 armour sheets at the modern armour path.
    for n in (1, 2):
        pack.writestr(f'assets/tc4modern/textures/models/armor/thaumium_layer_{n}.png', source.read(f'assets/thaumcraft/textures/models/thaumium_{n}.png'))
        pack.writestr(f'assets/tc4modern/textures/models/armor/void_layer_{n}.png', source.read(f'assets/thaumcraft/textures/models/void_{n}.png'))
    pack.writestr('assets/tc4modern/models/item/scanner.mtl', 'newmtl scanner\nKd 1 1 1\nmap_Kd tc4modern:item/scanner\n')
    display = {
        'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1.25, 1.25, 1.25]},
        'firstperson_righthand': {'rotation': [0, -20, 0], 'translation': [0, 2, 0], 'scale': [.85, .85, .85]},
        'firstperson_lefthand': {'rotation': [0, 20, 0], 'translation': [0, 2, 0], 'scale': [.85, .85, .85]},
        # 1.7.10 RenderPlayer block-style held item + Forge -0.5 + original EQUIPPED branch,
        # solved exactly against the modern ItemInHandLayer arm transform (tools/thaumometer_thirdperson.py).
        'thirdperson_righthand': {'rotation': [70, -75, 0], 'translation': [-0.5588, -1.8347, -0.8391], 'scale': [.75, .75, .75]},
        'thirdperson_lefthand': {'rotation': [70, -75, 0], 'translation': [-0.5588, -1.8347, -0.8391], 'scale': [.75, .75, .75]},
        'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.5, .5, .5]},
        'fixed': {'rotation': [0, 180, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    }
    model = {'loader': 'neoforge:obj', 'model': 'tc4modern:models/item/scanner.obj', 'automatic_culling': False, 'flip_v': True, 'ambientocclusion': False, 'display': display, 'textures': {'particle': 'tc4modern:item/scanner'}}
    pack.writestr('assets/tc4modern/models/item/thaumometer.json', json.dumps(model))
    # Unregistered, book-only original models; importing them never enables pending gameplay.
    book_display = {'gui': {'rotation': [30, 225, 0], 'translation': [0, 0, 0], 'scale': [.625, .625, .625]}}
    book_sources = [('deconstructor', 'ModelArcaneWorkbench', 'decontable'), ('node_jar', 'ModelJar', 'jar'), ('jar_void', 'ModelJar', 'jar_void'), ('centrifuge', 'ModelCentrifuge', 'centrifuge')]
    for name, cls, texture in book_sources:
        source_file = args.reference / ('decompiled/thaumcraft/client/renderers/models/' + cls + '.java')
        if not source_file.exists():
            raise FileNotFoundError(source_file)
        mesh = convert(source_file.read_text(), 'tc4modern:models/' + texture, origin_y=0 if name in ('node_jar', 'jar_void') else .5 if name == 'centrifuge' else 1, exclude=('Brine',)).replace('research_table.mtl', name + '.mtl')
        pack.writestr('assets/tc4modern/models/book/' + name + '.obj', mesh)
        pack.writestr('assets/tc4modern/models/book/' + name + '.mtl', 'newmtl table\nKd 1 1 1\nmap_Kd tc4modern:models/' + texture + '\n')
        pack.writestr('assets/tc4modern/models/book/' + name + '.json', json.dumps({'loader': 'neoforge:obj', 'model': 'tc4modern:models/book/' + name + '.obj', 'flip_v': True, 'automatic_culling': False, 'ambientocclusion': False, 'render_type': 'minecraft:translucent' if name == 'node_jar' else 'minecraft:solid', 'display': book_display}))
    pack.writestr('assets/tc4modern/models/book/salts.json', json.dumps({'parent':'minecraft:item/generated','textures':{'layer0':'tc4modern:items/bath_salts'}}))
    # BlockTubeRenderer.renderInventoryBlock bounds, in original sixteenths.
    def book_box(lo, hi, texture):
        return {'from': lo, 'to': hi, 'faces': {face: {'texture': texture} for face in ('up','down','north','south','east','west')}}
    def book_model(name, elements, textures):
        pack.writestr('assets/tc4modern/models/book/' + name + '.json', json.dumps({'elements': elements, 'textures': textures, 'display': book_display, 'ambientocclusion': False, 'render_type': 'minecraft:cutout'}))
    for name, side, top in [('spa','spa_side','spa_top'),('tallowblock','tallowblock','tallowblock_top'),('alchemyblock','alchemyblock','alchemyblock'),('brainbox','brainbox','brainbox')]:
        e = book_box([0,0,0],[16,16,16],'#side')
        e['faces']['up']['texture'] = '#top'
        e['faces']['down']['texture'] = '#top'
        book_model(name,[e],{'side':'tc4modern:blocks/'+side,'top':'tc4modern:blocks/'+top})
    for name,meta in [('tube_valve',1),('tube_filter',3),('tube_buffer',4),('tube_restrict',5),('tube_oneway',6)]:
        tex = 'pipe_restrict' if meta == 5 else 'pipe_buffer' if meta == 4 else 'pipe_1'
        els = [book_box([7,0,7],[9,16,9],'#tube')]
        if meta == 1:
            els += [book_box([6,6,6],[10,10,10],'#core'),book_box([10,7,7],[12,9,9],'#core'),book_box([12,4,4],[12.8,12,12],'#wheel')]
        elif meta == 3:
            els += [book_box([5.5,5.5,5.5],[10.5,10.5,10.5],'#core'),book_box([5.49,5.49,5.49],[10.51,10.51,10.51],'#filter')]
        elif meta == 4:
            els += [book_box([4,4,4],[12,12,12],'#tube')]
        else:
            els += [book_box([6.5,6.5,6.5],[9.5,9.5,9.5],'#core')]
            if meta == 6: els += [book_box([6.984,1.6,6.984],[9.016,14.4,9.016],'#arrow')]
        book_model(name,els,{'tube':'tc4modern:blocks/'+tex,'core':'tc4modern:blocks/'+('pipe_2' if meta==1 else 'pipe_filter' if meta==3 else 'pipe_restrict' if meta==5 else 'pipe_3'),'filter':'tc4modern:blocks/pipe_filter_core','arrow':'tc4modern:blocks/pipe_oneway','wheel':'tc4modern:blocks/pipe_valve'})
    crystal = source.read('assets/thaumcraft/textures/models/crystalizer.obj').decode()
    transformed = []
    for line in crystal.splitlines():
        if line.startswith(('mtllib ', 'usemtl ')): continue
        if line.startswith('v '):
            cx, cy, cz = map(float, line.split()[1:4])
            line = f'v {cx+.5} {cz} {.5-cy}'
        elif line.startswith('vn '):
            cx, cy, cz = map(float, line.split()[1:4])
            line = f'vn {cx} {cz} {-cy}'
        transformed.append(line)
    crystal = '\n'.join(transformed)
    pack.writestr('assets/tc4modern/models/book/crystalizer.obj', 'mtllib crystalizer.mtl\nusemtl crystalizer\n'+crystal)
    pack.writestr('assets/tc4modern/models/book/crystalizer.mtl', 'newmtl crystalizer\nKd 1 1 1\nmap_Kd tc4modern:models/crystalizer\n')
    pack.writestr('assets/tc4modern/models/book/crystalizer.json', json.dumps({'loader':'neoforge:obj','model':'tc4modern:models/book/crystalizer.obj','flip_v':True,'automatic_culling':False,'display':book_display}))
    from book_crystals import import_crystals
    import_crystals(pack, source, args.reference, book_display)
staging.replace(out)
print(out)
