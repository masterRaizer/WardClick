package com.wardclick.app

import android.app.Application
import com.yandex.mapkit.MapKitFactory

class WardClickApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // ключ задаётся в local.properties: MAPKIT_API_KEY=...
        if (BuildConfig.MAPKIT_API_KEY.isNotBlank()) {
            MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY)
            MapKitFactory.setLocale("ru_RU")
        }
    }
}
