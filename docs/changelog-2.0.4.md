# ZakoOpt 2.0.4

**English**
- Fixed Iris shaders: streaks across the screen and missing HUD text caused by the text cache (confirmed on 1.21.1).
- Fixed Iris shaders: missing HUD or a pale world when the outline pass was skipped (confirmed on 1.21.1).
- The immediate ring now takes its vertex format from the mesh, since Iris extends it under shaders.
- Fixed the skin atlas overflowing its buffer with glowing players; they are now skipped.
- The settings screen now opens from Mod Menu on 26.x before the first world join (group icons stay blank until a world is joined).

**Русский**
- Исправлено под шейдерами Iris: полосы по экрану и пропадающий текст HUD из-за кэша текста (проверено на 1.21.1).
- Исправлено под шейдерами Iris: пропадающий HUD или бледный мир при пропуске прохода обводки (проверено на 1.21.1).
- Немедленное кольцо берёт формат вершин у меша, потому что Iris расширяет его под шейдерами.
- Исправлено переполнение буфера атласа скинов при светящихся игроках: они теперь пропускаются.
- Экран настроек открывается из Mod Menu на 26.x до первого входа в мир (иконки групп пустые, пока не зайти в мир).
