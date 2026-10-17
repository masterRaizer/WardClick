package com.wardclick.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.wardclick.app.di.AppContainer
import com.wardclick.app.ui.navigation.MainAppScreen
import com.wardclick.app.ui.theme.WardClickTheme

class MainActivity : ComponentActivity() {
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
