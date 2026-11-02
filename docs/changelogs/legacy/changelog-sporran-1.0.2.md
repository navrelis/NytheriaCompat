## Sporran 1.0.2

Compatibility fix for Fabric mods that change vanilla behaviour. Continues the work of 1.0.1.

Many Sporran hooks replaced vanilla code even when no NeoForge mod overrode or listened to it. Fabric mods that hook
the same vanilla code were then skipped. Now Sporran only takes over when a NeoForge mod needs it:

- **Vanilla checks.** About 40 checks (sounds, friction, fire spread, light, explosion resistance, path finding, item
  damage and stack size, food, shields, bundles and more) wrongly took the NeoForge path for every block, item and
  entity. Fabric mods that change these work again on vanilla content. NeoForge mods that override them still work as
  before. A few checks that could never find an override now find real ones.
- **Placing blocks with items.** Fabric mods that change what happens when you place a block from an item work again.
  NeoForge place events still fire and can still cancel the placement.
- **Breaking blocks.** Fabric mods that change whether a tool harvests a block work again.
- **Hoppers.** Hoppers use vanilla's code again for plain vanilla containers. Containers from NeoForge mods keep
  working.
- **Fluids, biomes and vibrations.** Vanilla's code runs again for eye-in-fluid checks, biome mob and generation
  settings and vibration frequencies. NeoForge fluid types and biome modifiers are applied on top.
- **Block rendering.** Plain blocks use vanilla's and Sodium's render paths again. Only blocks that ask for special
  render layers use NeoForge's.
- **Smaller fixes.** Container data values reach Fabric mods' hooks again, and NeoForge mods that call the parent of a
  custom ingredient no longer crash.

Requires Minecraft 1.21.1, Fabric Loader, Fabric API and Fabric Language Kotlin. NeoForge mods are not included; get
them from their authors.
