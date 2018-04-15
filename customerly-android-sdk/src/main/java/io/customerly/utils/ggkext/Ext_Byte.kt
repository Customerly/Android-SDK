@file:Suppress("unused")

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

package io.customerly.utils.ggkext

import kotlin.experimental.and

/**
 * Created by Gianni on 11/11/17.
 */

internal fun Byte.asUnsigned() : Byte {
    val h80 = 0x80.toByte()
    return if (this and h80 == h80) {
        ((this and 0x7F.toByte()) + h80).toByte()
    } else {
        this
    }
}