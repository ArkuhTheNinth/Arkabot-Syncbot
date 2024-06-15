package com.arkabot.syncbot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Updater extends Worker {
    public static final String PREFS_NAME = "app_prefs";
    public static final String KEY_UPDATE_AVAILABLE = "update_available";

    public Updater(Context context, WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @Override
    public Result doWork() {
        checkForUpdates(getApplicationContext());
        return Result.success();
    }

    public static void checkForUpdates(Context context) {
        FileLogger.log(context, "Checking for updates...");
        new Thread(() -> {
            try {
                URL url = new URL("https://api.github.com/repos/ArkuhTheNinth/Arkabot-Syncbot/releases/latest");
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                int responseCode = urlConnection.getResponseCode();

                if (responseCode == 200) {
                    FileLogger.log(context, "Update check successful. Response code: 200");
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder content = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        content.append(inputLine);
                    }
                    in.close();

                    JSONObject latestRelease = new JSONObject(content.toString());
                    String latestVersion = latestRelease.getString("tag_name");

                    String currentVersion = BuildConfig.VERSION_NAME;
                    FileLogger.log(context, "Current version: " + currentVersion + ", Latest version: " + latestVersion);
                    if (!currentVersion.equals(latestVersion)) {
                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        prefs.edit().putBoolean(KEY_UPDATE_AVAILABLE, true).apply();
                        promptUpdate(context, latestRelease.getString("html_url"));
                    }
                } else {
                    FileLogger.log(context, "Update check failed with response code: " + responseCode);
                }
            } catch (Exception e) {
                FileLogger.log(context, "Error checking for updates: " + e.getMessage());
            }
        }).start();
    }

    public static void promptUpdate(Context context, String updateUrl) {
        new Handler(Looper.getMainLooper()).post(() -> {
            new AlertDialog.Builder(context)
                    .setTitle("Update Available")
                    .setMessage("A new version of the app is available. Please update to the latest version.")
                    .setPositiveButton("Update", (dialog, which) -> {
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl));
                        context.startActivity(browserIntent);
                    })
                    .setNegativeButton("Later", null)
                    .show();
        });
    }
}
