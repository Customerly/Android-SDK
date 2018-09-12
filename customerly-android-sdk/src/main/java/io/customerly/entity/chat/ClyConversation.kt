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

import android.support.annotation.Px
import android.text.Spanned
import android.widget.ImageView
import io.customerly.utils.download.imagehandler.ClyImageRequest
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
            lastMessageDiscarded = this.getTyped<Int>("last_message_discarded") == 1,
            writer = ClyWriter.Real.from(
                    type = this.getTyped("last_message_writer_type"),
                    id = this.getTyped("last_message_writer"),
                    name = this.optTyped<JSONObject>("last_account")?.optTyped(name = "name")),
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
                         lastMessageDiscarded : Boolean,
                         writer: ClyWriter,
                         unread : Boolean = true) : this(
                id = id,
                lastMessage = ClyConvLastMessage(
                        message = lastMessageAbstract,
                        date = lastMessageDate,
                        writer = writer,
                        discarded = lastMessageDiscarded),
                unread = unread)

    internal fun onNewMessage(message : ClyMessage) {
        this.lastMessage = message.toConvLastMessage()
        this.unread = true
    }

    internal fun loadUrl(into: ImageView, @Px size: Int): ClyImageRequest?
            = this.lastMessage.loadUrl(into = into, size = size)
}
