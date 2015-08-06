/*
 * A Time Tracker - Open Source Time Tracker for Android
 *
 * Copyright (C) 2013, 2014, 2015  Markus Kilås <markus@markuspage.com>
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

import static com.markuspage.android.atimetracker.DBHelper.END;
import static com.markuspage.android.atimetracker.DBHelper.NAME;
import static com.markuspage.android.atimetracker.DBHelper.RANGES_TABLE;
import static com.markuspage.android.atimetracker.DBHelper.RANGE_COLUMNS;
import static com.markuspage.android.atimetracker.DBHelper.START;
import static com.markuspage.android.atimetracker.DBHelper.TASK_COLUMNS;
import static com.markuspage.android.atimetracker.DBHelper.TASK_ID;
import static com.markuspage.android.atimetracker.DBHelper.TASK_TABLE;
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
import java.util.Iterator;
import java.util.NoSuchElementException;
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
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Vibrator;
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

/**
 * Manages and displays a list of tasks, providing the ability to edit and
 * display individual task items.
 *
 * @author ser
 */
public class Tasks extends ListActivity {

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
    /**
     * Defines how each task's time is displayed
     */
    private static final String FORMAT = "%02d:%02d";
    private static final String DECIMAL_FORMAT = "%02d.%02d";
    /**
     * How often to refresh the display, in milliseconds
     */
    private static final int REFRESH_MS = 60000;
    /**
     * The model for this view
     */
    private TaskAdapter adapter;
    /**
     * A timer for refreshing the display.
     */
    private Handler timer;
    /**
     * The call-back that actually updates the display.
     */
    private TimerTask updater;
    /**
     * The currently active task (the one that is currently being timed). There
     * can be only one.
     */
    private boolean running = false;
    /**
     * The currently selected task when the context menu is invoked.
     */
    private Task selectedTask;
    private SharedPreferences preferences;
    private static int fontSize = 16;
    private boolean concurrency;
    private static MediaPlayer clickPlayer;
    private boolean playClick = false;
    private boolean vibrateClick = true;
    private Vibrator vibrateAgent;
    private ProgressDialog progressDialog = null;
    private boolean decimalFormat = false;
    /**
     * A list of menu options, including both context and options menu items
     */
    protected static final int ADD_TASK = 0,
            EDIT_TASK = 1, DELETE_TASK = 2, REPORT = 3, SHOW_TIMES = 4,
            CHANGE_VIEW = 5, SELECT_START_DATE = 6, SELECT_END_DATE = 7,
            HELP = 8, EXPORT_VIEW = 9, SUCCESS_DIALOG = 10, ERROR_DIALOG = 11,
            SET_WEEK_START_DAY = 12, MORE = 13, BACKUP = 14, PREFERENCES = 15,
            PROGRESS_DIALOG = 16;
    // TODO: This could be done better...
    private static final String dbPath = "/data/data/com.markuspage.android.atimetracker/databases/timetracker.db";
    private final File dbBackup = new File(Environment.getExternalStorageDirectory(), "timetracker.db");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //android.os.Debug.waitForDebugger();
        preferences = getSharedPreferences(TIMETRACKERPREF, MODE_PRIVATE);
        fontSize = preferences.getInt(FONTSIZE, 16);
        concurrency = preferences.getBoolean(CONCURRENT, false);
        if (preferences.getBoolean(MILITARY, true)) {
            TimeRange.FORMAT = new SimpleDateFormat("HH:mm");
        } else {
            TimeRange.FORMAT = new SimpleDateFormat("hh:mm a");
        }

