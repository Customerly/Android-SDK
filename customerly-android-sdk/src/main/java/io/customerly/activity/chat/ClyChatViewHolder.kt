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

import android.app.DatePickerDialog
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.support.annotation.IntRange
import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.util.Base64
import android.util.Patterns
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.customerly.Customerly
import io.customerly.R
import io.customerly.activity.fullscreen.startClyFullScreenImageActivity
import io.customerly.activity.startClyWebViewActivity
import io.customerly.api.ClyApiRequest
import io.customerly.api.ClyApiResponse
import io.customerly.api.ENDPOINT_FORM_ATTRIBUTE
import io.customerly.api.ENDPOINT_PING
import io.customerly.entity.chat.ClyMessage
import io.customerly.entity.iamAnonymous
import io.customerly.entity.iamUser
import io.customerly.entity.ping.ClyFormCast
import io.customerly.entity.ping.ClyFormDetails
import io.customerly.entity.urlImageAccount
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.ggkext.*
import io.customerly.utils.shortDateFomatter
import kotlinx.android.synthetic.main.io_customerly__li_bubble_accountinfos.view.*
import java.text.DecimalFormat
import java.util.*


/**
 * Created by Gianni on 24/04/18.
 * Project: Customerly-KAndroid-SDK
 */
@MSTimestamp private const val TYPING_DOTS_SPEED = 500L

