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

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Spanned
import android.util.TypedValue
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import io.customerly.Customerly
import io.customerly.R
import io.customerly.activity.ClyIInputActivity
import io.customerly.activity.chat.startClyChatActivity
import io.customerly.api.ClyApiRequest
import io.customerly.api.ClyApiResponse
import io.customerly.api.ENDPOINT_CONVERSATION_DISCARD
import io.customerly.api.ENDPOINT_CONVERSATION_RETRIEVE
import io.customerly.entity.ClyAdmin
import io.customerly.entity.chat.*
import io.customerly.entity.iamAnonymous
import io.customerly.entity.iamLead
import io.customerly.entity.iamUser
import io.customerly.sxdependencies.annotations.SXUiThread
import io.customerly.sxdependencies.SXContextCompat
import io.customerly.sxdependencies.SXSwipeRefreshLayoutOnRefreshListener
import io.customerly.sxdependencies.SXLinearLayoutManager
import io.customerly.sxdependencies.textviewSingleLine
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.ggkext.*
import io.customerly.utils.playNotifSound
import io.customerly.utils.ui.RVDIVIDER_V_BOTTOM
import io.customerly.utils.ui.RvDividerDecoration
import kotlinx.android.synthetic.main.io_customerly__activity_list.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Created by Gianni on 03/09/16.
 * Project: Customerly Android SDK
 */
private const val REQUEST_CODE_THEN_REFRESH_LIST = 100

internal fun Activity.startClyConversationsActivity() {
    if (this !is ClyConversationsActivity) {
        this.startActivity(Intent(this, ClyConversationsActivity::class.java))
    }
}

internal class ClyConversationsActivity : ClyIInputActivity() {

    private val onRefreshListener = object : SXSwipeRefreshLayoutOnRefreshListener {
        private val weakActivity = this@ClyConversationsActivity.weak()
        override fun onRefresh() {
            weakActivity.get()?.also { activity ->
                if (Customerly.iamLead() || Customerly.iamUser()) {
                    ClyApiRequest(
                            context = activity,
                            endpoint = ENDPOINT_CONVERSATION_RETRIEVE,
                            requireToken = true,
                            trials = 2,
                            jsonObjectConverter = { it.optArrayList(name = "conversations", map = JSONObject::parseConversation) ?: ArrayList(0) },
                            callback = {
                                activity.displayInterface(conversationsList = when (it) {
                                    is ClyApiResponse.Success -> it.result
                                    is ClyApiResponse.Failure -> null
                                })
                            })
                            .p(key = "lead_hash", value = Customerly.currentUser.leadHash)
                            .start()
                } else {
                    activity.displayInterface(conversationsList = null)
                }
            }
        }
    }

