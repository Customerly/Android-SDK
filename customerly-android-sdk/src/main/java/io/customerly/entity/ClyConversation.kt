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

import android.support.annotation.Px
import android.text.Spanned
import io.customerly.utils.WriterType
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
                lastMessageAbstract = fromHtml(message = this.getTyped("last_message_abstract")),
                lastMessageDate = this.getTyped("last_message_date"),
                lastMessageWriterId = this.getTyped("last_message_writer"),
                lastMessageWriterType = this.getTyped("last_message_writer_type"),
                lastMessageWriterName = this.optTyped<JSONObject>("last_account")?.optTyped(name = "name"),
                unread = this.optTyped("unread", 0) == 1)
}

internal class ClyConversation (
        internal val id : Long,
        lastMessage : ClyConvLastMessage,
        internal var unread : Boolean = true) {

    internal var lastMessage : ClyConvLastMessage = lastMessage
        private set

    internal constructor(id : Long,
                         lastMessageAbstract : Spanned,
                         lastMessageDate : Long,
                         lastMessageWriterId : Long,
                         @WriterType lastMessageWriterType : Int,
                         lastMessageWriterName : String?,
                         unread : Boolean = true

        ) : this(
                id = id,
                lastMessage = ClyConvLastMessage(
                        message = lastMessageAbstract,
                        date = lastMessageDate,
                        writerType = lastMessageWriterType,
                        writerId = lastMessageWriterId,
                        writerName = lastMessageWriterName),
                unread = unread)

    internal fun onNewMessage(message : ClyMessage) {
        this.lastMessage = message.toConvLastMessage()
        this.unread = true
    }

    internal fun getImageUrl(@Px sizePx: Int) : String = this.lastMessage.getImageUrl(sizePx = sizePx)

}
