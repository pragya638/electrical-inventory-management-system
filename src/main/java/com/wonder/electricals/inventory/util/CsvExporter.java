package com.wonder.electricals.inventory.util;

import java.util.List;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

/**
 * Minimal RFC-4180 style CSV writer for report exports. Written by hand rather than
 * pulling in a reporting library, which keeps the dependency list short.
 */
@Component
public class CsvExporter {

    private static final String SEPARATOR = ",";
    private static final String NEW_LINE = "\r\n";

    /** Builds a CSV document from a header row and data rows. */
    public String build(List<String> headers, List<List<String>> rows) {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, headers);
        for (List<String> row : rows) {
            appendRow(csv, row);
        }
        return csv.toString();
    }

    private void appendRow(StringBuilder target, List<String> cells) {
        StringJoiner joiner = new StringJoiner(SEPARATOR);
        for (String cell : cells) {
            joiner.add(escape(cell));
        }
        target.append(joiner).append(NEW_LINE);
    }

    /** Wraps a value in quotes and doubles any embedded quote so Excel can parse it. */
    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
