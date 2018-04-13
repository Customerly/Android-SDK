@file:Suppress("unused")

package io.customerly.utils.ggkext

import android.annotation.TargetApi
import android.os.Build
import android.os.Looper
import java.lang.ref.WeakReference
import kotlin.reflect.KMutableProperty1

/**
 * Created by Gianni on 12/08/17.
 */

internal inline fun <T> T.apiMin(apiLevel :Int, block: T.() -> Unit): T {
    if(Build.VERSION.SDK_INT >= apiLevel) {
        block()
    }
    return this
}

internal fun <ANY : Any> ANY.weak() = WeakReference(this)

internal inline fun <ANY : Any, RETURN> WeakReference<ANY>.reference(block : (ANY)->RETURN) : RETURN? {
    return this.get()?.let(block)
}

internal inline fun <ANY> ANY.ignoreException(block : (ANY)->Unit) {
    try {
        block(this)
    } catch (exception : Exception) { }
}

internal inline fun <ANY> ANY.skipException(block : (ANY)->ANY) : ANY{
    try {
        block(this)
    } catch (exception : Exception) { }
    return this
}

internal inline fun <ANY,RETURN> ANY.nullOnException(block : (ANY)->RETURN) : RETURN? {
    return try {
        block(this)
    } catch (exception : Exception) {
        null
    }
}

internal fun <ITEM,FIELD_TYPE> ITEM.copy(vararg f: KMutableProperty1<ITEM, FIELD_TYPE>, to: ITEM) {
    f.forEach {
        it.set(to, it.get(this))
    }
}

@TargetApi(Build.VERSION_CODES.M)
internal fun isOnMainThread() : Boolean {
    return when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> Looper.getMainLooper().isCurrentThread
        else -> Thread.currentThread() == Looper.getMainLooper().thread
    }
}