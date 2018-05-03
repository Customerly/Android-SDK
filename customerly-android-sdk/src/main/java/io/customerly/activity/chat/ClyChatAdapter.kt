package io.customerly.activity.chat

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

import android.support.v7.widget.RecyclerView
import android.view.ViewGroup
import io.customerly.R
import io.customerly.entity.ClyMessage
import io.customerly.utils.ggkext.activity
import io.customerly.utils.ggkext.dp2px
import io.customerly.utils.ggkext.weak
import kotlinx.android.synthetic.main.io_customerly__activity_chat.*

/**
 * Created by Gianni on 27/04/18.
 * Project: Customerly-KAndroid-SDK
 */
internal class ClyChatAdapter(chatActivity : ClyChatActivity) : RecyclerView.Adapter<ClyChatViewHolder>() {

    private val weakChatActivity = chatActivity.weak()

    private fun position2listIndex(position: Int) = if(this.weakChatActivity.get()?.typingAccountId != TYPING_NO_ONE) { position - 1 } else { position }

    override fun getItemViewType(position: Int): Int {
        val listIndex = this.position2listIndex(position = position)
        return when(listIndex) {
            -1 -> R.layout.io_customerly__li_bubble_account_typing
            else -> {
                val message = weakChatActivity.get()?.chatList?.get(index = listIndex)
                when {
                    message == null -> R.layout.io_customerly__li_bubble_account_rich
                    message.isUserMessage -> R.layout.io_customerly__li_bubble_user
                    message.richMailLink == null -> R.layout.io_customerly__li_bubble_account
                    else -> R.layout.io_customerly__li_bubble_account_rich
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClyChatViewHolder {
        val recyclerView: RecyclerView = (parent as? RecyclerView) ?: (parent.activity as ClyChatActivity).io_customerly__recycler_view
        return when (viewType) {
            R.layout.io_customerly__li_bubble_account_typing -> ClyChatViewHolder.Typing(recyclerView = recyclerView)
            R.layout.io_customerly__li_bubble_user -> ClyChatViewHolder.Message.User(recyclerView = recyclerView)
            R.layout.io_customerly__li_bubble_account -> ClyChatViewHolder.Message.Account(recyclerView = recyclerView)
            else/* R.layout.io_customerly__li_message_account_rich */ -> ClyChatViewHolder.Message.AccountRich(recyclerView = recyclerView)
        }
    }

    override fun onBindViewHolder(holder: ClyChatViewHolder, position: Int) {
        val listIndex = this.position2listIndex(position = position)
        this.weakChatActivity.get()?.let { chatActivity ->
            val message: ClyMessage? = if (listIndex == -1) {
                null
            } else {
                chatActivity.chatList[listIndex]
            }
            val previousMessage: ClyMessage? = if (listIndex == chatActivity.chatList.size - 1) {
                null
            } else {
                chatActivity.chatList[listIndex + 1]
            }
            val isFirstMessageOfSender = when {
                previousMessage == null -> true
                message == null && !previousMessage.isUserMessage -> true
                message != null && !message.hasSameWriter(of = previousMessage) -> true
                else -> false
            }
            val dateToDisplay = if (message == null || previousMessage != null && message.isSentSameDay(of = previousMessage)) {
                null
            } else {
                message.dateString
            }

            holder.apply(chatActivity = chatActivity, message = message, dateToDisplay = dateToDisplay, isFirstMessageOfSender = isFirstMessageOfSender)
            holder.itemView.setPadding(0, if (isFirstMessageOfSender) 15.dp2px else 0, 0, if (listIndex <= 0 /* -1 is typing item */) 5.dp2px else 0)
            //paddingTop = 15dp to every first message of the group
            //paddingBottom = 5dp to the last message of the chat
        }
    }

    override fun getItemCount(): Int
            = this.weakChatActivity.get()?.let { it.chatList.size + if (it.typingAccountId == TYPING_NO_ONE) { 0 } else { 1 } } ?: 0

    override fun onViewDetachedFromWindow(holder: ClyChatViewHolder) { holder.itemView.clearAnimation() }
}