@file:Suppress("unused", "UNUSED_PARAMETER")

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

import org.jetbrains.annotations.Contract

/**
 * Created by Gianni on 12/08/17.
 */

internal infix fun <FROM, NOT> FROM.not(not : NOT?): FROM?
    =   if(this != not) {
            this
        } else {
            null
        }

internal inline fun <FROM, NOT, DEST> FROM?.not(not : NOT, block: (FROM?) -> DEST): DEST?
        =   (this not not)?.let { block(it) }

internal inline infix fun <FROM, DEST> FROM?.ifNull(block: () -> DEST?): DEST?
        =   if(this == null) block() else null

internal inline infix fun <FROM, DEST> FROM?.ifNotNull(block : ()->DEST?) : DEST?
        = this?.let { block() }

internal inline fun <FROM1, FROM2, DEST> ifNotNull(from1 : FROM1?, from2 : FROM2?, block : (FROM1, FROM2)->DEST?) : DEST?
        = if(from1 != null && from2 != null) block(from1, from2) else null

internal inline fun <FROM1, FROM2, FROM3, DEST> ifNotNull(from1 : FROM1, from2 : FROM2, from3 : FROM3, block : (FROM1, FROM2, FROM3)->DEST?) : DEST?
        = if(from1 != null && from2 != null) block(from1, from2, from3) else null

internal inline fun <FROM1, FROM2, FROM3, FROM4, DEST> ifNotNull(from1 : FROM1, from2 : FROM2, from3 : FROM3, from4 : FROM4, block : (FROM1, FROM2, FROM3, FROM4)->DEST?) : DEST?
        = if(from1 != null && from2 != null && from3 != null && from4 != null) block(from1, from2, from3, from4) else null

internal inline infix fun <FROM, DEST> FROM?.withNotNull(block: (FROM) -> DEST?): DEST?
        =   if(this != null) {
                block(this)
            } else null

infix fun <ITEM> Array<ITEM>.ifMinSize(minSize : Int): Array<ITEM>?
        =   if(this.size >= minSize) {
                this
            } else {
                null
            }

internal infix fun <ITEM> List<ITEM>.ifMinSize(minSize : Int): List<ITEM>?
        =   if(this.size >= minSize) {
                this
            } else {
                null
            }

internal infix fun <ITEM> List<ITEM>.optAt(index : Int): ITEM?
        =   if(this.size > index) {
                this[index]
            } else {
                null
            }

internal infix fun Int.ifMin(minInclusive : Int): Int?
        =   if(this >= minInclusive) {
            this
        } else {
            null
        }

internal inline fun <RETURN> Int.withIfMin(minInclusive : Int, ifMin : (Int)->RETURN, ifLess : (Int)->RETURN) : RETURN?
        = this.ifMin(minInclusive) withNotNull ifMin

internal infix fun Long.ifMin(minInclusive : Long): Long?
        =   if(this >= minInclusive) {
            this
        } else {
            null
        }

internal inline fun <RETURN> Long.withIfMin(minInclusive : Long, ifMin : (Long)->RETURN, ifLess : (Long)->RETURN) : RETURN?
        = this.ifMin(minInclusive) withNotNull ifMin

@Contract("_, _, { _,_ ->!null}, {_->!null}, {_->!null}, {!null} -> null ")
internal fun <ITEM1, ITEM2, RETURN> notNullCheck(
        item1: ITEM1?,
        item2: ITEM2?,
        both : ((ITEM1,ITEM2)->RETURN?)? = null,
        first : ((ITEM1)->RETURN?)? = null,
        second : ((ITEM2)->RETURN?)? = null,
        noone : (()->RETURN?)? = null) : RETURN?
    = when(item1) {
        null -> when(item2) {
            null -> noone?.invoke()
            else -> second?.invoke(item2)
        }
        else -> when(item2) {
            null -> first?.invoke(item1)
            else -> both?.invoke(item1, item2)
        }
    }


internal fun areAllNull(vararg elements: Any?) : Boolean = if (elements.isEmpty()) false else elements.asSequence().all { it == null }
internal fun areAllNotNull(vararg elements: Any?) : Boolean = if (elements.isEmpty()) false else elements.asSequence().all { it != null }

internal fun areAnyNull(vararg elements: Any?) : Boolean = if (elements.isEmpty()) false else elements.asSequence().any { it == null }
internal fun areAnyNotNull(vararg elements: Any?) : Boolean = if (elements.isEmpty()) false else elements.asSequence().any { it != null }