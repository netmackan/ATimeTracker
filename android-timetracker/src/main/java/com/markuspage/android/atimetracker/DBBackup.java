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
package com.markuspage.android.atimetracker;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import java.util.ArrayList;
import java.util.List;
import static com.markuspage.android.atimetracker.DBHelper.TASK_TABLE;
import static com.markuspage.android.atimetracker.DBHelper.TASK_COLUMNS;
import static com.markuspage.android.atimetracker.DBHelper.RANGES_TABLE;
import static com.markuspage.android.atimetracker.DBHelper.TASK_ID;
import static com.markuspage.android.atimetracker.DBHelper.START;
import static com.markuspage.android.atimetracker.DBHelper.END;
import static com.markuspage.android.atimetracker.DBHelper.NAME;

/**
 *
 * @author ser
 */
public class DBBackup extends AsyncTask<SQLiteDatabase, Integer, Void> {

    private ProgressDialog progressDialog;
    private Tasks callback;
    private boolean cancel = false;
    private int success_string;
    private int fail_string;

    public enum Result {

        SUCCESS, FAILURE
    };
    public static final int PRIMARY = 0, SECONDARY = 1, SETMAX = 2;
    private Result result;
    private String message = null;

    public DBBackup(Tasks callback, ProgressDialog progress, int success_string, int fail_string) {
        this.callback = callback;
        progressDialog = progress;
        progressDialog.setProgress(0);
        progressDialog.setSecondaryProgress(0);
        this.success_string = success_string;
        this.fail_string = fail_string;
    }

    @Override
    protected Void doInBackground(SQLiteDatabase... ss) {
        SQLiteDatabase source = ss[0];
        SQLiteDatabase dest = ss[1];

        // Read the tasks and IDs
        Cursor readCursor = source.query(TASK_TABLE, TASK_COLUMNS, null, null, null, null, "rowid");
        List<Task> tasks = readTasks(readCursor);

        // Match the tasks to tasks in the existing DB, and build re-index list
        readCursor = dest.query(TASK_TABLE, TASK_COLUMNS, null, null, null, null, "rowid");
        List<Task> toReorder = readTasks(readCursor);

        int step = (int) (100.0 / tasks.size());
//        publishProgress(SETMAX, tasks.size());

        // For each task in the backup DB, see if there's a matching task in the
        // current DB.  If there is, copy the times for the task over from the
        // backup DB.  If there isn't, copy the task and it's times over.
        for (Task t : tasks) {
            boolean matchedTask = false;
            publishProgress(PRIMARY, step);
            for (Task o : toReorder) {
                if (cancel) {
                    return null;
                }
                if (t.getTaskName().equals(o.getTaskName())) {
                    copyTimes(source, t.getId(), dest, o.getId());
                    toReorder.remove(o);
                    matchedTask = true;
                    break;
                }
            }
            if (!matchedTask) {
                copyTask(source, t, dest);
            }
        }
        result = Result.SUCCESS;
        message = dest.getPath();
        return null;
    }

    @Override
    protected void onPostExecute(Void v) {
        progressDialog.dismiss();
        callback.finishedCopy(result, message, success_string, fail_string);
    }

    @Override
    protected void onProgressUpdate(Integer... vs) {
        switch (vs[0]) {
            case PRIMARY:
                if (vs[1] == 0) {
                    progressDialog.setProgress(0);
                } else {
                    progressDialog.incrementProgressBy(vs[1]);
                }
                break;
            case SECONDARY:
                if (vs[1] == 0) {
                    progressDialog.setSecondaryProgress(0);
                } else {
                    progressDialog.incrementSecondaryProgressBy(vs[1]);
                }
                break;
            case SETMAX:
                progressDialog.setMax(vs[1]);
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCancelled() {
        cancel = true;
        progressDialog.dismiss();
    }

    private void copyTimes(SQLiteDatabase sourceDb, int sourceId, SQLiteDatabase destDb, int destId) {
        publishProgress(SECONDARY, 0);
        Cursor source = sourceDb.query(RANGES_TABLE, DBHelper.RANGE_COLUMNS,
                DBHelper.TASK_ID + " = ?", new String[]{String.valueOf(sourceId)}, null, null, null);
        Cursor dest = destDb.query(RANGES_TABLE, DBHelper.RANGE_COLUMNS,
                DBHelper.TASK_ID + " = ?", new String[]{String.valueOf(destId)}, null, null, null);
        List<TimeRange> destTimes = new ArrayList<TimeRange>();
        int step = (int) (100.0 / (dest.getCount() + source.getCount()));
        if (dest.moveToFirst()) {
            do {
                if (cancel) {
                    return;
                }
                publishProgress(SECONDARY, step);
                if (!dest.isNull(1)) {
                    destTimes.add(new TimeRange(dest.getLong(0), dest.getLong(1)));
                }
            } while (dest.moveToNext());
        }
        dest.close();
        if (source.moveToFirst()) {
            ContentValues values = new ContentValues();
            do {
                if (cancel) {
                    return;
                }
                publishProgress(SECONDARY, step);
                final long start = source.getLong(0);
                long end = source.getLong(1);
                if (!source.isNull(1)) {
                    TimeRange s = new TimeRange(start, end);
                    if (!destTimes.contains(s)) {
                        values.clear();
                        values.put(TASK_ID, destId);
                        values.put(START, start);
                        values.put(END, end);
                        destDb.insert(RANGES_TABLE, null, values);
                    }
                }
            } while (source.moveToNext());
        }
        source.close();
    }

    private void copyTask(SQLiteDatabase sourceDb, Task t, SQLiteDatabase destDb) {
        if (cancel) {
            return;
        }
        ContentValues values = new ContentValues();
        values.put(NAME, t.getTaskName());
        long id = destDb.insert(TASK_TABLE, null, values);
        copyTimes(sourceDb, t.getId(), destDb, (int) id);
    }

    private List<Task> readTasks(Cursor readCursor) {
        List<Task> tasks = new ArrayList<Task>();
        if (readCursor.moveToFirst()) {
            do {
                int tid = readCursor.getInt(0);
                Task t = new Task(readCursor.getString(1), tid);
                tasks.add(t);
            } while (readCursor.moveToNext());
        }
        readCursor.close();
        return tasks;
    }
}