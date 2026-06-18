package com.zipflash.mrp.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import com.zipflash.mrp.R;
import android.content.pm.PackageManager;
import android.content.ActivityNotFoundException;
import android.widget.Toast;

public class UpdateChecker {

    private Activity mActivity;
    private String mJsonUrl;
    private AlertDialog progressDialog; // For showing download progress
    private static final String APK_FILENAME = "ZipFlash-update.apk";

    public UpdateChecker(Activity activity, String jsonUrl) {
        mActivity = activity;
        mJsonUrl = jsonUrl;
    }

    public void checkForUpdate(final String currentVersion) {
        new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						URL url = new URL(mJsonUrl);
						HttpURLConnection conn = (HttpURLConnection) url.openConnection();
						conn.setRequestMethod("GET");
						conn.setConnectTimeout(5000);
						conn.setReadTimeout(5000);
						conn.connect();

						InputStream is = conn.getInputStream();
						BufferedReader reader = new BufferedReader(new InputStreamReader(is));
						StringBuilder sb = new StringBuilder();
						String line;
						while ((line = reader.readLine()) != null) {
							sb.append(line);
						}
						reader.close();
						is.close();
						conn.disconnect();

						JSONObject json = new JSONObject(sb.toString());
						final String latestVersion = json.getString("latest_version");
						final String apkUrl = json.getString("apk_url");
						final String changelog = json.optString("changelog", "");

						// Compare versions
						if (!currentVersion.equals(latestVersion)) {
							mActivity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										showUpdateDialog(latestVersion, changelog, apkUrl);
									}
								});
						}

					} catch (final Exception e) {
						e.printStackTrace();
						Log.e("UpdateChecker", "Update check failed: " + e.getMessage());
					}
				}
			}).start();
    }

    private void showUpdateDialog(final String latestVersion, String changelog, final String apkUrl) {
        final AlertDialog dialog = new AlertDialog.Builder(mActivity, R.style.CustomDialogTheme)
			.create();

        dialog.setCancelable(false); // User must update

        // Inflate custom layout
        LinearLayout layout = (LinearLayout) mActivity.getLayoutInflater()
			.inflate(R.layout.dialog_update, null);

        TextView tvTitle = layout.findViewById(R.id.tvTitle);
        TextView tvChangelog = layout.findViewById(R.id.tvChangelog);
        Button btnUpdate = layout.findViewById(R.id.btnUpdateNow);

        tvTitle.setText("Update Available (" + latestVersion + ")");
        tvChangelog.setText(changelog);

        btnUpdate.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					dialog.dismiss();
					downloadApk(apkUrl);
				}
			});

        dialog.setView(layout);
        dialog.show();

        // Make background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
				new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
        }
    }

    private void downloadApk(String url) {
        // Check and delete existing APK to ensure override
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File existingApk = new File(downloadsDir, APK_FILENAME);
        if (existingApk.exists()) {
            boolean deleted = existingApk.delete();
            if (!deleted) {
                Log.w("UpdateChecker", "Failed to delete existing APK: " + existingApk.getAbsolutePath());
            }
        }

        // Create a ProgressDialog with a ProgressBar
        progressDialog = new AlertDialog.Builder(mActivity, R.style.CustomDialogTheme)
			.create();
        progressDialog.setCancelable(false); // Prevent dismissing during download

        // Inflate custom layout for progress dialog
        LinearLayout layout = (LinearLayout) mActivity.getLayoutInflater()
			.inflate(R.layout.dialog_progress, null);

        final TextView tvProgress = layout.findViewById(R.id.tvProgress);
        final ProgressBar progressBar = layout.findViewById(R.id.progressBar);

        tvProgress.setText("Downloading update...");
        progressBar.setMax(100); // Set max progress to 100%

        progressDialog.setView(layout);
        progressDialog.show();

        // Make background transparent
        if (progressDialog.getWindow() != null) {
            progressDialog.getWindow().setBackgroundDrawable(
				new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT)
            );
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Downloading update...");
        request.setDescription("ZipFlash APK update");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILENAME);

        final DownloadManager manager = (DownloadManager) mActivity.getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(request);

        // Check download progress
        final Handler handler = new Handler();
        final Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(downloadId);
                Cursor cursor = manager.query(query);
                if (cursor != null && cursor.moveToFirst()) {
                    int status = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS));
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        final String uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        cursor.close();
                        progressDialog.dismiss();
                        // Automatically start APK installation
                        mActivity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									installApk(uriString);
								}
							});
                    } else if (status == DownloadManager.STATUS_RUNNING) {
                        // Calculate progress
                        long bytesDownloaded = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                        long bytesTotal = cursor.getLong(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));
                        if (bytesTotal > 0) {
                            final int progress = (int) ((bytesDownloaded * 100L) / bytesTotal);
                            mActivity.runOnUiThread(new Runnable() {
									@Override
									public void run() {
										progressBar.setProgress(progress);
										tvProgress.setText("Downloading: " + progress + "%");
									}
								});
                        }
                        handler.postDelayed(this, 500); // Update every 500ms
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        cursor.close();
                        progressDialog.dismiss();
                        Log.e("UpdateChecker", "Download failed");
                        mActivity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									new AlertDialog.Builder(mActivity)
                                        .setTitle("Download Failed")
                                        .setMessage("Failed to download the update. Please try again.")
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .show();
								}
							});
                    } else {
                        handler.postDelayed(this, 500); // Retry in 500ms
                    }
                } else {
                    if (cursor != null) {
                        cursor.close();
                    }
                    handler.postDelayed(this, 500); // Retry in 500ms
                }
            }
        };
        handler.post(progressRunnable);
    }

    private void installApk(String uriString) {
        Uri apkUri = null;
        File apkFile = null;

        // Try to use the DownloadManager's URI first
        if (uriString != null) {
            try {
                apkUri = Uri.parse(uriString);
                apkFile = new File(apkUri.getPath());
                if (!apkFile.exists()) {
                    apkUri = null; // File doesn't exist, fallback to manual search
                }
            } catch (Exception e) {
                Log.e("UpdateChecker", "Invalid URI: " + e.getMessage());
                apkUri = null; // Fallback to manual search
            }
        }

        // Fallback: Check Downloads folder for the APK
        if (apkUri == null) {
            File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            apkFile = new File(downloadsDir, APK_FILENAME);
            if (apkFile.exists()) {
                apkUri = Uri.fromFile(apkFile);
            }
        }

        // Attempt to install the APK
        if (apkUri != null && apkFile != null && apkFile.exists()) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                mActivity.startActivity(intent);
            } catch (Exception e) {
                Log.e("UpdateChecker", "Failed to start APK installation: " + e.getMessage());
                showInstallationError();
            }
        } else {
            Log.e("UpdateChecker", "APK file not found in Downloads folder");
            showInstallationError();
        }
    }

	private void showInstallationError() {
		mActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					AlertDialog dialog = new AlertDialog.Builder(mActivity, R.style.CustomDialogTheme)
						.setTitle("Download Complete!")
						.setMessage("Please install it manually from the Downloads folder or check your Notification panel.")
						.setPositiveButton("Close ZipFlash", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								dialog.dismiss();
								// Try to open ZArchiver's Download folder
								Intent zArchiverIntent = new Intent(Intent.ACTION_VIEW);
								zArchiverIntent.setPackage("ru.zdevs.zarchiver");
								Uri uri = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
								zArchiverIntent.setDataAndType(uri, "resource/folder");
								zArchiverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

								// Check if ZArchiver is installed
								PackageManager pm = mActivity.getPackageManager();
								if (zArchiverIntent.resolveActivity(pm) != null) {
									try {
										mActivity.startActivity(zArchiverIntent);
									} catch (ActivityNotFoundException e) {
										openGenericFileManager();
									}
								} else {
									openGenericFileManager();
								}
							}
						})
						.create();

					dialog.setCancelable(false); // Prevent dismissal via back button
					dialog.setCanceledOnTouchOutside(false); // Prevent dismissal by touching outside
					dialog.show();
				}

				

				private void openGenericFileManager() {
					Intent intent = new Intent(Intent.ACTION_VIEW);
					Uri uri = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
					intent.setDataAndType(uri, "resource/folder");
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					try {
						mActivity.startActivity(Intent.createChooser(intent, "Open Download Folder"));
					} catch (ActivityNotFoundException e) {
						Toast.makeText(mActivity.getApplicationContext(), "No file manager found to open Download folder", Toast.LENGTH_SHORT).show();
					}
				}
			});
	}
    
}
