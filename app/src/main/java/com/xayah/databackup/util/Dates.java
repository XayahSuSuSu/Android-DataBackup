/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.xayah.databackup.util;

import android.content.Context;
import android.text.format.DateUtils;

import androidx.annotation.VisibleForTesting;

import com.xayah.databackup.App;
import com.xayah.databackup.R;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

/**
 * Collection of date utilities.
 */
public class Dates {
    public static final long SECOND_IN_MILLIS = 1000;
    public static final long MINUTE_IN_MILLIS = SECOND_IN_MILLIS * 60;
    public static final long HOUR_IN_MILLIS = MINUTE_IN_MILLIS * 60;
    public static final long DAY_IN_MILLIS = HOUR_IN_MILLIS * 24;
    public static final long WEEK_IN_MILLIS = DAY_IN_MILLIS * 7;

    // Flags to specify whether or not to use 12 or 24 hour mode.
    // Callers of methods in this class should never have to specify these; this is really
    // intended only for unit tests.
    @SuppressWarnings("deprecation")
    @VisibleForTesting
    public static final int FORCE_12_HOUR = DateUtils.FORMAT_12HOUR;
    @SuppressWarnings("deprecation")
    @VisibleForTesting
    public static final int FORCE_24_HOUR = DateUtils.FORMAT_24HOUR;

    /**
     * Private default constructor
     */
    private Dates() {
    }

    private static Context getContext() {
        return App.globalContext;
    }

    /**
     * Get the relative time as a string
     *
     * @param time The time
     * @return The relative time
     */
    public static CharSequence getRelativeTimeSpanString(final long time) {
        final long now = System.currentTimeMillis();
        if (now - time < DateUtils.MINUTE_IN_MILLIS) {
            // Also fixes bug where posts appear in the future
            return getContext().getResources().getText(R.string.posted_just_now);
        }

        // Workaround for b/5657035. The platform method {@link DateUtils#getRelativeTimeSpan()}
        // passes a null context to other platform methods. However, on some devices, this
        // context is dereferenced when it shouldn't be and an NPE is thrown. We catch that
        // here and use a slightly less precise time.
        try {
            return DateUtils.getRelativeTimeSpanString(time, now, DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE).toString();
        } catch (final NullPointerException npe) {
            return getShortRelativeTimeSpanString(time);
        }
    }

    public static CharSequence getConversationTimeString(final long time) {
        return getTimeString(time, true /*abbreviated*/, false /*minPeriodToday*/);
    }

    public static CharSequence getMessageTimeString(final long time) {
        return getTimeString(time, false /*abbreviated*/, false /*minPeriodToday*/);
    }

    public static CharSequence getWidgetTimeString(final long time, final boolean abbreviated) {
        return getTimeString(time, abbreviated, true /*minPeriodToday*/);
    }

    public static CharSequence getFastScrollPreviewTimeString(final long time) {
        return getTimeString(time, true /* abbreviated */, true /* minPeriodToday */);
    }

