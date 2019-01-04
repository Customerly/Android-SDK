package io.customerly.entity.ping

import android.content.Context
import io.customerly.R
import io.customerly.utils.ggkext.STimestamp
import io.customerly.utils.ggkext.getTyped
import io.customerly.utils.ggkext.nullOnException
import io.customerly.utils.ggkext.optTyped
import org.json.JSONObject
import java.util.*

/**
 * Created by Gianni on 07/07/18.
 * Project: Customerly-KAndroid-SDK
 */

internal fun JSONObject.parseNextOfficeHours(): ClyNextOfficeHours? {
    return nullOnException {
        ClyNextOfficeHours(
                period = it.getTyped(name = "period"),
                startUtc = it.optTyped(name = "start_utc", fallback = 0),
                endUtc = it.optTyped(name = "end_utc", fallback = 0))
    }
}

internal data class ClyNextOfficeHours(
        val period: String,
        @STimestamp private val startUtc: Int,
        @STimestamp private val endUtc: Int) {

    @Suppress("unused")
    companion object {
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

        private const val PERIOD_A_SINGLE_DAY = "a_day"
    }

    /**
     * returns null if the office is open, returns this otherwise
     */
    internal fun isOfficeOpen(): Boolean = this.startUtc < (System.currentTimeMillis() / 1000)

    internal fun getBotMessage(context: Context): String? {
        if(this.isOfficeOpen()) {
            return null
        }

        val startLocalTz = Calendar.getInstance().also { it.timeInMillis = this.startUtc * 1000L }
        val endLocalTz = Calendar.getInstance().also { it.timeInMillis = this.endUtc * 1000L }
        val localDay = startLocalTz.get(Calendar.DAY_OF_WEEK)

        val periodLocalized = when(this.period) {
            ClyNextOfficeHours.PERIOD_WEEKENDS -> {
                when(localDay) {
                    Calendar.SATURDAY,Calendar.SUNDAY -> ClyNextOfficeHours.PERIOD_WEEKENDS
                    else -> ClyNextOfficeHours.PERIOD_A_SINGLE_DAY
                }
            }
            ClyNextOfficeHours.PERIOD_WEEKDAYS -> {
                when(localDay) {
                    Calendar.MONDAY,Calendar.TUESDAY,Calendar.WEDNESDAY,Calendar.THURSDAY,Calendar.FRIDAY -> ClyNextOfficeHours.PERIOD_WEEKDAYS
                    else -> ClyNextOfficeHours.PERIOD_A_SINGLE_DAY
                }
            }
            else -> ClyNextOfficeHours.PERIOD_A_SINGLE_DAY
        }

        val startTimeString: String = String.format(Locale.ITALIAN, "%d:%02d", startLocalTz.get(Calendar.HOUR_OF_DAY), startLocalTz.get(Calendar.MINUTE))
        val endTimeString: String = String.format(Locale.ITALIAN, "%d:%02d", endLocalTz.get(Calendar.HOUR_OF_DAY), endLocalTz.get(Calendar.MINUTE))

        return when(periodLocalized) {
            ClyNextOfficeHours.PERIOD_WEEKENDS -> {
                context.getString(R.string.io_customerly__outofoffice_weekends_fromx_toy, startTimeString, endTimeString)
            }
            ClyNextOfficeHours.PERIOD_WEEKDAYS -> {
                context.getString(R.string.io_customerly__outofoffice_weekdays_fromx_toy, startTimeString, endTimeString)
            }
            else -> {
                val todayLocalTz = Calendar.getInstance()
                if(startLocalTz.get(Calendar.YEAR) == todayLocalTz.get(Calendar.YEAR) && startLocalTz.get(Calendar.MONTH) == todayLocalTz.get(Calendar.MONTH) && startLocalTz.get(Calendar.DAY_OF_MONTH) == todayLocalTz.get(Calendar.DAY_OF_MONTH)) {
                    //Next opening = Today
                    context.getString(R.string.io_customerly__outofoffice_today_atx, startTimeString)
                } else {
                    val tomorrowLocalTz = todayLocalTz.apply { this.add(Calendar.DAY_OF_MONTH, 1) }
                    if(startLocalTz.get(Calendar.YEAR) == tomorrowLocalTz.get(Calendar.YEAR) && startLocalTz.get(Calendar.MONTH) == tomorrowLocalTz.get(Calendar.MONTH) && startLocalTz.get(Calendar.DAY_OF_MONTH) == tomorrowLocalTz.get(Calendar.DAY_OF_MONTH)) {
                        //Next opening = Tomorrow
                        context.getString(R.string.io_customerly__outofoffice_tomorrow_atx, startTimeString)
                    } else {
                        context.getString(when(localDay) {
                            Calendar.MONDAY -> R.string.io_customerly__outofoffice_monday_atx
                            Calendar.TUESDAY -> R.string.io_customerly__outofoffice_tuesday_atx
                            Calendar.WEDNESDAY -> R.string.io_customerly__outofoffice_wednesday_atx
                            Calendar.THURSDAY -> R.string.io_customerly__outofoffice_thursday_atx
                            Calendar.FRIDAY -> R.string.io_customerly__outofoffice_friday_atx
                            Calendar.SATURDAY -> R.string.io_customerly__outofoffice_saturday_atx
                            Calendar.SUNDAY -> R.string.io_customerly__outofoffice_sunday_atx
                            else -> R.string.io_customerly__outofoffice_tomorrow_atx
                        }, startTimeString)
                    }
                }
            }
        }
    }
}