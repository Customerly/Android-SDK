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

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * JSONObject get/opt JSONArray asSequence/Opt
 */
@Throws(JSONException::class)
internal inline fun <reified TYPE> JSONObject.getSequence(name : String) : Sequence<TYPE>
        = this.getJSONArray(name).asSequence()

@Throws(JSONException::class)
internal inline fun <reified TYPE> JSONObject.getSequenceOpt(name : String) : Sequence<TYPE?>
        = this.getJSONArray(name).asSequenceOpt()

@Throws(JSONException::class)
internal inline fun <reified TYPE> JSONObject.getSequenceOpt(name : String, fallback : TYPE) : Sequence<TYPE>
        = this.getJSONArray(name).asSequenceOpt(fallback = fallback)


internal inline fun <reified TYPE> JSONObject.optSequence(name : String) : Sequence<TYPE>?
        = this.optJSONArray(name)?.asSequence()

internal inline fun <reified TYPE> JSONObject.optSequenceOpt(name : String) : Sequence<TYPE?>?
        = this.optJSONArray(name)?.asSequenceOpt()

internal inline fun <reified TYPE> JSONObject.optSequenceOpt(name : String, fallback : TYPE) : Sequence<TYPE>?
        = this.optJSONArray(name)?.asSequenceOpt(fallback = fallback)

/**
 * JSONArray asSequence/Opt
 */
internal inline fun <reified TYPE> JSONArray.asSequence() : Sequence<TYPE>
        = if(this.length() == 0) {
            emptySequence()
        } else {
            (1 until this.length())
            .asSequence()
            .map { this.getTyped<TYPE>(index = it) }
        }

internal inline fun <reified TYPE> JSONArray.asSequenceOpt(fallback : TYPE) : Sequence<TYPE>
        = if(this.length() == 0) {
            emptySequence()
        } else {
            (1 until this.length())
            .asSequence()
            .map { this.optTyped(index = it, fallback = fallback) }
        }

internal inline fun <reified TYPE> JSONArray.asSequenceOpt() : Sequence<TYPE?>
        = if(this.length() == 0) {
            emptySequence()
        } else {
            (1 until this.length())
            .asSequence()
            .map { this.optTyped<TYPE>(index = it) }
        }

/**
 * JSONObject get/opt
 */
@Throws(JSONException::class)
internal inline fun <reified TYPE> JSONObject.getTyped(name : String) : TYPE {
    if(this.isNull(name)) {
        throw JSONException("No value found or null for name = $name")
    }
    return when(TYPE::class) {
        Boolean::class ->   this.getBoolean(name) as TYPE
        Double::class ->    this.getDouble(name) as TYPE
        Int::class ->       this.getInt(name) as TYPE
        Long::class ->      this.getLong(name) as TYPE
        String::class ->    this.getString(name) as TYPE
        JSONArray::class -> this.getJSONArray(name) as TYPE
        JSONObject::class ->this.getJSONObject(name) as TYPE
        else -> throw JSONException("Only can be retrieved Boolean,Double,Int,Long,String,JSONArray or JSONObject from a JSONObject")
    }
}

@Throws(JSONException::class)
internal inline fun <reified TYPE> JSONObject.optTyped(name : String) : TYPE? {
    return if(this.isNull(name)) {
        null
    } else when(TYPE::class) {
        Boolean::class ->   {
            val value = this.opt(name)
            when(value) {
                is Boolean  ->  value as TYPE
                is String   -> value.toBooleanOrNull() as TYPE
                else    ->  null
            }
        }
        Double::class -> {
            val value = this.opt(name)
            when (value) {
                is Double -> value as TYPE
                is Number -> value.toDouble() as TYPE
                is String -> value.toDoubleOrNull() as TYPE
                else -> null
            }
        }
        Int::class -> {
            val value = this.opt(name)
            when (value) {
                is Int -> value as TYPE
                is Number -> value.toInt() as TYPE
                is String -> value.toIntOrNull() as TYPE
                else -> null
            }
        }
        Long::class -> {
            val value = this.opt(name)
            when (value) {
                is Long -> value as TYPE
                is Number -> value.toLong() as TYPE
                is String -> value.toLongOrNull() as TYPE
                else -> null
            }
        }
        String::class ->    this.optString(name, null) as TYPE
        JSONArray::class -> this.optJSONArray(name) as TYPE
        JSONObject::class ->this.optJSONObject(name) as TYPE
        else -> throw JSONException("Only can be retrieved Boolean,Double,Int,Long,String,JSONArray or JSONObject from a JSONObject")
    }
}

@Throws(JSONException::class)
internal inline fun <reified TYPE> JSONObject.optTyped(name : String, fallback : TYPE) : TYPE
        = this.optTyped(name = name) ?: fallback

/**
 * JSONArray get/opt
 */
@Throws(JSONException::class)
internal inline fun <reified TYPE> JSONArray.getTyped(index : Int) : TYPE {
    if(this.isNull(index)) {
        throw JSONException("No value found or null at index = $index")
    }
    return when (TYPE::class) {
        Boolean::class -> this.getBoolean(index) as TYPE
        Double::class -> this.getDouble(index) as TYPE
        Int::class -> this.getInt(index) as TYPE
        Long::class -> this.getLong(index) as TYPE
        String::class -> this.getString(index) as TYPE
        JSONArray::class -> this.getJSONArray(index) as TYPE
        JSONObject::class -> this.getJSONObject(index) as TYPE
        else -> throw JSONException("Only can be retrieved Boolean,Double,Int,Long,String,JSONArray or JSONObject from a JSONArray")
    }
}

@Throws(JSONException::class)
internal inline fun <reified TYPE> JSONArray.optTyped(index : Int) : TYPE? {
    return if(this.isNull(index)) {
        null
    } else when(TYPE::class) {
        Boolean::class ->   {
            val value = this.opt(index)
            when(value) {
                is Boolean  ->  value as TYPE
                is String   -> value.toBooleanOrNull() as TYPE
                else    ->  null
            }
        }
        Double::class -> {
            val value = this.opt(index)
            when (value) {
                is Double -> value as TYPE
                is Number -> value.toDouble() as TYPE
                is String -> value.toDoubleOrNull() as TYPE
                else -> null
            }
        }
        Int::class -> {
            val value = this.opt(index)
            when (value) {
                is Int -> value as TYPE
                is Number -> value.toInt() as TYPE
                is String -> value.toIntOrNull() as TYPE
                else -> null
            }
        }
        Long::class -> {
            val value = this.opt(index)
            when (value) {
                is Long -> value as TYPE
                is Number -> value.toLong() as TYPE
                is String -> value.toLongOrNull() as TYPE
                else -> null
            }
        }
        String::class ->    this.optString(index, null) as TYPE
        JSONArray::class -> this.optJSONArray(index) as TYPE
        JSONObject::class ->this.optJSONObject(index) as TYPE
        else -> throw JSONException("Only can be retrieved Boolean,Double,Int,Long,String,JSONArray or JSONObject from a JSONArray")
    }
}

@Throws(JSONException::class)
internal inline fun <reified TYPE> JSONArray.optTyped(index : Int, fallback : TYPE) : TYPE
        = this.optTyped(index = index) ?: fallback