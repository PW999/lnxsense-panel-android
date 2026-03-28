package com.pw999.lnxsense.android.data.model

sealed interface ConnectionState {
    data object Disconnected : ConnectionState
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data class Error(val message: String) : ConnectionState
}
