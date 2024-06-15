package com.arkabot.syncbot;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import androidx.appcompat.app.AlertDialog;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Updater {
    public static final String PREFS_NAME = "UpdaterPrefs";
    public static final String KEY_UPDATE_AVAILABLE = "update_available";
    private static final String TAG = "Updater";
    private static final String UPDATE_URL = "https://api.github.com/repos/ArkuhTheNinth/Arkabot-Syncbot/releases/latest"; // Replace with your actual URL

    public static void checkForUpdates(Context context) {
        new CheckUpdateTask(context).execute(UPDATE_URL);
    }

    public static void promptUpdate(Context context, String url) {
        new AlertDialog.Builder(context)
                .setTitle("Update Available")
                .setMessage("A new version of the app is available. Do you want to update?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Open the update URL
                    context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                })
                .setNegativeButton("No", null)
                .show();
    }

    private static class CheckUpdateTask extends AsyncTask<String, Void, Boolean> {
        private Context context;

        CheckUpdateTask(Context context) {
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Response Code: " + responseCode);
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();

                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Parse JSON response
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    String latestVersion = jsonResponse.getString("tag_name");
                    Log.d(TAG, "Latest Version: " + latestVersion);

                    // Compare with current version (you need to implement getCurrentVersion())
                    String currentVersion = getCurrentVersion(context);
                    if (!currentVersion.equals(latestVersion)) {
                        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(KEY_UPDATE_AVAILABLE, true);
                        editor.apply();
                        return true;
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking for updates", e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean updateAvailable) {
            if (updateAvailable) {
                // Notify user about the update
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(KEY_UPDATE_AVAILABLE, true);
                editor.apply();
            }
        }

        private String getCurrentVersion(Context context) {
            try {
                return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            } catch (Exception e) {
                Log.e(TAG, "Error getting current version", e);
                return "0.0.0"; // Default version
            }
        }
    }
}
