# Vendored upstream code

The folders `forge/`, `fml/` and `loader/` used to be git submodules. They are now plain folders of this repository,
so the code in them can be edited here. They contain only what the build uses, plus the licence and credit files.
The upstream history is not carried over; the exact upstream commit each folder was taken from is listed below.

The Gradle build compiles these folders as part of the main source set (`forge/`, `fml/`) or as the Gradle
projects `:loader`, `:loader:fabric` and `:loader:cichlid` (`loader/`). See `build.gradle.kts` and `settings.gradle.kts`.

| Folder | Upstream project | Upstream repository | Commit taken from | Licence |
|---|---|---|---|---|
| `forge/` | NeoForge (1.21.1 port maintained by the Kilt project, based on NeoForged's NeoForge) | https://github.com/KiltMC/NeoForge | `4be8620fa70266a640fb04f6e72c8407bb2d3866` (`20.4-913-g4be8620fa`) | LGPL-2.1-only, see `forge/LICENSE.txt` and `forge/README-LICENSE.md`; credits in `forge/CREDITS.txt` |
| `fml/` | FancyModLoader (Kilt project fork of NeoForged's FancyModLoader, branch `1.21.1`) | https://github.com/KiltMC/FancyModLoader | `99aa898d99d718835af56d9a304dc5a4576c6968` (`4.0-75-g99aa898d`) | LGPL-2.1, see `fml/LICENSE.txt` |
| `loader/` | Knit Loader | https://github.com/KiltMC/KnitLoader | `b5f3fb0ab687f18af4735e9a2f9c9ab8d07c0664` (`main`) | MIT, Copyright 2025 KiltMC, see `loader/LICENSE` |

Upstream of the forks: https://github.com/neoforged/NeoForge and https://github.com/neoforged/FancyModLoader.

## What was kept

- `forge/`: `src/` (`main` and `generated`), `LICENSE.txt`, `CREDITS.txt`, `README-LICENSE.md`.
  Dropped: `tests/`, `testframework/`, `buildSrc/`, `docs/`, `projects/`, `server_files/`, `codeformat/`, the Gradle
  build files and wrapper, `.github/`, `.idea/`. `coremods/` (no mod needs it) and `patches/` (only read by the removed dev
  tasks `countPatchProgress` and `tagPatches`, never compiled) were removed in 1.0.0; git history keeps them.
- `fml/`: `loader/src/main/`, `LICENSE.txt`, `LICENSE-header.txt`, `README.md`.
  Dropped: `earlydisplay/`, `junit/`, `tests/`, `loader/src/test/`, `buildSrc/`, the Gradle build files and wrapper,
  `.github/`.
- `loader/`: `build.gradle.kts`, `gradle.properties`, `cichlid/`, `fabric/`, `src/`, `LICENSE`, `README.md`.
  Dropped: `quilt/` (its module is disabled in `settings.gradle.kts`), the standalone `settings.gradle.kts`.

The files that are kept were taken with `git archive` from the commits above and were not changed when they were vendored.

## Changes after vendoring

The rename to Nytheria Compat (see `docs/CHANGES.md`) edited these folders afterwards: in `forge/` and `fml/` the references
to Nytheria Compat's own packages, classes and `nytheriacompat$` members, the launcher/branding strings and the `// Nytheria Compat:` patch
comments; in `loader/` the comments that named the upstream mod, the description and contact links in
`loader/fabric/src/main/resources/fabric.mod.json`, and `loader/README.md` (replaced by a short description).
The licence and credit files are unchanged.

Every file that was modified after vendoring carries a dated change notice in its header (see `docs/CHANGES.md`).