internal sealed class ClyChatViewHolder (
        recyclerView: RecyclerView,
        @LayoutRes layoutRes: Int,
        @IntRange(from = 1, to = Long.MAX_VALUE) iconResId: Int = R.id.io_customerly__icon)
    : RecyclerView.ViewHolder(recyclerView.activity!!.inflater().inflate(layoutRes, recyclerView, false)) {

    val icon: ImageView? = this.itemView.findViewById(iconResId)

    internal open fun onViewRecycled() {}

    internal sealed class Bubble(recyclerView: RecyclerView, @LayoutRes layoutRes: Int,
                                 @IntRange(from = 1, to = Long.MAX_VALUE) contentResId: Int = R.id.io_customerly__content
    ) : ClyChatViewHolder(recyclerView = recyclerView, layoutRes = layoutRes) {

        val content: TextView = this.itemView.findViewById<TextView>(contentResId).apply { this.movementMethod = LinkMovementMethod.getInstance() }

        protected val iconSize = recyclerView.resources.getDimensionPixelSize(R.dimen.io_customerly__chat_li_icon_size).also { iconSize ->
            this.icon?.layoutParams?.also {
                it.height = iconSize
                it.width = iconSize
            }
        }

        abstract fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean)

        internal class Typing(recyclerView: RecyclerView): ClyChatViewHolder.Bubble(recyclerView = recyclerView, layoutRes = R.layout.io_customerly__li_bubble_account_typing) {
            init {
                val weakContentTv = this.content.weak()
                object : Runnable {
                    override fun run() {
                        weakContentTv.get()?.also { tv ->
                            tv.text = when (tv.text.toString()) {
                                ".    " -> ". .  "
                                ". .  " -> ". . ."
                                else /* "" or ". . ." */ -> ".    "
                            }
                            tv.postInvalidate()
                            tv.postDelayed(this, TYPING_DOTS_SPEED)
                        }
                    }
                }.run()
            }

            override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                val typingAccountID = chatActivity.typingAccountId
                this.content.text = ".    "
                this.icon?.also { icon ->
                    icon.visibility = if (isFirstMessageOfSender && typingAccountID != TYPING_NO_ONE) {
                        ClyImageRequest(context = chatActivity, url = urlImageAccount(accountId = typingAccountID, sizePX = this.iconSize, name = chatActivity.typingAccountName))
                                .fitCenter()
                                .transformCircle()
                                .resize(width = this.iconSize)
                                .placeholder(placeholder = R.drawable.io_customerly__ic_default_admin)
                                .into(imageView = icon)
                                .start()
                        View.VISIBLE
                    } else {
                        View.INVISIBLE
                    }
                }
            }
        }

        internal sealed class Message(
                recyclerView: RecyclerView,
                @LayoutRes layoutRes: Int,
                val sendingProgressBarResId: Int = 0,
                val pendingMessageResId: Int = 0,
                @IntRange(from = 1, to = Long.MAX_VALUE) attachmentLayoutResId: Int = R.id.io_customerly__attachment_layout,
                @IntRange(from = 1, to = Long.MAX_VALUE) dateResId: Int = R.id.io_customerly__date,
                @IntRange(from = 1, to = Long.MAX_VALUE) timeResId: Int = R.id.io_customerly__time,
                @IntRange(from = 1, to = Long.MAX_VALUE) val iconAttachment: Int
        ) : ClyChatViewHolder.Bubble(recyclerView = recyclerView, layoutRes = layoutRes) {

            private val attachmentLayout: LinearLayout? = this.itemView.findViewById(attachmentLayoutResId)
            private var sendingProgressBar: View? = null
            private var pendingMessageLabel: View? = null
            private val date: TextView = this.itemView.findViewById(dateResId)
            private val time: TextView? = this.itemView.findViewById(timeResId)

            private fun progressBarVisibility(show: Boolean) {
                when(show) {
                    true -> {
                        if(this.sendingProgressBar == null) {
                            this.sendingProgressBar = this.itemView.findViewById(this.sendingProgressBarResId)
                        }
                        this.sendingProgressBar?.visibility = View.VISIBLE
                    }
                    false -> {
                        this.sendingProgressBar?.visibility = View.GONE
                    }
                }
            }

            private fun pendingMessageLabelVisibility(show: Boolean) {
                when(show) {
                    true -> {
                        if(this.pendingMessageLabel == null) {
                            this.pendingMessageLabel = this.itemView.findViewById(this.pendingMessageResId)
                        }
                        this.pendingMessageLabel?.visibility = View.VISIBLE
                    }
                    false -> {
                        this.pendingMessageLabel?.visibility = View.GONE
                    }
                }
            }

            override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                if(message == null) {
                    //Always != null for this ViewHolder
                    this.icon?.visibility = View.INVISIBLE
                    this.content.apply {
                        this.text = null
                        this.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        this.setOnClickListener(null)
                    }
                    this.progressBarVisibility(show = false)
                    this.pendingMessageLabelVisibility(show = false)
                    this.itemView.setOnClickListener(null)
                    this.attachmentLayout?.apply {
                        this.removeAllViews()
                        this.visibility = View.GONE
                    }
                    return
                }
                //assert(message != null)
                this.date.visibility = dateToDisplay?.let {
                    this.date.text = it
                    View.VISIBLE
                } ?: View.GONE

                this.time?.apply {
                    if(message.writer.isBot) {
                        this.visibility = View.GONE
                    } else {
                        this.text = message.timeString
                        this.visibility = View.VISIBLE
                    }
                }

                this.icon?.also { icon ->
                    icon.visibility = if (isFirstMessageOfSender) {
                        message.writer.loadUrl(into = icon, sizePx = this.iconSize)
                        View.VISIBLE
                    } else {
                        View.INVISIBLE
                    }
                }

                if (message.content.isNotEmpty()) {
                    this.content.text = message.getContentSpanned(tv = this.content, pImageClickableSpan = { activity, imageUrl -> activity.startClyFullScreenImageActivity(imageUrl = imageUrl) })
                    this.content.visibility = View.VISIBLE
                } else {
                    this.onEmptyContent()
                }

                if(message.isStateFailed) {
                    this.content.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.io_customerly__ic_error, 0)
                    this.content.visibility = View.VISIBLE

                    ({ v : View ->
                        Customerly.checkConfigured {
                            message.setStateSending()
                            (v.activity as? ClyChatActivity)?.let {
                                it.notifyItemChangedInList(message = message)
                                it.startSendMessageRequest(message = message)
                            }
                        }
                    }).also {
                        this.content.setOnClickListener(it)
                        this.itemView.setOnClickListener(it)
                    }
                } else {
                    this.content.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    this.content.setOnClickListener(null)
                    this.itemView.setOnClickListener(null)
                }

                this.progressBarVisibility(show = message.isStateSending)
                this.pendingMessageLabelVisibility(show = message.isStatePending)

                this.attachmentLayout?.also { attachmentLayout ->
                    attachmentLayout.removeAllViews()
                    attachmentLayout.visibility = if(message.attachments.isNotEmpty()) {
                        message.attachments.forEach { attachment ->

                            val ll = LinearLayout(chatActivity).apply {
                                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                                orientation = LinearLayout.VERTICAL
                                gravity = Gravity.CENTER_HORIZONTAL
                                minimumWidth = 150.dp2px
                            }

                            val iv = ImageView(chatActivity)
                            when {
                                attachment.isImage() -> {
                                    //Image attachment
                                    iv.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 80.dp2px)
                                    val path = attachment.path
                                    if(path?.isNotEmpty() == true) {
                                        ClyImageRequest(context = chatActivity, url = path)
                                                .centerCrop()
                                                .placeholder(placeholder = R.drawable.io_customerly__pic_placeholder)
                                                .into(imageView = iv)
                                                .start()
                                        ll.setOnClickListener { it.activity?.startClyFullScreenImageActivity(imageUrl = path) }
                                    } else {
                                        iv.scaleType = ImageView.ScaleType.CENTER_CROP
                                        try {
                                            attachment.loadBase64FromMemory(context = chatActivity)?.let { Base64.decode(it, Base64.DEFAULT) }?.also {
                                                iv.setImageBitmap(BitmapFactory.decodeByteArray(it, 0, it.size))
                                            } ?: iv.setImageResource(R.drawable.io_customerly__pic_placeholder)
                                        } catch (outOfMemoryError: OutOfMemoryError) {
                                            iv.setImageResource(R.drawable.io_customerly__pic_placeholder)
                                        }
                                    }
                                }
                                else -> {
                                    ll.setBackgroundResource(if (message.writer.isUser) R.drawable.io_customerly__attachmentfile_border_user else R.drawable.io_customerly__attachmentfile_border_account)
                                    ll.setPadding(10.dp2px, 0, 10.dp2px, 10.dp2px)
                                    iv.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 40.dp2px)
                                    iv.setPadding(0, 15.dp2px, 0, 0)
                                    iv.setImageResource(this.iconAttachment)
                                    val name = attachment.name
                                    val path = attachment.path
                                    val weakChatActivity = chatActivity.weak()
                                    if(path?.isNotEmpty() == true) {
                                        ll.setOnClickListener {
                                            AlertDialog.Builder(chatActivity)
                                                    .setTitle(R.string.io_customerly__download)
                                                    .setMessage(R.string.io_customerly__download_the_file_)
                                                    .setPositiveButton(android.R.string.ok) { _, _ -> weakChatActivity.get()?.startAttachmentDownload(name, path) }
                                                    .setNegativeButton(android.R.string.cancel, null)
                                                    .setCancelable(true)
                                                    .show()
                                        }
                                    }
                                }
                            }

                            val tv = TextView(chatActivity).apply {
                                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                                setTextColor(ContextCompat.getColorStateList(chatActivity, if (message.writer.isUser) R.color.io_customerly__textcolor_white_grey else R.color.io_customerly__textcolor_malibu_grey))
                                setLines(1)
                                setSingleLine()
                                ellipsize = TextUtils.TruncateAt.MIDDLE
                                text = attachment.name
                                setPadding(0, 10.dp2px, 0, 0)
                                setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)

                            }
                            this.attachmentLayout.addView(ll.apply {
                                this.addView(iv)
                                this.addView(tv)
                            })
                            (ll.layoutParams as LinearLayout.LayoutParams).topMargin = 10.dp2px
                        }
                        View.VISIBLE
                    } else {
                        View.GONE
                    }
                }
            }

            internal open fun onEmptyContent() {
                this.content.apply {
                    text = null
                    visibility = View.GONE
                }
            }

            internal class User(recyclerView: RecyclerView)
                : Message(recyclerView = recyclerView, layoutRes = R.layout.io_customerly__li_bubble_user, sendingProgressBarResId = R.id.io_customerly__content_sending_progressspinner,
                    pendingMessageResId = R.id.io_customerly__pending_message_label, iconAttachment = R.drawable.io_customerly__ic_attach_user) {
                init {
                    (this.itemView.findViewById<View>(R.id.bubble).background as? GradientDrawable)?.setColor(Customerly.lastPing.widgetColor)
                }
            }

            internal sealed class Account(recyclerView: RecyclerView, @LayoutRes layoutRes: Int)
                : Message(recyclerView = recyclerView, layoutRes = layoutRes, iconAttachment = R.drawable.io_customerly__ic_attach_account_40dp) {

                internal class Text(recyclerView: RecyclerView) : Account(recyclerView = recyclerView, layoutRes = R.layout.io_customerly__li_bubble_account_text) {

                    private val accountName: TextView = this.itemView.findViewById(R.id.io_customerly__name)

                    override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                        super.apply(chatActivity = chatActivity, message = message, dateToDisplay = dateToDisplay, isFirstMessageOfSender = isFirstMessageOfSender)
                        this.accountName.visibility = message?.takeIf { isFirstMessageOfSender }?.writer?.getName(context = chatActivity)?.takeIf { it.isNotEmpty() }?.let {
                            this.accountName.text = it
                            View.VISIBLE
                        } ?: View.GONE
                    }
                }

                internal class Rich(recyclerView: RecyclerView) : Account(recyclerView = recyclerView, layoutRes = R.layout.io_customerly__li_bubble_account_rich) {

                    override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                        super.apply(chatActivity = chatActivity, message = message, dateToDisplay = dateToDisplay, isFirstMessageOfSender = isFirstMessageOfSender)
                        this.content.setText(Customerly.currentUser.email?.let { R.string.io_customerly__rich_message_text_via_email } ?: R.string.io_customerly__rich_message_text_no_email)
                        ({ v : View -> message?.richMailLink?.let { v.activity?.startClyWebViewActivity(targetUrl = it) } ?: Unit }
                                ).also {
                            this.content.setOnClickListener(it)
                            this.itemView.setOnClickListener(it)
                        }
                    }

                    override fun onEmptyContent() {}
                }

            }

            internal sealed class Bot(recyclerView: RecyclerView, @LayoutRes layoutRes: Int)
                : Message(recyclerView = recyclerView, layoutRes = layoutRes, iconAttachment = R.drawable.io_customerly__ic_attach_account_40dp) {

                override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                    super.apply(chatActivity = chatActivity, message = message, dateToDisplay = dateToDisplay, isFirstMessageOfSender = true)
                }

                internal class Text(recyclerView: RecyclerView)
                    : Bot(recyclerView = recyclerView, layoutRes = R.layout.io_customerly__li_bubble_bot_text)

                internal sealed class Form(recyclerView: RecyclerView, @LayoutRes layoutRes: Int, @IntRange(from = 1, to = Long.MAX_VALUE) privacyPolicyResId: Int = R.id.io_customerly__botform_privacy_policy)
                    : Bot(recyclerView = recyclerView, layoutRes = layoutRes) {

                    private val privacyPolicy: TextView = this.itemView.findViewById(privacyPolicyResId)

                    override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                        super.apply(chatActivity = chatActivity, message = message, dateToDisplay = dateToDisplay, isFirstMessageOfSender = true)

                        val privacyUrl = Customerly.lastPing.privacyUrl
                        if(privacyUrl?.isNotEmpty() == true && !Customerly.currentUser.privacyPolicyAlreadyChecked()) {
                            this.privacyPolicy.also { policy ->
                                policy.setOnClickListener {
                                    it.activity?.startClyWebViewActivity(targetUrl = privacyUrl, showClearInsteadOfBack = true)
                                }

                                val textIAccept = policy.context.getString(R.string.io_customerly__i_accept_the_)
                                val textPrivacyPolicy = policy.context.getString(R.string.io_customerly__privacy_policy)
                                policy.text = SpannableString(textIAccept + textPrivacyPolicy).apply {
                                    this.setSpan(
                                            ForegroundColorSpan(Color.parseColor("#38b9ff")),
                                            textIAccept.length,
                                            textIAccept.length + textPrivacyPolicy.length,
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                }
                                policy.visibility = View.VISIBLE
                            }
                        } else {
                            this.privacyPolicy.visibility = View.GONE
                        }
                    }

                    internal class Profiling(recyclerView: RecyclerView)
                        : Bot.Form(recyclerView = recyclerView, layoutRes = R.layout.io_customerly__li_bubble_bot_profilingform) {

                        private val containerInput: LinearLayout = this.itemView.findViewById(R.id.io_customerly__profilingform_container_input)
                        private val formInputInput: EditText = this.containerInput.findViewById(R.id.io_customerly__profilingform_input)
                        private val formInputSubmit: ImageButton = this.containerInput.findViewById(R.id.io_customerly__profilingform_button_submit_input)

                        private val containerTruefalse: LinearLayout = this.itemView.findViewById(R.id.io_customerly__profilingform_container_truefalse)
                        private val containerDate: LinearLayout = this.itemView.findViewById(R.id.io_customerly__profilingform_container_date)
                        private val sendingSpinner: View = this.itemView.findViewById(R.id.io_customerly__content_sending_progressspinner)

                        init {
                            this.formInputInput.apply {
                                this.addTextChangedListener(object : TextWatcher {
                                    val weakBotVH = this@Profiling.weak()
                                    override fun afterTextChanged(s: Editable?) {
                                        this.weakBotVH.get()?.currentForm?.apply {
                                            this.answer = when (this.cast) {
                                                ClyFormCast.NUMERIC -> s?.toString()?.toDoubleOrNull()
                                                ClyFormCast.STRING -> s?.toString()
                                                else -> null
                                            }
                                        }
                                    }
                                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
                                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { }
                                })
                            }

                            val weakBotForm = this.weak()
                            this.formInputSubmit.setOnClickListener { btnSendInput ->
                                weakBotForm.reference { vh ->
                                    val form = vh.currentForm
                                    if (form?.answer != null && ((form.answer as? String)?.isEmpty() != true)) {
                                        btnSendInput.dismissKeyboard()
                                        btnSendInput.isEnabled = false
                                        vh.formInputInput.apply {
                                            this.isEnabled = false
                                            this.isFocusable = false
                                            this.isFocusableInTouchMode = false
                                            this.clearFocus()
                                        }
                                        form.answerConfirmed = true
                                        vh.sendingSpinner.visibility = View.VISIBLE
                                        form.sendAnswer(chatActivity = btnSendInput.activity as? ClyChatActivity) {
                                            weakBotForm.get()?.sendingSpinner?.visibility = View.GONE
                                        }
                                    }
                                }
                            }
                        }

                        private var currentForm: ClyFormDetails? = null

                        override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                            super.apply(chatActivity = chatActivity, message = message, dateToDisplay = dateToDisplay, isFirstMessageOfSender = true)

                            if(message is ClyMessage.Bot.Form.Profiling) {
                                val form = message.form
                                this.currentForm = form
                                when (form.cast) {
                                    ClyFormCast.STRING, ClyFormCast.NUMERIC -> {
                                        this.containerTruefalse.visibility = View.GONE
                                        this.containerDate.visibility = View.GONE
                                        this.containerInput.visibility = View.VISIBLE
                                        this.formInputInput.also { inputEditText ->
                                            when (form.cast) {
                                                ClyFormCast.NUMERIC -> {
                                                    inputEditText.inputType = InputType.TYPE_CLASS_NUMBER
                                                    inputEditText.setText((form.answer as? Double)?.let {
                                                        DecimalFormat("#################.################").format(it)
                                                    })
                                                }
                                                else /* ClyFormCast.STRING */ -> {
                                                    inputEditText.inputType = InputType.TYPE_CLASS_TEXT
                                                    inputEditText.setText(form.answer as? String)
                                                }
                                            }
                                            inputEditText.hint = form.hint
                                            inputEditText.isEnabled = !form.answerConfirmed
                                            inputEditText.isFocusable = !form.answerConfirmed
                                            inputEditText.isFocusableInTouchMode = !form.answerConfirmed
                                            this.formInputSubmit.isEnabled = !form.answerConfirmed
                                        }
                                    }
                                    ClyFormCast.DATE -> {
                                        this.containerTruefalse.visibility = View.GONE
                                        this.containerInput.visibility = View.GONE
                                        this.formInputInput.also { inputEditText ->
                                            inputEditText.isEnabled = false
                                            inputEditText.isFocusable = false
                                            inputEditText.isFocusableInTouchMode = false
                                            inputEditText.clearFocus()
                                        }
                                        this.containerDate.visibility = View.VISIBLE
                                        if (!form.answerConfirmed) {
                                            this.containerDate.findViewById<TextView>(R.id.io_customerly__profilingform_date).apply {
                                                this.hint = form.hint?.takeIf { it.isNotEmpty() } ?: "--/--/----"
                                                this.text = (form.answer as? Long)?.nullOnException { shortDateFomatter.format(it.asDate) }
                                                this.setOnClickListener {

                                                    val now = Calendar.getInstance().apply {
                                                        (form.answer as? Long)?.also { answer ->
                                                            this.timeInMillis = answer
                                                        }
                                                    }

                                                    DatePickerDialog(it.context,
                                                            android.app.DatePickerDialog.OnDateSetListener { _, year, month, day ->
                                                                (it.parent as? View)?.findViewById<android.widget.TextView>(R.id.io_customerly__profilingform_date)?.apply {
                                                                    this.text = Calendar.getInstance().apply {
                                                                        this.set(Calendar.YEAR, year)
                                                                        this.set(Calendar.MONTH, month)
                                                                        this.set(Calendar.DAY_OF_MONTH, day)
                                                                    }.time.nullOnException { date ->
                                                                        form.answer = date.time
                                                                        shortDateFomatter.format(date)
                                                                    }
                                                                }
                                                            },
                                                            now.get(Calendar.YEAR),
                                                            now.get(Calendar.MONTH),
                                                            now.get(Calendar.DAY_OF_MONTH)).show()
                                                }

                                            }
                                            this.containerDate.findViewById<View>(R.id.io_customerly__profilingform_button_submit_date).apply {
                                                this.isEnabled = true
                                                val weakSendingSpinner = sendingSpinner.weak()
                                                this.setOnClickListener { submitDate ->
                                                    if (form.answer != null) {
                                                        (submitDate.parent as? View)?.findViewById<android.widget.TextView>(R.id.io_customerly__profilingform_date)?.apply {
                                                            this.isEnabled = false
                                                            submitDate.isEnabled = false
                                                            form.answerConfirmed = true
                                                            weakSendingSpinner.get()?.visibility = View.VISIBLE
                                                            form.sendAnswer(chatActivity = this.activity as? ClyChatActivity) {
                                                                weakSendingSpinner.get()?.visibility = View.GONE
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            this.containerDate.findViewById<TextView>(R.id.io_customerly__profilingform_date).apply {
                                                this.hint = form.hint?.takeIf { it.isNotEmpty() } ?: "--/--/----"
                                                this.isEnabled = false
                                                this.text = (form.answer as? Long)?.nullOnException { shortDateFomatter.format(it.asDate) }
                                            }
                                            this.containerDate.findViewById<View>(R.id.io_customerly__profilingform_button_submit_date).isEnabled = false
                                        }
                                    }
                                    ClyFormCast.BOOL -> {
                                        this.containerDate.visibility = View.GONE
                                        this.containerInput.visibility = View.GONE
                                        this.formInputInput.also { inputEditText ->
                                            inputEditText.isEnabled = false
                                            inputEditText.isFocusable = false
                                            inputEditText.isFocusableInTouchMode = false
                                            inputEditText.clearFocus()
                                        }
                                        this.containerTruefalse.visibility = View.VISIBLE

                                        this.containerTruefalse.findViewById<View>(R.id.io_customerly__profilingform_button_true).isSelected = form.answer == true
                                        this.containerTruefalse.findViewById<View>(R.id.io_customerly__profilingform_button_false).isSelected = form.answer == false

                                        val weakSendingSpinner = this.sendingSpinner.weak()
                                        this.containerTruefalse.findViewById<View>(R.id.io_customerly__profilingform_button_true).setOnClickListener { btnTrue ->
                                            btnTrue.isSelected = true
                                            (btnTrue.parent as? View)?.findViewById<View>(R.id.io_customerly__profilingform_button_false)?.isSelected = false
                                            form.answer = true
                                            form.answerConfirmed = true
                                            weakSendingSpinner.get()?.visibility = View.VISIBLE
                                            form.sendAnswer(chatActivity = btnTrue.activity as? ClyChatActivity) {
                                                weakSendingSpinner.get()?.visibility = View.GONE
                                            }
                                        }
                                        this.containerTruefalse.findViewById<View>(R.id.io_customerly__profilingform_button_false).setOnClickListener { btnFalse ->
                                            btnFalse.isSelected = true
                                            (btnFalse.parent as? View)?.findViewById<View>(R.id.io_customerly__profilingform_button_true)?.isSelected = false
                                            form.answer = false
                                            form.answerConfirmed = true
                                            weakSendingSpinner.get()?.visibility = View.VISIBLE
                                            form.sendAnswer(chatActivity = btnFalse.activity as? ClyChatActivity) {
                                                weakSendingSpinner.get()?.visibility = View.GONE
                                            }
                                        }
                                    }
                                }
                            } else {
                                this.currentForm = null
                                this.containerTruefalse.visibility = View.GONE
                                this.containerDate.visibility = View.GONE
                                this.containerInput.visibility = View.GONE
                            }
                        }
                    }

                    internal class AskEmail(recyclerView: RecyclerView)
                        : Bot.Form(recyclerView = recyclerView, layoutRes = R.layout.io_customerly__li_bubble_bot_askemail) {

                        private val input: EditText = this.itemView.findViewById(R.id.io_customerly__askemail_input)
                        private val submit: View = this.itemView.findViewById(R.id.io_customerly__askemail_submit)
                        private val sendingSpinner: View = this.itemView.findViewById(R.id.io_customerly__content_sending_progressspinner)

                        private var pendingMessage: ClyMessage.Human.UserLocal? = null

                        init {
                            val weakInput = this.input.weak()
                            val weakSendingSpinner = this.sendingSpinner.weak()
                            this.submit.setOnClickListener { btn ->
                                val weakActivity = btn.activity?.weak()
                                val weakSubmit = btn.weak()
                                weakInput.get()?.let { input ->
                                    val email = input.text.toString().trim().toLowerCase(Locale.ITALY)
                                    if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                                        btn.dismissKeyboard()
                                        weakSendingSpinner.get()?.visibility = View.VISIBLE

                                        if(Customerly.iamAnonymous()) {
                                            ClyApiRequest(
                                                    context = input.context,
                                                    endpoint = ENDPOINT_PING,
                                                    jsonObjectConverter = { Unit },
                                                    callback = { response ->
                                                        weakSendingSpinner.get()?.visibility = View.GONE
                                                        weakInput.get()?.apply {
                                                            this.isEnabled = false
                                                            this.isFocusable = false
                                                            this.isFocusableInTouchMode = false
                                                            this.clearFocus()
                                                        }
                                                        weakSubmit.get()?.isEnabled = false

                                                        this.pendingMessage?.also { pendingMessage ->
                                                            Customerly.jwtToken?.userID?.apply { pendingMessage.writer.id = this }
                                                            pendingMessage.setStateSending()
                                                            (weakActivity?.get() as? ClyChatActivity)?.notifyItemChangedInList(message = pendingMessage)

                                                            when (response) {
                                                                is ClyApiResponse.Success -> {
                                                                    (weakActivity?.get() as? ClyChatActivity)?.startSendMessageRequest(message = pendingMessage)
                                                                }
                                                                is ClyApiResponse.Failure -> {
                                                                    weakInput.reference { input ->
                                                                        if (input.tag == null) {
                                                                            input.tag = Unit
                                                                            val originalColor = input.textColors.defaultColor
                                                                            input.setTextColor(Color.RED)
                                                                            input.addTextChangedListener(object : TextWatcher {
                                                                                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                                                                                override fun afterTextChanged(s: Editable) {}
                                                                                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                                                                                    weakInput.reference {
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
                                                        }
                                                    })
                                                    .p(key = "lead_email", value = email)
                                                    .start()
                                        } else {
                                            ClyApiRequest(
                                                    context = input.context,
                                                    endpoint = ENDPOINT_FORM_ATTRIBUTE,
                                                    jsonObjectConverter = { jo -> jo },
                                                    callback = { response ->
                                                        weakSendingSpinner.get()?.visibility = View.GONE
                                                        weakInput.get()?.apply {
                                                            this.isEnabled = false
                                                            this.isFocusable = false
                                                            this.isFocusableInTouchMode = false
                                                            this.clearFocus()
                                                        }
                                                        weakSubmit.get()?.isEnabled = false
                                                        when (response) {
                                                            is ClyApiResponse.Success -> {
                                                                Customerly.clySocket.sendAttributeSet(name = "email", value = email, cast = ClyFormCast.STRING, userData = response.result)
                                                                Customerly.currentUser.updateUser(
                                                                        isUser = Customerly.iamUser(),
                                                                        contactEmail = email,
                                                                        contactName = response.result.optJSONObject("data")?.optTyped<String>(name = "name"),
                                                                        userId = response.result.optJSONObject("data")?.optTyped<String>(name = "user_id"))
                                                                (weakActivity?.get() as? ClyChatActivity)?.tryLoadForm()
                                                            }
                                                            is ClyApiResponse.Failure -> {
                                                                weakInput.reference { input ->
                                                                    if (input.tag == null) {
                                                                        input.tag = Unit
                                                                        val originalColor = input.textColors.defaultColor
                                                                        input.setTextColor(Color.RED)
                                                                        input.addTextChangedListener(object : TextWatcher {
                                                                            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                                                                            override fun afterTextChanged(s: Editable) {}
                                                                            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                                                                                weakInput.reference {
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
                                                    })
                                                    .p(key = "name", value = "email")
                                                    .p(key = "value", value = email)
                                                    .start()
                                        }
                                    } else {
                                        if (input.tag == null) {
                                            input.tag = Unit
                                            val originalColor = input.textColors.defaultColor
                                            input.setTextColor(Color.RED)
                                            input.addTextChangedListener(object : TextWatcher {
                                                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
                                                override fun afterTextChanged(s: Editable) {}
                                                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                                                    weakInput.reference {
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
                        }

                        override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                            super.apply(chatActivity = chatActivity, message = message, dateToDisplay = dateToDisplay, isFirstMessageOfSender = true)
                            this.pendingMessage = (message as? ClyMessage.Bot.Form.AskEmail)?.pendingMessage
                            ( Customerly.currentUser.email?.let { it to false } ?: null to true )
                                    .also { (email,formEnabled) ->
                                        this.input.isEnabled = formEnabled
                                        this.input.isFocusable = formEnabled
                                        this.input.isFocusableInTouchMode = formEnabled
                                        this.submit.isEnabled = formEnabled
                                        this.input.setText(email)
                                    }
                        }
                    }
                }
            }
        }

    }

    internal class AccountInfos(recyclerView: RecyclerView)
        : ClyChatViewHolder(recyclerView = recyclerView, layoutRes = R.layout.io_customerly__li_bubble_accountinfos) {

        fun apply(chatActivity: ClyChatActivity) {

            val clyAdminFull = chatActivity.conversationFullAdmin

            if(clyAdminFull != null) {

                this.itemView.io_customerly__cardaccount_job.apply {
                    this.visibility = clyAdminFull.jobTitle?.let { jobTitle ->
                        this.text = jobTitle
                        View.VISIBLE
                    } ?: View.GONE
                }

                this.itemView.io_customerly__cardaccount_motto.apply {
                    this.visibility = clyAdminFull.description?.let { description ->
                        this.text = description
                        View.VISIBLE
                    } ?: View.GONE
                }

                this.itemView.io_customerly__cardaccount_location_layout.apply {
                    this.visibility = clyAdminFull.location?.let { location ->
                        this.io_customerly__cardaccount_location.text = location
                        View.VISIBLE
                    } ?: View.GONE
                }

                this.itemView.io_customerly__cardaccount_fb.apply {
                    this.visibility = clyAdminFull.socialProfileFacebook?.let { fbUrl ->
                        this.setOnClickListener { it.activity?.startUrl(url = fbUrl) }
                        View.VISIBLE
                    } ?: View.GONE
                }

                this.itemView.io_customerly__cardaccount_twitter.apply {
                    this.visibility = clyAdminFull.socialProfileTwitter?.let { twitterUrl ->
                        this.setOnClickListener { it.activity?.startUrl(url = twitterUrl) }
                        View.VISIBLE
                    } ?: View.GONE
                }

                this.itemView.io_customerly__cardaccount_linkedin.apply {
                    this.visibility = clyAdminFull.socialProfileLinkedin?.let { linkedinUrl ->
                        this.setOnClickListener { it.activity?.startUrl(url = linkedinUrl) }
                        View.VISIBLE
                    } ?: View.GONE
                }

                this.itemView.io_customerly__cardaccount_instagram.apply {
                    this.visibility = clyAdminFull.socialProfileInstagram?.let { instagramUrl ->
                        this.setOnClickListener { it.activity?.startUrl(url = instagramUrl) }
                        View.VISIBLE
                    } ?: View.GONE
                }

                ClyImageRequest(context = chatActivity, url = clyAdminFull.getImageUrl(sizePx = 80.dp2px))
                        .fitCenter()
                        .transformCircle()
                        .resize(width = 80.dp2px)
                        .placeholder(placeholder = R.drawable.io_customerly__ic_default_admin)
                        .into(imageView = this.itemView.io_customerly__icon)
                        .start()
                this.itemView.io_customerly__cardaccount_name.apply {
                    this.text = clyAdminFull.name
                }

                this.itemView.io_customerly__cardaccount_cardlayout.visibility = View.VISIBLE

            } else {
                this.itemView.io_customerly__cardaccount_cardlayout.visibility = View.GONE
            }

        }
    }

}