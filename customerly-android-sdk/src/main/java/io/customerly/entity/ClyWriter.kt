package io.customerly.entity

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

import android.content.Context
import android.support.annotation.Px
import io.customerly.R
import io.customerly.utils.WRITER_TYPE__ACCOUNT
import io.customerly.utils.WRITER_TYPE__USER
import io.customerly.utils.WriterType

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */
internal class ClyWriter(@WriterType val type : Int, internal val id : Long, private val name : String?) {

    private val isUser : Boolean = type == WRITER_TYPE__USER
    private val isAccount : Boolean = type == WRITER_TYPE__ACCOUNT

    internal fun getName(context : Context) : String {
        return this.name ?: context.getString(R.string.io_customerly__you)
    }

    internal fun getImageUrl(@Px sizePx: Int) : String {
        return if(this.isAccount) {
            urlImageAccount(accountId = this.id, sizePX = sizePx)
        } else {
            urlImageUser(userID = this.id, sizePX = sizePx)
        }
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (
                        this.javaClass == other?.javaClass
                    &&
                        other is ClyWriter
                    &&
                        this.id == other.id
                    &&
                        this.type == other.type)
    }

    override fun hashCode(): Int {
        var result = this.id.hashCode()
        result = 31 * result + this.isUser.hashCode()
        result = 31 * result + this.isAccount.hashCode()
        return result
    }


}