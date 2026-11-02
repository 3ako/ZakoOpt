[Download: GitHub Releases](https://github.com/3ako/ZakoOpt/releases/latest) | [Modrinth](https://modrinth.com/mod/zakoopt)

# ZakoOpt

![image](https://media.forgecdn.net/attachments/description/1731365/description_94d47b82-6f79-4c14-ad6d-befe82abbd89.gif)

**ZakoOpt is a client-side mod that takes CPU load off rendering entities, block entities, particles and the HUD.** It helps most on busy spawns with crowds of players, on farms (mobs, spawners, pistons), around piles of dropped items and holograms, and in big Create factories.

The gain depends on your PC and the scene. If your GPU is the bottleneck or there is little on screen, you may see no difference. If your FPS drops, turn options off one by one (each has a tooltip) and report it on GitHub.

![image](https://media.forgecdn.net/attachments/description/1731365/description_3ba251cb-3c7e-4133-86e0-94663ca527f0.gif)

_Recorded on the same replay: Ryzen 9 7950X3D limited to 12 threads, RTX 5080, 1920×1080, Sodium 0.8.14 + Lithium 0.21.4 + ImmediatelyFast 1.14.3, Minecraft 1.21.11. Your numbers will differ._

**Create (NeoForge 1.21.1)**

![image](https://media.forgecdn.net/attachments/description/1731365/description_dc31535a-4783-4432-b84b-08f9964fe63f.gif)

A factory district with 16 Create factories: **13 FPS without ZakoOpt, 91 FPS with it.**

*   Items on belts, filter slots and factory gauge paths are drawn once and replayed instead of rebuilt every frame.
*   Far machines rest: Create machines past 80 blocks skip their client-side animation tick (distance adjustable, 64–256). The server keeps the real state, so nothing stops working.
*   Package address matching no longer recompiles the address pattern on every check.
*   All Create optimizations have their own tab in the settings and can be turned off one by one. Works with Create Aeronautics (Sable).

**Features**

*   **Entities:** LOD for far mobs and players, player skin atlas, models built on several CPU cores, enchantment glint included.
*   **Items:** LOD for far dropped items, no per-frame face sorting, optional "one copy per stack", fewer allocations for dropped items.
*   **Farms:** cached spawner mobs, cached piston-moved blocks.
*   **Blocks:** cached block breaking cracks.
*   **Particles:** LOD, light once per tick, built on several cores.
*   **Text and HUD:** cached name tags, holograms, chat, scoreboard and signs; HUD and animated GUI items redraw at monitor refresh rate.
*   **Rendering:** ring vertex buffer, one GPU sync per frame, fewer framebuffer switches.
*   **Monitor:** F6 shows an FPS and frame-time overlay over the game, Shift+F6 turns ZakoOpt on and off on the fly to compare.

Every optimization has its own toggle: with Sodium in **Video Settings → ZakoOpt**, without it through Mod Menu. Some features are not available on every version.

**Requirements:** Minecraft 1.21.1, 1.21.4, 1.21.8, 1.21.10, 1.21.11, 26.1.2, 26.2 or 26.3. Fabric on every version (with Fabric API), NeoForge on 1.21.1 and 26.x. ImmediatelyFast is required, Sodium is optional. Works with VulkanMod, Iris, Lithium. Client-side only.

Bugs and crashes: [GitHub Issues](https://github.com/3ako/ZakoOpt/issues), with your game version, mod list and `latest.log` or crash report.

***

## License

GNU LGPL-3.0 (`COPYING.LESSER`, together with `COPYING`, the GPL-3.0 text it builds on). When distributing or making derivative works, keep `NOTICE`: the author credit and the channel link.

Author: Zako. Channel: https://t.me/StarikZako
