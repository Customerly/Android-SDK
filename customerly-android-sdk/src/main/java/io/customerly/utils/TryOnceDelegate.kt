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