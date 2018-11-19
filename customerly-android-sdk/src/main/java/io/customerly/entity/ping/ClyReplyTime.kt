package io.customerly.entity.ping

import androidx.annotation.StringRes
import io.customerly.R

/**
 * Created by Gianni on 07/07/18.
 * Project: Customerly-KAndroid-SDK
 */
internal enum class ClyReplyTime(@StringRes val stringResId: Int) {
    MINUTES(stringResId = R.string.io_customerly__replytime_minutes),
    HOUR(stringResId = R.string.io_customerly__replytime_hour),
    DAY(stringResId = R.string.io_customerly__replytime_day);
}

internal val Int.toClyReplyTime: ClyReplyTime? get() = when(this) {
    0 -> ClyReplyTime.MINUTES
    1 -> ClyReplyTime.HOUR
    2 -> ClyReplyTime.DAY
    else -> null
}