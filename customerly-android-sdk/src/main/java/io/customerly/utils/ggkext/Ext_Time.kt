@file:Suppress("unused", "PropertyName", "ConstPropertyName")

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
 * Created by Gianni on 02/10/17.
 */

internal annotation class STimestamp
internal annotation class MSTimestamp

internal val Int.seconds_ms :Long   get() = this * 1000L
internal val Int.minutes_ms :Long   get() = this.seconds_ms * 60
internal val Int.hours_ms :Long     get() = this.minutes_ms * 60
internal val Int.days_ms :Long      get() = this.hours_ms * 24
internal val Int.weeks_ms :Long     get() = this.days_ms * 7
internal val Int.months30_ms :Long  get() = this.days_ms * 30
internal val Int.months31_ms :Long  get() = this.days_ms * 31

internal val Int.seconds_s :Long   get() = this.toLong()
internal val Int.minutes_s:Long    get() = this.seconds_s * 60
internal val Int.hours_s :Long     get() = this.minutes_s * 60
internal val Int.days_s :Long      get() = this.hours_s * 24
internal val Int.weeks_s :Long     get() = this.days_s * 7
internal val Int.months30_s :Long  get() = this.days_s * 30
internal val Int.months31_s :Long  get() = this.days_s * 31

internal val Long.asDate :Date get() = Date(this)
@STimestamp internal val Long.msAsSeconds :Long get() = this / 1000
@MSTimestamp internal val Long.secondsAsMs :Long get() = this * 1000

@STimestamp internal fun nowSeconds() = nowMs().msAsSeconds
internal fun nowMs() = System.currentTimeMillis()

private const val STEP = 1000L
private const val Q_1K = STEP
private const val Q_1M = Q_1K * STEP
private const val Q_1G = Q_1M * STEP
private const val Q_1T = Q_1G * STEP

private const val STEP_BYTE = 1024L
private const val Q_1KB = STEP_BYTE
private const val Q_1MB = Q_1KB * STEP_BYTE
private const val Q_1GB = Q_1MB * STEP_BYTE
private const val Q_1TB = Q_1GB * STEP_BYTE

internal fun Long.ktize() : String {
    return when(this) {
        in 0 until Q_1K -> this.toString()
        in Q_1K until Q_1M -> {
            val k = this / Q_1K
            if (this - k * Q_1K >= Q_1K * 0.5) "$k.5K" else "${k}K"
        }
        in Q_1M until Q_1G -> {
            val m = this / Q_1M
            if (this - m * Q_1M >= Q_1M * 0.5) "$m.5M" else "${m}M"
        }
        in Q_1G until Q_1T -> {
            val g = this / Q_1G
            if (this - g * Q_1G >= Q_1G * 0.5) "$g.5G" else "${g}G"
        }
        else -> {
            val t = this / Q_1T
            if (this - t * Q_1T >= Q_1T * 0.5) "$t.5T" else "${t}T"
        }
    }
}

internal fun Long.timizeSeconds() : String {
    return when(this) {
        in 0..59 -> String.format("%02ds", this)
        else -> {
            when {
                this >= 3600 -> String.format("%02d:%02d:%02d", this / 3600, (this / 60) % 60, this % 60)
                else -> String.format("%02d:%02d", (this / 60) % 60, this % 60)
            }
        }
    }
}

/**
 * Assert this Long is a past timestamp in seconds
 */
fun <RESULT> Long.formatByTimeAgo(
        seconds : ((Long)->RESULT)? = null,
        minutes : ((Long)->RESULT)? = null,
        hours : ((Long)->RESULT)? = null,
        days : ((Long)->RESULT)? = null,
        weeks : ((Long)->RESULT)? = null,
        months : ((Long)->RESULT)? = null,
        years : ((Long)->RESULT)? = null) : RESULT {

    assert( areAnyNotNull( seconds, minutes, hours, days, weeks, months, years ) )

    var timeAgo = Math.max(0, (System.currentTimeMillis() / 1000) - this)

    //Less than a minute ago or not up level
    if( (timeAgo < 60 && seconds != null) || areAllNull(minutes, hours, days, weeks, months, years) ) {
        return seconds!!(timeAgo)
    }

    timeAgo /= 60
    //Less than an hour ago or not up level
    if( (timeAgo < 60 && minutes != null) || areAllNull(hours, days, weeks, months, years) ) {
        return minutes!!(timeAgo)
    }

    timeAgo /= 60
    //Less than a day ago or not up level
    if( (timeAgo < 24 && hours != null) || areAllNull(days, weeks, months, years) ) {
        return hours!!(timeAgo)
    }

    timeAgo /= 24
    //Less than a week ago or not up level
    if( (timeAgo < 7 && days != null) || areAllNull(weeks, months, years) ) {
        return days!!(timeAgo)
    }

    //Less than a month ago or not up level
    if( (timeAgo < 31 && weeks != null) || areAllNull(months, years) ) {
        return weeks!!(timeAgo / 7)
    }

    //Less than a year ago or not up level
    if( (timeAgo < 365 && months != null) || areAllNull(years) ) {
        return months!!(timeAgo / 30)
    }

    //More than a year ago
    return years!!(timeAgo / 365)
}