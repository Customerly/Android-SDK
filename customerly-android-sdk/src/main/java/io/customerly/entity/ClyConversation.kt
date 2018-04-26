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
import android.content.res.Resources
import android.support.annotation.Px
import android.text.Spanned
import io.customerly.R
import io.customerly.utils.WriterType
import io.customerly.utils.ggkext.formatByTimeAgo
import io.customerly.utils.ggkext.getTyped
import io.customerly.utils.ggkext.optTyped
import io.customerly.utils.htmlformatter.fromHtml
import org.json.JSONException
import org.json.JSONObject

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */

@Throws(JSONException::class)
internal fun JSONObject.parseConversation() : ClyConversation {
    return ClyConversation(
                id = this.getTyped("conversation_id"),
                lastMessage = fromHtml(message = this.getTyped("last_message_abstract")),
                lastMessageDate = this.getTyped("last_message_date"),
                lastMessageWriterId = this.getTyped("last_message_writer"),
                lastMessageWriterType = this.getTyped("last_message_writer_type"),
                lastMessageWriterName = this.optTyped<JSONObject>("last_account")?.optTyped(name = "name"),
                unread = this.optTyped("unread", 0) == 1)
}

internal class ClyConversation (

        internal val id : Long,
        lastMessage : Spanned,
        lastMessageDate : Long,
        lastMessageWriterId : Long,
        @WriterType lastMessageWriterType : Int,
        lastMessageWriterName : String?,
        internal var unread : Boolean = true

)
{
    private var lastMessage : ClyConvLastMessage = ClyConvLastMessage(message = lastMessage, date = lastMessageDate, writerType = lastMessageWriterType, writerId = lastMessageWriterId, writerName = lastMessageWriterName)

    internal fun onNewMessage(message : ClyMessage) {
        this.lastMessage = message.toConvLastMessage()
        this.unread = true
    }

    internal fun getImageUrl(@Px sizePx: Int) : String = this.lastMessage.getImageUrl(sizePx = sizePx)

    internal fun getLastWriterName(context : Context) : String = this.lastMessage.getLastWriterName(context = context)

    internal fun getLastMessageTimeFormatted(resources: Resources) : String {
        return this.lastMessage.date.formatByTimeAgo(
                seconds = { resources.getString(R.string.io_customerly__XX_sec_ago, it) },
                minutes = { resources.getString(R.string.io_customerly__XX_min_ago, it) },
                hours = { resources.getQuantityString(R.plurals.io_customerly__XX_hours_ago, it.toInt(), it) },
                days = { resources.getQuantityString(R.plurals.io_customerly__XX_days_ago, it.toInt(), it) },
                weeks = { resources.getQuantityString(R.plurals.io_customerly__XX_weeks_ago, it.toInt(), it) },
                months = { resources.getQuantityString(R.plurals.io_customerly__XX_months_ago, it.toInt(), it) } )
    }
}
