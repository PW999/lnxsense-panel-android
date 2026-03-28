package com.pw999.lnxsense.android.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.pw999.lnxsense.android.data.SensorConfigStore
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.ui.browser.SensorBrowserScreen
import com.pw999.lnxsense.android.ui.config.SensorConfigScreen
import com.pw999.lnxsense.android.ui.connection.ConnectionScreen
import com.pw999.lnxsense.android.ui.dials.DialPanelScreen

object Routes {
    const val CONNECTION = "connection"
    const val BROWSER = "browser"
    const val CONFIG = "config"
    const val DIALS = "dials"
}

@Composable
fun LnxSenseNavGraph(
    navController: NavHostController,
    repository: SensorRepository,
    configStore: SensorConfigStore
) {
    NavHost(navController = navController, startDestination = Routes.CONNECTION) {
        composable(Routes.CONNECTION) {
            ConnectionScreen(
                repository = repository,
                configStore = configStore,
                onConnected = {
                    navController.navigate(Routes.BROWSER) {
                        popUpTo(Routes.CONNECTION) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.BROWSER) {
            SensorBrowserScreen(
                repository = repository,
                configStore = configStore,
                onConfigureDials = { navController.navigate(Routes.CONFIG) },
                onViewDials = { navController.navigate(Routes.DIALS) },
                onDisconnect = {
                    repository.disconnect()
                    navController.navigate(Routes.CONNECTION) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.CONFIG) {
            SensorConfigScreen(
                repository = repository,
                configStore = configStore,
                onViewDials = {
                    navController.navigate(Routes.DIALS) {
                        popUpTo(Routes.BROWSER)
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.DIALS) {
            DialPanelScreen(
                repository = repository,
                configStore = configStore,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
