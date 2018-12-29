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

import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.DatePicker;
import android.widget.TimePicker;

/**
 * Activity for editing a time.
 *
 * @author Sean Russell, ser@germane-software.com
 */
public class EditTime extends Activity implements OnClickListener {

    protected static final String END_DATE = "end-date";
    protected static final String START_DATE = "start-date";
    protected static final String CLEAR = "clear";
    private boolean editingRunning = false;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(Activities.TIMETRACKERPREF, MODE_PRIVATE);
        if (getIntent().getExtras().getLong(END_DATE) == NULL) {
            setContentView(R.layout.edit_running_time_range);
            editingRunning = true;
        } else {
            setContentView(R.layout.edit_time_range);
        }
        findViewById(R.id.accept).setOnClickListener(this);
        findViewById(R.id.time_edit_cancel).setOnClickListener(this);
        TimePicker startTime = (TimePicker) findViewById(R.id.start_time);
        TimePicker endTime = (TimePicker) findViewById(R.id.end_time);
        boolean militaryTime = preferences.getBoolean(Activities.MILITARY, false);
        startTime.setIs24HourView(militaryTime);
        if (endTime != null) {
            endTime.setIs24HourView(militaryTime);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        DatePicker startDate = (DatePicker) findViewById(R.id.start_date);
        TimePicker startTime = (TimePicker) findViewById(R.id.start_time);

        Calendar sd = Calendar.getInstance();
        sd.setFirstDayOfWeek(Calendar.MONDAY);
        if (!getIntent().getExtras().getBoolean(CLEAR)) {
            sd.setTimeInMillis(getIntent().getExtras().getLong(START_DATE));
        }
        startDate.updateDate(sd.get(Calendar.YEAR), sd.get(Calendar.MONTH),
                sd.get(Calendar.DAY_OF_MONTH));
        startTime.setCurrentHour(sd.get(Calendar.HOUR_OF_DAY));
        startTime.setCurrentMinute(sd.get(Calendar.MINUTE));

        if (!editingRunning) {
            DatePicker endDate = (DatePicker) findViewById(R.id.end_date);
            TimePicker endTime = (TimePicker) findViewById(R.id.end_time);
            Calendar ed = Calendar.getInstance();
            ed.setFirstDayOfWeek(Calendar.MONDAY);
            if (getIntent().getExtras().getBoolean(CLEAR)) {
                ed = sd;
            } else {
                ed.setTimeInMillis(getIntent().getExtras().getLong(END_DATE));
            }
            endDate.updateDate(ed.get(Calendar.YEAR), ed.get(Calendar.MONTH),
                    ed.get(Calendar.DAY_OF_MONTH));
            endTime.setCurrentHour(ed.get(Calendar.HOUR_OF_DAY));
            endTime.setCurrentMinute(ed.get(Calendar.MINUTE));
        }
    }

    @Override
    public void onClick(View v) {
        DatePicker startDate = (DatePicker) findViewById(R.id.start_date);
        TimePicker startTime = (TimePicker) findViewById(R.id.start_time);
        Calendar s = Calendar.getInstance();
        s.setFirstDayOfWeek(Calendar.MONDAY);
        s.set(startDate.getYear(), startDate.getMonth(), startDate.getDayOfMonth(),
                startTime.getCurrentHour(), startTime.getCurrentMinute());
        getIntent().putExtra(START_DATE, s.getTime().getTime());

        if (!editingRunning) {
            DatePicker endDate = (DatePicker) findViewById(R.id.end_date);
            TimePicker endTime = (TimePicker) findViewById(R.id.end_time);
            Calendar e = Calendar.getInstance();
            e.setFirstDayOfWeek(Calendar.MONDAY);
            e.set(endDate.getYear(), endDate.getMonth(), endDate.getDayOfMonth(),
                    endTime.getCurrentHour(), endTime.getCurrentMinute());
            if (e.compareTo(s) < 1) {
                showDialog(0);
                return;
            }
            getIntent().putExtra(END_DATE, e.getTime().getTime());
        }

        setResult(Activity.RESULT_OK, getIntent());
        finish();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        return new AlertDialog.Builder(this)
                .setTitle(R.string.range_error_title)
                .setIcon(android.R.drawable.stat_sys_warning)
                .setCancelable(true)
                .setMessage(R.string.end_not_greater_than_start)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}