package com.acooldog.toolbox.root;

import android.content.Context;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class RootDiagnosticLogStore {
    private static final String LOG_DIR = "root-diagnostic/process-logs";
    private static final String LOG_SUFFIX = ".log";

    private final File logDir;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    private final SimpleDateFormat lineTimeFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    public RootDiagnosticLogStore(@NonNull Context context) {
        this(new File(context.getApplicationContext().getFilesDir(), LOG_DIR));
    }

    RootDiagnosticLogStore(@NonNull File logDir) {
        this.logDir = logDir;
    }

    public synchronized void append(@NonNull RootDiagnosticEvent event) {
        ensureLogDir();
        File file = fileForDate(dateFormat.format(new Date(event.getTimestampMillis())));
        try (FileOutputStream outputStream = new FileOutputStream(file, true)) {
            outputStream.write(encode(event).getBytes(StandardCharsets.UTF_8));
            outputStream.write('\n');
        } catch (Exception ignored) {
            // Keep process log persistence best-effort.
        }
    }

    @NonNull
    public synchronized List<String> listDates() {
        ensureLogDir();
        File[] files = logDir.listFiles((dir, name) -> name.endsWith(LOG_SUFFIX)
                && isSafeDate(name.substring(0, name.length() - LOG_SUFFIX.length())));
        if (files == null || files.length == 0) {
            return Collections.emptyList();
        }
        Arrays.sort(files, (left, right) -> right.getName().compareTo(left.getName()));
        List<String> dates = new ArrayList<>();
        for (File file : files) {
            String name = file.getName();
            dates.add(name.substring(0, name.length() - LOG_SUFFIX.length()));
        }
        return dates;
    }

    public synchronized int countEventsForDate(@NonNull String date) {
        return loadEventsForDate(date).size();
    }

    @NonNull
    public synchronized String buildTextForDate(@NonNull String date) {
        List<RootDiagnosticEvent> events = loadEventsForDate(date);
        StringBuilder builder = new StringBuilder(Math.max(1024, events.size() * 160));
        for (RootDiagnosticEvent event : events) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(lineTimeFormat.format(new Date(event.getTimestampMillis())))
                    .append(" [")
                    .append(event.getTargetPackageName())
                    .append("] [")
                    .append(event.getModuleId())
                    .append('/')
                    .append(event.getType())
                    .append("] ")
                    .append(event.getDetail());
        }
        return builder.toString();
    }

    @NonNull
    List<RootDiagnosticEvent> loadEventsForDate(@NonNull String date) {
        if (!isSafeDate(date)) {
            return Collections.emptyList();
        }
        File file = fileForDate(date);
        if (!file.isFile()) {
            return Collections.emptyList();
        }
        List<RootDiagnosticEvent> events = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file),
                StandardCharsets.UTF_8
        ))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                try {
                    events.add(decode(line));
                } catch (Exception ignored) {
                    // Skip corrupted lines and keep the rest of the day readable.
                }
            }
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
        events.sort((left, right) -> Long.compare(right.getTimestampMillis(), left.getTimestampMillis()));
        return events;
    }

    private void ensureLogDir() {
        if (!logDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            logDir.mkdirs();
        }
    }

    @NonNull
    private File fileForDate(@NonNull String date) {
        return new File(logDir, date + LOG_SUFFIX);
    }

    private static boolean isSafeDate(@NonNull String date) {
        return date.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    @NonNull
    private static String encode(@NonNull RootDiagnosticEvent event) {
        return event.getTimestampMillis()
                + "\t" + encodePart(event.getSessionId())
                + "\t" + encodePart(event.getTargetPackageName())
                + "\t" + encodePart(event.getModuleId())
                + "\t" + encodePart(event.getType())
                + "\t" + encodePart(event.getDetail());
    }

    @NonNull
    private static RootDiagnosticEvent decode(@NonNull String line) {
        String[] parts = line.split("\t", -1);
        if (parts.length < 6) {
            throw new IllegalArgumentException("invalid diagnostic log line");
        }
        return new RootDiagnosticEvent(
                Long.parseLong(parts[0]),
                decodePart(parts[1]),
                decodePart(parts[2]),
                decodePart(parts[3]),
                decodePart(parts[4]),
                decodePart(parts[5])
        );
    }

    @NonNull
    private static String encodePart(@NonNull String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @NonNull
    private static String decodePart(@NonNull String value) {
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }
}
