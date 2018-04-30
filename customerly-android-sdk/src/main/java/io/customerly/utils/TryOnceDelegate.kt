package io.customerly.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Created by Gianni on 30/04/18.
 * Project: Customerly-KAndroid-SDK
 */
internal class TryOnceDelegate<T: Any?>(private val attempt: ()->T) : ReadWriteProperty<Any?, T?> {
    private var value: T? = null
    private var tried: Boolean = false

    override fun getValue(thisRef: Any?, property: KProperty<*>): T? {
        var defaultValue = this.value
        if(!this.tried) {
            this.tried = true
            if(defaultValue == null) {
                defaultValue = attempt()
                this.value = defaultValue
            }
        }
        return defaultValue
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T?) {
        this.value = value
    }
}