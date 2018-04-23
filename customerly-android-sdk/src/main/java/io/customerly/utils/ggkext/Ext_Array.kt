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

import android.os.Build
import android.support.annotation.RequiresApi
import android.util.LongSparseArray
import org.intellij.lang.annotations.Pattern
import org.intellij.lang.annotations.RegExp

/**
 * Created by Gianni on 02/10/17.
 */
@RegExp
internal const val SQL_ID_LIST_REG_EX = "\\(\\s*\\d+\\s*(,\\s*\\d+\\s*)*\\)"

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun <ANY : Any> Sequence<ANY>.toSqlList(transform:(ANY)->CharSequence)
        = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun <ANY : Any> Iterable<ANY>.toSqlList(transform:(ANY)->CharSequence)
    = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun Iterable<CharSequence>.toSqlList()
        = this.iterator().toSqlList()

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun <ANY : Any> Array<out ANY>.toSqlList(transform:(ANY)->CharSequence)
        = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun ByteArray.toSqlList(transform:(Byte)->CharSequence)
        = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun CharArray.toSqlList(transform:(Char)->CharSequence)
        = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun ShortArray.toSqlList(transform:(Short)->CharSequence)
        = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun IntArray.toSqlList(transform:(Int)->CharSequence)
        = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun LongArray.toSqlList(transform:(Long)->CharSequence)
        = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun FloatArray.toSqlList(transform:(Float)->CharSequence)
        = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun DoubleArray.toSqlList(transform:(Double)->CharSequence)
        = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun BooleanArray.toSqlList(transform:(Boolean)->CharSequence)
        = this.iterator().toSqlList(transform)

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun <ANY : Any> Iterator<ANY>.toSqlList(transform:(ANY)->CharSequence) :String {
    return if(this.hasNext()) {
        val first = this.next()
        if(this.hasNext()) {
            this.asSequence()
                    .joinToString(separator = ",",
                            prefix = "(${transform.invoke(first)},",
                            postfix = ")",
                            transform = transform)
        } else {
            "(${transform.invoke(first)})"
        }
    } else {
        "(0)"
    }
}

@RegExp
@Pattern(SQL_ID_LIST_REG_EX)
internal fun Iterator<CharSequence>.toSqlList() :String {
    return if(this.hasNext()) {
        val first = this.next()
        if(this.hasNext()) {
            this.asSequence()
                    .joinToString(separator = ",",
                            prefix = "($first,",
                            postfix = ")")
        } else {
            "($first)"
        }
    } else {
        "(0)"
    }
}

internal inline fun <E, reified T> List<E>.toTypedMappedArray(map : (E)->T) : Array<T> {
    return Array(this.size) { map(this[it])}
}

internal infix fun <K,V> K.entry(that: V): Map.Entry<K, V> = MyMapEntry(this, that)

private data class MyMapEntry<out K, out V>(override val key: K, override val value: V) : Map.Entry<K,V>

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
internal inline fun <E> LongSparseArray<E>.forEach(filterNotNullValues : Boolean = false, action: (E) -> Unit) {
    (0 until this.size())
            .asSequence()
            .let { sequence ->
                if(filterNotNullValues) {
                    sequence.filter { this.valueAt(it) != null }
                } else {
                    sequence
                }
            }
            .forEach { action(this.valueAt(it)) }
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
internal inline fun <E> LongSparseArray<E>.forEachIndex(filterNotNullValues : Boolean = false, action: (Long) -> Unit) {
    (0 until this.size())
            .asSequence()
            .let { sequence ->
                if(filterNotNullValues) {
                    sequence.filter { this.valueAt(it) != null }
                } else {
                    sequence
                }
            }
            .forEach { action(this.keyAt(it)) }
}

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
internal inline fun <E> LongSparseArray<E>.forEachIndexed(filterNotNullValues : Boolean = false, action: (Long, E) -> Unit) {
    (0 until this.size())
            .asSequence()
            .let { sequence ->
                if(filterNotNullValues) {
                    sequence.filter { this.valueAt(it) != null }
                } else {
                    sequence
                }
            }
            .forEach { action(this.keyAt(it), this.valueAt(it)) }
}

internal fun <T> Collection<T>.random()  = this.takeIf(Collection<T>::isNotEmpty)?.elementAtOrNull((0 until this.size).random())
internal fun <T> Array<T>.random()       = this.takeIf(Array<T>::isNotEmpty)?.get((0 until this.size).random())
internal fun ByteArray.random()          = this.takeIf(ByteArray::isNotEmpty)?.get((0 until this.size).random())
internal fun CharArray.random()          = this.takeIf(CharArray::isNotEmpty)?.get((0 until this.size).random())
internal fun ShortArray.random()         = this.takeIf(ShortArray::isNotEmpty)?.get((0 until this.size).random())
internal fun IntArray.random()           = this.takeIf(IntArray::isNotEmpty)?.get((0 until this.size).random())
internal fun LongArray.random()          = this.takeIf(LongArray::isNotEmpty)?.get((0 until this.size).random())
internal fun FloatArray.random()         = this.takeIf(FloatArray::isNotEmpty)?.get((0 until this.size).random())
internal fun DoubleArray.random()        = this.takeIf(DoubleArray::isNotEmpty)?.get((0 until this.size).random())
internal fun BooleanArray.random()       = this.takeIf(BooleanArray::isNotEmpty)?.get((0 until this.size).random())