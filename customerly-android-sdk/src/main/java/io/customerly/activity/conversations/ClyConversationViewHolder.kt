package io.customerly.activity.conversations

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
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import io.customerly.R
import io.customerly.entity.ClyConversation
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.ggkext.activity
import io.customerly.utils.ggkext.dp2px

/**
 * Created by Gianni on 27/04/18.
 * Project: Customerly-KAndroid-SDK
 */
internal class ClyConversationViewHolder(recyclerView: RecyclerView)
    : RecyclerView.ViewHolder(recyclerView.activity!!.layoutInflater.inflate(R.layout.io_customerly__li_conversation, recyclerView, false)) {

    private var conversationId = 0L

    private val icon = this.itemView.also {


        Log.e("ViewHolder", it.toString())
    }.findViewById<ImageView>(R.id.io_customerly__icon)
    private val name = this.itemView.findViewById<TextView>(R.id.io_customerly__name)
    private val lastMessage = this.itemView.findViewById<TextView>(R.id.io_customerly__last_message)
    private val time = this.itemView.findViewById<TextView>(R.id.io_customerly__time)

    init {
        this.icon.layoutParams.also {
            it.height = 50.dp2px
            it.width = 50.dp2px
        }
        this.itemView.setOnClickListener { v ->
            (v.activity as? ClyConversationsActivity)?.openConversationById(conversationId = this.conversationId, thenFinishCurrent = false)
        }
    }

    internal fun apply(conversationsActivity: ClyConversationsActivity, conversation: ClyConversation) {
        this.conversationId = conversation.id

//        this.icon.setImageResource(R.drawable.io_customerly__ic_default_admin)

//        val diskKey = CUSTOMERLY_SDK_NAME + '-' + "${conversation.getImageUrl(50.dp2px)}|${true}|${50.dp2px}|${50.dp2px}".hashCode()
//
//        val bitmapFile = File(File(conversationsActivity.cacheDir, CUSTOMERLY_SDK_NAME), diskKey)
//        if (bitmapFile.exists()) {
//            if (System.currentTimeMillis() - bitmapFile.lastModified() < 24 * 60 * 60 * 1000) {
//                BitmapFactory.decodeFile(bitmapFile.toString())?.also { bmp ->
//                    this.icon.setImageBitmap(bmp)
//                }
//            } else {
//                bitmapFile.delete()
//            }
//        } else {
//            this.icon.setImageResource(R.drawable.io_customerly__ic_default_admin)
//        }

        ClyImageRequest(context = conversationsActivity, url = conversation.getImageUrl(50.dp2px))
                .fitCenter()
                .transformCircle()
                .resize(width = 50.dp2px)
                .placeholder(placeholder = R.drawable.io_customerly__ic_default_admin)
                .into(imageView = this.icon)
                .start()

        this.name.text = conversation.lastMessage.getWriterName(context = conversationsActivity)
        this.lastMessage.text = conversation.lastMessage.message
        this.time.text = conversation.lastMessage.getTimeFormatted(context = conversationsActivity)
        this.itemView.isSelected = conversation.unread
    }

}