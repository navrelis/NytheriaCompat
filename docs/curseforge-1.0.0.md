**Nytheria Compat (formerly Sporran) is an internal dependency of the Nytheria modpack. It is not a general NeoForge loader and is not meant for other packs.**

It runs four NeoForge 1.21.1 mods on Fabric 1.21.1 and nothing else: L_Ender's Cataclysm, Astral Dimension, Lionfish API and Curios. It refuses any other NeoForge mod, and it needs Nytheria's core mods to start.

If you want to run NeoForge mods on Fabric, use [Kilt](https://www.curseforge.com/minecraft/mc-mods/kilt).

## Scope

- The mod is locked: it loads only the four NeoForge mods above and needs Nytheria's core mods. Any other NeoForge mod is refused at startup.
- Everything else behaves as on plain Fabric. NeoForge data (overlays, conditions, biome and structure modifiers, loot modifier lists, data maps) applies only to the four mods and to the mod's own packs, and NeoForge's copies of vanilla recipes, loot tables, advancements and data maps are not applied.
- It is much smaller than the earlier Sporran builds (1.0.8): 1052 of 1706 hooks removed, 19 events narrowed to the cases the four mods use, 795 of 2604 source files and 4 modules removed. The counting method is in `docs/CHANGES.md` in the source.

## Based on Kilt

Nytheria Compat is based on [Kilt](https://github.com/KiltMC/Kilt) 21.1.13 by [BluSpring](https://www.curseforge.com/members/bluspring), AlphaMode and the KiltMC contributors, under the GNU LGPL 2.1. It is a cut-down, modified version made for Nytheria.

**Bugs in Nytheria Compat are Nytheria's. Do not report them to the Kilt authors.**

Report problems with Nytheria Compat on its [issue tracker](https://github.com/navrelis/NytheriaCompat/issues).

## Licence and source code

- Free software under the **GNU LGPL-2.1** (see the License tab).
- The complete source code of every release is attached to its file on this page ("Additional Files", `-sources.zip`) and is also on [GitHub](https://github.com/navrelis/NytheriaCompat), with one tag per release.
- Modified files carry a dated change notice, and `docs/CHANGES.md` in the source lists the changes compared with Kilt 21.1.13. Notices of Kilt, NeoForge, FancyModLoader, Porting Lib and other bundled libraries are in the `NOTICE` file in the source and in the jar.
- Nytheria Compat does not include or redistribute any other mod. It is not affiliated with or endorsed by Kilt, NeoForged, Mojang or any mod author.
- No warranty: Nytheria Compat is provided "as is", to the extent permitted by applicable law; see sections 15 and 16 of the LGPL-2.1.
