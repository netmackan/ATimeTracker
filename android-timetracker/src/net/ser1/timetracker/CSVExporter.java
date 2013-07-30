package net.ser1.timetracker;

import java.io.OutputStream;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.database.Cursor;

public class CSVExporter {
    private static String escape( String s ) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"")) {
            s = s.replaceAll("\"", "\"\"");
            s = "\"" + s + "\"";
        }
        return s;
    }
    
    
    public static void exportRows( OutputStream o, String[][] rows ) {
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
    
    
    public static void exportRows( OutputStream o, Cursor c ) {
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
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            do {
                outputStream.println();
                prepend = "";
                for (int i=0; i<c.getColumnCount(); i++) {
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
