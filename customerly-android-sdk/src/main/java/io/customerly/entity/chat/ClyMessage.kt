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

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Handler
import android.os.Looper
import android.support.annotation.IntDef
import android.support.annotation.UiThread
import android.text.Spanned
import android.text.SpannedString
import android.view.WindowManager
import android.widget.TextView
import io.customerly.Customerly
import io.customerly.activity.ClyAppCompatActivity
import io.customerly.alert.showClyAlertMessage
import io.customerly.entity.ERROR_CODE__GENERIC
import io.customerly.entity.clySendError
import io.customerly.entity.ping.ClyFormDetails
import io.customerly.utils.ClyActivityLifecycleCallback
import io.customerly.utils.ggkext.*
import io.customerly.utils.htmlformatter.fromHtml
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by Gianni on 11/09/16.
 * Project: Customerly Android SDK
 */

@SuppressLint("ConstantLocale")
private val TIME_FORMATTER = SimpleDateFormat("HH:mm", Locale.getDefault())
private val DATE_FORMATTER = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG)

private const val CSTATE_COMPLETED = 1
private const val CSTATE_SENDING = 0
private const val CSTATE_FAILED = -1

@IntDef(CSTATE_COMPLETED, CSTATE_FAILED, CSTATE_SENDING)
@Retention(AnnotationRetention.SOURCE)
private annotation class CState

@Throws(JSONException::class)
internal fun JSONObject.parseMessage() : ClyMessage {
    return ClyMessage.Real(
            writerUserid = this.optTyped(name = "user_id", fallback = 0L),
            writerAccountId = this.optTyped(name = "account_id", fallback = 0L),
            writerAccountName = this.optTyped<JSONObject>(name = "account")?.optTyped(name = "name"),
            id = this.optTyped(name = "conversation_message_id", fallback = 0L),
            conversationId = this.optTyped(name = "conversation_id", fallback = 0L),
            content = this.optTyped(name = "content", fallback = ""),
            attachments = this.optSequenceOpt<JSONObject>(name = "attachments")
                    ?.map { it?.nullOnException { it.parseAttachment() } }
                    ?.requireNoNulls()
                    ?.toList()?.toTypedArray() ?: emptyArray(),
            contentAbstract = fromHtml(this.optTyped(name = "abstract", fallback = "")),
            sentDatetime = this.optTyped(name = "sent_date", fallback = 0L),
            seenDate = this.optTyped(name = "seen_date", fallback = 0L),
            richMailLink = if (this.optTyped(name = "rich_mail", fallback = 0) == 0) {
                null
            } else {
                this.optTyped<String>(name = "rich_mail_link")
            },
            cState = CSTATE_COMPLETED)
}

internal fun JSONObject.parseMessagesList() : ArrayList<ClyMessage> {
    return this.optArrayList<JSONObject, ClyMessage>(name = "messages", map = { it.parseMessage() }) ?: ArrayList(0)
}

