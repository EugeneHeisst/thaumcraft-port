# Thaumcraft 4 → Minecraft 1.21.1

2026-10-08: changed from independent mechanics prototype to local original-code port, as requested by the user. Original artifact: Thaumcraft-1.7.10-4.2.3.5.jar. Hashes and source paths: port-manifest.json.

Current status (2026-10-09): later dated sections supersede early implementation notes below. Research puzzles, entity/item/inventory scans, generated aspects, nodes/ores, arcane workbench, stone recipes and original goggles are now integrated. Complete mod content and fresh-world survival validation remain pending.

## Original code now used
- Aspect: full registry of 48 aspects, colors, component graph, original accessors. ResourceLocation/localization imports adapted.
- AspectList: original add/merge/reduce/sorting and NBT methods. Old NBT names mapped to 1.21.1 methods. Empty arrays corrected from [1] to [0]; ItemStack constructor delegates to modern registry adapter.
- PlayerKnowledge: original discovery, pools, parent checks and warp methods. Modern bridge serializes aspect knowledge in player persistent NBT; warp/research fields are not connected to gameplay yet.
- ScanManager: original validScan and checkAndSyncAspectKnowledge logic. Platform services (packets/UI/persistence) delegated to the modern bridge. Cap defaults to the original 100. Other scan methods/nodes/entities still pending.
- ResearchManager: original getCombinationResult and reduceToPrimals overloads. Research puzzles/entries are not ported yet.
- ConfigAspects: 125 direct constant registration records and 19 ore-dictionary records adapted via MCP stable 12 field names into modern registry IDs. Metadata variants mapped for wool, dyes, fish, wood and several other families. Computed entries, complex registrations, recipe-derived tags and original TC4 items remain pending. Unmapped old names cannot be targeted by modern registry IDs. New Minecraft content requires explicit decisions later.

## Local sources
Adapted original Java lives outside Git at /home/eugheist/thaumcraft4-reference/ported-src. Gradle compiles this source tree into the local development JAR. Override with `-Ptc4PortSources=/another/path`. This checkout alone does not contain the original/adapted source tree. Reference decompiles also remain outside Git.

## Platform bridge
ResearchKnowledge loads/saves the original PlayerKnowledge/AspectList, preserves 0.1/0.2 scanned IDs and points, implements the modern player/platform adapter. Login, respawn and scan result packets synchronize discovered aspects. Six primals are initially discovered with zero points. First compound discovery grants +2; at 100 points awards use sqrt and at 125 points awards above one reduce to one.

Temporary `/tc4 combine <first> <second>` exposes the original combination algorithm until the actual research table is ported. `/tc4 aspects` inspects available points. The original research-table bonus pools are not connected.

## Verification
Build and both check suites pass. OriginalPortTest verifies 48 aspects, all 42 symmetric combinations, reduction to primals, NBT round-trip including zero-point discovery, parent prerequisites, first-discovery bonus, cap behavior and bridge discovery chain.
Isolated muted Xvfb client: 0.2 knowledge survived migration/relogin; grass blocked with missing Victus; scanning water granted Aqua; `/tc4 combine aqua terra` discovered Victus; grass then scanned as Herba + Terra. Screenshots in reference directory: port-world.png, port-blocked.png, port-combined.png, port-unlocked.png. Saved and quit client normally.

## Remaining original systems
Research table and Thaumonomicon; research data/puzzles and recipes; items/entities; crafting-derived aspect calculation; aura nodes; wands/foci; crucible/alchemy; essentia network; infusion; golems; mobs/worldgen/warp effects. The present JAR is a partial port, not the complete original mod.

## Research foundation — 2026-10-09

Additional original code compiled from the external port tree: ResearchItem, ResearchCategories, ResearchCategoryList, text/image portions of ResearchPage, HexUtils, ResearchNoteData; ResearchManager.createNote/getData/updateData/checkResearchCompletion/checkConnections. ItemStack mutable NBT was replaced by a CompoundTag bridge; modern item components carry the unchanged note schema. Completion is still the original graph traversal: links require an immediate component relation and known aspects; all fixed anchors must connect. Ordinary placements cost one point and one ink use; original RESEARCHER1/2 refund/free-placement probabilities are bridged. Table bonus aspect pools are not yet implemented.