    internal var conversationsList: ArrayList<ClyConversation> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (this.onCreateLayout(R.layout.io_customerly__activity_list)) {

            this.io_customerly__recycler_view.also {
                it.layoutManager = SXLinearLayoutManager(this.applicationContext)
                it.itemAnimator = null
                it.setHasFixedSize(true)
                it.addItemDecoration(RvDividerDecoration.Vertical(context = this, colorRes = R.color.io_customerly__li_conversation_divider_color, where = RVDIVIDER_V_BOTTOM))
                it.adapter = ClyConversationsAdapter(conversationsActivity = this)
            }

            this.io_customerly__new_conversation_button.setOnClickListener {
                (it.activity as? ClyConversationsActivity)?.startClyChatActivity(conversationId = CONVERSATIONID_UNKNOWN_FOR_MESSAGE, mustShowBack = true, requestCode = REQUEST_CODE_THEN_REFRESH_LIST)
            }

            this.io_customerly__recycler_view_swipe_refresh.setOnRefreshListener(this.onRefreshListener)
            this.io_customerly__first_contact_swipe_refresh.setOnRefreshListener(this.onRefreshListener)
            this.onReconnection()
        }
    }

    @SXUiThread
    override fun onNewSocketMessages(messages: ArrayList<ClyMessage>) {
        val newConversationList = ArrayList(this.conversationsList)
        newConversationList.apply {
            this.addAll(
                    elements = messages.asSequence().groupBy { it.conversationId }
                            .mapNotNull { (conversationId,messages) ->
                                //Get list of most recent message groupedBy conversationId
                                messages.maxBy { it.id }?.let { lastMessage ->
                                    val existingConversation = this.find { it.id == conversationId }
                                    if(existingConversation != null) {
                                        //if the message belongs to a conversation already in list, the conversation item is updated and discarded from this sequence
                                        existingConversation.onNewMessage(message = lastMessage)
                                        null
                                    } else {
                                        //message of a new conversation: a new Conversation instance is created
                                        lastMessage.toConversation()
                                    }
                                }
                            }.toArrayList())
            this.sortByDescending { it.lastMessage.date }
        }
        this.displayInterface(conversationsList = newConversationList)

        this.playNotifSound()
    }

    override fun onResume() {
        super.onResume()
        if (Customerly.jwtToken == null) {
            this.onLogoutUser()
        }
    }

    override fun onLogoutUser() {
        this.finish()
    }

    override fun onReconnection() {
        this.io_customerly__recycler_view_swipe_refresh.isRefreshing = true
        this.io_customerly__first_contact_swipe_refresh.isRefreshing = true
        this.onRefreshListener.onRefresh()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_THEN_REFRESH_LIST) {
            this.onReconnection()
        }
    }

    @SXUiThread
    private fun displayInterface(conversationsList: ArrayList<ClyConversation>?) {
        if(conversationsList?.isNotEmpty() == true) {
            this.io_customerly__first_contact_swipe_refresh.visibility = View.GONE

            conversationsList
                    .asSequence()
                    .filter { it.unread && !it.lastMessage.discarded }
                    .fold(JSONArray()) { ja, c ->
                        c.lastMessage.discarded = true
                        ja.put(c.id)
                    }.takeIf {
                        it.length() != 0
                    }?.also { conversation_ids ->
                        ClyApiRequest<Any>(
                                context = this,
                                endpoint = ENDPOINT_CONVERSATION_DISCARD,
                                requireToken = true,
                                jsonObjectConverter = { it })
                                .p(key = "conversation_ids", value = conversation_ids)
                                .start()
                    }

            this.conversationsList = conversationsList
            this.io_customerly__recycler_view.adapter?.notifyDataSetChanged()
            this.inputLayout?.visibility = View.GONE
            this.io_customerly__new_conversation_layout.visibility = View.VISIBLE
            this.io_customerly__recycler_view_swipe_refresh.visibility = View.VISIBLE
            this.io_customerly__recycler_view_swipe_refresh.isRefreshing = false
            this.io_customerly__first_contact_swipe_refresh.isRefreshing = false
        } else {//Layout first contact
            this.conversationsList.clear()
            this.io_customerly__recycler_view_swipe_refresh.visibility = View.GONE
            this.io_customerly__new_conversation_layout.visibility = View.GONE
            this.restoreAttachments()
            this.inputLayout?.visibility = View.VISIBLE
            this.io_customerly__recycler_view_swipe_refresh.isRefreshing = false
            this.io_customerly__first_contact_swipe_refresh.isRefreshing = false
            var showWelcomeCard = false
            this.io_customerly__layout_first_contact__admin_container.removeAllViews()
            val admins: Array<ClyAdmin>? = Customerly.lastPing.activeAdmins
            admins?.asSequence()?.filterNotNull()?.forEach { admin ->
                this.io_customerly__layout_first_contact__admin_container.addView(
                        LinearLayout(this).apply {
                            this.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                            this.gravity = Gravity.CENTER_HORIZONTAL
                            this.orientation = LinearLayout.VERTICAL
                            this.setPadding(10.dp2px, 0, 10.dp2px, 0)

                            this.addView(
                                    ImageView(this.context).apply {
                                        this.layoutParams = LinearLayout.LayoutParams(45.dp2px, 45.dp2px).apply {
                                            this.topMargin = 10.dp2px
                                            this.bottomMargin = 10.dp2px
                                        }

                                        ClyImageRequest(context = this.context, url = admin.getImageUrl(45.dp2px))
                                                .fitCenter()
                                                .transformCircle()
                                                .resize(width = 45.dp2px)
                                                .placeholder(R.drawable.io_customerly__ic_default_admin)
                                                .into(imageView = this)
                                                .start()
                                    })

                            this.addView(
                                    TextView(this.context).apply {
                                        this.layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                        this.setTextColor(SXContextCompat.getColor(this.context, R.color.io_customerly__welcomecard_texts))
                                        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                        this.text = admin.name
                                        textviewSingleLine(this, false)
                                        this.minLines = 2
                                        this.maxLines = 3
                                        this.gravity = Gravity.CENTER_HORIZONTAL
                                        this.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                                    })
                        })
            }

            admins?.asSequence()?.mapNotNull { it.lastActive }?.max()?.let { lastActiveAdmin ->
                showWelcomeCard = true
                this.io_customerly__layout_first_contact__last_activity.also { tv ->
                    tv.text = lastActiveAdmin.formatByTimeAgo(
                            seconds = { this.getString(R.string.io_customerly__last_activity_now) },
                            minutes = { this.resources.getQuantityString(R.plurals.io_customerly__last_activity_XXm_ago, it.toInt(), it) },
                            hours   = { this.resources.getQuantityString(R.plurals.io_customerly__last_activity_XXh_ago, it.toInt(), it) },
                            days    = { this.resources.getQuantityString(R.plurals.io_customerly__last_activity_XXd_ago, it.toInt(), it) })

                    tv.visibility = View.VISIBLE
                }
            }

            val welcome : Spanned? = Customerly.welcomeMessage
            if (welcome?.isNotEmpty() == true) {
                showWelcomeCard = true
                this.io_customerly__layout_first_contact__welcome.apply {
                    this.text = welcome
                    this.visibility = View.VISIBLE
                }
            }

            this.io_customerly__layout_first_contact.visibility = if(showWelcomeCard) View.VISIBLE else View.GONE

            this.io_customerly__first_contact_swipe_refresh.visibility = View.VISIBLE
        }
    }

    override fun onSendMessage(content: String, attachments: Array<ClyAttachment>) {
        this.startClyChatActivity(
                conversationId = CONVERSATIONID_UNKNOWN_FOR_MESSAGE,
                messageContent = content,
                attachments = attachments.toList().toArrayList(),
                mustShowBack = Customerly.iamLead() || Customerly.iamUser(),
                requestCode = REQUEST_CODE_THEN_REFRESH_LIST)
        if(Customerly.iamAnonymous()) {
            this.finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    internal fun openConversationById(conversationId: Long) {
        if(conversationId != 0L && this.checkConnection()) {
            this.startClyChatActivity(conversationId = conversationId, mustShowBack = true, requestCode = REQUEST_CODE_THEN_REFRESH_LIST)
        } else {
            Toast.makeText(this.applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
        }
    }

}
