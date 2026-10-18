# Thaumcraft feature search (0.17.181)

Operator level 2 commands, suggested by the native command dispatcher:

- `/locate structure tc4modern:mound`
- `/locate structure tc4modern:hilltop_stones`
- `/locate structure tc4modern:eldritch_ring`
- `/locate structure tc4modern:obsidian_totem`

The search checks seed candidates in increasing distance from the command source, in a square of +/-100 chunks. It finishes candidate chunks through ordinary world generation and checks actual characteristic blocks before reporting clickable coordinates. This is an incremental search; the immediate command return value is 1 when queued, not a distance for `/execute store`. One Thaumcraft search runs at a time per server. Searches can take minutes and generate additional terrain/save data. A result is a verified intact building, not a mathematical guarantee of the closest block among all buildings.

These are native Brigadier branches for legacy features, not dummy structure registry entries. They do not support structure tags or explorer maps. Vanilla structure/biome/POI command branches remain available. No existing generation, blueprint, loot, node, entity, biome, dimension or rarity rules are changed; no second generation pass or retrogen is introduced. Disabled Thaumcraft structures, flat generators and dimensions other than the Overworld are rejected.

Existing chunks have no native structure starts to index retroactively. Intact old buildings can be found when their chunks match the current seed/settings candidate filter. Changed modpacks, feature ordering, worldgen datapacks or totem rarity may invalidate that filter for previously generated terrain. Modified/destroyed buildings and player-made copies are not reliably distinguished; the checks recognize characteristic intact block layouts. There is no promise of an exhaustive historical index, and searching never resets or regenerates old chunks.

Wizard towers and banker houses are vanilla village pool pieces: use `/locate structure #minecraft:village`, which finds a village, not a guarantee of a particular piece. Outer Lands labyrinths are created through portal activation and generated from saved maze cells by their dedicated dimension generator, rather than natural independent structure placements. They are not advertised as Overworld locate targets.

For the standard placement layout, the search reproduces Minecraft's actual decoration seed and globally sorted feature index, evaluates the actual leading rarity filter, and (for totems) the roll after in-square placement. Unrecognized placement layouts conservatively check every chunk. Additional datapack conditions are left to real generation; a candidate alone never produces a result.
