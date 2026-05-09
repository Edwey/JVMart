package com.jvmart.utils;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvExportUtil {
    private CsvExportUtil() {}

    public static Path exportCsv(
            Window owner,
            String dialogTitle,
            String suggestedFileName,
            String[] header,
            List<String[]> rows
    ) throws IOException {
        FileChooser chooser = new FileChooser();
        chooser.setTitle(dialogTitle);
        chooser.setInitialFileName(suggestedFileName.endsWith(".csv") ? suggestedFileName : suggestedFileName + ".csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        java.io.File file = chooser.showSaveDialog(owner);
        if (file == null) {
            return null;
        }

        StringBuilder sb = new StringBuilder(4096);
        if (header != null && header.length > 0) {
            appendRow(sb, header);
        }
        if (rows != null) {
            for (String[] row : rows) {
                appendRow(sb, row);
            }
        }

        Path out = file.toPath();
        Files.writeString(out, sb.toString(), StandardCharsets.UTF_8);
        return out;
    }

    private static void appendRow(StringBuilder sb, String[] cols) {
        if (cols == null) {
            sb.append('\n');
            return;
        }
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(escape(cols[i]));
        }
        sb.append('\n');
    }

    private static String escape(String v) {
        if (v == null) return "";
        boolean mustQuote = v.contains(",") || v.contains("\"") || v.contains("\n") || v.contains("\r");
        String cleaned = v.replace("\"", "\"\"");
        return mustQuote ? "\"" + cleaned + "\"" : cleaned;
    }
}

