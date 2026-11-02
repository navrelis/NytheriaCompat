## Sporran 1.0.8

One fix.

- **Fixed: Fabric mods that carry both Fabric and NeoForge overlay lists had their NeoForge overlays applied on top.**
  Lithostitched 1.8.1 is one of them: its Fabric overlay list switches the debug suite and the seed-parity overlay on
  only when the mod's config asks for it, but Sporran also read the NeoForge list, could not understand the Fabric
  config condition there and treated it as "true". The debug suite was therefore always on: armadillos in every
  Overworld biome, sky islands and more. Sporran now leaves the overlays of a pack that declares `fabric:overlays` to
  Fabric API and its conditions. Chunks that were generated before this fix keep what was generated.

- **Terralith: config toggles now take effect.**
  Terralith 2.6.2 declares six overlays in both lists, gated by its `terralith:config` condition. Up to 1.0.7 Sporran
  treated that unknown condition as "true" and always applied all six, whatever `config/terralith.json` said: custom
  structures, skylands and terrain slabs were off, recipe changes and vanilla stone generation were on, and the intro
  message was off. From 1.0.8 Terralith follows its config file. With the default config, new chunks therefore get
  Terralith's structures, skylands biomes, terrain slabs and stone generation (seams against chunks generated before),
  and Terralith's recipe changes are gone. To keep the previous behaviour, set under `"modules"` in
  `config/terralith.json`: `custom_structures` false, `skylands` false, `terrain_slabs` false, `recipe_changes` true,
  `vanilla_stone_gen` true, `intro_message` false.