ConfigResearchData adapts metadata from 199 original ConfigResearch registrations. Text page order and named recipe references are retained. Recipe API overloads, nontext custom pages, trigger data, original forbidden/warp effects and most original item icons are not fully wired. The book renderer is a modern platform adapter using original textures, graph coordinates and page text; it does not yet reproduce all original effects/animations. Long text is paginated rather than truncated.

Five original survival recipe patterns were adapted into external ported-resources: ordinary table, scribing tools, refill, iron cap and basic wooden wand. Modern vanilla iron nuggets and dye tags replace the old IDs/ore dictionary. Two ordinary tables plus scribing tools create the research table and transfer tools into slot zero. WandManager's bookshelf trigger creates the book without vis, matching the original. Wand vis storage/draining/charging is pending.

Local resources now import all original texture PNGs and language tables. The converter reads original ModelTable, ModelResearchTable and ModelWand cuboids/UVs outside Git and emits local OBJ/MTL models. Focus, conditional inkwell/scroll/paper decorations and full item renderer effects remain pending. Original OBJ UVs must be flipped for NeoForge (`flip_v: true`); old `models/` and `items/` textures require additional modern atlas directory sources. OBJ material names must exist before the loader encounters `usemtl`.

Verification: all existing checks plus ResearchPuzzleTest pass. The new checks cover original category/cost/complexity data, seeded note generation and holes, anchor counts, full note NBT, hex coordinate inverses, anchor/out-of-grid edit refusal, point spending, component connections/completion, completed-note immutability, research parents, duplicates and NBT persistence. Muted isolated Xvfb clients verified book-generated notes, slot insertion, a controlled Terra/Victus/Aqua note solved via the real client packet path, learning and child unlock, original model textures, survival table conversion, iron-cap/wand crafting and bookshelf conversion. Full natural generated-note solve in survival has not yet been verified end to end; nodes, missing scanning modes and other content remain active work.

## 0.5.0 aura and survival slice
External original adaptations now include vis storage, node reservoir regeneration/exhaustion/tapping, node type/modifier enums, renderer layer formulas, node content generation, biome aura records, shard palette/drop counts and original ore generation loop. Modern adapters handle item components, block entities, client vertex submission/shader threshold, biome classification and feature registration. New Overworld chunks provide all six ores and naturally generated nodes; original thaumometer recipe consumes shards. Node-type world effects and other dimensions remain incomplete; faint non-scanner visibility is not yet verified. Root tc4-local-resources.zip is the auto-loaded archive (resourcepacks/ is not the deployment destination).

## 0.6.x arcane crafting
Original arcane workbench model, GUI coordinates, ingredient/wand inventory, recipe shape matcher and cap-adjusted atomic vis consumption are bridged to modern menus/components. Original enchanted fabric and arcane stone recipes are adapted from ConfigRecipes externally; ordinary stone-brick/stair/slab recipes are synchronized through the vanilla recipe manager. The book shows all these recipe records. Secondary fabric research spends original Pannus/Praecantatio costs. Actual clients verified charge persistence, denied crafting without sufficient vis, ingredient consumption, successful crafting with exact vis costs, shift crafting and contents/drop preservation.

Arcane recipes currently load supported original records from a fixed JAR classpath list, rather than a custom reloadable/synchronized datapack recipe type. Remaining original arcane recipes, goggles/armor, recipe-derived aspect generation and further Thaumcraft systems are ongoing. Standard modern stone ingredients use c:stones in place of the old ore dictionary.

