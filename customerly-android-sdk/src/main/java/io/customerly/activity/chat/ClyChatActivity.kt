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

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.annotation.ColorInt
import android.support.annotation.UiThread
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.DefaultItemAnimator
import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import io.customerly.Customerly
import io.customerly.R
import io.customerly.activity.CLYINPUT_EXTRA_MUST_SHOW_BACK
import io.customerly.activity.ClyIInputActivity
import io.customerly.alert.showClyAlertMessage
import io.customerly.api.*
import io.customerly.entity.ClyAdminFull
import io.customerly.entity.chat.*
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.download.startFileDownload
import io.customerly.utils.getContrastBW
import io.customerly.utils.ggkext.*
import io.customerly.utils.network.ClySntpClient
import io.customerly.utils.ui.RvProgressiveScrollListener
import kotlinx.android.synthetic.main.io_customerly__activity_chat.*
import java.lang.ref.WeakReference
import java.util.*

/**
 * Created by Gianni on 03/09/16.
 * Project: Customerly Android SDK
 */

private const val EXTRA_CONVERSATION_ID = "EXTRA_CONVERSATION_ID"
private const val EXTRA_MESSAGE_CONTENT = "EXTRA_MESSAGE_CONTENT"
private const val EXTRA_MESSAGE_ATTACHMENTS = "EXTRA_MESSAGE_ATTACHMENTS"
private const val MESSAGES_PER_PAGE = 20
internal const val TYPING_NO_ONE = 0L
private const val PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE = 4321

internal fun Activity.startClyChatActivity(conversationId: Long, messageContent:String? = null, attachments: ArrayList<ClyAttachment>? = null, mustShowBack: Boolean = true, requestCode: Int = -1) {
    val intent = Intent(this, ClyChatActivity::class.java)
    if (conversationId > 0) {
        intent.putExtra(EXTRA_CONVERSATION_ID, conversationId)
    }
    if (messageContent != null) {
        intent.putExtra(EXTRA_MESSAGE_CONTENT, messageContent)
    }
    if (attachments != null && attachments.isNotEmpty()) {
        intent.putParcelableArrayListExtra(EXTRA_MESSAGE_ATTACHMENTS, attachments)
    }
    if (requestCode == -1) {
        if (this is ClyChatActivity) {
            //If i am starting a IAct_Chat Activity from a IAct_Chat activity i'll show the back button only if it is visible in the current IAct_Chat activity.
            //Then i finish the current activity to avoid long stack of IAct_Chat activities
            this.startActivity(intent.putExtra(CLYINPUT_EXTRA_MUST_SHOW_BACK, this.mustShowBack))
            this.finish()
        } else {
            this.startActivity(intent.putExtra(CLYINPUT_EXTRA_MUST_SHOW_BACK, mustShowBack))
        }
    } else {
        this.startActivityForResult(intent.putExtra(CLYINPUT_EXTRA_MUST_SHOW_BACK, mustShowBack), requestCode)
    }
}

internal class ClyChatActivity : ClyIInputActivity() {

