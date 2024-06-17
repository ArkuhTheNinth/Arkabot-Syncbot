package com.arkabot.syncbot;

import android.content.Context;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.DbxException;
import java.io.IOException;
import java.io.InputStream;

public class UploadWorker extends Worker {
    private DbxClientV2 client;
    private Context context;

    public UploadWorker(Context context, WorkerParameters params) {
        super(context, params);
        this.context = context;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/syncbot").build();
        String accessToken = new TokenManager(context).getAccessToken();
        FileLogger.log(context, "Initializing Dropbox client with access token: " + (accessToken != null ? "present" : "missing"));
        client = new DbxClientV2(config, accessToken);
    }

    @Override
    public Result doWork() {
        String fileUri = getInputData().getString("fileUri");
        String dropboxDirectory = getInputData().getString("dropboxDirectory");
        DocumentFile file = DocumentFile.fromSingleUri(context, Uri.parse(fileUri));

        if (file == null) {
            FileLogger.log(context, "File not found: " + fileUri);
            return Result.failure();
        }

        try {
            String dropboxPath = dropboxDirectory + "/" + file.getName();
            dropboxPath = dropboxPath.replaceAll("//", "/");

            if (!dropboxPath.startsWith("/")) {
                dropboxPath = "/" + dropboxPath;
            }

            FileLogger.log(context, "Collecting file: " + file.getName());

            try (InputStream in = context.getContentResolver().openInputStream(file.getUri())) {
                if (in == null) {
                    FileLogger.log(context, "Failed to open input stream; Check your connection.");
                    return Result.failure();
                }
                FileLogger.log(context, "Uploading...");

                FileMetadata metadata = client.files().uploadBuilder(dropboxPath)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(in);

                FileLogger.log(context, "Success!");
                deleteFileFromDevice(file);
                return Result.success();
            } catch (DbxException | IOException e) {
                FileLogger.log(context, "Error uploading file to Dropbox: " + e.getMessage());
                e.printStackTrace();
                return Result.failure();
            }
        } catch (Exception e) {
            FileLogger.log(context, "Unexpected error during file upload: " + e.getMessage());
            e.printStackTrace();
            return Result.failure();
        }
    }

    private void deleteFileFromDevice(DocumentFile file) {
        if (file.delete()) {
            FileLogger.log(context, "Deleted uploaded file.");
        } else {
            FileLogger.log(context, "Failed to delete " + file.getName());
        }
    }
}
