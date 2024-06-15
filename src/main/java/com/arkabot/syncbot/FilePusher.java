package com.arkabot.syncbot;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.DbxException;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicLong;

public class FilePusher {
    private DbxClientV2 client;
    private Context context;

    public FilePusher(Context context) {
        this.context = context;
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/syncbot").build();
        String accessToken = new TokenManager(context).getAccessToken();
        FileLogger.log(context, "Initializing Dropbox client with access token: " + (accessToken != null ? "present" : "missing"));
        client = new DbxClientV2(config, accessToken);
    }

    public void uploadFileToDropbox(DocumentFile file, String dropboxDirectory) {
        try {
            String dropboxPath = dropboxDirectory + "/" + file.getName();
            dropboxPath = dropboxPath.replaceAll("//", "/");

            if (!dropboxPath.startsWith("/")) {
                dropboxPath = "/" + dropboxPath;
            }

            long fileSize = file.length();
            AtomicLong bytesUploaded = new AtomicLong(0);

            FileLogger.log(context, "Preparing to upload file: " + file.getName() + " to Dropbox path: " + dropboxPath + " with size: " + fileSize + " bytes");

            try (InputStream in = context.getContentResolver().openInputStream(file.getUri())) {
                if (in == null) {
                    FileLogger.log(context, "Failed to open input stream for file: " + file.getName());
                    return;
                }
                FileLogger.log(context, "Input stream opened for file: " + file.getName());

                FileMetadata metadata = client.files().uploadBuilder(dropboxPath)
                        .withMode(WriteMode.OVERWRITE)
                        .uploadAndFinish(in, new ProgressListener(fileSize, bytesUploaded));

                FileLogger.log(context, "File uploaded successfully to Dropbox: " + metadata.getPathLower());
                deleteFileFromDevice(file);
            } catch (DbxException | IOException e) {
                FileLogger.log(context, "Error uploading file to Dropbox: " + e.getMessage());
                e.printStackTrace();
            }
        } catch (Exception e) {
            FileLogger.log(context, "Unexpected error during file upload: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private class ProgressListener implements com.dropbox.core.util.IOUtil.ProgressListener {
        private final long fileSize;
        private final AtomicLong bytesUploaded;

        public ProgressListener(long fileSize, AtomicLong bytesUploaded) {
            this.fileSize = fileSize;
            this.bytesUploaded = bytesUploaded;
        }

        @Override
        public void onProgress(long bytesRead) {
            bytesUploaded.addAndGet(bytesRead);
            double progress = (double) bytesUploaded.get() / fileSize * 100;
            FileLogger.log(context, String.format("Upload progress: %.2f%%", progress));
        }
    }

    private void deleteFileFromDevice(DocumentFile file) {
        if (file.delete()) {
            FileLogger.log(context, "File deleted from device: " + file.getName());
        } else {
            FileLogger.log(context, "Failed to delete file from device: " + file.getName());
        }
    }
}
