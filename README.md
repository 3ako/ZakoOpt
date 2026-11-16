<div align="center">
  <img src="common/src/main/resources/assets/zakoopt/icon.png" width="128">
  <h1>ZakoOpt</h1>

  <img src="https://img.shields.io/badge/Loader-Fabric-lightyellow">
  <img src="https://img.shields.io/badge/Loader-NeoForge-orange">
  <img src="https://img.shields.io/badge/Environment-Client-purple">
  <a href="https://github.com/3ako/ZakoOpt/blob/main/COPYING.LESSER"><img src="https://img.shields.io/badge/License-LGPL--3.0-blue"></a>
  <br>
  <a href="https://modrinth.com/mod/zakoopt"><img src="https://img.shields.io/modrinth/dt/zakoopt?logo=modrinth&label=Modrinth&color=158000"></a>
  <a href="https://www.curseforge.com/minecraft/mc-mods/zakoopt"><img src="https://cf.way2muchnoise.eu/full_1731365_downloads.svg"></a>
  <a href="https://github.com/3ako/ZakoOpt/releases/latest"><img src="https://img.shields.io/github/v/release/3ako/ZakoOpt?label=GitHub"></a>

  <p><b>A client-side mod that takes CPU load off rendering entities, block entities, particles and the HUD.</b>
  <br>Built for busy servers: crowded spawns, mob farms, piles of items, holograms, walls of signs and big Create factories.</p>
</div>

![ZakoOpt](https://media.forgecdn.net/attachments/description/1731365/description_94d47b82-6f79-4c14-ad6d-befe82abbd89.gif)

## Performance

ZakoOpt helps when the CPU is the bottleneck: lots of things on screen, a mid-range or older CPU. If your GPU is the limit or the scene is empty, expect little difference.

### Signs
_A wall of 90,000 signs with varied text, Minecraft 1.21.11, Sodium + ImmediatelyFast._

| Build             | FPS  |
|-------------------|------|
| Without ZakoOpt   | 1    |
| ZakoOpt 2.1.2     | 17   |
| ZakoOpt 2.1.3+    | ~850 |

### Create factories
_16 Create factories on screen, NeoForge 1.21.1._

| Without ZakoOpt | With ZakoOpt | Improvement |
|-----------------|--------------|-------------|
| 13 FPS          | 91 FPS       | 7x          |

### Crowded server
![Crowded server](https://media.forgecdn.net/attachments/description/1731365/description_3ba251cb-3c7e-4133-86e0-94663ca527f0.gif)

_The same replay with and without the mod: Ryzen 9 7950X3D limited to 12 threads, RTX 5080, 1920×1080, Sodium 0.8.14 + Lithium 0.21.4 + ImmediatelyFast 1.14.3, Minecraft 1.21.11._

![Create](https://media.forgecdn.net/attachments/description/1731365/description_dc31535a-4783-4432-b84b-08f9964fe63f.gif)

## Optimizations

- **Entities:** LOD for far mobs and players, a shared player skin atlas, models built on several CPU cores (enchantment glint included).
- **Items:** LOD for far dropped items, no per-frame face sorting, fewer allocations, optional "one copy per stack".
- **Signs:** boards baked into the chunk mesh, text of a whole chunk section uploaded to the GPU once.
- **Blocks:** chests baked into the chunk mesh, cached spawner mobs, piston-moved blocks and breaking cracks.
- **Particles:** LOD, light computed once per tick, built on several cores.
- **Text and HUD:** cached name tags, holograms, chat and scoreboard; HUD and animated GUI items redraw at your monitor's refresh rate.
- **Create:** items on belts, filter slots and gauge paths drawn once and replayed; machines past 80 blocks skip their client animation tick (the server keeps running them).
- **Rendering:** ring vertex buffer, one GPU sync per frame, fewer framebuffer switches.

Every optimization has its own toggle and tooltip: **Video Settings → ZakoOpt** with Sodium, or Mod Menu without it. Some are not available on every version.

**F6** shows an FPS and frame-time overlay, **Shift+F6** turns ZakoOpt off and on to compare.

## Compatibility

| Minecraft                    | Loader           |
|------------------------------|------------------|
| 26.1.2, 26.2, 26.3           | Fabric, NeoForge |
| 1.21.4, 1.21.8, 1.21.10, 1.21.11 | Fabric       |
| 1.21.1                       | Fabric, NeoForge |

- **Required:** ImmediatelyFast, Fabric API (on Fabric).
- **Optional:** Sodium. Works with Iris, Lithium, VulkanMod, Create.
- Client-side only, no need to install it on the server.

## FAQ

**My FPS went down.** Turn options off one by one in the ZakoOpt settings to find the one that hurts, then open an issue.

**Something looks wrong.** Options that change visuals say so in their tooltip. Turn that option off and report it.

## Bugs

[GitHub Issues](https://github.com/3ako/ZakoOpt/issues): your game version, mod list and `latest.log` or crash report.

## License

GNU LGPL-3.0 (`COPYING.LESSER`, together with `COPYING`). When distributing or making derivative works, keep `NOTICE`: the author credit and the channel link.

Author: Zako · [Telegram](https://t.me/StarikZako)
