# ZakoOpt

![Ферма: без ZakoOpt и с ним](R2-farm.gif)

## Русский

**ZakoOpt — клиентский мод, который снимает нагрузку с процессора там, где игра упирается в отрисовку сущностей, блоков-сущностей, частиц и интерфейса.** Больше всего помогает на спавнах с толпой игроков, на фермах (мобы, спавнеры, поршни), при куче выброшенных предметов и голограмм.

Прирост зависит от компьютера и сцены. Если игра упирается в видеокарту или на экране мало всего, разницы может не быть. Если FPS упал, отключите опции по одной (у каждой есть подсказка) и сообщите о проблеме на GitHub.

![Спавн: без ZakoOpt и с ним](R1-spawn.jpg)

*Записи сделаны на одном и том же реплее: Ryzen 9 7950X3D, игре оставлено 12 потоков, RTX 5080, 1920×1080, Sodium 0.8.14 + Lithium 0.21.4 + ImmediatelyFast 1.14.3, Minecraft 1.21.11. На вашем компьютере цифры будут другими.*

**Что умеет**
- **Сущности:** упрощение дальних мобов и игроков (LOD), атлас скинов, модели собираются на нескольких ядрах.
- **Предметы:** упрощение дальних выброшенных предметов, без сортировки граней каждый кадр, опция «одна копия стака».
- **Фермы:** кэш мобов в спавнерах, кэш блоков, которые двигают поршни.
- **Частицы:** LOD, свет раз в тик, сборка на нескольких ядрах.
- **Текст и HUD:** кэш ников, голограмм, чата, сайдбара и табличек; HUD и анимированные предметы в GUI обновляются с частотой монитора.
- **Отрисовка:** кольцевой буфер вершин, одна синхронизация с видеокартой за кадр, меньше переключений фреймбуфера.

Каждая оптимизация включается и выключается отдельно: с Sodium — в **Настройках графики → ZakoOpt**, без него — через Mod Menu. Часть функций есть не на всех версиях.

**Требования:** Minecraft 1.21.4, 1.21.8, 1.21.10, 1.21.11, 26.1.2 или 26.2; Fabric API; ImmediatelyFast. Sodium — по желанию. Работает с VulkanMod, Iris, Lithium. Только клиент, на сервер ставить не нужно.

Баги и вылеты — в [Issues на GitHub](https://github.com/3ako/ZakoOpt/issues): версия игры, список модов и `latest.log` или краш-репорт.

---

## English

**ZakoOpt is a client-side mod that takes CPU load off rendering entities, block entities, particles and the HUD.** It helps most on busy spawns with crowds of players, on farms (mobs, spawners, pistons), and around piles of dropped items and holograms.

The gain depends on your PC and the scene. If your GPU is the bottleneck or there is little on screen, you may see no difference. If your FPS drops, turn options off one by one (each has a tooltip) and report it on GitHub.

![Spawn: without and with ZakoOpt](R1-spawn.jpg)

*Recorded on the same replay: Ryzen 9 7950X3D limited to 12 threads, RTX 5080, 1920×1080, Sodium 0.8.14 + Lithium 0.21.4 + ImmediatelyFast 1.14.3, Minecraft 1.21.11. Your numbers will differ.*

**Features**
- **Entities:** LOD for far mobs and players, player skin atlas, models built on several CPU cores.
- **Items:** LOD for far dropped items, no per-frame face sorting, optional "one copy per stack".
- **Farms:** cached spawner mobs, cached piston-moved blocks.
- **Particles:** LOD, light once per tick, built on several cores.
- **Text and HUD:** cached name tags, holograms, chat, scoreboard and signs; HUD and animated GUI items redraw at monitor refresh rate.
- **Rendering:** ring vertex buffer, one GPU sync per frame, fewer framebuffer switches.

Every optimization has its own toggle: with Sodium in **Video Settings → ZakoOpt**, without it through Mod Menu. Some features are not available on every version.

**Requirements:** Minecraft 1.21.4, 1.21.8, 1.21.10, 1.21.11, 26.1.2 or 26.2; Fabric API; ImmediatelyFast. Sodium is optional. Works with VulkanMod, Iris, Lithium. Client-side only.

Bugs and crashes: [GitHub Issues](https://github.com/3ako/ZakoOpt/issues), with your game version, mod list and `latest.log` or crash report.

---

[LGPL-3.0](https://www.gnu.org/licenses/lgpl-3.0.html). If you fork it or use the code, keep the credit: **Zako, [t.me/StarikZako](https://t.me/StarikZako)**.
