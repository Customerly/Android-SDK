package io.customerly.entity.ping

import android.content.Context
import io.customerly.R
import io.customerly.utils.ggkext.*
import org.json.JSONObject
import java.util.*

/**
 * Created by Gianni on 07/07/18.
 * Project: Customerly-KAndroid-SDK
 */

@STimestamp private var offsetTimeZone: Int = Calendar.getInstance().let { it.timeZone.getOffset(it.timeInMillis) / 1000 }

internal fun JSONObject.parseOfficeHours(): ClyOfficeHours? {
    return nullOnException {
        ClyOfficeHours(
                period = it.getTyped(name = "period"),
                startTime = it.optTyped(name = "start_time", fallback = 0),
                endTime = it.optTyped(name = "end_time", fallback = 0))
    }
}

private const val PERIOD_EVERYDAY = "everyday"
private const val PERIOD_WEEKENDS = "weekends"
private const val PERIOD_WEEKDAYS = "weekdays"
private const val PERIOD_MONDAY = "monday"
private const val PERIOD_TUESDAY = "tuesday"
private const val PERIOD_WEDNESDAY = "wednesday"
private const val PERIOD_THURSDAY = "thursday"
private const val PERIOD_FRIDAY = "friday"
private const val PERIOD_SATURDAY = "saturday"
private const val PERIOD_SUNDAY = "sunday"

