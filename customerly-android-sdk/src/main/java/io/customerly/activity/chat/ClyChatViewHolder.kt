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
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.support.annotation.IntRange
import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import io.customerly.Customerly
import io.customerly.R
import io.customerly.activity.fullscreen.startClyFullScreenImageActivity
import io.customerly.activity.startClyWebViewActivity
import io.customerly.entity.chat.ClyMessage
import io.customerly.entity.ping.ClyFormCast
import io.customerly.entity.urlImageAccount
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.ggkext.*
import io.customerly.utils.shortDateFomatter
import kotlinx.android.synthetic.main.io_customerly__li_bubble_account_infos.view.*
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
                        ClyImageRequest(context = chatActivity, url = urlImageAccount(accountId = typingAccountID, sizePX = this.iconSize))
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
                sendingProgressBarResId: Int = 0,
                @IntRange(from = 1, to = Long.MAX_VALUE) attachmentLayoutResId: Int = R.id.io_customerly__attachment_layout,
                @IntRange(from = 1, to = Long.MAX_VALUE) dateResId: Int = R.id.io_customerly__date,
                @IntRange(from = 1, to = Long.MAX_VALUE) timeResId: Int = R.id.io_customerly__time,
                @IntRange(from = 1, to = Long.MAX_VALUE) val iconAttachment: Int
        ) : ClyChatViewHolder.Bubble(recyclerView = recyclerView, layoutRes = layoutRes) {

            private val attachmentLayout: LinearLayout? = this.itemView.findViewById(attachmentLayoutResId)
            private val sendingProgressBar: ProgressBar? = this.itemView.findViewById(sendingProgressBarResId)
            private val date: TextView = this.itemView.findViewById(dateResId)
            private val time: TextView = this.itemView.findViewById(timeResId)

            override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                if(message == null) {
                    //Always != null for this ViewHolder
                    this.icon?.visibility = View.INVISIBLE
                    this.content.apply {
                        this.text = null
                        this.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                        this.setOnClickListener(null)
                    }
                    this.sendingProgressBar?.visibility = View.GONE
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

                this.time.text = message.timeString

                this.icon?.also { icon ->
                    icon.visibility = if (isFirstMessageOfSender) {
                        message.writer.loadUrl(into = icon, size = this.iconSize)
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

                this.sendingProgressBar?.visibility = if(message.isStateSending) View.VISIBLE else View.GONE

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

            internal class User(recyclerView: RecyclerView) : Message(
                    recyclerView = recyclerView,
                    layoutRes = R.layout.io_customerly__li_bubble_user,
                    sendingProgressBarResId = R.id.io_customerly__content_sending__only_user_li,
                    iconAttachment = R.drawable.io_customerly__ic_attach_user) {
                init {
                    (this.itemView.findViewById<View>(R.id.bubble).background as? GradientDrawable)?.setColor(Customerly.lastPing.widgetColor)
                }
            }

            internal class Account(recyclerView: RecyclerView) : Message(
                    recyclerView = recyclerView,
                    layoutRes = R.layout.io_customerly__li_bubble_account,
                    iconAttachment = R.drawable.io_customerly__ic_attach_account_40dp) {

                private val accountName: TextView = this.itemView.findViewById(R.id.io_customerly__name)

                override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                    super.apply(chatActivity = chatActivity, message = message, dateToDisplay = dateToDisplay, isFirstMessageOfSender = isFirstMessageOfSender)
                    this.accountName.visibility = message?.takeIf { isFirstMessageOfSender }?.writer?.getName(context = chatActivity)?.takeIf { it.isNotEmpty() }?.let {
                        this.accountName.text = it
                        View.VISIBLE
                    } ?: View.GONE
                }
            }

            internal class AccountRich(recyclerView: RecyclerView) : Message(
                    recyclerView = recyclerView,
                    layoutRes = R.layout.io_customerly__li_bubble_account_rich,
                    iconAttachment = R.drawable.io_customerly__ic_attach_account_40dp) {

                override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                    super.apply(chatActivity = chatActivity, message = message, dateToDisplay = dateToDisplay, isFirstMessageOfSender = isFirstMessageOfSender)
                    ({ v : View -> message?.richMailLink?.let { v.activity?.startClyWebViewActivity(targetUrl = it) } ?: Unit }
                            ).also {
                        this.content.setOnClickListener(it)
                        this.itemView.setOnClickListener(it)
                    }
                }

                override fun onEmptyContent() {}
            }

            internal class Bot(recyclerView: RecyclerView) : Message(
                    recyclerView = recyclerView,
                    layoutRes = R.layout.io_customerly__li_bubble_bot,
                    iconAttachment = R.drawable.io_customerly__ic_attach_account_40dp) {

                init {
                    this.itemView.findViewById<View>(R.id.io_customerly__bubble).apply {
                        this.layoutParams = this.layoutParams.apply {
                            this.width = RelativeLayout.LayoutParams.MATCH_PARENT
                        }
                    }
                }
                private val containerTruefalse: LinearLayout = this.itemView.findViewById(R.id.io_customerly__profilingform_container_truefalse)
                private val containerInput: LinearLayout = this.itemView.findViewById(R.id.io_customerly__profilingform_container_input)
                private val containerDate: LinearLayout = this.itemView.findViewById(R.id.io_customerly__profilingform_container_date)
                private val sendingSpinner: View = this.itemView.findViewById(R.id.io_customerly__content_sending__only_user_li)

                override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
                    super.apply(chatActivity = chatActivity, message = message, dateToDisplay = dateToDisplay, isFirstMessageOfSender = true)

                    if(message is ClyMessage.BotProfilingForm) {
                        when(message.form.cast) {
                            ClyFormCast.STRING, ClyFormCast.NUMERIC -> {
                                this.containerTruefalse.visibility = View.GONE
                                this.containerDate.visibility = View.GONE
                                this.containerInput.visibility = View.VISIBLE
                                if(!message.form.answerConfirmed) {
                                    this.containerInput.findViewById<EditText>(R.id.io_customerly__profilingform_input).apply {
                                        this.hint = message.form.hint
                                        val textWatcher = object : TextWatcher {
                                            override fun afterTextChanged(s: Editable?) {  }
                                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { }
                                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                                                when (message.form.cast) {
                                                    ClyFormCast.NUMERIC ->  message.form.answer = s?.toString()?.toDoubleOrNull()
                                                    ClyFormCast.STRING ->  message.form.answer = s?.toString()
                                                    else -> message.form.answer = null
                                                }
                                            }
                                        }

                                        (this.tag as? TextWatcher)?.also { this.removeTextChangedListener(it) }
                                        this.tag = textWatcher
                                        this.addTextChangedListener(textWatcher)

                                        when (message.form.cast) {
                                            ClyFormCast.NUMERIC -> {
                                                this.inputType = InputType.TYPE_CLASS_NUMBER
                                                this.setText((message.form.answer as? Double)?.let {
                                                    DecimalFormat("#.###########").format(it)
                                                })
                                            }
                                            ClyFormCast.STRING -> {
                                                this.inputType = InputType.TYPE_CLASS_TEXT
                                                this.setText(message.form.answer as? String)
                                            }
                                            else -> {
                                                this.inputType = InputType.TYPE_CLASS_TEXT
                                                this.setText(message.form.answer as? String)
                                            }
                                        }
                                    }

                                    this.containerInput.findViewById<View>(R.id.io_customerly__profilingform_button_submit_input).apply {
                                        val weakSendingSpinner = sendingSpinner.weak()
                                        this.setOnClickListener { btnSendInput ->
                                            if(message.form.answer != null && ((message.form.answer as? String)?.isEmpty() != true) ) {
                                                btnSendInput.isEnabled = false
                                                (btnSendInput.parent as? View)?.findViewById<View>(R.id.io_customerly__profilingform_input)?.isEnabled = false
                                                message.form.answerConfirmed = true
                                                weakSendingSpinner.get()?.visibility = View.VISIBLE
                                                message.form.sendAnswer(chatActivity = btnSendInput.activity as? ClyChatActivity) {
                                                    weakSendingSpinner.get()?.visibility = View.GONE
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    this.containerInput.findViewById<EditText>(R.id.io_customerly__profilingform_input).apply {
                                        this.hint = message.form.hint
                                        this.setText(message.form.answer?.toString())
                                    }
                                    this.containerInput.findViewById<View>(R.id.io_customerly__profilingform_button_submit_input).isEnabled = false
                                }
                            }
                            ClyFormCast.DATE -> {
                                this.containerTruefalse.visibility = View.GONE
                                this.containerInput.visibility = View.GONE
                                this.containerDate.visibility = View.VISIBLE
                                if(!message.form.answerConfirmed) {
                                    this.containerDate.findViewById<TextView>(R.id.io_customerly__profilingform_date).apply {
                                        this.hint = message.form.hint?.takeIf { it.isNotEmpty() } ?: "--/--/----"
                                        this.text = (message.form.answer as? Long)?.nullOnException { shortDateFomatter.format(it.asDate) }
                                        this.setOnClickListener {

                                            val now = Calendar.getInstance().apply {
                                                (message.form.answer as? Long)?.also {
                                                    this.timeInMillis = it
                                                }
                                            }

                                            DatePickerDialog(it.context,
                                                    android.app.DatePickerDialog.OnDateSetListener { _, year, month, day ->
                                                        (it.parent as? View)?.findViewById<android.widget.TextView>(R.id.io_customerly__profilingform_date)?.apply {
                                                            this.text = Calendar.getInstance().apply {
                                                                this.set(Calendar.YEAR, year)
                                                                this.set(Calendar.MONTH, month)
                                                                this.set(Calendar.DAY_OF_MONTH, day)
                                                            }.time.let {
                                                                message.form.answer = it.time
                                                                it.nullOnException { shortDateFomatter.format(it) }
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
                                            if(message.form.answer != null) {
                                                (submitDate.parent as? View)?.findViewById<android.widget.TextView>(R.id.io_customerly__profilingform_date)?.apply {
                                                    this.isEnabled = false
                                                    submitDate.isEnabled = false
                                                    message.form.answerConfirmed = true
                                                    weakSendingSpinner.get()?.visibility = View.VISIBLE
                                                    message.form.sendAnswer(chatActivity = this.activity as? ClyChatActivity) {
                                                        weakSendingSpinner.get()?.visibility = View.GONE
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    this.containerDate.findViewById<TextView>(R.id.io_customerly__profilingform_date).apply {
                                        this.hint = message.form.hint?.takeIf { it.isNotEmpty() } ?: "--/--/----"
                                        this.isEnabled = false
                                        this.text = (message.form.answer as? Long)?.nullOnException { shortDateFomatter.format(it.asDate) }
                                    }
                                    this.containerDate.findViewById<View>(R.id.io_customerly__profilingform_button_submit_date).isEnabled = false
                                }
                            }
                            ClyFormCast.BOOL -> {
                                this.containerDate.visibility = View.GONE
                                this.containerInput.visibility = View.GONE
                                this.containerTruefalse.visibility = View.VISIBLE

                                this.containerTruefalse.findViewById<View>(R.id.io_customerly__profilingform_button_true).isSelected = message.form.answer == true
                                this.containerTruefalse.findViewById<View>(R.id.io_customerly__profilingform_button_false).isSelected = message.form.answer == false

                                val weakSendingSpinner = sendingSpinner.weak()
                                this.containerTruefalse.findViewById<View>(R.id.io_customerly__profilingform_button_true).setOnClickListener { btnTrue ->
                                    btnTrue.isSelected = true
                                    (btnTrue.parent as? View)?.findViewById<View>(R.id.io_customerly__profilingform_button_false)?.isSelected = false
                                    message.form.answer = true
                                    message.form.answerConfirmed = true
                                    weakSendingSpinner.get()?.visibility = View.VISIBLE
                                    message.form.sendAnswer(chatActivity = btnTrue.activity as? ClyChatActivity) {
                                        weakSendingSpinner.get()?.visibility = View.GONE
                                    }
                                }
                                this.containerTruefalse.findViewById<View>(R.id.io_customerly__profilingform_button_false).setOnClickListener { btnFalse ->
                                    btnFalse.isSelected = true
                                    (btnFalse.parent as? View)?.findViewById<View>(R.id.io_customerly__profilingform_button_true)?.isSelected = false
                                    message.form.answer = false
                                    message.form.answerConfirmed = true
                                    weakSendingSpinner.get()?.visibility = View.VISIBLE
                                    message.form.sendAnswer(chatActivity = btnFalse.activity as? ClyChatActivity) {
                                        weakSendingSpinner.get()?.visibility = View.GONE
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

    }

    internal class AccountInfos(recyclerView: RecyclerView) : ClyChatViewHolder(recyclerView = recyclerView, layoutRes = R.layout.io_customerly__li_bubble_account_infos) {

        fun apply(chatActivity: ClyChatActivity) {
            (
                    chatActivity.conversationFullAdmin?.also { clyAdminFull ->

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

                    } ?: {
                        this.itemView.io_customerly__cardaccount_job.visibility = View.GONE
                        this.itemView.io_customerly__cardaccount_motto.visibility = View.GONE
                        this.itemView.io_customerly__cardaccount_location_layout.visibility = View.GONE
                        this.itemView.io_customerly__cardaccount_fb.visibility = View.GONE
                        this.itemView.io_customerly__cardaccount_twitter.visibility = View.GONE
                        this.itemView.io_customerly__cardaccount_linkedin.visibility = View.GONE
                        this.itemView.io_customerly__cardaccount_instagram.visibility = View.GONE
                        Customerly.lastPing.activeAdmins?.asSequence()?.maxBy { it.lastActive }
                    }()
                        )?.also { admin ->
                        ClyImageRequest(context = chatActivity, url = admin.getImageUrl(sizePx = 80.dp2px))
                                .fitCenter()
                                .transformCircle()
                                .resize(width = 80.dp2px)
                                .placeholder(placeholder = R.drawable.io_customerly__ic_default_admin)
                                .into(imageView = this.itemView.io_customerly__icon)
                                .start()
                        this.itemView.io_customerly__cardaccount_name.apply {
                            this.text = admin.name
                        }
                    }

        }
    }

}