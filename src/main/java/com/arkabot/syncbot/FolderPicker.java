package com.arkabot.syncbot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.DocumentsContract;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

public class FolderPicker {
    private static final int REQUEST_CODE_PICK_FOLDER = 1;
    private Context context;
    private FolderPickerListener listener;

    public FolderPicker(Context context, FolderPickerListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void chooseFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        ((Activity) context).startActivityForResult(intent, REQUEST_CODE_PICK_FOLDER);
    }

    public void handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE_PICK_FOLDER && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri treeUri = data.getData();
                if (treeUri != null) {
                    context.getContentResolver().takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                    DocumentFile pickedDir = DocumentFile.fromTreeUri(context, treeUri);
                    if (pickedDir != null && pickedDir.isDirectory()) {
                        listener.onFolderPicked(pickedDir.getUri());
                    } else {
                        listener.onFolderPickFailed("Selected directory is not valid");
                    }
                } else {
                    listener.onFolderPickFailed("Failed to get directory URI");
                }
            }
        }
    }

    public interface FolderPickerListener {
        void onFolderPicked(Uri uri);
        void onFolderPickFailed(String errorMessage);
    }
}
