# ZakoOpt — NeoForge 1.21.10

Порт [ZakoOpt](https://github.com/3ako/ZakoOpt) (автор — Zako, https://t.me/StarikZako) с Fabric на NeoForge 1.21.10.
Неофициальный порт: вопросы по нему — сюда, а не автору оригинала. Что изменено — в `NOTICE` в корне репозитория.
Лицензия: GNU LGPL-3.0 (`COPYING.LESSER`, `COPYING` в корне репозитория).

## Зависимости в игре
- NeoForge 21.10.x для Minecraft 1.21.10
- ImmediatelyFast (NeoForge) — обязателен
- Sodium 0.7.x (NeoForge) — рекомендуется; без него часть оптимизаций отключается

## Сборка
Это отдельный Gradle-проект, не часть Fabric-сборки в `zakoopt/`. Нужна Java 21 (Gradle скачает сама).

```bash
cd zakoopt/neoforge-1.21.10
./gradlew build
```
Готовый jar: `build/libs/zakoopt-1.1.6+1.21.10-neoforge.jar`.

Запуск dev-клиента: `./gradlew runClient` (флаги: `-Piris`, `-Pnosodium`, `-Pjvm="..."`, `-Pworld=<сейв>`).

Настройки: кнопка «Настроить» у мода в списке модов NeoForge.

## Откуда код

Исходники — копия Fabric-слоёв, из которых собирается модуль `zakoopt/1.21.10`: `common`, `1.21.x`, `1.21.4-1.21.10`,
`1.21.8+`, `1.21.9+`, `1.21.8-1.21.11`, `1.21.8-1.21.10`, `1.21.9-1.21.11`, `1.21.10`. Изменения автора в этих папках
сюда сами не попадают — их нужно переносить вручную.

В dev-клиенте Sodium падает на слое валидации графики NeoForge (`ValidationGpuTexture`): выключите
`enableB3DValidationLayer` в `run/config/neoforge-client.toml`. В обычной игре этого слоя нет.
