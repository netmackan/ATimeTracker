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

/**
 * TimeTracker main activity.
 *
 * @author Sean Russell, ser@germane-software.com
 */
package com.markuspage.android.atimetracker;

import android.Manifest;
import static com.markuspage.android.atimetracker.DBHelper.END;
import static com.markuspage.android.atimetracker.DBHelper.NAME;
import static com.markuspage.android.atimetracker.DBHelper.RANGES_TABLE;
import static com.markuspage.android.atimetracker.DBHelper.RANGE_COLUMNS;
import static com.markuspage.android.atimetracker.DBHelper.START;
import static com.markuspage.android.atimetracker.Report.weekEnd;
import static com.markuspage.android.atimetracker.Report.weekStart;
import static com.markuspage.android.atimetracker.TimeRange.NULL;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
import android.support.annotation.NonNull;
import android.support.v13.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.method.SingleLineTransformationMethod;
import android.text.util.Linkify;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import static com.markuspage.android.atimetracker.DBHelper.ACTIVITY_ID;
import static com.markuspage.android.atimetracker.DBHelper.ACTIVITY_COLUMNS;
import static com.markuspage.android.atimetracker.DBHelper.ACTIVITY_TABLE;

/**
 * Manages and displays a list of activities, providing the ability to edit and
 * display individual activity items.
 *
 * @author Sean Russell, ser@germane-software.com
 */
public class Activities extends ListActivity implements ActivityCompat.OnRequestPermissionsResultCallback {

    public static final String TIMETRACKERPREF = "timetracker.pref";
    protected static final String FONTSIZE = "font-size";
    protected static final String MILITARY = "military-time";
    protected static final String CONCURRENT = "concurrent-tasks";
    protected static final String SOUND = "sound-enabled";
    protected static final String VIBRATE = "vibrate-enabled";
    protected static final String START_DAY = "start_day";
    protected static final String START_DATE = "start_date";
    protected static final String END_DATE = "end_date";
    protected static final String VIEW_MODE = "view_mode";
    protected static final String REPORT_DATE = "report_date";
    protected static final String TIMEDISPLAY = "time_display";
    protected static final String ROUND_REPORT_TIMES = "round_report_times";
    protected static final String APP_VERSION = "app_version";
    
    /**
     * Defines how each activity's time is displayed
     */
    private static final String FORMAT = "%02d:%02d";
    private static final String DECIMAL_FORMAT = "%02d.%02d";
    /**
     * How often to refresh the display, in milliseconds
     */
    private static final int REFRESH_MS = 60000;

    /** Callback ID for exporting after asking permissions. */
    private static final int MY_PERMISSIONS_REQUEST_EXPORT = 100;
    
    /** Callback ID for creating backup after asking permissions. */
    private static final int MY_PERMISSIONS_REQUEST_CREATE_BACKUP = 101;

    /** Callback ID for restoring backup after asking permissions. */
    private static final int MY_PERMISSIONS_REQUEST_RESTORE_BACKUP = 102;
    
    /**
     * The model for this view
     */
    private ActivityAdapter adapter;
    /**
     * A timer for refreshing the display.
     */
    private Handler timer;
    /**
     * The call-back that actually updates the display.
     */
    private TimerTask updater;
    /**
     * The currently active activity (the one that is currently being timed). There
     * can be only one.
     */
    private boolean running = false;
    /**
     * The currently selected activity when the context menu is invoked.
     */
    private Activity selectedActivity;
    private SharedPreferences preferences;
    private int fontSize = 16;
    private boolean concurrency;
    private MediaPlayer clickPlayer;
    private boolean playClick = false;
    private boolean vibrateClick = true;
    private Vibrator vibrateAgent;
    private ProgressDialog progressDialog = null;
    private boolean decimalFormat = false;
    private String versionName;
    /**
     * A list of menu options, including both context and options menu items
     */
    protected static final int ADD_ACTIVITY = 0,
            EDIT_ACTIVITY = 1, DELETE_ACTIVITY = 2, REPORT = 3, SHOW_TIMES = 4,
            CHANGE_VIEW = 5, SELECT_START_DATE = 6, SELECT_END_DATE = 7,
            HELP = 8, EXPORT_VIEW = 9, SUCCESS_DIALOG = 10, ERROR_DIALOG = 11,
            SET_WEEK_START_DAY = 12, BACKUP = 14, PREFERENCES = 15,
            PROGRESS_DIALOG = 16, RESTORE = 17;
    // TODO: This could be done better...
    private static final String DB_FILE = "/data/data/com.markuspage.android.atimetracker/databases/timetracker.db";
    private final File dbBackup = new File(Environment.getExternalStorageDirectory(), "timetracker.db");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //android.os.Debug.waitForDebugger();
        preferences = getSharedPreferences(TIMETRACKERPREF, MODE_PRIVATE);
        fontSize = preferences.getInt(FONTSIZE, 16);
        concurrency = preferences.getBoolean(CONCURRENT, false);

