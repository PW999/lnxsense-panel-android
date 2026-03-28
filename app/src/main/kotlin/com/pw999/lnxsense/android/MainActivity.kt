package com.pw999.lnxsense.android

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.navigation.LnxSenseNavGraph
import com.pw999.lnxsense.android.ui.theme.LnxSenseTheme
import com.pw999.lnxsense.android.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Keep screen on globally — this is a monitoring app, the screen should
        // stay awake while the Activity is visible. The flag is automatically
        // ignored when the app goes to background.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val repository = SensorRepository.getInstance()
        val configStore = SensorConfigStore(applicationContext)

        setContent {
            val themeMode by configStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

            LnxSenseTheme(themeMode = themeMode) {
                val navController = rememberNavController()
                LnxSenseNavGraph(
                    navController = navController,
                    repository = repository,
                    configStore = configStore
                )
            }
        }
    }
}
