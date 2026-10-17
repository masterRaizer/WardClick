package com.wardclick.app.util

import android.content.Context

/**
 * Фотографии демо-особняков.
 *
 * В демо-данных фото указывается как «demo://l1». Сначала ищется картинка, вшитая в приложение
 * (res/drawable-nodpi/demo_l1.jpg — её скачивает Gradle-задача downloadDemoPhotos при сборке),
 * и только если её нет — грузится из интернета. Поэтому фото не пропадают при плохой связи.
 * Обычные ссылки http/https (например, с реального сервера) грузятся из сети как есть.
 */
object DemoPhotos {
    private const val SCHEME = "demo://"

    /** Запасные ссылки — те же фото, что скачиваются при сборке. */
    private val remote = mapOf(
        "l1" to unsplash("photo-1505843513577-22bb7d21e455"),
        "l2" to unsplash("photo-1464146072230-91cabc968266"),
        "l3" to unsplash("photo-1564013799919-ab600027ffc6"),
        "l4" to unsplash("photo-1570129477492-45c003edd2be"),
        "l5" to unsplash("photo-1512917774080-9991f1c4c750"),
        "l6" to unsplash("photo-1600585154340-be6161a56a0c"),
        "l7" to unsplash("photo-1510798831971-661eb04b3739"),
        "l8" to unsplash("photo-1600596542815-ffad4c1539a9")
    )

    private fun unsplash(id: String) = "https://images.unsplash.com/$id?w=1200&q=80&fm=jpg&fit=crop"

    fun uri(listingKey: String) = SCHEME + listingKey

    /** Модель для Coil: Int (фото из ресурсов приложения), URL или null. */
    fun resolve(context: Context, url: String?): Any? {
        if (url.isNullOrBlank()) return null
        if (!url.startsWith(SCHEME)) return url
        val key = url.removePrefix(SCHEME)
        val resId = context.resources.getIdentifier("demo_$key", "drawable", context.packageName)
        return if (resId != 0) resId else remote[key]
    }
}
