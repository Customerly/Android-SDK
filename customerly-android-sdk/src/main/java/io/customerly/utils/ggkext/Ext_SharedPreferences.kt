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

import android.content.SharedPreferences
import org.json.JSONObject

/**
 * Created by Gianni on 05/01/18.
 */

internal fun SharedPreferences.safeString(key: String): String?
        = this.nullOnException { it.getString(key, null) }

internal fun SharedPreferences.safeString(key: String, defValue: String): String
        = this.nullOnException { it.getString(key, defValue) } ?: defValue

internal fun SharedPreferences.safeJson(key: String): JSONObject?
        = this.nullOnException { it.getString(key, null) }?.nullOnException { JSONObject(it) }

internal fun SharedPreferences.safeJsonNonNull(key: String): JSONObject
        = this.safeJson(key = key) ?: JSONObject()

internal fun SharedPreferences.safeInt(key: String, default : Int = 0): Int
        = this.nullOnException { it.getInt(key, default) } ?: default

internal fun SharedPreferences.safeBoolean(key: String, default : Boolean = false): Boolean
        = this.nullOnException { it.getBoolean(key, default) } ?: default