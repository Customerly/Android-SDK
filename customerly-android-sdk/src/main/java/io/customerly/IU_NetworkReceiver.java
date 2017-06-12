package io.customerly;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;

/**
 * Created by Gianni on 27/03/17.
 * Project: CustomerlyApp
 */

public class IU_NetworkReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP && IU_Utils.checkConnection(context)) {
            Customerly.get().__SOCKET__check();
        }
    }

    public static void registerLollipopNetworkReceiver(Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            cm.registerNetworkCallback(
                    new NetworkRequest.Builder()
                            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                            .build(),
                    new ConnectivityManager.NetworkCallback() {
                        @Override
                        public void onAvailable(Network network) {
                            boolean connected = false;
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                connected = cm.bindProcessToNetwork(network);
                            } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                                //noinspection deprecation
                                connected = ConnectivityManager.setProcessDefaultNetwork(network);
                            }
                            if(connected) {
                                Customerly.get().__SOCKET__check();
                            }
                        }
                    });
        }
    }
}