    public static CharSequence getMessageDetailsTimeString(final long time) {
        final Context context = getContext();
        int flags;
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            flags = FORCE_24_HOUR;
        } else {
            flags = FORCE_12_HOUR;
        }
        return getOlderThanAYearTimestamp(time,
                context.getResources().getConfiguration().locale, false /*abbreviated*/,
                flags);
    }

    private static CharSequence getTimeString(final long time, final boolean abbreviated,
                                              final boolean minPeriodToday) {
        final Context context = getContext();
        int flags;
        if (android.text.format.DateFormat.is24HourFormat(context)) {
            flags = FORCE_24_HOUR;
        } else {
            flags = FORCE_12_HOUR;
        }
        return getTimestamp(time, System.currentTimeMillis(), abbreviated,
                context.getResources().getConfiguration().locale, flags, minPeriodToday);
    }

    @VisibleForTesting
    public static CharSequence getTimestamp(final long time, final long now,
                                            final boolean abbreviated, final Locale locale, final int flags,
                                            final boolean minPeriodToday) {
        final long timeDiff = now - time;

        if (!minPeriodToday && timeDiff < DateUtils.MINUTE_IN_MILLIS) {
            return getLessThanAMinuteOldTimeString(abbreviated);
        } else if (!minPeriodToday && timeDiff < DateUtils.HOUR_IN_MILLIS) {
            return getLessThanAnHourOldTimeString(timeDiff, flags);
        } else if (getNumberOfDaysPassed(time, now) == 0) {
            return getTodayTimeStamp(time, flags);
        } else if (timeDiff < DateUtils.WEEK_IN_MILLIS) {
            return getThisWeekTimestamp(time, locale, abbreviated, flags);
        } else if (timeDiff < DateUtils.YEAR_IN_MILLIS) {
            return getThisYearTimestamp(time, locale, abbreviated, flags);
        } else {
            return getOlderThanAYearTimestamp(time, locale, abbreviated, flags);
        }
    }

    private static CharSequence getLessThanAMinuteOldTimeString(
            final boolean abbreviated) {
        return getContext().getResources().getText(
                abbreviated ? R.string.posted_just_now : R.string.posted_now);
    }

    private static CharSequence getLessThanAnHourOldTimeString(final long timeDiff,
                                                               final int flags) {
        final long count = (timeDiff / MINUTE_IN_MILLIS);
        final String format = getContext().getResources().getQuantityString(
                R.plurals.num_minutes_ago, (int) count);
        return String.format(format, count);
    }

    private static CharSequence getTodayTimeStamp(final long time, final int flags) {
        return DateUtils.formatDateTime(getContext(), time,
                DateUtils.FORMAT_SHOW_TIME | flags);
    }

    private static CharSequence getExplicitFormattedTime(final long time, final int flags,
                                                         final String format24, final String format12) {
        SimpleDateFormat formatter;
        if ((flags & FORCE_24_HOUR) == FORCE_24_HOUR) {
            formatter = new SimpleDateFormat(format24);
        } else {
            formatter = new SimpleDateFormat(format12);
        }
        return formatter.format(new Date(time));
    }

    private static CharSequence getThisWeekTimestamp(final long time,
                                                     final Locale locale, final boolean abbreviated, final int flags) {
        final Context context = getContext();
        if (abbreviated) {
            return DateUtils.formatDateTime(context, time,
                    DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY | flags);
        } else {
            if (locale.equals(Locale.US)) {
                return getExplicitFormattedTime(time, flags, "EEE HH:mm", "EEE h:mmaa");
            } else {
                return DateUtils.formatDateTime(context, time,
                        DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_SHOW_TIME
                                | DateUtils.FORMAT_ABBREV_WEEKDAY
                                | flags);
            }
        }
    }

    private static CharSequence getThisYearTimestamp(final long time, final Locale locale,
                                                     final boolean abbreviated, final int flags) {
        final Context context = getContext();
        if (abbreviated) {
            return DateUtils.formatDateTime(context, time,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_ABBREV_MONTH
                            | DateUtils.FORMAT_NO_YEAR | flags);
        } else {
            if (locale.equals(Locale.US)) {
                return getExplicitFormattedTime(time, flags, "MMM d, HH:mm", "MMM d, h:mmaa");
            } else {
                return DateUtils.formatDateTime(context, time,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
                                | DateUtils.FORMAT_ABBREV_MONTH
                                | DateUtils.FORMAT_NO_YEAR
                                | flags);
            }
        }
    }

    private static CharSequence getOlderThanAYearTimestamp(final long time,
                                                           final Locale locale, final boolean abbreviated, final int flags) {
        final Context context = getContext();
        if (abbreviated) {
            if (locale.equals(Locale.US)) {
                return getExplicitFormattedTime(time, flags, "M/d/yy", "M/d/yy");
            } else {
                return DateUtils.formatDateTime(context, time,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR
                                | DateUtils.FORMAT_NUMERIC_DATE);
            }
        } else {
            if (locale.equals(Locale.US)) {
                return getExplicitFormattedTime(time, flags, "M/d/yy, HH:mm", "M/d/yy, h:mmaa");
            } else {
                return DateUtils.formatDateTime(context, time,
                        DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME
                                | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR
                                | flags);
            }
        }
    }

    public static CharSequence getShortRelativeTimeSpanString(final long time) {
        final long now = System.currentTimeMillis();
        final long duration = Math.abs(now - time);

        int resId;
        long count;

        final Context context = getContext();

        if (duration < HOUR_IN_MILLIS) {
            count = duration / MINUTE_IN_MILLIS;
            resId = R.plurals.num_minutes_ago;
        } else if (duration < DAY_IN_MILLIS) {
            count = duration / HOUR_IN_MILLIS;
            resId = R.plurals.num_hours_ago;
        } else if (duration < WEEK_IN_MILLIS) {
            count = getNumberOfDaysPassed(time, now);
            resId = R.plurals.num_days_ago;
        } else {
            // Although we won't be showing a time, there is a bug on some devices that use
            // the passed in context. On these devices, passing in a {@code null} context
            // here will generate an NPE. See b/5657035.
            return DateUtils.formatDateRange(context, time, time,
                    DateUtils.FORMAT_ABBREV_MONTH | DateUtils.FORMAT_ABBREV_RELATIVE);
        }

        final String format = context.getResources().getQuantityString(resId, (int) count);
        return String.format(format, count);
    }

    private static long getNumberOfDaysPassed(final long date1, final long date2) {
        LocalDateTime dateTime1 = LocalDateTime.ofInstant(Instant.ofEpochMilli(date1),
                ZoneId.systemDefault());
        LocalDateTime dateTime2 = LocalDateTime.ofInstant(Instant.ofEpochMilli(date2),
                ZoneId.systemDefault());
        return Math.abs(ChronoUnit.DAYS.between(dateTime2, dateTime1));
    }
}