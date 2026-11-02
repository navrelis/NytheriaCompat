<!-- Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md. -->
<!-- Based on Knit Loader by KiltMC (see VENDORED.md). Licensed under the MIT licence (loader/LICENSE). -->

# Knit Loader

A mod loader abstraction for bridge-type mods: it scans, sorts and injects mods made for another mod loader into the
native one. Nytheria Compat's `NytheriaCompatLoader` extends Knit's `KnitModLoader` to load NeoForge mods on Fabric.

Modules built by this repository:
- `:loader` (common code, package `xyz.bluspring.knit`)
- `:loader:fabric` (Fabric Loader integration, nested in the Nytheria Compat jar)
- `:loader:cichlid` (CichlidMC integration, not used at runtime)

Knit Loader is licensed under the MIT license, see [LICENSE](LICENSE). Origin and upstream commit: see
[VENDORED.md](../VENDORED.md).