        int which = preferences.getInt(VIEW_MODE, 0);
        if (adapter == null) {
            adapter = new TaskAdapter(this);
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
                        Tasks.this.getListView().invalidate();
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
                        "Failed to set up audio player: "
                        + illegalStateException.getMessage());
            }
        }
        decimalFormat = preferences.getBoolean(TIMEDISPLAY, false);
        registerForContextMenu(getListView());
        if (adapter.tasks.isEmpty()) {
            showDialog(HELP);
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
        menu.add(0, ADD_TASK, 0, R.string.add_task_title).setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, REPORT, 1, R.string.generate_report_title).setIcon(android.R.drawable.ic_menu_week);
        menu.add(0, MORE, 2, R.string.more).setIcon(android.R.drawable.ic_menu_more);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Task menu");
        menu.add(0, EDIT_TASK, 0, getText(R.string.edit_task));
        menu.add(0, DELETE_TASK, 0, getText(R.string.delete_task));
        menu.add(0, SHOW_TIMES, 0, getText(R.string.show_times));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        selectedTask = (Task) adapter.getItem((int) info.id);
        switch (item.getItemId()) {
            case SHOW_TIMES:
                Intent intent = new Intent(this, TaskTimes.class);
                intent.putExtra(TASK_ID, selectedTask.getId());
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
            case ADD_TASK:
            case MORE:
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
            case ADD_TASK:
                return openNewTaskDialog();
            case EDIT_TASK:
                return openEditTaskDialog();
            case DELETE_TASK:
                return openDeleteTaskDialog();
            case CHANGE_VIEW:
                return openChangeViewDialog();
            case HELP:
                return openAboutDialog();
            case SUCCESS_DIALOG:
                operationSucceed = new AlertDialog.Builder(Tasks.this)
                        .setTitle(R.string.success)
                        .setIcon(android.R.drawable.stat_notify_sdcard)
                        .setMessage(exportMessage)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
                return operationSucceed;
            case ERROR_DIALOG:
                operationFailed = new AlertDialog.Builder(Tasks.this)
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
            case MORE:
                return new AlertDialog.Builder(Tasks.this).setItems(R.array.moreMenu, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        DBBackup backup;
                        System.err.println("IN CLICK");
                        switch (which) {
                            case 0: // CHANGE_VIEW:
                                showDialog(CHANGE_VIEW);
                                break;
                            case 1: // EXPORT_VIEW:
                                String fname = export();
                                perform(fname, R.string.export_csv_success, R.string.export_csv_fail);
                                break;
                            case 2: // COPY DB TO SD
                                showDialog(Tasks.PROGRESS_DIALOG);
                                if (dbBackup.exists()) {
                                    // Find the database
                                    SQLiteDatabase backupDb = SQLiteDatabase.openDatabase(dbBackup.getAbsolutePath(), null, SQLiteDatabase.OPEN_READWRITE);
                                    SQLiteDatabase appDb = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READONLY);
                                    backup = new DBBackup(Tasks.this, progressDialog, R.string.backup_success, R.string.backup_failed);
                                    backup.execute(appDb, backupDb);
                                } else {
                                    InputStream in = null;
                                    OutputStream out = null;

                                    try {
                                        in = new BufferedInputStream(new FileInputStream(dbPath));
                                        out = new BufferedOutputStream(new FileOutputStream(dbBackup));
                                        for (int c = in.read(); c != -1; c = in.read()) {
                                            out.write(c);
                                        }
                                        finishedCopy(DBBackup.Result.SUCCESS, dbBackup.getAbsolutePath(), R.string.backup_success, R.string.backup_failed);
                                    } catch (Exception ex) {
                                        Logger.getLogger(Tasks.class.getName()).log(Level.SEVERE, null, ex);
                                        exportMessage = ex.getLocalizedMessage();
                                        showDialog(ERROR_DIALOG);
                                    } finally {
                                        progressDialog.dismiss();
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
                                    }
                                }
                                break;
                            case 3: // RESTORE FROM BACKUP
                                if (dbBackup.exists()) {
                                    try {
                                        showDialog(Tasks.PROGRESS_DIALOG);
                                        SQLiteDatabase backupDb = SQLiteDatabase.openDatabase(dbBackup.getAbsolutePath(), null, SQLiteDatabase.OPEN_READONLY);
                                        SQLiteDatabase appDb = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
                                        backup = new DBBackup(Tasks.this, progressDialog, R.string.restore_success, R.string.restore_failed);
                                        backup.execute(backupDb, appDb);
                                    } catch (Exception ex) {
                                        Logger.getLogger(Tasks.class.getName()).log(Level.SEVERE, null, ex);
                                        exportMessage = ex.getLocalizedMessage();
                                        showDialog(ERROR_DIALOG);
                                    }
                                } else {
                                    Logger.getLogger(Tasks.class.getName()).log(Level.SEVERE, "Backup file does not exist: " + dbBackup.getAbsolutePath());
                                    exportMessage = getString(R.string.restore_failed, "No backup file: " + dbBackup.getAbsolutePath());
                                    showDialog(ERROR_DIALOG);
                                }
                                break;
                            case 4: // PREFERENCES
                                Intent intent = new Intent(Tasks.this, Settings.class);
                                startActivityForResult(intent, PREFERENCES);
                                break;
                            case 5: // HELP:
                                showDialog(HELP);
                                break;
                            default:
                                break;
                        }
                    }
                }).create();
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
     * Creates a progressDialog to change the dates for which task times are
     * shown. Offers a short selection of pre-defined defaults, and the option
     * to choose a range from a progressDialog.
     *
     * @see arrays.xml
     * @return the progressDialog to be displayed
     */
    private Dialog openChangeViewDialog() {
        return new AlertDialog.Builder(Tasks.this).setItems(R.array.views, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor ed = preferences.edit();
                ed.putInt(VIEW_MODE, which);
                ed.commit();
                if (which == 5) {
                    Calendar calInstance = Calendar.getInstance();
                    new DatePickerDialog(Tasks.this,
                            new DatePickerDialog.OnDateSetListener() {
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

                            new DatePickerDialog(Tasks.this,
                                    new DatePickerDialog.OnDateSetListener() {
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
                                    Tasks.this.switchView(5);  // Update the list view
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
                adapter.loadTasks(tw);
                break;
            case 1: // this week
                adapter.loadTasks(weekStart(tw, startDay), weekEnd(tw, startDay));
                break;
            case 2: // yesterday
                tw.add(Calendar.DAY_OF_MONTH, -1);
                adapter.loadTasks(tw);
                break;
            case 3: // last week
                tw.add(Calendar.WEEK_OF_YEAR, -1);
                adapter.loadTasks(weekStart(tw, startDay), weekEnd(tw, startDay));
                break;
            case 4: // all
                adapter.loadTasks();
                break;
            case 5: // select range
                Calendar start = Calendar.getInstance();
                start.setTimeInMillis(preferences.getLong(START_DATE, 0));
                System.err.println("START = " + start.getTime());
                Calendar end = Calendar.getInstance();
                end.setTimeInMillis(preferences.getLong(END_DATE, 0));
                System.err.println("END = " + end.getTime());
                adapter.loadTasks(start, end);
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
        for (Task t : adapter.tasks) {
            total += t.getTotal();
        }
        setTitle(baseTitle + " " + formatTotal(decimalFormat, total, 0));
    }

    /**
     * Constructs a progressDialog for defining a new task. If accepted, creates
     * a new task. If cancelled, closes the progressDialog with no affect.
     *
     * @return the progressDialog to display
     */
    private Dialog openNewTaskDialog() {
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.edit_task, null);
        return new AlertDialog.Builder(Tasks.this) //.setIcon(R.drawable.alert_dialog_icon)
                .setTitle(R.string.add_task_title).setView(textEntryView).setPositiveButton(R.string.add_task_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                EditText textView = (EditText) textEntryView.findViewById(R.id.task_edit_name_edit);
                String name = textView.getText().toString();
                adapter.addTask(name);
                Tasks.this.getListView().invalidate();
            }
        }).setNegativeButton(android.R.string.cancel, null).create();
    }

    /**
     * Constructs a progressDialog for editing task attributes. If accepted,
     * alters the task being edited. If cancelled, dismissed the progressDialog
     * with no effect.
     *
     * @return the progressDialog to display
     */
    private Dialog openEditTaskDialog() {
        if (selectedTask == null) {
            return null;
        }
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.edit_task, null);
        return new AlertDialog.Builder(Tasks.this).setView(textEntryView).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                EditText textView = (EditText) textEntryView.findViewById(R.id.task_edit_name_edit);
                String name = textView.getText().toString();
                selectedTask.setTaskName(name);

                adapter.updateTask(selectedTask);

                Tasks.this.getListView().invalidate();
            }
        }).setNegativeButton(android.R.string.cancel, null).create();
    }

    /**
     * Constructs a progressDialog asking for confirmation for a delete request.
     * If accepted, deletes the task. If cancelled, closes the progressDialog.
     *
     * @return the progressDialog to display
     */
    private Dialog openDeleteTaskDialog() {
        if (selectedTask == null) {
            return null;
        }
        String formattedMessage = getString(R.string.delete_task_message,
                selectedTask.getTaskName());
        return new AlertDialog.Builder(Tasks.this).setTitle(R.string.delete_task_title).setIcon(android.R.drawable.stat_sys_warning).setCancelable(true).setMessage(formattedMessage).setPositiveButton(R.string.delete_ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                adapter.deleteTask(selectedTask);
                Tasks.this.getListView().invalidate();
            }
        }).setNegativeButton(android.R.string.cancel, null).create();
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
        try {
            OutputStream out = new FileOutputStream(fout);
            Cursor currentRange = adapter.getCurrentRange();
            CSVExporter.exportRows(out, currentRange);
            currentRange.close();

            return fname;
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace(System.err);
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

    private Dialog openAboutDialog() {
        String versionName = "";
        try {
            PackageInfo pkginfo = this.getPackageManager().getPackageInfo("com.markuspage.android.atimetracker", 0);
            versionName = pkginfo.versionName;
        } catch (NameNotFoundException nnfe) {
            // Denada
        }

        String formattedVersion = getString(R.string.version, versionName);

        LayoutInflater factory = LayoutInflater.from(this);
        View about = factory.inflate(R.layout.about, null);

        TextView version = (TextView) about.findViewById(R.id.version);
        version.setText(formattedVersion);
        TextView links = (TextView) about.findViewById(R.id.usage);
        Linkify.addLinks(links, Linkify.ALL);
        links = (TextView) about.findViewById(R.id.credits);
        Linkify.addLinks(links, Linkify.ALL);

        return new AlertDialog.Builder(Tasks.this).setView(about).setPositiveButton(android.R.string.ok, null).create();
    }

    @Override
    protected void onPrepareDialog(int id, Dialog d) {
        EditText textView;
        switch (id) {
            case ADD_TASK:
                textView = (EditText) d.findViewById(R.id.task_edit_name_edit);
                textView.setText("");
                break;
            case EDIT_TASK:
                textView = (EditText) d.findViewById(R.id.task_edit_name_edit);
                textView.setText(selectedTask.getTaskName());
                break;
            default:
                break;
        }
    }

    /**
     * The view for an individial task in the list.
     */
    private class TaskView extends LinearLayout {

        /**
         * The view of the task name displayed in the list
         */
        private TextView taskName;
        /**
         * The view of the total time of the task.
         */
        private TextView total;
        private ImageView checkMark;

        public TaskView(Context context, Task t) {
            super(context);
            setOrientation(LinearLayout.HORIZONTAL);
            setPadding(5, 10, 5, 10);

            taskName = new TextView(context);
            taskName.setTextSize(fontSize);
            taskName.setText(t.getTaskName());
            addView(taskName, new LinearLayout.LayoutParams(
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
            markupSelectedTask(t);
        }

        public void setTask(Task t) {
            taskName.setTextSize(fontSize);
            total.setTextSize(fontSize);
            taskName.setText(t.getTaskName());
            total.setText(formatTotal(decimalFormat, t.getTotal(), 0));
            markupSelectedTask(t);
        }

        private void markupSelectedTask(Task t) {
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

    private class TaskAdapter extends BaseAdapter {

        private DBHelper dbHelper;
        protected ArrayList<Task> tasks;
        private Context savedContext;
        private long currentRangeStart;
        private long currentRangeEnd;

        public TaskAdapter(Context c) {
            savedContext = c;
            dbHelper = new DBHelper(c);
            dbHelper.getWritableDatabase();
            tasks = new ArrayList<Task>();
        }

        public void close() {
            dbHelper.close();
        }

        /**
         * Loads all tasks.
         */
        private void loadTasks() {
            currentRangeStart = currentRangeEnd = -1;
            loadTasks("", true);
        }

        protected void loadTasks(Calendar day) {
            loadTasks(day, (Calendar) day.clone());
        }

        protected void loadTasks(Calendar start, Calendar end) {
            String[] res = makeWhereClause(start, end);
            loadTasks(res[0], res[1] == null ? false : true);
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
            boolean loadCurrentTask = today.compareTo(start) != -1
                    && today.compareTo(end) != 1;
            query = String.format(query, end.getTimeInMillis(), start.getTimeInMillis());
            return new String[]{query, loadCurrentTask ? query : null};
        }

        /**
         * Load tasks, given a filter. This overwrites any currently loaded
         * tasks in the "tasks" data structure.
         *
         * @param whereClause A SQL where clause limiting the range of dates to
         * load. This must be a clause against the ranges table.
         * @param loadCurrent Whether or not to include data for currently
         * active tasks.
         */
        private void loadTasks(String whereClause, boolean loadCurrent) {
            tasks.clear();

            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.query(TASK_TABLE, TASK_COLUMNS, null, null, null, null, null);

            Task t;
            if (c.moveToFirst()) {
                do {
                    int tid = c.getInt(0);
                    String[] tids = {String.valueOf(tid)};
                    t = new Task(c.getString(1), tid);
                    Cursor r = db.rawQuery("SELECT SUM(end) - SUM(start) AS total FROM " + RANGES_TABLE + " WHERE " + TASK_ID + " = ? AND end NOTNULL " + whereClause, tids);
                    if (r.moveToFirst()) {
                        t.setCollapsed(r.getLong(0));
                    }
                    r.close();
                    if (loadCurrent) {
                        r = db.query(RANGES_TABLE, RANGE_COLUMNS,
                                TASK_ID + " = ? AND end ISNULL",
                                tids, null, null, null);
                        if (r.moveToFirst()) {
                            t.setStartTime(r.getLong(0));
                        }
                        r.close();
                    }
                    tasks.add(t);
                } while (c.moveToNext());
            }
            c.close();
            Collections.sort(tasks);
            running = findCurrentlyActive().hasNext();
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
                    + " FROM " + TASK_TABLE + " t, " + RANGES_TABLE + " r "
                    + " WHERE r." + TASK_ID + " = t.ROWID " + res[0]
                    + " ORDER BY t.name, r.start ASC", null);
            return r;
        }

        public Iterator<Task> findCurrentlyActive() {
            return new Iterator<Task>() {
                Iterator<Task> iter = tasks.iterator();
                Task next = null;

                public boolean hasNext() {
                    if (next != null) {
                        return true;
                    }
                    while (iter.hasNext()) {
                        Task t = iter.next();
                        if (t.isRunning()) {
                            next = t;
                            return true;
                        }
                    }
                    return false;
                }

                public Task next() {
                    if (hasNext()) {
                        Task t = next;
                        next = null;
                        return t;
                    }
                    throw new NoSuchElementException();
                }

                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        protected void addTask(String taskName) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(NAME, taskName);
            long id = db.insert(TASK_TABLE, NAME, values);
            Task t = new Task(taskName, (int) id);
            tasks.add(t);
            Collections.sort(tasks);
            notifyDataSetChanged();
        }

        protected void updateTask(Task t) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(NAME, t.getTaskName());
            String id = String.valueOf(t.getId());
            String[] vals = {id};
            db.update(TASK_TABLE, values, "ROWID = ?", vals);

            if (t.getStartTime() != NULL) {
                values.clear();
                long startTime = t.getStartTime();
                values.put(START, startTime);
                vals = new String[]{id, String.valueOf(startTime)};
                if (t.getEndTime() != NULL) {
                    values.put(END, t.getEndTime());
                }
                // If an update fails, then this is an insert
                if (db.update(RANGES_TABLE, values, TASK_ID + " = ? AND " + START + " = ?", vals) == 0) {
                    values.put(TASK_ID, t.getId());
                    db.insert(RANGES_TABLE, END, values);
                }
            }

            Collections.sort(tasks);
            notifyDataSetChanged();
        }

        public void deleteTask(Task t) {
            tasks.remove(t);
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            String[] id = {String.valueOf(t.getId())};
            db.delete(TASK_TABLE, "ROWID = ?", id);
            db.delete(RANGES_TABLE, TASK_ID + " = ?", id);
            notifyDataSetChanged();
        }

        public int getCount() {
            return tasks.size();
        }

        public Object getItem(int position) {
            return tasks.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            TaskView view = null;
            if (convertView == null) {
                Object item = getItem(position);
                if (item != null) {
                    view = new TaskView(savedContext, (Task) item);
                }
            } else {
                view = (TaskView) convertView;
                Object item = getItem(position);
                if (item != null) {
                    view.setTask((Task) item);
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
            } catch (Exception exception) {
                // Ignore this; it is probably because the media isn't yet ready.
                // There's nothing the user can do about it.
                // ignore this.  There's nothing the user can do about it.
                Logger.getLogger("TimeTracker").log(Level.INFO,
                        "Failed to play audio: "
                        + exception.getMessage());
            }
        }

        // Stop the update.  If a task is already running and we're stopping
        // the timer, it'll stay stopped.  If a task is already running and 
        // we're switching to a new task, or if nothing is running and we're
        // starting a new timer, then it'll be restarted.

        Object item = getListView().getItemAtPosition(position);
        if (item != null) {
            Task selected = (Task) item;
            if (!concurrency) {
                boolean startSelected = !selected.isRunning();
                if (running) {
                    running = false;
                    timer.removeCallbacks(updater);
                    // Disable currently running tasks
                    for (Iterator<Task> iter = adapter.findCurrentlyActive();
                            iter.hasNext();) {
                        Task t = iter.next();
                        t.stop();
                        adapter.updateTask(t);
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
                    running = adapter.findCurrentlyActive().hasNext();
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
            adapter.updateTask(selected);
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
            if (extras.getBoolean(MILITARY)) {
                if (preferences.getBoolean(MILITARY, true)) {
                    TimeRange.FORMAT = new SimpleDateFormat("HH:mm");
                } else {
                    TimeRange.FORMAT = new SimpleDateFormat("hh:mm a");
                }
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
                                "Failed to set up audio player: "
                                + illegalStateException.getMessage());
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
