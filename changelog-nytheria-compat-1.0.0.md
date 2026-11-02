## Nytheria Compat 1.0.0 (formerly Sporran)

Nytheria Compat now runs four NeoForge 1.21.1 mods on Fabric and leaves everything else as plain Fabric. It is no longer a
general NeoForge loader; it is an internal dependency of the Nytheria modpack.

- **Supported mods.** L_Ender's Cataclysm 3.33, Astral Dimension 3.0, Lionfish API 3.1 and Curios 9.5.1. Other NeoForge
  mods are not supported and not tested.
- **Lock.** Nytheria Compat refuses any other NeoForge mod and needs Nytheria's core mods to start. The lock is in the
  released jar.
- **Less interference.** NeoForge data (overlays, load conditions, biome and structure modifiers, global loot modifier
  lists, data maps) only applies to the four mods and to Nytheria Compat's own packs. Fabric mods, data packs and all other
  packs behave as on plain Fabric. The JVM option `-Dnytheriacompat.neoDataFromFabricMods=true` brings the old behaviour back.
- **Vanilla recipes, loot tables and advancements** are no longer replaced by NeoForge's versions.
- **Smaller.** Parts of the loader, hooks and events that none of the four mods needs were removed or narrowed
  (1052 hooks removed, 19 events narrowed, 795 source files and 4 modules removed, counted against Sporran 1.0.8; method in
  `docs/CHANGES.md`).
- **Fixes.** A hang when stopping a dedicated server, `LivingDamageEvent.Post` firing on a blocked hit, and chunk
  ticket methods that threw.
- **Existing worlds** keep loading; the saved data key is unchanged.
- Status: checked by automated tests, server and client runs and the modpack harness; not yet play-tested by hand through a full playthrough.

Requires Minecraft 1.21.1, Fabric Loader 0.19.5 or newer, Fabric API and Fabric Language Kotlin. Astral Dimension needs
GeckoLib for Fabric. The NeoForge mods are not included.
