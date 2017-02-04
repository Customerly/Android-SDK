package io.customerly;

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

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

/**
 * Created by Gianni on 28/01/17.
 * Project: CustomerlyAndroidSDK
 */
class PW_AlertMessage extends PopupWindow {

    private static final int
            DRAG_MIN_DISTANCE = IU_Utils.px(40),
            SWIPE_MIN_DISTANCE = IU_Utils.px(100),
            AUTO_FADE_OUT_DELAY = 4000,
            FADE_OUT_DURATION = 2000,
            ENTER_TRANSLATE_DURATION = 500,
            ABORT_CLICK_AFTER_MS = 700;

    @Nullable private static PW_AlertMessage _CurrentVisible = null;

    private long _ConversationID = 0, _MessageID = 0;

    private final Runnable _FadeOutAfterTOT = this::fadeOut;
    private boolean _FadingOut = false;
    @Nullable private String _MessageRawLink;

    @SuppressLint("InflateParams")
    private PW_AlertMessage(@NonNull Activity activity) {
        super(activity.getLayoutInflater().inflate(R.layout.io_customerly__alert_message, null), WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, false);
        this.getContentView().setOnTouchListener(new View.OnTouchListener() {
            private float _ViewXStart;
            private float _DownRawXStart;
            private boolean _Dragging = false;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        this._ViewXStart = view.getX();
                        this._DownRawXStart = event.getRawX();
                        PW_AlertMessage.this.abortFadeOut();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (this._Dragging || Math.abs((int) (event.getRawX() - this._DownRawXStart)) > DRAG_MIN_DISTANCE) {
                            this._Dragging = true;
                            view.animate()
                                    .x(event.getRawX() + this._ViewXStart - this._DownRawXStart)
                                    .setDuration(0)
                                    .start();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        this._Dragging = false;
                        if (Math.abs(event.getRawX() - this._DownRawXStart) > SWIPE_MIN_DISTANCE) {
                            PW_AlertMessage.this.dismissAllowingStateLoss();
                        } else if(PW_AlertMessage.this._ConversationID != 0 && event.getEventTime() - event.getDownTime() < ABORT_CLICK_AFTER_MS) {
                            IAct_Chat.start(activity, false, PW_AlertMessage.this._ConversationID);
                            if(PW_AlertMessage.this._MessageRawLink != null) {
                                IU_Utils.intentUrl(activity, PW_AlertMessage.this._MessageRawLink);
                            }
                            PW_AlertMessage.this.dismissAllowingStateLoss();
                        } else {
                            view.animate()
                                    .x(this._ViewXStart)
                                    .setDuration(0)
                                    .start();
                            PW_AlertMessage.this.getContentView().postDelayed(PW_AlertMessage.this._FadeOutAfterTOT, AUTO_FADE_OUT_DELAY);
                        }
                        return true;
                }
                return false;
            }
        });

