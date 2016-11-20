package io.customerly;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;

/**
 * Created by Gianni on 18/05/16.
 * Project: CRMHero Android SDK
 */
@SuppressWarnings({"unused"})
class Internal_Utils__ScaleAnimationEvo {

    static void startScaleIn(@NonNull View view, long duration) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, null, 0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    }

    static void startScaleIn(@NonNull View view, long duration, @Nullable Runnable onAnimationEnd) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, onAnimationEnd, 0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    }

    static void startScaleIn(@NonNull View view, long duration, @Nullable Runnable onAnimationStart, @Nullable Runnable onAnimationRepeat, @Nullable Runnable onAnimationEnd) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, onAnimationStart, onAnimationRepeat, onAnimationEnd, 0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    }

    static void startScaleIn(@NonNull View view, long duration, float pivotXValue, float pivotYValue) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, null, 0, 1, 0, 1, Animation.RELATIVE_TO_SELF, pivotXValue, Animation.RELATIVE_TO_SELF, pivotYValue);
    }

    static void startScaleIn(@NonNull View view, long duration, @Nullable Runnable onAnimationEnd, float pivotXValue, float pivotYValue) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, onAnimationEnd, 0, 1, 0, 1, Animation.RELATIVE_TO_SELF, pivotXValue, Animation.RELATIVE_TO_SELF, pivotYValue);
    }

    static void startScaleOut(@NonNull View view, long duration) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, null, 1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    }

    static void startScaleOut(@NonNull View view, long duration, @Nullable Runnable onAnimationEnd) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, onAnimationEnd, 1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    }

    static void startScaleOut(@NonNull View view, long duration, @Nullable Runnable onAnimationStart, @Nullable Runnable onAnimationRepeat, @Nullable Runnable onAnimationEnd) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, onAnimationStart, onAnimationRepeat, onAnimationEnd, 1, 0, 1, 0, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    }

    static void startScaleOut(@NonNull View view, long duration, float pivotXValue, float pivotYValue) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, null, 1, 0, 1, 0, Animation.RELATIVE_TO_SELF, pivotXValue, Animation.RELATIVE_TO_SELF, pivotYValue);
    }

    static void startScaleOut(@NonNull View view, long duration, @Nullable Runnable onAnimationEnd, float pivotXValue, float pivotYValue) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, onAnimationEnd, 1, 0, 1, 0, Animation.RELATIVE_TO_SELF, pivotXValue, Animation.RELATIVE_TO_SELF, pivotYValue);
    }

    static void start(@NonNull View view, long duration, float fromX, float toX, float fromY, float toY, int pivotXType, float pivotXValue, int pivotYType, float pivotYValue) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, null, fromX, toX, fromY, toY, pivotXType, pivotXValue, pivotYType, pivotYValue);
    }

    static void start(@NonNull View view, long duration, float fromX, float toX, float fromY, float toY) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, null, fromX, toX, fromY, toY, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    }

    static void start(@NonNull View view, long duration, @Nullable Runnable onAnimationEnd, float fromX, float toX, float fromY, float toY) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, onAnimationEnd, fromX, toX, fromY, toY, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    }

    static void start(@NonNull View view, long duration, @Nullable Runnable onAnimationStart, @Nullable Runnable onAnimationRepeat, @Nullable Runnable onAnimationEnd, float fromX, float toX, float fromY, float toY) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, onAnimationStart, onAnimationRepeat, onAnimationEnd, fromX, toX, fromY, toY, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
    }

    static void start(@NonNull View view, long duration, @Nullable Runnable onAnimationEnd, float fromX, float toX, float fromY, float toY, int pivotXType, float pivotXValue, int pivotYType, float pivotYValue) {
        Internal_Utils__ScaleAnimationEvo.start(view, duration, null, null, onAnimationEnd, fromX, toX, fromY, toY, pivotXType, pivotXValue, pivotYType, pivotYValue);
    }

    static void start(@NonNull View view, long duration, @Nullable Runnable onAnimationStart, @Nullable Runnable onAnimationRepeat, @Nullable Runnable onAnimationEnd, float fromX, float toX, float fromY, float toY, int pivotXType, float pivotXValue, int pivotYType, float pivotYValue) {
        ScaleAnimation anim = new ScaleAnimation(fromX, toX, fromY, toY, pivotXType, pivotXValue, pivotYType, pivotYValue);
        anim.setDuration(duration);
        anim.setFillAfter(true);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {
                if(onAnimationStart != null)
                    onAnimationStart.run();
            }
            @Override public void onAnimationRepeat(Animation animation) {
                if(onAnimationRepeat != null)
                    onAnimationRepeat.run();
            }
            @Override public void onAnimationEnd(Animation animation) {
                if(onAnimationEnd != null)
                    onAnimationEnd.run();
            }
        });
        view.clearAnimation();
        view.startAnimation(anim);
    }
}
