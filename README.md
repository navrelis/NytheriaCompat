<!-- Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md. -->
<!-- Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1. -->

# Nytheria Compat

Formerly Sporran. The mod id is now `nytheria_compat` (was `sporran`).

**Nytheria Compat is an internal dependency of the Nytheria modpack. It is not a general NeoForge loader and is not meant for
other packs.**

It runs four NeoForge 1.21.1 mods on Fabric and nothing else: L_Ender's Cataclysm, Astral Dimension, Lionfish API
and Curios. It refuses any other NeoForge mod, and it needs Nytheria's core mods to start.

Nytheria Compat is based on [Kilt](https://github.com/KiltMC/Kilt) by BluSpring, AlphaMode and the KiltMC contributors, under the
LGPL-2.1. Bugs in Nytheria Compat are Nytheria's: do not report them to the Kilt authors. If you want to run NeoForge mods on
Fabric, use [Kilt](https://github.com/KiltMC/Kilt).

Licence information and credits are at the end of this page; [docs/CHANGES.md](docs/CHANGES.md) lists what was changed.
It is experimental and may crash or break worlds. Back up your worlds first.

**You download the NeoForge mods yourself.** This project does not include or redistribute any mods. Some mod authors
do not want their mods used on Fabric; please respect that.

- [Scope](#scope)
- [What you need](#what-you-need)
- [Installation](#installation)
- [What goes in the mods folder](#what-goes-in-the-mods-folder)
- [Do not install together with](#do-not-install-together-with)
- [First start, cache and configuration](#first-start-cache-and-configuration)
- [Known limitations](#known-limitations)
- [Reporting bugs](#reporting-bugs)
- [For developers](#for-developers)
- [License and legal](#license-and-legal)
- [Credits](#credits)

## Scope

Nytheria Compat 1.0.0 runs these NeoForge 1.21.1 mods and no others:

- L_Ender's Cataclysm 3.33
- Astral Dimension 3.0
- Lionfish API 3.1
- Curios 9.5.1

Everything else in your game, Fabric mods, data packs and resource packs, stays plain Fabric and is meant to behave as
it does without Nytheria Compat.

## What you need

Everything in this table is required on **both the client and the server**, except where noted.

| Component | Version | Notes |
| --- | --- | --- |
| Minecraft | 1.21.1 | |
| Java | 21 | |
| [Fabric Loader](https://fabricmc.net/use/) | 0.19.5 or newer | `fabric.mod.json` only enforces 0.19.3, deliberately, but use 0.19.5 or newer. |
| [Fabric API](https://modrinth.com/mod/fabric-api) | 0.116.17+1.21.1 (tested) | |
| [Fabric Language Kotlin](https://modrinth.com/mod/fabric-language-kotlin) | 1.14.1+kotlin.2.4.20 (tested) | This version needs Fabric Loader 0.19.5 or newer. |
| Nytheria Compat (`NytheriaCompat-<version>.jar`) | 1.0.0 | Comes with Nytheria. The source of every release is in this repository ([tags](https://github.com/navrelis/NytheriaCompat/tags)). |

On top of that, you need the NeoForge 1.21.1 jars of the four mods above that you want to play. Get them from their
authors' official pages (for example their CurseForge or Modrinth pages), and respect each mod's licence and the wishes
of its author.

Astral Dimension needs **GeckoLib for Fabric** (tested with 4.9.3). The NeoForge GeckoLib jar is not needed.

## Installation

1. Install Fabric Loader 0.19.5 or newer for Minecraft 1.21.1.
2. Put Fabric API, Fabric Language Kotlin and `NytheriaCompat-<version>.jar` into your `mods` folder.
3. Put the NeoForge jars of the supported mods you want to play into the same `mods` folder.
4. Do the same on the server and on every client that joins it.
5. Start the game. The first start takes longer, see [First start](#first-start-cache-and-configuration).

### What goes in the mods folder

- `fabric-api`
- `fabric-language-kotlin`
- `NytheriaCompat-<version>.jar`
- The NeoForge 1.21.1 jars of the supported mods you want to play (see [Scope](#scope)).
- GeckoLib for **Fabric** at the matching version, if you play Astral Dimension.

The NeoForge GeckoLib jar is **not** needed. Nytheria Compat uses the Fabric one. If only the NeoForge GeckoLib is
present, the client refuses to start and shows a loader message asking for the Fabric version.

## Do not install together with

- Kilt - provides `neoforge` and the same NeoForge classes; install only one of them.
- **Embeddium**
- **Async**
- **Sinytra Connector**

## First start, cache and configuration

- On the first start Nytheria Compat remaps the NeoForge jars and Minecraft. This takes about 30-40 seconds
  longer than a normal start. Later starts use the cache.
- The cache is in `<game dir>/.nytheria_compat/`. It is safe to delete and is rebuilt on the next start.
- `config/nytheria_compat_overrides.json` is the optional override file (force-disabled mod IDs and dependency
  overrides). Override files and cache folders
  of other NeoForge-on-Fabric loaders are not read.
- NeoForge's data handling (`neoforge:overlays`, `neoforge:conditions`, biome and structure modifiers, global loot
  modifier lists, data maps) only applies to the NeoForge mods Nytheria Compat runs and to Nytheria Compat's own packs. Fabric mods
  (many ship NeoForge data next to their Fabric data), datapacks and all other packs behave as on plain Fabric, which
  ignores that data. In those packs a file with both `fabric:load_conditions` and `neoforge:conditions` is decided by
  Fabric API alone; a file with only `neoforge:conditions` is still checked, so recipes for missing mods stay out. Data
  map types that Fabric mods register through NeoForge Data Pack Extensions are still read from every pack. The
  log says what was left out ("Ignored ... not NeoForge mods"). The JVM option `-Dnytheriacompat.neoDataFromFabricMods=true`
  applies that data from every pack again, as earlier versions did. The names of 1.0.8 (`-Dsporran.*`) still work. <!-- rename:keep -->
  The other `-Dnytheria_compat.*` options are listed in
  `src/main/kotlin/dev/nytheria/compat/loader/NytheriaCompatFlags.kt`.

## Known limitations

- It is experimental. Only the four mods under [Scope](#scope), in the versions listed there, are supported. Updating
  one of them to a different version may need re-testing.
- Some warnings in the log come from the mods themselves and also happen on NeoForge, for example missing
  translations and sounds, missing textures, unknown recipe categories and unknown tags.
- A few minor NeoForge event gaps remain. The full list is in [docs/CHANGES.md](docs/CHANGES.md).

## Reporting bugs

Report problems with Nytheria Compat to Nytheria.
**Do not report them to the authors of Kilt, or to the authors of the mods you run through Nytheria Compat.**
They do not support running their mods on Fabric through Nytheria Compat, and most issues are caused by the compatibility layer rather than by the mods themselves.

Please include your `latest.log` and the versions of every mod in your `mods` folder.

## For developers

### Building

```
git clone <repository URL>
cd <repository folder>
./gradlew remapJar
```

Use JDK 21. The NeoForge, FancyModLoader and Knit Loader sources are vendored in `forge/`, `fml/` and `loader/` (see [VENDORED.md](VENDORED.md)), no submodules are needed.

The jar ends up in `build/libs/` as `NytheriaCompat-<version>.jar` (ignore the `-cichlid-libs` jar). Add
`-Pbuild.release=true` to drop the `-local.<commit>` suffix from the version.
Use `remapJar` only. The root `build` and `shadowJar` tasks are not used and are known to fail.

### Testing

The automated test harness (dedicated server, client and modpack self-tests) is kept in the development repository and is not part of the public source release.

## License and legal

- This project is licensed under the **GNU Lesser General Public License v2.1** (LGPL-2.1). See [LICENSE](LICENSE).
- It is a modified version of an LGPL-2.1 project (see [NOTICE](NOTICE)). The modifications are listed in [docs/CHANGES.md](docs/CHANGES.md).
- It includes [NeoForge](https://github.com/neoforged/NeoForge) code (LGPL-2.1) and [Porting Lib](https://github.com/Fabricators-of-Create/Porting-Lib), together with the other libraries listed in `build.gradle.kts`, under their respective licenses.
- **No third-party mods are included in or redistributed by this project.** NeoForge mods must be obtained from their authors, and each mod's licence and its author's wishes must be respected.
- Nytheria Compat is an independent compatibility layer. It is **not affiliated with, endorsed by, or supported by any mod author**, nor by Mojang, Microsoft, NeoForged, FabricMC or the authors of the projects Nytheria Compat is based on.

## Credits

Nytheria Compat is licensed under the LGPL-2.1 ([LICENSE](LICENSE)). It is based on [Kilt](https://github.com/KiltMC/Kilt) 21.1.13 by BluSpring, AlphaMode and the KiltMC contributors, and
bundles NeoForge, FancyModLoader, [Porting Lib](https://github.com/Fabricators-of-Create/Porting-Lib) and other
libraries under their own licences; the full notices are in [NOTICE](NOTICE).

For compatibility with Fabric mods that check for Kilt (Twilight Forest, Stellar View), Nytheria Compat also provides the mod ID
`kilt` and keeps one interface under Kilt's package name; see [docs/CHANGES.md](docs/CHANGES.md).
