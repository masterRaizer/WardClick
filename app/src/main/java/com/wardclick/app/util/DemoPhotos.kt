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
        "l8" to unsplash("photo-1600596542815-ffad4c1539a9"),
        "l9" to unsplash("photo-1582268611958-ebfd161ef9cf"),
        "l10" to unsplash("photo-1613490493576-7fde63acd811"),
        "l11" to unsplash("photo-1576941089067-2de3c901e126"),
        "l12" to unsplash("photo-1598228723793-52759bba239c"),
        "l13" to unsplash("photo-1416331108676-a22ccb276e35"),
        "l14" to unsplash("photo-1475855581690-80accde3ae2b"),
        "l15" to unsplash("photo-1449844908441-8829872d2607"),
        "l16" to unsplash("photo-1600047509807-ba8f99d2cdde"),
        "l17" to unsplash("photo-1613977257363-707ba9348227"),
        "l18" to unsplash("photo-1523217582562-09d0def993a6"),
        "l19" to unsplash("photo-1602343168117-bb8ffe3e2e9f"),
        "l20" to unsplash("photo-1605276374104-dee2a0ed3cd6"),
        "l21" to unsplash("photo-1568605114967-8130f3a36994"),
        "l22" to unsplash("photo-1580587771525-78b9dba3b914")
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
