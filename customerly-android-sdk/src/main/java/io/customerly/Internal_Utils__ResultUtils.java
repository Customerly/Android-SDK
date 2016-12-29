package io.customerly;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.List;

/**
 * Created by Gianni on 25/06/15.
 * Project: GGlib
 */
@SuppressWarnings("unused")
class Internal_Utils__ResultUtils {
    interface OnNoResult {
        void onResult();
    }
    interface OnResult<D> {
        void onResult(@Nullable D result);
    }
    interface OnResultList<D> {
        void onResult(@Nullable List<D> result);
    }
    interface OnNonNullResult<D> {
        void onResult(@NonNull D result);
    }
}