## 0.7.0 generated aspects
Scanning uses original explicit registrations, all40 literal ore categories plus16 dye categories, and recipe-derived tags. Original arithmetic and52 complex vanilla registration records are adapted externally. Modern tags replace OreDictionary category membership; optional metal tags require an available ingot as in the original. Papers and books receive their original Cognitio additions. Ordinary shaped/shapeless and currently ported arcane recipes participate; unsupported custom recipe types, crucible/infusion sources and dynamic item bonus tags are not yet covered. Cache invalidation follows modern recipe/tag synchronization. Actual clients verified prerequisites, derived aspect counts and reload behavior.

## 0.10.0 alchemy and taint slice — 2026-10-09

Original adapted classes now cover crucible heat/water/aspect state, ten starter recipes and special item release; nitor and alumentum; finite flux flow/spills/decay; vis exhaustion and taint damage; slime size/merging/splitting; fibres, solid taint decay/falling; spores, swarmers, swarm motion and attacks. These classes and generated resource records remain in the external reference tree. Own Java bridges bind modern components, menus, fluid capability, blocks/entities, biome columns, models, particles, sounds and damage registries. The access transformer exposes only the loot-eligibility timer required by the original summoned swarm's 100-tick marker.

Proof is recorded in the reference client's crucible/nitor/alumentum/flux/taint logs and screenshots. Swarm attack proof: health 1000→992 and Poison duration95, original particles visible; cleanup restored base/health20 and exact armor/item components. Native goo spawned slimes; original crust fell to the expected cell, logs inhibited falling, outside-biome soil reverted, and a command-block detector proved the original metadata8 conversion creates nine fluid quanta. No actual Prism worlds were driven. All eight standalone check gates and Gradle test/check passed on NeoForge21.1.249.

Not yet proven: fresh-survival acquisition of every starter recipe, natural taint biome/fibre spread, summoned-swarm loot marker in gameplay, full legacy creature navigation and remaining environmental/render/audio effects. The biome's original decoration/spawn lists are not ported. Fluid amount9 temporarily uses the full-block model; old corner height averaging remains unfinished. This local release is a checkpoint in the continuing full port, not completion.

## 0.17.4 — aspect coverage foundation (2026-10-09)

The isolated dedicated-server audit covers every registered vanilla/TC4 object: 1435 items, 1108 blocks and 142 entity types, with no unexplained empty aspect lists. Air is empty by design; aura nodes, dropped items, players and filled essentia phials use their actual contents. `run-aspect-audit/aspect-coverage.json` records final values and 139 tested variants. Run `./dev.sh runAspectAudit -Pneo_version=21.1.249` to regenerate the report without touching Prism worlds.

Original registrations take precedence. Omitted TC4 creature/raw-material tags, potion/equipment/enchantment/wand rules and eight wand-part infusion recipes are now connected. Original-derived thaumonomicon, crucible and salis mundus tags use their real ingredient tags. Modern content receives curated fallback values from `assets/tc4modern/aspect_catalog.json` after recipe resolution; these are new balancing decisions, not historical TC4 values. Recipe recursion tracks each ingredient branch separately, and stack-specific bonuses never mutate the shared cache. Potion types and taint metadata variants retain distinct scan keys; durability and stack count do not create repeatable scans.

Research tables now replenish original environment bonuses every 602 server ticks, persist them, synchronize them through menu data slots and spend reserves only when personal pools are empty. Same-aspect experiments validate the full two-point cost atomically. Altitude thresholds use height relative to the modern world's lower build limit; crystal-cluster bonuses await the unregistered crystal blocks. The UI uses a compact plus marker for reserves; exact legacy sparkle rendering is not claimed.

Dedicated-server loading now isolates node particles and warp packet client receivers. Verification: full Gradle check/build on NeoForge 21.1.249; actual server registry/recipe audit; all 48 filled phials, all 36 cap/rod combinations, potion types, taint variants and enchanted/damaged equipment; cache isolation and table persistence/consumption tests. Native client visuals and long-term balance of the new-content catalog remain to be reviewed in gameplay.

### BASICS book (0.17.5)

