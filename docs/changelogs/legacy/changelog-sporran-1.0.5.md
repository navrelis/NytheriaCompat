## Sporran 1.0.5

One fix.

- **Fixed: the screen shake of NeoForge mods spun the picture.** Mods that shake the screen (boss slams, for example)
  tilt the camera by a few degrees. Sporran passed that tilt to the game in the wrong unit and with the wrong sign, so
  it was 57 times too strong: the picture turned by a full circle and more, many times a second, and nothing could be
  seen. The tilt now matches NeoForge: a shake of 7.5 degrees tilts the view by 7.5 degrees.
- A camera tilt is no longer kept while a player sleeps in a bed (NeoForge resets it there as well).
