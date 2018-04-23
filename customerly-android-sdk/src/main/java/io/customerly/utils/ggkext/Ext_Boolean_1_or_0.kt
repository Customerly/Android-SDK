@file:Suppress("unused")

package io.customerly.utils.ggkext

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