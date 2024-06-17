package com.arkabot.syncbot;

import android.content.Context;
import android.util.Log;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;

public class FileLogger {
    private static final String LOG_TAG = "FileLogger";
    private static final String LOG_FILE_NAME = "app_logs.txt";
    private static final long ONE_DAY_MILLIS = 24 * 60 * 60 * 1000;
    private static LogUpdateListener logUpdateListener;

    public static synchronized void log(Context context, String message) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date());
        String logMessage = timestamp + " " + message;
        Log.d(LOG_TAG, logMessage);
        writeLogToFile(context, logMessage);
    }

    private static void writeLogToFile(Context context, String message) {
        File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
        try (FileWriter writer = new FileWriter(logFile, true)) {
            writer.append(message).append("\n");
            writer.flush(); // Ensure logs are written immediately
            if (logUpdateListener != null) {
                logUpdateListener.onLogUpdated(message);
            }
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error writing log to file", e);
        }
    }

    public static synchronized String readLogs(Context context) {
        File logFile = new File(context.getFilesDir(), LOG_FILE_NAME);
        StringBuilder logs = new StringBuilder();
        long currentTime = System.currentTimeMillis();
        try (Scanner scanner = new Scanner(logFile)) {
            while (scanner.hasNextLine()) {
                String logLine = scanner.nextLine();
                long logTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(logLine.substring(0, 19)).getTime();
                if (currentTime - logTime <= ONE_DAY_MILLIS) {
                    logs.append(logLine).append("\n");
                }
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error reading log file", e);
        }
        return logs.toString();
    }

    public static void setLogUpdateListener(LogUpdateListener listener) {
        logUpdateListener = listener;
    }

    public interface LogUpdateListener {
        void onLogUpdated(String newLog);
    }
}
