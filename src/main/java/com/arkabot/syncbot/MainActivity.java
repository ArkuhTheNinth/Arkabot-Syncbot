package com.arkabot.syncbot;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements FileLogger.LogUpdateListener, FolderPicker.FolderPickerListener {
    private TokenManager tokenManager;
    private TextView logTextView;
    private ScrollView logScrollView;
    private Button pushFilesButton, chooseLocalFolderButton;
    private String localDirectoryUri;
    private String dropboxDirectory;
    private FolderPicker folderPicker;
    private static final int REQUEST_PERMISSIONS = 123;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSIONS);
            }
        }


        // Initialize logging as early as possible
        FileLogger.log(this, "App launched");

        setContentView(R.layout.activity_main);

        tokenManager = new TokenManager(this);

        logTextView = findViewById(R.id.logTextView);
        logScrollView = findViewById(R.id.logScrollView);
        pushFilesButton = findViewById(R.id.pushFilesButton);
        chooseLocalFolderButton = findViewById(R.id.chooseLocalFolderButton);

        folderPicker = new FolderPicker(this, this);

        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());

        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build());

        FileLogger.log(this, "Checking and requesting permissions");
        checkAndRequestPermissions();
        FileLogger.log(this, "Loading preferences");
        loadPreferences();
        FileLogger.log(this, "Initializing views");
        initializeViews();
        FileLogger.log(this, "Loading logs");
        loadLogs();
        FileLogger.log(this, "Handling intent");
        handleIntent(getIntent());


        // Schedule daily update checks
        FileLogger.log(this, "Scheduling daily update checks");
        PeriodicWorkRequest updateCheckRequest = new PeriodicWorkRequest.Builder(Updater.class, 1, TimeUnit.DAYS)
                .build();
        WorkManager.getInstance(this).enqueue(updateCheckRequest);

        // Check for updates on launch
        FileLogger.log(this, "Checking for updates on launch");
        Updater.checkForUpdates(this);

        // Prompt user if an update is available
        SharedPreferences prefs = getSharedPreferences(Updater.PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean(Updater.KEY_UPDATE_AVAILABLE, false)) {
            FileLogger.log(this, "Update available, prompting user");
            Updater.promptUpdate(this, "https://github.com/yourusername/yourrepository/releases/latest"); // Replace with your actual repository
        }

        // Check and refresh token if needed
        if (tokenManager.shouldRefreshToken()) {
            FileLogger.log(this, "Refreshing token as needed");
            tokenManager.updateLastRefreshTime();
            refreshToken();
        }

        // Set log update listener
        FileLogger.log(this, "Setting log update listener");
        FileLogger.setLogUpdateListener(this);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        String action = intent.getAction();
        Uri uri = intent.getData();
        FileLogger.log(this, "Intent action: " + action);
        if (Intent.ACTION_VIEW.equals(action) && uri != null && uri.toString().startsWith(Constants.DROPBOX_REDIRECT_URI)) {
            FileLogger.log(this, "Handling redirect URI: " + uri.toString());
            handleRedirectUri(uri.toString());
        }
    }

    private void handleRedirectUri(String uri) {
        FileLogger.log(this, "Handling redirect URI: " + uri);
        try {
            Uri parsedUri = Uri.parse(uri);
            String fragment = parsedUri.getFragment();
            if (fragment != null) {
                String[] params = fragment.split("&");
                String accessToken = null;
                long expiryTime = System.currentTimeMillis() + (3600 * 1000); // Example expiry time: 1 hour

                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length != 2) continue;
                    String key = keyValue[0];
                    String value = keyValue[1];
                    FileLogger.log(this, "Parsed parameter: " + key + " = " + value);

                    if (key.equals("access_token")) {
                        accessToken = value;
                    } else if (key.equals("expires_in")) {
                        expiryTime = System.currentTimeMillis() + (Long.parseLong(value) * 1000);
                    }
                }

                if (accessToken != null) {
                    FileLogger.log(this, "Access token found: " + accessToken);
                    tokenManager.saveTokens(accessToken, expiryTime);
                    initializeViews();
                } else {
                    FileLogger.log(this, "Error: Access token not found in the URL fragment.");
                }
            } else {
                FileLogger.log(this, "Error: No fragment found in the URL.");
            }
        } catch (Exception e) {
            FileLogger.log(this, "Error handling redirect URI: " + e.getMessage());
        }
    }

    @Override
    public void onLogUpdated(String newLog) {
        runOnUiThread(() -> {
            logTextView.append(newLog + "\n");
            logScrollView.post(() -> logScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    public void log(final String message) {
        runOnUiThread(() -> {
            logTextView.append(message + "\n");
            logScrollView.post(() -> logScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
        FileLogger.log(this, message);
    }

    private void loadLogs() {
        String logs = FileLogger.readLogs(this);
        runOnUiThread(() -> {
            logTextView.setText(logs);
            logScrollView.post(() -> logScrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    public void initializeViews() {
        findViewById(R.id.loginButton).setOnClickListener(v -> startDropboxOAuth());

        findViewById(R.id.chooseLocalFolderButton).setOnClickListener(v -> folderPicker.chooseFolder());

        pushFilesButton.setOnClickListener(v -> {
            FileLogger.log(this, "Push Files button was clicked.");
            if (localDirectoryUri != null && dropboxDirectory != null) {
                startFileUpload();
            } else {
                FileLogger.log(this, "Error: Local directory or Dropbox directory is not selected.");
            }
        });
    }

    private void startDropboxOAuth() {
        OauthHandler oauthHandler = new OauthHandler(this);
        oauthHandler.startDropboxOAuth();
    }

    private void refreshToken() {
        if (tokenManager.shouldRefreshToken()) {
            FileLogger.log(this, "Refreshing token...");
            OauthHandler oauthHandler = new OauthHandler(this);
            oauthHandler.startDropboxOAuth();
        }
    }

    public void startFileUpload() {
        FileLogger.log(this, "Starting file upload.");

        if (!isValidDirectory(Uri.parse(localDirectoryUri))) {
            FileLogger.log(this, "Error: Selected local directory is not valid.");
            return;
        }

        DocumentFile localDir = DocumentFile.fromTreeUri(this, Uri.parse(localDirectoryUri));
        if (localDir == null) {
            FileLogger.log(this, "Error: localDir is null.");
            return;
        }

        DocumentFile[] filesToUpload = localDir.listFiles();
        if (filesToUpload == null || filesToUpload.length == 0) {
            FileLogger.log(this, "No files to upload in the selected directory.");
            return;
        }

        for (DocumentFile file : filesToUpload) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".pdf")) {
                try {
                    if (dropboxDirectory == null || !dropboxDirectory.startsWith("/")) {
                        dropboxDirectory = "/" + (dropboxDirectory == null ? "" : dropboxDirectory);
                    }
                    uploadFileToDropbox(file, dropboxDirectory);
                } catch (Exception e) {
                    FileLogger.log(this, "Error uploading file " + file.getName() + " to Dropbox: " + e.getMessage());
                }
            } else {
                FileLogger.log(this, "Skipping non-PDF file: " + file.getName());
            }
        }
    }

    private void uploadFileToDropbox(DocumentFile file, String dropboxDirectory) throws IOException {
        FileLogger.log(this, "Uploading file " + file.getName() + " to Dropbox directory: " + dropboxDirectory);
        FilePusher filePusher = new FilePusher(this);
        filePusher.uploadFileToDropbox(file, dropboxDirectory);
    }

    private void checkAndRequestPermissions() {
        if (hasStoragePermission()) {
            FileLogger.log(this, "Storage permission is granted.");
            // Setup predefined directories if needed or load the previously selected directory
        } else {
            requestStoragePermission();
        }
    }

    private boolean hasStoragePermission() {
        int readPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);
        return readPermission == PackageManager.PERMISSION_GRANTED;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        folderPicker.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            FileLogger.log(this, "Storage permission granted.");
            // Setup predefined directories if needed or load the previously selected directory
        } else {
            FileLogger.log(this, "Storage permission denied.");
        }
    }

    @Override
    public void onFolderPicked(Uri uri) {
        FileLogger.log(this, "Local directory selected: " + uri.toString());
        localDirectoryUri = uri.toString();
        savePreferences();
    }

    @Override
    public void onFolderPickFailed(String errorMessage) {
        FileLogger.log(this, "Failed to pick local directory: " + errorMessage);
    }

    private boolean isValidDirectory(Uri uri) {
        DocumentFile documentFile = DocumentFile.fromTreeUri(this, uri);
        return documentFile != null && documentFile.exists() && documentFile.isDirectory();
    }

    private void savePreferences() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.KEY_LOCAL_DIR_URI, localDirectoryUri);
        editor.putString(Constants.KEY_DROPBOX_DIR, dropboxDirectory);
        editor.apply();
    }

    private void loadPreferences() {
        SharedPreferences prefs = getSharedPreferences(Constants.PREFS_NAME, MODE_PRIVATE);
        localDirectoryUri = prefs.getString(Constants.KEY_LOCAL_DIR_URI, null);
        dropboxDirectory = prefs.getString(Constants.KEY_DROPBOX_DIR, Constants.DROPBOX_DIRECTORY);
    }

    public String getAccessToken() {
        return tokenManager.getAccessToken();
    }
}
