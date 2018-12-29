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

import static com.markuspage.android.atimetracker.DBHelper.END;
import static com.markuspage.android.atimetracker.DBHelper.RANGES_TABLE;
import static com.markuspage.android.atimetracker.DBHelper.RANGE_COLUMNS;
import static com.markuspage.android.atimetracker.DBHelper.START;
import static com.markuspage.android.atimetracker.EditTime.END_DATE;
import static com.markuspage.android.atimetracker.EditTime.START_DATE;
import static com.markuspage.android.atimetracker.TimeRange.NULL;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.method.SingleLineTransformationMethod;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import static com.markuspage.android.atimetracker.Activities.MILITARY;
import static com.markuspage.android.atimetracker.DBHelper.ACTIVITY_ID;
import static com.markuspage.android.atimetracker.DBHelper.ACTIVITY_NAME;

/**
 * Activity listing times in activity.
 *
 * @author Sean Russell, ser@germane-software.com
 */
public class ActivityTimes extends ListActivity implements DialogInterface.OnClickListener {

    private TimesAdapter adapter;
    private int fontSize;
    private static final int ADD_TIME = 0, DELETE_TIME = 2, EDIT_TIME = 3, MOVE_TIME = 4;
    private static final int SEP = -99;
    private boolean decimalFormat;
    private SimpleDateFormat timeFormat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences preferences = getSharedPreferences("timetracker.pref", MODE_PRIVATE);
        fontSize = preferences.getInt(Activities.FONTSIZE, 16);
        if (adapter == null) {
            adapter = new TimesAdapter(this);
            setListAdapter(adapter);
        }
        decimalFormat = preferences.getBoolean(Activities.TIMEDISPLAY, false);
        if (preferences.getBoolean(MILITARY, true)) {
            timeFormat = new SimpleDateFormat("HH:mm");
        } else {
            timeFormat = new SimpleDateFormat("hh:mm a");
        }

