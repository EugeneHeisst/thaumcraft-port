# Thaumcraft PORT — Codex handoff

## Local 0.17.173 — EMT tool interoperability

Local development dependency only; production remains0.17.172, no push/release/install. Added TC4Addons BoreTool hooks (arcane bore accepts and pays electric pickaxes, no durability), BlockBreakHook through existing ElementalAxeBreakMixin for original onBlockStartBreak behavior, and infusion output hooks preserving addon result components such as EU. Exposed original ElementalTools.harvest/furthest and client startScan for addons. Existing behavior without registered hooks unchanged.

Validated with EMT0.1.1 + IC20.1.18 + ASP0.1.0 + JEI: server123/123 including real native bore mining with EMT/IC2 drills and ordinary diamond pickaxe durability, insufficient EU/no target destruction, native rockbreaker/stream hooks and protection, original charged infusion outputs. Hidden silent client scan payload starts native ore-through-wall renderer, tool/research/GUI models inspected. Full build passed using localJEI property because Maven download timed out: JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build -PjeiLocalJar=/home/eugheist/.local/share/PrismLauncher/instances/1.21.1/minecraft/mods/jei-1.21.1-neoforge-19.57.0.451.jar. JAR/build/EMTcompatlogs/screens in versions/0.17.173.

Next EMT armor→remainingEMT→Gravi Suit→combined compatibility. TC4 production deployment requires separate user authorization.

2026-10-16 Codex LOCAL0.17.174: public native FociContent.startUsing for addon sustained foci; TC4Addons.registerInfusionComponents/tagIngredient carries persistent tag alternatives through native matrix matching/consumption/save, book and expanded JEI variants. EMT0.1.3 uses it for any primal shard/any sapling. Native fish/potion handling preserved. Combined EMT225/225server incl matrix assignment/reload + real client foci packets/held shield/energyball/Christmas/JEI/book passed; build/archiveversions0.17.174 done. Production remains172, no install/push/release.

2026-10-16 Codex LOCAL0.17.175: AuraNodeEntity typed protected constructor for addon node subclasses; old public pos/state constructor delegates unchanged. EMTportableNode extends genuineAuraNodeEntity with ownregisteredBlockEntityType, native generation/wand/renderer, forcedFADING/UNSTABLE. Actual saveWithFullMetadata/BlockEntity.loadStatic retains EMTsubtype/id/vis. EMT0.1.6server302/302+hidden nativeclient placement/aspects/vis/cloud/disposal/JEI/book passed; build/archiveversions0.17.175 done. Production still172, no install/push/release.


2026-10-16 23:45 Moscow: LOCAL0.17.176 for EMT0.1.7. Native tin/silver/lead clusters (original aspects/models), typed TC4Addons native-cluster input/output/factor/perBlock bridge shared by Dowsing elemental-pick drops and ArcaneBore. RecipeAspects divides infusion aspect estimates by actual addon output count. ConfigResearchData now guards its own initialization, rather than assuming any addon category means native research loaded. Validated EMT355/355 including genuine pick/bore harvest and native INFUSION research presence, isolated startup disabled config, hidden82item/3cluster/book/JEI client, build. Archived versions/0.17.176; production still172. No publish/install.


## Codex 2026-10-17 — LOCAL 0.17.178 (production stays177)

ArcaneBore copies its effective enchanted tool after protection and before EU debit. VirtualSilkVajra with exactly3333EU now dropsstone rather than losingSilk after payment; ownSilk/Unbreaking retained. NativeBoreAudit5/5 (lastEU, ownenchants, off,3332, canceledprotection), realnetwork26/26/118272EU conserved, archivedversions/0.17.178. No install/publication for178 yet. Latest installedset/approval/Claudehandoff: /home/eugheist/Project/CLAUDE_CONTINUE_PORTS.md.

Final local-next full production-pack regression: dedicated29/29, hidden client/JEI14/14 (2026-10-17 04:51 Moscow); exact EMT.10+TC178 with IC2.23/ASP.12/Gravi.14. Archived full-pack-server.log/full-pack-client.log. Production unchanged.


## Issue #1 — Portable Hole star-field walls, 0.17.179 (2026-10-17)

User explicitly requested a native original-game test before implementing the fix. The original Minecraft1.7.10/Forge1614/TC4.2.3.5 was launched on its own muted Xvfb with a small instrumentation agent. The genuine controller RMB path created72holeblocks and72clientTileHole instances; native TileHoleRenderer counter1749. Definitive DOWN screenshots05/06 show dark star-field walls and blue edge sparkles, open entrance/exit. Earlier horizontal fixture lacked client tile entities and is diagnostic only; do NOT use its plain stone walls as the original reference. Reference log, screenshots, instrumentation source and notes archived at versions/0.17.179/original-reference/. Full runtime in ignored related/thaumcraft4-reference/legacy-portable-hole-audit/, rerunnable via run-original-hole.sh; user originalPrismgameuntouched.

Root cause: HOLE is invisible and only edge sparkles were implemented; HOLE_ENTITY had no block-entity renderer. New PortableHoleRenderer registers via FociClient, renders original inward.001/.999planes only against solidopaque nonHOLE neighbours. Sixdirections,16layers/seed31100/originaltextures/colors/time/layerdepth+scale, camera-projectedUVs, depth-writingbase, additivefield; beyondsquared512 uses half-grayparticlefield32. No world mechanics, focus cost, lifetime, save format or addon API changes.

New PortableHoleRenderAudit uses native BER collection and screenshots in an isolated world. Final failing-before baseline:42checks/25failures; fixed54checks/0failures. Checksinclude6sides,inside,connectedHOLE/air(no partitions), originalfartexture/quadcount/RGBA127 and real35tickrestoration. Pixelclassification handles shadedgold by colorratios, not absolutebrightness (originalprobe44/36/9). Native screenshots visually inspected. Run ./audit-hole-client.sh (hidden/silent, existing copied FocalInspection auditworld only). JAR/build/server/fullpack results and deployment state follow below when complete. GitHub publication/issue closure still requires user's review; release note must state no human in-game test.


Final validation/deployment 2026-10-17 19:54 Moscow: final filtered starfield54/54PASS; real FociAudit3×3×9andrestorationPASS, native image compared with genuine original. Cleanbuild then finalbuildPASS; finalJARSHA22c67c732de81bfb422bfb8ed505a2525227ece8c5b334503313a60871b4d1e1. ExactfinalJAR fullpackdedicated29/29+hiddenclientJEI14/14, noERROR. Installedserver+Prism,oldTC178backedupatmod-backups/codex-hole-20261017-195300,offlineworldbackup209007537bytesCRCverified. FreshDone19:53:44/1.568s,0ERROR,20TPS/1.9ms. All5portsSHAidenticalbothplaces. Currentmanifest ../compat-test/CODEX_HOLE_DEPLOY_STATE.json and CURRENT_DEPLOY_STATE.json. Sourcecommitd5e3d2d. Release/issueclosureawaitspecificuserreview; noGitHubwrites.


User review complete: «проверил все четко». This answers the pending review/publication question; publish TC4 0.17.179 and close issue#1. Release notes accurately scope manual verification to the Portable Hole fix; other tests remain automated. Other ports’ pending releases are separate.