        int which = preferences.getInt(VIEW_MODE, 0);
        if (adapter == null) {
            adapter = new ActivityAdapter(this);
            setListAdapter(adapter);
            switchView(which);
        }
        if (timer == null) {
            timer = new Handler();
        }
        if (updater == null) {
            updater = new TimerTask() {
                @Override
                public void run() {
                    if (running) {
                        adapter.notifyDataSetChanged();
                        setTitle();
                        Activities.this.getListView().invalidate();
                    }
                    timer.postDelayed(this, REFRESH_MS);
                }
            };
        }
        playClick = preferences.getBoolean(SOUND, false);
        if (playClick && clickPlayer == null) {
            clickPlayer = MediaPlayer.create(this, R.raw.click);
            try {
                clickPlayer.prepareAsync();
            } catch (IllegalStateException illegalStateException) {
                // ignore this.  There's nothing the user can do about it.
                Logger.getLogger("TimeTracker").log(Level.SEVERE,
                        "Failed to set up audio player: {0}", illegalStateException.getMessage());
            }
        }
        decimalFormat = preferences.getBoolean(TIMEDISPLAY, false);
        registerForContextMenu(getListView());
        
        // Display help if this it the first start with this version
        final String lastVersion = preferences.getString(APP_VERSION, "0.0");
        if (!getVersionName().equals(lastVersion)) {
            showDialog(HELP);
            SharedPreferences.Editor ed = preferences.edit();
            ed.putString(APP_VERSION, getVersionName());
            ed.commit();
        }

        vibrateAgent = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        vibrateClick = preferences.getBoolean(VIBRATE, true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (timer != null) {
            timer.removeCallbacks(updater);
        }
    }

