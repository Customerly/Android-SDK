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
import io.customerly.entity.iamLead
import io.customerly.entity.ping.ClyNextOfficeHours
import io.customerly.sxdependencies.*
import io.customerly.sxdependencies.annotations.SXColorInt
import io.customerly.sxdependencies.annotations.SXStringRes
import io.customerly.sxdependencies.annotations.SXUiThread
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.download.startFileDownload
import io.customerly.utils.getContrastBW
import io.customerly.utils.ggkext.*
import io.customerly.utils.network.ClySntpClient
import io.customerly.utils.ui.RvProgressiveScrollListener
import kotlinx.android.synthetic.main.io_customerly__activity_chat.*
import java.lang.ref.WeakReference
import java.util.*
import kotlin.math.min

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
    if (conversationId != CONVERSATIONID_UNKNOWN_FOR_MESSAGE) {
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

    private var conversationId: Long = CONVERSATIONID_UNKNOWN_FOR_MESSAGE
    internal var typingAccountId = TYPING_NO_ONE
    internal var typingAccountName: String? = null
    internal var chatList = ArrayList<ClyMessage>(0)
    internal var conversationFullAdmin: ClyAdminFull? = null
    private var conversationFullAdminFromMessagesAccountId: Long? = null
    private val onBottomReachedListener = object : Function1<RvProgressiveScrollListener,Unit> {
        private val wActivity : WeakReference<ClyChatActivity> = this@ClyChatActivity.weak()
        override fun invoke(scrollListener: RvProgressiveScrollListener) {
            this.wActivity.get()?.let { activity ->
                Customerly.checkConfigured {
                    activity.conversationId.takeIf { it != CONVERSATIONID_UNKNOWN_FOR_MESSAGE }?.also { conversationId ->
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

                                                if (addedMessagesCount > 0) {
                                                    activity.chatList = newChatList
                                                    (activity.io_customerly__recycler_view.layoutManager as? SXLinearLayoutManager)?.let { llm ->
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
                                                    activity.updateAccountInfos()
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
                            it.p(key = "messages_before_id", value = activity.chatList.mapNotNull { msg -> if(msg.id > 0) msg.id else null }.min() ?: Long.MAX_VALUE)
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
            this.io_customerly__actionlayout.setBackgroundColor(Customerly.lastPing.widgetColor)
            this.io_customerly__progress_view.indeterminateDrawable.setColorFilterMultiply(color = Customerly.lastPing.widgetColor)
            this.io_customerly__recycler_view.also { recyclerView ->
                recyclerView.layoutManager = SXLinearLayoutManager(this.applicationContext).also { llm ->
                    llm.reverseLayout = true
                    RvProgressiveScrollListener(llm = llm, onBottomReached = this.onBottomReachedListener).also {
                        this.progressiveScrollListener = it
                        recyclerView.addOnScrollListener(it)
                    }
                }
                recyclerView.itemAnimator = SXDefaultItemAnimator()
                recyclerView.setHasFixedSize(true)
                recyclerView.adapter = ClyChatAdapter(chatActivity = this)
            }
            val conversationId: Long = this.intent.getLongExtra(EXTRA_CONVERSATION_ID, CONVERSATIONID_UNKNOWN_FOR_MESSAGE)
            if(conversationId != CONVERSATIONID_UNKNOWN_FOR_MESSAGE) {
                this.onConversationId(conversationId = conversationId)
                this.updateAccountInfos(fallbackUserOnLastActive = false)
            } else {
                this.io_customerly__progress_view.visibility = View.GONE
                val messageContent:String? = this.intent.getStringExtra(EXTRA_MESSAGE_CONTENT)
                if(messageContent != null) {
                    this.onSendMessage(
                            content = messageContent,
                            attachments = this.intent.getParcelableArrayListExtra<ClyAttachment>(EXTRA_MESSAGE_ATTACHMENTS)?.toTypedArray()
                                    ?: emptyArray())
                }

                this.updateAccountInfos(fallbackUserOnLastActive = false)
            }
        }
    }

    private fun onConversationId(conversationId: Long) {
        this.conversationId = conversationId
        this.inputLayout?.visibility = View.VISIBLE
        val weakRv = this.io_customerly__recycler_view.weak()

        Customerly.clySocket.typingListener = { pConversationId, accountId, accountName, pTyping ->
            if((pTyping && typingAccountId != accountId)
                    ||
                    (!pTyping && typingAccountId != TYPING_NO_ONE)) {
                if (conversationId == pConversationId) {
                    weakRv.get()?.post {
                        when(pTyping) {
                            true -> when(this.typingAccountId) {
                                TYPING_NO_ONE -> {
                                    this.typingAccountId = accountId
                                    this.typingAccountName = accountName
                                    weakRv.get()?.also { recyclerView ->
                                        recyclerView.adapter?.notifyItemInserted(0)
                                        (recyclerView.layoutManager as? SXLinearLayoutManager)?.takeIf {
                                            it.findFirstCompletelyVisibleItemPosition() == 0
                                        }?.scrollToPosition(0)
                                    }
                                }
                                else -> {
                                    this.typingAccountId = accountId
                                    this.typingAccountName = accountName
                                    weakRv.get()?.adapter?.notifyItemChanged(0)
                                }
                            }
                            false -> {
                                this.typingAccountId = TYPING_NO_ONE
                                this.typingAccountName = null
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
            weakActivity.reference { activity ->
                if (activity.chatList.count { it.writer.isAccount } == 0) {
                    var botMessageContent: String? = null

                    val nextOfficeHours: ClyNextOfficeHours? = Customerly.lastPing.nextOfficeHours
                    if(nextOfficeHours?.isOfficeOpen() == false) {
                        botMessageContent = nextOfficeHours.getBotMessage(context = activity)
                    }

                    @SXStringRes val replyTimeStringResId: Int? = Customerly.lastPing.replyTime?.stringResId
                    if(botMessageContent == null && replyTimeStringResId != null && replyTimeStringResId > 0) {
                        botMessageContent = activity.getString(replyTimeStringResId)
                    }

                    if(botMessageContent != null) {
                        activity.addMessageAt0(message = ClyMessage.Bot.Text(
                                messageId = activity.chatList.optAt(0)?.id
                                        ?: -System.currentTimeMillis(),
                                conversationId = conversationId,
                                content = botMessageContent))
                    }
                }
            }
        }, 3000)
        this.io_customerly__recycler_view.postDelayed( {
            weakActivity.reference { activity ->

                if(Customerly.currentUser.email == null) {

                    activity.addMessageAt0(message = ClyMessage.Bot.Text(
                            messageId = activity.chatList.optAt(0)?.id
                                    ?: -System.currentTimeMillis(),
                            conversationId = conversationId,
                            content = activity.getString(R.string.io_customerly__give_us_a_way_to_reach_you)))

                    activity.io_customerly__recycler_view.postDelayed( {
                        weakActivity.get()?.addMessageAt0(message = ClyMessage.Bot.Form.AskEmail(conversationId = conversationId, messageId = 1))
                    }, 1000)

                } else if (activity.chatList.count { it.writer.isAccount } == 0) {
                    activity.tryLoadForm()
                }
                null
            }
        }, 4000)
    }

    override fun onResume() {
        super.onResume()
        this.progressiveScrollListener?.let { this.onBottomReachedListener(it) }
    }

    override fun onDestroy() {
        Customerly.clySocket.typingListener = null
        this.conversationId.takeIf { it != CONVERSATIONID_UNKNOWN_FOR_MESSAGE }?.apply { Customerly.clySocket.sendStopTyping(conversationId = this) }
        super.onDestroy()
    }

    override fun onReconnection() {
        this.progressiveScrollListener?.let { this.onBottomReachedListener(it) }
    }

    override fun onLogoutUser() {
        this.finish()
    }

    internal fun startAttachmentDownload(filename: String, fullPath: String) {
        if (SXContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            startFileDownload(context = this, filename = filename, fullPath = fullPath)
        } else {
            this.permissionRequestPendingFileName = filename
            this.permissionRequestPendingPath = fullPath
            if (SXActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                SXAlertDialogBuilder(this)
                        .setTitle(R.string.io_customerly__permission_request)
                        .setMessage(R.string.io_customerly__permission_request_explanation_write)
                        .setPositiveButton(android.R.string.ok) { _, _ -> SXActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE) }
                        .show()
            } else {
                SXActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSION_REQUEST__WRITE_EXTERNAL_STORAGE -> {
                for (i in 0 until min(grantResults.size, permissions.size)) {
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
        ClySntpClient.getNtpTimeAsync(context = this) { time ->

            val utc = (time ?: System.currentTimeMillis()).msAsSeconds

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
                jsonObjectConverter = { response ->
                    Customerly.clySocket.sendMessage(timestamp = response.optLong("timestamp", -1L))
                    Customerly.currentUser.leadHash = response.optString("lead_hash").takeIf { it != "" }
                    response.optJSONObject("message")?.parseMessage()
                },
                callback = { response ->
                    weakAct.get()?.also { activity ->
                        val chatList = activity.chatList
                        when (response) {
                            is ClyApiResponse.Success -> {
                                chatList.indexOf(message).takeIf { it != -1 }?.also {
                                    chatList[it] = response.result
                                    activity.notifyItemChangedInList(message = response.result)
                                    if(activity.conversationId == CONVERSATIONID_UNKNOWN_FOR_MESSAGE) {
                                        activity.onConversationId(conversationId = response.result.conversationId)
                                    }
                                }
                            }
                            is ClyApiResponse.Failure -> {
                                message.setStateFailed()
                                activity.notifyItemChangedInList(message = message)
                                Toast.makeText(activity.applicationContext, R.string.io_customerly__connection_error, Toast.LENGTH_SHORT).show()
                            }
                        }
                        activity.updateAccountInfos(true)
                    }
                }).apply {
                    if(message.conversationId != CONVERSATIONID_UNKNOWN_FOR_MESSAGE) {
                        this.p(key = "conversation_id", value = message.conversationId)
                    }
                }
                .p(key = "message", value = message.content)
                .p(key = "attachments", value = message.attachments.toJSON(context = this))
                .p(key = "lead_hash", value = Customerly.currentUser.leadHash)
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

    @SXUiThread
    override fun onSendMessage(content: String, attachments: Array<ClyAttachment>) {
        val userId = Customerly.jwtToken?.userID
        val conversationId = this.conversationId
        when {
            userId != null -> ClyMessage.Human.UserLocal(userId = userId, conversationId = conversationId, content = content, attachments = attachments).also { message ->
                this.addMessageAt0(message = message)
                this.startSendMessageRequest(message = message)
            }
            Customerly.lastPing.allowAnonymousChat -> {
                val pendingMessage = ClyMessage.Human.UserLocal(conversationId = conversationId, content = content, attachments = attachments)
                this.addMessageAt0(message = pendingMessage)
                val weakActivity = this.weak()
                ClyApiRequest(
                        context = this.applicationContext,
                        endpoint = ENDPOINT_PING,
                        jsonObjectConverter = { Unit },
                        callback = { response ->
                            Customerly.jwtToken?.userID?.apply { pendingMessage.writer.id = this }
                            weakActivity.reference { clyChatActivity ->
                                when (response) {
                                    is ClyApiResponse.Success -> {
                                        pendingMessage.setStateSending()
                                        clyChatActivity.notifyItemChangedInList(message = pendingMessage)
                                        clyChatActivity.startSendMessageRequest(message = pendingMessage)
                                    }
                                    is ClyApiResponse.Failure -> {
                                        pendingMessage.setStateFailed()
                                        clyChatActivity.notifyItemChangedInList(message = pendingMessage)
                                    }
                                }
                            }
                        })
                        .p(key = "force_lead", value = true)
                        .start()
            }
            else -> {
                this.inputLayout?.visibility = View.GONE
                ClyMessage.Human.UserLocal(conversationId = conversationId, content = content, attachments = attachments).also { pendingMessage ->
                    this.addMessageAt0(message = pendingMessage)
                    this.addMessageAt0(message = ClyMessage.Bot.Form.AskEmail(conversationId = pendingMessage.conversationId, messageId = 1, pendingMessage = pendingMessage))
                }
            }
        }
    }

    internal fun tryLoadForm() {
        if(Customerly.iamLead()
                && this.chatList.asSequence().none {
                    when (it) {
                        is ClyMessage.Bot.Form.Profiling -> !it.form.answerConfirmed
                        is ClyMessage.Bot.Form.AskEmail -> Customerly.currentUser.email == null
                        else -> false
                    }
                }) {
            Customerly.lastPing.nextFormDetails?.also { form ->
                this.conversationId.takeIf { it != CONVERSATIONID_UNKNOWN_FOR_MESSAGE }?.also { conversationId ->
                    this.addMessageAt0(message = ClyMessage.Bot.Form.Profiling(
                            messageId = this.chatList.optAt(0)?.id ?: -System.currentTimeMillis(),
                            conversationId = conversationId,
                            form = form))
                }
            }
        }
    }

    private fun addMessageAt0(message: ClyMessage) {
        this.chatList.add(0, message)
        this.io_customerly__recycler_view?.let {
            (it.adapter as? ClyChatAdapter)?.apply {
                this.notifyItemInserted(this.listIndex2position(listIndex = 0))
            }
            (it.layoutManager as? SXLinearLayoutManager)?.takeIf { llm -> llm.findFirstCompletelyVisibleItemPosition() == 0 }?.scrollToPosition(0)
        }
    }

    @SXUiThread
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

        this.chatList = newChatList

        this.io_customerly__recycler_view?.let {
            val wRv = it.weak()
            it.post {
                wRv.get()?.let { rv ->
                    rv.adapter?.notifyDataSetChanged()
                    (rv.layoutManager as? SXLinearLayoutManager)?.takeIf { llm -> llm.findFirstCompletelyVisibleItemPosition() == 0 }?.scrollToPosition(0)
                }
            }
        }
        this.updateAccountInfos()

        newChatList.firstOrNull()?.takeIf { it.isNotSeen }?.also { this.sendSeen(messageId = it.id) }
    }

    private fun updateAccountInfos(fallbackUserOnLastActive: Boolean = true) {
        val lastAccountId = this.chatList.firstOrNull { it.writer.isAccount }?.writer?.id

        if(this.conversationFullAdmin == null || this.conversationFullAdminFromMessagesAccountId != lastAccountId) {
            Customerly.getAccountDetails { adminsFullDetails ->

                val fullAdmin = when {
                    lastAccountId != null -> adminsFullDetails?.firstOrNull { it.accountId == lastAccountId }
                    fallbackUserOnLastActive -> adminsFullDetails?.maxBy { it.lastActive }
                    else -> null
                }

                @SXColorInt val titleTextColorInt = Customerly.lastPing.widgetColor.getContrastBW()
                if(fullAdmin != null) {
                    this.conversationFullAdmin = fullAdmin
                    this.conversationFullAdminFromMessagesAccountId = lastAccountId

                    fullAdmin.jobTitle?.also { jobTitle ->
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

                    this.io_customerly__icon.visibility = View.VISIBLE
                    ClyImageRequest(context = this, url = fullAdmin.getImageUrl(sizePx = 48.dp2px))
                            .fitCenter()
                            .transformCircle()
                            .resize(width = 48.dp2px)
                            .placeholder(placeholder = R.drawable.io_customerly__ic_default_admin)
                            .into(imageView = this.io_customerly__icon)
                            .start()
                    this.io_customerly__name.apply {
                        this.text = fullAdmin.name
                        this.setTextColor(titleTextColorInt)
                    }

                    this.io_customerly__recycler_view.apply {
                        (this.adapter as? ClyChatAdapter)?.also { adapter ->
                            val llm = this.layoutManager as? SXLinearLayoutManager
                            this.post {
                                val scrollTo0 = llm?.findFirstCompletelyVisibleItemPosition() == 0

                                adapter.notifyAccountCardChanged()

                                if (scrollTo0) {
                                    llm?.scrollToPosition(0)
                                }
                            }
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
