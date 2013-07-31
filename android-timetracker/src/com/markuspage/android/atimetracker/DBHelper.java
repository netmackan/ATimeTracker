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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    public static final String END = "end";
    public static final String START = "start";
    public static final String TASK_ID = "task_id";
    public static final String[] RANGE_COLUMNS = {START, END};
    public static final String NAME = "name";
    public static final String[] TASK_COLUMNS = new String[]{"ROWID", NAME};
    public static final String TIMETRACKER_DB_NAME = "timetracker.db";
    public static final int DBVERSION = 5;
    public static final String RANGES_TABLE = "ranges";
    public static final String TASK_TABLE = "tasks";
    public static final String TASK_NAME = "name";
    public static final String ID_NAME = "_id";

    public DBHelper(Context context) {
        super(context, TIMETRACKER_DB_NAME, null, DBVERSION);
        instance = this;
    }
    /**
     * Despite the name, this is not a singleton constructor
     */
    private static DBHelper instance;

    public static DBHelper getInstance() {
        return instance;
    }
    private static final String CREATE_TASK_TABLE =
            "CREATE TABLE %s ("
            + ID_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + TASK_NAME + " TEXT COLLATE LOCALIZED NOT NULL"
            + ");";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format(CREATE_TASK_TABLE, TASK_TABLE));
        db.execSQL("CREATE TABLE " + RANGES_TABLE + "("
                + TASK_ID + " INTEGER NOT NULL,"
                + START + " INTEGER NOT NULL,"
                + END + " INTEGER"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            arg0.execSQL(String.format(CREATE_TASK_TABLE, "temp"));
            arg0.execSQL("insert into temp(rowid," + TASK_NAME + ") select rowid,"
                    + TASK_NAME + " from " + TASK_TABLE + ";");
            arg0.execSQL("drop table " + TASK_TABLE + ";");
            arg0.execSQL(String.format(CREATE_TASK_TABLE, TASK_TABLE));
            arg0.execSQL("insert into " + TASK_TABLE + "(" + ID_NAME + "," + TASK_NAME
                    + ") select rowid," + TASK_NAME + " from temp;");
            arg0.execSQL("drop table temp;");
        } else if (oldVersion < 5) {
            arg0.execSQL(String.format(CREATE_TASK_TABLE, "temp"));
            arg0.execSQL("insert into temp(" + ID_NAME + "," + TASK_NAME + ") select rowid,"
                    + TASK_NAME + " from " + TASK_TABLE + ";");
            arg0.execSQL("drop table " + TASK_TABLE + ";");
            arg0.execSQL(String.format(CREATE_TASK_TABLE, TASK_TABLE));
            arg0.execSQL("insert into " + TASK_TABLE + "(" + ID_NAME + "," + TASK_NAME
                    + ") select " + ID_NAME + "," + TASK_NAME + " from temp;");
            arg0.execSQL("drop table temp;");
        }
    }
}