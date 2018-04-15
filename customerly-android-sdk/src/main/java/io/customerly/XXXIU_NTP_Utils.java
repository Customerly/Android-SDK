package io.customerly;

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

import android.content.Context;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.jetbrains.annotations.Contract;

/**
 * Created by Gianni on 08/03/16.
 * Project: Customerly Android SDK
 */
@SuppressWarnings("unused")
class XXXIU_NTP_Utils {
    @Contract("_, !null, null -> fail")
    static void getSafeNow_fromUiThread(@Nullable Context context, @NonNull XXXIU_ResultUtils.OnNonNullResult<Long> onNetworkTimeNotNull, @Nullable XXXIU_ResultUtils.OnNoResult onNetworkError) {
        XXXIU_NTP_Utils.internal_getSafeNow_fromUiThread(context, null, onNetworkTimeNotNull, onNetworkError);
    }
    @Contract("_, null, null, _ -> fail; _, null, !null, null -> fail")
    private static void internal_getSafeNow_fromUiThread(@Nullable Context context, @Nullable XXXIU_ResultUtils.OnResult<Long> onNetworkTime, @Nullable XXXIU_ResultUtils.OnNonNullResult<Long> onNetworkTimeNotNull, @Nullable XXXIU_ResultUtils.OnNoResult onNetworkError) {
        if(onNetworkTime == null && (onNetworkTimeNotNull == null || onNetworkError == null))
            throw new IllegalStateException("You have to specify a ResultUtils.OnResult or both ResultUtils.OnNonNullResult and ResultUtils.OnNoResult");
        if(context != null && ! XXXIU_Utils.checkConnection(context)) {
            if(onNetworkTime != null) {
                onNetworkTime.onResult(null);
            } else {
                onNetworkError.onResult();
            }
        }

        new AsyncTask<Void, Void, Long>() {
            @Override
            protected Long doInBackground(Void... params) {
                return XXXIU_NTP_Utils.getSafeNow_fromBackgroundThread();
            }
            @Override
            protected void onPostExecute(Long time) {
                if(onNetworkTime != null) {
                    onNetworkTime.onResult(time);
                } else if(time != null) {
                    onNetworkTimeNotNull.onResult(time);
                } else {
                    onNetworkError.onResult();
                }
            }
        }.execute();
    }
    @SuppressWarnings("SpellCheckingInspection")
    private static XXXIU_SntpClient _SntpClient;
    @SuppressWarnings("WeakerAccess")
    @Nullable static Long getSafeNow_fromBackgroundThread() {
        if(XXXIU_NTP_Utils._SntpClient == null) {
            XXXIU_NTP_Utils._SntpClient = new XXXIU_SntpClient();
        }
        int server = 0;
        while(server < SERVERS.length) {
            if (XXXIU_NTP_Utils._SntpClient.requestTime(SERVERS[server++], 5000)) {
                return XXXIU_NTP_Utils._SntpClient.getNtpTime() + SystemClock.elapsedRealtime() - XXXIU_NTP_Utils._SntpClient.getNtpTimeReference();
            }
        }
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
