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

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Created by Gianni on 05/01/18.
 */
internal fun String.hash(sha1 : Boolean = false, sha256 : Boolean = false, sha512 : Boolean = false) : String {
    assert(sha1.as1or0 + sha256.as1or0 + sha512.as1or0 == 1)//Solo uno deve essere valorizzato
    return this.hash(
            when {
                sha512  ->  "SHA-512"
                sha512  ->  "SHA-256"
                sha512  ->  "SHA-1"
                else    ->  throw NoSuchAlgorithmException()
            })
}

internal fun String.hash(algorithm : String) : String {
    return MessageDigest.getInstance(algorithm)
            .digest(this.toByteArray()).joinToString(separator = "") { String.format("%02x", it) }
}

internal fun String.toBooleanOrNull() : Boolean? {
    return when {
        "true".equals(this, ignoreCase = true) -> true
        "false".equals(this, ignoreCase = true) -> false
        else    ->   null
    }
}