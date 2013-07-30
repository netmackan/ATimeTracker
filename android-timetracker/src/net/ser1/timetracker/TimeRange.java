/**
 * TimeTracker 
 * ©2008, 2009 Sean Russell
 * @author Sean Russell <ser@germane-software.com>
 */
package net.ser1.timetracker;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class TimeRange implements Comparable<TimeRange> {
    private long start;
    private long end;
    public static final long NULL = -1;
    
    public TimeRange( long start, long end ) {
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
        if (end == NULL) return System.currentTimeMillis() - start;
        return end - start;
    }
    
    protected static DateFormat FORMAT = new SimpleDateFormat("HH:mm");
    public String toString() {
        Date s = new Date(start);
        StringBuffer b = new StringBuffer(FORMAT.format(s));
        b.append(" - ");
        if (end != NULL) {
            b.append(FORMAT.format(new Date(end)));
        } else {
            b.append( "..." );
        }
        return b.toString();
    }
    
    public int compareTo(TimeRange another) {
        if (start < another.start) {
            return -1;
        } else if (start > another.start) {
            return 1;
        } else {
            if (end == NULL) return 1;
            if (another.end == NULL) return -1;
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
            TimeRange t = (TimeRange)o;
            return t.start == start && t.end == end;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (int)(start + (end * 101));
    }
    
    private static final int[] FIELDS = {
        Calendar.HOUR_OF_DAY,
        Calendar.MINUTE,
        Calendar.SECOND,
        Calendar.MILLISECOND
      };
    
    /**
     * Finds the amount of time from a range that overlaps with a day
     * @param day The day on which the time must overlap to be counted
     * @param start The range start
     * @param end The range end
     * @return The number of milliseconds of the range that overlaps with the 
     * day
     */
    public static long overlap( Calendar day, long start, long end ) {
        for (int x : FIELDS) day.set(x, day.getMinimum(x));
        long ms_start = day.getTime().getTime();
        day.add(Calendar.DAY_OF_MONTH, 1);
        long ms_end = day.getTime().getTime();
        
        if (ms_end < start || end < ms_start) return 0;
        
        long off_start = ms_start > start ? ms_start : start;
        long off_end   = ms_end < end ? ms_end : end;
        long off_diff  = off_end - off_start;
        return off_diff;
    }
}
