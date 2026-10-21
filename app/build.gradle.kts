import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.UnknownHostException
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Ключ Yandex MapKit берётся из local.properties (MAPKIT_API_KEY=...), в git он не попадает.
val mapkitKey: String = run {
    val file = rootProject.layout.projectDirectory.file("local.properties")
    val text = providers.fileContents(file).asText.orNull ?: ""
    Properties().apply { load(text.reader()) }.getProperty("MAPKIT_API_KEY", "").trim()
}

// Архитектуры APK. По умолчанию все три; для быстрой сборки можно оставить одну:
// ./gradlew assembleDebug -Pwardclick.abis=arm64-v8a (так CI собирает каждый push в main)
val apkAbis: List<String> = providers.gradleProperty("wardclick.abis").orNull
    ?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }?.ifEmpty { null }
    ?: listOf("arm64-v8a", "armeabi-v7a", "x86_64")

android {
    namespace = "com.wardclick.app"
    // 37 — минимум для библиотеки «жидкого стекла» Kyant Backdrop
    compileSdk = 37

    defaultConfig {
        applicationId = "com.wardclick.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // true  = встроенный MockApiService (без сервера), false = реальный REST-сервер
        buildConfigField("boolean", "USE_MOCK", "true")
        // 10.0.2.2 — localhost компьютера из эмулятора; для телефона укажите IP ПК
        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/api/\"")
        // ключ Яндекс Карт (пустая строка = карта показывает подсказку вместо MapView)
        buildConfigField("String", "MAPKIT_API_KEY", "\"$mapkitKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // Отдельный APK под каждую архитектуру процессора: телефону нужен app-arm64-v8a-*.apk
    splits {
        abi {
            isEnable = true
            reset()
            include(*apkAbis.toTypedArray())
            isUniversalApk = false
        }
    }

    packaging {
        // нативные библиотеки (Яндекс Карты) хранятся в APK сжатыми — файл заметно меньше
        jniLibs { useLegacyPackaging = true }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

// ---------------------------------------------------------------------------------------------
// Фото демо-особняков вшиваются в приложение, чтобы они не пропадали при плохом интернете.
// Задача скачивает их один раз (при первой сборке) в src/main/res/drawable-nodpi/demo_lN.jpg.
// Уже скачанные файлы не трогаются; если сети нет — сборка не падает, в приложении будет заглушка.
// ---------------------------------------------------------------------------------------------
abstract class DownloadDemoPhotos : DefaultTask() {
    @get:Input
    abstract val photos: MapProperty<String, List<String>>

    @get:Internal
    abstract val outDir: DirectoryProperty

    @TaskAction
    fun download() {
        val dir = outDir.get().asFile.apply { mkdirs() }
        var offline = false
        photos.get().toSortedMap().forEach { (name, urls) ->
            val target = File(dir, "$name.jpg")
            if (target.exists() && target.length() > 10_000) return@forEach
            if (offline) return@forEach
            for (id in urls) {
                val url = "https://images.unsplash.com/$id?w=1200&q=80&fm=jpg&fit=crop"
                try {
                    val conn = URI(url).toURL().openConnection() as HttpURLConnection
                    conn.connectTimeout = 10_000
                    conn.readTimeout = 30_000
                    conn.instanceFollowRedirects = true
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (WardClick build)")
                    val code = conn.responseCode
                    if (code == 200) {
                        val bytes = conn.inputStream.use { it.readBytes() }
                        val isJpeg = bytes.size > 10_000 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
                        if (isJpeg) {
                            target.writeBytes(bytes)
                            logger.lifecycle("WardClick: фото $name скачано ($id, ${bytes.size / 1024} КБ)")
                            break
                        }
                    } else {
                        logger.warn("WardClick: $id ответ $code")
                    }
                    conn.disconnect()
                } catch (e: UnknownHostException) {
                    logger.warn("WardClick: нет интернета, фото не скачаны (${e.message})")
                    offline = true
                    break
                } catch (e: Exception) {
                    logger.warn("WardClick: не удалось скачать $id: ${e.message}")
                }
            }
            if (!target.exists()) logger.warn("WardClick: фото $name не скачано — будет загружаться из сети")
        }
    }
}

val downloadDemoPhotos = tasks.register<DownloadDemoPhotos>("downloadDemoPhotos") {
    group = "wardclick"
    description = "Скачивает фото демо-особняков в res/drawable-nodpi"
    outDir.set(layout.projectDirectory.dir("src/main/res/drawable-nodpi"))
    photos.set(
        mapOf(
            // ключ = имя файла ресурса, значение = id фото на Unsplash (по порядку: основное, запасные)
            "demo_l1" to listOf("photo-1505843513577-22bb7d21e455", "photo-1580587771525-78b9dba3b914"),
            "demo_l2" to listOf("photo-1464146072230-91cabc968266", "photo-1570129477492-45c003edd2be"),
            "demo_l3" to listOf("photo-1564013799919-ab600027ffc6", "photo-1416331108676-a22ccb276e35"),
            "demo_l4" to listOf("photo-1570129477492-45c003edd2be", "photo-1583608205776-bfd35f0d9f83"),
            "demo_l5" to listOf("photo-1512917774080-9991f1c4c750", "photo-1613490493576-7fde63acd811"),
            "demo_l6" to listOf("photo-1600585154340-be6161a56a0c", "photo-1600047509807-ba8f99d2cdde"),
            "demo_l7" to listOf("photo-1510798831971-661eb04b3739", "photo-1449158743715-0a90ebb6d2d8"),
            "demo_l8" to listOf("photo-1600596542815-ffad4c1539a9", "photo-1613977257363-707ba9348227"),
            "demo_l9" to listOf("photo-1582268611958-ebfd161ef9cf"),
            "demo_l10" to listOf("photo-1613490493576-7fde63acd811"),
            "demo_l11" to listOf("photo-1576941089067-2de3c901e126"),
            "demo_l12" to listOf("photo-1598228723793-52759bba239c"),
            "demo_l13" to listOf("photo-1416331108676-a22ccb276e35"),
            "demo_l14" to listOf("photo-1475855581690-80accde3ae2b"),
            "demo_l15" to listOf("photo-1449844908441-8829872d2607"),
            "demo_l16" to listOf("photo-1600047509807-ba8f99d2cdde"),
            "demo_l17" to listOf("photo-1613977257363-707ba9348227"),
            "demo_l18" to listOf("photo-1523217582562-09d0def993a6"),
            "demo_l19" to listOf("photo-1602343168117-bb8ffe3e2e9f"),
            "demo_l20" to listOf("photo-1605276374104-dee2a0ed3cd6"),
            "demo_l21" to listOf("photo-1568605114967-8130f3a36994"),
            "demo_l22" to listOf("photo-1580587771525-78b9dba3b914")
        )
    )
}

tasks.named("preBuild") { dependsOn(downloadDemoPhotos) }


dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)
    implementation(libs.gson)
    // «Liquid Glass»: размытие, линза и блики (Kyant0/AndroidLiquidGlass, Apache 2.0 — см. THIRD_PARTY.md).
    // Формы (Capsule) библиотека объявляет только для запуска, для компиляции они подключены отдельно
    implementation(libs.backdrop)
    implementation(libs.kyant.shapes)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.yandex.mapkit)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
