package com.arkabot.syncbot;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class FilePusher {
    private Context context;

    public FilePusher(Context context) {
        this.context = context;
    }

    public void uploadFileToDropbox(DocumentFile file, String dropboxDirectory) {
        Data inputData = new Data.Builder()
                .putString("fileUri", file.getUri().toString())
                .putString("dropboxDirectory", dropboxDirectory)
                .build();

        OneTimeWorkRequest uploadWorkRequest = new OneTimeWorkRequest.Builder(UploadWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context).enqueue(uploadWorkRequest);
    }
}