internal data class ClyOfficeHours(
        val period: String,
        @STimestamp private val startTime: Int,
        @STimestamp private val endTime: Int) {

    private val startTimeString: String = (this.startTime + offsetTimeZone).toHoursMinsString

    private fun isToday(now: Calendar = Calendar.getInstance()): Boolean {
        return if(this.period == PERIOD_EVERYDAY) {
            true
        } else {
            when(now.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> this.period == PERIOD_WEEKDAYS || this.period == PERIOD_MONDAY
                Calendar.TUESDAY -> this.period == PERIOD_WEEKDAYS || this.period == PERIOD_TUESDAY
                Calendar.WEDNESDAY -> this.period == PERIOD_WEEKDAYS || this.period == PERIOD_WEDNESDAY
                Calendar.THURSDAY -> this.period == PERIOD_WEEKDAYS || this.period == PERIOD_THURSDAY
                Calendar.FRIDAY -> this.period == PERIOD_WEEKDAYS || this.period == PERIOD_FRIDAY
                Calendar.SATURDAY -> this.period == PERIOD_WEEKENDS || this.period == PERIOD_SATURDAY
                Calendar.SUNDAY -> this.period == PERIOD_WEEKENDS || this.period == PERIOD_SUNDAY
                else -> false
            }
        }
    }

    private fun isNowIn(now: Calendar = Calendar.getInstance()): Boolean {
        return if(this.isToday(now = now)) {
            @STimestamp val nowTimeSinceStartDay = (now.timeInMillis / 1000) % 1.days_s
            nowTimeSinceStartDay >= this.startTime && nowTimeSinceStartDay <= this.endTime
        } else {
            false
        }
    }

    internal fun getNearestFactor(now: Calendar = Calendar.getInstance()): Long {
        return if(this.isNowIn(now = now)) {
            0
        } else {
            @STimestamp val nowTimeSinceStartDay = (now.timeInMillis / 1000) % 1.days_s
            var dayOfWeek: Int = now.get(Calendar.DAY_OF_WEEK)
            var offset = 0
            if(nowTimeSinceStartDay > this.startTime) {
                dayOfWeek++
                offset++
            }

            offset + when(dayOfWeek) {
                Calendar.MONDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKDAYS, PERIOD_MONDAY -> 0
                        PERIOD_TUESDAY -> 1
                        PERIOD_WEDNESDAY -> 2
                        PERIOD_THURSDAY -> 3
                        PERIOD_FRIDAY -> 4
                        PERIOD_WEEKENDS, PERIOD_SATURDAY -> 5
                        PERIOD_SUNDAY -> 6
                        else -> 99999
                    } + this.startTime - nowTimeSinceStartDay
                }
                Calendar.TUESDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKDAYS, PERIOD_TUESDAY -> 0
                        PERIOD_WEDNESDAY -> 1
                        PERIOD_THURSDAY -> 2
                        PERIOD_FRIDAY -> 3
                        PERIOD_WEEKENDS, PERIOD_SATURDAY -> 4
                        PERIOD_SUNDAY -> 5
                        PERIOD_MONDAY -> 6
                        else -> 99999
                    } + this.startTime - nowTimeSinceStartDay
                }
                Calendar.WEDNESDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKDAYS, PERIOD_WEDNESDAY -> 0
                        PERIOD_THURSDAY -> 1
                        PERIOD_FRIDAY -> 2
                        PERIOD_WEEKENDS, PERIOD_SATURDAY -> 3
                        PERIOD_SUNDAY -> 4
                        PERIOD_MONDAY -> 5
                        PERIOD_TUESDAY -> 6
                        else -> 99999
                    } + this.startTime - nowTimeSinceStartDay
                }
                Calendar.THURSDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKDAYS, PERIOD_THURSDAY -> 0
                        PERIOD_FRIDAY -> 1
                        PERIOD_WEEKENDS, PERIOD_SATURDAY -> 2
                        PERIOD_SUNDAY -> 3
                        PERIOD_MONDAY -> 4
                        PERIOD_TUESDAY -> 5
                        PERIOD_WEDNESDAY -> 6
                        else -> 99999
                    } + this.startTime - nowTimeSinceStartDay
                }
                Calendar.FRIDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKDAYS, PERIOD_FRIDAY -> 0
                        PERIOD_WEEKENDS, PERIOD_SATURDAY -> 1
                        PERIOD_SUNDAY -> 2
                        PERIOD_MONDAY -> 3
                        PERIOD_TUESDAY -> 4
                        PERIOD_WEDNESDAY -> 5
                        PERIOD_THURSDAY -> 6
                        else -> 99999
                    } + this.startTime - nowTimeSinceStartDay
                }
                Calendar.SATURDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKENDS, PERIOD_SATURDAY -> 0
                        PERIOD_SUNDAY -> 1
                        PERIOD_WEEKDAYS, PERIOD_MONDAY -> 2
                        PERIOD_TUESDAY -> 3
                        PERIOD_WEDNESDAY -> 4
                        PERIOD_THURSDAY -> 5
                        PERIOD_FRIDAY -> 6
                        else -> 99999
                    } + this.startTime - nowTimeSinceStartDay
                }
                Calendar.SUNDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKENDS, PERIOD_SUNDAY -> 0
                        PERIOD_WEEKDAYS, PERIOD_MONDAY -> 1
                        PERIOD_TUESDAY -> 2
                        PERIOD_WEDNESDAY -> 3
                        PERIOD_THURSDAY -> 4
                        PERIOD_FRIDAY -> 5
                        PERIOD_SATURDAY -> 6
                        else -> 99999
                    } + this.startTime - nowTimeSinceStartDay
                }
                else -> 99999
            }
        }
    }

    internal fun getBotMessage(context: Context, now: Calendar = Calendar.getInstance()): String? {
        return if(this.isNowIn(now = now)) {
            null
        } else {
            @STimestamp val nowTimeSinceStartDay = (now.timeInMillis / 1000) % 1.days_s
            var dayOfWeek: Int = now.get(Calendar.DAY_OF_WEEK)
            var offset = 0
            if(nowTimeSinceStartDay > this.startTime) {
                dayOfWeek++
                offset++
            }

            when(dayOfWeek) {
                Calendar.MONDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKDAYS, PERIOD_MONDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_today_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_TUESDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_WEEKENDS -> {
                            context.getString(R.string.io_customerly__outofoffice_nextweekend_atx, this.startTimeString)
                        }
                        PERIOD_WEDNESDAY, PERIOD_THURSDAY, PERIOD_FRIDAY, PERIOD_SATURDAY, PERIOD_SUNDAY -> {
                            context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                        }
                        else -> null
                    }
                }
                Calendar.TUESDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKDAYS, PERIOD_TUESDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_today_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_WEDNESDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_WEEKENDS -> {
                            context.getString(R.string.io_customerly__outofoffice_nextweekend_atx, this.startTimeString)
                        }
                        PERIOD_THURSDAY, PERIOD_FRIDAY, PERIOD_SATURDAY, PERIOD_SUNDAY, PERIOD_MONDAY -> {
                            context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                        }
                        else -> null
                    }
                }
                Calendar.WEDNESDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKDAYS, PERIOD_WEDNESDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_today_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_THURSDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_WEEKENDS -> {
                            context.getString(R.string.io_customerly__outofoffice_nextweekend_atx, this.startTimeString)
                        }
                        PERIOD_FRIDAY, PERIOD_SATURDAY, PERIOD_SUNDAY, PERIOD_MONDAY, PERIOD_TUESDAY -> {
                            context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                        }
                        else -> null
                    }
                }
                Calendar.THURSDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKDAYS, PERIOD_THURSDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_today_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_FRIDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_WEEKENDS -> {
                            context.getString(R.string.io_customerly__outofoffice_nextweekend_atx, this.startTimeString)
                        }
                        PERIOD_SATURDAY, PERIOD_SUNDAY, PERIOD_MONDAY, PERIOD_TUESDAY, PERIOD_WEDNESDAY -> {
                            context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                        }
                        else -> null
                    }
                }
                Calendar.FRIDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKDAYS, PERIOD_FRIDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_today_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_SATURDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_WEEKENDS -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_nextweekend_atx, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_SUNDAY, PERIOD_MONDAY, PERIOD_TUESDAY, PERIOD_WEDNESDAY, PERIOD_THURSDAY -> {
                            context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                        }
                        else -> null
                    }
                }
                Calendar.SATURDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKENDS, PERIOD_SATURDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_today_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_SUNDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_WEEKDAYS -> {
                            context.getString(R.string.io_customerly__outofoffice_nextweekday_atx, this.startTimeString)
                        }
                        PERIOD_MONDAY, PERIOD_TUESDAY, PERIOD_WEDNESDAY, PERIOD_THURSDAY, PERIOD_FRIDAY -> {
                            context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                        }
                        else -> null
                    }
                }
                Calendar.SUNDAY -> {
                    when(this.period) {
                        PERIOD_EVERYDAY, PERIOD_WEEKENDS, PERIOD_SUNDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_today_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_MONDAY -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_WEEKDAYS -> {
                            when(offset) {
                                1 -> context.getString(R.string.io_customerly__outofoffice_nextweekday_atx, this.startTimeString)
                                0 -> context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, this.startTimeString)
                                else -> null
                            }
                        }
                        PERIOD_TUESDAY, PERIOD_WEDNESDAY, PERIOD_THURSDAY, PERIOD_FRIDAY, PERIOD_SATURDAY -> {
                            context.getString(R.string.io_customerly__outofoffice_dayx_atx, this.period, this.startTimeString)
                        }
                        else -> null
                    }
                }
                else -> null
            }
        }
    }
}