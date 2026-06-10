package com.zipflash.mrp.helper

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.zipflash.mrp.R
import com.zipflash.mrp.api.ApiClient
import com.zipflash.mrp.helper.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.lang.ref.WeakReference

class UpdateChecker(activity: Activity, private val mJsonUrl: String = GITHUB_UPDATE_URL) {

    companion object {
        const val GITHUB_UPDATE_URL = "https://raw.githubusercontent.com/marterp/ZipFlash-NoRoot/refs/heads/main/update.json"
        const val APK_FILENAME = "ZipFlash-update.apk"
    }

    private val mActivityRef = WeakReference(activity)
    private var progressDialog: AlertDialog? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var downloadJob: kotlinx.coroutines.Job? = null

    private fun getActivity(): Activity? = mActivityRef.get()

    fun checkForUpdate(currentVersion: String) {
        scope.launch {
            try {
                val call = ApiClient.updateApi.checkUpdateSync()
                val response = call.execute().body() ?: throw Exception("Empty response")
                val latestVersion = response.latestVersion
                val apkUrl = response.apkUrl
                val changelog = response.changelog

                if (currentVersion != latestVersion) {
                    val act = getActivity()
                    act?.runOnUiThread { showUpdateDialog(latestVersion, changelog, apkUrl) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("UpdateChecker", "Update check failed: ${e.message}")
            }
        }
    }

    private fun showUpdateDialog(latestVersion: String, changelog: String, apkUrl: String) {
        val act = getActivity() ?: return

        val dialog = AlertDialog.Builder(act, R.style.CustomDialogTheme).create()
        dialog.setCancelable(false)

        val layout = act.layoutInflater.inflate(R.layout.dialog_update, null) as LinearLayout
        val tvTitle = layout.findViewById<TextView>(R.id.tvTitle)
        val tvChangelog = layout.findViewById<TextView>(R.id.tvChangelog)
        val btnUpdate = layout.findViewById<Button>(R.id.btnUpdateNow)

        tvTitle.text = "Update Available ($latestVersion)"
        tvChangelog.text = changelog

        btnUpdate.setOnClickListener {
            dialog.dismiss()
            downloadApk(apkUrl)
        }

        dialog.setView(layout)
        dialog.show()

        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
    }

    private fun downloadApk(url: String) {
        val act = getActivity() ?: return

        if (!PermissionHelper.hasNotificationPermission(act)) {
            PermissionHelper.requestNotificationPermission(act)
        }

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val existingApk = File(downloadsDir, APK_FILENAME)
        if (existingApk.exists()) {
            val deleted = existingApk.delete()
            if (!deleted) {
                Log.w("UpdateChecker", "Failed to delete existing APK: ${existingApk.absolutePath}")
            }
        }

        progressDialog = AlertDialog.Builder(act, R.style.CustomDialogTheme).create()
        progressDialog?.setCancelable(false)

        val layout = act.layoutInflater.inflate(R.layout.dialog_progress, null) as LinearLayout
        val tvProgress = layout.findViewById<TextView>(R.id.tvProgress)
        val progressBar = layout.findViewById<ProgressBar>(R.id.progressBar)

        tvProgress.text = "Downloading update..."
        progressBar.max = 100

        progressDialog?.setView(layout)
        progressDialog?.show()

        progressDialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val request = DownloadManager.Request(Uri.parse(url))
        request.setTitle("Downloading update...")
        request.setDescription("ZipFlash APK update")
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILENAME)

        val manager = act.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        downloadJob = scope.launch {
            while (isActive) {
                val query = DownloadManager.Query()
                query.setFilterById(downloadId)
                val cursor = manager.query(query)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (statusIndex < 0) {
                        delay(500)
                        continue
                    }
                    val status = cursor.getInt(statusIndex)
                    if (status == DownloadManager.STATUS_SUCCESSFUL) {
                        val uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)
                        val uriString = if (uriIndex >= 0) cursor.getString(uriIndex) else null
                        cursor.close()
                        progressDialog?.dismiss()
                        val act = getActivity()
                        act?.runOnUiThread { installApk(uriString) }
                        break
                    } else if (status == DownloadManager.STATUS_RUNNING) {
                        val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        if (bytesDownloadedIndex >= 0 && bytesTotalIndex >= 0) {
                            val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                            val bytesTotal = cursor.getLong(bytesTotalIndex)
                            if (bytesTotal > 0) {
                                val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                                val act = getActivity()
                                act?.runOnUiThread {
                                    progressBar.progress = progress
                                    tvProgress.text = "Downloading: $progress%"
                                }
                            }
                        }
                        delay(500)
                    } else if (status == DownloadManager.STATUS_FAILED) {
                        cursor.close()
                        progressDialog?.dismiss()
                        Log.e("UpdateChecker", "Download failed")
                        val act = getActivity()
                        act?.runOnUiThread {
                            AlertDialog.Builder(act)
                                .setTitle("Download Failed")
                                .setMessage("Failed to download the update. Please try again.")
                                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                                .show()
                        }
                        break
                    } else {
                        delay(500)
                    }
                } else {
                    delay(500)
                }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        progressDialog?.dismiss()
    }

    private fun installApk(uriString: String?) {
        val act = getActivity() ?: return

        var apkUri: Uri? = null
        var apkFile: File? = null

        if (uriString != null) {
            try {
                apkUri = Uri.parse(uriString)
                apkFile = File(apkUri.path!!)
                if (!apkFile.exists()) {
                    apkUri = null
                }
            } catch (e: Exception) {
                Log.e("UpdateChecker", "Invalid URI: ${e.message}")
                apkUri = null
            }
        }

        if (apkUri == null) {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            apkFile = File(downloadsDir, APK_FILENAME)
            if (apkFile.exists()) {
                apkUri = Uri.fromFile(apkFile)
            }
        }

        if (apkUri != null && apkFile != null && apkFile.exists()) {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
                act.startActivity(intent)
            } catch (e: Exception) {
                Log.e("UpdateChecker", "Failed to start APK installation: ${e.message}")
                showInstallationError()
            }
        } else {
            Log.e("UpdateChecker", "APK file not found in Downloads folder")
            showInstallationError()
        }
    }

