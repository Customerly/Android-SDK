package io.customerly.entity.chat

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
import android.widget.ImageView
import androidx.annotation.Px
import io.customerly.R
import io.customerly.entity.urlImageAccount
import io.customerly.entity.urlImageUser
import io.customerly.utils.WRITER_TYPE__ACCOUNT
import io.customerly.utils.WRITER_TYPE__BOT
import io.customerly.utils.WRITER_TYPE__USER
import io.customerly.utils.WriterType
import io.customerly.utils.download.imagehandler.ClyImageRequest

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */
internal sealed class ClyWriter(@WriterType private val type : Int, internal var id : Long, private val name : String?) {

    val isUser : Boolean get() = this.type == WRITER_TYPE__USER
    val isAccount : Boolean get() = this.type == WRITER_TYPE__ACCOUNT
    val isBot : Boolean get() = this.type == WRITER_TYPE__BOT

    internal fun getName(context : Context) : String {
        return this.name ?: context.getString(R.string.io_customerly__you)
    }

    private fun getImageUrl(@Px sizePx: Int) : String {
        return when {
            this.isUser -> urlImageUser(userID = this.id, sizePX = sizePx)
            this.isAccount -> urlImageAccount(accountId = this.id, sizePX = sizePx, name = this.name)
            else -> ""
        }
    }

    internal fun loadUrl(into: ImageView, @Px sizePx: Int): ClyImageRequest? {
        return when {
            this.isUser || this.isAccount -> {
                ClyImageRequest(
                        context = into.context,
                        url = this.getImageUrl(sizePx = sizePx))
                        .fitCenter()
                        .transformCircle()
                        .resize(width = sizePx)
                        .placeholder(placeholder = R.drawable.io_customerly__ic_default_admin)
                        .into(imageView = into)
                        .start()
            }
            else -> {
                if(this.isBot) {
                    into.setImageResource(R.drawable.io_customerly__ic_bot_50dp)
                }
                null
            }
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

    internal sealed class Real(@WriterType type : Int, id : Long, name : String?)
        : ClyWriter(type = type, id = id, name = name) {

        internal class User(userId: Long, name: String?): Real(type = WRITER_TYPE__USER, id = userId, name = name)

        internal class Account(accountId: Long, name: String?): Real(type = WRITER_TYPE__ACCOUNT, id = accountId, name = name)

        companion object {
            fun from(userId: Long = 0, accountId: Long = 0, name: String?) : Real {
                return if(userId != 0L) {
                    User(userId = userId, name = name)
                } else {
                    Account(accountId = accountId, name = name)
                }
            }
            fun from(@WriterType type : Int, id : Long, name : String?) : Real {
                return if(type == WRITER_TYPE__USER) {
                    User(userId = id, name = name)
                } else {
                    Account(accountId = id, name = name)
                }
            }
        }
    }

    internal object Bot: ClyWriter(type = WRITER_TYPE__BOT, id = -1, name = null)
}