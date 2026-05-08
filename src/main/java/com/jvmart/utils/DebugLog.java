package com.jvmart.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;

/**
 * Minimal NDJSON debug logger for the current debug session.
 * Writes to: debug-ff0e8e.log in the current workspace working directory.
 */
public final class DebugLog {
    private static final String SESSION_ID = "ff0e8e";
    private static final String LOG_FILENAME = "debug-ff0e8e.log";

    private DebugLog() {}

    public static void log(
            String hypothesisId,
            String location,
            String message,
            Map<String, ?> data,
            String runId
    ) {
        long ts = System.currentTimeMillis();
        String id = "log_" + ts + "_" + Thread.currentThread().getId();

        String json = toJsonLine(hypothesisId, location, message, data, runId, id, ts);
        // Try to resolve relative to the FXML/class location (workspace root)
        Path workDir = resolveWorkspaceDir();
        Path p = workDir.resolve(LOG_FILENAME);
        try {
            Files.writeString(
                    p,
                    json + "\n",
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException ignored) {
            // Debug logging must never break UI/navigation.
        }
    }

    private static String toJsonLine(
            String hypothesisId,
            String location,
            String message,
            Map<String, ?> data,
            String runId,
            String id,
            long timestamp
    ) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("{");
        sb.append("\"sessionId\":\"").append(escapeJson(SESSION_ID)).append("\",");
        sb.append("\"id\":\"").append(escapeJson(id)).append("\",");
        sb.append("\"timestamp\":").append(timestamp).append(",");
        sb.append("\"runId\":\"").append(escapeJson(runId)).append("\",");
        sb.append("\"location\":\"").append(escapeJson(location)).append("\",");
        sb.append("\"hypothesisId\":\"").append(escapeJson(hypothesisId)).append("\",");
        sb.append("\"message\":\"").append(escapeJson(message)).append("\"");

        if (data != null && !data.isEmpty()) {
            sb.append(",\"data\":{");
            boolean first = true;
            for (var e : data.entrySet()) {
                if (!first) sb.append(",");
                first = false;
                sb.append("\"").append(escapeJson(String.valueOf(e.getKey()))).append("\":");
                Object v = e.getValue();
                if (v == null) {
                    sb.append("null");
                } else if (v instanceof Number || v instanceof Boolean) {
                    sb.append(String.valueOf(v));
                } else {
                    sb.append("\"").append(escapeJson(String.valueOf(v))).append("\"");
                }
            }
            sb.append("}");
        }
        sb.append("}");
        return sb.toString();
    }

    private static Path resolveWorkspaceDir() {
        // Prefer the JVMART_HOME env var if set, then walk up from the class location
        String envHome = System.getenv("JVMART_HOME");
        if (envHome != null && !envHome.isBlank()) {
            return Path.of(envHome);
        }
        // Walk up from user.dir until we find pom.xml (workspace root)
        Path dir = Path.of(System.getProperty("user.dir"));
        for (int i = 0; i < 8; i++) {
            if (dir.resolve("pom.xml").toFile().exists()) {
                return dir;
            }
            Path parent = dir.getParent();
            if (parent == null) break;
            dir = parent;
        }
        return Path.of(System.getProperty("user.dir"));
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

