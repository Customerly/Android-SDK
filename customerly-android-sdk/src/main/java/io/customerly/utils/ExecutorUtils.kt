package io.customerly.utils

import android.os.Handler
import android.os.Looper
import io.customerly.utils.ggkext.weak
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Created by Gianni on 16/05/18.
 * Project: Customerly-KAndroid-SDK
 */

private val bkgExecutor: ExecutorService by lazy { Executors.newScheduledThreadPool(2 * Runtime.getRuntime().availableProcessors()) }

internal fun <T> doOnBackground(task: ()->T) = bkgExecutor.submit(task)

internal inline fun <T: Any> T.doOnUiThread(crossinline task: (T)->Unit) {
    if (Looper.getMainLooper().thread == Thread.currentThread()) {
        task(this)
    } else {
        val weakThis = this.weak()
        Handler(Looper.getMainLooper()).post { weakThis.get()?.apply { task(this) } }
    }
}