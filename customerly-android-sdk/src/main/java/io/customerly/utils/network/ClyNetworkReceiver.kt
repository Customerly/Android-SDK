package io.customerly.utils.network

/*
 * Copyright (C) 2017 Customerly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import io.customerly.Cly
import io.customerly.utils.ggkext.checkConnection

/**
 * Created by Gianni on 27/03/17.
 * Project: CustomerlyApp
 */

internal fun Context.registerLollipopNetworkReceiver() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        (this.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
                ?.registerNetworkCallback(
                NetworkRequest.Builder()
                        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                        .build(),
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Cly.clySocket.check()
                    }
                })
    }
}

class ClyNetworkReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                && intent != null && "android.net.conn.CONNECTIVITY_CHANGE" == intent.action
                && context.checkConnection()) {
            Cly.clySocket.check()
        }
    }
}