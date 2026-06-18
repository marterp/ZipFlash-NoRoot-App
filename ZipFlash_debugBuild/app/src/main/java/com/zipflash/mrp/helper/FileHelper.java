package com.zipflash.mrp.helper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import androidx.annotation.NonNull;
import java.io.File;

public class FileHelper {

    // Get filename from Uri
    public static String getFileName(@NonNull Context context, @NonNull Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            Cursor cursor = context.getContentResolver().query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null
            );
            if (cursor != null) {
                if (cursor.moveToFirst()) result = cursor.getString(0);
                cursor.close();
            }
        }

        if (result == null) {
            String path = uri.getPath();
            if (path != null) {
                int cut = path.lastIndexOf('/');
                result = (cut != -1) ? path.substring(cut + 1) : path;
            } else {
                result = "script.sh";
            }
        }

        return result;
    }

    public static File getMRPDir() {
        return new File("/data/local/tmp/MRP");
    }
}
