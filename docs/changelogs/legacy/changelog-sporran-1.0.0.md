## Sporran 1.0.0

First release of the relaunched Sporran project.

Sporran is a modified version of Kilt 21.1.13 and is fully LGPL-2.1 compliant: the complete source code is public on
GitHub, every modified upstream file carries a dated change notice, the licence notices of the bundled projects are in
the `NOTICE` file inside the jar, and `docs/CHANGES.md` lists everything that differs from Kilt. This release replaces
the earlier builds 1.0.0 to 1.0.8, which have been withdrawn; everything they fixed is included.

- **Mobs and projectiles.** Mobs between 0.75 and 1 block wide (also cows, pigs and sheep) walk along their paths again,
  arrows and thrown weapons of NeoForge mods behave as they do on NeoForge, and arrow impacts fire the NeoForge impact
  event. A server crash with magma cubes in modded fluids and a crash on saving after a potion effect ran out are fixed.
- **Events.** Block drops, player ticks, critical hits, interactions, explosions, respawns and entity deaths fire
  exactly once and with the right data, including for Fabric and Architectury mods that listen to them.
- **Camera and rendering.** Screen shake tilts the view by the intended few degrees instead of spinning it, and the
  "Conflicting default methods" crash with connected textures is gone.
- **Recipes and networking.** Chest recipes no longer lose their chest in the first world, and joining a server no
  longer fails with "Registry remapping failed" next to some Fabric mods.
- **Data packs and world generation.** Fabric mods that carry both Fabric and NeoForge overlay lists no longer get
  switched-off overlays applied, structures with skull checks generate, and a rare world generation hang is fixed.
  Terralith now follows its config file (see `docs/CHANGES.md` for how to keep the old behaviour).
- **Faster.** Chunk building with Sodium and other rendering checks are much cheaper; our tests show a first start
  about 32 % faster and a cached start about 11 % faster.
- **Quieter log**, and a profiler fix for spark's mod sources view.

Requires Minecraft 1.21.1, Fabric Loader, Fabric API and Fabric Language Kotlin. NeoForge mods are not included; get
them from their authors.
