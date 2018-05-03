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

/**
 * Created by Gianni on 17/04/18.
 * Project: Customerly-KAndroid-SDK
 */
class ClySemaphore(private var value : Boolean = false) {
    private val lock = arrayOfNulls<Any>(0)

    fun on() = synchronized(this.lock) {
        if(! this.look()) {
            this.value = true
            false
        } else {
            true
        }
    }

    fun off() = synchronized(this.lock) {
        if(this.look()) {
            this.value = false
            true
        } else {
            false
        }
    }

    fun look() : Boolean = this.value
}