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

import java.util.*

/**
 * Created by Gianni on 11/11/17.
 */

/**
 * Es: 5.sum2n() = 0+1+2+3+4+5 = 15
 */
internal fun Int.sum2n() = IntRange(0, this).sum()

internal fun Int.inside(minInclusive : Int, maxInclusive : Int) = Math.max(minInclusive, Math.min(maxInclusive, this))

internal fun getStartOfTodaySeconds()
        = Calendar.getInstance()
        .also {
            it.set(Calendar.MILLISECOND, 0)
            it.set(Calendar.SECOND, 0)
            it.set(Calendar.MINUTE, 0)
            it.set(Calendar.HOUR_OF_DAY, 0)
            it.set(Calendar.HOUR, 0)
        }.timeInMillis / 1000

val /* @STimestamp */Int.toHoursMins get() = (this / 1.hours_s).toInt() to ((this % 1.hours_s).sAsMinutes).toInt()

val /* @STimestamp */Long.toHoursMins get() = (this / 1.hours_s) to ((this % 1.hours_s).sAsMinutes)

internal val /* @STimestamp */Int.toHoursMinsString get() = this.toHoursMins.let { (hours, mins) -> String.format(Locale.ITALIAN, "%d:%02d", hours, mins) }