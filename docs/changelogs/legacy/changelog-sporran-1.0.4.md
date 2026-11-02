## Sporran 1.0.4

Faster chunk building and three fixes.

- **Faster chunk building with Sodium.** While chunks are built, Sporran checks for every block, face and model
  whether it uses one of NeoForge's rendering features (custom ambient occlusion, quads, model data, render types,
  light emission, face hiding, fluid overlays). These checks were needlessly expensive: in a flight through a large
  modpack they took 14 % of the working time of Sodium's chunk build threads and produced about a quarter of the game's
  short-lived memory garbage. They now cost next to nothing, so new chunks appear a little sooner and the garbage
  collector has less to do. Nothing behaves differently.
- The same improvement for the vanilla chunk builder (without Sodium), the light engine, the lightmap and the
  rendering of items, armor, elytra, boats and container screens.
- All remaining checks of this kind got cheaper as well.
- **Fixed a server crash** (`StackOverflowError: Ticking entity`) when a magma cube swims in a fluid other than lava,
  for example in a fluid added by a NeoForge mod.
- **Fixed: chest recipes with an empty chest slot.** In the first world after starting the game (and on a server
  until the next `/reload`), the recipes for the trapped chest, the chest minecart, the chest boats and the bamboo chest
  raft could lose their chest: recipe viewers showed an empty slot and logged errors, and servers sent the recipes to
  players that way. It happened when another mod looked at the recipes while they were still loading. These recipes
  now always list the wooden chests.
- **Fixed: an error stack trace in the log on every start** (`AnalyzerException: Insufficient maximum stack size`) in
  packs where another mod changes how liquids spread. It was harmless but looked like a crash.
