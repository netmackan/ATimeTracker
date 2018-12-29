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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper class for database interactions.
 *
 * @author Sean Russell, ser@germane-software.com
 */
public class DBHelper extends SQLiteOpenHelper {

    public static final String END = "end";
    public static final String START = "start";
    public static final String ACTIVITY_ID = "task_id";
    public static final String[] RANGE_COLUMNS = {START, END};
    public static final String NAME = "name";
    public static final String[] ACTIVITY_COLUMNS = new String[]{"ROWID", NAME};
    public static final String TIMETRACKER_DB_NAME = "timetracker.db";
    public static final int DBVERSION = 5;
    public static final String RANGES_TABLE = "ranges";
    public static final String ACTIVITY_TABLE = "tasks";
    public static final String ACTIVITY_NAME = "name";
    public static final String ID_NAME = "_id";

    public DBHelper(Context context) {
        super(context, TIMETRACKER_DB_NAME, null, DBVERSION);
    }

    private static final String CREATE_ACTIVITY_TABLE =
            "CREATE TABLE %s ("
            + ID_NAME + " INTEGER PRIMARY KEY AUTOINCREMENT,"
            + ACTIVITY_NAME + " TEXT COLLATE LOCALIZED NOT NULL"
            + ");";

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(String.format(CREATE_ACTIVITY_TABLE, ACTIVITY_TABLE));
        db.execSQL("CREATE TABLE " + RANGES_TABLE + "("
                + ACTIVITY_ID + " INTEGER NOT NULL,"
                + START + " INTEGER NOT NULL,"
                + END + " INTEGER"
                + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase arg0, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            arg0.execSQL(String.format(CREATE_ACTIVITY_TABLE, "temp"));
            arg0.execSQL("insert into temp(rowid," + ACTIVITY_NAME + ") select rowid,"
                    + ACTIVITY_NAME + " from " + ACTIVITY_TABLE + ";");
            arg0.execSQL("drop table " + ACTIVITY_TABLE + ";");
            arg0.execSQL(String.format(CREATE_ACTIVITY_TABLE, ACTIVITY_TABLE));
            arg0.execSQL("insert into " + ACTIVITY_TABLE + "(" + ID_NAME + "," + ACTIVITY_NAME
                    + ") select rowid," + ACTIVITY_NAME + " from temp;");
            arg0.execSQL("drop table temp;");
        } else if (oldVersion < 5) {
            arg0.execSQL(String.format(CREATE_ACTIVITY_TABLE, "temp"));
            arg0.execSQL("insert into temp(" + ID_NAME + "," + ACTIVITY_NAME + ") select rowid,"
                    + ACTIVITY_NAME + " from " + ACTIVITY_TABLE + ";");
            arg0.execSQL("drop table " + ACTIVITY_TABLE + ";");
            arg0.execSQL(String.format(CREATE_ACTIVITY_TABLE, ACTIVITY_TABLE));
            arg0.execSQL("insert into " + ACTIVITY_TABLE + "(" + ID_NAME + "," + ACTIVITY_NAME
                    + ") select " + ID_NAME + "," + ACTIVITY_NAME + " from temp;");
            arg0.execSQL("drop table temp;");
        }
    }
}