    @Override
    protected void onStop() {
        if (adapter != null) {
            adapter.close();
        }
        if (clickPlayer != null) {
            clickPlayer.release();
            clickPlayer = null;
        }
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // This is only to cause the view to reload, so that we catch 
        // updates to the time list.
        int which = preferences.getInt(VIEW_MODE, 0);
        switchView(which);

        if (timer != null && running) {
            timer.post(updater);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_ACTIVITY, 0, R.string.add_activity_title).setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, REPORT, 1, R.string.generate_report_title).setIcon(android.R.drawable.ic_menu_week);
        menu.add(0, CHANGE_VIEW, 2, R.string.change_date_range);
        menu.add(0, EXPORT_VIEW, 3, R.string.export_view_to_csv);
        menu.add(0, BACKUP, 4, R.string.back_up_to_sd_card);
        menu.add(0, RESTORE, 5, R.string.restore_from_backup);
        menu.add(0, PREFERENCES, 6, R.string.preferences).setIcon(android.R.drawable.ic_menu_preferences);
        menu.add(0, HELP, 7, R.string.help).setIcon(android.R.drawable.ic_menu_help);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Activities menu");
        menu.add(0, EDIT_ACTIVITY, 0, getText(R.string.edit_activity));
        menu.add(0, DELETE_ACTIVITY, 0, getText(R.string.delete_activity));
        menu.add(0, SHOW_TIMES, 0, getText(R.string.show_times));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        selectedActivity = (Activity) adapter.getItem((int) info.id);
        switch (item.getItemId()) {
            case SHOW_TIMES:
                Intent intent = new Intent(this, ActivityTimes.class);
                intent.putExtra(ACTIVITY_ID, selectedActivity.getId());
                if (adapter.currentRangeStart != -1) {
                    intent.putExtra(START, adapter.currentRangeStart);
                    intent.putExtra(END, adapter.currentRangeEnd);
                }
                startActivity(intent);
                break;
            default:
                showDialog(item.getItemId());
                break;
        }
        return super.onContextItemSelected(item);
    }
    private AlertDialog operationSucceed;
    private AlertDialog operationFailed;
    private String exportMessage;
    private String baseTitle;

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
            case ADD_ACTIVITY:
            case CHANGE_VIEW:
            case EXPORT_VIEW:
            case BACKUP:
            case RESTORE:
            case PREFERENCES:
            case HELP:
                showDialog(item.getItemId());
                break;
            case REPORT:
                Intent intent = new Intent(this, Report.class);
                intent.putExtra(REPORT_DATE, System.currentTimeMillis());
                intent.putExtra(START_DAY, preferences.getInt(START_DAY, 0) + 1);
                intent.putExtra(TIMEDISPLAY, decimalFormat);
                intent.putExtra(ROUND_REPORT_TIMES, preferences.getInt(ROUND_REPORT_TIMES, 0));
                startActivity(intent);
                break;
            default:
                // Ignore the other menu items; they're context menu
                break;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case ADD_ACTIVITY:
                return openNewActivityDialog();
            case EDIT_ACTIVITY:
                return openEditActivityDialog();
            case DELETE_ACTIVITY:
                return openDeleteActivityDialog();
            case CHANGE_VIEW:
                return openChangeViewDialog();
            case HELP:
                return openAboutDialog();
            case SUCCESS_DIALOG:
                operationSucceed = new AlertDialog.Builder(Activities.this)
                        .setTitle(R.string.success)
                        .setIcon(android.R.drawable.stat_notify_sdcard)
                        .setMessage(exportMessage)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                return operationSucceed;
            case ERROR_DIALOG:
                operationFailed = new AlertDialog.Builder(Activities.this)
                        .setTitle(R.string.failure)
                        .setIcon(android.R.drawable.stat_notify_sdcard)
                        .setMessage(exportMessage)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                return operationFailed;
            case PROGRESS_DIALOG:
                progressDialog = new ProgressDialog(this);
                progressDialog.setMessage("Copying records...");
                progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                progressDialog.setCancelable(false);
                return progressDialog;
            case EXPORT_VIEW: {
                requestExport();
                break;
            }
            case BACKUP: {
                requestBackupCreation();
                break;
            }
            case RESTORE: {
                requestBackupRestore();
                break;
            }
            case PREFERENCES: { // PREFERENCES
                Intent intent = new Intent(Activities.this, Settings.class);
                startActivityForResult(intent, PREFERENCES);
                break;
            }
        }
        return null;
    }

    protected void perform(String message, int success_string, int fail_string) {
        if (message != null) {
            exportMessage = getString(success_string, message);
            if (operationSucceed != null) {
                operationSucceed.setMessage(exportMessage);
            }
            showDialog(SUCCESS_DIALOG);
        } else {
            exportMessage = getString(fail_string, message);
            if (operationFailed != null) {
                operationFailed.setMessage(exportMessage);
            }
            showDialog(ERROR_DIALOG);
        }
    }

    /**
     * Creates a progressDialog to change the dates for which activity times are
     * shown. Offers a short selection of pre-defined defaults, and the option
     * to choose a range from a progressDialog.
     *
     * @see arrays.xml
     * @return the progressDialog to be displayed
     */
    private Dialog openChangeViewDialog() {
        return new AlertDialog.Builder(Activities.this).setItems(R.array.views, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putInt(VIEW_MODE, which);
                ed.commit();
                if (which == 5) {
                    Calendar calInstance = Calendar.getInstance();
                    new DatePickerDialog(Activities.this,
                            new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year,
                                int monthOfYear, int dayOfMonth) {
                            Calendar start = Calendar.getInstance();
                            start.set(Calendar.YEAR, year);
                            start.set(Calendar.MONTH, monthOfYear);
                            start.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                            start.set(Calendar.HOUR, start.getMinimum(Calendar.HOUR));
                            start.set(Calendar.MINUTE, start.getMinimum(Calendar.MINUTE));
                            start.set(Calendar.SECOND, start.getMinimum(Calendar.SECOND));
                            start.set(Calendar.MILLISECOND, start.getMinimum(Calendar.MILLISECOND));
                            SharedPreferences.Editor ed = preferences.edit();
                            ed.putLong(START_DATE, start.getTime().getTime());
                            ed.commit();

                            new DatePickerDialog(Activities.this,
                                    new DatePickerDialog.OnDateSetListener() {
                                @Override
                                public void onDateSet(DatePicker view, int year,
                                        int monthOfYear, int dayOfMonth) {
                                    Calendar end = Calendar.getInstance();
                                    end.set(Calendar.YEAR, year);
                                    end.set(Calendar.MONTH, monthOfYear);
                                    end.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                                    end.set(Calendar.HOUR, end.getMaximum(Calendar.HOUR));
                                    end.set(Calendar.MINUTE, end.getMaximum(Calendar.MINUTE));
                                    end.set(Calendar.SECOND, end.getMaximum(Calendar.SECOND));
                                    end.set(Calendar.MILLISECOND, end.getMaximum(Calendar.MILLISECOND));
                                    SharedPreferences.Editor ed = preferences.edit();
                                    ed.putLong(END_DATE, end.getTime().getTime());
                                    ed.commit();
                                    Activities.this.switchView(5);  // Update the list view
                                }
                            },
                                    year,
                                    monthOfYear,
                                    dayOfMonth).show();
                        }
                    },
                            calInstance.get(Calendar.YEAR),
                            calInstance.get(Calendar.MONTH),
                            calInstance.get(Calendar.DAY_OF_MONTH)).show();
                } else {
                    switchView(which);
                }
            }
        }).create();
    }

    private void switchView(int which) {
        Calendar tw = Calendar.getInstance();
        int startDay = preferences.getInt(START_DAY, 0) + 1;
        tw.setFirstDayOfWeek(startDay);
        String ttl = getString(R.string.title,
                getResources().getStringArray(R.array.views)[which]);
        switch (which) {
            case 0: // today
                adapter.loadActivity(tw);
                break;
            case 1: // this week
                adapter.loadActivities(weekStart(tw, startDay), weekEnd(tw, startDay));
                break;
            case 2: // yesterday
                tw.add(Calendar.DAY_OF_MONTH, -1);
                adapter.loadActivity(tw);
                break;
            case 3: // last week
                tw.add(Calendar.WEEK_OF_YEAR, -1);
                adapter.loadActivities(weekStart(tw, startDay), weekEnd(tw, startDay));
                break;
            case 4: // all
                adapter.loadActivities();
                break;
            case 5: // select range
                Calendar start = Calendar.getInstance();
                start.setTimeInMillis(preferences.getLong(START_DATE, 0));
                System.err.println("START = " + start.getTime());
                Calendar end = Calendar.getInstance();
                end.setTimeInMillis(preferences.getLong(END_DATE, 0));
                System.err.println("END = " + end.getTime());
                adapter.loadActivities(start, end);
                DateFormat f = DateFormat.getDateInstance(DateFormat.SHORT);
                ttl = getString(R.string.title,
                        f.format(start.getTime()) + " - " + f.format(end.getTime()));
                break;
            default: // Unknown
                break;
        }
        baseTitle = ttl;
        setTitle();
        getListView().invalidate();
    }

    private void setTitle() {
        long total = 0;
        for (Activity t : adapter.activities) {
            total += t.getTotal();
        }
        setTitle(baseTitle + " " + formatTotal(decimalFormat, total, 0));
    }

    /**
     * Constructs a progressDialog for defining a new activity. If accepted, creates
     * a new activity. If cancelled, closes the progressDialog with no affect.
     *
     * @return the progressDialog to display
     */
    private Dialog openNewActivityDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.edit_activity, null);
        final AlertDialog dialog = new AlertDialog.Builder(Activities.this)
                .setTitle(R.string.add_activity_title).setView(textEntryView)
                .setPositiveButton(R.string.add_activity_ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText textView = (EditText) textEntryView.findViewById(R.id.activity_edit_name_edit);
                String name = textView.getText().toString();
                if (!name.isEmpty()) {
                    adapter.addActivity(name);
                    Activities.this.getListView().invalidate();
                    dialog.dismiss();
                }
            }
        });
        return dialog;
    }

    /**
     * Constructs a progressDialog for editing activity attributes. If accepted,
     * alters the activity being edited. If cancelled, dismissed the progressDialog
     * with no effect.
     *
     * @return the progressDialog to display
     */
    private Dialog openEditActivityDialog() {
        if (selectedActivity == null) {
            return null;
        }
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.edit_activity, null);
        final AlertDialog dialog = new AlertDialog.Builder(Activities.this)
                .setView(textEntryView)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText textView = (EditText) textEntryView.findViewById(R.id.activity_edit_name_edit);
                String name = textView.getText().toString();
                if (!name.isEmpty()) {
                    selectedActivity.setName(name);
                    adapter.updateActivity(selectedActivity);
                    Activities.this.getListView().invalidate();
                    dialog.dismiss();
                }
            }
        });
        return dialog;
    }

    /**
     * Constructs a progressDialog asking for confirmation for a delete request.
     * If accepted, deletes the activity. If canceled, closes the progressDialog.
     *
     * @return the progressDialog to display
     */
    private Dialog openDeleteActivityDialog() {
        if (selectedActivity == null) {
            return null;
        }
        String formattedMessage = getString(R.string.delete_activity_message,
                selectedActivity.getName());
        return new AlertDialog.Builder(Activities.this)
                .setTitle(R.string.delete_activity_title)
                .setIcon(android.R.drawable.stat_sys_warning)
                .setCancelable(true)
                .setMessage(formattedMessage)
                .setPositiveButton(R.string.delete_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                adapter.deleteActivity(selectedActivity);
                Activities.this.getListView().invalidate();
            }
        }).setNegativeButton(android.R.string.cancel, null)
                .create();
    }
    final static String SDCARD = "/sdcard/";

    private String export() {
        // Export, then show a progressDialog
        String rangeName = getRangeName();
        String fname = rangeName + ".csv";
        File fout = new File(SDCARD + fname);
        // Change the file name until there's no conflict
        int counter = 0;
        while (fout.exists()) {
            fname = rangeName + "_" + counter + ".csv";
            fout = new File(SDCARD + fname);
            counter++;
        }
        try (
                OutputStream out = new FileOutputStream(fout);
                Cursor currentRange = adapter.getCurrentRange()
            ) {
            CSVExporter.exportRows(out, currentRange);
            return fname;
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace(System.err);
            return null;
        } catch (IOException ex) {
            ex.printStackTrace(System.err);
            return null;
        }
    }

    private String getRangeName() {
        if (adapter.currentRangeStart == -1) {
            return "all";
        }
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        Date d = new Date();
        d.setTime(adapter.currentRangeStart);
        return f.format(d);
    }

    private String getVersionName() {
        if (versionName == null) {
            try {
                PackageInfo pkginfo = this.getPackageManager().getPackageInfo("com.markuspage.android.atimetracker", 0);
                versionName = pkginfo.versionName;
            } catch (NameNotFoundException nnfe) {
                nnfe.printStackTrace();
                versionName = "n/a";
            }
        }
        return versionName;
    }

    private Dialog openAboutDialog() {
        String formattedVersion = getString(R.string.version, getVersionName());

        LayoutInflater factory = LayoutInflater.from(this);
        View about = factory.inflate(R.layout.about, null);

        TextView version = (TextView) about.findViewById(R.id.version);
        version.setText(formattedVersion);
        TextView links = (TextView) about.findViewById(R.id.usage);
        Linkify.addLinks(links, Linkify.ALL);
        links = (TextView) about.findViewById(R.id.credits);
        Linkify.addLinks(links, Linkify.ALL);

        return new AlertDialog.Builder(Activities.this).setView(about).setPositiveButton(android.R.string.ok, null).create();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog d) {
        EditText textView;
        switch (id) {
            case ADD_ACTIVITY:
                textView = (EditText) d.findViewById(R.id.activity_edit_name_edit);
                textView.setText("");
                break;
            case EDIT_ACTIVITY:
                textView = (EditText) d.findViewById(R.id.activity_edit_name_edit);
                textView.setText(selectedActivity.getName());
                break;
            default:
                break;
        }
    }
    
    /**
     * Check if we have permission to export and if not ask for
     * permission to do it.
     */
    private void requestExport() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            new AlertDialog.Builder(this)
                    .setMessage(R.string.permission_to_export_needed)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(Activities.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_EXPORT);
                }
            }).create().show();

        } else {
            doExport();
        }
    }

    /**
     * Check if we have permission to restore the backup and if not ask for
     * permission to do it.
     */
    private void requestBackupRestore() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            new AlertDialog.Builder(this)
                    .setMessage(R.string.permission_to_restore_needed)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(Activities.this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_RESTORE_BACKUP);
                }
            }).create().show();

        } else {
            doBackupRestore();
        }
    }
    
    /**
     * Check if we have permission to create the backup and if not ask for
     * permission to do it.
     */
    private void requestBackupCreation() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Show an explanation to the user *asynchronously* -- don't block
            // this thread waiting for the user's response! After the user
            // sees the explanation, try again to request the permission.
            new AlertDialog.Builder(this)
                    .setMessage(R.string.permission_to_backup_needed)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ActivityCompat.requestPermissions(Activities.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_CREATE_BACKUP);
                }
            }).create().show();

        } else {
            doBackupCreation();
        }
    }
    
    /**
     * Perform the export.
     * This assumes permission has already been granted.
     */
    private void doExport() {
        String fname = export();
        perform(fname, R.string.export_csv_success, R.string.export_csv_fail);
    }

    /**
     * Perform the restore.
     * This assumes permission has already been granted.
     */
    private void doBackupRestore() {
        if (dbBackup.exists()) {
            try {
                showDialog(Activities.PROGRESS_DIALOG);
                SQLiteDatabase backupDb = SQLiteDatabase.openDatabase(dbBackup.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                SQLiteDatabase appDb = SQLiteDatabase.openDatabase(DB_FILE, null, SQLiteDatabase.OPEN_READWRITE);
                DBBackup backup = new DBBackup(Activities.this, progressDialog, R.string.restore_success, R.string.restore_failed);
                backup.execute(backupDb, appDb);
            } catch (Exception ex) {
                Logger.getLogger(Activities.class.getName()).log(Level.SEVERE, null, ex);
                exportMessage = ex.getLocalizedMessage();
                showDialog(ERROR_DIALOG);
            }
        } else {
            Logger.getLogger(Activities.class.getName()).log(Level.SEVERE, "Backup file does not exist: {0}", dbBackup.getAbsolutePath());
            exportMessage = getString(R.string.restore_failed, "No backup file: " + dbBackup.getAbsolutePath());
            showDialog(ERROR_DIALOG);
        }
    }
    
    /**
     * Perform the backup creation.
     * This assumes permission has already been granted.
     */
    private void doBackupCreation() {
        showDialog(Activities.PROGRESS_DIALOG);
        if (dbBackup.exists()) {
            // Find the database
            SQLiteDatabase backupDb = SQLiteDatabase.openDatabase(dbBackup.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
            SQLiteDatabase appDb = SQLiteDatabase.openDatabase(DB_FILE, null, SQLiteDatabase.OPEN_READONLY);
            DBBackup backup = new DBBackup(Activities.this, progressDialog, R.string.backup_success, R.string.backup_failed);
            backup.execute(appDb, backupDb);
        } else {
            InputStream in = null;
            OutputStream out = null;

            try {
                in = new BufferedInputStream(new FileInputStream(DB_FILE));
                out = new BufferedOutputStream(new FileOutputStream(dbBackup));
                for (int c = in.read(); c != -1; c = in.read()) {
                    out.write(c);
                }
                finishedCopy(DBBackup.Result.SUCCESS, dbBackup.getAbsolutePath(), R.string.backup_success, R.string.backup_failed);
            } catch (IOException ex) {
                Logger.getLogger(Activities.class.getName()).log(Level.SEVERE, null, ex);
                exportMessage = ex.getLocalizedMessage();
                showDialog(ERROR_DIALOG);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ignored) {
                }
                try {
                    if (out != null) {
                        out.close();
                    }
                } catch (IOException ignored) {
                }
                progressDialog.dismiss();
            }
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_RESTORE_BACKUP:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestBackupRestore();
                }
                break;
            case MY_PERMISSIONS_REQUEST_CREATE_BACKUP:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestExport();
                }
                break;
            case MY_PERMISSIONS_REQUEST_EXPORT:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    requestExport();
                }
                break;
        }
    }

    /**
     * The view for an individual activity in the list.
     */
    private class ActivityView extends LinearLayout {

        /**
         * The view of the activity name displayed in the list
         */
        private final TextView name;
        /**
         * The view of the total time of the activity.
         */
        private final TextView total;
        private final ImageView checkMark;

        public ActivityView(Context context, Activity t) {
            super(context);
            setOrientation(LinearLayout.HORIZONTAL);
            setPadding(5, 10, 5, 10);

            name = new TextView(context);
            name.setTextSize(fontSize);
            name.setText(t.getName());
            addView(name, new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 1f));

            checkMark = new ImageView(context);
            checkMark.setImageResource(R.drawable.ic_check_mark_dark);
            checkMark.setVisibility(View.INVISIBLE);
            addView(checkMark, new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 0f));

            total = new TextView(context);
            total.setTextSize(fontSize);
            total.setGravity(Gravity.RIGHT);
            total.setTransformationMethod(SingleLineTransformationMethod.getInstance());
            total.setText(formatTotal(decimalFormat, t.getTotal(), 0));
            addView(total, new LinearLayout.LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 0f));

            setGravity(Gravity.TOP);
            markupSelectedActivity(t);
        }

        public void setActivity(Activity t) {
            name.setTextSize(fontSize);
            total.setTextSize(fontSize);
            name.setText(t.getName());
            total.setText(formatTotal(decimalFormat, t.getTotal(), 0));
            markupSelectedActivity(t);
        }

        private void markupSelectedActivity(Activity t) {
            if (t.isRunning()) {
                checkMark.setVisibility(View.VISIBLE);
            } else {
                checkMark.setVisibility(View.INVISIBLE);
            }
        }
    }
    private static final long MS_H = 3600000;
    private static final long MS_M = 60000;
    private static final long MS_S = 1000;
    private static final double D_M = 10.0 / 6.0;
    private static final double D_S = 1.0 / 36.0;

    /*
     * This is pretty stupid, but because Java doesn't support closures, we have
     * to add extra overhead (more method indirection; method calls are relatively 
     * expensive) if we want to re-use code.  Notice that a call to this method
     * actually filters down through four methods before it returns.
     */
    static String formatTotal(boolean decimalFormat, long ttl, long roundMinutes) {
        return formatTotal(decimalFormat, FORMAT, ttl, roundMinutes);
    }

    static String formatTotal(boolean decimalFormat, String format, long ttl, long roundMinutes) {
        if (roundMinutes > 0) {
            long totalMinutes = ttl / MS_M;
            ttl = roundMinutes * Math.round((float) totalMinutes / roundMinutes) * MS_M;
        }
        long hours = ttl / MS_H;
        long hours_in_ms = hours * MS_H;
        long minutes = (ttl - hours_in_ms) / MS_M;
        long minutes_in_ms = minutes * MS_M;
        long seconds = (ttl - hours_in_ms - minutes_in_ms) / MS_S;
        return formatTotal(decimalFormat, format, hours, minutes, seconds, roundMinutes);
    }

    static String formatTotal(boolean decimalFormat, long hours, long minutes, long seconds, long roundMinutes) {
        return formatTotal(decimalFormat, FORMAT, hours, minutes, seconds, roundMinutes);
    }

    static String formatTotal(boolean decimalFormat, String format, long hours, long minutes, long seconds, long roundMinutes) {
        if (decimalFormat) {
            format = DECIMAL_FORMAT;
            minutes = Math.round((D_M * minutes) + (D_S * seconds));
            seconds = 0;
        }
        return String.format(format, hours, minutes, seconds);
    }

    private class ActivityAdapter extends BaseAdapter {

        private final DBHelper dbHelper;
        protected ArrayList<Activity> activities;
        private final Context savedContext;
        private long currentRangeStart;
        private long currentRangeEnd;

        public ActivityAdapter(Context c) {
            savedContext = c;
            dbHelper = new DBHelper(c);
            dbHelper.getWritableDatabase();
            activities = new ArrayList<Activity>();
        }

        public void close() {
            dbHelper.close();
        }

        /**
         * Loads all activities.
         */
        private void loadActivities() {
            currentRangeStart = currentRangeEnd = -1;
            loadActivities("", true);
        }

        protected void loadActivity(Calendar day) {
            loadActivities(day, (Calendar) day.clone());
        }

        protected void loadActivities(Calendar start, Calendar end) {
            String[] res = makeWhereClause(start, end);
            loadActivities(res[0], res[1] != null);
        }

        /**
         * Java doesn't understand tuples, so the return value of this is a
         * hack.
         *
         * @param start
         * @param end
         * @return a String pair hack, where the second item is null for false,
         * and non-null for true
         */
        private String[] makeWhereClause(Calendar start, Calendar end) {
            String query = "AND " + START + " < %d AND " + START + " >= %d";
            Calendar today = Calendar.getInstance();
            today.setFirstDayOfWeek(preferences.getInt(START_DAY, 0) + 1);
            today.set(Calendar.HOUR_OF_DAY, 12);
            for (int field : new int[]{Calendar.HOUR_OF_DAY, Calendar.MINUTE,
                Calendar.SECOND,
                Calendar.MILLISECOND}) {
                for (Calendar d : new Calendar[]{today, start, end}) {
                    d.set(field, d.getMinimum(field));
                }
            }
            end.add(Calendar.DAY_OF_MONTH, 1);
            currentRangeStart = start.getTimeInMillis();
            currentRangeEnd = end.getTimeInMillis();
            boolean loadCurrentActivity = today.compareTo(start) != -1
                    && today.compareTo(end) != 1;
            query = String.format(query, end.getTimeInMillis(), start.getTimeInMillis());
            return new String[]{query, loadCurrentActivity ? query : null};
        }

        /**
         * Load activities, given a filter. This overwrites any currently loaded
         * activites in the "tasks" data structure.
         *
         * @param whereClause A SQL where clause limiting the range of dates to
         * load. This must be a clause against the ranges table.
         * @param loadCurrent Whether or not to include data for currently
         * active activities.
         */
        private void loadActivities(String whereClause, boolean loadCurrent) {
            activities.clear();

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.query(ACTIVITY_TABLE, ACTIVITY_COLUMNS, null, null, null, null, null);

            Activity t;
            if (c.moveToFirst()) {
                do {
                    int tid = c.getInt(0);
                    String[] tids = {String.valueOf(tid)};
                    t = new Activity(c.getString(1), tid);
                    Cursor r = db.rawQuery("SELECT SUM(end) - SUM(start) AS total FROM " + RANGES_TABLE + " WHERE " + ACTIVITY_ID + " = ? AND end NOTNULL " + whereClause, tids);
                    if (r.moveToFirst()) {
                        t.setCollapsed(r.getLong(0));
                    }
                    r.close();
                    if (loadCurrent) {
                        r = db.query(RANGES_TABLE, RANGE_COLUMNS,
                                ACTIVITY_ID + " = ? AND end ISNULL",
                                tids, null, null, null);
                        if (r.moveToFirst()) {
                            t.setStartTime(r.getLong(0));
                        }
                        r.close();
                    }
                    activities.add(t);
                } while (c.moveToNext());
            }
            c.close();
            Collections.sort(activities);
            running = !findCurrentlyActive().isEmpty();
            notifyDataSetChanged();
        }

        /**
         * Don't forget to close the cursor!!
         *
         * @return
         */
        protected Cursor getCurrentRange() {
            String[] res = {""};
            if (currentRangeStart != -1 && currentRangeEnd != -1) {
                Calendar start = Calendar.getInstance();
                start.setFirstDayOfWeek(preferences.getInt(START_DAY, 0) + 1);
                start.setTimeInMillis(currentRangeStart);
                Calendar end = Calendar.getInstance();
                end.setFirstDayOfWeek(preferences.getInt(START_DAY, 0) + 1);
                end.setTimeInMillis(currentRangeEnd);
                res = makeWhereClause(start, end);
            }
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor r = db.rawQuery("SELECT t.name, r.start, r.end "
                    + " FROM " + ACTIVITY_TABLE + " t, " + RANGES_TABLE + " r "
                    + " WHERE r." + ACTIVITY_ID + " = t.ROWID " + res[0]
                    + " ORDER BY t.name, r.start ASC", null);
            return r;
        }

        public List<Activity> findCurrentlyActive() {
            List<Activity> activeActivities = new ArrayList<Activity>();
            for (Activity a : activities) {
                if (a.isRunning()) {
                    activeActivities.add(a);
                }
            }
            return activeActivities;
        }

        protected void addActivity(String name) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(NAME, name);
            long id = db.insert(ACTIVITY_TABLE, NAME, values);
            Activity t = new Activity(name, (int) id);
            activities.add(t);
            Collections.sort(activities);
            notifyDataSetChanged();
        }

        protected void updateActivity(Activity t) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(NAME, t.getName());
            String id = String.valueOf(t.getId());
            String[] vals = {id};
            db.update(ACTIVITY_TABLE, values, "ROWID = ?", vals);

            if (t.getStartTime() != NULL) {
                values.clear();
                long startTime = t.getStartTime();
                values.put(START, startTime);
                vals = new String[]{id, String.valueOf(startTime)};
                if (t.getEndTime() != NULL) {
                    values.put(END, t.getEndTime());
                }
                // If an update fails, then this is an insert
                if (db.update(RANGES_TABLE, values, ACTIVITY_ID + " = ? AND " + START + " = ?", vals) == 0) {
                    values.put(ACTIVITY_ID, t.getId());
                    db.insert(RANGES_TABLE, END, values);
                }
            }

            Collections.sort(activities);
            notifyDataSetChanged();
        }

        public void deleteActivity(Activity t) {
            activities.remove(t);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String[] id = {String.valueOf(t.getId())};
            db.delete(ACTIVITY_TABLE, "ROWID = ?", id);
            db.delete(RANGES_TABLE, ACTIVITY_ID + " = ?", id);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return activities.size();
        }

        @Override
        public Object getItem(int position) {
            return activities.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ActivityView view = null;
            if (convertView == null) {
                Object item = getItem(position);
                if (item != null) {
                    view = new ActivityView(savedContext, (Activity) item);
                }
            } else {
                view = (ActivityView) convertView;
                Object item = getItem(position);
                if (item != null) {
                    view.setActivity((Activity) item);
                }
            }
            return view;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        if (vibrateClick) {
            vibrateAgent.vibrate(100);
        }
        if (playClick) {
            try {
                //clickPlayer.prepare();
                clickPlayer.start();
            } catch (IllegalStateException exception) {
                // Ignore this; it is probably because the media isn't yet ready.
                // There's nothing the user can do about it.
                // ignore this.  There's nothing the user can do about it.
                Logger.getLogger("TimeTracker").log(Level.INFO,
                        "Failed to play audio: {0}", exception.getMessage());
            }
        }

        // Stop the update.  If a activity is already running and we're stopping
        // the timer, it'll stay stopped.  If a activity is already running and 
        // we're switching to a new activity, or if nothing is running and we're
        // starting a new timer, then it'll be restarted.

        Object item = getListView().getItemAtPosition(position);
        if (item != null) {
            Activity selected = (Activity) item;
            if (!concurrency) {
                boolean startSelected = !selected.isRunning();
                if (running) {
                    running = false;
                    timer.removeCallbacks(updater);
                    // Disable currently running activities
                    for (Activity a : adapter.findCurrentlyActive()) {
                        a.stop();
                        adapter.updateActivity(a);
                    }
                }
                if (startSelected) {
                    selected.start();
                    running = true;
                    timer.post(updater);
                }
            } else {
                if (selected.isRunning()) {
                    selected.stop();
                    running = !adapter.findCurrentlyActive().isEmpty();
                    if (!running) {
                        timer.removeCallbacks(updater);
                    }
                } else {
                    selected.start();
                    if (!running) {
                        running = true;
                        timer.post(updater);
                    }
                }
            }
            adapter.updateActivity(selected);
        }
        getListView().invalidate();
        super.onListItemClick(l, v, position, id);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PREFERENCES && data != null) {
            Bundle extras = data.getExtras();
            if (extras.getBoolean(START_DAY)) {
                switchView(preferences.getInt(VIEW_MODE, 0));
            }
            if (extras.getBoolean(CONCURRENT)) {
                concurrency = preferences.getBoolean(CONCURRENT, false);
            }
            if (extras.getBoolean(SOUND)) {
                playClick = preferences.getBoolean(SOUND, false);
                if (playClick && clickPlayer == null) {
                    clickPlayer = MediaPlayer.create(this, R.raw.click);
                    try {
                        clickPlayer.prepareAsync();
                        clickPlayer.setVolume(1, 1);
                    } catch (IllegalStateException illegalStateException) {
                        // ignore this.  There's nothing the user can do about it.
                        Logger.getLogger("TimeTracker").log(Level.SEVERE,
                                "Failed to set up audio player: {0}",
                                illegalStateException.getMessage());
                    }
                }
            }
            if (extras.getBoolean(VIBRATE)) {
                vibrateClick = preferences.getBoolean(VIBRATE, true);
            }
            if (extras.getBoolean(FONTSIZE)) {
                fontSize = preferences.getInt(FONTSIZE, 16);
            }
            if (extras.getBoolean(TIMEDISPLAY)) {
                decimalFormat = preferences.getBoolean(TIMEDISPLAY, false);
            }
        }

        if (getListView() != null) {
            getListView().invalidate();
        }
    }

    protected void finishedCopy(DBBackup.Result result, String message, int success_string, int fail_string) {
        if (result == DBBackup.Result.SUCCESS) {
            switchView(preferences.getInt(VIEW_MODE, 0));
            message = dbBackup.getAbsolutePath();
        }
        perform(message, success_string, fail_string);
    }

}
