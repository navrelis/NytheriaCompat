## Sporran 1.0.7

One fix.

- **Fixed: crash when a block or its item is drawn with connected textures.** A NeoForge extension method of blocks
  (the one a block uses to report which block it looks like) collided with the same method of Fabric blocks that also
  customise their reported appearance. The game then stopped with "Conflicting default methods" as soon as such a block,
  or its item in the creative inventory, was drawn with connected textures. Sporran now uses the one method both sides
  share, so the block reports its appearance as before and the crash is gone.
