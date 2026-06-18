package com.zipflash.mrp.helper;

import android.content.Context;
import android.net.Uri;
import android.widget.TextView;

import com.zipflash.mrp.SettingsHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.File;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public class ScriptRunner {

    public interface OnScriptFinishedListener {
        void onFinished();
        void onError(String error);
    }

    public static void runSingleSh(final Context context,
                                   final Uri uri,
                                   final TextView outputView,
                                   final OnScriptFinishedListener listener) {

        new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final String fileName = FileHelper.getFileName(context, uri);
                        if (fileName == null) {
                            outputView.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        outputView.append("[!] Invalid file.\n");
                                    }
                                });
                            if (listener != null) listener.onError("Invalid file.");
                            return;
                        }

                        final String targetDir = "/data/local/tmp/MRP";
                        final String targetPath = targetDir + "/" + fileName;

                        // Copy file into /data/local/tmp/MRP
                        final ShizukuRemoteProcess copyProcess = Shizuku.newProcess(
                            new String[]{"sh", "-c", "mkdir -p " + targetDir + " && cat > " + targetPath},
                            null,
                            null
                        );

                        byte[] buffer = new byte[4096];
                        InputStream is = context.getContentResolver().openInputStream(uri);
                        OutputStream os = copyProcess.getOutputStream();
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            os.write(buffer, 0, len);
                        }
                        os.close();
                        is.close();
                        copyProcess.waitFor();
                        copyProcess.destroy();

                        // Make file executable
                        Shizuku.newProcess(
                            new String[]{"chmod", "+x", targetPath},
                            null,
                            null
                        ).waitFor();

                        // ✅ Hand off execution to ShellHelper
                        ShellHelper.runShellScript(context, targetPath, outputView, new ShellHelper.OnScriptFinishedListener() {
                                @Override
                                public void onFinished() {
                                    if (listener != null) listener.onFinished();
                                }

                                @Override
                                public void onError(String error) {
                                    if (listener != null) listener.onError(error);
                                }
                            });

                    } catch (final Exception e) {
                        outputView.post(new Runnable() {
                                @Override
                                public void run() {
                                    outputView.append("[!] Failed to prepare file: " + e.getMessage() + "\n");
                                }
                            });
                        if (listener != null) listener.onError(e.getMessage());
                    }
                }
            }).start();
    }
}
