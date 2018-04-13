@file:Suppress("unused")

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

package io.customerly.utils.ggkext

import android.content.res.Resources

/**
 * Created by Gianni on 12/08/17
 */
internal val Int.dp2px: Int
    get() {
        var dpi = Resources.getSystem().displayMetrics.density
        dpi = if (dpi > 100/*120, 160, 213, 240, 320, 480 or 640 dpi*/) dpi / 160f else dpi
        dpi = if (dpi == 0f) 1f else dpi
        return (this * dpi).toInt()
    }

internal val Int.px2dp: Int
    get() {
        var dpi = Resources.getSystem().displayMetrics.density
        dpi = if (dpi > 100/*120, 160, 213, 240, 320, 480 or 640 dpi*/) dpi / 160f else dpi
        dpi = if (dpi == 0f) 1f else dpi
        return (this / dpi).toInt()
    }

internal val Float.dp2px: Float
    get() {
        var dpi = Resources.getSystem().displayMetrics.density
        dpi = if (dpi > 100/*120, 160, 213, 240, 320, 480 or 640 dpi*/) dpi / 160f else dpi
        dpi = if (dpi == 0f) 1f else dpi
        return this * dpi
    }

internal val Float.px2dp: Float
    get() {
        var dpi = Resources.getSystem().displayMetrics.density
        dpi = if (dpi > 100/*120, 160, 213, 240, 320, 480 or 640 dpi*/) dpi / 160f else dpi
        dpi = if (dpi == 0f) 1f else dpi
        return this / dpi
    }