        registerForContextMenu(getListView());
        Bundle extras = getIntent().getExtras();
        if (extras.get(START) != null) {
            adapter.loadTimes(extras.getInt(ACTIVITY_ID),
                    extras.getLong(START),
                    extras.getLong(END));
        } else {
            adapter.loadTimes(extras.getInt(ACTIVITY_ID));
        }
    }

    @Override
    protected void onStop() {
        adapter.close();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        getListView().invalidate();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, ADD_TIME, 0, R.string.add_time_title).setIcon(android.R.drawable.ic_menu_add);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem i) {
        int id = i.getItemId();
        if (id == ADD_TIME) {
            Intent intent = new Intent(this, EditTime.class);
            intent.putExtra(EditTime.CLEAR, true);
            startActivityForResult(intent, id);
        }
        return super.onOptionsItemSelected(i);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        menu.setHeaderTitle("Time menu");
        menu.add(0, EDIT_TIME, 0, "Edit Time");
        menu.add(0, DELETE_TIME, 0, "Delete Time");
        menu.add(0, MOVE_TIME, 0, "Move Time");
    }
    private TimeRange selectedRange;

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        selectedRange = (TimeRange) adapter.getItem((int) info.id);
        int id = item.getItemId();
        action = id;
        switch (id) {
            case DELETE_TIME:
                showDialog(id);
                break;
            case EDIT_TIME:
                Intent intent = new Intent(this, EditTime.class);
                intent.putExtra(EditTime.START_DATE, selectedRange.getStart());
                intent.putExtra(EditTime.END_DATE, selectedRange.getEnd());
                startActivityForResult(intent, id);
                break;
            case MOVE_TIME:
                showDialog(id);
            default:
                break;
        }
        return super.onContextItemSelected(item);
    }
    private int action;

    @Override
    public void onClick(DialogInterface dialog, int whichButton) {
        switch (action) {
            case DELETE_TIME:
                adapter.deleteTimeRange(selectedRange);
                break;
            case MOVE_TIME:
                adapter.assignTimeToActivityAt(selectedRange, whichButton);
                break;
            default:
                break;
        }
        ActivityTimes.this.getListView().invalidate();
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DELETE_TIME:
                return new AlertDialog.Builder(this)
                        .setTitle(R.string.delete_time_title)
                        .setIcon(android.R.drawable.stat_sys_warning)
                        .setCancelable(true)
                        .setMessage(R.string.delete_time_message)
                        .setPositiveButton(R.string.delete_ok, this)
                        .setNegativeButton(android.R.string.cancel, null).create();
            case MOVE_TIME:
                return new AlertDialog.Builder(this)
                        .setCursor(adapter.getActivityNames(), this, ACTIVITY_NAME)
                        .create();
            default:
                break;
        }
        return null;
    }
    private static final DateFormat SEPFORMAT = new SimpleDateFormat("EEEE, MMM dd yyyy");

    private class TimesAdapter extends BaseAdapter {

        private final Context savedContext;
        private final DBHelper dbHelper;
        private final ArrayList<TimeRange> times;

        public TimesAdapter(Context c) {
            savedContext = c;
            dbHelper = new DBHelper(c);
            dbHelper.getWritableDatabase();
            times = new ArrayList<TimeRange>();
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int position) {
            return times.get(position).getEnd() != SEP;
        }

        public void close() {
            dbHelper.close();
        }

        public void deleteTimeRange(TimeRange range) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();

            String whereClause = START + " = ? AND " + ACTIVITY_ID + " = ?";
            String[] whereValues;
            if (range.getEnd() == NULL) {
                whereClause += " AND " + END + " ISNULL";
                whereValues = new String[]{
                    String.valueOf(range.getStart()),
                    String.valueOf(getIntent().getExtras().getInt(DBHelper.ACTIVITY_ID))
                };
            } else {
                whereClause += " AND " + END + " = ?";
                whereValues = new String[]{
                    String.valueOf(range.getStart()),
                    String.valueOf(getIntent().getExtras().getInt(DBHelper.ACTIVITY_ID)),
                    String.valueOf(range.getEnd())
                };
            }
            db.delete(RANGES_TABLE, whereClause, whereValues);
            int pos = times.indexOf(range);
            if (pos > -1) {
                times.remove(pos);
                // p-1 = sep && p = END ||
                // p-1 = sep && p+1 = END
                // But, by this time, p+1 is now p, because we've removed p
                if (pos != 0 && times.get(pos - 1).getEnd() == SEP
                        && (pos == times.size() || times.get(pos).getEnd() == SEP)) {
                    times.remove(pos - 1);
                }
            }
            notifyDataSetChanged();
        }

        protected void loadTimes(int selectedActivityId) {
            loadTimes(ACTIVITY_ID + " = ?",
                    new String[]{String.valueOf(selectedActivityId)});
        }

        protected void loadTimes(int selectedActivityId, long start, long end) {
            loadTimes(ACTIVITY_ID + " = ? AND " + START + " < ? AND " + START + " > ?",
                    new String[]{String.valueOf(selectedActivityId),
                String.valueOf(end),
                String.valueOf(start)});
        }

        protected void loadTimes(String where, String[] args) {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.query(RANGES_TABLE, RANGE_COLUMNS, where, args,
                    null, null, START + "," + END);
            if (c.moveToFirst()) {
                do {
                    times.add(new TimeRange(c.getLong(0),
                            c.isNull(1) ? NULL : c.getLong(1)));
                } while (c.moveToNext());
            }
            c.close();
            addSeparators();
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Object item = getItem(position);
            if (item == null) {
                return convertView;
            }
            TimeRange range = (TimeRange) item;
            if (range.getEnd() == SEP) {
                TextView headerText;
                if (convertView == null || !(convertView instanceof TextView)) {
                    headerText = new TextView(savedContext);
                    headerText.setTextSize(fontSize);
                    headerText.setTextColor(Color.YELLOW);
                    headerText.setTypeface(Typeface.DEFAULT, Typeface.ITALIC);
                    headerText.setText(SEPFORMAT.format(new Date(range.getStart())));
                } else {
                    headerText = (TextView) convertView;
                }
                headerText.setText(SEPFORMAT.format(new Date(range.getStart())));
                return headerText;
            }
            TimeView timeView;
            if (convertView == null || !(convertView instanceof TimeView)) {
                timeView = new TimeView(savedContext, (TimeRange) item);
            } else {
                timeView = (TimeView) convertView;
            }
            timeView.setTimeRange((TimeRange) item);
            return timeView;
        }

        @Override
        public int getCount() {
            return times.size();
        }

        @Override
        public Object getItem(int position) {
            return times.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        private void assignTimeToActivityAt(TimeRange range, int which) {
            Cursor c = getActivityNames();
            if (c.moveToFirst()) {
                while (which > 0) {
                    c.moveToNext();
                    which--;
                }
            }
            int newActivityId = c.getInt(0);
            if (!c.isAfterLast()) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();

                String whereClause = START + " = ? AND " + ACTIVITY_ID + " = ?";
                String[] whereValues;
                if (range.getEnd() == NULL) {
                    whereClause += " AND " + END + " ISNULL";
                    whereValues = new String[]{
                        String.valueOf(range.getStart()),
                        String.valueOf(getIntent().getExtras().getInt(DBHelper.ACTIVITY_ID))
                    };
                } else {
                    whereClause += " AND " + END + " = ?";
                    whereValues = new String[]{
                        String.valueOf(range.getStart()),
                        String.valueOf(getIntent().getExtras().getInt(DBHelper.ACTIVITY_ID)),
                        String.valueOf(range.getEnd())
                    };
                }
                ContentValues values = new ContentValues();
                values.put(ACTIVITY_ID, newActivityId);
                db.update(RANGES_TABLE, values, whereClause, whereValues);
                int pos = times.indexOf(range);
                times.remove(pos);
                if (pos != 0 && times.get(pos - 1).getEnd() == SEP
                        && (pos == times.size() || times.get(pos).getEnd() == SEP)) {
                    times.remove(pos - 1);
                }
                notifyDataSetChanged();
            }
            c.close();
        }

        private final class TimeView extends LinearLayout {

            private final TextView dateRange;
            private final TextView total;

            public TimeView(Context context, TimeRange t) {
                super(context);
                setOrientation(LinearLayout.HORIZONTAL);
                setPadding(5, 10, 5, 10);

                dateRange = new TextView(context);
                dateRange.setTextSize(fontSize);
                addView(dateRange, new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 1f));

                total = new TextView(context);
                total.setTextSize(fontSize);
                total.setGravity(Gravity.RIGHT);
                total.setTransformationMethod(SingleLineTransformationMethod.getInstance());
                addView(total, new LinearLayout.LayoutParams(
                        LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 0.0f));

                setTimeRange(t);
            }

            public void setTimeRange(TimeRange t) {
                dateRange.setText(t.format(timeFormat));
                total.setText(Activities.formatTotal(decimalFormat, t.getTotal(), 0));
                /* If the following is added, then the timer to update the
                 * display must also be added
                 if (t.getEnd() == NULL) {
                 dateRange.getPaint().setShadowLayer(1, 1, 1,Color.YELLOW);
                 total.getPaint().setShadowLayer(1, 1, 1, Color.YELLOW);
                 } else {
                 dateRange.getPaint().clearShadowLayer();
                 total.getPaint().clearShadowLayer();
                 }
                 */
            }
        }

        public void clear() {
            times.clear();
        }

        public void addTimeRange(long sd, long ed) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(ACTIVITY_ID, getIntent().getExtras().getInt(ACTIVITY_ID));
            values.put(START, sd);
            values.put(END, ed);
            db.insert(RANGES_TABLE, END, values);
            insert(times, new TimeRange(sd, ed));
            notifyDataSetChanged();
        }

        // Inserts an item into the list in order.  Why Java doesn't provide
        // this is beyond me.
        private void insert(ArrayList<TimeRange> list, TimeRange item) {
            int insertPoint = 0;
            for (; insertPoint < list.size(); insertPoint++) {
                if (list.get(insertPoint).compareTo(item) != -1) {
                    break;
                }
            }
            list.add(insertPoint, item);
            if (insertPoint > 0) {
                Calendar c = Calendar.getInstance();
                c.setFirstDayOfWeek(Calendar.MONDAY);
                TimeRange prev = list.get(insertPoint - 1);
                c.setTimeInMillis(prev.getStart());
                int pyear = c.get(Calendar.YEAR),
                        pday = c.get(Calendar.DAY_OF_YEAR);
                c.setTimeInMillis(item.getStart());
                if (pday != c.get(Calendar.DAY_OF_YEAR)
                        || pyear != c.get(Calendar.YEAR)) {
                    times.add(insertPoint, new TimeRange(startOfDay(item.getStart()), SEP));
                }
            } else {
                times.add(insertPoint, new TimeRange(startOfDay(item.getStart()), SEP));
            }
        }

        private long startOfDay(long start) {
            Calendar cal = Calendar.getInstance();
            cal.setFirstDayOfWeek(Calendar.MONDAY);
            cal.setTimeInMillis(start);
            cal.set(Calendar.HOUR_OF_DAY, cal.getMinimum(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, cal.getMinimum(Calendar.MINUTE));
            cal.set(Calendar.SECOND, cal.getMinimum(Calendar.SECOND));
            cal.set(Calendar.MILLISECOND, cal.getMinimum(Calendar.MILLISECOND));
            return cal.getTimeInMillis();
        }

        private void addSeparators() {
            int dayOfYear = -1, year = -1;
            Calendar curDay = Calendar.getInstance();
            curDay.setFirstDayOfWeek(Calendar.MONDAY);
            for (int i = 0; i < times.size(); i++) {
                TimeRange tr = times.get(i);
                curDay.setTimeInMillis(tr.getStart());
                int doy = curDay.get(Calendar.DAY_OF_YEAR);
                int y = curDay.get(Calendar.YEAR);
                if (doy != dayOfYear || y != year) {
                    dayOfYear = doy;
                    year = y;
                    times.add(i, new TimeRange(startOfDay(tr.getStart()), SEP));
                    i++;
                }
            }
        }

        public void updateTimeRange(long sd, long ed, int newActivityId, TimeRange old) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(START, sd);
            int currentActivityId = getIntent().getExtras().getInt(ACTIVITY_ID);
            String whereClause = START + "=? AND " + ACTIVITY_ID + "=?";
            String[] whereValues;
            if (ed != NULL) {
                values.put(END, ed);
                whereClause += " AND " + END + "=?";
                whereValues = new String[]{String.valueOf(old.getStart()),
                    String.valueOf(currentActivityId),
                    String.valueOf(old.getEnd())
                };
            } else {
                whereClause += " AND " + END + " ISNULL";
                whereValues = new String[]{String.valueOf(old.getStart()),
                    String.valueOf(currentActivityId)
                };
            }
            db.update(RANGES_TABLE, values,
                    whereClause,
                    whereValues);
            if (newActivityId != currentActivityId) {
                times.remove(old);
            } else {
                old.setStart(sd);
                old.setEnd(ed);
            }
            notifyDataSetChanged();
        }

        protected Cursor getActivityNames() {
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor c = db.query(DBHelper.ACTIVITY_TABLE, DBHelper.ACTIVITY_COLUMNS, null, null,
                    null, null, ACTIVITY_NAME);
            return c;
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent intent) {
        if (resCode == Activity.RESULT_OK) {
            long sd = intent.getExtras().getLong(START_DATE);
            long ed = intent.getExtras().getLong(END_DATE);
            switch (reqCode) {
                case ADD_TIME:
                    adapter.addTimeRange(sd, ed);
                    break;
                case EDIT_TIME:
                    adapter.updateTimeRange(sd, ed,
                            getIntent().getExtras().getInt(ACTIVITY_ID), selectedRange);
                    break;
            }
        }
        this.getListView().invalidate();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        // Disable previous
        selectedRange = (TimeRange) getListView().getItemAtPosition(position);
        if (selectedRange != null) {
            Intent intent = new Intent(this, EditTime.class);
            intent.putExtra(EditTime.START_DATE, selectedRange.getStart());
            intent.putExtra(EditTime.END_DATE, selectedRange.getEnd());
            startActivityForResult(intent, EDIT_TIME);
        }
    }
}
