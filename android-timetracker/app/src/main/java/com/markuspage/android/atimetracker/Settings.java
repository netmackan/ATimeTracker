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
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.text.format.DateUtils;

/**
 * Preferences activity.
 *
 * @author Sean Russell, ser@germane-software.com
 */
public class Settings extends ListActivity implements OnClickListener {

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
    private SharedPreferences applicationPreferences;
    private List<Map<String, String>> prefs;
    private SimpleAdapter adapter;
    protected final String PREFS_ACTION = "PrefsAction";
    private Map<String, Integer> fontMap;
    private static final int[] ROUND = new int[] { 0, 15, 30, 60 };
    private final String[] ROUND_NAMES = new String[ROUND.length];
    private HashMap<String, String> roundPref = new HashMap<String, String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applicationPreferences = getSharedPreferences(Activities.TIMETRACKERPREF, MODE_PRIVATE);
        prefs = new ArrayList<Map<String, String>>();
        setContentView(R.layout.preferences);

        Map<String, String> pref = new HashMap<String, String>();

        pref.put(PREFERENCE, getString(R.string.week_start_day));
        final int weekStart = applicationPreferences.getInt(Activities.START_DAY, 0) % 7;
        pref.put(CURRENT, DateUtils.getDayOfWeekString(weekStart + 1, DateUtils.LENGTH_LONG));
        pref.put(CURRENTVALUE, String.valueOf(weekStart == 0 ? 0 : 1));
        pref.put(VALUETYPE, INT);
        pref.put(PREFERENCENAME, Activities.START_DAY);
        prefs.add(pref);

        addBooleanPreference(R.string.hour_mode, Activities.MILITARY,
                R.string.military, R.string.civilian, true);

        addBooleanPreference(R.string.concurrency, Activities.CONCURRENT,
                R.string.concurrent, R.string.exclusive, false);

        addBooleanPreference(R.string.sound, Activities.SOUND,
                R.string.sound_enabled, R.string.sound_disabled, false);

        addBooleanPreference(R.string.vibrate, Activities.VIBRATE,
                R.string.vibrate_enabled, R.string.vibrate_disabled, true);

        pref = new HashMap<String, String>();
        pref.put(PREFERENCE, getString(R.string.font_size));
        final int fontSize = applicationPreferences.getInt(Activities.FONTSIZE,
                SMALL);
        updateFontPrefs(pref, fontSize);
        pref.put(VALUETYPE, INT);
        pref.put(PREFERENCENAME, Activities.FONTSIZE);
        prefs.add(pref);
        fontMap = new HashMap<String, Integer>(3);
        fontMap.put(getString(R.string.small_font), SMALL);
        fontMap.put(getString(R.string.medium_font), MEDIUM);
        fontMap.put(getString(R.string.large_font), LARGE);

        addBooleanPreference(R.string.time_display, Activities.TIMEDISPLAY,
                R.string.decimal_time, R.string.standard_time, false);

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
        final int roundTimes = applicationPreferences.getInt(Activities.ROUND_REPORT_TIMES, 0);
        roundPref.put(CURRENT, roundTimes == 0 ? getString(R.string.round_no) : getString(R.string.round_minutes, roundTimes));
        roundPref.put(CURRENTVALUE, String.valueOf(roundTimes));
        roundPref.put(VALUETYPE, INT);
        roundPref.put(PREFERENCENAME, Activities.ROUND_REPORT_TIMES);
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
            int enabled, int disabled, boolean defaultEnabled) {
        Map<String, String> pref;
        pref = new HashMap<String, String>();
        String prefNameString = getString(prefName);
        pref.put(PREFERENCE, prefNameString);
        boolean value = applicationPreferences.getBoolean(name, defaultEnabled);
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

        if (pref.get(PREFERENCENAME).equals(Activities.START_DAY)) {
            showDialog(CHOOSE_DAY);
        } else if (pref.get(PREFERENCENAME).equals(Activities.ROUND_REPORT_TIMES)) {
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

            if (pref.get(PREFERENCENAME).equals(Activities.FONTSIZE)) {
                updateFontPrefs(pref, fontMap.get(disabled));  // disabled is the new enabled!
            }
        }

        adapter.notifyDataSetChanged();
        this.getListView().invalidate();
    }

    @Override
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
                if (value != applicationPreferences.getBoolean(prefName, !value)) {
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
                    @Override
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
                    @Override
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
