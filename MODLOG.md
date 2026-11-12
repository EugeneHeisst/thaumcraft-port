# Thaumcraft UNOFFICIAL PORT — changes

This summary records public feature milestones. Detailed subsystem behavior and historical validation are in [port notes](docs/port-notes.md) and [validation reports](docs/validation/).

## 0.17.193 — 2026-11-12

- Build inputs and converter reference directories can be configured with Gradle properties and environment variables.
- Source provenance records use paths relative to the repository root.
- Focus and infusion payloads use neutral protocol versions. **Both client and server must use 0.17.193 or later; 0.17.192 and 0.17.193 cannot connect to each other.** Packet identifiers and codecs are unchanged.
- Build and port documentation describes the public project configuration.

## 0.17.192 — 2026-11-06

- Restored native trader professions, generated village content and daily trade restocking.
- Corrected Pech interaction behavior and jar aspect rendering.
- Regression results and reproduction instructions: [November 6 validation](docs/validation/issues-2026-11-06.md).

## 0.17.191 — 2026-10-29

- Addon aspects use their registered texture identifiers in native scanning, the thaumometer, goggles and the deconstruction table.
- Corrected crab behavior, furnace outputs and related native interactions.
- Regression results: [October 29 validation](docs/validation/issues-2026-10-29.md).

## 0.17.188 — 2026-10-24

- Added stripped greatwood and silverwood logs and wood, including axe stripping and wood variants.
- Corrected nitor particle textures for breaking and infusion consumption.

## 0.17.185

- Restored terrain during warp fog effects when alternate chunk rendering is present; the original exponential fog effect remains.

## 0.17.179–0.17.183

- Restored Portable Hole star-field walls in all six directions, preserving open tunnel entrances and connected holes.
- Added native structure location support and corrected boots, snow and arcane recipe identifiers.

## 0.17.x feature milestones

- Complete aspect scanning for registered items, blocks and entities; dynamic item contents, potion/equipment modifiers and recipe-derived aspects.
- Original Thaumonomicon research metadata, pages, graphics, parent relationships, discovery triggers, hidden entries and operator commands.
- Focus upgrades and pouch controls, staff and sceptre variants, wand charging, vis relays and energized nodes.
- Infusion item recipes and enchantments, runic augmentation, original instability/symmetry rules and consequence handlers.
- Golem variants, cores, upgrades, inventory interactions, bell controls and reservoir behavior.
- Warp events, taint spreading, original mobs, champions and bosses, Outer Lands structures and portal progression.
- Configurable world generation, biome integration, villages, professions, chest loot and retrogen.
- Optional JEI recipe displays and magic workbench recipe transfer.
- Creative inventory ordering, colored banners, variant item models and original names.

## 0.15.x–0.16.x feature milestones

- Original thaumometer first- and third-person transforms, animated scan display and rune effects.
- Cinnabar, amber, greatwood and silverwood content, silverwood aura nodes and original natural placement rules.
- Alchemical furnace, alembics, essentia tubes and jars, phials, labels and vis relays.
- Tube connection controls, original vent smoke and persistence.
- Infusion matrix, pedestals, pillars, altar formation, original cycle logic and infusion enchantments.
- Thaumium tools and armor, crafting, native damage values and anvil repair.

## 0.1.x–0.13.x foundations

- Ported original aspect registration, graph combinations, knowledge persistence, scanning prerequisites and rewards.
- Research puzzles, notes, tables, original model conversion and resource import.
- Wand assembly and vis storage, aura nodes, ores, magical forest and natural structures.
- Arcane workbench, crucible, nitor, alumentum, flux and taint mechanics.
- Bellows, greatwood trees and the original infernal furnace multiblock, smelting bonuses and food recipes.

Sodium и Iris не поддерживаются: с ними возможны визуальные баги.
