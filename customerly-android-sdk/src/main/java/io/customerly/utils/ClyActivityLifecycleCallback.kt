package io.customerly.utils

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

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Handler
import android.os.Looper
import io.customerly.Customerly
import io.customerly.alert.dismissAlertMessageOnActivityDestroyed

/**
 * Created by Gianni on 02/05/18.
 * Project: Customerly-KAndroid-SDK
 */
internal object ClyActivityLifecycleCallback : ForegroundAppChecker() {

    fun registerOn(application: Application) {
        application.registerActivityLifecycleCallbacks(this)
    }

    override fun doOnAppGoBackground(applicationContext: Context) {
        Customerly.clySocket.disconnect()
    }
    override fun doOnActivityResumed(activity: Activity, fromBackground: Boolean) {
        if(fromBackground) {
            Customerly.checkConfigured {
                Customerly.clySocket.connect()
                Customerly.ping()
            }
        }

        Customerly.postOnActivity?.let { postOnActivity ->
            if(Customerly.isEnabledActivity(activity = activity)) {
                Handler(Looper.getMainLooper()).postDelayed({
                    if(postOnActivity(activity) && Customerly.postOnActivity == postOnActivity) {
                        Customerly.postOnActivity = null
                    }
                }, 500)
            }
        }
    }
    override fun doOnActivityDestroyed(activity: Activity) {
        activity.dismissAlertMessageOnActivityDestroyed()//Need to dismiss the alert or leak window exception comes out
    }
}