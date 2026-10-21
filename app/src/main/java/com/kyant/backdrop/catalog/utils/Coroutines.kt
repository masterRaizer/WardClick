package com.kyant.backdrop.catalog.utils

// Изменено для WardClick: в каталоге здесь expect/actual под несколько платформ,
// у нас платформа одна — берётся реализация для Android.

suspend fun awaitFrame() {
    kotlinx.coroutines.android.awaitFrame()
}
