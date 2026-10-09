package com.srirammart.util;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Small RFC-4180 CSV reader (quotes, escaped quotes, line breaks inside quotes). */
public final class CsvParser {
    private CsvParser() {}

    /** Reads a CSV with a header line into one map per data row. "_line" holds the source line number. */
    public static List<Map<String, String>> readMaps(Reader reader) throws IOException {
        List<String[]> rows = read(reader);
        List<Map<String, String>> out = new ArrayList<>();
        if (rows.isEmpty()) return out;
        String[] header = rows.get(0);
        for (int r = 1; r < rows.size(); r++) {
            String[] cells = rows.get(r);
            Map<String, String> m = new LinkedHashMap<>();
            for (int c = 0; c < header.length; c++) m.put(header[c].trim(), c < cells.length ? cells[c] : "");
            m.put("_line", String.valueOf(r + 1));
            out.add(m);
        }
        return out;
    }

    public static List<String[]> read(Reader reader) throws IOException {
        StringBuilder all = new StringBuilder();
        char[] buf = new char[8192];
        int n;
        while ((n = reader.read(buf)) > 0) all.append(buf, 0, n);
        String s = all.toString();
        if (s.startsWith("\uFEFF")) s = s.substring(1);

        List<String[]> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '"') { cell.append('"'); i++; }
                    else quoted = false;
                } else cell.append(c);
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\r') {
                // ignore, handled by '\n'
            } else if (c == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                addRow(rows, row);
                row = new ArrayList<>();
            } else {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            addRow(rows, row);
        }
        return rows;
    }

    private static void addRow(List<String[]> rows, List<String> row) {
        boolean empty = true;
        for (String c : row) if (!c.isBlank()) { empty = false; break; }
        if (!empty) rows.add(row.toArray(new String[0]));
    }
}
