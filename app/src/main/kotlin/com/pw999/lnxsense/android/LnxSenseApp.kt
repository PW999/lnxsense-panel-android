package com.pw999.lnxsense.android

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.pw999.lnxsense.android.data.SensorRepository
import com.pw999.lnxsense.android.data.model.ConnectionState

class LnxSenseApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(AppLifecycleObserver())
    }

    /**
     * Disconnects the socket when the entire app goes to background and
     * automatically reconnects when the app returns to the foreground.
     *
     * The server sends readings every ~1 second, so keeping the socket open while
     * backgrounded would waste battery and network for data nobody is looking at.
     */
    private class AppLifecycleObserver : DefaultLifecycleObserver {
        private var wasConnected = false

        override fun onStop(owner: LifecycleOwner) {
            val repo = SensorRepository.getInstance()
            wasConnected = repo.connectionState.value is ConnectionState.Connected
            if (wasConnected) {
                repo.disconnect()
            }
        }

        override fun onStart(owner: LifecycleOwner) {
            if (wasConnected) {
                val repo = SensorRepository.getInstance()
                // Reconnect silently — the user stays on whichever screen they were on
                repo.reconnect()
                wasConnected = false
            }
        }
    }
}
