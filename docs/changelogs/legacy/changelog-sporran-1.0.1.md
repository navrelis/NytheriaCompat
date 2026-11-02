## Sporran 1.0.1

Compatibility fix for Fabric mods that change enchanting or the grindstone.

Three Sporran hooks replaced vanilla code for every item, even when no NeoForge mod needed them. Fabric mods that hook
the same vanilla code were then skipped. Now Sporran only takes over when a NeoForge mod overrides the method or
changes the NeoForge event:

- **Enchantment levels.** Fabric mods that change an item's enchantment level (for example to switch an enchantment
  off) work again on vanilla items. NeoForge mods that change enchantment levels still work as before.
- **Enchanting table.** Fabric mods that change which enchantments the enchanting table offers for an item work again.
- **Grindstone.** Taking the result of a grindstone runs vanilla's code again unless a NeoForge mod changes it. With a
  Fabric mod that grinds enchanted books one at a time, the rest of the stack is no longer lost.
- **Other vanilla hooks.** Four more places (air checks, shulker box slots, dripstone filling cauldrons, key
  bindings without modifier keys) no longer replace vanilla code when no NeoForge mod needs it.

Requires Minecraft 1.21.1, Fabric Loader, Fabric API and Fabric Language Kotlin. NeoForge mods are not included; get
them from their authors.