    private fun showInstallationError() {
        val act = getActivity() ?: return

        act.runOnUiThread {
            val dialog = AlertDialog.Builder(act, R.style.CustomDialogTheme)
                .setTitle("Download Complete!")
                .setMessage("Please install it manually from the Downloads folder or check your Notification panel.")
                .setPositiveButton("Close ZipFlash") { dialog, _ ->
                    dialog.dismiss()
                    val zArchiverIntent = Intent(Intent.ACTION_VIEW)
                    zArchiverIntent.setPackage("ru.zdevs.zarchiver")
                    val uri = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
                    zArchiverIntent.setDataAndType(uri, "resource/folder")
                    zArchiverIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    val pm = act.packageManager
                    if (zArchiverIntent.resolveActivity(pm) != null) {
                        try {
                            act.startActivity(zArchiverIntent)
                        } catch (e: android.content.ActivityNotFoundException) {
                            openGenericFileManager()
                        }
                    } else {
                        openGenericFileManager()
                    }
                }
                .create()

            dialog.setCancelable(false)
            dialog.setCanceledOnTouchOutside(false)
            dialog.show()
        }
    }

    private fun openGenericFileManager() {
        val act = getActivity() ?: return
        val intent = Intent(Intent.ACTION_VIEW)
        val uri = Uri.fromFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS))
        intent.setDataAndType(uri, "resource/folder")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            act.startActivity(Intent.createChooser(intent, "Open Download Folder"))
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(act.applicationContext, "No file manager found to open Download folder", Toast.LENGTH_SHORT).show()
        }
    }
}
