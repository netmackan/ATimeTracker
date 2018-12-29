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

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.database.Cursor;

/**
 * Helper class for formatting the CSV export.
 *
 * @author Sean Russell, ser@germane-software.com
 */
public class CSVExporter {

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        if (s.contains(",") || s.contains("\"")) {
            s = s.replaceAll("\"", "\"\"");
            s = "\"" + s + "\"";
        }
        return s;
    }

    public static void exportRows(OutputStream o, String[][] rows) {
        PrintStream outputStream = new PrintStream(o);
        for (String[] cols : rows) {
            String prepend = "";
            for (String col : cols) {
                outputStream.print(prepend);
                outputStream.print(escape(col));
                prepend = ",";
            }
            outputStream.println();
        }
    }

    public static void exportRows(OutputStream o, Cursor c) {
        PrintStream outputStream = new PrintStream(o);
        String prepend = "";
        String[] columnNames = c.getColumnNames();
        for (String s : columnNames) {
            outputStream.print(prepend);
            outputStream.print(escape(s));
            prepend = ",";
        }
        if (c.moveToFirst()) {
            Date d = new Date();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            do {
                outputStream.println();
                prepend = "";
                for (int i = 0; i < c.getColumnCount(); i++) {
                    outputStream.print(prepend);
                    String outValue;
                    if (columnNames[i].equals("start")) {
                        d.setTime(c.getLong(i));
                        outValue = formatter.format(d);
                    } else if (columnNames[i].equals("end")) {
                        if (c.isNull(i)) {
                            outValue = "";
                        } else {
                            d.setTime(c.getLong(i));
                            outValue = formatter.format(d);
                        }
                    } else {
                        outValue = escape(c.getString(i));
                    }
                    outputStream.print(outValue);
                    prepend = ",";
                }
            } while (c.moveToNext());
        }
        outputStream.println();
    }
}
