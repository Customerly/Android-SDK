package io.customerly;

/**
 * Created by Gianni on 11/09/16.
 * Project: CustomerlySDK
 */
@SuppressWarnings("unused")
class Internal_Utils__TimeAgoUtils {
    interface SecondsAgo<RETURN> {   RETURN onSeconds(long seconds); }
    interface MinutesAgo<RETURN> {   RETURN onMinutes(long minutes); }
    interface HoursAgo<RETURN>   {   RETURN onHours(long hours);     }
    interface DaysAgo<RETURN>    {   RETURN onDays(long days);       }
    interface WeeksAgo<RETURN>   {   RETURN onWeeks(long weeks);     }
    interface MonthsAgo<RETURN>  {   RETURN onMonths(long months);   }
    interface YearsAgo<RETURN>   {   RETURN onYears(long years);     }

    private static <RETURN> RETURN calculate(long timestamp_in_seconds,
                                             SecondsAgo<RETURN> onSecondsAgo,
                                             MinutesAgo<RETURN> onMinutesAgo,
                                             HoursAgo<RETURN> onHoursAgo,
                                             DaysAgo<RETURN> onDaysAgo,
                                             WeeksAgo<RETURN> onWeeksAgo,
                                             MonthsAgo<RETURN> onMonthsAgo,
                                             YearsAgo<RETURN> onYearsAgo) {

        timestamp_in_seconds = Math.max(0, (System.currentTimeMillis() / 1000) - timestamp_in_seconds);
        if(onSecondsAgo == null && onMinutesAgo == null && onHoursAgo == null && onDaysAgo == null && onWeeksAgo == null && onMonthsAgo == null && onYearsAgo == null) {
            throw new IllegalArgumentException("You must provide at least one handler");
        } else if(timestamp_in_seconds < 60 && onSecondsAgo != null || (onMinutesAgo == null && onHoursAgo == null && onDaysAgo == null && onWeeksAgo == null && onMonthsAgo == null && onYearsAgo == null)) {
            //Less than 1 minute ago or no up level
            return onSecondsAgo.onSeconds(timestamp_in_seconds);
        } else if((timestamp_in_seconds/=60) < 60 && onMinutesAgo != null || (onHoursAgo == null && onDaysAgo == null && onWeeksAgo == null && onMonthsAgo == null && onYearsAgo == null)) {
            //Less than 1 hour ago or no up level
            return onMinutesAgo.onMinutes(timestamp_in_seconds);
        } else if((timestamp_in_seconds/=60) < 24 && onHoursAgo != null || (onDaysAgo == null && onWeeksAgo == null && onMonthsAgo == null && onYearsAgo == null)) {
            //Less than 1 day ago or no up level
            return onHoursAgo.onHours(timestamp_in_seconds);
        } else if((timestamp_in_seconds/=24) < 7 && onDaysAgo != null || (onWeeksAgo == null && onMonthsAgo == null && onYearsAgo == null)) {
            //Less than 1 week ago or no up level
            return onDaysAgo.onDays(timestamp_in_seconds);
        } else if((timestamp_in_seconds) < 31 && onWeeksAgo != null || (onMonthsAgo == null && onYearsAgo == null)) {
            //Less than 1 month ago or no up level
            return onWeeksAgo.onWeeks(timestamp_in_seconds / 7);
        } else if((timestamp_in_seconds) < 365 && onMonthsAgo != null || onYearsAgo == null) {
            //Less than 1 year ago or no up level
            return onMonthsAgo.onMonths(timestamp_in_seconds / 30);
        } else {
            //More than 1 year ago
            return onYearsAgo.onYears(timestamp_in_seconds / 365);
        }
    }

    public static <RETURN> RETURN calculate(long timestampSeconds, SecondsAgo<RETURN> onSecondsAgo, MinutesAgo<RETURN> onMinutesAgo, HoursAgo<RETURN> onHoursAgo,
                                            DaysAgo<RETURN> onDaysAgo, WeeksAgo<RETURN> onWeeksAgo, MonthsAgo<RETURN> onMonthsAgo) {
        return Internal_Utils__TimeAgoUtils.calculate(timestampSeconds, onSecondsAgo, onMinutesAgo, onHoursAgo, onDaysAgo, onWeeksAgo, onMonthsAgo, null);
    }
    public static <RETURN> RETURN calculate(long timestampSeconds, SecondsAgo<RETURN> onSecondsAgo, MinutesAgo<RETURN> onMinutesAgo, HoursAgo<RETURN> onHoursAgo,
                                            DaysAgo<RETURN> onDaysAgo, MonthsAgo<RETURN> onMonthsAgo) {
        return Internal_Utils__TimeAgoUtils.calculate(timestampSeconds, onSecondsAgo, onMinutesAgo, onHoursAgo, onDaysAgo, null, onMonthsAgo, null);
    }
    public static <RETURN> RETURN calculate(long timestampSeconds, SecondsAgo<RETURN> onSecondsAgo, MinutesAgo<RETURN> onMinutesAgo, HoursAgo<RETURN> onHoursAgo,
                                            DaysAgo<RETURN> onDaysAgo, WeeksAgo<RETURN> onWeeksAgo) {
        return Internal_Utils__TimeAgoUtils.calculate(timestampSeconds, onSecondsAgo, onMinutesAgo, onHoursAgo, onDaysAgo, onWeeksAgo, null, null);
    }
    static <RETURN> RETURN calculate(long timestampSeconds, SecondsAgo<RETURN> onSecondsAgo, MinutesAgo<RETURN> onMinutesAgo, HoursAgo<RETURN> onHoursAgo,
                                            DaysAgo<RETURN> onDaysAgo) {
        return Internal_Utils__TimeAgoUtils.calculate(timestampSeconds, onSecondsAgo, onMinutesAgo, onHoursAgo, onDaysAgo, null, null, null);
    }
    public static <RETURN> RETURN calculate(long timestampSeconds, SecondsAgo<RETURN> onSecondsAgo, MinutesAgo<RETURN> onMinutesAgo, HoursAgo<RETURN> onHoursAgo) {
        return Internal_Utils__TimeAgoUtils.calculate(timestampSeconds, onSecondsAgo, onMinutesAgo, onHoursAgo, null, null, null, null);
    }
}
