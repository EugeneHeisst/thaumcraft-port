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
