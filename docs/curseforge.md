# ZakoOpt

**Client-side optimization addon for Sodium.** ZakoOpt targets the places where Sodium alone still struggles: crowded spawns full of players, mob farms with spawners and pistons, particle-heavy scenes and the HUD.

## Demo

[![ZakoOpt before / after](https://img.youtube.com/vi/VIDEO_ID/maxresdefault.jpg)](https://www.youtube.com/watch?v=VIDEO_ID)

*Left: Sodium + Lithium + ImmediatelyFast. Right: the same plus ZakoOpt. Same replay, same moment.*

## Results

Same game session and same ReplayMod recordings, so every pass shows exactly the same scene. Each mode was measured over 4 runs, and the mod was switched on and off within the session.

| Scene | FPS without → with | 1% low without → with |
|---|---|---|
| Busy spawn (~55 players) | 549 → 993 (**+81%**) | 344 → 570 (**+66%**) |
| Mob farm (mobs, spawners, pistons, particles) | 125 → 438 (**+251%**) | 66 → 190 (**+190%**) |
| Flying over the world | 1346 → 2426 (**+80%**) | 761 → 1214 (**+60%**) |

With **Iris + Complementary Reimagined** shaders: spawn **+65%**, mob farm **+115%**, flying **+57%** FPS.

*Baseline: Sodium 0.8.14 + Lithium 0.21.4 + ImmediatelyFast 1.14.3. Ryzen 9 7950X3D, RTX 5080, 854×480. Your numbers will differ with hardware and resolution. The more entities, block entities and particles on screen, the bigger the gain.*

## What it does

**Entities and players**
- Entity LOD: beyond a set distance, mobs and players skip armour, held items, capes and the outer skin layer.
- Player skin atlas: skins are packed into one texture, so the crowd draws in fewer batches.
- Item LOD: dropped items far away are simplified.

**Block entities and farms**
- Spawners: far spawners stop drawing the mob inside and spin once per tick. Near spawners are cached.
- Piston-moved blocks: geometry is cached, and lighting is flat.
- Signs, chests and other block entities: lookups are cached per tick.

**Particles**
- Particle light is computed once per tick, and particle physics is simplified.
- Particle LOD.
- Particles and their vertices are built on several threads.

**Rendering pipeline**
- A ring vertex buffer with no extra memory copies.
- One GPU fence per frame.
- Fewer framebuffer binds and clears.
- The entity outline pass is skipped when nothing glows.

**Interface**
- The HUD and animated GUI items redraw at monitor refresh rate instead of every frame.
- Text layout is cached.

Every optimization has its own toggle.

## Settings

Everything is in **Video Settings → ZakoOpt** (Sodium's options screen):
- **Enable all** turns every optimization on or off in one click.
- Each optimization has its own toggle and a tooltip explaining what it changes.
- Distance sliders set the entity, item and spawner LOD.

Settings are saved in `config/zakoopt.json`.

## Requirements

- Minecraft **1.21.11**, Fabric Loader **0.16+**
- [Sodium](https://modrinth.com/mod/sodium) **0.8+** (required)
- [ImmediatelyFast](https://modrinth.com/mod/immediatelyfast) (required)
- [Fabric API](https://modrinth.com/mod/fabric-api) (required)

**Works with:**
- Lithium
- Sodium Extra
- Reese's Sodium Options
- Iris shaders. With a shaderpack on, one vertex-buffer optimization switches itself off automatically.

Client-side only: no server install needed, and it works on any server.

## FAQ

**Does it change how the game looks?**
Only the LOD options do, and only at a distance. Each one can be turned off or have its distance raised, for example for high resolutions.

**Is it safe on servers with anti-cheat?**
ZakoOpt only changes rendering and sends nothing to the server.

**Found a bug or a crash?**
Send the crash report or `latest.log` to the author via [Telegram](https://t.me/StarikZako).

## License

[LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html). The mod is free. If you fork it or use the code, keep the author credit: **Zako, [t.me/StarikZako](https://t.me/StarikZako)**.

---

📢 **Author's Telegram channel:** [t.me/StarikZako](https://t.me/StarikZako): news, benchmarks and new builds first.
