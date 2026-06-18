package com.zipflash.mrp.helper;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import com.zipflash.mrp.SettingsHelper;

import java.io.*;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

/**
 * ShellHelper — works in both Shizuku and limited/normal mode.
 * Compatible with Java 7/8 and AIDE.
 */
public class ShellHelper {

    // Callback interface
    public interface OnScriptFinishedListener {
        void onFinished();
        void onError(String error);
    }

    // -------------------------------------------------------------
    // Run Script File (.sh)
    // -------------------------------------------------------------
    public static void runShellScript(final Context context,
                                      final String scriptPath,
                                      final TextView outputView,
                                      final OnScriptFinishedListener listener) {

        final File scriptFile = new File(scriptPath);
        final SettingsHelper settings = new SettingsHelper(context);

        // Validate file
        if (!scriptFile.exists()) {
            if (listener != null) listener.onError("Script not found: " + scriptPath);
            return;
        }

        // Block non-.sh files if restricted
        if (!settings.isAnyFileMode() && !scriptPath.endsWith(".sh")) {
            postLine(outputView, "[!] Only .sh files are allowed. Enable Any File Mode in Settings.\n");
            if (listener != null) listener.onError("Only .sh files allowed.");
            return;
        }

        postClear(outputView, "[#] Running: " + scriptFile.getName() + "\n");

        new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String[] command;
						if (scriptPath.endsWith(".sh")) {
							command = new String[]{"sh", scriptFile.getAbsolutePath()};
						} else {
							command = new String[]{scriptFile.getAbsolutePath()};
						}

						// -------------------- Check privilege --------------------
						if (CheckPermHelper.hasShizukuPermission()) {
							// Run via Shizuku
							runProcessWithShizuku(command, scriptFile.getParent(),
												  outputView, listener);
							return;
						}

						if (isRootAvailable()) {
							// Root fallback
							runProcessWithRoot(command, scriptFile.getParent(),
											   outputView, listener);
							return;
						}

						// Limited Mode: no Shizuku, no root
						//postLine(outputView, "[Limited Mode]");
						if (listener != null) listener.onFinished();

					} catch (final Exception e) {
						if (listener != null) listener.onError(e.getMessage());
						postLine(outputView, "[!] Failed: " + e.getMessage() + "\n");
					}
				}
			}).start();
    }

    // -------------------------------------------------------------
    // Run Inline Command
    // -------------------------------------------------------------
    public static void runShellCommand(final String command,
                                       final TextView outputView,
                                       final OnScriptFinishedListener listener) {
        new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						// --- Privilege path ---
						if (CheckPermHelper.hasShizukuPermission()) {
							runProcessWithShizuku(new String[]{"sh", "-c", command},
												  null, outputView, listener);
							return;
						}

						if (isRootAvailable()) {
							runProcessWithRoot(new String[]{"su", "-c", command},
											   null, outputView, listener);
							return;
						}

						// --- Limited mode ---
						//postLine(outputView, "[Limited Mode]");
						if (listener != null) listener.onFinished();

					} catch (final Exception e) {
						if (listener != null) listener.onError(e.getMessage());
						postLine(outputView, "[!] Failed: " + e.getMessage() + "\n");
					}
				}
			}).start();
    }

    // -------------------------------------------------------------
    // --- Private helpers ---
    // -------------------------------------------------------------

    private static void runProcessWithShizuku(String[] command,
                                              String workingDir,
                                              TextView outputView,
                                              OnScriptFinishedListener listener) throws Exception {

        ShizukuRemoteProcess process = Shizuku.newProcess(command, null, workingDir);

        BufferedReader input = new BufferedReader(
			new InputStreamReader(process.getInputStream()));
        BufferedReader error = new BufferedReader(
			new InputStreamReader(process.getErrorStream()));

        String line;
        while ((line = input.readLine()) != null) {
            postLine(outputView, line + "\n");
        }
        while ((line = error.readLine()) != null) {
            postLine(outputView, "[ERR] " + line + "\n");
        }

        int exitCode = process.waitFor();
        process.destroy();

        if (exitCode == 0) {
            if (listener != null) listener.onFinished();
        } else {
            if (listener != null) listener.onError("Exit code: " + exitCode);
        }
    }

    private static void runProcessWithRoot(String[] command,
                                           String workingDir,
                                           TextView outputView,
                                           OnScriptFinishedListener listener) throws Exception {

        ProcessBuilder pb = new ProcessBuilder(command);
        if (workingDir != null) pb.directory(new File(workingDir));
        Process process = pb.start();

        BufferedReader input = new BufferedReader(
			new InputStreamReader(process.getInputStream()));
        BufferedReader error = new BufferedReader(
			new InputStreamReader(process.getErrorStream()));

        String line;
        while ((line = input.readLine()) != null) {
            postLine(outputView, line + "\n");
        }
        while ((line = error.readLine()) != null) {
            postLine(outputView, "[ERR] " + line + "\n");
        }

        int exitCode = process.waitFor();
        process.destroy();

        if (exitCode == 0) {
            if (listener != null) listener.onFinished();
        } else {
            if (listener != null) listener.onError("Exit code: " + exitCode);
        }
    }

    private static void postLine(final TextView view, final String line) {
        if (view == null) return;
        view.post(new Runnable() {
				@Override
				public void run() {
					view.append(line);
				}
			});
    }

    private static void postClear(final TextView view, final String line) {
        if (view == null) return;
        view.post(new Runnable() {
				@Override
				public void run() {
					view.setText("");
					view.append(line);
				}
			});
    }

    private static boolean isRootAvailable() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"which", "su"});
            int exit = p.waitFor();
            return exit == 0;
        } catch (Exception e) {
            return false;
        }
    }
	
	
	
	
	// -------------------------------------------------------------
// Simple one-line privileged command wrapper (used by MainActivity)
// -------------------------------------------------------------
	public static String runPrivilegedCommand(final String cmd) {
		final StringBuilder output = new StringBuilder();

		try {
			// Prefer Shizuku if available
			if (CheckPermHelper.hasShizukuPermission()) {
				ShizukuRemoteProcess proc = Shizuku.newProcess(
                    new String[]{"sh", "-c", cmd}, null, null);

				BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
				}
				proc.waitFor();
				proc.destroy();
				return output.toString();
			}

			// Root fallback
			if (isRootAvailable()) {
				Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", cmd});
				BufferedReader reader = new BufferedReader(
                    new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = reader.readLine()) != null) {
					output.append(line).append("\n");
				}
				p.waitFor();
				return output.toString();
			}

			// Limited mode: no privilege
			//Log.i("ZipFlash", "[Limited Mode]");
			return "";

		} catch (Throwable t) {
			Log.e("ZipFlash", "runPrivilegedCommand failed: " + t.getMessage());
			return "";
		}
	}
	
}
