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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.text.format.DateUtils;

/**
 *
 * @author ser
 */
public class Settings extends ListActivity implements OnClickListener {

    private final static String TAG = "ATimeTracker.Settings";
    private static final int DAY_OF_WEEK_PREF_IDX = 0;
    public static final int LARGE = 24;
    public static final int MEDIUM = 20;
    public static final int SMALL = 16;
    private static final String BOOL = "bool";
    private static final String CURRENT = "current";
    private static final String CURRENTVALUE = "current-value";
    private static final String DISABLED = "disabled";
    private static final String DISABLEDVALUE = "disabled-value";
    private static final String INT = "int";
    private static final String PREFERENCE = "preference";
    private static final String PREFERENCENAME = "preference-name";
    private static final String VALUETYPE = "value-type";
    private static final int CHOOSE_DAY = 0;
    private static final int CHOOSE_ROUNDING = 1;
    
    // Notification preferences
    private static final int NOTIFICATION_ALWAYS = 0,
        NOTIFICATION_ACTIVE = 1,
        NOTIFICATION_NEVER = 2;

    private SharedPreferences applicationPreferences;
    private List<Map<String, String>> prefs;
    private SimpleAdapter adapter;
    protected final String PREFS_ACTION = "PrefsAction";
    private Map<String, Integer> fontMap;
    private static final int[] ROUND = new int[] { 0, 15, 30, 60 };
    private final String[] ROUND_NAMES = new String[ROUND.length];
    private HashMap<String, String> roundPref = new HashMap<String, String>();
    private HashMap<String, Integer>  notificationMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applicationPreferences = getSharedPreferences(Tasks.TIMETRACKERPREF, MODE_PRIVATE);
        prefs = new ArrayList<Map<String, String>>();
        setContentView(R.layout.preferences);

        Map<String, String> pref = new HashMap<String, String>();

        pref.put(PREFERENCE, getString(R.string.week_start_day));
        final int weekStart = applicationPreferences.getInt(Tasks.START_DAY, 0) % 7;
        pref.put(CURRENT, DateUtils.getDayOfWeekString(weekStart + 1, DateUtils.LENGTH_LONG));
        pref.put(CURRENTVALUE, String.valueOf(weekStart == 0 ? 0 : 1));
        pref.put(VALUETYPE, INT);
        pref.put(PREFERENCENAME, Tasks.START_DAY);
        prefs.add(pref);

        addBooleanPreference(R.string.hour_mode, Tasks.MILITARY,
                R.string.military, R.string.civilian);

        addBooleanPreference(R.string.concurrency, Tasks.CONCURRENT,
                R.string.concurrent, R.string.exclusive);

        addBooleanPreference(R.string.sound, Tasks.SOUND,
                R.string.sound_enabled, R.string.sound_disabled);

        addBooleanPreference(R.string.vibrate, Tasks.VIBRATE,
                R.string.vibrate_enabled, R.string.vibrate_disabled);

        pref = new HashMap<String, String>();
        pref.put(PREFERENCE, getString(R.string.font_size));
        final int fontSize = applicationPreferences.getInt(Tasks.FONTSIZE,
                SMALL);
        updateFontPrefs(pref, fontSize);
        pref.put(VALUETYPE, INT);
        pref.put(PREFERENCENAME, Tasks.FONTSIZE);
        prefs.add(pref);
        fontMap = new HashMap<String, Integer>(3);
        fontMap.put(getString(R.string.small_font), SMALL);
        fontMap.put(getString(R.string.medium_font), MEDIUM);
        fontMap.put(getString(R.string.large_font), LARGE);

        pref = new HashMap<String, String>();
        pref.put(PREFERENCE, getString(R.string.notification));
        final int notificationMode = applicationPreferences.getInt(
                Tasks.NOTIFICATION_MODE, NOTIFICATION_ALWAYS);
        updateNotificationPrefs(pref, notificationMode);
        pref.put(VALUETYPE, INT);
        pref.put(PREFERENCENAME, Tasks.NOTIFICATION_MODE);
        prefs.add(pref);
        notificationMap = new HashMap<String, Integer>(3);
        notificationMap.put(getString(R.string.notification_always),
                NOTIFICATION_ALWAYS);
        notificationMap.put(getString(R.string.notification_active),
                NOTIFICATION_ACTIVE);
        notificationMap.put(getString(R.string.notification_never),
                NOTIFICATION_NEVER);

