@file:Suppress("unused")

package io.customerly.utils.ggkext

/**
 * Created by Gianni on 11/08/17.
 */
internal val Boolean.as1or0 :Int
    get() {
        return if(this) 1 else 0
    }

/**
 * Convert a 0 Int value to false Boolean value
 * Convert any other Int value to Boolean true
 */
internal val Int?.asBool :Boolean
    get() {
        return this == 1
    }

/**
 * Convert a 1 Int value to true Boolean value
 * Convert a 0 Int value to false Boolean value
 * Convert any other Int value to null
 */
internal val Int.asBoolStrict :Boolean?
    get() {
        return when(this) {
            1 -> true
            0 -> false
            else -> null
        }
    }