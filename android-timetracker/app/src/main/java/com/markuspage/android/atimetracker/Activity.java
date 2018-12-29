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

import static com.markuspage.android.atimetracker.TimeRange.NULL;

/**
 * Activity activity.
 *
 * @author Sean Russell, ser@germane-software.com
 */
public class Activity implements Comparable<Activity> {

    private String name;
    private final int id;
    private long startTime = NULL;
    private long endTime = NULL;
    private long collapsed;

    /**
     * Constructs a new instance of Activity.
     *
     * @param name The title of the activity. Must not be null.
     * @param id The ID of the activity. Must not be null
     */
    public Activity(String name, int id) {
        this.name = name;
        this.id = id;
        collapsed = 0;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getTotal() {
        long sum = 0;
        if (startTime != NULL && endTime == NULL) {
            sum += System.currentTimeMillis() - startTime;
        }
        return sum + collapsed;
    }

    public void setCollapsed(long collapsed) {
        this.collapsed = collapsed;
    }

    public long getCollapsed() {
        return collapsed;
    }

    public void start() {
        if (endTime != NULL || startTime == NULL) {
            startTime = System.currentTimeMillis();
            endTime = NULL;
        }
    }

    public void stop() {
        if (endTime == NULL) {
            endTime = System.currentTimeMillis();
            collapsed += endTime - startTime;
        }
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.id;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Activity other = (Activity) obj;
        return this.id == other.id;
    }

    @Override
    public int compareTo(Activity another) {
        return name.toUpperCase().compareTo(another.getName().toUpperCase());
    }

    public boolean isRunning() {
        return startTime != NULL && endTime == NULL;
    }
}