    private var conversationId: Long? = null
    internal var typingAccountId = TYPING_NO_ONE
    internal var chatList = ArrayList<ClyMessage>(0)
    internal var conversationFullAdmin: ClyAdminFull? = null
    private val onBottomReachedListener = object : Function1<RvProgressiveScrollListener,Unit> {
        private val wActivity : WeakReference<ClyChatActivity> = this@ClyChatActivity.weak()
        override fun invoke(scrollListener: RvProgressiveScrollListener) {
            this.wActivity.get()?.let { activity ->
                Customerly.checkConfigured {
                    activity.conversationId?.also { conversationId ->
                        ClyApiRequest(
                                context = this.wActivity.get(),
                                endpoint = ENDPOINT_MESSAGE_RETRIEVE,
                                requireToken = true,
                                trials = 2,
                                onPreExecute = { this.wActivity.get()?.io_customerly__progress_view?.visibility = View.VISIBLE },
                                jsonObjectConverter = { it.parseMessagesList() },
                                callback = { response ->
                                    when (response) {
                                        is ClyApiResponse.Success -> {
                                            this.wActivity.get()?.let { activity ->

                                                response.result.sortByDescending { it.id }
                                                val newChatList = ArrayList(activity.chatList)
                                                val previousSize = newChatList.size
                                                val scrollToLastUnread = response.result
                                                        .asSequence()
                                                        .filterNot { newChatList.contains(it) }//Avoid duplicates
                                                        .mapIndexedNotNull { index, clyMessage ->
                                                            newChatList.add(clyMessage)//Add new not duplicate message to list
                                                            if (clyMessage.isNotSeen) {//If not seen map it to his index, discard it otherwise
                                                                index
                                                            } else {
                                                                null
                                                            }
                                                        }
                                                        .min() ?: -1
                                                val addedMessagesCount = newChatList.size - previousSize

                                                activity.io_customerly__progress_view.visibility = View.GONE
                                                activity.io_customerly__recycler_view.visibility = View.VISIBLE

                                                if (addedMessagesCount > 0) {
                                                    activity.setNewChatList(newChatList = newChatList)
                                                    (activity.io_customerly__recycler_view.layoutManager as? LinearLayoutManager)?.let { llm ->
                                                        if (llm.findFirstCompletelyVisibleItemPosition() == 0) {
                                                            llm.scrollToPosition(0)
                                                        } else if (previousSize == 0 && scrollToLastUnread != -1) {
                                                            llm.scrollToPosition(scrollToLastUnread)
                                                        }
                                                    }
                                                    (activity.io_customerly__recycler_view.adapter as? ClyChatAdapter)?.let { adapter ->
                                                        if (previousSize != 0) {
                                                            adapter.notifyItemChanged(adapter.listIndex2position(previousSize - 1))
                                                        }
                                                        adapter.notifyItemRangeInserted(adapter.listIndex2position(previousSize), adapter.listIndex2position(addedMessagesCount))
                                                    }
                                                }

                                                response.result.firstOrNull()?.takeIf { it.isNotSeen }?.let { activity.sendSeen(messageId = it.id) }

                                                if (previousSize == 0) {
                                                    activity.io_customerly__recycler_view.layoutManager?.scrollToPosition(0)
                                                }
                                            }

                                            if (response.result.size >= MESSAGES_PER_PAGE) {
                                                scrollListener.onFinishedUpdating()
                                            }
                                        }
                                        is ClyApiResponse.Failure -> {
                                            this.wActivity.get()?.let {
                                                it.io_customerly__progress_view?.visibility = View.GONE
                                                Toast.makeText(it.applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }).also {
                            it.p(key = "conversation_id", value = conversationId)
                            it.p(key = "per_page", value = MESSAGES_PER_PAGE)
                            it.p(key = "messages_before_id", value = activity.chatList.minBy { it.id }?.id
                                    ?: Long.MAX_VALUE)
                        }.start()
                    }
                }
            }
        }
    }
    private var progressiveScrollListener: RvProgressiveScrollListener? = null
    private var permissionRequestPendingFileName: String? = null
    private var permissionRequestPendingPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (this.onCreateLayout(R.layout.io_customerly__activity_chat)) {

            this.io_customerly__progress_view.indeterminateDrawable.setColorFilter(Customerly.lastPing.widgetColor, android.graphics.PorterDuff.Mode.MULTIPLY)
            this.io_customerly__recycler_view.also { recyclerView ->
                recyclerView.layoutManager = LinearLayoutManager(this.applicationContext).also { llm ->
                    llm.reverseLayout = true
                    RvProgressiveScrollListener(llm = llm, onBottomReached = this.onBottomReachedListener).also {
                        this.progressiveScrollListener = it
                        recyclerView.addOnScrollListener(it)
                    }
                }
                recyclerView.itemAnimator = DefaultItemAnimator()
                recyclerView.setHasFixedSize(true)
                recyclerView.adapter = ClyChatAdapter(chatActivity = this)
            }

            this.updateActivityTitleBar(accountInfos = false)

            val conversationId: Long = this.intent.getLongExtra(EXTRA_CONVERSATION_ID, 0)
            if(conversationId != 0L) {
                this.onConversationId(conversationId = conversationId)
            } else {
                //TODO handle no conversationId
                val messageContent:String? = this.intent.getStringExtra(EXTRA_MESSAGE_CONTENT)
                val messageAttachments:ArrayList<ClyAttachment>? = this.intent.getParcelableArrayListExtra(EXTRA_MESSAGE_ATTACHMENTS)

                if(messageContent != null) {
                    //TODO send message. if anonymous set as pending and add botform ask email.

                    /*
                    //TODO Replace deprecated ProgressDialog
                val progressDialog = ProgressDialog.show(this, this.getString(R.string.io_customerly__new_conversation), this.getString(R.string.io_customerly__sending_message), true, false)

                ClyApiRequest(
                        context = this,
                        endpoint = ENDPOINT_PING,
                        jsonObjectConverter = { Unit },
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
                     */
                }

                //TODO
                this.io_customerly__progress_view.visibility = View.GONE
                //TODO serve?  this.io_customerly__recycler_view.visibility = View.VISIBLE

                /* Privacy policy

                (Customerly.lastPing.privacyUrl?.let { privacyUrl ->
                this.io_customerly__input_privacy_policy.also { policy ->
                policy.setOnClickListener {
                it.activity?.startClyWebViewActivity(targetUrl = privacyUrl, showClearInsteadOfBack = true)
                }
                policy.text = spannedFromHtml(source = this.getString(R.string.io_customerly__i_accept_the_privacy_policy))
                }
                2.5f.dp2px.toInt() to View.VISIBLE
                } ?: 10.dp2px to View.GONE).let { (topMargin, visibility) ->
                (this.io_customerly__input_email_button.layoutParams as? LinearLayout.LayoutParams)?.topMargin = topMargin
                this.io_customerly__input_privacy_policy.visibility = visibility
                }

                 */

                /*
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
                 */
            }
        }
    }

    private fun onConversationId(conversationId: Long) {
        this.conversationId = conversationId
        val weakRv = this.io_customerly__recycler_view.weak()

        Customerly.clySocket.typingListener = { pConversationId, accountId, pTyping ->
            if((pTyping && typingAccountId != accountId)
                    ||
                    (!pTyping && typingAccountId != TYPING_NO_ONE)) {
                if (conversationId == pConversationId) {
                    weakRv.get()?.post {
                        when(pTyping) {
                            true -> when(this.typingAccountId) {
                                TYPING_NO_ONE -> {
                                    this.typingAccountId = accountId
                                    weakRv.get()?.also { recyclerView ->
                                        recyclerView.adapter?.notifyItemInserted(0)
                                        (recyclerView.layoutManager as? LinearLayoutManager)?.takeIf {
                                            it.findFirstCompletelyVisibleItemPosition() == 0
                                        }?.scrollToPosition(0)
                                    }
                                }
                                else -> {
                                    this.typingAccountId = accountId
                                    weakRv.get()?.adapter?.notifyItemChanged(0)
                                }
                            }
                            false -> {
                                this.typingAccountId = TYPING_NO_ONE
                                weakRv.get()?.adapter?.notifyItemRemoved(0)
                            }
                        }
                    }
                }
            }
        }

        this.inputInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.isEmpty()) {
                    Customerly.clySocket.sendStopTyping(conversationId = conversationId)
                } else {
                    Customerly.clySocket.sendStartTyping(conversationId = conversationId, previewText = s.toString())
                }
            }
        })

