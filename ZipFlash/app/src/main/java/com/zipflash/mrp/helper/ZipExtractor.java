package com.zipflash.mrp.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.widget.TextView;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public class ZipExtractor {

    /**
     * Extracts a module ZIP into /data/local/tmp/modules/<ModuleName>.
     * Returns the full module directory path.
     */
    /**
	 * Extracts a module ZIP into /data/local/tmp/modules/<ModuleName>.
	 * Returns the full module directory path.
	 * Ensures run.sh and revert.sh are present, otherwise deletes the module and throws an exception.
	 */
	public static String extractToModules(final Context context, final Uri uri, final TextView outputView) throws IOException, InterruptedException {
		final File modulesDir = new File("/data/local/tmp/modules");
		// Create directory using Shizuku
		if (!modulesDir.exists()) {
			ShizukuRemoteProcess mkdir = Shizuku.newProcess(
				new String[]{"mkdir", "-p", modulesDir.getAbsolutePath()}, null, null);
			mkdir.waitFor();
			if (mkdir.exitValue() != 0) {
				throw new IOException("Failed to create directory: " + modulesDir.getAbsolutePath());
			}
			mkdir.destroy();
		}

		// --- Get module name from ZIP filename ---
		String fileName = FileHelper.getFileName(context, uri);
		if (fileName == null) throw new IOException("Invalid file name");
		if (fileName.endsWith(".zip")) {
			fileName = fileName.substring(0, fileName.length() - 4);
		}

		final File moduleDir = new File(modulesDir, fileName);

		// --- Clean old version ---
		if (moduleDir.exists()) {
			deleteRecursive(moduleDir);
		}
		// Create module directory using Shizuku
		ShizukuRemoteProcess mkdirModule = Shizuku.newProcess(
			new String[]{"mkdir", "-p", moduleDir.getAbsolutePath()}, null, null);
		mkdirModule.waitFor();
		if (mkdirModule.exitValue() != 0) {
			throw new IOException("Failed to create module directory: " + moduleDir.getAbsolutePath());
		}
		mkdirModule.destroy();

		// --- Extract ZIP into moduleDir ---
		InputStream is = context.getContentResolver().openInputStream(uri);
		if (is == null) throw new IOException("Unable to open ZIP input stream");

		ZipInputStream zis = new ZipInputStream(is);
		ZipEntry entry;
		byte[] buffer = new byte[4096];
		int len;

		while ((entry = zis.getNextEntry()) != null) {
			File outFile = new File(moduleDir, entry.getName());

			if (entry.isDirectory()) {
				// Create directory using Shizuku
				ShizukuRemoteProcess mkdirEntry = Shizuku.newProcess(
					new String[]{"mkdir", "-p", outFile.getAbsolutePath()}, null, null);
				mkdirEntry.waitFor();
				if (mkdirEntry.exitValue() != 0) {
					throw new IOException("Failed to create directory: " + outFile.getAbsolutePath());
				}
				mkdirEntry.destroy();
			} else {
				File parent = outFile.getParentFile();
				if (parent != null && !parent.exists()) {
					ShizukuRemoteProcess mkdirParent = Shizuku.newProcess(
						new String[]{"mkdir", "-p", parent.getAbsolutePath()}, null, null);
					mkdirParent.waitFor();
					if (mkdirParent.exitValue() != 0) {
						throw new IOException("Failed to create parent directory: " + parent.getAbsolutePath());
					}
					mkdirParent.destroy();
				}

				// Write file using Shizuku
				ShizukuRemoteProcess copy = Shizuku.newProcess(
					new String[]{"sh", "-c", "cat > " + outFile.getAbsolutePath()}, null, null);
				OutputStream os = copy.getOutputStream();
				while ((len = zis.read(buffer)) > 0) {
					os.write(buffer, 0, len);
				}
				os.close();
				copy.waitFor();
				if (copy.exitValue() != 0) {
					throw new IOException("Failed to write file: " + outFile.getAbsolutePath());
				}
				copy.destroy();
			}
			zis.closeEntry();
		}
		zis.close();
		is.close();

		// --- Validate presence of run.sh and revert.sh ---
		File runScript = new File(moduleDir, "run.sh");
		File revertScript = new File(moduleDir, "revert.sh");
		if (!runScript.exists() || !revertScript.exists()) {
			// Clean up the module directory if scripts are missing
			deleteRecursive(moduleDir);
			final String errorMessage = "Module extraction failed: Missing " +
				("script");
			outputView.post(new Runnable() {
					@Override
					public void run() {
						outputView.append("[!] " + errorMessage + "\n");
					}
				});
			throw new IOException(errorMessage);
		}

		// --- Save module state as enabled ---
		SharedPreferences prefs = context.getSharedPreferences("modules_state", Context.MODE_PRIVATE);
		prefs.edit().putBoolean(fileName, true).apply();

		final String modName = fileName;
		outputView.post(new Runnable() {
				@Override
				public void run() {
					outputView.append("[✓] Installed & enabled module: " + modName + "\n");
				}
			});

		return moduleDir.getAbsolutePath();
	}

    /**
     * Copies a script (run.sh or revert.sh) from moduleDir into /data/local/tmp/MRP/
     * so it can be executed via ShellHelper. (Uses Shizuku to bypass SELinux restrictions)
     */
    public static File prepareScriptForExecution(File moduleDir, String scriptName) throws IOException {
        File scriptFile = new File(moduleDir, scriptName);
        if (!scriptFile.exists()) {
            throw new IOException("Script not found: " + scriptName);
        }

        File mrpDir = new File("/data/local/tmp/MRP");

        try {
            // Clean and recreate /data/local/tmp/MRP
            Shizuku.newProcess(new String[]{"sh", "-c", "rm -rf " + mrpDir.getAbsolutePath()}, null, null).waitFor();
            Shizuku.newProcess(new String[]{"mkdir", "-p", mrpDir.getAbsolutePath()}, null, null).waitFor();

            File outFile = new File(mrpDir, scriptName);

            // Copy via Shizuku (cat > file)
            ShizukuRemoteProcess process = Shizuku.newProcess(
                new String[]{"sh", "-c", "cat > " + outFile.getAbsolutePath()},
                null,
                null
            );

            FileInputStream in = new FileInputStream(scriptFile);
            OutputStream os = process.getOutputStream();

            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) > 0) {
                os.write(buffer, 0, len);
            }

            in.close();
            os.close();
            process.waitFor();
            process.destroy();

            return outFile;

        } catch (Exception e) {
            throw new IOException("Failed to prepare script: " + e.getMessage(), e);
        }
    }

    // -------- Utility Methods --------
    private static void deleteRecursive(File file) throws InterruptedException, IOException {
        try {
            ShizukuRemoteProcess rm = Shizuku.newProcess(
                new String[]{"rm", "-rf", file.getAbsolutePath()}, null, null);
            rm.waitFor();
            if (rm.exitValue() != 0) {
                throw new IOException("Failed to delete " + file.getAbsolutePath());
            }
            rm.destroy();
        } catch (IOException e) {
            throw e;
        }
    }
}
