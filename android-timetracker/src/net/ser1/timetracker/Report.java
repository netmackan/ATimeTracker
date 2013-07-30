package net.ser1.timetracker;

import static net.ser1.timetracker.DBHelper.END;
import static net.ser1.timetracker.DBHelper.NAME;
import static net.ser1.timetracker.DBHelper.RANGES_TABLE;
import static net.ser1.timetracker.DBHelper.RANGE_COLUMNS;
import static net.ser1.timetracker.DBHelper.START;
import static net.ser1.timetracker.DBHelper.TASK_COLUMNS;
import static net.ser1.timetracker.DBHelper.TASK_ID;
import static net.ser1.timetracker.DBHelper.TASK_NAME;
import static net.ser1.timetracker.DBHelper.TASK_TABLE;
import static net.ser1.timetracker.Tasks.REPORT_DATE;
import static net.ser1.timetracker.Tasks.START_DAY;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;



public class Report extends Activity implements OnClickListener {
    
    enum Day {
        MONDAY( Calendar.MONDAY, "Mon" ), 
        TUESDAY( Calendar.TUESDAY, "Tue" ), 
        WEDNESDAY( Calendar.WEDNESDAY, "Wed"), 
        THURSDAY( Calendar.THURSDAY, "Thu"), 
        FRIDAY( Calendar.FRIDAY, "Fri"), 
        SATURDAY( Calendar.SATURDAY, "Sat"), 
        SUNDAY( Calendar.SUNDAY, "Sun");
        
        public int calEnum;
        public String header;
        Day( int calEnum, String header ) {
            this.calEnum = calEnum;
            this.header = header;
        }
        static Day fromCalEnum( int calEnum ) {
            for (Day v : values()) {
                if (v.calEnum == calEnum) return v;
            }
            return null;
        }
        public String toString() {
            return header;
        }
        public int calEnum() {
            return calEnum;
        }
    }

