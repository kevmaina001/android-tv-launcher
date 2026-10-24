package com.kev.launcher

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

enum class NetworkKind { ETHERNET, WIFI, OFFLINE }

/** How the TV is online right now; updates live when a cable is pulled or Wi-Fi drops. */
@Composable
fun rememberNetworkKind(): NetworkKind {
    val context = LocalContext.current
    var kind by remember { mutableStateOf(currentNetworkKind(context)) }

    DisposableEffect(context) {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        if (cm == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return@DisposableEffect onDispose { }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                kind = caps.toKind()
            }

            override fun onLost(network: Network) {
                kind = currentNetworkKind(context)
            }
        }
        cm.registerDefaultNetworkCallback(callback)
        onDispose { cm.unregisterNetworkCallback(callback) }
    }
    return kind
}

private fun currentNetworkKind(context: Context): NetworkKind {
    val cm = context.getSystemService(ConnectivityManager::class.java) ?: return NetworkKind.OFFLINE
    val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return NetworkKind.OFFLINE
    return caps.toKind()
}

private fun NetworkCapabilities.toKind() = when {
    hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkKind.ETHERNET
    hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkKind.WIFI
    else -> NetworkKind.OFFLINE
}
