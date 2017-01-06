package io.customerly;

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.Contract;

/**
 * Created by Gianni on 08/03/16.
 * Project: CustomerlyAndroidSDK
 */
@SuppressWarnings("unused")
class Internal_Utils__NTP_Utils {
    @Contract("_, !null, null -> fail")
    static void getSafeNow_fromUiThread(@Nullable Context context, @NonNull Internal_Utils__ResultUtils.OnNonNullResult<Long> onNetworkTimeNotNull, @Nullable Internal_Utils__ResultUtils.OnNoResult onNetworkError) {
        Internal_Utils__NTP_Utils.internal_getSafeNow_fromUiThread(context, null, onNetworkTimeNotNull, onNetworkError);
    }
    @Contract("_, null, null, _ -> fail; _, null, !null, null -> fail")
    private static void internal_getSafeNow_fromUiThread(@Nullable Context context, @Nullable Internal_Utils__ResultUtils.OnResult<Long> onNetworkTime, @Nullable Internal_Utils__ResultUtils.OnNonNullResult<Long> onNetworkTimeNotNull, @Nullable Internal_Utils__ResultUtils.OnNoResult onNetworkError) {
        if(onNetworkTime == null && (onNetworkTimeNotNull == null || onNetworkError == null))
            throw new IllegalStateException("You have to specify a ResultUtils.OnResult or both ResultUtils.OnNonNullResult and ResultUtils.OnNoResult");
        if(context != null && ! Internal_Utils__Utils.checkConnection(context)) {
            if(onNetworkTime != null)
                onNetworkTime.onResult(null);
            else
                onNetworkError.onResult();
        }

        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... params) {
                return Internal_Utils__NTP_Utils.getSafeNow_fromBackgroundThread();
            }
            @Override
            protected void onPostExecute(Long time) {
                if(onNetworkTime != null)
                    onNetworkTime.onResult(time);
                else if(time != null)
                    onNetworkTimeNotNull.onResult(time);
                else
                    onNetworkError.onResult();
            }
        }.execute();
    }
    @SuppressWarnings("SpellCheckingInspection")
    private static Internal_Utils__SntpClient _SntpClient;
    @SuppressWarnings("WeakerAccess")
    @Nullable static Long getSafeNow_fromBackgroundThread() {
        if(Internal_Utils__NTP_Utils._SntpClient == null)
            Internal_Utils__NTP_Utils._SntpClient = new Internal_Utils__SntpClient();
        int server = 0;
        while(server < SERVERS.length)
            if (Internal_Utils__NTP_Utils._SntpClient.requestTime(SERVERS[server++], 5000))
                return Internal_Utils__NTP_Utils._SntpClient.getNtpTime() + SystemClock.elapsedRealtime() - Internal_Utils__NTP_Utils._SntpClient.getNtpTimeReference();
        return null;
    }
    private static final String[] SERVERS = new String[] {
            "0.pool.ntp.org",
            "1.pool.ntp.org",
            "2.pool.ntp.org",
            "3.pool.ntp.org",
            "0.uk.pool.ntp.org",
            "1.uk.pool.ntp.org",
            "2.uk.pool.ntp.org",
            "3.uk.pool.ntp.org",
            "0.US.pool.ntp.org",
            "1.US.pool.ntp.org",
            "2.US.pool.ntp.org",
            "3.US.pool.ntp.org",
            "asia.pool.ntp.org",
            "europe.pool.ntp.org",
            "north-america.pool.ntp.org",
            "oceania.pool.ntp.org",
            "south-america.pool.ntp.org",
            "africa.pool.ntp.org",
            "time.apple.com"
    };
}