    /**
     * Defines how each task's time is displayed 
     */
    private Calendar weekStart, weekEnd;
    private Map<Integer,TextView[]> dateViews = new TreeMap<Integer,TextView[]>();
    private static final int PAD = 2;
    private static final int RPAD = 4;
    private static final String FORMAT = "%02d:%02d";
    private TextView weekView;
    private static final SimpleDateFormat WEEK_FORMAT = new SimpleDateFormat("w");
    private static final SimpleDateFormat TITLE_FORMAT = new SimpleDateFormat("EEE, MMM d");
    private DBHelper dbHelper;
    private SQLiteDatabase db;
    private int startDay;
    private boolean decimalTime = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.report);
        TableLayout mainReport = (TableLayout)findViewById(R.id.report);
        
        Calendar c = Calendar.getInstance();
        c.setFirstDayOfWeek( Calendar.MONDAY );
        c.setTimeInMillis(getIntent().getExtras().getLong(REPORT_DATE));
        startDay = getIntent().getExtras().getInt(START_DAY);
        weekStart = weekStart(c, startDay);
        weekEnd = weekEnd(c, startDay);
        String beginning = TITLE_FORMAT.format(weekStart.getTime());
        String ending = TITLE_FORMAT.format(weekEnd.getTime());
        String title = getString(R.string.report_title, beginning, ending);
        setTitle( title );
        decimalTime = getIntent().getExtras().getBoolean(Tasks.TIMEDISPLAY);

        createHeader( mainReport );
        
        dbHelper = new DBHelper(this);
        db = dbHelper.getReadableDatabase();
        
        weekView = (TextView)findViewById(R.id.week);
        weekView.setText(getString(R.string.week,WEEK_FORMAT.format(c.getTime())));
        
        ((ImageButton)findViewById(R.id.decrement_week)).setOnClickListener(this);
        ((ImageButton)findViewById(R.id.increment_week)).setOnClickListener(this);
        
        createReport( mainReport );
        createTotals( mainReport );

        fillInTasksAndRanges();
    }
    
    @Override
    public void onStop() {
        db.close();
        super.onStop();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, Tasks.EXPORT_VIEW, 0, R.string.export_view)
            .setIcon(android.R.drawable.ic_menu_save);
        return true;
    }

    private AlertDialog exportSucceed;
    private String exportMessage;
    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch (item.getItemId()) {
        case Tasks.EXPORT_VIEW:
            String fname = export();
            if (fname != null) {
                exportMessage = getString(R.string.export_csv_success, fname);
                if (exportSucceed != null) exportSucceed.setMessage(exportMessage);
                showDialog(Tasks.SUCCESS_DIALOG);
            } else {
                exportMessage = getString(R.string.export_csv_fail);
                showDialog(Tasks.ERROR_DIALOG);
            }
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
        case Tasks.SUCCESS_DIALOG:
            exportSucceed = new AlertDialog.Builder(this)
            .setTitle(R.string.success)
            .setIcon(android.R.drawable.stat_notify_sdcard)
            .setMessage(exportMessage)
            .setPositiveButton(android.R.string.ok, null)
            .create();
            return exportSucceed;
        case Tasks.ERROR_DIALOG:
            return new AlertDialog.Builder(this)
            .setTitle(R.string.failure)
            .setIcon(android.R.drawable.stat_notify_sdcard)
            .setMessage(exportMessage)
            .setPositiveButton(android.R.string.ok, null)
            .create();
        default:
            break;
        }
        return null;
    }

    /**
     * Yes, this _is_ a duplicate of the exact same code in Tasks.  Java doesn't 
     * support mix-ins, which leads to bad programming practices out of 
     * necessity.
     */
    final static String SDCARD = "/sdcard/";
    private String export() {
        // Export, then show a dialog
        String rangeName = getRangeName();
        String fname = "report_"+rangeName+".csv";
        File fout = new File( SDCARD+fname );
        // Change the file name until there's no conflict
        int counter = 0;
        while (fout.exists()) {
            fname = "report_"+rangeName+"_"+counter+".csv";
            fout = new File( SDCARD+fname );
            counter++;
        }
        try {
            OutputStream out = new FileOutputStream(fout);
            CSVExporter.exportRows(out, getCurrentRange());
            
            return fname;
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace(System.err);
            return null;
        }
    }
    
    private String[][] getCurrentRange() {
        List<String[]> tasks = new ArrayList<String[]>();

        Map<Integer,String> taskNames = new TreeMap<Integer,String>();
        Cursor c = db.query(TASK_TABLE, new String[] {"ROWID",TASK_NAME}, null, null, null, null, "ROWID");
        if (c.moveToFirst()) {
            do {
                int tid = c.getInt(0);
                String tname = c.getString(1);
                taskNames.put(tid, tname);
            } while (c.moveToNext());
        }
        c.close();
        
        int[] weekDays = new int[7];
        for (int i = 0; i < 7; i++) {
            weekDays[i] = ((weekStart.getFirstDayOfWeek()-1+i)%7)+1;
        }
        // Add the headers
        String[] headers = new String[9];
        headers[0] = "Task name";
        for (int i = 0; i<7; i++) {
            Day s = Day.fromCalEnum(weekDays[i]);
            headers[i+1] = s.header;
        }
        headers[8] = "Total";
        tasks.add(headers);
        
        for (int tid : dateViews.keySet()) {
            if (tid == -1) continue;
            String[] rowForTask = new String[9];
            tasks.add(rowForTask);
            rowForTask[0] = taskNames.get(tid);
            TextView[] arryForDay = dateViews.get(tid);
            for (int i = 0 ; i < 8; i++) {
                rowForTask[i+1] = arryForDay[i].getText().toString();
            }
        }            
        
        TextView[] totals = dateViews.get(-1);
        String[] totalsRow = new String[9];
        tasks.add(totalsRow);
        totalsRow[0] = "Day total";
        for (int i = 0; i < 8; i++) {
            totalsRow[i+1] = totals[i].getText().toString();
        }
        
        String[][] k = {{}};
        return tasks.toArray(k);
    }

    private String getRangeName() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        return formatter.format(weekStart.getTime());
    }

    private static final int DKDKYELLOW = Color.argb(100, 75, 75, 0);
    private void createTotals(TableLayout mainReport) {
        TextView[] totals = new TextView[8];
        dateViews.put(-1, totals);
        TableRow row = new TableRow(this);
        mainReport.addView(row, new TableLayout.LayoutParams());
        TextView blank = new TextView(this);
        blank.setPadding(PAD,PAD*2,RPAD,PAD);
        row.addView(blank, new TableRow.LayoutParams(0));
        for (int i = 0; i < 7; i++) {
            TextView dayTime = new TextView(this);
            totals[i] = dayTime;
            dayTime.setPadding(PAD,PAD*2,RPAD,PAD);
            dayTime.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
            if (i % 2 == 1) 
                dayTime.setBackgroundColor(DKYELLOW);
            else
                dayTime.setBackgroundColor(DKDKYELLOW);
            row.addView(dayTime, new TableRow.LayoutParams());
        }

        TextView total = new TextView(this);
        totals[7] = total;
        total.setText("");
        total.setPadding(PAD,PAD*2,RPAD,PAD);
        total.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
        total.setBackgroundColor(DKYELLOW);
        row.addView(total, new TableRow.LayoutParams());
    }

    @Override
    protected void onResume() {
        super.onResume();
        db = dbHelper.getReadableDatabase();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        db.close();
    }
    
    private static final int DKYELLOW = Color.argb(150, 100, 100, 0);
    private void createHeader(TableLayout mainReport) {
        TableRow row = new TableRow(this);
        mainReport.addView(row, new TableLayout.LayoutParams());

        TextView blank = new TextView(this);
        blank.setText("Task");
        blank.setPadding(PAD,PAD,RPAD,PAD);
        blank.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        row.addView(blank, new TableRow.LayoutParams(0));
        
        int[] weekDays = new int[7];
        for (int i = 0; i < 7; i++) {
            weekDays[i] = ((weekStart.getFirstDayOfWeek()-1+i)%7)+1;
        }

        for (int i = 0; i < 7; i++) {
            Day s = Day.fromCalEnum(weekDays[i]);
            TextView header  = new TextView(this);
            header.setText(s.toString());
            header.setPadding(PAD,PAD,RPAD,PAD);
            header.setGravity(Gravity.CENTER_HORIZONTAL);
            header.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
            if (i % 2 == 1) 
                header.setBackgroundColor(Color.DKGRAY);
            row.addView(header,new TableRow.LayoutParams());
        }
        
        TextView total = new TextView(this);
        total.setText("Ttl");
        total.setPadding(PAD,PAD,RPAD,PAD+2);
        total.setGravity(Gravity.CENTER_HORIZONTAL);
        total.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        total.setBackgroundColor(DKYELLOW);
        row.addView(total, new TableRow.LayoutParams());
    }

    private void createReport( TableLayout mainReport ) {
        Cursor c = db.query(TASK_TABLE, TASK_COLUMNS, null, null, null, null, NAME);
        if (c.moveToFirst()) {
            do {
                int tid = c.getInt(0);
                TextView[] arryForDay = new TextView[8];

                dateViews.put(tid, arryForDay);
                
                TableRow row = new TableRow(this);
                mainReport.addView(row, new TableLayout.LayoutParams());
                
                TextView taskName = new TextView(this);
                taskName.setText(c.getString(1));
                taskName.setPadding(PAD,PAD,RPAD,PAD);
                row.addView(taskName, new TableRow.LayoutParams(0));
                
                for (int i = 0; i < 7; i++) {
                    TextView dayTime = new TextView(this);
                    arryForDay[i] = dayTime;
                    dayTime.setPadding(PAD,PAD,RPAD,PAD);
                    if (i % 2 == 1) dayTime.setBackgroundColor(Color.DKGRAY);
                    row.addView(dayTime, new TableRow.LayoutParams());
                }
    
                TextView total = new TextView(this);
                arryForDay[7] = total;
                total.setPadding(PAD,PAD,RPAD,PAD);
                total.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
                total.setBackgroundColor(DKYELLOW);
                row.addView(total, new TableRow.LayoutParams());
            } while (c.moveToNext());
        }
        c.close();
    }
        
    /**
     * Calculates the date/time of the beginning of the week in 
     * which the supplied calendar date falls
     * @param tw the day for which to calculate the week start
     * @param startDay the day on which the week starts.  This must be 1-based
     * (1 = Sunday).
     * @return a Calendar marking the start of the week
     */
    public static Calendar weekStart(Calendar tw, int startDay) {
        Calendar ws = (Calendar)tw.clone();
        ws.setFirstDayOfWeek( startDay );
        // START ANDROID BUG WORKAROUND
        // Android has a broken Calendar class, so the if-statement wrapping
        // the following set() is necessary to keep Android from incorrectly
        // changing the date:
        int adjustedDay = ws.get(Calendar.DAY_OF_WEEK);
        ws.add(Calendar.DATE, -((7-(startDay-adjustedDay)) % 7));
        // The above code _should_ be:
        // ws.set(Calendar.DAY_OF_WEEK, startDay);
        // END ANDROID BUG WORKAROUND
        ws.set(Calendar.HOUR_OF_DAY, ws.getMinimum(Calendar.HOUR_OF_DAY));
        ws.set(Calendar.MINUTE, ws.getMinimum(Calendar.MINUTE));
        ws.set(Calendar.SECOND, ws.getMinimum(Calendar.SECOND));
        ws.set(Calendar.MILLISECOND, ws.getMinimum(Calendar.MILLISECOND));
        return ws;
    }

    /**
     * Calculates the date/time of the end of the week in 
     * which the supplied calendar data falls
     * @param tw the day for which to calculate the week end
     * @return a Calendar marking the end of the week
     */
    public static Calendar weekEnd(Calendar tw, int startDay) {
        Calendar ws = (Calendar)tw.clone();
        ws.setFirstDayOfWeek( startDay );
        // START ANDROID BUG WORKAROUND
        // Android has a broken Calendar class, so the if-statement wrapping
        // the following set() is necessary to keep Android from incorrectly
        // changing the date:
        int adjustedDay = ws.get(Calendar.DAY_OF_WEEK);
        ws.add(Calendar.DATE, -((7-(startDay-adjustedDay))%7));
        // The above code _should_ be:
        // ws.set(Calendar.DAY_OF_WEEK, startDay);
        // END ANDROID BUG WORKAROUND
        ws.add(Calendar.DAY_OF_WEEK, 6);
        ws.set(Calendar.HOUR_OF_DAY, ws.getMaximum(Calendar.HOUR_OF_DAY));
        ws.set(Calendar.MINUTE, ws.getMaximum(Calendar.MINUTE));
        ws.set(Calendar.SECOND, ws.getMaximum(Calendar.SECOND));
        ws.set(Calendar.MILLISECOND, ws.getMaximum(Calendar.MILLISECOND));
        return ws;
    }

    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.increment_week:
            weekStart.add(Calendar.WEEK_OF_YEAR, 1);
            weekEnd.add(Calendar.WEEK_OF_YEAR, 1);
            break;
        case R.id.decrement_week:
            weekStart.add(Calendar.WEEK_OF_YEAR, -1);
            weekEnd.add(Calendar.WEEK_OF_YEAR, -1);
            break;
        default:
            break;
        }
        String beginning = TITLE_FORMAT.format(weekStart.getTime());
        String ending = TITLE_FORMAT.format(weekEnd.getTime());
        String title = getString(R.string.report_title, beginning, ending);
        setTitle( title );
        fillInTasksAndRanges();
        weekView.setText(getString(R.string.week,WEEK_FORMAT.format(weekStart.getTime())));
    }
    
    private void fillInTasksAndRanges() {
        // Iterate over each task and set the day values, and accumulate the day 
        // and week totals
        Cursor c = db.query(TASK_TABLE, TASK_COLUMNS, null, null, null, null, NAME);
        // The totals for all tasks for each day, plus one for the week total.
        long dayTotals[] = {0,0,0,0,0,0,0,0};
        if (c.moveToFirst()) {
            do {
                int tid = c.getInt(0);
                String tid_s = String.valueOf(tid);
                TextView[] arryForDay = dateViews.get(tid);
                // Fetch an array of times (per day) for the task
                long[] days = getDays(tid_s);
                // The total for this task, for the whole week
                int weekTotal = 0;
                for (int i = 0 ; i < 7; i++) {
                    weekTotal += days[i];
                    dayTotals[i] += days[i];
                    arryForDay[i].setText(Tasks.formatTotal(decimalTime, FORMAT, days[i]));
                }
                // Set the week total.  Since this value can be more than 24 hours,
                // we have to format it by hand:
                arryForDay[7].setText(Tasks.formatTotal(decimalTime, FORMAT, weekTotal));
                dayTotals[7] += weekTotal;
            } while (c.moveToNext());
        }
        c.close();
        
        TextView[] totals = dateViews.get(-1);
        for (int i = 0; i < 7; i++) {
            totals[i].setText(Tasks.formatTotal(decimalTime, FORMAT, dayTotals[i]));
        }
        totals[7].setText(Tasks.formatTotal(decimalTime, FORMAT, dayTotals[7]));
    }

    /**
     * Fetch the times for a task within the currently set time range, by day.
     * 
     * @param tid_s The ID of the task for which to fetch times
     * @return An array containinging, in each cell, the sum of the times which
     *         fall within that day. Index 0 is the first day starting on the
     *         currently set week.  This uses TimeRange.overlap() to make sure
     *         that only time that actually falls on the day is included (even
     *         if a particular time entry spans days).
     * @see TimeRange.overlap()
     */
    private long[] getDays(String tid_s) {
        Calendar day = Calendar.getInstance();
        day.setFirstDayOfWeek( startDay );
        long days[] = {0,0,0,0,0,0,0};
        Cursor r = db.query(RANGES_TABLE, RANGE_COLUMNS, TASK_ID+" = ? AND "
                +START+" < ? AND ( "+END+" > ? OR "+END+" ISNULL )",
                new String[] { tid_s, 
                               String.valueOf(weekEnd.getTimeInMillis()),
                               String.valueOf(weekStart.getTimeInMillis())},
                null,null,null);

        if (r.moveToFirst()) {
            do {
                long start = r.getLong(0);
                long end;
                if (r.isNull(1)) {
                    end = System.currentTimeMillis();
                } else {
                    end = r.getLong(1);
                }
                
                day.setTimeInMillis(end);
                
                int[] weekDays = new int[7];
                for (int i = 0; i < 7; i++) {
                    weekDays[i] = ((weekStart.getFirstDayOfWeek()-1+i)%7)+1;
                }

                // At this point, "day" must be set to the start time
                for (int i=0; i<7; i++) {
                    Day d = Day.fromCalEnum(weekDays[i]);
                    day.set(Calendar.DAY_OF_WEEK, d.calEnum);
                    days[i] += TimeRange.overlap(day, start, end);
                }
                
            } while (r.moveToNext());
        } 
        r.close();
        return days;
    }
}