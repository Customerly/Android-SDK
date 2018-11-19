package io.customerly.alert

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

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Activity
import android.view.*
import android.view.animation.Animation
import android.view.animation.TranslateAnimation
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.annotation.UiThread
import io.customerly.Customerly
import io.customerly.R
import io.customerly.activity.chat.startClyChatActivity
import io.customerly.activity.startClyWebViewActivity
import io.customerly.entity.chat.ClyMessage
import io.customerly.utils.ClySemaphore
import io.customerly.utils.ggkext.activity
import io.customerly.utils.ggkext.dp2px
import io.customerly.utils.ggkext.weak
import io.customerly.utils.playNotifSound
import kotlinx.android.synthetic.main.io_customerly__alert_message.view.*

/**
 * Created by Gianni on 28/01/17.
 * Project: CustomerlyAndroidSDK
 */

private val DRAG_MIN_DISTANCE = 40.dp2px
private val SWIPE_MIN_DISTANCE = 100.dp2px
private const val AUTO_FADE_OUT_DELAY = 4000L
private const val FADE_OUT_DURATION = 2000
private const val ENTER_TRANSLATE_DURATION = 500L
private const val ABORT_CLICK_AFTER_MS = 700

private var currentClyAlertMessage : ClyAlertMessage? = null

internal fun Activity.dismissAlertMessageOnActivityDestroyed() {
    currentClyAlertMessage?.takeIf { it.activity === this }?.dismissAllowingStateLoss()
}

internal fun dismissAlertMessageOnUserLogout() {
    currentClyAlertMessage?.dismissAllowingStateLoss()
}

@UiThread
@Throws(WindowManager.BadTokenException::class)
internal fun Activity.showClyAlertMessage(message: ClyMessage) {
    if(true != currentClyAlertMessage?.onNewMessage(activity = this, newMessage = message)) {
        currentClyAlertMessage = null

        this.window.decorView.let { activityDecorView ->
            val clyAlertMessage = ClyAlertMessage(activity = this, message = message)
            val topOffsetFix = (activityDecorView as? ViewGroup)
                    ?.takeIf { it.childCount == 1 }
                    ?.getChildAt(0)
                    ?.paddingTop
                    ?: 0

            clyAlertMessage.showAtLocation(activityDecorView, Gravity.TOP or Gravity.CENTER_HORIZONTAL, 0, topOffsetFix)
            currentClyAlertMessage = clyAlertMessage
            clyAlertMessage.postFadeOut()
            this.playNotifSound()
        }
    }

    message.discard(context = this.applicationContext)
}