        val weakActivity = this.weak()
        this.io_customerly__recycler_view.postDelayed( {
            val now = Calendar.getInstance()

            val nearestOfficeHours = Customerly.lastPing.officeHours?.map { it to it.getNearestFactor(now = now) }?.minBy { (_,factor) -> factor }
            if(nearestOfficeHours == null || nearestOfficeHours.second == 0L) {
                Customerly.lastPing.replyTime?.stringResId?.also { replyTimeStringResId ->
                    weakActivity.reference { activity ->
                        if(activity.chatList.count { it.writer.isAccount } == 0) {
                            activity.addMessageAt0(message = ClyMessage.Bot(conversationId = conversationId, content = activity.getString(replyTimeStringResId)))
                        }
                    }
                }
            } else {
                weakActivity.reference { activity ->
                    if(activity.chatList.count { it.writer.isAccount } == 0) {
                        nearestOfficeHours.first.getBotMessage(context = activity, now = now)?.apply {
                            activity.addMessageAt0(message = ClyMessage.Bot(conversationId = conversationId, content = this))
                        }
                    }
                }
            }
        }, 3000)
        this.io_customerly__recycler_view.postDelayed( {
            weakActivity.get()?.tryLoadForm()
        }, 4000)
    }

    override fun onResume() {
        super.onResume()
        this.progressiveScrollListener?.let { this.onBottomReachedListener(it) }
    }

    override fun onDestroy() {
        Customerly.clySocket.typingListener = null
        this.conversationId?.apply { Customerly.clySocket.sendStopTyping(conversationId = this) }
        super.onDestroy()
    }

    override fun onReconnection() {
        this.progressiveScrollListener?.let { this.onBottomReachedListener(it) }
    }

    override fun onLogoutUser() {
        this.finish()
    }

    internal fun startAttachmentDownload(filename: String, fullPath: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startFileDownload(context = this, filename = filename, fullPath = fullPath)
        } else {
            this.permissionRequestPendingFileName = filename
            this.permissionRequestPendingPath = fullPath
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder(this)
                        .setTitle(R.string.io_customerly__permission_request)
                        .setMessage(R.string.io_customerly__permission_request_explanation_write)
                        .setPositiveButton(android.R.string.ok) { _, _ -> ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE) }
                        .show()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE -> {
                for (i in 0 until Math.min(grantResults.size, permissions.size)) {
                    if (permissions[i] == Manifest.permission.WRITE_EXTERNAL_STORAGE && grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        val pendingFileName = this.permissionRequestPendingFileName
                        val pendingPath = this.permissionRequestPendingPath
                        if(pendingFileName?.isNotEmpty() == true && pendingPath?.isNotEmpty() == true) {
                            this.startAttachmentDownload(filename = pendingFileName, fullPath = pendingPath)
                            this.permissionRequestPendingFileName = null
                            this.permissionRequestPendingPath = null
                        }
                        return
                    }
                }
                Toast.makeText(this.applicationContext, R.string.io_customerly__permission_denied_write, Toast.LENGTH_LONG).show()
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun sendSeen(messageId: Long) {
        val wContext = this.weak()
        ClySntpClient.getNtpTimeAsync(context = this) {

            val utc = (it ?: System.currentTimeMillis()).msAsSeconds

            Customerly.clySocket.sendSeen(messageId = messageId, seenTimestamp = utc)

            ClyApiRequest<Any>(
                    context = wContext.get(),
                    endpoint = ENDPOINT_MESSAGE_SEEN,
                    requireToken = true,
                    jsonObjectConverter = { it })
                    .p(key = "conversation_message_id", value = messageId)
                    .p(key = "seen_date", value = utc)
                    .start()
        }
    }

    internal fun startSendMessageRequest(message: ClyMessage) {
        val weakAct = this.weak()
        ClyApiRequest(
                context = this,
                endpoint = ENDPOINT_MESSAGE_SEND,
                requireToken = true,
                trials = 2,
                jsonObjectConverter = {
                    Customerly.clySocket.sendMessage(timestamp = it.optLong("timestamp", -1L))
                    it.optJSONObject("message")?.parseMessage()
                },
                callback = { response ->
                    weakAct.get()?.also { activity ->
                        val chatList = activity.chatList
                        when (response) {
                            is ClyApiResponse.Success -> {
                                chatList.indexOf(message).takeIf { it != -1 }?.also { chatList[it] = response.result }
                            }
                            is ClyApiResponse.Failure -> {
                                message.setStateFailed()
                                Toast.makeText(activity.applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
                            }
                        }
                        weakAct.get()?.notifyItemChangedInList(message = message)
                    }
                })
                .p(key = "conversation_id", value = message.conversationId)
                .p(key = "message", value = message.content)
                .p(key = "attachments", value = message.attachments.toJSON(context = this))
                .start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                this.finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @UiThread
    override fun onSendMessage(content: String, attachments: Array<ClyAttachment>) {
        Customerly.jwtToken?.userID?.let { userID ->
            this.conversationId?.also { conversationId ->
                ClyMessage.Real(writerUserid = userID, conversationId = conversationId, content = content, attachments = attachments).also { message ->
                    message.setStateSending()
                    this.addMessageAt0(message = message)
                    this.startSendMessageRequest(message = message)
                }
            }
        }
    }

    internal fun tryLoadForm() {
        if(this.chatList.asSequence().none { it is ClyMessage.BotProfilingForm && !it.form.answerConfirmed}) {
            Customerly.lastPing.nextFormDetails?.also { form ->
                this.conversationId?.also { conversationId ->
                    this.addMessageAt0(message = ClyMessage.BotProfilingForm(conversationId = conversationId, form = form))
                }
            }
        }
    }

    private fun addMessageAt0(message: ClyMessage) {
        this.chatList.add(0, message)
        this.io_customerly__recycler_view?.let {
            it.adapter?.notifyItemInserted(
                    if (this.typingAccountId == TYPING_NO_ONE) {
                        0
                    } else {
                        1
                    })
            (it.layoutManager as? LinearLayoutManager)?.takeIf { it.findFirstCompletelyVisibleItemPosition() == 0 }?.scrollToPosition(0)
        }
    }

    @UiThread
    override fun onNewSocketMessages(messages: ArrayList<ClyMessage>) {
        messages
                .asSequence()
                .lastOrNull { it.conversationId != this.conversationId}
                ?.let { otherConversationMessage ->
                    try {
                        this.showClyAlertMessage(message = otherConversationMessage)
                    } catch (exception : WindowManager.BadTokenException) { }
                }

        val newChatList = ArrayList(this.chatList)
        messages
                .asSequence()
                .filter { it.conversationId == this.conversationId && !newChatList.contains(it) }
                .forEach { newChatList.add(it) }

        newChatList.sortByDescending { it.id }//Sorting by conversation_message_id DESC

        this.setNewChatList(newChatList = newChatList)

        this.io_customerly__recycler_view?.let {
            val wRv = it.weak()
            it.post {
                wRv.get()?.let {
                    it.adapter?.notifyDataSetChanged()
                    (it.layoutManager as? LinearLayoutManager)?.takeIf { it.findFirstCompletelyVisibleItemPosition() == 0 }?.scrollToPosition(0)
                }
            }
        }

        newChatList.firstOrNull()?.takeIf { it.isNotSeen }?.also { this.sendSeen(messageId = it.id) }
    }

    /**
     * Returns true if the account card need a redraw
     */
    private fun setNewChatList(newChatList: ArrayList<ClyMessage>) {
        this.chatList = newChatList
        if(this.conversationFullAdmin == null) {
            newChatList.firstOrNull { it.writer.isAccount }?.writer?.id?.let { lastAccountId ->

                Customerly.getAccountDetails { adminsFullDetails ->
                    val notifyRedrawAccountCard: Boolean = adminsFullDetails?.firstOrNull { it.accountId == lastAccountId }?.let {
                        this.conversationFullAdmin = it
                        true
                    } ?: false

                    this.updateActivityTitleBar(accountInfos = true)

                    if (notifyRedrawAccountCard) {
                        (this.io_customerly__recycler_view.adapter as? ClyChatAdapter)?.also { adapter ->
                            adapter.notifyItemChanged(adapter.itemCount - 1)
                        }
                    }

                }
            }
        }
    }

    private fun updateActivityTitleBar(accountInfos: Boolean) {
        this.io_customerly__actionlayout.setBackgroundColor(Customerly.lastPing.widgetColor)
        @ColorInt val titleTextColorInt = Customerly.lastPing.widgetColor.getContrastBW()
        if(accountInfos) {
            (
                    this.conversationFullAdmin?.also { clyAdminFull ->
                        clyAdminFull.jobTitle?.also { jobTitle ->
                            this.io_customerly__job.apply {
                                this.text = jobTitle
                                this.setTextColor(titleTextColorInt)
                                this.visibility = View.VISIBLE
                            }
                        }
                        this.io_customerly__title.setOnClickListener {
                            (it.activity as? ClyChatActivity)?.also { chatActivity ->
                                chatActivity.io_customerly__recycler_view?.layoutManager?.also { llm ->
                                    this.progressiveScrollListener?.skipNextBottom()
                                    llm.scrollToPosition(llm.itemCount - 1)
                                }
                            }
                        }
                    } ?: Customerly.lastPing.activeAdmins?.asSequence()?.maxBy { it.lastActive }
                    )?.also { admin ->
                this.io_customerly__icon.visibility = View.VISIBLE
                ClyImageRequest(context = this, url = admin.getImageUrl(sizePx = 48.dp2px))
                        .fitCenter()
                        .transformCircle()
                        .resize(width = 48.dp2px)
                        .placeholder(placeholder = R.drawable.io_customerly__ic_default_admin)
                        .into(imageView = this.io_customerly__icon)
                        .start()
                this.io_customerly__name.apply {
                    this.text = admin.name
                    this.setTextColor(titleTextColorInt)
                }
            }
        } else {
            this.io_customerly__icon.visibility = View.GONE
            this.io_customerly__name.apply {
                this.setText(R.string.io_customerly__activity_title_chat)
                this.setTextColor(titleTextColorInt)
            }
        }
    }

    fun notifyItemChangedInList(message: ClyMessage) {
        this.chatList.indexOf(message).takeIf { it != -1 }?.also { listIndex ->
            (this.io_customerly__recycler_view.adapter as? ClyChatAdapter)?.also { adapter ->
                adapter.notifyItemChanged(adapter.listIndex2position(listIndex))
            }
        }
    }

}
