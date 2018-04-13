package io.customerly.utils

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

import android.graphics.Color
import android.support.annotation.ColorInt
import android.support.annotation.FloatRange

/**
 * Created by Gianni on 04/04/18.
 * Project: Customerly-KAndroid-SDK
 */
@ColorInt
fun @receiver:ColorInt Int.getContrastBW(): Int {
    return if (this == 0 || 0.299 * Color.red(this) + (0.587 * Color.green(this) + 0.114 * Color.blue(this)) > 186) {
        Color.BLACK
    } else {
        Color.WHITE
    }
}


@ColorInt
fun @receiver:ColorInt Int.alterColor(@FloatRange(from = 0.0, to = 255.0) factor: Float): Int {
    return Color.argb(Color.alpha(this),
            Math.min(255f, Color.red(this) * factor).toInt(),
            Math.min(255f, Color.green(this) * factor).toInt(),
            Math.min(255f, Color.blue(this) * factor).toInt())
}