        addBooleanPreference(R.string.time_display, Tasks.TIMEDISPLAY,
                R.string.decimal_time, R.string.standard_time);

        // Round times in report
        for (int i = 0; i < ROUND.length; i++) {
            if (ROUND[i] == 0) {
                ROUND_NAMES[i] = getString(R.string.round_no);
            } else {
                ROUND_NAMES[i] = getString(R.string.round_minutes, ROUND[i]);
            }
        }
        roundPref = new HashMap<String, String>();

        roundPref.put(PREFERENCE, getString(R.string.round_report_time));
        final int roundTimes = applicationPreferences.getInt(Tasks.ROUND_REPORT_TIMES, 0);
        roundPref.put(CURRENT, roundTimes == 0 ? getString(R.string.round_no) : getString(R.string.round_minutes, roundTimes));
        roundPref.put(CURRENTVALUE, String.valueOf(roundTimes));
        roundPref.put(VALUETYPE, INT);
        roundPref.put(PREFERENCENAME, Tasks.ROUND_REPORT_TIMES);
        prefs.add(roundPref);
        adapter = new SimpleAdapter(this,
                prefs,
                R.layout.preferences_row,
                new String[]{PREFERENCE, CURRENT},
                new int[]{R.id.preference_name, R.id.current_value});

        setListAdapter(adapter);
        findViewById(R.id.pref_accept).setOnClickListener(this);