Original content source: ConfigResearch.initBasicResearch, ConfigRecipes, GuiResearchBrowser, GuiResearchRecipe and TCFontRenderer from the user's 4.2.3.5 JAR. The local port now preserves 19 research entries and 59 source page boundaries. Original font wrapping lives outside Git in thaumcraft.client.lib.ResearchBookLayout; platform adapters are ResearchBookText, BasicsBookRecipes and BookDisplayModels. Resource import adds Minecraft 1.7.10 Unicode glyphs and book-only deconstructor/jar/crystal meshes to the local ZIP. There are no gameplay registrations for these display-only objects.

Run `python tools/import_tc4_resources.py Thaumcraft-1.7.10-4.2.3.5.jar run-book-audit/resourcepacks`, enable file/tc4-local-resources.zip in that directory's options.txt, then `xvfb-run -a -s '-screen 0 1280x960x24' env LIBGL_ALWAYS_SOFTWARE=1 ./dev.sh runBookAudit -Pneo_version=21.1.249`. The property-gated audit uses an isolated run directory, no server/world and no user desktop input. result.txt and screenshots are local ignored artifacts. ResearchBookTest is part of check. Original text/page/recipe metadata is verified; modern title/item rendering is not a proof of pixel-identical legacy-client output. Gameplay mechanics described by BASICS are deliberately deferred by the user.


## 0.17.7 — research-table interface

Completed the pending modern adapter for original GuiResearchTable: original slot/grid positions, five-row scrolling aspect palette, drag/drop placement and combination selection, erase gestures, prerequisite-gated component hints and shift combinations, original sound requests, reserve sparkle, parchment runes and anchor-connected highlights. Notes can be copied after RESEARCHDUPE using paper, ink and the original research cost plus accumulated copies; the hover panel displays that cost. The combination button shows its short cooldown overlay. Server handlers retain distance/menu/ink/anchor validation and atomic point spending.

Verification: NeoForge 21.1.249 full check/build; isolated muted native research-table audit covering render, dragging, placement, personal-point spending, erasure, combining, reserve spending and refusal to erase without usable ink. Native screenshots are in run-table-audit/table-audit. Sounds are validated as requests, not by listening. These checks do not establish a complete natural survival research playthrough or pixel-identical 1.7.10 presentation.


## 0.17.8 — original debug/admin commands

Adapted the original CommandThaumcraft from TC4 4.2.3.5 outside Git and registered its `/thaumcraft`, `/thaum`, `/tc` aliases through Brigadier. Preserved all original help/research/aspect/warp branches, case handling, argument count rules and operator level 2. Research grants recursively complete parents, hidden parents and siblings, award warp once per newly learned research, include hidden/lost entries for all, and retain auto-unlocked entries on reset. Reset touches only research. Aspect grants add points using original short conversion without the scan cap. Warp set/add use lower-level mutators, allowing negative permanent additions and preserving the event counter; omitted/unknown type selects normal/sticky, add bounds -100..100 and set minimum 0. Original help/feedback text retained. Online targets and modern single-player selectors supported; legacy custom development helpers preserved.

Validation: full NeoForge21.1.249 check/build; LegacyCommandTest compares short conversion with original PlayerKnowledge and verifies dependency/sibling cycles, repeated grants, auto-unlock reset, unrelated data preservation and command-specific warp rules. Isolated runCommandAudit verifies actual dedicated-server registration, all aliases/actions, console-to-player and @s grants, independent players, original research warp, reset preserving warp, numeric limits and operator permissions. Uses two ephemeral fake players and no Prism world changes. No live client screenshot/chat check claimed.


## 0.17.9 — inventory model corrections

Corrected original wand inventory rendering by restoring ForgeHooksClient.renderInventoryItem's final -90° Y rotation, omitted from the prior modern helper. All wand cap/rod combinations now show the full diagonal wand rather than a nearly horizontal edge. A shared helper reproduces the original ten-pixel Forge transform in the modern sixteen-pixel item space. Thaumometer GUI rendering now uses the unchanged original scanner OBJ, original INVENTORY branch rotations and its scanscreen quad instead of the simplified front-facing ring. A context-sensitive baked-model wrapper routes GUI renders to this renderer and returns the existing model for other item contexts; first-person rendering remains independently handled.

