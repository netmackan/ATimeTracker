/*
 * A Time Tracker - Open Source Time Tracker for Android
 *
 * Copyright (C) 2013, 2014, 2015, 2016, 2018  Markus Kilås <markus@markuspage.com>
 * Copyright (C) 2008, 2009, 2010  Sean Russell <ser@germane-software.com>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.markuspage.android.atimetracker;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Representation of a time range.
 *
 * @author Sean Russell, ser@germane-software.com
 */
public class TimeRange implements Comparable<TimeRange> {

    public static final long NULL = -1;

    private static final int[] FIELDS = {
        Calendar.HOUR_OF_DAY,
        Calendar.MINUTE,
        Calendar.SECOND,
        Calendar.MILLISECOND
    };

    private long start;
    private long end;

    public TimeRange(long start, long end) {
        this.start = start;
        this.end = end;
    }

    public long getStart() {
        return start;
    }

    public void setStart(long start) {
        this.start = start;
    }

    public long getEnd() {
        return end;
    }

    public void setEnd(long end) {
        this.end = end;
    }

    public long getTotal() {
        if (end == NULL) {
            return System.currentTimeMillis() - start;
        }
        return end - start;
    }

    @Override
    public String toString() {
        Date s = new Date(start);
        final StringBuilder b = new StringBuilder(s.toString());
        b.append(" - ");
        if (end != NULL) {
            b.append(new Date(end));
        } else {
            b.append("...");
        }
        return b.toString();
    }

    /**
     * Formats this time range in textual form using the supplied date format.
     * @param format for the times
     * @return the textual representation
     */
    public String format(DateFormat format) {
        Date s = new Date(start);
        final StringBuilder b = new StringBuilder(format.format(s));
        b.append(" - ");
        if (end != NULL) {
            b.append(format.format(new Date(end)));
        } else {
            b.append("...");
        }
        return b.toString();
    }

    @Override
    public int compareTo(TimeRange another) {
        if (start < another.start) {
            return -1;
        } else if (start > another.start) {
            return 1;
        } else {
            if (end == NULL) {
                return 1;
            }
            if (another.end == NULL) {
                return -1;
            }
            if (end < another.end) {
                return -1;
            } else if (end > another.end) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TimeRange) {
            TimeRange t = (TimeRange) o;
            return t.start == start && t.end == end;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (int) (start + (end * 101));
    }

    /**
     * Finds the amount of time from a range that overlaps with a day
     *
     * @param day The day on which the time must overlap to be counted
     * @param start The range start
     * @param end The range end
     * @return The number of milliseconds of the range that overlaps with the
     * day
     */
    public static long overlap(Calendar day, long start, long end) {
        for (int x : FIELDS) {
            day.set(x, day.getMinimum(x));
        }
        long ms_start = day.getTime().getTime();
        day.add(Calendar.DAY_OF_MONTH, 1);
        long ms_end = day.getTime().getTime();

        if (ms_end < start || end < ms_start) {
            return 0;
        }

        long off_start = ms_start > start ? ms_start : start;
        long off_end = ms_end < end ? ms_end : end;
        long off_diff = off_end - off_start;
        return off_diff;
    }
}
