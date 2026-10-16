# Thaumcraft PORT — Codex handoff

## Local 0.17.173 — EMT tool interoperability

Local development dependency only; production remains0.17.172, no push/release/install. Added TC4Addons BoreTool hooks (arcane bore accepts and pays electric pickaxes, no durability), BlockBreakHook through existing ElementalAxeBreakMixin for original onBlockStartBreak behavior, and infusion output hooks preserving addon result components such as EU. Exposed original ElementalTools.harvest/furthest and client startScan for addons. Existing behavior without registered hooks unchanged.

Validated with EMT0.1.1 + IC20.1.18 + ASP0.1.0 + JEI: server123/123 including real native bore mining with EMT/IC2 drills and ordinary diamond pickaxe durability, insufficient EU/no target destruction, native rockbreaker/stream hooks and protection, original charged infusion outputs. Hidden silent client scan payload starts native ore-through-wall renderer, tool/research/GUI models inspected. Full build passed using localJEI property because Maven download timed out: JAVA_HOME=/usr/lib/jvm/java-21-openjdk ./gradlew build -PjeiLocalJar=/home/eugheist/.local/share/PrismLauncher/instances/1.21.1/minecraft/mods/jei-1.21.1-neoforge-19.57.0.451.jar. JAR/build/EMTcompatlogs/screens in versions/0.17.173.

Next EMT armor→remainingEMT→Gravi Suit→combined compatibility. TC4 production deployment requires separate user authorization.
