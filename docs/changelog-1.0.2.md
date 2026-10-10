# ZakoOpt 1.0.2

## Русский

**Главное**
- **Sodium больше не обязателен.** Мод работает без него (нужен только ImmediatelyFast), с VulkanMod (1.21.4, 1.21.10, 1.21.11, 26.1.2) и со встроенным Vulkan в 26.2.
- **Модели мобов собираются на нескольких ядрах** — во всех версиях, кроме 1.21.8. В толпе мобов в броне это +20–50% FPS. Без Sodium и с VulkanMod прирост больше всего.

**Выброшенные предметы**
- Грани выброшенных предметов больше не сортируются каждый кадр: на больших кучах предметов до +40% FPS. Опция «Без сортировки предметов», включена по умолчанию.
- Новая опция «Одна копия выброшенного стака»: стак рисуется одним предметом вместо пяти. Кучи предметов рисуются в разы быстрее, но стаки выглядят тоньше. По умолчанию выключена.
- 1.21.10–1.21.11: размеры модели предмета больше не пересчитываются каждый кадр.

**1.21.4**
- Ники, голограммы, чат, сайдбар и таблички: строки текста кэшируются и не собираются заново каждый кадр (+35% на скоплении голограмм).
- Атлас скинов: скины игроков сводятся в одну текстуру.
- Блоки, которые двигают поршни, кэшируются (до 4 раз больше FPS на поршневых фермах). Освещение у них плоское; отключается опцией «Кэш движущихся блоков».
- Мобы в спавнерах кэшируются и не перерисовываются целиком каждый кадр.
- Открытый чат рисуется в два прохода вместо двух на каждую строку.
- В F3 появились счётчики вызовов отрисовки за кадр.

**Настройки**
- Новая опция «Кольцевой буфер всегда»: держит кольцевой буфер включённым, даже если автоматическая проверка его выключила. Помогает, если в тяжёлых местах с ним быстрее.

**Исправления**
- Игра могла намертво зависнуть на видеокартах Intel.
- HUD становился полупрозрачным и выцветшим вместе с модом Gnetum (например, в клиенте Ogulniega). Теперь кэш HUD в этом случае отключается сам.
- HUD моргал с VulkanMod.

---

## English

**Highlights**
- **Sodium is no longer required.** The mod runs without it (only ImmediatelyFast is needed), with VulkanMod (1.21.4, 1.21.10, 1.21.11, 26.1.2) and with the built-in Vulkan backend of 26.2.
- **Mob models are built on several CPU cores** on every version except 1.21.8: +20–50% FPS in crowds of armoured mobs, most of all without Sodium and with VulkanMod.

**Dropped items**
- Dropped item faces are no longer sorted every frame: up to +40% FPS on big item piles. Option "No dropped item sorting", on by default.
- New option "One copy per dropped stack": a stack is drawn as one item instead of up to five. Item piles draw several times faster, but stacks look thinner. Off by default.
- 1.21.10–1.21.11: item model bounds are no longer recomputed every frame.

**1.21.4**
- Name tags, holograms, chat, scoreboard and signs: lines of text are cached instead of rebuilt every frame (+35% around many holograms).
- Player skin atlas: player skins are packed into one texture.
- Blocks moved by pistons are cached (up to 4x FPS on piston farms). Their lighting is flat; turn it off with "Moving block cache".
- Mobs inside spawners are cached instead of fully redrawn every frame.
- Open chat draws in two passes instead of two per line.
- F3 shows draw call counters per frame.

**Settings**
- New option "Always use the ring buffer": keeps the vertex ring buffer on even when the automatic test turned it off. Useful when it is faster in your heavy scenes.

**Fixes**
- The game could freeze completely on Intel graphics.
- With Gnetum (shipped by the Ogulniega client, for example) the HUD turned see-through and washed out. The HUD cache now turns itself off next to Gnetum.
- The HUD flickered with VulkanMod.
