/*
 * A Time Tracker - Open Source Time Tracker for Android
 *
 * Copyright (C) 2013  Markus Kilås <markus@markuspage.com>
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
/**
 * TimeTracker ©2008, 2009 Sean Russell
 *
 * @author Sean Russell <ser@germane-software.com>
 */
package com.markuspage.android.atimetracker;

import static com.markuspage.android.atimetracker.TimeRange.NULL;

public class Task implements Comparable<Task> {

    private String taskName;
    private int id;
    private long startTime = NULL;
    private long endTime = NULL;
    private long collapsed;

    /**
     * Constructs a new task.
     *
     * @param name The title of the task. Must not be null.
     * @param id The ID of the task. Must not be null
     */
    public Task(String name, int id) {
        taskName = name;
        this.id = id;
        collapsed = 0;
    }

    public int getId() {
        return id;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
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

    public boolean equals(Task other) {
        return other != null && other.getId() == id;
    }

    public int compareTo(Task another) {
        return taskName.toUpperCase().compareTo(another.getTaskName().toUpperCase());
    }

    public boolean isRunning() {
        return startTime != NULL && endTime == NULL;
    }
}