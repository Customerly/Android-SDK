package io.customerly

/**
 * Created by Gianni on 19/04/18.
 * Project: Customerly-KAndroid-SDK
 */
//TODO Rename Customerly

fun checkClyConfigured(reportingErrorEnabled : Boolean = true, then: (Customerly)->Unit){
    Customerly.get().takeIf { it._isConfigured(! reportingErrorEnabled) }?.let(then)
}