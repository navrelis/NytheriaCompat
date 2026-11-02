<!-- Changed by the Nytheria Compat project between 2026-09-29 and 2026-11-02; see docs/CHANGES.md. -->
<!-- Based on Kilt by KiltMC (https://github.com/KiltMC/Kilt). Licensed under the GNU LGPL 2.1. -->

# Nytheria Compat Compatibility Modules
This folder contains the compatibility layers Nytheria Compat 1.0.0 keeps. The bridges for Forge Energy,
Trinkets, other Forge mods, Create and most other Fabric mods were removed: they served mods that are
not targets of Nytheria Compat 1.0.0.

## Supported Bridges
| Mods                                                                                | Bridge                                            |
|-------------------------------------------------------------------------------------|---------------------------------------------------|
| NeoForge `ModConfigSpec` users (Cataclysm, Curios), Forge Config API Port (Fabric)  | Nytheria Compat: NeoForge Config API (`forge-config-api`) |
| Sophisticated Core (Fabric port), which the pack runs as a Fabric mod               | Nytheria Compat: Fabric Mods Compat (`fabric-compats`)    |