internal class ClyAlertMessage
    @SuppressLint("InflateParams")
    internal constructor(activity: Activity, message: ClyMessage)
        : PopupWindow(
            activity.layoutInflater.inflate(R.layout.io_customerly__alert_message, null),
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            false) {

    private var message: ClyMessage? = null
    private val fadeOutAfterTOT = Runnable { this.fadeOut() }
    private val fadingOut = ClySemaphore()

    init {
        this.bindMessage(message = message)
        val wAlertMessage = this.weak()
        this.contentView.setOnTouchListener(object : View.OnTouchListener {
            private var viewXStart: Float = 0f
            private var downRawXStart: Float = 0f
            private var dragging = false

            override fun onTouch(view: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        this.viewXStart = view.x
                        this.downRawXStart = event.rawX
                        wAlertMessage.get()?.abortFadeOut()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (this.dragging || Math.abs((event.rawX - this.downRawXStart).toInt()) > DRAG_MIN_DISTANCE) {
                            this.dragging = true
                            view.animate()
                                    .x(event.rawX + this.viewXStart - this.downRawXStart)
                                    .setDuration(0)
                                    .start()
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        this.dragging = false
                        val alert = wAlertMessage.get()
                        when {
                            Math.abs(event.rawX - this.downRawXStart) > SWIPE_MIN_DISTANCE -> {
                                alert?.dismissAllowingStateLoss()
                            }
                            alert?.message != null && event.eventTime - event.downTime < ABORT_CLICK_AFTER_MS -> {
                                view.performClick()
                                alert.dismissAllowingStateLoss()
                            }
                            else -> {
                                view.animate()
                                        .x(this.viewXStart)
                                        .setDuration(0)
                                        .start()
                                alert?.contentView?.postDelayed(alert.fadeOutAfterTOT, AUTO_FADE_OUT_DELAY)
                            }
                        }
                        return true
                    }
                }
                return false
            }
        })
        this.contentView.setOnClickListener {
            it.activity?.also { act ->
                wAlertMessage.get()?.message?.let { message ->
                    act.startClyChatActivity(mustShowBack = false, conversationId = message.conversationId)
                    message.richMailLink?.apply {
                        act.startClyWebViewActivity(targetUrl = this)
                    }
                }
            }
        }

        this.contentView.startAnimation(
                TranslateAnimation(
                    Animation.RELATIVE_TO_SELF, //fromXType
                    0.0f, //fromXValue
                    Animation.RELATIVE_TO_SELF, //toXType
                    0.0f, //toXValue
                    Animation.RELATIVE_TO_SELF, //fromYType
                    -1.0f, //fromYValue
                    Animation.RELATIVE_TO_SELF, //toYType
                    0.0f)//toYValue
                    .also { it.duration = ENTER_TRANSLATE_DURATION }
        )
    }

    private fun bindMessage(message: ClyMessage) {
        this.message = message
        val icon = this.contentView.findViewById<View>(R.id.io_customerly__icon) as ImageView

        message.writer.loadUrl(into = icon, sizePx = 50.dp2px)

        this.contentView.io_customerly__name.text = message.writer.getName(context = this.contentView.context)

        if (message.richMailLink == null) {
            this.contentView.io_customerly__content.text = message.contentAbstract
        } else {
            this.contentView.io_customerly__content.setText(
                    Customerly.currentUser.email?.let { R.string.io_customerly__rich_message_text__condensed_for_alert_via_email } ?: R.string.io_customerly__rich_message_text__condensed_for_alert_no_email)
        }
    }

    internal fun postFadeOut() {
        this.contentView.postDelayed(this.fadeOutAfterTOT, AUTO_FADE_OUT_DELAY)
    }

    private fun fadeOut() {
        this.fadingOut.on()
        this.contentView.removeCallbacks(this.fadeOutAfterTOT)
        this.contentView.clearAnimation()
        this.contentView.animate().alpha(0f).setDuration(FADE_OUT_DURATION.toLong()).setListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}
            override fun onAnimationCancel(animation: Animator) {}
            override fun onAnimationRepeat(animation: Animator) {}
            override fun onAnimationEnd(animation: Animator) {
                if (fadingOut.look()) {
                    dismissAllowingStateLoss()
                }
            }
        }).start()
    }

    private fun abortFadeOut() {
        this.contentView.removeCallbacks(this.fadeOutAfterTOT)
        if (this.fadingOut.off()) {
            this.contentView.animate().cancel()
            this.contentView.animate().alpha(1f).setDuration(0).start()
        }
    }

    internal fun dismissAllowingStateLoss() {
        try {
            this.dismiss()
        } catch (ignored: Exception) { }
    }

    override fun dismiss() {
        super.dismiss()
        if (currentClyAlertMessage === this) {
            currentClyAlertMessage = null
        }
    }

    /**
     * Intercept the message if it is already the current message and returns true
     * Otherwise dismiss this alert and return false
     */
    internal fun onNewMessage(activity: Activity, newMessage: ClyMessage) : Boolean {
        return when {
            activity !== this.activity -> {
                this.dismissAllowingStateLoss()
                false
            }
            this.message?.id == newMessage.id -> {
                //Already displaying that message
                this.bindMessage(message = newMessage)
                this.abortFadeOut()
                this.contentView.postDelayed(this.fadeOutAfterTOT, AUTO_FADE_OUT_DELAY)
                true
            }
            else/*activity === this.activity*/ -> {
                this.fadeOut()
                false
            }
        }
    }

}