internal sealed class ClyMessage(
        internal val writer : ClyWriter,
        internal val id : Long,
        internal val conversationId : Long,
        internal val content : String,
        internal val attachments : Array<ClyAttachment> = emptyArray(),
        internal val contentAbstract : Spanned = when {
            content.isNotEmpty() -> fromHtml(message = content)
            attachments.isNotEmpty() -> SpannedString("[Attachment]")
            else -> SpannedString("")
        },
        @STimestamp internal val sentDatetime : Long = System.currentTimeMillis().msAsSeconds,
        @STimestamp private val seenDate : Long = sentDatetime,
        internal val richMailLink : String? = null,
        @CState private var cState : Int = CSTATE_SENDING) {

    internal open class Bot(
            conversationId: Long,
            content: String
    ): ClyMessage(
            writer = ClyWriter.Bot,
            content = content,
            id = - Math.abs(content.hashCode()).toLong(),
            conversationId = conversationId,
            cState = CSTATE_COMPLETED
    )

    internal class BotProfilingForm(
            conversationId: Long,
            internal val form: ClyFormDetails
    ): Bot(
            content = form.label.takeIf { it.isNotEmpty() } ?: form.hint ?: "",
            conversationId = conversationId
    )

    internal class Real(
            writerUserid : Long = 0,
            writerAccountId : Long = 0,
            writerAccountName : String? = null,
            id : Long = 0,
            conversationId : Long,
            content : String,
            attachments : Array<ClyAttachment>,
            contentAbstract : Spanned = when {
                content.isNotEmpty() -> fromHtml(message = content)
                attachments.isNotEmpty() -> SpannedString("[Attachment]")
                else -> SpannedString("")
            },
            @STimestamp sentDatetime : Long = System.currentTimeMillis().msAsSeconds,
            @STimestamp seenDate : Long = sentDatetime,
            richMailLink : String? = null,
            @CState cState : Int = CSTATE_SENDING
    ): ClyMessage(
            writer = ClyWriter.Real.from(userId = writerUserid, accountId = writerAccountId, name = writerAccountName),
            id = id,
            conversationId = conversationId,
            content = content,
            attachments = attachments,
            contentAbstract = contentAbstract,
            sentDatetime = sentDatetime,
            seenDate = seenDate,
            richMailLink = richMailLink,
            cState = cState
    )

    internal val dateString: String = DATE_FORMATTER.format(Date(this.sentDatetime.secondsAsMs))
    internal val timeString: String = TIME_FORMATTER.format(Date(this.sentDatetime.secondsAsMs))

    private var contentSpanned : Spanned? = null

    internal val isNotSeen: Boolean
        get() = this.writer.isAccount && this.seenDate == 0L

    internal val isStateSending: Boolean
        get() = this.cState == CSTATE_SENDING
    internal val isStateFailed: Boolean
        get() = this.cState == CSTATE_FAILED
    internal fun setStateSending() {
        this.cState = CSTATE_SENDING
    }
    internal fun setStateFailed() {
        this.cState = CSTATE_FAILED
    }

    internal fun isSentSameDay(of : ClyMessage) : Boolean
            = this.sentDatetime / (/*1000**/60 * 60 * 24) == of.sentDatetime / (/*1000**/60 * 60 * 24)

    internal fun getContentSpanned(tv: TextView, pImageClickableSpan : (Activity, String)->Unit): Spanned {
        var spanned = this.contentSpanned
        if(spanned == null) {
            spanned = fromHtml(message = this.content, tv = tv, pImageClickableSpan = pImageClickableSpan)
            this.contentSpanned = spanned
        }
        return spanned
    }

    fun toConversation(): ClyConversation {
        return ClyConversation(id = this.conversationId, lastMessage = this.toConvLastMessage())
    }

    fun toConvLastMessage() : ClyConvLastMessage {
        return ClyConvLastMessage(message = this.contentAbstract, date = this.sentDatetime, writer = this.writer)
    }

    override fun equals(other: Any?): Boolean {
        return this === other || (this.javaClass == other?.javaClass && this.id == (other as ClyMessage).id)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    fun postDisplay() {
        Handler(Looper.getMainLooper()).post {
            val currentActivity: Activity? = ClyActivityLifecycleCallback.getLastDisplayedActivity()
            if (currentActivity != null && Customerly.isEnabledActivity(activity = currentActivity)) {
                this.displayNow(activity = currentActivity)
            } else {
                Customerly.postOnActivity = { activity ->
                    this.displayNow(activity = activity)
                    true
                }
            }
        }
    }

    @UiThread
    private fun displayNow(activity: Activity, retryOnFailure: Boolean = true) {
        try {
            when (activity) {
                is ClyAppCompatActivity -> activity.onNewSocketMessages(messages = arrayListOf(this))
                else -> {
                    try {
                        activity.showClyAlertMessage(message = this)
                        Customerly.log(message = "Last message alert successfully displayed")
                    } catch (changedActivityWhileExecuting: WindowManager.BadTokenException) {
                        if(retryOnFailure) {
                            ClyActivityLifecycleCallback.getLastDisplayedActivity()
                                    ?.takeIf { Customerly.isEnabledActivity(activity = it) }
                                    ?.let {
                                        this.displayNow(activity = it, retryOnFailure = false)
                                    }
                        }
                    }
                }
            }
        } catch (exception: Exception) {
            Customerly.log(message = "A generic error occurred Customerly while displaying a last message alert")
            clySendError(errorCode = ERROR_CODE__GENERIC, description = "Generic error in Customerly while displaying a last message alert", throwable = exception)
        }
    }
}
