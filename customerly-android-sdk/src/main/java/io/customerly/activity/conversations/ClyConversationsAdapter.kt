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

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.customerly.utils.ggkext.activity
import io.customerly.utils.ggkext.weak
import kotlinx.android.synthetic.main.io_customerly__activity_list.*

/**
 * Created by Gianni on 27/04/18.
 * Project: Customerly-KAndroid-SDK
 */
internal class ClyConversationsAdapter(conversationsActivity : ClyConversationsActivity) : RecyclerView.Adapter<ClyConversationViewHolder>() {

    private val weakConversationsActivity = conversationsActivity.weak()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClyConversationViewHolder
        = ClyConversationViewHolder(recyclerView = (parent as? RecyclerView) ?: (parent.activity as ClyConversationsActivity).io_customerly__recycler_view)

    override fun onBindViewHolder(holder: ClyConversationViewHolder, position: Int) {
        this.weakConversationsActivity.get()?.let { holder.apply(conversationsActivity = it, conversation = it.conversationsList[position]) }
    }

    override fun getItemCount(): Int  = this.weakConversationsActivity.get()?.conversationsList?.size ?: 0

    override fun onViewRecycled(holder: ClyConversationViewHolder) {
        holder.onViewRecycled()
    }
}