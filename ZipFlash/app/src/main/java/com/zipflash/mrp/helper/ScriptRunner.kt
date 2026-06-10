package com.zipflash.mrp.helper

import android.content.Context
import android.net.Uri
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import java.io.File

object ScriptRunner {
    interface OnScriptFinishedListener {
        fun onFinished()
        fun onError(error: String)
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun runSingleShBlocking(context: Context, uri: Uri, listener: OnScriptFinishedListener?) {
        try {
            if (!CheckPermHelper.hasShizukuPermission()) {
                listener?.onError("Shizuku permission not available.")
                return
            }

            val fileName = FileHelper.getFileName(context, uri)
            if (fileName == null) {
                listener?.onError("Invalid file.")
                return
            }

            val targetDir = "/data/local/tmp/MRP"
            val targetPath = "$targetDir/$fileName"

            val copyProcess = Shizuku.newProcess(
                arrayOf("sh", "-c", "mkdir -p $targetDir && cat > $targetPath"), null, null
            )

            val `is` = context.contentResolver.openInputStream(uri) ?: throw Exception("Failed to open input stream")
            val os = copyProcess.outputStream
            val buffer = ByteArray(4096)
            var len: Int
            while (`is`.read(buffer).also { len = it } > 0) {
                os.write(buffer, 0, len)
            }
            os.close()
            `is`.close()
            copyProcess.waitFor()
            copyProcess.destroy()

            Shizuku.newProcess(arrayOf("chmod", "+x", targetPath), null, null).waitFor()

            ShellHelper.runShellScriptBlocking(context, targetPath, object : ShellHelper.OnScriptFinishedListener {
                override fun onFinished() {
                    listener?.onFinished()
                }

                override fun onError(error: String) {
                    listener?.onError(error)
                }
            })
        } catch (e: Exception) {
            listener?.onError(e.message ?: "Unknown error")
        }
    }

    fun runSingleSh(context: Context, uri: Uri, outputView: TextView, listener: OnScriptFinishedListener?) {
        scope.launch {
            try {
                if (!CheckPermHelper.hasShizukuPermission()) {
                    outputView.post { outputView.append("[!] Shizuku permission not available.\n") }
                    listener?.onError("Shizuku permission not available.")
                    return@launch
                }

                val fileName = FileHelper.getFileName(context, uri)
                if (fileName == null) {
                    outputView.post { outputView.append("[!] Invalid file.\n") }
                    listener?.onError("Invalid file.")
                    return@launch
                }

                val targetDir = "/data/local/tmp/MRP"
                val targetPath = "$targetDir/$fileName"

                val copyProcess = Shizuku.newProcess(
                    arrayOf("sh", "-c", "mkdir -p $targetDir && cat > $targetPath"), null, null
                )

                val `is` = context.contentResolver.openInputStream(uri) ?: throw Exception("Failed to open input stream")
                val os = copyProcess.outputStream
                val buffer = ByteArray(4096)
                var len: Int
                while (`is`.read(buffer).also { len = it } > 0) {
                    os.write(buffer, 0, len)
                }
                os.close()
                `is`.close()
                copyProcess.waitFor()
                copyProcess.destroy()

                Shizuku.newProcess(arrayOf("chmod", "+x", targetPath), null, null).waitFor()

                ShellHelper.runShellScript(context, targetPath, outputView, object : ShellHelper.OnScriptFinishedListener {
                    override fun onFinished() {
                        listener?.onFinished()
                    }

                    override fun onError(error: String) {
                        listener?.onError(error)
                    }
                })

            } catch (e: Exception) {
                outputView.post { outputView.append("[!] Failed to prepare file: ${e.message}\n") }
                listener?.onError(e.message ?: "Unknown error")
            }
        }
    }
}
