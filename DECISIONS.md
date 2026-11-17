# Decisions

One line per decision, newest last within each section, with the number that settled it.
Scenes: R2-farm (mob farm), R3-fly (flight), L-chests (~900 chests), the 90 000-sign wall, the 16-factory Create district.
FPS A/B on one machine moves ±30 FPS around 300 between runs: a difference inside that is noise, not a result.

## Kept

- **Mixin handlers guarded** (2026-10-13): a throwing handler turns its feature off instead of crashing. Entity and
  particle handlers stay unguarded: wrapped, they cost 5% FPS with mobs.
- **HUD cache blends in one full-screen pass** (2026-10-15): the per-tile blend (60×34 tiles at 1080p) cost more than it
  saved, −5.7% FPS on R3-fly.
- **Create: client tick of far machines skipped, belts/filters/gauge paths replayed**: 16-factory district 13 → 91 FPS
  (NeoForge 1.21.1).
- **Fewer allocations** (2026-11-06): render-thread garbage on R2-farm 230 → 104 KB/frame (1.21.4), 335 → 121 KB/frame
  (1.21.8); FPS within noise, GC at 0.3% of the frame. The aim is fewer and shorter GC pauses, not average FPS.
- **Shut chests baked into Sodium's chunk mesh** (2026-11-06): L-chests 1095 → 2592 FPS, p99 1.74 → 0.83 ms.
  Off under Iris; a failure turns it off for the session instead of crashing the chunk mesher.
- **Block entities behind solid blocks skipped** (2.1.0): up to ×11 FPS in hubs with signs behind walls.
- **Text caches rest while they mostly miss** (2.1.0): a hub of signs and holograms was slower with the mod than without.
- **Signs baked per chunk section** (2.1.3): board in the chunk mesh, the section's text uploaded once and drawn with a
  couple of calls. 90 000-sign wall: 1 FPS vanilla, 17 FPS on 2.1.2, ~850 FPS.
- **Item single copy is an option, off by default**: a dropped stack drawn as one item is 3× faster on a pile, but it
  changes the look.
- **Item LOD leaves items in hands alone by default** (2.1.4): the flat far sword in other players' hands was noticed;
  "Items in hands" turns it back on.
- **Parallel models need at least 24 submits a frame** (`MIN_SUBMITS`): below that the hand-off costs more than it saves.
- **Entities drawn in a GUI skip item LOD and the far-animation freeze** (2.1.8, #13, #26): their pose is in GUI
  coordinates, so every distance check reads them as far.

## Not kept

- **Block-entity lookup caches** (2026-11-06): renderer cached on the type plus skipping Sodium's empty crack map.
  R2-farm, 6 pairs: 312 vs 327 FPS, inside the noise. The hot method in JFR was where the cache missed, not the cause.
- **HUD tiles** (2026-10-15): see the HUD cache line above.

## Not yet acted on

- Static block-entity cache per section beyond chests and signs (shulkers, pots, banners): one draw per section.
- Batch entity submits by render type and atlas, one draw per material.
- F3 and the ZakoOpt overlay probably refresh every frame; mcrs refreshes its F3 10×/s.
