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

import java.util.Iterator;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * A thread that will continually update the notification.
 */
public class TaskNotificationThread extends Thread{

    private static final long SLEEP_TIME = 1000; // 1 second
    private static final String TAG = null;
    private static final int notificationId = 001;
    private Tasks tasksActivity = null;
    private Tasks.TaskAdapter taskAdapter = null;
    private NotificationCompat.Builder notificationBuilder = null;
    private NotificationManager notificationManager = null;

    private static TaskNotificationThread threadInstance = null;

    private TaskNotificationThread(){
        super();
    }

    /**
     * Set the attributes for the thread.
     *
     * @param tasksActivity The TasksActivity that is currently running.
     * @param taskAdapter The TaskAdapter that is a part of TasksActivity,
     *      used for obtaining the running tasks.
     */
    public void init(Tasks tasksActivity, Tasks.TaskAdapter taskAdapter){
        this.tasksActivity = tasksActivity;
        this.taskAdapter = taskAdapter;
        Log.d(TAG, "Creating notification thread:");
        Log.d(TAG, "\tActivity: " + tasksActivity);
        Log.d(TAG, "\tAdapter: " + taskAdapter);

        // Do this once so that we don't get a stack overflow! ;-)
        this.notificationManager =  getNotificationManager();
    }

    /**
     * Initialize a notification builder.
     *
     */
    private synchronized void initNotification() {

        Log.d(TAG, "Initializing Notification...");

        this.notificationBuilder = 
            new NotificationCompat.Builder(this.tasksActivity)
            .setSmallIcon(R.drawable.icon)
            .setContentTitle("ATimeTracker")
            .setOngoing(true);

        Intent tasksIntent = new Intent(this.tasksActivity, Tasks.class);

        PendingIntent taskerPendingIntent = PendingIntent.getActivity(
                this.tasksActivity,
                0,
                tasksIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
                );

        this.notificationBuilder.setContentIntent(taskerPendingIntent);
    }

    /**
     * Convenience method to get the system's notification manager.
     **/
    private NotificationManager getNotificationManager(){
        Log.d(TAG, "Getting notification manager...");
        return (NotificationManager) this.tasksActivity.getSystemService(
                Context.NOTIFICATION_SERVICE); 
    }

    /**
     * Start the thread.
     */
    @Override
    public synchronized void start(){
        if (!this.isAlive()){
            super.start();
            Log.d(TAG, "START - TaskNotificationThread");
            this.initNotification();
        } else {
            Log.d(TAG, "Thread " + this + " already started.");
        }
    }

    /**
     * Get a list of the running tasks, get their name, and update the
     * notification with a list of their names.
     */
    public synchronized void setNotification(){
        Iterator<Task> active = null;
        Task t = null;
        String content = null, title = null;
        int nTasks = 0;

        StringBuilder titleBuilder = new StringBuilder();
        StringBuilder contentBuilder = new StringBuilder();

        active = this.taskAdapter.findCurrentlyActive();

        // If tasks are running then update the notification
        if (active.hasNext()) {
            Log.d(TAG, "Active Tasks:");

            while (active.hasNext()) {
                ++nTasks;
                t = active.next();
                Log.d(TAG, "\t- " + t.getTaskName());
                contentBuilder.append("\t- ")
                    .append(t.getTaskName())
                    .append("\n");
            }

            // Construct the content and title
            titleBuilder.append(String.valueOf(nTasks)).append(
                    " task");
            if (nTasks > 1)
                titleBuilder.append("s");
            titleBuilder.append(" running:");
            //contentBuilder.insert(0, titleBuilder);
            content = contentBuilder.toString();
            title = titleBuilder.toString();

            // Set the content and title
            this.notificationBuilder
                .setContentTitle(title)
                .setContentText(content);

            // TODO: Watch this, because sometimes we get a
            // StackOverflowError.
            this.notificationManager.notify(notificationId,
                    this.notificationBuilder.build());
        } else {
            // Otherwise, cancel the notification.
            Log.d(TAG, "No active tasks. Not setting notification.");
            this.notificationManager.cancel(notificationId);
        }

    }

    @Override
    public void run(){
        while (true){
            setNotification();

            Log.d(TAG, "Running " + this);

            // Finally, go to sleep and call run() again.
            try {
                Log.d(TAG, "Sleeping for " + SLEEP_TIME);
                Thread.sleep(TaskNotificationThread.SLEEP_TIME);
                run();
            } catch (InterruptedException e){
                Log.w(TAG, "Could not sleep.");
            }
        }
    }

    public static TaskNotificationThread getInstance(){
        if (threadInstance == null)
            threadInstance = new TaskNotificationThread();
        return threadInstance;
    }
}

