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
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.media.MediaPlayer
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.util.Patterns
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
import io.customerly.api.*
import io.customerly.entity.*
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.ggkext.*
import io.customerly.utils.ui.RVDIVIDER_V_BOTTOM
import io.customerly.utils.ui.RvDividerDecoration
import kotlinx.android.synthetic.main.io_customerly__activity_list.*
import org.json.JSONObject
import java.util.Locale
import kotlin.collections.ArrayList

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

    private val onRefreshListener = object : SwipeRefreshLayout.OnRefreshListener {
        private val weakActivity = this@ClyConversationsActivity.weak()
        override fun onRefresh() {
            weakActivity.get()?.also { activity ->
                if (Customerly.jwtToken?.isAnonymous != true) {
                    ClyApiRequest(
                            context = activity,
                            endpoint = ENDPOINT_CONVERSATION_RETRIEVE,
                            requireToken = true,
                            trials = 2,
                            converter = { it.optArrayList(name = "conversations", map = JSONObject::parseConversation) ?: ArrayList(0) },
                            callback = {
                                activity.displayInterface(conversationsList = when (it) {
                                    is ClyApiResponse.Success -> it.result
                                    is ClyApiResponse.Failure -> null
                                })
                            })
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
                it.layoutManager = LinearLayoutManager(this.applicationContext)
                it.itemAnimator = DefaultItemAnimator()
                it.setHasFixedSize(true)
                it.addItemDecoration(RvDividerDecoration.Vertical(context = this, colorRes = R.color.io_customerly__li_conversation_divider_color, where = RVDIVIDER_V_BOTTOM))
                it.adapter = ClyConversationsAdapter(conversationsActivity = this)
            }

            this.inputLayout?.visibility = View.GONE
            this.io_customerly__new_conversation_button.setOnClickListener {
                (it.activity as? ClyConversationsActivity)?.let { activity ->
                    activity.io_customerly__new_conversation_layout.visibility = View.GONE
                    activity.restoreAttachments()
                    activity.inputLayout?.visibility = View.VISIBLE
                }
            }

            this.io_customerly__recycler_view_swipe_refresh.setOnRefreshListener(this.onRefreshListener)
            this.io_customerly__first_contact_swipe_refresh.setOnRefreshListener(this.onRefreshListener)
            this.onReconnection()
        }
    }

    override fun onNewSocketMessages(messages: ArrayList<ClyMessage>) {
        val newConversationList = ArrayList(this.conversationsList)
        newConversationList.apply {
            this.addAll(
                    elements = messages.asSequence().groupBy { it.conversationId }
                            .mapNotNull { (conversationId,messages) ->
                                //Get list of most recent message groupedBy conversationId
                                messages.maxBy { it.id }?.let { lastMessage ->
                                    this.find { it.id == conversationId }?.let { existingConversation ->
                                        //if the message belongs to a conversation already in list, the conversation item is updated and discarded from this sequence
                                        existingConversation.onNewMessage(message = lastMessage)
                                        null
                                    } ?: lastMessage.toConversation() //message of a new conversation: a new Conversation instance is created
                                }
                            }.toArrayList())
            this.sortBy { it.lastMessage.date }
        }
        this.displayInterface(conversationsList = newConversationList)

        MediaPlayer.create(this, R.raw.notif_2).apply {
            this.setOnCompletionListener {
                it.reset()
                it.release()
            }
        }.start()
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

    override fun onBackPressed() {
        if (this.io_customerly__input_email_layout?.visibility == View.VISIBLE) {
            this.io_customerly__input_email_layout?.visibility = View.GONE
            this.inputLayout?.visibility = View.VISIBLE
        } else {
            super.onBackPressed()
        }
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

    private fun displayInterface(conversationsList: ArrayList<ClyConversation>?) {
        val weakActivity = this.weak()
        if(conversationsList?.isNotEmpty() == true) {
            this.io_customerly__first_contact_swipe_refresh.visibility = View.GONE
            this.io_customerly__recycler_view.post {
                weakActivity.get()?.let {
                    it.conversationsList = conversationsList
                    it.io_customerly__recycler_view.adapter.notifyDataSetChanged()
                }
            }
            this.inputLayout?.visibility = View.GONE
            this.io_customerly__input_email_layout.visibility = View.GONE
            this.io_customerly__new_conversation_layout.visibility = View.VISIBLE
            this.io_customerly__recycler_view_swipe_refresh.visibility = View.VISIBLE
            this.io_customerly__recycler_view_swipe_refresh.isRefreshing = false
            this.io_customerly__first_contact_swipe_refresh.isRefreshing = false
        } else {//Layout first contact
            this.io_customerly__recycler_view_swipe_refresh.visibility = View.GONE
            this.io_customerly__input_email_layout.visibility = View.GONE
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
                                        this.setTextColor(ContextCompat.getColor(this.context, R.color.io_customerly__welcomecard_texts))
                                        this.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                                        this.text = admin.name
                                        this.setSingleLine(false)
                                        this.minLines = 2
                                        this.maxLines = 3
                                        this.gravity = Gravity.CENTER_HORIZONTAL
                                        this.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                                    })
                        })
            }

            admins?.asSequence()?.mapNotNull { it.last_active }?.max()?.let { lastActiveAdmin ->
                showWelcomeCard = true
                this.io_customerly__layout_first_contact__last_activity.also {
                    it.text = lastActiveAdmin.formatByTimeAgo(
                            seconds = { this.getString(R.string.io_customerly__last_activity_now) },
                            minutes = { this.resources.getQuantityString(R.plurals.io_customerly__last_activity_XXm_ago, it.toInt(), it) },
                            hours   = { this.resources.getQuantityString(R.plurals.io_customerly__last_activity_XXh_ago, it.toInt(), it) },
                            days    = { this.resources.getQuantityString(R.plurals.io_customerly__last_activity_XXd_ago, it.toInt(), it) })

                    it.visibility = View.VISIBLE
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

    override fun onSendMessage(content: String, attachments: Array<ClyAttachment>, ghostToVisitorEmail: String?) {
        val weakActivity = this.weak()
        if(Customerly.jwtToken?.isAnonymous != false) {
            if(ghostToVisitorEmail == null) {
                this.inputLayout?.visibility = View.GONE
                this.io_customerly__input_email_layout.visibility = View.VISIBLE

                this.io_customerly__input_email_edit_text.requestFocus()
                this.io_customerly__input_email_button.setOnClickListener { _ ->
                    weakActivity.get()?.let { activity ->
                        val emailEt = activity.io_customerly__input_email_edit_text
                        val email = emailEt.text.toString().trim().toLowerCase(Locale.ITALY)
                        if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                            activity.io_customerly__input_email_layout.visibility = View.GONE
                            emailEt.text = null
                            activity.inputLayout?.visibility = View.VISIBLE
                            activity.onSendMessage(content = content, attachments = attachments, ghostToVisitorEmail = email)
                        } else {
                            if (emailEt.tag == null) {
                                emailEt.tag = Unit
                                val originalColor = emailEt.textColors.defaultColor
                                emailEt.setTextColor(Color.RED)
                                emailEt.addTextChangedListener(object : TextWatcher {
                                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                                    override fun afterTextChanged(s: Editable) {}
                                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                                        weakActivity.get()?.io_customerly__input_email_edit_text?.also {
                                            it.setTextColor(originalColor)
                                            it.removeTextChangedListener(this)
                                            it.tag = null
                                        }
                                    }
                                })
                            }
                        }
                    }
                }
            } else {
                //TODO Replace deprecated ProgressDialog
                val progressDialog = ProgressDialog.show(this, this.getString(R.string.io_customerly__new_conversation), this.getString(R.string.io_customerly__sending_message), true, false)

                ClyApiRequest(
                        context = this,
                        endpoint = ENDPOINT_PING,
                        converter = { Unit },
                        callback = { response ->
                            when(response) {
                                is ClyApiResponse.Success -> {
                                    weakActivity.get()?.doLeadUserSendMessage(progressDialog = progressDialog, content = content, attachments = attachments, thenFinishCurrent = true)
                                }
                                is ClyApiResponse.Failure -> {
                                    progressDialog.ignoreException { it.dismiss() }
                                    weakActivity.get()?.also { activity ->
                                        activity.inputInput?.setText(content)
                                        attachments.forEach { it.addAttachmentToInput(inputActivity = activity) }
                                        Toast.makeText(activity.applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        })
                        .p(key = "lead_email", value = ghostToVisitorEmail)
                        .start()
            }
        } else {
            this.doLeadUserSendMessage(content = content, attachments = attachments, thenFinishCurrent = ghostToVisitorEmail != null)
        }
    }

    private fun doLeadUserSendMessage(
            //TODO Replace deprecated ProgressDialog
            progressDialog: ProgressDialog = ProgressDialog.show(
                    this,
                    this.getString(R.string.io_customerly__new_conversation),
                    this.getString(R.string.io_customerly__sending_message),
                    true,
                    false),
            content: String,
            attachments: Array<ClyAttachment>,
            thenFinishCurrent: Boolean) {

        val weakActivity = this.weak()
        ClyApiRequest(
                context = this,
                endpoint = ENDPOINT_MESSAGE_SEND,
                requireToken = true,
                trials = 2,
                converter = { data ->
                    data
                            .takeIf { data.has("conversation") }
                            ?.optJSONObject("message")
                            ?.also { Customerly.clySocket.sendMessage(timestamp = data.optLong("timestamp", -1L)) }
                            ?.optLong("conversation_id", -1L)
                            ?: -1L
                },
                callback = { response ->
                    progressDialog.ignoreException { it.dismiss() }
                    weakActivity.get()?.also { activity ->
                        when {
                            response is ClyApiResponse.Success && response.result != -1L -> {
                                activity.openConversationById(conversationId = response.result, thenFinishCurrent = thenFinishCurrent)
                            }
                            else/* response is ClyApiResponse.Success && response.result == -1L || response is ClyApiResponse.Failure */ -> {
                                activity.inputInput?.setText(content)
                                attachments.forEach { it.addAttachmentToInput(inputActivity = activity) }
                                Toast.makeText(activity.applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                })
                .p(key = "message", value = content)
                .p(key = "attachments", value = attachments.toJSON(context = this))
                .start()
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

    internal fun openConversationById(conversationId: Long, thenFinishCurrent: Boolean = false) {
        if(conversationId != 0L && this.checkConnection()) {
            this.startClyChatActivity(conversationId = conversationId, mustShowBack = !thenFinishCurrent, requestCode = REQUEST_CODE_THEN_REFRESH_LIST)
            if(thenFinishCurrent) {
                this.finish()
            }
        } else {
            Toast.makeText(this.applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
        }
    }

}
