package com.wardclick.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.wardclick.app.BuildConfig
import com.wardclick.app.data.model.ListingSummaryDto
import com.wardclick.app.util.rub
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.IconStyle
import com.yandex.mapkit.map.InputListener
import com.yandex.mapkit.map.MapObjectTapListener
import com.yandex.mapkit.mapview.MapView
import com.yandex.runtime.image.ImageProvider
import java.lang.ref.WeakReference
import kotlin.math.ln
import kotlin.math.max
import com.yandex.mapkit.map.Map as YMap

object MapConfig {
    /** true, если в local.properties задан MAPKIT_API_KEY. */
    val hasKey: Boolean get() = BuildConfig.MAPKIT_API_KEY.isNotBlank()
}

/** Подсказка вместо карты, пока не указан ключ MapKit. */
@Composable
fun MapKeyMissingBox(modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Map, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(
                "Карта Яндекса не подключена",
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                "Получите ключ MapKit на developer.tech.yandex.ru и добавьте в local.properties строку\nMAPKIT_API_KEY=ваш_ключ\nзатем пересоберите приложение.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}

/** MapView, жизненный цикл которого связан с LifecycleOwner (обязательное требование MapKit). */
@Composable
private fun rememberMapView(): MapView {
    val context = LocalContext.current
    val mapView = remember {
        MapKitFactory.initialize(context.applicationContext)
        MapView(context)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, mapView) {
        var started = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> if (!started) {
                    started = true
                    MapKitFactory.getInstance().onStart()
                    mapView.onStart()
                }
                Lifecycle.Event.ON_STOP -> if (started) {
                    started = false
                    mapView.onStop()
                    MapKitFactory.getInstance().onStop()
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (started) {
                started = false
                mapView.onStop()
                MapKitFactory.getInstance().onStop()
            }
        }
    }
    return mapView
}

private fun hasCoords(l: ListingSummaryDto) = l.latitude != 0.0 || l.longitude != 0.0

/** Приблизительный zoom, при котором в экран помещается область заданного размера (в градусах). */
private fun zoomForSpan(spanDegrees: Double): Float {
    val z = ln(360.0 / (max(spanDegrees, 0.002) * 1.8)) / ln(2.0)
    return z.coerceIn(3.0, 16.0).toFloat()
}

/**
 * Карта со всеми найденными объектами. На каждом маркере — цена за ночь.
 * Нажатие на маркер -> onSelect(id), нажатие на пустое место -> onSelect(null).
 */
@Composable
fun ListingsMap(
    listings: List<ListingSummaryDto>,
    selectedId: String?,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!MapConfig.hasKey) {
        MapKeyMissingBox(modifier)
        return
    }
    val context = LocalContext.current
    val mapView = rememberMapView()
    val currentOnSelect by rememberUpdatedState(onSelect)

    // сильные ссылки на слушателей: MapKit хранит их слабо
    val holder = remember {
        object {
            val tapListener: MapObjectTapListener = MapObjectTapListener { obj, _ ->
                currentOnSelect(obj.userData as? String)
                true
            }
            val inputListener: InputListener = object : InputListener {
                override fun onMapTap(map: YMap, point: Point) {
                    currentOnSelect(null)
                }

                override fun onMapLongTap(map: YMap, point: Point) {}
            }
            val inputRef = WeakReference(inputListener)
            val tapRef = WeakReference(tapListener)
        }
    }

    DisposableEffect(mapView) {
        val map = mapView.mapWindow.map
        map.addInputListener(holder.inputRef)
        onDispose { map.removeInputListener(holder.inputRef) }
    }

    // перерисовка маркеров
    LaunchedEffect(listings, selectedId) {
        val map = mapView.mapWindow.map
        val objects = map.mapObjects
        objects.clear()
        listings.filter(::hasCoords).forEach { l ->
            val isSelected = l.id == selectedId
            val bitmap = createPriceMarker(context, rub(l.pricePerNight), isSelected)
            val placemark = objects.addPlacemark()
            placemark.geometry = Point(l.latitude, l.longitude)
            placemark.setIcon(
                ImageProvider.fromBitmap(bitmap),
                IconStyle().setAnchor(android.graphics.PointF(0.5f, 1f)).setZIndex(if (isSelected) 10f else 0f)
            )
            placemark.userData = l.id
            placemark.addTapListener(holder.tapRef)
        }
    }

    // камера: при смене набора объектов — показать все; при выборе — приблизиться к выбранному
    val idsKey = remember(listings) { listings.joinToString(",") { it.id } }
    LaunchedEffect(idsKey) {
        val pts = listings.filter(::hasCoords)
        if (pts.isEmpty()) return@LaunchedEffect
        val map = mapView.mapWindow.map
        val minLat = pts.minOf { it.latitude }
        val maxLat = pts.maxOf { it.latitude }
        val minLon = pts.minOf { it.longitude }
        val maxLon = pts.maxOf { it.longitude }
        val center = Point((minLat + maxLat) / 2, (minLon + maxLon) / 2)
        val zoom = if (pts.size == 1) 14f else zoomForSpan(max(maxLat - minLat, maxLon - minLon))
        map.move(CameraPosition(center, zoom, 0f, 0f), Animation(Animation.Type.SMOOTH, 0.5f), null)
    }
    LaunchedEffect(selectedId) {
        val l = listings.firstOrNull { it.id == selectedId } ?: return@LaunchedEffect
        if (!hasCoords(l)) return@LaunchedEffect
        val map = mapView.mapWindow.map
        val zoom = max(map.cameraPosition.zoom, 11f)
        map.move(
            CameraPosition(Point(l.latitude, l.longitude), zoom, 0f, 0f),
            Animation(Animation.Type.SMOOTH, 0.4f),
            null
        )
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

/** Небольшая карта с одной точкой (экран объекта). Жесты отключены, чтобы не мешать прокрутке. */
@Composable
fun PointMap(
    latitude: Double,
    longitude: Double,
    priceText: String,
    modifier: Modifier = Modifier
) {
    if (!MapConfig.hasKey) {
        MapKeyMissingBox(modifier)
        return
    }
    val context = LocalContext.current
    val mapView = rememberMapView()

    LaunchedEffect(latitude, longitude, priceText) {
        val map = mapView.mapWindow.map
        map.isScrollGesturesEnabled = false
        map.isZoomGesturesEnabled = false
        map.isRotateGesturesEnabled = false
        map.isTiltGesturesEnabled = false
        val point = Point(latitude, longitude)
        map.mapObjects.clear()
        val placemark = map.mapObjects.addPlacemark()
        placemark.geometry = point
        placemark.setIcon(
            ImageProvider.fromBitmap(createPriceMarker(context, priceText, true)),
            IconStyle().setAnchor(android.graphics.PointF(0.5f, 1f))
        )
        map.move(CameraPosition(point, 15f, 0f, 0f))
    }

    AndroidView(factory = { mapView }, modifier = modifier.fillMaxWidth())
}

/** Открыть точку в приложении «Яндекс Карты»; если его нет — в браузере. */
fun openInYandexMaps(context: Context, latitude: Double, longitude: Double) {
    val app = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("yandexmaps://maps.yandex.ru/?pt=$longitude,$latitude&z=16&l=map")
    )
    try {
        context.startActivity(app)
    } catch (e: Exception) {
        try {
            val web = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://yandex.ru/maps/?pt=$longitude,$latitude&z=16&l=map")
            )
            context.startActivity(web)
        } catch (e2: Exception) {
            Toast.makeText(context, "Не удалось открыть Яндекс Карты", Toast.LENGTH_SHORT).show()
        }
    }
}
