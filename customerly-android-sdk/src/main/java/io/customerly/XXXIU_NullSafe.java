package io.customerly;

import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by Gianni on 23/01/17.
 * Project: CustomerlyAndroidSDK
 */
abstract class XXXIU_NullSafe {
    @IntDef({View.VISIBLE, View.INVISIBLE, View.GONE})
    @Retention(RetentionPolicy.SOURCE)
    @interface Visibility {}

    static void setVisibility(@Nullable View view, @Visibility int visibility) {
        if(view != null) {
            view.setVisibility(visibility);
        }
    }

    static void post(@Nullable View view, @NonNull Runnable action) {
        if(view != null) {
            view.post(action);
        }
    }
}
