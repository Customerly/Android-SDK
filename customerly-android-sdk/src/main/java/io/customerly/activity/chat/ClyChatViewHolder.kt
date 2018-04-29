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

import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.support.annotation.IntRange
import android.support.annotation.LayoutRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import io.customerly.*
import io.customerly.XXXXXcancellare.XXXCustomerly
import io.customerly.activity.fullscreen.startClyFullScreenImageActivity
import io.customerly.activity.startClyWebViewActivity
import io.customerly.entity.ClyMessage
import io.customerly.entity.urlImageAccount
import io.customerly.utils.download.imagehandler.ClyImageRequest
import io.customerly.utils.ggkext.activity
import io.customerly.utils.ggkext.dp2px
import io.customerly.utils.ggkext.inflater
import io.customerly.utils.ggkext.weak

/**
 * Created by Gianni on 24/04/18.
 * Project: Customerly-KAndroid-SDK
 */
private const val TYPING_DOTS_SPEED = 500L

internal sealed class ClyChatViewHolder (
        recyclerView: RecyclerView,
        @LayoutRes layoutRes: Int,
        @IntRange(from = 1, to = Long.MAX_VALUE) iconResId: Int = R.id.io_customerly__icon,
        @IntRange(from = 1, to = Long.MAX_VALUE) contentResId: Int = R.id.io_customerly__content)
    : RecyclerView.ViewHolder(recyclerView.activity!!.inflater().inflate(layoutRes, recyclerView, false)) {

    val icon: ImageView? = this.itemView.findViewById(iconResId)
    val content: TextView = this.itemView.findViewById<TextView>(contentResId).apply { this.movementMethod = LinkMovementMethod.getInstance() }

    protected val iconSize = recyclerView.resources.getDimensionPixelSize(R.dimen.io_customerly__chat_li_icon_size).also { iconsize ->
        this.itemView.layoutParams.also {
            it.height = iconsize
            it.width = iconsize
        }
    }

    //TODO usato?
    internal fun clearAnimation() {
        this.itemView.clearAnimation()
    }

    abstract fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean)

    class Typing(recyclerView: RecyclerView): ClyChatViewHolder(recyclerView = recyclerView, layoutRes = R.layout.io_customerly__li_bubble_account_typing) {
        init {
            this.content.weak().also { contentTv ->
                object : Runnable {
                    override fun run() {
                        contentTv.get()?.also { tv ->
                            tv.text = when(tv.text) {
                                ".    " -> ". .  "
                                ". .  " -> ". . ."
                                else /* "" or ". . ." */ -> ".    "
                            }
                            tv.requestLayout()
                            tv.postDelayed(this, TYPING_DOTS_SPEED)
                        }
                    }
                }
            }
        }
        override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
            val typingAccountID = chatActivity.typingAccountId
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
            ) : ClyChatViewHolder(recyclerView = recyclerView, layoutRes = layoutRes) {

        private val attachmentLayout: LinearLayout = this.itemView.findViewById(attachmentLayoutResId)
        private val sendingProgressBar: ProgressBar? = this.itemView.findViewById(sendingProgressBarResId)
        private val date: TextView = this.itemView.findViewById(dateResId)
        private val time: TextView = this.itemView.findViewById(timeResId)

        override fun apply(chatActivity: ClyChatActivity, message: ClyMessage?, dateToDisplay: String?, isFirstMessageOfSender: Boolean) {
            if(message == null) {
                //Always != null for this ViewHolder
                this.icon?.visibility = View.INVISIBLE
                this.content.apply {
                    text = null
                    setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                    setOnClickListener(null)
                }
                this.sendingProgressBar?.visibility = View.GONE
                this.itemView.setOnClickListener(null)
                this.attachmentLayout.apply {
                    removeAllViews()
                    visibility = View.GONE
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
                    ClyImageRequest(context = chatActivity, url = message.getImageUrl(sizePx = this.iconSize))
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
                    checkClyConfigured {
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

            this.attachmentLayout.removeAllViews()
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
                            ll.setBackgroundResource(if (message.isUserMessage) R.drawable.io_customerly__attachmentfile_border_user else R.drawable.io_customerly__attachmentfile_border_account)
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
                        setTextColor(ContextCompat.getColorStateList(chatActivity, if (message.isUserMessage) R.color.io_customerly__textcolor_white_grey else R.color.io_customerly__textcolor_malibu_grey))
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
                (this.itemView.findViewById<View>(R.id.bubble).background as? GradientDrawable)?.setColor(XXXCustomerly.get().__PING__LAST_widget_color)
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
    }
}