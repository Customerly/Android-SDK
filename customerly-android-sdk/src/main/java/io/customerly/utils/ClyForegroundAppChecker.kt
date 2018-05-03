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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import io.customerly.utils.ggkext.weak
import java.lang.ref.WeakReference

/**
 * Created by Gianni on 25/03/17.
 */
@Suppress("MemberVisibilityCanPrivate", "unused")
internal abstract class ForegroundAppChecker : Application.ActivityLifecycleCallbacks {
    private var lastDisplayedActivity : WeakReference<Activity>? = null
    private var pausedApplicationContext : WeakReference<Context>? = null
    private var foreground = false
    private val checkBackground = Runnable {
        this.pausedApplicationContext?.get()?.let { applicationContext ->
            if (this.foreground) {
                this.foreground = false
                this.doOnAppGoBackground(applicationContext = applicationContext)
            }
        }
    }

    internal val isInBackground : Boolean get() = !this.foreground

    internal fun getLastDisplayedActivity() : Activity? = this.lastDisplayedActivity?.get()

    final override fun onActivityResumed(activity: Activity) {
        this.lastDisplayedActivity = activity.weak()
        this.pausedApplicationContext = null
        val wasBackground = !this.foreground
        this.foreground = true
        Handler(Looper.getMainLooper()).removeCallbacks(this.checkBackground)
        this.doOnActivityResumed(activity, wasBackground)
    }

    final override fun onActivityPaused(activity: Activity) {
        this.pausedApplicationContext = activity.applicationContext.weak()
        val h = Handler(Looper.getMainLooper())
        h.removeCallbacks(this.checkBackground)
        h.postDelayed(this.checkBackground, 500)
        this.doOnActivityPaused(activity)
    }

    final override fun onActivityDestroyed(activity: Activity) {
        if(this.lastDisplayedActivity?.get() == activity) {
            this.lastDisplayedActivity = null
        }
        this.doOnActivityDestroyed(activity = activity)
    }
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle?) {}

    protected abstract fun doOnAppGoBackground(applicationContext : Context)
    protected abstract fun doOnActivityResumed(activity: Activity, fromBackground: Boolean)
    protected open fun doOnActivityDestroyed(activity: Activity) {}
    protected open fun doOnActivityPaused(activity: Activity) {}
}
