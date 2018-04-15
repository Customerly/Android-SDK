package io.customerly.utils.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build

import io.customerly.Customerly
import io.customerly.utils.ggkext.checkConnection

/**
 * Created by Gianni on 27/03/17.
 * Project: CustomerlyApp
 */

internal fun registerLollipopNetworkReceiver(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        (context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.registerNetworkCallback(
                NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Customerly.get().__SOCKET__check()
                    }
                })
    }
}

class ClyNetworkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                && intent != null && "android.net.conn.CONNECTIVITY_CHANGE" == intent.action
                && context.checkConnection()) {
            Customerly.get().__SOCKET__check()
        }
    }
}