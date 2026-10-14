# Creative inventory parity — 0.17.134

The reference is the actual `Thaumcraft.tabTC` population in the user's original Thaumcraft 4.2.3.5 client, captured through `CreativeTabs.displayAllReleventItems` with cheat sheets disabled: 453 stacks, including metadata/NBT variants. Capture: `/tmp/tc4-creative-original/creative.tsv`; original JAR SHA256 is recorded in `port-manifest.json`. No reference-world changes were made.

`src/main/resources/creative-tab.json` records modern IDs, original translation keys and reference positions. The port sorts existing original counterparts by these positions. Modern-only entries, such as separate spawn eggs, follow the original groups; optional copper entries stay within their original item families. Registry fallback ensures future items cannot silently disappear. The cheat Thaumonomicon is shown immediately after the ordinary book only when enabled. Two obsolete wand IDs are represented by actual component-based wands; research-note internal forms remain hidden.

Nine previously absent BlockItems are now available: greatwood leaves, flux goo/gas, tainted stone/soil, tainted fibres/grass/plant/spore. Variant-specific placement preserves the original states. Tainted item aspects use the corresponding original ConfigAspects entries. Sixteen coloured banners are added alongside the cult banner. Original Russian and English names cover registered counterparts, with distinct candle/banner colour names and readable golem core type names. The local resource ZIP must be updated together with the JAR because it has translation priority. Its non-language entries remain byte-identical.

Native checks (isolated Xvfb clients, copied inspection save):

- `runCreativeTabAudit`: 370 registered item IDs, 482 displayed stacks with cheat sheets disabled; every eligible ID is present, original relative order is monotonic, all names resolve in Russian and English, models resolve, sixteen banner colours and distinct candle names. Technical block states without Item forms are enumerated in the report; this does not claim that every internal structure block is directly placeable.
- `runCreativeGameplayAudit`: actual placement of all nine new forms and their variant states; 128 ordinary creative node placements vary aspects, types and modifiers, while explicit custom aspects survive placement. Real creative GUI hovering sends scan packets and records server knowledge. Actual `/tc debug all` synchronizes every registered item and creative variant; the real tooltip shows discovered aspects. Actual Baubles screen shows four empty accessory silhouettes.
- `runCreativeScanSurvivalRegression`: existing survival inventory-hover packet audit, early-finish timing and cursor-removal cancellation.

Node generation calls the same original-derived helper used for natural nodes; it does not invent a new distribution. Rendering keeps original per-aspect layers: fixes concern the eye-position rotation axis, modern quad reordering and the item preview's four fixed aspects/opacity. It does not remove the original animation layers. Screenshots are inspected; this is not a claim of exhaustive pixel equality for every node/viewpoint.

The user's main Prism client and world are not restarted or altered by audits. Earlier paused progression audit edits are excluded from this release.
