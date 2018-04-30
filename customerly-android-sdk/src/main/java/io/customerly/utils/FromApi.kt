package io.customerly.utils

import android.annotation.TargetApi
import android.os.Build

/**
 * Created by Gianni on 30/04/18.
 * Project: Customerly-KAndroid-SDK
 */
@TargetApi(Build.VERSION_CODES.O)
inline fun <F: Any> F.fromApiO(otherwise: F.()->Unit = {}, then: F.()->Unit) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        this.then()
    } else {
        this.otherwise()
    }
}



@TargetApi(Build.VERSION_CODES.O)
inline fun <F: Any> F.fromApi26(otherwise: F.()->Unit = {}, then: F.()->Unit) = this.fromApiO(then = then, otherwise = otherwise)