Expanded runInventoryAudit to paginate every registered item and 93 additional variants: 36 wand assemblies, 48 filled phials, three equipped foci and six filled/labelled jars or labels. All 195 icons render without unresolved names or missing models/particle textures; enlarged native screenshot covers complex items. Native Xvfb client opens no world and remains muted. Screenshots/result under run-table-audit/inventory-audit, especially inventory-details.png. Original source chains verified against local TC4 and Forge1.7.10 decompilation; no pixelwise comparison with a running legacy client claimed. Resources unchanged.

2026-10-09: v0.17.10: visible inventory checks at user request; six infused ore models inherit block transforms and separate overlay depth, fixing flat icons; node stabilizer/converter inventory views gain original block perspective and full inventory light on overlays. New creative aura_node BlockItem uses animated original node atlas and places a real six-primal40vis reservoir (240 total). Scanner hotfix from0.17.9 retained.
Includes staged BASICS gameplay: deconstruction table,7clusters, node jar capture/release, fragments/Crimson reading, Cinderpearl, Mana Beans/pods, Haste/Repair, original fragment chest weights and40potion slots. Magical Forest via embedded TerraBlender4.1.0.8, original large-oak algorithm and pod model, guaranteed pods after tree placement with ground search under canopy. This does not complete all19 BASICS entries: Pech/trading, cultists/Crimson acquisition and filled-jar inventory effects remain pending.
Validation: check/build NeoForge21.1.249 PASS; native visible inventory PASS210 registered/variant icons, screenshot enlarged ores/devices/node inspected; native BasicsServerAudit includes actual BlockItem placement and240vis/base reservoir, prior mechanics PASS; native ForestServerAudit freshworld at768,70,768:25chunks6718logs4474branches51naturalpods, colours/growth/Lux breeding/NBT/ripe drops/eating/40potion slots/five fragment chest tables PASS. Visible checks use isolated worlds; existing clients/worlds preserved.


### Alchemy devices (0.17.67)

Local original sources: reference/alchemy-original (fresh CFR), external ported-src/thaumcraft/common/tiles/essentia/{AlchemyMachineState,VoidJarState,ThaumatoriumState}. Modern hosts register centrifuge, essentia_crystalizer, void_jar, alchemical_construct, thaumatorium (top boolean) and mnemonic_matrix. Thaumatorium itself has no craftable standalone block item; form it with a wand using two constructs over a crucible, then operate the selection GUI. Original recipe identities are persisted as stable modern keys rather than old integer hashes. Full external source/resource directory remains necessary to build.

DEVIATION: creative filled-jar drops are explicitly requested by the user. Thaumatorium null recipe input is guarded, selection requests are validated server-side, and upgrade trimming keeps owner lists aligned. Original transient processing progress and unusual face/suction behaviors are retained. Source KeyHandler registers F/G/H only; Baubles provides its separate B binding. The original F default conflicts with modern Minecraft offhand swap and can be changed in Controls.

### Inventory scanner, hover landing and held levitator (0.17.68)

Inventory scan completion is server-timed: an early finish packet stays pending until twenty ticks have elapsed. Pending scans validate the same menu, cursor thaumometer, occupied slot and target identity; cursor/screen/hover cancellation clears them. This avoids assuming equal client/server tick progress. The existing inventory gesture remains picking up the thaumometer onto the cursor and hovering an item.

HoverLandingMixin redirects only the final onGround check in LocalPlayer.aiStep, which otherwise disables flying and sends abilities on landing. The redirect applies only while a noncreative player wears a fueled active harness. Original fuel timing and mayfly=false remain intact. Levitator's generated model inherits the modern standard block display parent for hand, ground and frame contexts.
