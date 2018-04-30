package io.customerly.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by Gianni on 30/04/18.
 * Project: Customerly-KAndroid-SDK
 */
internal class DefaultInitializerDelegate<T: Any>(private val constructor: ()->T) : ReadWriteProperty<Any?, T> {
    private var value: T? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        var defaultValue = this.value
        if(defaultValue == null) {
            defaultValue = constructor()
            this.value = defaultValue
        }
        return defaultValue
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }
}