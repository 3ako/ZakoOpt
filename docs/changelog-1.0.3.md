# ZakoOpt 1.0.3

## Русский

**Новое**
- **Поддержка Minecraft 26.3.** Работает с Sodium 0.9.2+ (в том числе со сборкой из Lunar), без Sodium и с Iris.
- **Пакетная отрисовка чанков в Lunar.** На видеокартах NVIDIA под Windows Lunar рисует ландшафт по одному вызову на кусок вместо одной пакетной команды. ZakoOpt возвращает пакетную отрисовку: в тесте кадр ландшафта стал до 39% быстрее. Работает в Lunar на 1.21.x и 26.x, без Lunar ничего не меняет. Опция «Пакетная отрисовка чанков в Lunar», действует после перезапуска.
- **Ранний отсев дальних блок-сущностей.** Сундуки, таблички и головы дальше своей дальности прорисовки отбрасываются первой же проверкой, раньше хуков других модов. Заметнее всего в Lunar. 1.21.10 и новее.
- Отдельная опция «Многопоточные модели»: теперь её можно выключить, не трогая многопоточные частицы.
- Окно «Доступно обновление» ведёт на страницу мода на CurseForge.

**Совместимость**
- Миникарта Xaero больше не рисует свою подложку поверх мира при включённом кэше HUD.
- 26.2 и новее: вылет с Xaero's World Map («Cannot wait on a fence for the current submit»). Опция «Один fence на кадр» теперь работает только на 26.1.2: начиная с 26.2 игра делает это сама.

**Исправления**
- Броня на дальних игроках и мобах рябила при включённой «Анимации дальних раз в тик».
- Вылет при отрисовке игроков с VulkanMod (1.21.4–1.21.11).
- Зачарованные предметы пропадали из хотбара при включённом кэше HUD (1.21.8 и новее).
- Утечки памяти после перезагрузки ресурсов (F3+T) и вылет на поршнях без Sodium на 1.21.10–1.21.11. Спасибо alexec0de.

---

## English

**New**
- **Minecraft 26.3 support.** Works with Sodium 0.9.2+ (including Lunar's build), without Sodium, and with Iris.
- **Batched chunk drawing in Lunar.** On NVIDIA cards under Windows, Lunar draws terrain with one call per piece instead of one batched command. ZakoOpt brings batching back: up to 39% faster terrain frames in testing. Works in Lunar on 1.21.x and 26.x, does nothing without Lunar. Option "Batched chunk drawing in Lunar", takes effect after a restart.
- **Early cull of far block entities.** Chests, signs and heads beyond their render distance are dropped by the very first check, before other mods' hooks. Most noticeable in Lunar. 1.21.10 and newer.
- Separate "Multithreaded models" option: it can now be turned off without touching multithreaded particles.
- The "Update available" dialog now opens the mod's CurseForge page.

**Compatibility**
- Xaero's Minimap no longer draws its backdrop over the world with the HUD cache on.
- 26.2 and newer: crash with Xaero's World Map ("Cannot wait on a fence for the current submit"). The "One fence per frame" option now only runs on 26.1.2: from 26.2 the game does this itself.

**Fixes**
- Armour on far players and mobs flickered with "Far animation per tick" on.
- Crash rendering players with VulkanMod (1.21.4–1.21.11).
- Enchanted items vanished from the hotbar with the HUD cache on (1.21.8 and newer).
- Memory leaks after a resource reload (F3+T) and a piston crash without Sodium on 1.21.10–1.21.11. Thanks to alexec0de.