        super.onCreate(savedInstanceState);
    }

    private void addBooleanPreference(int prefName, String name,
            int enabled, int disabled) {
        Map<String, String> pref;
        pref = new HashMap<String, String>();
        String prefNameString = getString(prefName);
        pref.put(PREFERENCE, prefNameString);
        boolean value = applicationPreferences.getBoolean(name, false);
        String enabledString = getString(enabled);
        String disabledString = getString(disabled);
        pref.put(CURRENT, value ? enabledString : disabledString);
        pref.put(DISABLED, value ? disabledString : enabledString);
        pref.put(CURRENTVALUE, String.valueOf(value));
        pref.put(DISABLEDVALUE, String.valueOf(!value));
        pref.put(VALUETYPE, BOOL);
        pref.put(PREFERENCENAME, name);
        prefs.add(pref);
    }

    private void updateNotificationPrefs(Map<String, String> pref,
            int notificationMode){
        final String notificationAlways = getString(R.string.notification_always);
        final String notificationActive = getString(R.string.notification_active);
        final String notificationNever = getString(R.string.notification_never);
        switch(notificationMode){
            case NOTIFICATION_ALWAYS:
                pref.put(CURRENT, notificationAlways);
                pref.put(DISABLED, notificationNever);
                pref.put(DISABLEDVALUE, String.valueOf(NOTIFICATION_NEVER));
                break;
            case NOTIFICATION_ACTIVE:
                pref.put(CURRENT, notificationActive);
                pref.put(DISABLED, notificationAlways);
                pref.put(DISABLEDVALUE, String.valueOf(NOTIFICATION_ALWAYS));
                break;
            case NOTIFICATION_NEVER:
                pref.put(CURRENT, notificationNever);
                pref.put(DISABLED, notificationActive);
                pref.put(DISABLEDVALUE, String.valueOf(NOTIFICATION_ACTIVE));
                break;
        }
        pref.put(CURRENTVALUE, String.valueOf(notificationMode));
        Log.d(TAG, "Updated preferences...");
        Log.d(TAG, "\tCurrent: " + pref.get(CURRENT));
        Log.d(TAG, "\tDisabled: " + pref.get(DISABLED));
        Log.d(TAG, "\tDisabled Value: " + pref.get(DISABLEDVALUE));
        Log.d(TAG, "\tCurrent Value: " + String.valueOf(notificationMode));
    }

    private void updateFontPrefs(Map<String, String> pref, int fontSize) {
        final String smallFont = getString(R.string.small_font);
        final String mediumFont = getString(R.string.medium_font);
        final String largeFont = getString(R.string.large_font);
        switch (fontSize) {
            case SMALL:
                pref.put(CURRENT, smallFont);
                pref.put(DISABLED, mediumFont);
                pref.put(DISABLEDVALUE, String.valueOf(MEDIUM));
                break;
            case MEDIUM:
                pref.put(CURRENT, mediumFont);
                pref.put(DISABLED, largeFont);
                pref.put(DISABLEDVALUE, String.valueOf(LARGE));
                break;
            case LARGE:
                pref.put(CURRENT, largeFont);
                pref.put(DISABLED, smallFont);
                pref.put(DISABLEDVALUE, String.valueOf(SMALL));
        }
        pref.put(CURRENTVALUE, String.valueOf(fontSize));
    }
    
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Map<String, String> pref = prefs.get((int) id);

        if (pref.get(PREFERENCENAME).equals(Tasks.START_DAY)) {
            showDialog(CHOOSE_DAY);
        } else if (pref.get(PREFERENCENAME).equals(Tasks.ROUND_REPORT_TIMES)) {
            showDialog(CHOOSE_ROUNDING);
        } else {

            String current = pref.get(CURRENT);
            String disabled = pref.get(DISABLED);
            pref.put(CURRENT, disabled);
            pref.put(DISABLED, current);
            String current_value = pref.get(CURRENTVALUE);
            String disabled_value = pref.get(DISABLEDVALUE);
            pref.put(CURRENTVALUE, disabled_value);
            pref.put(DISABLEDVALUE, current_value);

            if (pref.get(PREFERENCENAME).equals(Tasks.NOTIFICATION_MODE)){
                updateNotificationPrefs(pref, notificationMap.get(disabled));
            }

            if (pref.get(PREFERENCENAME).equals(Tasks.FONTSIZE)) {
                updateFontPrefs(pref, fontMap.get(disabled));  // disabled is the new enabled!
            }
        }

        adapter.notifyDataSetChanged();
        this.getListView().invalidate();
    }

    public void onClick(View v) {
        Intent returnIntent = getIntent();
        SharedPreferences.Editor ed = applicationPreferences.edit();
        for (Map<String, String> pref : prefs) {
            String prefName = pref.get(PREFERENCENAME);
            if (pref.get(VALUETYPE).equals(INT)) {
                final Integer value = Integer.valueOf(pref.get(CURRENTVALUE));
                if (value != applicationPreferences.getInt(prefName, 0)) {
                    ed.putInt(prefName, value);
                    returnIntent.putExtra(prefName, true);
                }
            } else if (pref.get(VALUETYPE).equals(BOOL)) {
                final Boolean value = Boolean.valueOf(pref.get(CURRENTVALUE));
                if (value != applicationPreferences.getBoolean(prefName, false)) {
                    ed.putBoolean(prefName, value);
                    returnIntent.putExtra(prefName, true);
                }
            }
        }
        ed.commit();

        getIntent().putExtra(PREFS_ACTION, PREFS_ACTION);
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }
    static String[] DAYS_OF_WEEK = new String[7];

    static {
        for (int i = 0; i < 7; i++) {
            DAYS_OF_WEEK[i] = DateUtils.getDayOfWeekString(i + 1, DateUtils.LENGTH_LONG);
        }
    }

    @Override
    protected Dialog onCreateDialog(int dialogId) {
        switch (dialogId) {
            case CHOOSE_DAY:
                return new AlertDialog.Builder(this).setItems(DAYS_OF_WEEK, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface iface, int whichChoice) {
                        Map<String, String> startDay = prefs.get(DAY_OF_WEEK_PREF_IDX);
                        startDay.put(CURRENT, DAYS_OF_WEEK[whichChoice]);
                        startDay.put(CURRENTVALUE, String.valueOf(whichChoice));
                        adapter.notifyDataSetChanged();
                        Settings.this.getListView().invalidate();
                    }
                }).create();
            case CHOOSE_ROUNDING:
                return new AlertDialog.Builder(this).setItems(ROUND_NAMES, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface iface, int whichChoice) {
                        roundPref.put(CURRENT, ROUND_NAMES[whichChoice]);
                        roundPref.put(CURRENTVALUE, String.valueOf(ROUND[whichChoice]));
                        adapter.notifyDataSetChanged();
                        Settings.this.getListView().invalidate();
                    }
                }).create();
            default:
                break;
        }
        return null;
    }
}