        TranslateAnimation anim = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, //fromXType
                0.0f,                       //fromXValue
                Animation.RELATIVE_TO_SELF, //toXType
                0.0f,                       //toXValue
                Animation.RELATIVE_TO_SELF, //fromYType
                -1.0f,                      //fromYValue
                Animation.RELATIVE_TO_SELF, //toYType
                0.0f);                      //toYValue
        anim.setDuration(ENTER_TRANSLATE_DURATION);
        this.getContentView().startAnimation(anim);
    }

    @UiThread
    static void show(@NonNull Activity activity, @NonNull IE_Message message) {
        PW_AlertMessage alert = PW_AlertMessage._CurrentVisible;
        if (alert != null) {
            if(alert._MessageID == message.conversation_message_id) { //Already displaying that message
                alert.bindMessage(message);
                alert.abortFadeOut();
                alert.getContentView().postDelayed(alert._FadeOutAfterTOT, AUTO_FADE_OUT_DELAY);
                return;
            }
            if(activity == alert.getActivity()) {
                alert.fadeOut();
            } else {
                alert.dismissAllowingStateLoss();
            }
        }
        alert = new PW_AlertMessage(activity);
        alert.bindMessage(message);
        int top_offset_fix = 0;
        View decorView = activity.getWindow().getDecorView();
        if (decorView instanceof ViewGroup) {
            ViewGroup decorViewGroup = (ViewGroup) decorView;
            if (decorViewGroup.getChildCount() == 1) {
                top_offset_fix = decorViewGroup.getChildAt(0).getPaddingTop();
            }
        }
        alert.showAtLocation(activity.getWindow().getDecorView(), Gravity.TOP | Gravity.CENTER_HORIZONTAL, 0, top_offset_fix);
        PW_AlertMessage._CurrentVisible = alert;
        alert.getContentView().postDelayed(alert._FadeOutAfterTOT, AUTO_FADE_OUT_DELAY);
        MediaPlayer mp = MediaPlayer.create(activity, R.raw.notif_2);
        mp.setOnCompletionListener(mp1 -> {
            mp1.reset();
            mp1.release();
        });
        mp.start();
    }

    private void bindMessage(@NonNull IE_Message message) {
        this._ConversationID = message.conversation_id;
        this._MessageID = message.conversation_message_id;
        this._MessageRawLink = message.rich_mail_link;
        int _50dp = IU_Utils.px(50);
        Customerly._Instance._RemoteImageHandler.request(new IU_RemoteImageHandler.Request()
                .fitCenter()
                .transformCircle()
                .load(message.getImageUrl(_50dp))
                .into((ImageView)this.getContentView().findViewById(R.id.io_customerly__icon))
                .override(_50dp, _50dp)
                .placeholder(R.drawable.io_customerly__ic_default_admin));

        ((TextView)this.getContentView().findViewById(R.id.io_customerly__name))
                .setText(message.if_account__name != null ? message.if_account__name : this.getContentView().getResources().getString(R.string.io_customerly__support));


        if(message.rich_mail_link == null) {
            ((TextView) this.getContentView().findViewById(R.id.io_customerly__content))
                    .setText(message.content);
        } else {
            ((TextView) this.getContentView().findViewById(R.id.io_customerly__content))
                    .setText(R.string.io_customerly__rich_message_text__condensed_for_alert);
        }
    }

    private void fadeOut() {
        this._FadingOut = true;
        PW_AlertMessage.this.getContentView().removeCallbacks(PW_AlertMessage.this._FadeOutAfterTOT);
        this.getContentView().clearAnimation();
        this.getContentView().animate().alpha(0).setDuration(FADE_OUT_DURATION).setListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) { }
            @Override public void onAnimationCancel(Animator animation) { }
            @Override public void onAnimationRepeat(Animator animation) { }
            @Override public void onAnimationEnd(Animator animation) {
                if(PW_AlertMessage.this._FadingOut) {
                    dismissAllowingStateLoss();
                }
            }
        }).start();
    }

    private void abortFadeOut() {
        PW_AlertMessage.this.getContentView().removeCallbacks(PW_AlertMessage.this._FadeOutAfterTOT);
        if(PW_AlertMessage.this._FadingOut) {
            PW_AlertMessage.this._FadingOut = false;
            PW_AlertMessage.this.getContentView().animate().cancel();
            PW_AlertMessage.this.getContentView().animate().alpha(1).setDuration(0).start();
        }
    }

    private void dismissAllowingStateLoss() {
        try {
            this.dismiss();
        } catch (IllegalStateException ignored) { }
    }

    @Override
    public void dismiss() {
        super.dismiss();
        if(PW_AlertMessage._CurrentVisible == this) {
            PW_AlertMessage._CurrentVisible = null;
        }
    }

    static void onActivityDestroyed(@NonNull Activity activity) {
        PW_AlertMessage alert = PW_AlertMessage._CurrentVisible;
        if(alert != null && activity == alert.getActivity()) {
            alert.dismissAllowingStateLoss();
        }
    }

    static void onUserLogout() {
        PW_AlertMessage alert = PW_AlertMessage._CurrentVisible;
        if(alert != null) {
            alert.dismissAllowingStateLoss();
        }
    }

    @Nullable private Activity getActivity() {
        Context context = this.getContentView().getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity)context;
            }
            context = ((ContextWrapper)context).getBaseContext();
        }
        return null;
    }
}
