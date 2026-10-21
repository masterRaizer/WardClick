# Сторонние компоненты

## Kyant0/AndroidLiquidGlass (Kyant Backdrop)

Эффект «жидкого стекла» — размытие, преломление у края (линза), блики и тени — даёт библиотека
[AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass), подключённая из Maven Central
как `io.github.kyant0:backdrop-android` (вместе с `io.github.kyant0:shapes-android`). Эту же
библиотеку использует приложение [Ward](https://github.com/shashachkaaa/Ward), откуда взята идея.

Готовых компонентов библиотека не публикует — они лежат в её каталоге примеров. Поэтому
`LiquidBottomTabs`, `LiquidBottomTab`, `LiquidButton` и вспомогательные к ним классы перенесены
в проект из оригинального репозитория (коммит `65ab177`) в пакет `com.kyant.backdrop.catalog`
(`app/src/main/java/com/kyant/backdrop/catalog`). Изменения от оригинала отмечены в начале
изменённых файлов:

- `Coroutines.kt` — вместо `expect/actual` реализация только для Android;
- `DampedDragAnimation.kt` — время для `VelocityTracker` из `SystemClock`, жёсткость хода — параметром;
- `LiquidBottomTabs.kt` — цвет акцента, заливка капсулы и жёсткость хода капли параметрами;
  после простого тапа капля не возвращается на прежнюю вкладку.

    Copyright (c) Kyant0
    Licensed under the Apache License, Version 2.0
    http://www.apache.org/licenses/LICENSE-2.0

Полный текст лицензии — `app/src/main/java/com/kyant/backdrop/catalog/LICENSE`.
