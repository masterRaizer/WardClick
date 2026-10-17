package com.wardclick.app

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wardclick.app.di.AppContainer
import com.wardclick.app.ui.navigation.MainAppScreen
import com.wardclick.app.ui.theme.WardClickTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    // Принудительно русская локаль: календарь, даты и системные элементы
    // внутри приложения всегда на русском, независимо от языка устройства.
    override fun attachBaseContext(newBase: Context) {
        val ru = Locale("ru", "RU")
        Locale.setDefault(ru)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(ru)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = AppContainer(this)

        setContent {
            WardClickTheme {
                MainAppScreen(appContainer)
            }
        }
    }
}
